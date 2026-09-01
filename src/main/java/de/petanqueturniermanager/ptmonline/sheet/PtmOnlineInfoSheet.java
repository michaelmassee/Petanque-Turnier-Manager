/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.sheet;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.FontWeight;
import com.sun.star.beans.XPropertySet;
import com.sun.star.sheet.XSpreadsheet;
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
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.ptmonline.PtmOnlineRegistrationMapping;

/**
 * Schreibt die bisher nur unsichtbar in DocumentProperties gehaltenen PTM-Online-Informationen
 * (Online-Turnier-ID, Base-URL, letzte Synchronisation, Team-Nr/Online-Registrierungs-ID-Zuordnung)
 * sichtbar in ein Sheet. Wird nach jeder PTM-Online-Aktion (Turnier anlegen, Anmeldungen
 * importieren, Ergebnisse exportieren) neu geschrieben; existiert das Sheet noch nicht, wird es
 * angelegt. Der Nutzer wird dabei bewusst nicht auf dieses Sheet umgeschaltet — die Aktualisierung
 * laeuft im Hintergrund zur laufenden Turnierarbeit.
 */
public final class PtmOnlineInfoSheet {

    private static final Logger logger = LogManager.getLogger(PtmOnlineInfoSheet.class);

    private static final int SPALTE_LABEL = 0;
    private static final int SPALTE_WERT = 1;
    private static final int ZEILE_TURNIER_ID = 0;
    private static final int ZEILE_LETZTE_SYNC = 2;

    private static final int SPALTE_MAPPING_TEAM_NR = 0;
    private static final int SPALTE_MAPPING_ONLINE_ID = 1;
    private static final int ZEILE_MAPPING_HEADER = 4;
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
     * Legt das Sheet bei Bedarf an und schreibt Stammdaten sowie Mapping-Tabelle neu.
     */
    public static void aktualisiere(WorkingSpreadsheet ws, String baseUrl,
            PtmOnlineRegistrationMapping mapping) throws GenerateException {
        SheetHelper sh = new SheetHelper(ws);
        String sheetName = SheetNamen.ptmOnline();
        XSpreadsheet sheet = sh.findByName(sheetName);
        boolean neuAngelegt = sheet == null;
        if (neuAngelegt) {
            sheet = sh.newIfNotExist(sheetName, anhaengePosition(sh), TAB_FARBE);
        }
        if (sheet == null) {
            throw new GenerateException(I18n.get("error.tabelle.nicht.vorhanden", sheetName));
        }

        schreibeStammdaten(ws, sheet, baseUrl, mapping);
        schreibeMapping(ws, sheet, mapping);
        if (neuAngelegt) {
            formatiere(sh, sheet);
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

    private static void schreibeStammdaten(WorkingSpreadsheet ws, XSpreadsheet sheet, String baseUrl,
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
