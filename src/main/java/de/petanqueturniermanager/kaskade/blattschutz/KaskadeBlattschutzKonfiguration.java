/**
 * Erstellung : 2026 / Michael Massee
 **/

package de.petanqueturniermanager.kaskade.blattschutz;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.table.CellContentType;

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
import de.petanqueturniermanager.kaskade.konfiguration.KaskadeKonfigurationSheet;
import de.petanqueturniermanager.kaskade.spielrunde.KaskadeSpielrundeSheet;
import de.petanqueturniermanager.ko.KoTurnierbaumSheet;

/**
 * Blattschutz-Konfiguration für das Kaskaden-KO-Turniersystem.
 * <p>
 * Editierbare Bereiche:
 * <ul>
 *   <li><b>Meldeliste:</b> Alle Spalten außer Nr (Spalte 0): Teamname, Spielernamen, SP, Aktiv</li>
 *   <li><b>Kaskaden-Runden:</b> Ergebnis-Spalten A und B (bis zur letzten Datenzeile)</li>
 *   <li><b>Kaskaden-Felder (KO-Brackets):</b> Nur die gespeicherten Score-Zellen</li>
 *   <li><b>Gruppenrangliste:</b> vollständig gesperrt</li>
 * </ul>
 */
public class KaskadeBlattschutzKonfiguration implements IBlattschutzKonfiguration {

    private static final Logger logger = LogManager.getLogger(KaskadeBlattschutzKonfiguration.class);
    private static final KaskadeBlattschutzKonfiguration INSTANCE = new KaskadeBlattschutzKonfiguration();

    /** Erste Daten-Zeile der Kaskaden-Meldeliste (3 Header-Zeilen: 0, 1, 2 → Daten ab 3). */
    private static final int MELDELISTE_ERSTE_DATEN_ZEILE = 3;

    private KaskadeBlattschutzKonfiguration() {
    }

    public static KaskadeBlattschutzKonfiguration get() {
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
        sammleKaskadenRundenSchutzInfos(xDoc, infos);
        sammleKaskadenFelderSchutzInfos(xDoc, infos);
        sammleGruppenRanglisteSchutzInfo(xDoc, infos);

        return infos;
    }

    private void sammleMeldelisteSchutzInfo(XSpreadsheetDocument xDoc, WorkingSpreadsheet ws,
            List<SheetSchutzInfo> infos) {
        SheetMetadataHelper.findeSheet(xDoc, SheetMetadataHelper.SCHLUESSEL_KASKADE_MELDELISTE).ifPresent(sheet -> {
            try {
                var konfigSheet = new KaskadeKonfigurationSheet(ws);
                int aktivSpalte = berechneAktivSpalte(konfigSheet);
                infos.add(SheetSchutzInfo.mitEditierbarenBereichen(sheet, List.of(
                        RangePosition.from(1, MELDELISTE_ERSTE_DATEN_ZEILE,
                                aktivSpalte, MeldungenSpalte.MAX_ANZ_MELDUNGEN))));
            } catch (GenerateException e) {
                logger.warn("Editierbare Meldeliste-Spalten konnten nicht berechnet werden: {}", e.getMessage(), e);
            }
        });
    }

    private void sammleKaskadenRundenSchutzInfos(XSpreadsheetDocument xDoc, List<SheetSchutzInfo> infos) {
        var schluessel = SheetMetadataHelper.getSchluesselMitPrefix(xDoc,
                SheetMetadataHelper.SCHLUESSEL_KASKADE_RUNDE_PREFIX);
        for (var key : schluessel) {
            SheetMetadataHelper.findeSheet(xDoc, key).ifPresent(sheet ->
                    infos.add(SheetSchutzInfo.mitEditierbarenBereichen(sheet,
                            List.of(berechneRundeErgebnisBereich(sheet)))));
        }
    }

    private RangePosition berechneRundeErgebnisBereich(XSpreadsheet sheet) {
        return RangePosition.from(
                KaskadeSpielrundeSheet.ERG_TEAM_A_SPALTE,
                KaskadeSpielrundeSheet.ERSTE_DATEN_ZEILE,
                KaskadeSpielrundeSheet.ERG_TEAM_B_SPALTE,
                ermittleLetzteRundeZeile(sheet));
    }

    private int ermittleLetzteRundeZeile(XSpreadsheet sheet) {
        int letzteZeile = KaskadeSpielrundeSheet.ERSTE_DATEN_ZEILE;
        try {
            for (int zeile = KaskadeSpielrundeSheet.ERSTE_DATEN_ZEILE;
                    zeile <= MeldungenSpalte.MAX_ANZ_MELDUNGEN; zeile++) {
                var xCell = sheet.getCellByPosition(KaskadeSpielrundeSheet.TEAM_A_SPALTE, zeile);
                if (CellContentType.EMPTY.equals(xCell.getType())) {
                    break;
                }
                letzteZeile = zeile;
            }
        } catch (Exception e) {
            logger.warn("Letzte Kaskadenrunden-Zeile konnte nicht ermittelt werden: {}", e.getMessage(), e);
        }
        return letzteZeile;
    }

    private void sammleKaskadenFelderSchutzInfos(XSpreadsheetDocument xDoc, List<SheetSchutzInfo> infos) {
        var schluessel = SheetMetadataHelper.getSchluesselMitPrefix(xDoc,
                SheetMetadataHelper.SCHLUESSEL_KASKADE_FELD_PREFIX);
        for (var feldKey : schluessel) {
            SheetMetadataHelper.findeSheet(xDoc, feldKey).ifPresent(sheet -> {
                var scoreKey = SheetMetadataHelper.scoreSchluessel(feldKey);
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

    private void sammleGruppenRanglisteSchutzInfo(XSpreadsheetDocument xDoc, List<SheetSchutzInfo> infos) {
        SheetMetadataHelper.findeSheet(xDoc, SheetMetadataHelper.SCHLUESSEL_KASKADE_GRUPPENRANGLISTE)
                .ifPresent(sheet -> infos.add(SheetSchutzInfo.vollGesperrt(sheet)));
    }

    /**
     * Berechnet die Aktiv-Spalte dynamisch aus der Turnierkonfiguration.
     * Layout: 0=Nr, 1=[Teamname], x..y=Spieler(n), letzte+1=SP, letzte+2=Aktiv
     */
    private int berechneAktivSpalte(KaskadeKonfigurationSheet konfigSheet) throws GenerateException {
        int spaltenProSpieler = konfigSheet.isMeldeListeVereinsnameAnzeigen() ? 3 : 2;
        int ersterOffset = konfigSheet.isMeldeListeTeamnameAnzeigen() ? 2 : 1;
        int letzteDataSpalte = ersterOffset + konfigSheet.getMeldeListeFormation().getAnzSpieler() * spaltenProSpieler - 1;
        return letzteDataSpalte + 2; // +1=SP, +2=Aktiv
    }
}
