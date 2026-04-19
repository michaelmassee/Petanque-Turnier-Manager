/**
 * Erstellung : 19.04.2026 / Michael Massee
 **/

package de.petanqueturniermanager.liga.blattschutz;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.table.CellContentType;
import com.sun.star.table.XCell;

import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeKonstanten;
import de.petanqueturniermanager.basesheet.meldeliste.MeldungenSpalte;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.cellstyle.CellStyleHelper;
import de.petanqueturniermanager.helper.cellstyle.EditierbareZelleHintergrundFarbeGeradeStyle;
import de.petanqueturniermanager.helper.cellstyle.EditierbareZelleHintergrundFarbeUnGeradeStyle;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.EditierbaresZelleFormatHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.blattschutz.IBlattschutzKonfiguration;
import de.petanqueturniermanager.helper.sheet.blattschutz.SheetSchutzInfo;
import de.petanqueturniermanager.liga.spielplan.LigaSpielPlanSheet;

/**
 * Blattschutz-Konfiguration für das Liga-Turniersystem.
 * <p>
 * Editierbare Bereiche:
 * <ul>
 *   <li><b>Meldeliste:</b> Name-Spalte (B)</li>
 *   <li><b>Spielplan:</b> Datum/Uhrzeit/Ort und Ergebnis-Spalten</li>
 *   <li><b>Rangliste, Direktvergleich:</b> vollständig gesperrt</li>
 * </ul>
 */
public class LigaBlattschutzKonfiguration implements IBlattschutzKonfiguration, MeldeListeKonstanten {

    private static final Logger logger = LogManager.getLogger(LigaBlattschutzKonfiguration.class);
    private static final LigaBlattschutzKonfiguration INSTANCE = new LigaBlattschutzKonfiguration();

    private LigaBlattschutzKonfiguration() {
    }

    public static LigaBlattschutzKonfiguration get() {
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

        sammleMeldelisteSchutzInfo(xDoc, infos);
        sammleSpielplanSchutzInfo(xDoc, infos);
        sammleVollGesperrteSheets(xDoc, infos);

        return infos;
    }

    private void sammleMeldelisteSchutzInfo(XSpreadsheetDocument xDoc, List<SheetSchutzInfo> infos) {
        SheetMetadataHelper.findeSheet(xDoc, SheetMetadataHelper.SCHLUESSEL_LIGA_MELDELISTE).ifPresent(sheet ->
                infos.add(SheetSchutzInfo.mitEditierbarenBereichen(sheet, List.of(
                        RangePosition.from(SPIELER_NR_SPALTE + 1, ERSTE_DATEN_ZEILE,
                                SPIELER_NR_SPALTE + 1, MeldungenSpalte.MAX_ANZ_MELDUNGEN)))));
    }

    private void sammleSpielplanSchutzInfo(XSpreadsheetDocument xDoc, List<SheetSchutzInfo> infos) {
        SheetMetadataHelper.findeSheet(xDoc, SheetMetadataHelper.SCHLUESSEL_LIGA_SPIELPLAN).ifPresent(sheet -> {
            int ersteDatenZeile = LigaSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE;
            int letzteZeile = ermittleLetzteSpielplanZeile(sheet);
            var bereiche = List.of(
                    RangePosition.from(LigaSpielPlanSheet.DATUM_SPALTE, ersteDatenZeile,
                            LigaSpielPlanSheet.ORT_SPALTE, letzteZeile),
                    RangePosition.from(LigaSpielPlanSheet.PUNKTE_A_SPALTE, ersteDatenZeile,
                            LigaSpielPlanSheet.SPIELPNKT_B_SPALTE, letzteZeile));
            infos.add(SheetSchutzInfo.mitEditierbarenBereichen(sheet, bereiche));
        });
    }

    private int ermittleLetzteSpielplanZeile(XSpreadsheet sheet) {
        int letzteZeile = LigaSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE;
        try {
            for (int zeile = LigaSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE;
                    zeile <= MeldungenSpalte.MAX_ANZ_MELDUNGEN; zeile++) {
                XCell xCell = sheet.getCellByPosition(LigaSpielPlanSheet.SPIEL_NR_SPALTE, zeile);
                if (CellContentType.EMPTY.equals(xCell.getType())) {
                    break;
                }
                letzteZeile = zeile;
            }
        } catch (Exception e) {
            logger.warn("Letzte Spielplan-Zeile konnte nicht ermittelt werden: {}", e.getMessage(), e);
        }
        return letzteZeile;
    }

    private void sammleVollGesperrteSheets(XSpreadsheetDocument xDoc, List<SheetSchutzInfo> infos) {
        SheetMetadataHelper.findeSheet(xDoc, SheetMetadataHelper.SCHLUESSEL_LIGA_RANGLISTE)
                .ifPresent(sheet -> infos.add(SheetSchutzInfo.vollGesperrt(sheet)));
        SheetMetadataHelper.findeSheet(xDoc, SheetMetadataHelper.SCHLUESSEL_LIGA_DIREKTVERGLEICH)
                .ifPresent(sheet -> infos.add(SheetSchutzInfo.vollGesperrt(sheet)));
    }
}
