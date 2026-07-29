/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.formulex.meldeliste;

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
class FormuleXMeldeListeKompaktierenUITest extends BaseCalcUITest {

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
        FormuleXMeldeListeSheetNew meldeListeNew = new FormuleXMeldeListeSheetNew(wkingSpreadsheet);
        meldeListeNew.createMeldelisteWithParams(formation, teamnameAktiv, false, 4);

        int anzSpieler = formation.getAnzSpieler();
        int ersteDatenZeile = FormuleXListeDelegate.ERSTE_DATEN_ZEILE;
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
            zeile.newInt(FormuleXListeDelegate.AKTIV_WERT_NIMMT_TEIL);
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

        new FormuleXMeldeListeSheetUpdate(wkingSpreadsheet).doRun();

        int erwarteteAnzMeldungen = ANZ_TEAMS - 2;
        int pruefBisZeile = ersteDatenZeile + ANZ_TEAMS + 1;
        RangePosition pruefRange = RangePosition.from(0, ersteDatenZeile, aktivSpalte, pruefBisZeile);
        RangeData nachAktualisieren = RangeHelper.from(xSheet, doc, pruefRange).getDataFromRange();

        int vornameSpalteImBlock = vornameSpalte;

        // Alle Zeilen bis erwarteteAnzMeldungen muessen einen Namen UND eine gueltige Nr haben
        for (int i = 0; i < erwarteteAnzMeldungen; i++) {
            RowData zeile = nachAktualisieren.get(i);
            String vorname = zeile.get(vornameSpalteImBlock).getStringVal();
            assertThat(vorname).as("Vorname in Zeile %d (kompakter Bereich)", i).isNotBlank();
            int nr = zeile.get(0).getIntVal(-1);
            assertThat(nr).as("Team-Nr in Zeile %d (kompakter Bereich)", i).isGreaterThan(0);
        }

        // Ab dort keine weiteren Meldungen mehr (keine Lücken-Reste, alles zusammengerückt)
        for (int i = erwarteteAnzMeldungen; i < nachAktualisieren.size(); i++) {
            RowData zeile = nachAktualisieren.get(i);
            String vorname = zeile.get(vornameSpalteImBlock).getStringVal();
            assertThat(vorname).as("Vorname in Zeile %d (muss leer sein)", i).isNullOrEmpty();
        }
    }

    /**
     * Regression: bei Formationen mit mehreren Namens-Spalten (Vorname+Nachname) durfte die
     * Zeilen-Entfernung bisher nur die erste Namens-Spalte prüfen – eine Zeile mit leerem
     * Vorname aber gefülltem Nachname wurde dadurch fälschlich gelöscht, obwohl ein Spieler
     * gemeldet ist.
     */
    @Test
    void zeileMitLeeremVornameAberGefuelltemNachnameBleibtErhalten() throws Exception {
        FormuleXMeldeListeSheetNew meldeListeNew = new FormuleXMeldeListeSheetNew(wkingSpreadsheet);
        meldeListeNew.createMeldelisteWithParams(Formation.TETE, false, false, 4);

        int ersteDatenZeile = FormuleXListeDelegate.ERSTE_DATEN_ZEILE;
        int vornameSpalte = meldeListeNew.getVornameSpalte(0);
        int nachnameSpalte = meldeListeNew.getNachnameSpalte(0);
        int aktivSpalte = meldeListeNew.getAktivSpalte();

        RangeData data = new RangeData();
        RowData zeile1 = data.addNewRow();
        zeile1.newInt(1);
        zeile1.newString(""); // Vorname leer
        zeile1.newString("Mueller"); // Nachname vorhanden -> Zeile MUSS erhalten bleiben
        zeile1.newEmpty(); // Setzposition
        zeile1.newInt(FormuleXListeDelegate.AKTIV_WERT_NIMMT_TEIL);

        RowData zeile2 = data.addNewRow();
        zeile2.newInt(2);
        zeile2.newString(""); // komplett ohne Namen -> Zeile MUSS entfernt werden
        zeile2.newString("");
        zeile2.newEmpty();
        zeile2.newInt(FormuleXListeDelegate.AKTIV_WERT_NIMMT_TEIL);

        XSpreadsheet xSheet = meldeListeNew.getXSpreadSheet();
        RangeHelper.from(xSheet, doc, data.getRangePosition(Position.from(0, ersteDatenZeile))).setDataInRange(data);

        new FormuleXMeldeListeSheetUpdate(wkingSpreadsheet).doRun();

        RangePosition pruefRange = RangePosition.from(0, ersteDatenZeile, aktivSpalte, ersteDatenZeile + 2);
        RangeData nachAktualisieren = RangeHelper.from(xSheet, doc, pruefRange).getDataFromRange();

        assertThat(nachAktualisieren.get(0).get(0).getIntVal(-1)).as("Nr Zeile 1 bleibt erhalten").isEqualTo(1);
        assertThat(nachAktualisieren.get(0).get(nachnameSpalte).getStringVal())
                .as("Nachname Zeile 1 bleibt erhalten").isEqualTo("Mueller");

        assertThat(nachAktualisieren.get(1).get(0).getIntVal(-1))
                .as("Team-Nr der komplett leeren Zeile 2 wurde entfernt").isEqualTo(-1);
    }

    /**
     * Namen und Teamname müssen beim Aktualisieren getrimmt werden (führende/nachfolgende
     * Leerzeichen entfernt) – ein Feld mit nur Leerzeichen zählt danach als leer und die Zeile
     * wird entfernt.
     */
    @Test
    void namenUndTeamnameWerdenGetrimmtUndWhitespaceOnlyGiltAlsLeer() throws Exception {
        FormuleXMeldeListeSheetNew meldeListeNew = new FormuleXMeldeListeSheetNew(wkingSpreadsheet);
        meldeListeNew.createMeldelisteWithParams(Formation.TETE, true, false, 4);

        int ersteDatenZeile = FormuleXListeDelegate.ERSTE_DATEN_ZEILE;
        int teamnameSpalte = meldeListeNew.getTeamnameSpalte();
        int vornameSpalte = meldeListeNew.getVornameSpalte(0);
        int aktivSpalte = meldeListeNew.getAktivSpalte();

        RangeData data = new RangeData();
        RowData zeile1 = data.addNewRow();
        zeile1.newInt(1);
        zeile1.newString("  Team Eins  "); // Teamname mit Leerzeichen -> muss getrimmt werden
        zeile1.newString("  Max  "); // Vorname mit Leerzeichen -> muss getrimmt werden
        zeile1.newString("Muster"); // Nachname
        zeile1.newEmpty(); // Setzposition
        zeile1.newInt(FormuleXListeDelegate.AKTIV_WERT_NIMMT_TEIL);

        RowData zeile2 = data.addNewRow();
        zeile2.newInt(2);
        zeile2.newString("   "); // nur Leerzeichen -> gilt als leer
        zeile2.newString("   "); // nur Leerzeichen -> gilt als leer
        zeile2.newString("   "); // nur Leerzeichen -> gilt als leer -> Zeile MUSS entfernt werden
        zeile2.newEmpty();
        zeile2.newInt(FormuleXListeDelegate.AKTIV_WERT_NIMMT_TEIL);

        XSpreadsheet xSheet = meldeListeNew.getXSpreadSheet();
        RangeHelper.from(xSheet, doc, data.getRangePosition(Position.from(0, ersteDatenZeile))).setDataInRange(data);

        new FormuleXMeldeListeSheetUpdate(wkingSpreadsheet).doRun();

        RangePosition pruefRange = RangePosition.from(0, ersteDatenZeile, aktivSpalte, ersteDatenZeile + 2);
        RangeData nachAktualisieren = RangeHelper.from(xSheet, doc, pruefRange).getDataFromRange();

        assertThat(nachAktualisieren.get(0).get(0).getIntVal(-1)).as("Nr Zeile 1 bleibt erhalten").isEqualTo(1);
        assertThat(nachAktualisieren.get(0).get(teamnameSpalte).getStringVal())
                .as("Teamname wurde getrimmt").isEqualTo("Team Eins");
        assertThat(nachAktualisieren.get(0).get(vornameSpalte).getStringVal())
                .as("Vorname wurde getrimmt").isEqualTo("Max");

        assertThat(nachAktualisieren.get(1).get(0).getIntVal(-1))
                .as("Team-Nr der Whitespace-only-Zeile wurde entfernt").isEqualTo(-1);
    }

}
