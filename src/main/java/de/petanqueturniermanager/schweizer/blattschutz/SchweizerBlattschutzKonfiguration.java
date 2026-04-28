/**
 * Erstellung : 22.04.2026 / Michael Massee
 **/

package de.petanqueturniermanager.schweizer.blattschutz;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.table.CellContentType;
import com.sun.star.table.XCell;

import de.petanqueturniermanager.basesheet.meldeliste.MeldungenSpalte;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.cellstyle.CellStyleHelper;
import de.petanqueturniermanager.helper.cellstyle.EditierbareZelleHintergrundFarbeGeradeStyle;
import de.petanqueturniermanager.helper.cellstyle.EditierbareZelleHintergrundFarbeUnGeradeStyle;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.EditierbaresZelleFormatHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.blattschutz.IBlattschutzKonfiguration;
import de.petanqueturniermanager.helper.sheet.blattschutz.SheetSchutzInfo;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerKonfigurationSheet;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerAbstractSpielrundeSheet;

/**
 * Blattschutz-Konfiguration für das Schweizer Turniersystem.
 * <p>
 * Editierbare Bereiche:
 * <ul>
 *   <li><b>Meldeliste:</b> Alle Spalten außer Nr (Spalte 0): Teamname, Spielernamen, SP, Aktiv</li>
 *   <li><b>Spielrunden:</b> Ergebnis A und B (bis zur letzten Datenzeile)</li>
 *   <li><b>Rangliste:</b> vollständig gesperrt</li>
 * </ul>
 */
public class SchweizerBlattschutzKonfiguration implements IBlattschutzKonfiguration {

    private static final Logger logger = LogManager.getLogger(SchweizerBlattschutzKonfiguration.class);
    private static final SchweizerBlattschutzKonfiguration INSTANCE = new SchweizerBlattschutzKonfiguration();

    /** Erste Daten-Zeile der Schweizer Meldeliste (3 Header-Zeilen: 0, 1, 2 → Daten ab 3). */
    private static final int MELDELISTE_ERSTE_DATEN_ZEILE = 3;

    private SchweizerBlattschutzKonfiguration() {
    }

    public static SchweizerBlattschutzKonfiguration get() {
        return INSTANCE;
    }

    @Override
    public void zelleStylesAktualisieren(WorkingSpreadsheet ws) {
        var doc = ws.getWorkingSpreadsheetDocument();
        CellStyleHelper.from(doc,
                new EditierbareZelleHintergrundFarbeGeradeStyle(
                        EditierbaresZelleFormatHelper.EDITIERBAR_GERADE_FARBE)).apply();
        CellStyleHelper.from(doc,
                new EditierbareZelleHintergrundFarbeUnGeradeStyle(
                        EditierbaresZelleFormatHelper.EDITIERBAR_UNGERADE_FARBE)).apply();
    }

    @Override
    public List<SheetSchutzInfo> berechneSchutzInfos(WorkingSpreadsheet ws) {
        var xDoc = ws.getWorkingSpreadsheetDocument();
        var infos = new ArrayList<SheetSchutzInfo>();

        sammleMeldelisteSchutzInfo(xDoc, ws, infos);
        sammleSpielrundenSchutzInfos(xDoc, infos);
        sammleVollGesperrteSheets(xDoc, infos);

        return infos;
    }

    private void sammleMeldelisteSchutzInfo(XSpreadsheetDocument xDoc, WorkingSpreadsheet ws,
            List<SheetSchutzInfo> infos) {
        SheetMetadataHelper.findeSheet(xDoc, SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_MELDELISTE).ifPresent(sheet -> {
            try {
                var konfigSheet = new SchweizerKonfigurationSheet(ws);
                int aktivSpalte = berechneAktivSpalte(konfigSheet);
                infos.add(SheetSchutzInfo.mitEditierbarenBereichen(sheet, List.of(
                        RangePosition.from(1, MELDELISTE_ERSTE_DATEN_ZEILE,
                                aktivSpalte, MeldungenSpalte.MAX_ANZ_MELDUNGEN))));
            } catch (GenerateException e) {
                logger.warn("Editierbare Meldeliste-Spalten konnten nicht berechnet werden: {}", e.getMessage(), e);
            }
        });
    }

    private void sammleSpielrundenSchutzInfos(XSpreadsheetDocument xDoc, List<SheetSchutzInfo> infos) {
        var schluessel = SheetMetadataHelper.getSchluesselMitPrefix(xDoc,
                SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_SPIELRUNDE_PREFIX);
        for (var key : schluessel) {
            SheetMetadataHelper.findeSheet(xDoc, key).ifPresent(sheet ->
                    infos.add(SheetSchutzInfo.mitEditierbarenBereichen(sheet,
                            List.of(berechneSpielrundeErgebnisBereich(sheet)))));
        }
    }

    private void sammleVollGesperrteSheets(XSpreadsheetDocument xDoc, List<SheetSchutzInfo> infos) {
        SheetMetadataHelper.findeSheet(xDoc, SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_RANGLISTE)
                .ifPresent(sheet -> infos.add(SheetSchutzInfo.vollGesperrt(sheet)));
        SheetMetadataHelper.findeSheet(xDoc, SheetMetadataHelper.SCHLUESSEL_TEILNEHMER)
                .ifPresent(sheet -> infos.add(SheetSchutzInfo.vollGesperrt(sheet)));
    }

    /**
     * Berechnet den editierbaren Ergebnis-Bereich einer Spielrunde.
     * Ergebnis A = Spalte {@code ERG_TEAM_A_SPALTE}, Ergebnis B = {@code ERG_TEAM_B_SPALTE}.
     * Die letzte Zeile wird aus dem Sheet-Inhalt ermittelt.
     */
    private RangePosition berechneSpielrundeErgebnisBereich(XSpreadsheet sheet) {
        return RangePosition.from(
                SchweizerAbstractSpielrundeSheet.ERG_TEAM_A_SPALTE,
                SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE,
                SchweizerAbstractSpielrundeSheet.ERG_TEAM_B_SPALTE,
                ermittleLetzteSpielrundeZeile(sheet));
    }

    private int ermittleLetzteSpielrundeZeile(XSpreadsheet sheet) {
        int letzteZeile = SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE;
        try {
            for (int zeile = SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE;
                    zeile <= MeldungenSpalte.MAX_ANZ_MELDUNGEN; zeile++) {
                XCell xCell = sheet.getCellByPosition(SchweizerAbstractSpielrundeSheet.TEAM_A_SPALTE, zeile);
                if (CellContentType.EMPTY.equals(xCell.getType())) {
                    break;
                }
                letzteZeile = zeile;
            }
        } catch (Exception e) {
            logger.warn("Letzte Spielrunde-Zeile konnte nicht ermittelt werden: {}", e.getMessage(), e);
        }
        return letzteZeile;
    }

    /**
     * Berechnet die Aktiv-Spalte dynamisch aus der Turnierkonfiguration.
     * Layout: 0=Nr, 1=[Teamname], x..y=Spieler(n), letzte+1=SP, letzte+2=Aktiv
     */
    private int berechneAktivSpalte(SchweizerKonfigurationSheet konfigSheet) throws GenerateException {
        int spaltenProSpieler = konfigSheet.isMeldeListeVereinsnameAnzeigen() ? 3 : 2;
        int ersterOffset = konfigSheet.isMeldeListeTeamnameAnzeigen() ? 2 : 1;
        int letzteDataSpalte = ersterOffset + konfigSheet.getMeldeListeFormation().getAnzSpieler() * spaltenProSpieler - 1;
        return letzteDataSpalte + 2; // +1=SP, +2=Aktiv
    }
}
