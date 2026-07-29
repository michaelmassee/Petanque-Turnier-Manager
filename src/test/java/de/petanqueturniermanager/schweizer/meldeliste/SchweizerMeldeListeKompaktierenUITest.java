/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.schweizer.meldeliste;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.helper.TestnamenLoader;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;

/**
 * Prüft, dass "Meldeliste aktualisieren" Lücken schließt: komplett leere Zeilen
 * und Zeilen mit Team-Nr ohne Spielername werden entfernt, alle verbliebenen
 * Meldungen rücken lückenlos nach oben zusammen. Über alle Formationen (Tete,
 * Doublette, Triplette) und mit/ohne Teamname-Spalte.
 */
class SchweizerMeldeListeKompaktierenUITest extends BaseCalcUITest {

    private static final int ANZ_TEAMS = 6;

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
        SchweizerMeldeListeSheetNew meldeListeNew = new SchweizerMeldeListeSheetNew(wkingSpreadsheet);
        meldeListeNew.createMeldelisteWithParams(formation, teamnameAktiv, false);

        int anzSpieler = formation.getAnzSpieler();
        int ersteDatenZeile = SchweizerListeDelegate.ERSTE_DATEN_ZEILE;
        int vornameSpalte = meldeListeNew.getVornameSpalte(0);
        int aktivSpalte = meldeListeNew.getAktivSpalte();

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
            zeile.newInt(SchweizerListeDelegate.AKTIV_WERT_NIMMT_TEIL);
        }

        XSpreadsheet xSheet = meldeListeNew.getXSpreadSheet();
        RangeHelper.from(xSheet, doc, data.getRangePosition(Position.from(0, ersteDatenZeile))).setDataInRange(data);

        // Lücke 1: eine Zeile in der Mitte komplett leeren (Team Nr. 3 verschwindet spurlos)
        int komplettLeereZeile = ersteDatenZeile + 2;
        RangeHelper.from(xSheet, doc, RangePosition.from(0, komplettLeereZeile, aktivSpalte, komplettLeereZeile))
                .clearRange();

        // Lücke 2: Team-Nr ohne Namen (letzte Zeile: Name geleert, Nr bleibt stehen)
        int nrOhneNamenZeile = ersteDatenZeile + ANZ_TEAMS - 1;
        RangeHelper.from(xSheet, doc, RangePosition.from(vornameSpalte, nrOhneNamenZeile, aktivSpalte, nrOhneNamenZeile))
                .clearRange();

        new SchweizerMeldeListeSheetUpdate(wkingSpreadsheet).doRun();

        int erwarteteAnzMeldungen = ANZ_TEAMS - 2;
        int pruefBisZeile = ersteDatenZeile + ANZ_TEAMS + 1;
        RangePosition pruefRange = RangePosition.from(0, ersteDatenZeile, aktivSpalte, pruefBisZeile);
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

    /**
     * Regression: eine Team-Nr OHNE Namen, die hinter mehreren Lücken-Zeilen steht, wurde von
     * "Meldeliste aktualisieren" bisher übersehen. Ursache: {@code naechsteFreieDatenZeileInSpielerNrSpalte()}
     * brach beim ERSTEN Lücken-Gap ab, statt den gesamten Bereich zu scannen – Nummern, die
     * hinter einer weiteren Lücke standen, wurden dadurch nie geprüft/entfernt.
     */
    @Test
    void teamNrHinterMehrerenLueckenWirdEntfernt() throws Exception {
        SchweizerMeldeListeSheetNew meldeListeNew = new SchweizerMeldeListeSheetNew(wkingSpreadsheet);
        meldeListeNew.createMeldelisteWithParams(Formation.TETE, false, false);

        int ersteDatenZeile = SchweizerListeDelegate.ERSTE_DATEN_ZEILE;
        int vornameSpalte = meldeListeNew.getVornameSpalte(0);
        int aktivSpalte = meldeListeNew.getAktivSpalte();

        RangeData data = new RangeData();
        RowData zeile1 = data.addNewRow();
        zeile1.newInt(1);
        zeile1.newString("Max");
        zeile1.newString("Muster");
        zeile1.newEmpty(); // Setzposition
        zeile1.newInt(SchweizerListeDelegate.AKTIV_WERT_NIMMT_TEIL);

        XSpreadsheet xSheet = meldeListeNew.getXSpreadSheet();
        RangeHelper.from(xSheet, doc, data.getRangePosition(Position.from(0, ersteDatenZeile))).setDataInRange(data);

        // Lücken + zwei "Nr ohne Namen"-Zeilen dahinter schreiben (spiegelt den real gemeldeten
        // Fall: mehrere Leerzeilen, danach eine Zeile nur mit Startnummer, wieder Leerzeilen,
        // danach nochmal eine Zeile nur mit Startnummer)
        int nrOhneNamen1 = ersteDatenZeile + 3;
        int nrOhneNamen2 = ersteDatenZeile + 6;
        NumberCellValue nrCelVal = NumberCellValue.from(xSheet, Position.from(0, nrOhneNamen1));
        meldeListeNew.getSheetHelper().setNumberValueInCell(nrCelVal.setValue(87));
        meldeListeNew.getSheetHelper().setNumberValueInCell(nrCelVal.zeile(nrOhneNamen2).setValue(90));

        new SchweizerMeldeListeSheetUpdate(wkingSpreadsheet).doRun();

        RangePosition pruefRange = RangePosition.from(0, ersteDatenZeile, aktivSpalte, nrOhneNamen2 + 1);
        RangeData nachAktualisieren = RangeHelper.from(xSheet, doc, pruefRange).getDataFromRange();

        assertThat(nachAktualisieren.get(0).get(0).getIntVal(-1)).as("Nr der einzigen echten Meldung").isEqualTo(1);
        assertThat(nachAktualisieren.get(0).get(vornameSpalte).getStringVal())
                .as("Name der echten Meldung bleibt erhalten").isNotBlank();

        for (int i = 1; i < nachAktualisieren.size(); i++) {
            RowData zeile = nachAktualisieren.get(i);
            assertThat(zeile.get(0).getIntVal(-1))
                    .as("Zeile %d darf keine Nr mehr ohne Namen enthalten (Nr 87/90 aus der Lücke)", i)
                    .isEqualTo(-1);
        }
    }

}
