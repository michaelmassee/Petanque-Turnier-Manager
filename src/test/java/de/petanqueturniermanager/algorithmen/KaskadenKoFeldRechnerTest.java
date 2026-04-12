package de.petanqueturniermanager.algorithmen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

public class KaskadenKoFeldRechnerTest {

    // ---------------------------------------------------------------
    // Exception-Tests
    // ---------------------------------------------------------------

    @Test
    public void testBerechne_gesamtTeamsZuKlein_wirftIllegalArgumentException() {
        assertThatThrownBy(() -> KaskadenKoFeldRechner.berechne(1, 2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> KaskadenKoFeldRechner.berechne(0, 2))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testBerechne_kaskadenStufenUngueltig_wirftIllegalArgumentException() {
        assertThatThrownBy(() -> KaskadenKoFeldRechner.berechne(16, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> KaskadenKoFeldRechner.berechne(16, 4))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---------------------------------------------------------------
    // k=1: 2 Felder
    // ---------------------------------------------------------------

    @Test
    public void testBerechne_3Teams_1Kaskade_minimalfall() {
        // A=1 (Sieger, kein Spiel), B=2 (Verlierer, keine Cadrage)
        var felder = KaskadenKoFeldRechner.berechne(3, 1);
        assertThat(felder).hasSize(2);
        assertFeld(felder.get(0), "A", "S", 1);
        assertFeld(felder.get(1), "B", "V", 2);

        // A hat 1 Team → kein CadrageRechner
        assertThat(felder.get(0).cadrageRechner()).isEmpty();
        // B hat 2 Teams → keine Cadrage nötig (2 ist Zweierpotenz)
        assertThat(felder.get(1).cadrageRechner()).isPresent();
        assertThat(felder.get(1).isCadrageNoetig()).isFalse();
    }

    @Test
    public void testBerechne_4Teams_1Kaskade() {
        var felder = KaskadenKoFeldRechner.berechne(4, 1);
        assertThat(felder).hasSize(2);
        assertFeld(felder.get(0), "A", "S", 2);
        assertFeld(felder.get(1), "B", "V", 2);
        assertInvarianten(felder, 4, 1);
    }

    // ---------------------------------------------------------------
    // k=2: 4 Felder – alle Beispiele aus der Spezifikation
    // ---------------------------------------------------------------

    @Test
    public void testBerechne_16Teams_2Kaskaden_alleZweierpotenzen() {
        var felder = KaskadenKoFeldRechner.berechne(16, 2);
        assertThat(felder).hasSize(4);
        assertFeld(felder.get(0), "A", "SS", 4);
        assertFeld(felder.get(1), "B", "SV", 4);
        assertFeld(felder.get(2), "C", "VS", 4);
        assertFeld(felder.get(3), "D", "VV", 4);

        // keine Cadrage nötig (alle Felder haben 4 = 2^2)
        felder.forEach(f -> assertThat(f.isCadrageNoetig()).isFalse());
        assertInvarianten(felder, 16, 2);
    }

    @Test
    public void testBerechne_24Teams_2Kaskaden_cadrageJeFeld() {
        // Pro Feld 6 Teams → Cadrage auf 4 (4 Cadrage-Teams, 2 Spiele)
        var felder = KaskadenKoFeldRechner.berechne(24, 2);
        assertThat(felder).hasSize(4);
        assertFeld(felder.get(0), "A", "SS", 6);
        assertFeld(felder.get(1), "B", "SV", 6);
        assertFeld(felder.get(2), "C", "VS", 6);
        assertFeld(felder.get(3), "D", "VV", 6);

        felder.forEach(f -> {
            assertThat(f.isCadrageNoetig()).isTrue();
            assertThat(f.anzCadrageSpiele()).isEqualTo(2);
            assertThat(f.zielTeams()).isEqualTo(4);
        });
        assertInvarianten(felder, 24, 2);
    }

    @Test
    public void testBerechne_28Teams_2Kaskaden_7erBloeocke() {
        // Pro Feld 7 Teams → Cadrage auf 4 (6 Cadrage-Teams, 3 Spiele)
        var felder = KaskadenKoFeldRechner.berechne(28, 2);
        assertThat(felder).hasSize(4);
        assertFeld(felder.get(0), "A", "SS", 7);
        assertFeld(felder.get(1), "B", "SV", 7);
        assertFeld(felder.get(2), "C", "VS", 7);
        assertFeld(felder.get(3), "D", "VV", 7);

        felder.forEach(f -> {
            assertThat(f.isCadrageNoetig()).isTrue();
            assertThat(f.anzCadrageSpiele()).isEqualTo(3);
            assertThat(f.zielTeams()).isEqualTo(4);
        });
        assertInvarianten(felder, 28, 2);
    }

    @Test
    public void testBerechne_30Teams_2Kaskaden_gemischteCadrage() {
        // A=7, B=8, C=7, D=8 – A/C brauchen Cadrage, B/D nicht
        var felder = KaskadenKoFeldRechner.berechne(30, 2);
        assertThat(felder).hasSize(4);
        assertFeld(felder.get(0), "A", "SS", 7);
        assertFeld(felder.get(1), "B", "SV", 8);
        assertFeld(felder.get(2), "C", "VS", 7);
        assertFeld(felder.get(3), "D", "VV", 8);

        assertThat(felder.get(0).isCadrageNoetig()).isTrue();
        assertThat(felder.get(1).isCadrageNoetig()).isFalse();
        assertThat(felder.get(2).isCadrageNoetig()).isTrue();
        assertThat(felder.get(3).isCadrageNoetig()).isFalse();
        assertInvarianten(felder, 30, 2);
    }

    @Test
    public void testBerechne_34Teams_2Kaskaden_bUndDMitCadrage() {
        // A=8, B=9, C=8, D=9 – A/C direkt KO, B/D je 1 Cadrage-Spiel
        var felder = KaskadenKoFeldRechner.berechne(34, 2);
        assertThat(felder).hasSize(4);
        assertFeld(felder.get(0), "A", "SS", 8);
        assertFeld(felder.get(1), "B", "SV", 9);
        assertFeld(felder.get(2), "C", "VS", 8);
        assertFeld(felder.get(3), "D", "VV", 9);

        assertThat(felder.get(0).isCadrageNoetig()).isFalse();
        assertThat(felder.get(1).isCadrageNoetig()).isTrue();
        assertThat(felder.get(1).anzCadrageSpiele()).isEqualTo(1);
        assertThat(felder.get(2).isCadrageNoetig()).isFalse();
        assertThat(felder.get(3).isCadrageNoetig()).isTrue();
        assertThat(felder.get(3).anzCadrageSpiele()).isEqualTo(1);
        assertInvarianten(felder, 34, 2);
    }

    @Test
    public void testBerechne_50Teams_2Kaskaden_maximaleKomplexitaet() {
        // A=12, B=13, C=12, D=13
        var felder = KaskadenKoFeldRechner.berechne(50, 2);
        assertThat(felder).hasSize(4);
        assertFeld(felder.get(0), "A", "SS", 12);
        assertFeld(felder.get(1), "B", "SV", 13);
        assertFeld(felder.get(2), "C", "VS", 12);
        assertFeld(felder.get(3), "D", "VV", 13);

        // B und D: 13 Teams → Ziel 8, 10 Cadrage-Teams, 5 Spiele
        assertThat(felder.get(1).anzCadrageSpiele()).isEqualTo(5);
        assertThat(felder.get(1).zielTeams()).isEqualTo(8);
        assertThat(felder.get(3).anzCadrageSpiele()).isEqualTo(5);
        assertInvarianten(felder, 50, 2);
    }

    // ---------------------------------------------------------------
    // k=3: 8 Felder
    // ---------------------------------------------------------------

    @Test
    public void testBerechne_34Teams_3Kaskaden() {
        var felder = KaskadenKoFeldRechner.berechne(34, 3);
        assertThat(felder).hasSize(8);

        // Pfade und Bezeichner
        assertThat(felder.get(0).pfad()).isEqualTo("SSS");
        assertThat(felder.get(0).bezeichner()).isEqualTo("A");
        assertThat(felder.get(7).pfad()).isEqualTo("VVV");
        assertThat(felder.get(7).bezeichner()).isEqualTo("H");

        // Teamanzahlen (aus 34 über k=2: [8,9,8,9], dann nochmals geteilt)
        // A=4,B=4,C=4,D=5,E=4,F=4,G=4,H=5
        int[] erwartet = {4, 4, 4, 5, 4, 4, 4, 5};
        for (int i = 0; i < 8; i++) {
            assertThat(felder.get(i).gesamtTeams())
                    .as("Feld %s (Pfad %s)", felder.get(i).bezeichner(), felder.get(i).pfad())
                    .isEqualTo(erwartet[i]);
        }
        assertInvarianten(felder, 34, 3);
    }

    // ---------------------------------------------------------------
    // Invarianten
    // ---------------------------------------------------------------

    @Test
    public void testBerechne_summeFelderGleichGesamtTeams_fuerAlleBeispiele() {
        // Für verschiedene Teamanzahlen und Stufen muss die Summe immer stimmen
        for (int k = 1; k <= 3; k++) {
            for (int n = 2; n <= 64; n++) {
                var felder = KaskadenKoFeldRechner.berechne(n, k);
                int summe = felder.stream().mapToInt(KaskadenKoFeldInfo::gesamtTeams).sum();
                assertThat(summe)
                        .as("Summe der Felder muss %d ergeben fuer k=%d", n, k)
                        .isEqualTo(n);
            }
        }
    }

    @Test
    public void testBerechne_pfadlaengGleichKaskadenStufen() {
        for (int k = 1; k <= 3; k++) {
            var felder = KaskadenKoFeldRechner.berechne(16, k);
            for (var feld : felder) {
                assertThat(feld.pfad())
                        .as("Pfadlaenge fuer Feld %s muss %d sein", feld.bezeichner(), k)
                        .hasSize(k);
            }
        }
    }

    @Test
    public void testBerechne_anzahlFelderGleich2HochK() {
        assertThat(KaskadenKoFeldRechner.berechne(16, 1)).hasSize(2);
        assertThat(KaskadenKoFeldRechner.berechne(16, 2)).hasSize(4);
        assertThat(KaskadenKoFeldRechner.berechne(16, 3)).hasSize(8);
    }

    @Test
    public void testBerechne_bezeichnerSindSequenziellAZ() {
        var bezeichner2 = KaskadenKoFeldRechner.berechne(16, 2).stream()
                .map(KaskadenKoFeldInfo::bezeichner).toList();
        assertThat(bezeichner2).containsExactly("A", "B", "C", "D");

        var bezeichner3 = KaskadenKoFeldRechner.berechne(16, 3).stream()
                .map(KaskadenKoFeldInfo::bezeichner).toList();
        assertThat(bezeichner3).containsExactly("A", "B", "C", "D", "E", "F", "G", "H");
    }

    // ---------------------------------------------------------------
    // Hilfsmethoden
    // ---------------------------------------------------------------

    private static void assertFeld(KaskadenKoFeldInfo feld, String bezeichner, String pfad, int gesamtTeams) {
        assertThat(feld.bezeichner()).as("bezeichner").isEqualTo(bezeichner);
        assertThat(feld.pfad()).as("pfad").isEqualTo(pfad);
        assertThat(feld.gesamtTeams()).as("gesamtTeams").isEqualTo(gesamtTeams);
    }

    private static void assertInvarianten(List<KaskadenKoFeldInfo> felder, int gesamtTeams, int kaskadenStufen) {
        int summe = felder.stream().mapToInt(KaskadenKoFeldInfo::gesamtTeams).sum();
        assertThat(summe).as("Summe der Feldgroessen muss gesamtTeams ergeben").isEqualTo(gesamtTeams);
        felder.forEach(f -> assertThat(f.pfad())
                .as("Pfadlaenge muss kaskadenStufen entsprechen")
                .hasSize(kaskadenStufen));
    }
}
