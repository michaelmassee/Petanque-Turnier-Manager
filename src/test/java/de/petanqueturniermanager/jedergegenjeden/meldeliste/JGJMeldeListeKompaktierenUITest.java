/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.jedergegenjeden.meldeliste;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.helper.TestnamenLoader;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.schweizer.konfiguration.SpielplanTeamAnzeige;

/**
 * Prüft, dass "Meldeliste aktualisieren" Lücken schließt: komplett leere Zeilen
 * und Zeilen mit Team-Nr ohne Spielername werden entfernt, alle verbliebenen
 * Meldungen rücken lückenlos nach oben zusammen. Über alle Formationen (Tete,
 * Doublette, Triplette) und mit/ohne Teamname-Spalte.
 */
class JGJMeldeListeKompaktierenUITest extends BaseCalcUITest {

    private static final int ANZ_TEAMS = 6;
    private static final int ERSTE_DATEN_ZEILE = JGJMeldeListeDelegate.ERSTE_DATEN_ZEILE;

    static Stream<Arguments> formationUndTeamname() {
        return Stream.of(
                Arguments.of(Formation.TETE, true),
                Arguments.of(Formation.TETE, false),
                Arguments.of(Formation.DOUBLETTE, true),
                Arguments.of(Formation.DOUBLETTE, false),
                Arguments.of(Formation.TRIPLETTE, true),
                Arguments.of(Formation.TRIPLETTE, false));
    }

    @ParameterizedTest(name = "Formation={0}, Teamname={1}")
    @MethodSource("formationUndTeamname")
    void meldelisteWirdKompaktiert(Formation formation, boolean teamnameAktiv) throws Exception {
        JGJMeldeListeSheet_New meldeListeNew = new JGJMeldeListeSheet_New(wkingSpreadsheet);
        meldeListeNew.createMeldelisteWithParams(formation, teamnameAktiv, false, SpielplanTeamAnzeige.NR);

        int anzSpieler = formation.getAnzSpieler();
        // Layout ohne Vereinsname (2 Spalten je Spieler): Nr | [Teamname] | (Vorname+Nachname)* | SP | Aktiv
        int ersterSpielerOffset = teamnameAktiv ? 2 : 1;
        int vornameSpalte = ersterSpielerOffset;
        int letzteDataSpalte = ersterSpielerOffset + anzSpieler * 2 - 1;
        int aktivSpalte = letzteDataSpalte + 2;

        var testnamenLoader = new TestnamenLoader();
        List<TestnamenLoader.SpielerTestname> spieler = testnamenLoader.listeMitSpielerTestNamen(ANZ_TEAMS * anzSpieler);

        RangeData data = new RangeData();
        for (int i = 0; i < ANZ_TEAMS; i++) {
            RowData zeile = data.addNewRow();
            zeile.newInt(i + 1); // Team-Nr
            if (teamnameAktiv) {
                zeile.newString("Team " + (i + 1));
            }
            for (int s = 0; s < anzSpieler; s++) {
                var stn = spieler.get(i * anzSpieler + s);
                zeile.newString(stn.vorname());
                zeile.newString(stn.nachname());
            }
            zeile.newEmpty(); // Setzposition
            zeile.newInt(JGJMeldeListeDelegate.AKTIV_WERT_NIMMT_TEIL);
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

        new JGJMeldeListeSheet_Update(wkingSpreadsheet).doRun();

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
