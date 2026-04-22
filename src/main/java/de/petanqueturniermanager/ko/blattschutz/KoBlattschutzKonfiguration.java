/**
 * Erstellung : 2026 / Michael Massee
 **/

package de.petanqueturniermanager.ko.blattschutz;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.text.XText;
import com.sun.star.uno.UnoRuntime;

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
import de.petanqueturniermanager.ko.konfiguration.KoKonfigurationSheet;

/**
 * Blattschutz-Konfiguration für das K.-O.-Turniersystem.
 * <p>
 * Editierbare Bereiche:
 * <ul>
 *   <li><b>Meldeliste:</b> Alle Spalten außer Nr (Spalte 0): Teamname, Spielernamen, RNG, Aktiv</li>
 *   <li><b>Turnierbaum-Sheets:</b> Score-Spalten aller Runden (inkl. Cadrage)</li>
 * </ul>
 */
public class KoBlattschutzKonfiguration implements IBlattschutzKonfiguration {

    private static final Logger logger = LogManager.getLogger(KoBlattschutzKonfiguration.class);
    private static final KoBlattschutzKonfiguration INSTANCE = new KoBlattschutzKonfiguration();

    /** Erste Daten-Zeile der K.-O.-Meldeliste (3 Header-Zeilen: 0, 1, 2 → Daten ab 3). */
    private static final int MELDELISTE_ERSTE_DATEN_ZEILE = 3;

    /** Erste Daten-Zeile im Turnierbaum (2 Header-Zeilen: 0, 1 → Daten ab 2). */
    private static final int TURNIERBAUM_ERSTE_ZEILE = 2;

    /** Großzügige Zeilengrenze für einen KO-Bracket-Sheet (64 Teams × 3 Zeilen + Puffer). */
    private static final int TURNIERBAUM_MAX_ZEILE = 250;

    /** Text der Score-Spalten-Überschrift (Zeile 1) – muss mit schreibeSpaltenHeader() übereinstimmen. */
    private static final String PKT_HEADER = "Pkt";

    /** Maximale Spaltenzahl beim Scan der Turnierbaum-Header-Zeile (12 Runden × 4 + Cadrage + Sieger). */
    private static final int TURNIERBAUM_SCAN_BREITE = 64;

    private KoBlattschutzKonfiguration() {
    }

    public static KoBlattschutzKonfiguration get() {
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
        sammleTurnierbaumSchutzInfos(xDoc, infos);

        return infos;
    }

    private void sammleMeldelisteSchutzInfo(XSpreadsheetDocument xDoc, WorkingSpreadsheet ws,
            List<SheetSchutzInfo> infos) {
        SheetMetadataHelper.findeSheet(xDoc, SheetMetadataHelper.SCHLUESSEL_KO_MELDELISTE).ifPresent(sheet -> {
            try {
                var konfigSheet = new KoKonfigurationSheet(ws);
                int aktivSpalte = berechneAktivSpalte(konfigSheet);
                infos.add(SheetSchutzInfo.mitEditierbarenBereichen(sheet, List.of(
                        RangePosition.from(1, MELDELISTE_ERSTE_DATEN_ZEILE,
                                aktivSpalte, MeldungenSpalte.MAX_ANZ_MELDUNGEN))));
            } catch (GenerateException e) {
                logger.warn("Editierbare Meldeliste-Spalten konnten nicht berechnet werden: {}", e.getMessage(), e);
            }
        });
    }

    private void sammleTurnierbaumSchutzInfos(XSpreadsheetDocument xDoc, List<SheetSchutzInfo> infos) {
        var schluessel = SheetMetadataHelper.getSchluesselMitPrefix(xDoc,
                SheetMetadataHelper.SCHLUESSEL_KO_TURNIERBAUM_PREFIX);
        for (var key : schluessel) {
            SheetMetadataHelper.findeSheet(xDoc, key).ifPresent(sheet -> {
                var editierbareBereiche = ermittleScoreSpalten(sheet);
                if (editierbareBereiche.isEmpty()) {
                    infos.add(SheetSchutzInfo.vollGesperrt(sheet));
                } else {
                    infos.add(SheetSchutzInfo.mitEditierbarenBereichen(sheet, editierbareBereiche));
                }
            });
        }
    }

    /**
     * Ermittelt die editierbaren Score-Spalten eines Turnierbaum-Sheets.
     * Score-Spalten sind durch die Überschrift "Pkt" in der Header-Zeile 1 erkennbar.
     */
    private List<RangePosition> ermittleScoreSpalten(XSpreadsheet sheet) {
        var bereiche = new ArrayList<RangePosition>();
        try {
            for (int spalte = 0; spalte < TURNIERBAUM_SCAN_BREITE; spalte++) {
                var xText = UnoRuntime.queryInterface(XText.class, sheet.getCellByPosition(spalte, 1));
                String header = (xText != null) ? xText.getString() : "";
                if (PKT_HEADER.equals(header)) {
                    bereiche.add(RangePosition.from(spalte, TURNIERBAUM_ERSTE_ZEILE, spalte, TURNIERBAUM_MAX_ZEILE));
                }
            }
        } catch (Exception e) {
            logger.warn("Score-Spalten im Turnierbaum konnten nicht ermittelt werden: {}", e.getMessage(), e);
        }
        return bereiche;
    }

    /**
     * Berechnet die Aktiv-Spalte dynamisch aus der Turnierkonfiguration.
     * Layout: 0=Nr, 1=[Teamname], x..y=Spieler(n), letzte+1=RNG, letzte+2=Aktiv
     */
    private int berechneAktivSpalte(KoKonfigurationSheet konfigSheet) throws GenerateException {
        int spaltenProSpieler = konfigSheet.isMeldeListeVereinsnameAnzeigen() ? 3 : 2;
        int ersterOffset = konfigSheet.isMeldeListeTeamnameAnzeigen() ? 2 : 1;
        int letzteDataSpalte = ersterOffset + konfigSheet.getMeldeListeFormation().getAnzSpieler() * spaltenProSpieler - 1;
        return letzteDataSpalte + 2; // +1=RNG, +2=Aktiv
    }
}
