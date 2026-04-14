package de.petanqueturniermanager.algorithmen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

public class KaskadenKoRundenPlanerTest {

    // ---------------------------------------------------------------
    // Exception-Tests
    // ---------------------------------------------------------------

    @Test
    public void testBerechne_gesamtTeamsZuKlein_wirftIllegalArgumentException() {
        assertThatThrownBy(() -> KaskadenKoRundenPlaner.berechne(1, 2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> KaskadenKoRundenPlaner.berechne(0, 2))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testBerechne_kaskadenStufenZuKlein_wirftIllegalArgumentException() {
        assertThatThrownBy(() -> KaskadenKoRundenPlaner.berechne(16, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testBerechne_kaputtteStrategie_wirftIllegalStateException() {
        // Strategie liefert immer 0 Paarungen – falsch für m > 1
        KaskadenKoPaarungsStrategie kaputt = (pfad, rundenNr, anzTeams) -> List.of();
        assertThatThrownBy(() -> KaskadenKoRundenPlaner.berechne(16, 2, kaputt))
                .isInstanceOf(IllegalStateException.class);
    }

    // ---------------------------------------------------------------
    // k=1: 1 Kaskadenrunde, 2 Endfelder
    // ---------------------------------------------------------------

    @Test
    public void testBerechne_4Teams_1Kaskade() {
        var plan = KaskadenKoRundenPlaner.berechne(4, 1);

        assertThat(plan.gesamtTeams()).isEqualTo(4);
        assertThat(plan.kaskadenStufen()).isEqualTo(1);
        assertThat(plan.kaskadeRunden()).hasSize(1);

        var runde1 = plan.kaskadeRunden().get(0);
        assertThat(runde1.rundenNr()).isEqualTo(1);
        assertThat(runde1.gruppenRunden()).hasSize(1);

        var gruppe = runde1.gruppenRunden().get(0);
        assertGruppenRunde(gruppe, "", 4, 2, 0);
        assertSpielPaar(gruppe.spielPaare().get(0), 1, 1, 2);
        assertSpielPaar(gruppe.spielPaare().get(1), 2, 3, 4);

        assertInvariantenAlleRunden(plan);
    }

    // ---------------------------------------------------------------
    // k=2: 2 Kaskadenrunden – Spezifikationsbeispiele
    // ---------------------------------------------------------------

    @Test
    public void testBerechne_16Teams_2Kaskaden_alleZweierpotenzen() {
        var plan = KaskadenKoRundenPlaner.berechne(16, 2);

        assertThat(plan.kaskadeRunden()).hasSize(2);

        // Runde 1: 1 Gruppe, 16 Teams, 8 Paare, 0 Freilose
        var runde1 = plan.kaskadeRunden().get(0);
        assertThat(runde1.gruppenRunden()).hasSize(1);
        var r1g0 = runde1.gruppenRunden().get(0);
        assertGruppenRunde(r1g0, "", 16, 8, 0);
        assertSpielPaar(r1g0.spielPaare().get(0), 1, 1, 2);
        assertSpielPaar(r1g0.spielPaare().get(7), 8, 15, 16);

        // Runde 2: 2 Gruppen (8 Teams je), 4 Paare je, 0 Freilose
        var runde2 = plan.kaskadeRunden().get(1);
        assertThat(runde2.gruppenRunden()).hasSize(2);
        assertGruppenRunde(runde2.gruppenRunden().get(0), "S", 8, 4, 0);
        assertGruppenRunde(runde2.gruppenRunden().get(1), "V", 8, 4, 0);

        assertInvariantenAlleRunden(plan);
    }

    @Test
    public void testBerechne_34Teams_2Kaskaden() {
        var plan = KaskadenKoRundenPlaner.berechne(34, 2);

        // Runde 1: 34 Teams, 17 Paare, 0 Freilose
        var r1 = plan.kaskadeRunden().get(0);
        assertGruppenRunde(r1.gruppenRunden().get(0), "", 34, 17, 0);
        assertSpielPaar(r1.gruppenRunden().get(0).spielPaare().get(0), 1, 1, 2);
        assertSpielPaar(r1.gruppenRunden().get(0).spielPaare().get(16), 17, 33, 34);

        // Runde 2: S-Gruppe (17 Teams, 8 Paare, 1 Freilos) und V-Gruppe (17 Teams, 8 Paare, 1 Freilos)
        var r2 = plan.kaskadeRunden().get(1);
        assertThat(r2.gruppenRunden()).hasSize(2);
        var r2gS = r2.gruppenRunden().get(0);
        assertGruppenRunde(r2gS, "S", 17, 8, 1);
        assertSpielPaar(r2gS.spielPaare().get(0), 1, 1, 2);
        assertSpielPaar(r2gS.spielPaare().get(7), 8, 15, 16);
        // Position 17 ist Freilos → nicht in spielPaare

        var r2gV = r2.gruppenRunden().get(1);
        assertGruppenRunde(r2gV, "V", 17, 8, 1);

        assertInvariantenAlleRunden(plan);
    }

    @Test
    public void testBerechne_30Teams_2Kaskaden() {
        var plan = KaskadenKoRundenPlaner.berechne(30, 2);

        // Runde 1: 30 Teams, 15 Paare, 0 Freilose
        var r1 = plan.kaskadeRunden().get(0);
        assertGruppenRunde(r1.gruppenRunden().get(0), "", 30, 15, 0);

        // Runde 2: 2 Gruppen à 15 Teams, je 7 Paare, 1 Freilos
        var r2 = plan.kaskadeRunden().get(1);
        assertGruppenRunde(r2.gruppenRunden().get(0), "S", 15, 7, 1);
        assertGruppenRunde(r2.gruppenRunden().get(1), "V", 15, 7, 1);

        assertInvariantenAlleRunden(plan);
    }

    // ---------------------------------------------------------------
    // k=3: 3 Kaskadenrunden, 8 Endfelder
    // ---------------------------------------------------------------

    @Test
    public void testBerechne_34Teams_3Kaskaden() {
        var plan = KaskadenKoRundenPlaner.berechne(34, 3);

        assertThat(plan.kaskadeRunden()).hasSize(3);

        // Runde 3: 4 Gruppen
        var r3 = plan.kaskadeRunden().get(2);
        assertThat(r3.gruppenRunden()).hasSize(4);
        assertThat(r3.gruppenRunden().get(0).pfad()).isEqualTo("SS");
        assertThat(r3.gruppenRunden().get(1).pfad()).isEqualTo("SV");
        assertThat(r3.gruppenRunden().get(2).pfad()).isEqualTo("VS");
        assertThat(r3.gruppenRunden().get(3).pfad()).isEqualTo("VV");

        // SS: 8 Teams (aus 17→8 Sieger→8/2=4 Sieger, 4 Paare, 0 Freilose)
        assertGruppenRunde(r3.gruppenRunden().get(0), "SS", 8, 4, 0);
        // SV: 9 Teams (17→8 Sieger→9 Verlierer): 4 Paare, 1 Freilos
        assertGruppenRunde(r3.gruppenRunden().get(1), "SV", 9, 4, 1);
        // VS: 8 Teams, 4 Paare, 0 Freilose
        assertGruppenRunde(r3.gruppenRunden().get(2), "VS", 8, 4, 0);
        // VV: 9 Teams, 4 Paare, 1 Freilos
        assertGruppenRunde(r3.gruppenRunden().get(3), "VV", 9, 4, 1);

        assertInvariantenAlleRunden(plan);
    }

    // ---------------------------------------------------------------
    // freispielGewonnen=true: Freilos-Team → Sieger-Gruppe
    // ---------------------------------------------------------------

    @Test
    public void testBerechne_34Teams_2Kaskaden_freispielGewonnen() {
        var plan = KaskadenKoRundenPlaner.berechne(34, 2, true);

        // Runde 1: 34 Teams, 17 Paare, 0 Freilose – unverändert
        var r1 = plan.kaskadeRunden().get(0);
        assertGruppenRunde(r1.gruppenRunden().get(0), "", 34, 17, 0);

        // Runde 2: freispielGewonnen=true → S bekommt ceil(17/2)=9, V bekommt floor(17/2)=8
        var r2 = plan.kaskadeRunden().get(1);
        assertThat(r2.gruppenRunden()).hasSize(2);
        assertGruppenRunde(r2.gruppenRunden().get(0), "S", 17, 8, 1);
        assertGruppenRunde(r2.gruppenRunden().get(1), "V", 17, 8, 1);

        // Endfelder: 34 ist gerade → Stufe 1 unverändert (S=17, V=17).
        // Stufe 2 mit freispielGewonnen: ceil(17/2)=9 → Sieger, floor(17/2)=8 → Verlierer
        assertThat(plan.felder()).hasSize(4);
        assertThat(plan.felder().get(0).bezeichner()).isEqualTo("A");
        assertThat(plan.felder().get(0).gesamtTeams()).isEqualTo(9);  // SS: ceil(17/2)=9
        assertThat(plan.felder().get(1).bezeichner()).isEqualTo("B");
        assertThat(plan.felder().get(1).gesamtTeams()).isEqualTo(8);  // SV: floor(17/2)=8
        assertThat(plan.felder().get(2).bezeichner()).isEqualTo("C");
        assertThat(plan.felder().get(2).gesamtTeams()).isEqualTo(9);  // VS: ceil(17/2)=9
        assertThat(plan.felder().get(3).bezeichner()).isEqualTo("D");
        assertThat(plan.felder().get(3).gesamtTeams()).isEqualTo(8);  // VV: floor(17/2)=8

        assertInvariantenAlleRunden(plan);
    }

    @Test
    public void testBerechne_geradeTeams_freispielGewonnenKeinEinfluss() {
        // Bei gerader Teamanzahl gibt es kein Freilos → freispielGewonnen hat keinen Einfluss
        var planOhne = KaskadenKoRundenPlaner.berechne(16, 2, false);
        var planMit  = KaskadenKoRundenPlaner.berechne(16, 2, true);

        for (int r = 0; r < planOhne.kaskadeRunden().size(); r++) {
            var rundeOhne = planOhne.kaskadeRunden().get(r);
            var rundeMit  = planMit.kaskadeRunden().get(r);
            for (int g = 0; g < rundeOhne.gruppenRunden().size(); g++) {
                assertThat(rundeMit.gruppenRunden().get(g).anzTeams())
                        .as("anzTeams Runde %d Gruppe %d", r + 1, g)
                        .isEqualTo(rundeOhne.gruppenRunden().get(g).anzTeams());
            }
        }
    }

    @Test
    public void testBerechne_felderKonsistenzMitFreispielGewonnen() {
        // Endfelder im Plan müssen mit KaskadenKoFeldRechner.berechne(n,k,true) übereinstimmen
        for (int k = 1; k <= 3; k++) {
            for (int n = 2; n <= 30; n++) {
                var plan     = KaskadenKoRundenPlaner.berechne(n, k, true);
                var erwartet = KaskadenKoFeldRechner.berechne(n, k, true);
                for (int i = 0; i < erwartet.size(); i++) {
                    assertThat(plan.felder().get(i).gesamtTeams())
                            .as("gesamtTeams[%d] n=%d k=%d freispielGewonnen=true", i, n, k)
                            .isEqualTo(erwartet.get(i).gesamtTeams());
                }
            }
        }
    }

    // ---------------------------------------------------------------
    // Felder-Konsistenz
    // ---------------------------------------------------------------

    @Test
    public void testBerechne_felderStimmenMitKaskadenKoFeldRechnerUeberein() {
        for (int k = 1; k <= 3; k++) {
            for (int n = 2; n <= 50; n++) {
                var plan = KaskadenKoRundenPlaner.berechne(n, k);
                var erwartet = KaskadenKoFeldRechner.berechne(n, k);
                assertThat(plan.felder())
                        .as("Felder müssen mit KaskadenKoFeldRechner.berechne(%d,%d) übereinstimmen", n, k)
                        .hasSameSizeAs(erwartet);
                for (int i = 0; i < erwartet.size(); i++) {
                    var ist = plan.felder().get(i);
                    var exp = erwartet.get(i);
                    assertThat(ist.bezeichner()).as("bezeichner[%d] n=%d k=%d", i, n, k).isEqualTo(exp.bezeichner());
                    assertThat(ist.pfad()).as("pfad[%d] n=%d k=%d", i, n, k).isEqualTo(exp.pfad());
                    assertThat(ist.gesamtTeams()).as("gesamtTeams[%d] n=%d k=%d", i, n, k).isEqualTo(exp.gesamtTeams());
                    assertThat(ist.isCadrageNoetig()).as("isCadrageNoetig[%d] n=%d k=%d", i, n, k).isEqualTo(exp.isCadrageNoetig());
                    assertThat(ist.zielTeams()).as("zielTeams[%d] n=%d k=%d", i, n, k).isEqualTo(exp.zielTeams());
                }
            }
        }
    }

    // ---------------------------------------------------------------
    // Invarianten
    // ---------------------------------------------------------------

    @Test
    public void testInvariante_anzTeamsGleich2xSpieleGleichFreilose() {
        var plan = KaskadenKoRundenPlaner.berechne(34, 2);
        pruefeInvarianteAnzTeams(plan);
    }

    @Test
    public void testInvariante_anzTeamsModulo2GleichFreilose() {
        for (int k = 1; k <= 3; k++) {
            for (int n = 2; n <= 30; n++) {
                pruefeInvarianteFreilos(KaskadenKoRundenPlaner.berechne(n, k));
            }
        }
    }

    @Test
    public void testInvariante_anzGruppenProRunde() {
        var plan = KaskadenKoRundenPlaner.berechne(16, 3);
        assertThat(plan.kaskadeRunden().get(0).gruppenRunden()).hasSize(1); // 2^0
        assertThat(plan.kaskadeRunden().get(1).gruppenRunden()).hasSize(2); // 2^1
        assertThat(plan.kaskadeRunden().get(2).gruppenRunden()).hasSize(4); // 2^2
    }

    @Test
    public void testInvariante_teamsummeProRundeGleichGesamtTeams() {
        for (int k = 1; k <= 3; k++) {
            for (int n = 2; n <= 30; n++) {
                var plan = KaskadenKoRundenPlaner.berechne(n, k);
                for (var runde : plan.kaskadeRunden()) {
                    int summe = runde.gruppenRunden().stream()
                            .mapToInt(KaskadenKoGruppenRunde::anzTeams)
                            .sum();
                    assertThat(summe)
                            .as("Teamsumme in Runde %d muss %d sein (n=%d, k=%d)",
                                    runde.rundenNr(), n, n, k)
                            .isEqualTo(n);
                }
            }
        }
    }

    @Test
    public void testInvariante_keinePositionsDuplikateInSpielpaaren() {
        var plan = KaskadenKoRundenPlaner.berechne(34, 2);
        for (var runde : plan.kaskadeRunden()) {
            for (var gruppe : runde.gruppenRunden()) {
                var allePositionen = gruppe.spielPaare().stream()
                        .flatMap(p -> java.util.stream.Stream.of(p.positionA(), p.positionB()))
                        .toList();
                assertThat(allePositionen)
                        .as("Keine Positions-Duplikate in Gruppe pfad='%s'", gruppe.pfad())
                        .doesNotHaveDuplicates();
            }
        }
    }

    // ---------------------------------------------------------------
    // Hilfsmethoden
    // ---------------------------------------------------------------

    private static void assertGruppenRunde(
            KaskadenKoGruppenRunde gruppe, String pfad, int anzTeams, int anzPaare, int anzFreilose) {
        assertThat(gruppe.pfad()).as("pfad").isEqualTo(pfad);
        assertThat(gruppe.anzTeams()).as("anzTeams").isEqualTo(anzTeams);
        assertThat(gruppe.spielPaare()).as("spielPaare.size").hasSize(anzPaare);
        assertThat(gruppe.anzFreilose()).as("anzFreilose").isEqualTo(anzFreilose);
    }

    private static void assertSpielPaar(KaskadenKoSpielPaar paar, int spielNr, int posA, int posB) {
        assertThat(paar.spielNr()).as("spielNr").isEqualTo(spielNr);
        assertThat(paar.positionA()).as("positionA").isEqualTo(posA);
        assertThat(paar.positionB()).as("positionB").isEqualTo(posB);
    }

    private static void assertInvariantenAlleRunden(KaskadenKoRundenPlan plan) {
        pruefeInvarianteAnzTeams(plan);
        pruefeInvarianteFreilos(plan);
    }

    private static void pruefeInvarianteAnzTeams(KaskadenKoRundenPlan plan) {
        for (var runde : plan.kaskadeRunden()) {
            for (var gruppe : runde.gruppenRunden()) {
                int erwartet = 2 * gruppe.spielPaare().size() + gruppe.anzFreilose();
                assertThat(gruppe.anzTeams())
                        .as("anzTeams == 2*spielPaare.size()+anzFreilose für pfad='%s'", gruppe.pfad())
                        .isEqualTo(erwartet);
            }
        }
    }

    private static void pruefeInvarianteFreilos(KaskadenKoRundenPlan plan) {
        for (var runde : plan.kaskadeRunden()) {
            for (var gruppe : runde.gruppenRunden()) {
                assertThat(gruppe.anzTeams() % 2)
                        .as("anzTeams %% 2 == anzFreilose für pfad='%s'", gruppe.pfad())
                        .isEqualTo(gruppe.anzFreilose());
            }
        }
    }
}
