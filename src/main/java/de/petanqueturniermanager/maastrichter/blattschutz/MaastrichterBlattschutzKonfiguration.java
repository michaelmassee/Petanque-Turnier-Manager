/**
 * Erstellung : 2026 / Michael Massee
 **/

package de.petanqueturniermanager.maastrichter.blattschutz;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;

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
import de.petanqueturniermanager.maastrichter.konfiguration.MaastrichterKonfigurationSheet;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerAbstractSpielrundeSheet;

/**
 * Blattschutz-Konfiguration für das Maastrichter Turniersystem.
 * <p>
 * Editierbare Bereiche:
 * <ul>
 *   <li><b>Meldeliste:</b> Alle Spalten außer Nr (Spalte 0): Teamname, Spielernamen, SP, Aktiv</li>
 *   <li><b>Vorrunden:</b> Ergebnis A und B (bis zur letzten Datenzeile)</li>
 *   <li><b>Finalrunden:</b> Nur die konkreten Score-Zellen (aus {@code KoTurnierbaumSheet} gespeichert)</li>
 *   <li><b>Vorrunden-Rangliste:</b> vollständig gesperrt</li>
 * </ul>
 */
public class MaastrichterBlattschutzKonfiguration implements IBlattschutzKonfiguration {

    private static final Logger logger = LogManager.getLogger(MaastrichterBlattschutzKonfiguration.class);
    private static final MaastrichterBlattschutzKonfiguration INSTANCE = new MaastrichterBlattschutzKonfiguration();

    /** Erste Daten-Zeile der Maastrichter Meldeliste (3 Header-Zeilen). */
    private static final int MELDELISTE_ERSTE_DATEN_ZEILE = 3;

    private MaastrichterBlattschutzKonfiguration() {
    }

    public static MaastrichterBlattschutzKonfiguration get() {
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
        sammleVorrundenUndRanglisteSchutzInfos(xDoc, infos);
        sammleFinalrundenSchutzInfos(xDoc, infos);

        return infos;
    }

    private void sammleMeldelisteSchutzInfo(XSpreadsheetDocument xDoc, WorkingSpreadsheet ws,
            List<SheetSchutzInfo> infos) {
        SheetMetadataHelper.findeSheet(xDoc, SheetMetadataHelper.SCHLUESSEL_MAASTRICHTER_MELDELISTE).ifPresent(sheet -> {
            try {
                var konfigSheet = new MaastrichterKonfigurationSheet(ws);
                int aktivSpalte = berechneAktivSpalte(konfigSheet);
                infos.add(SheetSchutzInfo.mitEditierbarenBereichen(sheet, List.of(
                        RangePosition.from(1, MELDELISTE_ERSTE_DATEN_ZEILE,
                                aktivSpalte, MeldungenSpalte.MAX_ANZ_MELDUNGEN))));
            } catch (GenerateException e) {
                logger.warn("Editierbare Meldeliste-Spalten konnten nicht berechnet werden: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * Sammelt Schutzinfos für alle Vorrunden und die Vorrunden-Rangliste.
     * Der Schlüssel der Rangliste ist der Prefix selbst (endet nicht mit "__"),
     * Vorrunden haben numerische Suffixe (enden mit "__").
     */
    private void sammleVorrundenUndRanglisteSchutzInfos(XSpreadsheetDocument xDoc, List<SheetSchutzInfo> infos) {
        var schluessel = SheetMetadataHelper.getSchluesselMitPrefix(xDoc,
                SheetMetadataHelper.SCHLUESSEL_MAASTRICHTER_VORRUNDE_PREFIX);
        for (var key : schluessel) {
            SheetMetadataHelper.findeSheet(xDoc, key).ifPresent(sheet -> {
                if (key.equals(SheetMetadataHelper.SCHLUESSEL_MAASTRICHTER_VORRUNDE_PREFIX)) {
                    // Vorrunden-Rangliste: kein Schlüssel-Suffix → vollständig gesperrt
                    infos.add(SheetSchutzInfo.vollGesperrt(sheet));
                } else {
                    // Vorrunde: Ergebnis-Spalten editierbar
                    infos.add(SheetSchutzInfo.mitEditierbarenBereichen(sheet,
                            List.of(berechneVorrundeErgebnisBereich(sheet))));
                }
            });
        }
    }

    private void sammleFinalrundenSchutzInfos(XSpreadsheetDocument xDoc, List<SheetSchutzInfo> infos) {
        var schluessel = SheetMetadataHelper.getSchluesselMitPrefix(xDoc,
                SheetMetadataHelper.SCHLUESSEL_MAASTRICHTER_FINALRUNDE_PREFIX);
        for (var finalrundeKey : schluessel) {
            SheetMetadataHelper.findeSheet(xDoc, finalrundeKey).ifPresent(sheet -> {
                var scoreKey = SheetMetadataHelper.scoreSchluessel(finalrundeKey);
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

    /**
     * Berechnet den editierbaren Ergebnis-Bereich einer Vorrunde.
     * Gleiche Spaltenstruktur wie Schweizer Spielrunden.
     */
    private RangePosition berechneVorrundeErgebnisBereich(XSpreadsheet sheet) {
        return RangePosition.from(
                SchweizerAbstractSpielrundeSheet.ERG_TEAM_A_SPALTE,
                SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE,
                SchweizerAbstractSpielrundeSheet.ERG_TEAM_B_SPALTE,
                ermittleLetzteVorrundeZeile(sheet));
    }

    private int ermittleLetzteVorrundeZeile(XSpreadsheet sheet) {
        int letzteZeile = SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE;
        try {
            for (int zeile = SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE;
                    zeile <= MeldungenSpalte.MAX_ANZ_MELDUNGEN; zeile++) {
                var xCell = sheet.getCellByPosition(SchweizerAbstractSpielrundeSheet.TEAM_A_SPALTE, zeile);
                if (com.sun.star.table.CellContentType.EMPTY.equals(xCell.getType())) {
                    break;
                }
                letzteZeile = zeile;
            }
        } catch (Exception e) {
            logger.warn("Letzte Vorrunde-Zeile konnte nicht ermittelt werden: {}", e.getMessage(), e);
        }
        return letzteZeile;
    }

    /**
     * Berechnet die Aktiv-Spalte dynamisch aus der Turnierkonfiguration.
     * Layout: 0=Nr, 1=[Teamname], x..y=Spieler(n), letzte+1=SP, letzte+2=Aktiv
     */
    private int berechneAktivSpalte(MaastrichterKonfigurationSheet konfigSheet) throws GenerateException {
        int spaltenProSpieler = konfigSheet.isMeldeListeVereinsnameAnzeigen() ? 3 : 2;
        int ersterOffset = konfigSheet.isMeldeListeTeamnameAnzeigen() ? 2 : 1;
        int letzteDataSpalte = ersterOffset + konfigSheet.getMeldeListeFormation().getAnzSpieler() * spaltenProSpieler - 1;
        return letzteDataSpalte + 2; // +1=SP, +2=Aktiv
    }
}
