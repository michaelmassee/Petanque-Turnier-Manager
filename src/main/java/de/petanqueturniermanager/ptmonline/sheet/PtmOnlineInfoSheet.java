/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.sheet;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.FontWeight;
import com.sun.star.beans.XPropertySet;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.table.XCellRange;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.ptmonline.PtmOnlineRegistrationMapping;
import de.petanqueturniermanager.ptmonline.dto.TournamentMetadataDto;

/**
 * Schreibt die bisher nur unsichtbar in DocumentProperties gehaltenen PTM-Online-Informationen
 * (Online-Turnier-ID, Base-URL, letzte Synchronisation, Eckdaten, Team-Nr/Online-Registrierungs-ID-
 * Zuordnung) sichtbar in ein Sheet.
 * <p>
 * Der Status-Block (Turnier-ID/Base-URL/letzte Sync) und die Mapping-Tabelle sind reine Anzeige und
 * werden nach jeder PTM-Online-Aktion per {@link #aktualisiereBestEffort} neu geschrieben. Der
 * Eckdaten-Block (Name, Kontakt, Sichtbarkeit, ...) ist dagegen direkt im Sheet editierbar — er wird
 * NUR explizit per {@link #schreibeEckdaten} (Turnier anlegen / Eckdaten bearbeiten / Abgleich mit
 * bereits vorhandener Online-Verknuepfung) geschrieben, damit {@link #aktualisiereBestEffort} Nutzer-
 * Eingaben dort nicht bei jeder Nebenaktion (z.B. Anmeldungen importieren) ueberschreibt.
 * {@link #leseEckdaten} liest den aktuellen Stand vor jedem Online-Push zurueck.
 */
public final class PtmOnlineInfoSheet {

    private static final Logger logger = LogManager.getLogger(PtmOnlineInfoSheet.class);

    private static final int SPALTE_LABEL = 0;
    private static final int SPALTE_WERT = 1;

    private static final int ZEILE_TURNIER_ID = 0;
    private static final int ZEILE_LETZTE_SYNC = 2;

    private static final int ZEILE_NAME = 4;
    private static final int ZEILE_DATUM = 5;
    private static final int ZEILE_STARTZEIT = 6;
    private static final int ZEILE_ORT = 7;
    private static final int ZEILE_TYP = 8;
    private static final int ZEILE_FORMATION = 9;
    private static final int ZEILE_BESCHREIBUNG = 10;
    private static final int ZEILE_STATUS = 11;
    private static final int ZEILE_SICHTBARKEIT = 12;
    private static final int ZEILE_MAX_ANMELDUNGEN = 13;
    private static final int ZEILE_ANMELDESCHLUSS = 14;
    private static final int ZEILE_STARTGELD_CENT = 15;
    private static final int ZEILE_KONTAKT_NAME = 16;
    private static final int ZEILE_KONTAKT_EMAIL = 17;
    private static final int ZEILE_KONTAKT_TELEFON = 18;
    private static final int ZEILE_INTERNE_NOTIZEN = 19;
    private static final int ZEILE_TEILNEHMER_OEFFENTLICH = 20;
    private static final int ZEILE_LIZENZPFLICHT = 21;

    private static final int SPALTE_MAPPING_TEAM_NR = 0;
    private static final int SPALTE_MAPPING_ONLINE_ID = 1;
    private static final int ZEILE_MAPPING_HEADER = 23;
    private static final int ZEILE_MAPPING_ERSTE_DATENZEILE = ZEILE_MAPPING_HEADER + 1;
    /** Grosszuegige Reserve: ein Rewrite muss auch eine zuvor laengere Mapping-Tabelle ueberschreiben. */
    private static final int MAPPING_MAX_ZEILEN = 2000;

    private static final int HEADER_HG_FARBE = 0xCFE2F3;
    private static final String TAB_FARBE = "1F4E79";

    private static final int SPALTENBREITE_LABEL = 4500;
    private static final int SPALTENBREITE_WERT = 6000;

    private static final DateTimeFormatter SYNC_FORMAT =
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault());

    private PtmOnlineInfoSheet() {}

    /**
     * Wie {@link #aktualisiere}, aber fehlertolerant: die Info-Sheet-Aktualisierung ist reine
     * Zusatzinformation und darf eine ansonsten erfolgreiche PTM-Online-Aktion nicht scheitern lassen.
     */
    public static void aktualisiereBestEffort(WorkingSpreadsheet ws, String baseUrl, PtmOnlineRegistrationMapping mapping) {
        try {
            aktualisiere(ws, baseUrl, mapping);
        } catch (GenerateException e) {
            logger.warn("PTM-Online-Info-Sheet konnte nicht aktualisiert werden: {}", e.getMessage(), e);
        }
    }

    /**
     * Legt das Sheet bei Bedarf an und schreibt Status-Info (Turnier-ID/Base-URL/letzte Sync) sowie
     * Mapping-Tabelle neu. Fasst den vom Nutzer editierbaren Eckdaten-Block bewusst NICHT an — siehe
     * Klassen-Kommentar.
     */
    public static void aktualisiere(WorkingSpreadsheet ws, String baseUrl,
            PtmOnlineRegistrationMapping mapping) throws GenerateException {
        SheetHelper sh = new SheetHelper(ws);
        XSpreadsheet sheet = sheetSicherstellen(ws, sh);

        schreibeStatusInfo(ws, sheet, baseUrl, mapping);
        schreibeMapping(ws, sheet, mapping);
    }

    /**
     * Schreibt den Eckdaten-Block (Name, Kontakt, Sichtbarkeit, ...) explizit neu — bewusst
     * ausgelagert aus {@link #aktualisiere}, damit dieser Block nur bei tatsaechlicher inhaltlicher
     * Aenderung (Turnier anlegen, Eckdaten bearbeiten, Uebernahme des Online-Stands) ueberschrieben
     * wird und sonst editierbar bleibt.
     */
    public static void schreibeEckdatenBestEffort(WorkingSpreadsheet ws, TournamentMetadataDto metadata) {
        try {
            SheetHelper sh = new SheetHelper(ws);
            XSpreadsheet sheet = sheetSicherstellen(ws, sh);
            schreibeEckdaten(ws, sheet, metadata);
        } catch (GenerateException e) {
            logger.warn("PTM-Online-Eckdaten konnten nicht ins Sheet geschrieben werden: {}", e.getMessage(), e);
        }
    }

    /**
     * Liest den aktuellen Stand des Eckdaten-Blocks aus dem Sheet — die massgebliche Quelle fuer
     * einen Online-Push, da der Nutzer die Felder direkt im Sheet editieren kann. Leer, wenn das
     * Sheet (noch) nicht existiert.
     */
    public static Optional<TournamentMetadataDto> leseEckdaten(WorkingSpreadsheet ws) {
        SheetHelper sh = new SheetHelper(ws);
        XSpreadsheet sheet = sh.findByName(SheetNamen.ptmOnline());
        if (sheet == null) {
            return Optional.empty();
        }
        return Optional.of(new TournamentMetadataDto(
                text(sh, sheet, ZEILE_NAME), text(sh, sheet, ZEILE_DATUM), text(sh, sheet, ZEILE_STARTZEIT),
                text(sh, sheet, ZEILE_ORT), leer2Null(text(sh, sheet, ZEILE_BESCHREIBUNG)),
                text(sh, sheet, ZEILE_TYP), text(sh, sheet, ZEILE_FORMATION), text(sh, sheet, ZEILE_STATUS),
                ganzzahl(text(sh, sheet, ZEILE_MAX_ANMELDUNGEN)), leer2Null(text(sh, sheet, ZEILE_ANMELDESCHLUSS)),
                ganzzahl(text(sh, sheet, ZEILE_STARTGELD_CENT)), leer2Null(text(sh, sheet, ZEILE_KONTAKT_NAME)),
                leer2Null(text(sh, sheet, ZEILE_KONTAKT_EMAIL)), leer2Null(text(sh, sheet, ZEILE_KONTAKT_TELEFON)),
                text(sh, sheet, ZEILE_SICHTBARKEIT), leer2Null(text(sh, sheet, ZEILE_INTERNE_NOTIZEN)),
                boolWert(text(sh, sheet, ZEILE_TEILNEHMER_OEFFENTLICH)), boolWert(text(sh, sheet, ZEILE_LIZENZPFLICHT))));
    }

    private static XSpreadsheet sheetSicherstellen(WorkingSpreadsheet ws, SheetHelper sh) throws GenerateException {
        String sheetName = SheetNamen.ptmOnline();
        XSpreadsheet sheet = sh.findByName(sheetName);
        boolean neuAngelegt = sheet == null;
        if (neuAngelegt) {
            sheet = sh.newIfNotExist(sheetName, anhaengePosition(sh), TAB_FARBE);
        }
        if (sheet == null) {
            throw new GenerateException(I18n.get("error.tabelle.nicht.vorhanden", sheetName));
        }
        registriereSheetMetadaten(ws, sheet);
        if (neuAngelegt) {
            formatiere(sh, sheet);
        }
        return sheet;
    }

    /**
     * Registriert das Sheet als Named-Range-Metadaten-Eintrag (Schluessel {@link
     * SheetMetadataHelper#SCHLUESSEL_PTM_ONLINE_INFO}), damit es in der Sidebar-Sheetliste
     * ({@link de.petanqueturniermanager.sidebar.sheets.SheetBaumOrganisierer}) erscheint — die
     * liest ausschliesslich Sheets mit solchen Schluesseln, nicht die Tabs des Dokuments direkt.
     * {@link SheetHelper#newIfNotExist} legt das Sheet nur an, ohne diese Metadaten zu schreiben.
     */
    private static void registriereSheetMetadaten(WorkingSpreadsheet ws, XSpreadsheet sheet) {
        XSpreadsheetDocument xDoc = ws.getWorkingSpreadsheetDocument();
        if (!SheetMetadataHelper.istRegistriertesSheet(xDoc, sheet, SheetMetadataHelper.SCHLUESSEL_PTM_ONLINE_INFO)) {
            SheetMetadataHelper.schreibeSheetMetadaten(xDoc, sheet, SheetMetadataHelper.SCHLUESSEL_PTM_ONLINE_INFO);
        }
    }

    private static short anhaengePosition(SheetHelper sh) {
        try {
            return (short) sh.getSheets().getElementNames().length;
        } catch (RuntimeException e) {
            logger.warn("Position für PTM-Online-Info-Sheet nicht ermittelbar, Fallback hintenan", e);
            return Short.MAX_VALUE;
        }
    }

    private static void schreibeStatusInfo(WorkingSpreadsheet ws, XSpreadsheet sheet, String baseUrl,
            PtmOnlineRegistrationMapping mapping) throws GenerateException {
        RangeData rd = new RangeData();

        RowData turnierIdZeile = rd.addNewRow();
        turnierIdZeile.newString(I18n.get("ptmonline.info.label.turnier_id"));
        turnierIdZeile.newString(mapping.getTournamentId().orElse(I18n.get("ptmonline.info.wert.nicht_angelegt")));

        RowData baseUrlZeile = rd.addNewRow();
        baseUrlZeile.newString(I18n.get("ptmonline.info.label.baseurl"));
        baseUrlZeile.newString(baseUrl);

        RowData syncZeile = rd.addNewRow();
        syncZeile.newString(I18n.get("ptmonline.info.label.letzte_sync"));
        syncZeile.newString(mapping.getLastSync().map(SYNC_FORMAT::format).orElse(I18n.get("ptmonline.info.wert.noch_nie")));

        RangeHelper.from(sheet, ws.getWorkingSpreadsheetDocument(),
                rd.getRangePosition(Position.from(SPALTE_LABEL, ZEILE_TURNIER_ID))).setDataInRange(rd);
    }

    private static void schreibeEckdaten(WorkingSpreadsheet ws, XSpreadsheet sheet, TournamentMetadataDto metadata)
            throws GenerateException {
        RangeData rd = new RangeData();
        zeile(rd, "ptmonline.info.label.name", metadata.name());
        zeile(rd, "ptmonline.info.label.datum", metadata.date());
        zeile(rd, "ptmonline.info.label.startzeit", metadata.startTime());
        zeile(rd, "ptmonline.info.label.ort", metadata.location());
        zeile(rd, "ptmonline.info.label.typ", metadata.type());
        zeile(rd, "ptmonline.info.label.formation", metadata.formation());
        zeile(rd, "ptmonline.info.label.beschreibung", metadata.description());
        zeile(rd, "ptmonline.info.label.status", metadata.status());
        zeile(rd, "ptmonline.info.label.sichtbarkeit", metadata.visibility());
        zeileInt(rd, "ptmonline.info.label.max_anmeldungen", metadata.maxRegistrations());
        zeile(rd, "ptmonline.info.label.anmeldeschluss", metadata.registrationDeadline());
        zeileInt(rd, "ptmonline.info.label.startgeld_cent", metadata.entryFeeCents());
        zeile(rd, "ptmonline.info.label.kontakt_name", metadata.contactName());
        zeile(rd, "ptmonline.info.label.kontakt_email", metadata.contactEmail());
        zeile(rd, "ptmonline.info.label.kontakt_telefon", metadata.contactPhone());
        zeile(rd, "ptmonline.info.label.interne_notizen", metadata.internalNotes());
        zeileBool(rd, "ptmonline.info.label.teilnehmer_oeffentlich", metadata.participantsPublic());
        zeileBool(rd, "ptmonline.info.label.lizenzpflicht", metadata.licenseRequired());

        RangeHelper.from(sheet, ws.getWorkingSpreadsheetDocument(),
                rd.getRangePosition(Position.from(SPALTE_LABEL, ZEILE_NAME))).setDataInRange(rd);
    }

    private static void zeile(RangeData rd, String labelKey, String wert) {
        RowData zeile = rd.addNewRow();
        zeile.newString(I18n.get(labelKey));
        zeile.newString(wert == null ? "" : wert);
    }

    private static void zeileInt(RangeData rd, String labelKey, int wert) {
        RowData zeile = rd.addNewRow();
        zeile.newString(I18n.get(labelKey));
        zeile.newString(wert == 0 ? "" : Integer.toString(wert));
    }

    private static void zeileBool(RangeData rd, String labelKey, boolean wert) {
        RowData zeile = rd.addNewRow();
        zeile.newString(I18n.get(labelKey));
        zeile.newString(I18n.get(wert ? "ptmonline.info.wert.ja" : "ptmonline.info.wert.nein"));
    }

    private static String text(SheetHelper sh, XSpreadsheet sheet, int zeile) {
        String wert = sh.getTextFromCell(sheet, Position.from(SPALTE_WERT, zeile));
        return wert == null ? "" : wert;
    }

    private static String leer2Null(String wert) {
        return StringUtils.isBlank(wert) ? null : wert;
    }

    private static int ganzzahl(String wert) {
        try {
            return StringUtils.isBlank(wert) ? 0 : Integer.parseInt(wert.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static boolean boolWert(String wert) {
        return I18n.get("ptmonline.info.wert.ja").equalsIgnoreCase(StringUtils.trimToEmpty(wert));
    }

    private static void schreibeMapping(WorkingSpreadsheet ws, XSpreadsheet sheet, PtmOnlineRegistrationMapping mapping)
            throws GenerateException {
        RangeData header = new RangeData();
        RowData headerZeile = header.addNewRow();
        headerZeile.newString(I18n.get("ptmonline.info.spalte.team_nr"));
        headerZeile.newString(I18n.get("ptmonline.info.spalte.online_id"));
        RangeHelper.from(sheet, ws.getWorkingSpreadsheetDocument(),
                header.getRangePosition(Position.from(SPALTE_MAPPING_TEAM_NR, ZEILE_MAPPING_HEADER))).setDataInRange(header);

        RangePosition datenBereich = RangePosition.from(SPALTE_MAPPING_TEAM_NR, ZEILE_MAPPING_ERSTE_DATENZEILE,
                SPALTE_MAPPING_ONLINE_ID, ZEILE_MAPPING_ERSTE_DATENZEILE + MAPPING_MAX_ZEILEN);
        RangeHelper.from(sheet, ws.getWorkingSpreadsheetDocument(), datenBereich).clearRange();

        Map<Integer, String> alleMappings = mapping.getAlleMappings();
        if (alleMappings.isEmpty()) {
            return;
        }
        RangeData daten = new RangeData();
        alleMappings.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(eintrag -> {
            RowData zeile = daten.addNewRow();
            zeile.newInt(eintrag.getKey());
            zeile.newString(eintrag.getValue());
        });
        RangeHelper.from(sheet, ws.getWorkingSpreadsheetDocument(),
                daten.getRangePosition(Position.from(SPALTE_MAPPING_TEAM_NR, ZEILE_MAPPING_ERSTE_DATENZEILE)))
                .setDataInRange(daten);
    }

    private static void formatiere(SheetHelper sh, XSpreadsheet sheet) {
        try {
            XCellRange labelSpalte = sheet.getCellRangeByPosition(SPALTE_LABEL, ZEILE_TURNIER_ID, SPALTE_LABEL, ZEILE_LETZTE_SYNC);
            XPropertySet labelProps = Lo.qi(XPropertySet.class, labelSpalte);
            labelProps.setPropertyValue("CharWeight", FontWeight.BOLD);

            XCellRange eckdatenLabelSpalte = sheet.getCellRangeByPosition(SPALTE_LABEL, ZEILE_NAME, SPALTE_LABEL, ZEILE_LIZENZPFLICHT);
            XPropertySet eckdatenLabelProps = Lo.qi(XPropertySet.class, eckdatenLabelSpalte);
            eckdatenLabelProps.setPropertyValue("CharWeight", FontWeight.BOLD);

            XCellRange mappingHeader = sheet.getCellRangeByPosition(SPALTE_MAPPING_TEAM_NR, ZEILE_MAPPING_HEADER,
                    SPALTE_MAPPING_ONLINE_ID, ZEILE_MAPPING_HEADER);
            XPropertySet headerProps = Lo.qi(XPropertySet.class, mappingHeader);
            headerProps.setPropertyValue("CharWeight", FontWeight.BOLD);
            headerProps.setPropertyValue("CellBackColor", HEADER_HG_FARBE);
        } catch (com.sun.star.uno.Exception e) {
            logger.warn("PTM-Online-Info-Sheet-Formatierung fehlgeschlagen: {}", e.getMessage());
        }
        sh.setColumnWidth(sheet, SPALTE_LABEL, SPALTENBREITE_LABEL);
        sh.setColumnWidth(sheet, SPALTE_WERT, SPALTENBREITE_WERT);
    }
}
