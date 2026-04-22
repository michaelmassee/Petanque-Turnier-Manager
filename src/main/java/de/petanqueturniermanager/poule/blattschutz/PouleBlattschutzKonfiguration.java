/**
 * Erstellung : 2026 / Michael Massee
 **/

package de.petanqueturniermanager.poule.blattschutz;

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
import de.petanqueturniermanager.ko.KoTurnierbaumSheet;
import de.petanqueturniermanager.poule.konfiguration.PouleKonfigurationSheet;
import de.petanqueturniermanager.poule.vorrunde.AbstractPouleVorrundeSheet;

/**
 * Blattschutz-Konfiguration für das Poule-A/B-Turniersystem.
 * <p>
 * Editierbare Bereiche:
 * <ul>
 *   <li><b>Meldeliste:</b> Alle Spalten außer Nr (Spalte 0): Teamname, Spielernamen, SP, Aktiv</li>
 *   <li><b>Vorrunde:</b> Ergebnis-Spalten A und B (SPALTE_ERG_A bis SPALTE_ERG_B)</li>
 *   <li><b>KO-Sheets:</b> Nur die konkreten Score-Zellen (aus {@code KoTurnierbaumSheet} gespeichert)</li>
 *   <li><b>Spielplan-Sheets, Rangliste:</b> vollständig gesperrt</li>
 * </ul>
 */
public class PouleBlattschutzKonfiguration implements IBlattschutzKonfiguration {

    private static final Logger logger = LogManager.getLogger(PouleBlattschutzKonfiguration.class);
    private static final PouleBlattschutzKonfiguration INSTANCE = new PouleBlattschutzKonfiguration();

    /** Erste Daten-Zeile der Poule-Meldeliste (3 Header-Zeilen: 0, 1, 2 → Daten ab 3). */
    private static final int MELDELISTE_ERSTE_DATEN_ZEILE = 3;

    private PouleBlattschutzKonfiguration() {
    }

    public static PouleBlattschutzKonfiguration get() {
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
        var konfigSheet = new PouleKonfigurationSheet(ws);
        var infos = new ArrayList<SheetSchutzInfo>();

        sammleMeldelisteSchutzInfo(xDoc, konfigSheet, infos);
        sammleVorrundeSchutzInfo(xDoc, infos);
        sammleKoSchutzInfos(xDoc, infos);
        sammleVollGesperrteSheets(xDoc, infos);

        return infos;
    }

    private void sammleMeldelisteSchutzInfo(XSpreadsheetDocument xDoc, PouleKonfigurationSheet konfigSheet,
            List<SheetSchutzInfo> infos) {
        SheetMetadataHelper.findeSheet(xDoc, SheetMetadataHelper.SCHLUESSEL_POULE_MELDELISTE).ifPresent(sheet -> {
            try {
                int aktivSpalte = berechneAktivSpalte(konfigSheet);
                infos.add(SheetSchutzInfo.mitEditierbarenBereichen(sheet, List.of(
                        RangePosition.from(1, MELDELISTE_ERSTE_DATEN_ZEILE,
                                aktivSpalte, MeldungenSpalte.MAX_ANZ_MELDUNGEN))));
            } catch (GenerateException e) {
                logger.warn("Editierbare Meldeliste-Spalten konnten nicht berechnet werden: {}", e.getMessage(), e);
            }
        });
    }

    private void sammleVorrundeSchutzInfo(XSpreadsheetDocument xDoc, List<SheetSchutzInfo> infos) {
        SheetMetadataHelper.findeSheet(xDoc, SheetMetadataHelper.SCHLUESSEL_POULE_VORRUNDE).ifPresent(sheet -> {
            int letzteZeile = ermittleLetzteVorrundeZeile(sheet);
            infos.add(SheetSchutzInfo.mitEditierbarenBereichen(sheet, List.of(
                    RangePosition.from(AbstractPouleVorrundeSheet.SPALTE_ERG_A,
                            AbstractPouleVorrundeSheet.ERSTE_DATEN_ZEILE,
                            AbstractPouleVorrundeSheet.SPALTE_ERG_B,
                            letzteZeile))));
        });
    }

    private int ermittleLetzteVorrundeZeile(XSpreadsheet sheet) {
        int letzteZeile = AbstractPouleVorrundeSheet.ERSTE_DATEN_ZEILE;
        try {
            for (int zeile = AbstractPouleVorrundeSheet.ERSTE_DATEN_ZEILE;
                    zeile <= MeldungenSpalte.MAX_ANZ_MELDUNGEN; zeile++) {
                XCell xCell = sheet.getCellByPosition(AbstractPouleVorrundeSheet.SPALTE_TEAM_A_NR, zeile);
                if (CellContentType.EMPTY.equals(xCell.getType())) {
                    break;
                }
                letzteZeile = zeile;
            }
        } catch (Exception e) {
            logger.warn("Letzte Vorrunde-Zeile konnte nicht ermittelt werden: {}", e.getMessage(), e);
        }
        return letzteZeile;
    }

    private void sammleKoSchutzInfos(XSpreadsheetDocument xDoc, List<SheetSchutzInfo> infos) {
        var schluessel = SheetMetadataHelper.getSchluesselMitPrefix(xDoc,
                SheetMetadataHelper.SCHLUESSEL_POULE_KO_PREFIX);
        for (var koKey : schluessel) {
            SheetMetadataHelper.findeSheet(xDoc, koKey).ifPresent(sheet -> {
                var scoreKey = SheetMetadataHelper.scoreSchluessel(koKey);
                var encoded = SheetMetadataHelper.leseScoreText(xDoc, scoreKey);
                var editierbareBereiche = (encoded != null)
                        ? KoTurnierbaumSheet.decodeScoreBereiche(encoded)
                        : List.<RangePosition>of();
                if (editierbareBereiche.isEmpty()) {
                    infos.add(SheetSchutzInfo.vollGesperrt(sheet));
                } else {
                    infos.add(SheetSchutzInfo.mitEditierbarenBereichen(sheet, editierbareBereiche));
                }
            });
        }
    }

    private void sammleVollGesperrteSheets(XSpreadsheetDocument xDoc, List<SheetSchutzInfo> infos) {
        SheetMetadataHelper.findeSheet(xDoc, SheetMetadataHelper.SCHLUESSEL_POULE_VORRUNDEN_RANGLISTE)
                .ifPresent(sheet -> infos.add(SheetSchutzInfo.vollGesperrt(sheet)));

        var spielplanSchluessel = SheetMetadataHelper.getSchluesselMitPrefix(xDoc,
                SheetMetadataHelper.SCHLUESSEL_POULE_SPIELPLAN_PREFIX);
        for (var key : spielplanSchluessel) {
            SheetMetadataHelper.findeSheet(xDoc, key)
                    .ifPresent(sheet -> infos.add(SheetSchutzInfo.vollGesperrt(sheet)));
        }
    }

    /**
     * Berechnet die Aktiv-Spalte dynamisch aus der Turnierkonfiguration.
     * Layout: 0=Nr, 1=[Teamname], x..y=Spieler(n), letzte+1=SP, letzte+2=Aktiv
     */
    private int berechneAktivSpalte(PouleKonfigurationSheet konfigSheet) throws GenerateException {
        int spaltenProSpieler = konfigSheet.isMeldeListeVereinsnameAnzeigen() ? 3 : 2;
        int ersterOffset = konfigSheet.isMeldeListeTeamnameAnzeigen() ? 2 : 1;
        int letzteDataSpalte = ersterOffset + konfigSheet.getMeldeListeFormation().getAnzSpieler()
                * spaltenProSpieler - 1;
        return letzteDataSpalte + 2; // +1=SP, +2=Aktiv
    }
}
