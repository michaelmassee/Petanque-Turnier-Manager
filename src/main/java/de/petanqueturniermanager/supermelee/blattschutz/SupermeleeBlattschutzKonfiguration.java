/**
 * Erstellung : 2026 / Michael Massee
 **/

package de.petanqueturniermanager.supermelee.blattschutz;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheetDocument;

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
import de.petanqueturniermanager.supermelee.spielrunde.SpielrundeSheetKonstanten;

/**
 * Blattschutz-Konfiguration für das Supermelee-Turniersystem.
 * <p>
 * Identifiziert alle Supermelee-Sheets über {@link SheetMetadataHelper} und
 * legt fest, welche Zellbereiche trotz Schutz editierbar bleiben sollen:
 * <ul>
 *   <li><b>Meldeliste:</b> Name-, SP- und Spieltag-Spalten</li>
 *   <li><b>Spielrunden:</b> Ergebnis-A- und Ergebnis-B-Spalten</li>
 *   <li><b>Alle anderen Sheets:</b> vollständig gesperrt</li>
 * </ul>
 */
public class SupermeleeBlattschutzKonfiguration implements IBlattschutzKonfiguration {

    private static final Logger logger = LogManager.getLogger(SupermeleeBlattschutzKonfiguration.class);
    private static final SupermeleeBlattschutzKonfiguration INSTANCE =
            new SupermeleeBlattschutzKonfiguration();

    private SupermeleeBlattschutzKonfiguration() {
    }

    public static SupermeleeBlattschutzKonfiguration get() {
        return INSTANCE;
    }

    // -------------------------------------------------------------------------
    // IBlattschutzKonfiguration

    @Override
    public void zelleStylesAktualisieren(WorkingSpreadsheet ws) {
        var doc = ws.getWorkingSpreadsheetDocument();
        // Styles direkt auf Dokumentebene erzeugen/aktualisieren – unabhängig davon,
        // ob die Meldeliste im Dokument gerade existiert.
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
        sammleSpielrundenSchutzInfos(xDoc, infos);
        sammleVollGesperrteSheets(xDoc, infos);

        return infos;
    }

    // -------------------------------------------------------------------------

    /** Fügt die Meldeliste mit ihren editierbaren Bereichen hinzu. */
    private void sammleMeldelisteSchutzInfo(XSpreadsheetDocument xDoc,
            List<SheetSchutzInfo> infos) {
        SheetMetadataHelper.findeSheet(xDoc,
                SheetMetadataHelper.SCHLUESSEL_SUPERMELEE_MELDELISTE).ifPresent(sheet -> {
            var bereiche = berechneMeldelisteBereiche(xDoc);
            infos.add(SheetSchutzInfo.mitEditierbarenBereichen(sheet, bereiche));
        });
    }

    /** Fügt jede Spielrunde mit ihren editierbaren Ergebnis-Bereichen hinzu. */
    private void sammleSpielrundenSchutzInfos(XSpreadsheetDocument xDoc,
            List<SheetSchutzInfo> infos) {
        var schluessel = SheetMetadataHelper.getSchluesselMitPrefix(xDoc,
                SheetMetadataHelper.SCHLUESSEL_SUPERMELEE_SPIELRUNDE_PREFIX);
        for (var key : schluessel) {
            SheetMetadataHelper.findeSheet(xDoc, key).ifPresent(sheet ->
                    infos.add(SheetSchutzInfo.mitEditierbarenBereichen(sheet,
                            List.of(berechneSpielrundeErgebnisBereich()))));
        }
    }

    /** Fügt alle vollständig zu sperrenden Supermelee-Sheets hinzu. */
    private void sammleVollGesperrteSheets(XSpreadsheetDocument xDoc,
            List<SheetSchutzInfo> infos) {
        fuegeVollgesperrteAusPrefix(xDoc, SheetMetadataHelper.SCHLUESSEL_SUPERMELEE_SPIELRUNDE_PLAN_PREFIX, infos);
        fuegeVollgesperrteAusPrefix(xDoc, SheetMetadataHelper.SCHLUESSEL_SUPERMELEE_ANMELDUNGEN_PREFIX, infos);
        fuegeVollgesperrteAusPrefix(xDoc, SheetMetadataHelper.SCHLUESSEL_SUPERMELEE_SPIELTAG_TEILNEHMER_PREFIX, infos);
        fuegeVollgesperrteAusPrefix(xDoc, SheetMetadataHelper.SCHLUESSEL_SPIELTAG_RANGLISTE_PREFIX, infos);

        SheetMetadataHelper.findeSheet(xDoc,
                SheetMetadataHelper.SCHLUESSEL_SUPERMELEE_ENDRANGLISTE)
                .ifPresent(sheet -> infos.add(SheetSchutzInfo.vollGesperrt(sheet)));
        SheetMetadataHelper.findeSheet(xDoc,
                SheetMetadataHelper.SCHLUESSEL_SUPERMELEE_TEAMS)
                .ifPresent(sheet -> infos.add(SheetSchutzInfo.vollGesperrt(sheet)));
    }

    private void fuegeVollgesperrteAusPrefix(XSpreadsheetDocument xDoc, String prefix,
            List<SheetSchutzInfo> infos) {
        for (var key : SheetMetadataHelper.getSchluesselMitPrefix(xDoc, prefix)) {
            SheetMetadataHelper.findeSheet(xDoc, key)
                    .ifPresent(sheet -> infos.add(SheetSchutzInfo.vollGesperrt(sheet)));
        }
    }

    // -------------------------------------------------------------------------
    // Bereichsberechnung

    /**
     * Berechnet die editierbaren Bereiche der Meldeliste.
     * <p>
     * Spalten-Layout für Supermelee (Formation EINZEL, 1 Spieler pro Team):
     * <ul>
     *   <li>Spalte 0 = Spieler-Nr (nicht editierbar)</li>
     *   <li>Spalte 1 = Name (editierbar)</li>
     *   <li>Spalte 2 = SP/Setzposition (editierbar)</li>
     *   <li>Spalte 3 bis 2+anzSpieltage = Spieltage (editierbar)</li>
     * </ul>
     */
    private List<RangePosition> berechneMeldelisteBereiche(XSpreadsheetDocument xDoc) {
        int ersteDatenZeile = MeldeListeKonstanten.ERSTE_DATEN_ZEILE;
        int maxZeile = MeldungenSpalte.MAX_ANZ_MELDUNGEN;

        var bereiche = new ArrayList<RangePosition>();
        bereiche.add(RangePosition.from(1, ersteDatenZeile, 1, maxZeile)); // Name
        bereiche.add(RangePosition.from(2, ersteDatenZeile, 2, maxZeile)); // SP

        int anzSpieltage = SheetMetadataHelper.getSchluesselMitPrefix(xDoc,
                SheetMetadataHelper.SCHLUESSEL_SPIELTAG_RANGLISTE_PREFIX).length;
        if (anzSpieltage > 0) {
            // Spieltage: Spalte 3 bis 2+anzSpieltage (inklusiv)
            bereiche.add(RangePosition.from(3, ersteDatenZeile, 2 + anzSpieltage, maxZeile));
        }
        return bereiche;
    }

    /**
     * Berechnet den editierbaren Ergebnis-Bereich einer Spielrunde.
     * <p>
     * Ergebnis A = Spalte {@code ERSTE_SPALTE_ERGEBNISSE} (Index 7 = col H),
     * Ergebnis B = Spalte {@code ERSTE_SPALTE_ERGEBNISSE + 1} (Index 8 = col I).
     */
    private RangePosition berechneSpielrundeErgebnisBereich() {
        return RangePosition.from(
                SpielrundeSheetKonstanten.ERSTE_SPALTE_ERGEBNISSE,
                SpielrundeSheetKonstanten.ERSTE_DATEN_ZEILE,
                SpielrundeSheetKonstanten.ERSTE_SPALTE_ERGEBNISSE + 1,
                MeldungenSpalte.MAX_ANZ_MELDUNGEN);
    }
}
