/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.triptete.meldeliste;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.helper.TestnamenLoader;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;

/**
 * Prüft, dass "Meldeliste aktualisieren" Lücken schließt: komplett leere Zeilen
 * und Zeilen mit Team-Nr ohne Spielername werden entfernt, alle verbliebenen
 * Meldungen rücken lückenlos nach oben zusammen. TripTete kennt nur die
 * Formation Triplette (nicht konfigurierbar) — daher nur mit/ohne Teamname.
 */
class TripTeteMeldeListeKompaktierenUITest extends BaseCalcUITest {

    private static final int ANZ_TEAMS = 6;
    private static final int ANZ_SPIELER = 3;
    private static final int ERSTE_DATEN_ZEILE = TripTeteMeldeListeDelegate.ERSTE_DATEN_ZEILE_OVERRIDE;

    @ParameterizedTest(name = "Teamname={0}")
    @ValueSource(booleans = { true, false })
    void meldelisteWirdKompaktiert(boolean teamnameAktiv) throws Exception {
        TripTeteMeldeListeSheetNew meldeListeNew = new TripTeteMeldeListeSheetNew(wkingSpreadsheet);
        meldeListeNew.getKonfigurationSheet().setMeldeListeTeamnameAnzeigen(teamnameAktiv);
        meldeListeNew.createMeldeliste();

        TripTeteMeldeListeDelegate delegate = new TripTeteMeldeListeDelegate(meldeListeNew, wkingSpreadsheet);
        int vornameSpalte = delegate.getVornameSpalte(0);
        int aktivSpalte = delegate.getAktivSpalte();

        var testnamenLoader = new TestnamenLoader();
        List<TestnamenLoader.SpielerTestname> spieler = testnamenLoader.listeMitSpielerTestNamen(ANZ_TEAMS * ANZ_SPIELER);

        RangeData data = new RangeData();
        for (int i = 0; i < ANZ_TEAMS; i++) {
            RowData zeile = data.addNewRow();
            zeile.newInt(i + 1); // Team-Nr
            if (teamnameAktiv) {
                zeile.newString("Team " + (i + 1));
            }
            for (int s = 0; s < ANZ_SPIELER; s++) {
                var stn = spieler.get(i * ANZ_SPIELER + s);
                zeile.newString(stn.vorname());
                zeile.newString(stn.nachname());
            }
            zeile.newInt(1); // Aktiv
        }

        XSpreadsheet xSheet = meldeListeNew.getXSpreadSheet();
        RangeHelper.from(xSheet, doc, data.getRangePosition(Position.from(0, ERSTE_DATEN_ZEILE))).setDataInRange(data);

        // Lücke 1: eine Zeile in der Mitte komplett leeren (Team Nr. 3 verschwindet spurlos)
        int komplettLeereZeile = ERSTE_DATEN_ZEILE + 2;
        RangeHelper.from(xSheet, doc, RangePosition.from(0, komplettLeereZeile, aktivSpalte, komplettLeereZeile))
                .clearRange();

        // Lücke 2: Team-Nr ohne Namen (letzte Zeile: Name geleert, Nr bleibt stehen)
        int nrOhneNamenZeile = ERSTE_DATEN_ZEILE + ANZ_TEAMS - 1;
        RangeHelper.from(xSheet, doc, RangePosition.from(vornameSpalte, nrOhneNamenZeile, aktivSpalte, nrOhneNamenZeile))
                .clearRange();

        new TripTeteMeldeListeSheetUpdate(wkingSpreadsheet).doRun();

        int erwarteteAnzMeldungen = ANZ_TEAMS - 2;
        int pruefBisZeile = ERSTE_DATEN_ZEILE + ANZ_TEAMS + 1;
        RangePosition pruefRange = RangePosition.from(0, ERSTE_DATEN_ZEILE, aktivSpalte, pruefBisZeile);
        RangeData nachAktualisieren = RangeHelper.from(xSheet, doc, pruefRange).getDataFromRange();

        for (int i = 0; i < erwarteteAnzMeldungen; i++) {
            RowData zeile = nachAktualisieren.get(i);
            String vorname = zeile.get(vornameSpalte).getStringVal();
            assertThat(vorname).as("Vorname in Zeile %d (kompakter Bereich)", i).isNotBlank();
            int nr = zeile.get(0).getIntVal(-1);
            assertThat(nr).as("Team-Nr in Zeile %d (kompakter Bereich)", i).isGreaterThan(0);
        }

        for (int i = erwarteteAnzMeldungen; i < nachAktualisieren.size(); i++) {
            RowData zeile = nachAktualisieren.get(i);
            String vorname = zeile.get(vornameSpalte).getStringVal();
            assertThat(vorname).as("Vorname in Zeile %d (muss leer sein)", i).isNullOrEmpty();
        }
    }

}
