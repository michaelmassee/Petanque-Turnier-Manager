/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.formulex.blattschutz;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.table.CellContentType;
import com.sun.star.table.XCell;

import de.petanqueturniermanager.basesheet.meldeliste.MeldungenSpalte;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.formulex.konfiguration.FormuleXKonfigurationSheet;
import de.petanqueturniermanager.formulex.spielrunde.FormuleXAbstractSpielrundeSheet;
import de.petanqueturniermanager.helper.cellstyle.CellStyleHelper;
import de.petanqueturniermanager.helper.cellstyle.EditierbareZelleHintergrundFarbeGeradeStyle;
import de.petanqueturniermanager.helper.cellstyle.EditierbareZelleHintergrundFarbeUnGeradeStyle;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.EditierbaresZelleFormatHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.blattschutz.IBlattschutzKonfiguration;
import de.petanqueturniermanager.helper.sheet.blattschutz.SheetSchutzInfo;

/**
 * Blattschutz-Konfiguration für das Formule X Turniersystem.
 * <p>
 * Editierbare Bereiche:
 * <ul>
 *   <li><b>Meldeliste:</b> Alle Spalten außer Nr (Spalte 0): Teamname, Spielernamen, SP, Aktiv</li>
 *   <li><b>Spielrunden:</b> Ergebnis A und B (bis zur letzten Datenzeile)</li>
 *   <li><b>Rangliste:</b> vollständig gesperrt</li>
 * </ul>
 */
public class FormuleXBlattschutzKonfiguration implements IBlattschutzKonfiguration {

    private static final Logger LOGGER = LogManager.getLogger(FormuleXBlattschutzKonfiguration.class);
    private static final FormuleXBlattschutzKonfiguration INSTANCE = new FormuleXBlattschutzKonfiguration();

    private static final int MELDELISTE_ERSTE_DATEN_ZEILE = 3;

    private FormuleXBlattschutzKonfiguration() {
    }

    public static FormuleXBlattschutzKonfiguration get() {
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
        SheetMetadataHelper.findeSheet(xDoc, SheetMetadataHelper.SCHLUESSEL_FORMULEX_MELDELISTE).ifPresent(sheet -> {
            try {
                var konfigSheet = new FormuleXKonfigurationSheet(ws);
                int aktivSpalte = berechneAktivSpalte(konfigSheet);
                infos.add(SheetSchutzInfo.mitEditierbarenBereichen(sheet, List.of(
                        RangePosition.from(1, MELDELISTE_ERSTE_DATEN_ZEILE,
                                aktivSpalte, MeldungenSpalte.MAX_ANZ_MELDUNGEN))));
            } catch (GenerateException e) {
                LOGGER.warn("Editierbare Meldeliste-Spalten konnten nicht berechnet werden: {}", e.getMessage(), e);
            }
        });
    }

    private void sammleSpielrundenSchutzInfos(XSpreadsheetDocument xDoc, List<SheetSchutzInfo> infos) {
        var schluessel = SheetMetadataHelper.getSchluesselMitPrefix(xDoc,
                SheetMetadataHelper.SCHLUESSEL_FORMULEX_SPIELRUNDE_PREFIX);
        for (var key : schluessel) {
            SheetMetadataHelper.findeSheet(xDoc, key).ifPresent(sheet ->
                    infos.add(SheetSchutzInfo.mitEditierbarenBereichen(sheet,
                            List.of(berechneSpielrundeErgebnisBereich(sheet)))));
        }
    }

    private void sammleVollGesperrteSheets(XSpreadsheetDocument xDoc, List<SheetSchutzInfo> infos) {
        SheetMetadataHelper.findeSheet(xDoc, SheetMetadataHelper.SCHLUESSEL_FORMULEX_RANGLISTE)
                .ifPresent(sheet -> infos.add(SheetSchutzInfo.vollGesperrt(sheet)));
    }

    private RangePosition berechneSpielrundeErgebnisBereich(com.sun.star.sheet.XSpreadsheet sheet) {
        return RangePosition.from(
                FormuleXAbstractSpielrundeSheet.ERG_TEAM_A_SPALTE,
                FormuleXAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE,
                FormuleXAbstractSpielrundeSheet.ERG_TEAM_B_SPALTE,
                ermittleLetzteSpielrundeZeile(sheet));
    }

    private int ermittleLetzteSpielrundeZeile(com.sun.star.sheet.XSpreadsheet sheet) {
        int letzteZeile = FormuleXAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE;
        try {
            for (int zeile = FormuleXAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE;
                    zeile <= MeldungenSpalte.MAX_ANZ_MELDUNGEN; zeile++) {
                XCell xCell = sheet.getCellByPosition(FormuleXAbstractSpielrundeSheet.TEAM_A_SPALTE, zeile);
                if (CellContentType.EMPTY.equals(xCell.getType())) {
                    break;
                }
                letzteZeile = zeile;
            }
        } catch (Exception e) {
            LOGGER.warn("Letzte Spielrunde-Zeile konnte nicht ermittelt werden: {}", e.getMessage(), e);
        }
        return letzteZeile;
    }

    private int berechneAktivSpalte(FormuleXKonfigurationSheet konfigSheet) throws GenerateException {
        int spaltenProSpieler = konfigSheet.isMeldeListeVereinsnameAnzeigen() ? 3 : 2;
        int ersterOffset = konfigSheet.isMeldeListeTeamnameAnzeigen() ? 2 : 1;
        int letzteDataSpalte = ersterOffset
                + konfigSheet.getMeldeListeFormation().getAnzSpieler() * spaltenProSpieler - 1;
        return letzteDataSpalte + 2; // +1=SP, +2=Aktiv
    }
}
