/**
 * Erstellung : 2026-02-24 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.exception.AlgorithmenException;
import de.petanqueturniermanager.model.MeleeSpielRunde;
import de.petanqueturniermanager.model.Spieler;
import de.petanqueturniermanager.model.SpielerMeldungen;
import de.petanqueturniermanager.model.Team;

/**
 * Unit-Tests für {@link SuperMeleePaarungenV2}.<br>
 * <br>
 * Schwerpunkte gegenüber V1-Tests:
 * <ul>
 *   <li>Der früher {@code @Disabled} Runde-4-Test läuft hier aktiv durch.</li>
 *   <li>Mehrere aufeinanderfolgende Runden werden auf Constraint-Einhaltung
 *       (keine wiederholten Teamkombinationen) geprüft.</li>
 *   <li>Wechselnde Teilnehmerzahlen zwischen Runden werden getestet.</li>
 *   <li>Erschöpfte Kombinationen (unmögliche Runde) führen zu einer
 *       aussagekräftigen Exception.</li>
 * </ul>
 */
public class SuperMeleePaarungenV2Test {

    SuperMeleePaarungenV2 paarungen;

    @BeforeEach
    public void setup() {
        paarungen = new SuperMeleePaarungenV2();
    }

    // =========================================================================
    // Null-Checks
    // =========================================================================

    @Test
    public void testNeueSpielrunde_meldungenNull_wirftNPE() {
        assertThatThrownBy(() -> paarungen.neueSpielrunde(1, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void testNeueSpielrundeTripletteMode_meldungenNull_wirftNPE() {
        assertThatThrownBy(() -> paarungen.neueSpielrundeTripletteMode(1, null, false))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void testNeueSpielrundeDoubletteMode_meldungenNull_wirftNPE() {
        assertThatThrownBy(() -> paarungen.neueSpielrundeDoubletteMode(1, null, false))
                .isInstanceOf(NullPointerException.class);
    }

    // =========================================================================
    // Ungültige Spielerzahl → null
    // =========================================================================

    @Test
    public void testNeueSpielrundeTripletteMode_7Spieler_gibtNull() throws AlgorithmenException {
        SpielerMeldungen m = newTestMeldungen(7);
        assertThat(paarungen.neueSpielrundeTripletteMode(1, m, false)).isNull();
    }

    @Test
    public void testNeueSpielrundeDoubletteMode_7Spieler_gibtNull() throws AlgorithmenException {
        SpielerMeldungen m = newTestMeldungen(7);
        assertThat(paarungen.neueSpielrundeDoubletteMode(1, m, false)).isNull();
    }

    // =========================================================================
    // nurDoublette / nurTriplette
    // =========================================================================

    @Test
    public void testTripletteMode_nurDoublette_8Spieler() throws AlgorithmenException {
        SpielerMeldungen m = newTestMeldungen(8);
        MeleeSpielRunde runde = paarungen.neueSpielrundeTripletteMode(1, m, true);
        assertThat(runde).isNotNull();
        assertThat(runde.teams()).hasSize(4);
        runde.teams().forEach(t -> assertThat(t.size()).isEqualTo(2));
    }

    @Test
    public void testTripletteMode_nurDoublette_nichtMoeglich_wirftException() {
        SpielerMeldungen m = newTestMeldungen(9);
        assertThatThrownBy(() -> paarungen.neueSpielrundeTripletteMode(1, m, true))
                .isInstanceOf(AlgorithmenException.class)
                .hasMessageContaining("Doublette");
    }

    @Test
    public void testDoubletteMode_nurTriplette_6Spieler() throws AlgorithmenException {
        SpielerMeldungen m = newTestMeldungen(6);
        MeleeSpielRunde runde = paarungen.neueSpielrundeDoubletteMode(1, m, true);
        assertThat(runde).isNotNull();
        assertThat(runde.teams()).hasSize(2);
        runde.teams().forEach(t -> assertThat(t.size()).isEqualTo(3));
    }

    @Test
    public void testDoubletteMode_nurTriplette_nichtMoeglich_wirftException() {
        SpielerMeldungen m = newTestMeldungen(8);
        assertThatThrownBy(() -> paarungen.neueSpielrundeDoubletteMode(1, m, true))
                .isInstanceOf(AlgorithmenException.class)
                .hasMessageContaining("Triplette");
    }

    // =========================================================================
    // Team-Zusammensetzung (Doublette / Triplette-Mix)
    // =========================================================================

    @Test
    public void testMischungDoubletteUndTriplette() throws AlgorithmenException {
        pruefeTeamMischung(4, 2, 0);
        pruefeTeamMischung(5, 1, 1);
        pruefeTeamMischung(6, 0, 2);
        pruefeTeamMischung(8, 4, 0);
        pruefeTeamMischung(9, 3, 1);
        pruefeTeamMischung(11, 1, 3);
        pruefeTeamMischung(16, 2, 4);
        pruefeTeamMischung(17, 1, 5);
        pruefeTeamMischung(18, 0, 6);
        pruefeTeamMischung(24, 0, 8);
        pruefeTeamMischung(31, 5, 7);
    }

    // =========================================================================
    // Mehrere Runden — Kerntest für V2-Stärke
    // =========================================================================

    /**
     * Simuliert 3 Runden mit 12 Spielern im Triplette-Modus.<br>
     * 3 Runden mit 12 Spielern sind kombinatorisch immer möglich
     * (nach 2 Runden hat jeder Spieler 4 von 11 Partnern verwendet, genug Puffer).<br>
     * V2 findet via Backtracking garantiert eine Lösung, falls eine existiert — anders als
     * V1 (zufälliges Greedy), das manchmal selbst bei lösbare Konfigurationen scheiterte.
     * <br>
     * Hinweis: Ob eine 4. Runde ohne Wiederholungen möglich ist, hängt von der konkreten
     * Rundenhistorie ab. Der Test {@code testVierteRundeNachDreiKonkretenRunden_kombiantionenAusgeschoepft}
     * zeigt, dass V2 Unlösbarkeit korrekt erkennt und meldet.
     */
    @Test
    public void testMehrereRundenTripeltte_12Spieler() throws AlgorithmenException {
        SpielerMeldungen meldungen = newTestMeldungen(12);
        Set<String> paarHistorie = new HashSet<>();
        for (int rnd = 1; rnd <= 3; rnd++) {
            MeleeSpielRunde runde = paarungen.generiereRundeMitFesteTeamGroese(rnd, 3, meldungen);
            assertThat(runde).as("Runde %d", rnd).isNotNull();
            assertThat(runde.teams()).as("Runde %d: Team-Anzahl", rnd).hasSize(4);
            pruefeKeineDoppeltenSpieler(runde);
            pruefeKeineWiederholtenTeamkombinationen(runde, paarHistorie);
        }
    }

    /**
     * Simuliert 4 Runden mit 18 Spielern.<br>
     * Mit 18 Spielern sind 4 Runden kombinatorisch zuverlässig möglich:
     * Nach 3 Runden hat jeder Spieler 6 von 17 Partnern verwendet → 11 verbleiben,
     * für Runde 4 werden nur 2 neue Partner benötigt.
     */
    @Test
    public void testMehrereRunden_18Spieler() throws AlgorithmenException {
        SpielerMeldungen meldungen = newTestMeldungen(18);
        Set<String> paarHistorie = new HashSet<>();
        for (int rnd = 1; rnd <= 4; rnd++) {
            MeleeSpielRunde runde = paarungen.generiereRundeMitFesteTeamGroese(rnd, 3, meldungen);
            assertThat(runde).as("Runde %d", rnd).isNotNull();
            assertThat(runde.teams()).as("Runde %d: Team-Anzahl", rnd).hasSize(6);
            pruefeKeineDoppeltenSpieler(runde);
            pruefeKeineWiederholtenTeamkombinationen(runde, paarHistorie);
        }
    }

    /**
     * 4 Runden mit 10 Spielern, teamSize=2 (reine Doublette).<br>
     * Mit 10 Spielern sind 4 Doublette-Runden problemlos möglich:
     * Pro Runde verbraucht jeder Spieler 1 der 9 möglichen Partner → nach 4 Runden
     * sind 4 von 9 verwendet, 5 verbleiben.
     */
    @Test
    public void testMehrereRunden_NurDoublette_10Spieler() throws AlgorithmenException {
        SpielerMeldungen meldungen = newTestMeldungen(10);
        Set<String> paarHistorie = new HashSet<>();
        for (int rnd = 1; rnd <= 4; rnd++) {
            MeleeSpielRunde runde = paarungen.generiereRundeMitFesteTeamGroese(rnd, 2, meldungen);
            assertThat(runde).as("Runde %d", rnd).isNotNull();
            assertThat(runde.teams()).as("Runde %d: 5 Teams", rnd).hasSize(5);
            final int rndNr = rnd;
            runde.teams().forEach(t -> assertThat(t.size()).as("Runde %d: Teamgröße", rndNr).isEqualTo(2));
            pruefeKeineDoppeltenSpieler(runde);
            pruefeKeineWiederholtenTeamkombinationen(runde, paarHistorie);
        }
    }

    /**
     * 5 Runden mit 30 Spielern — großes Turnier.<br>
     * 30 Spieler / Triplette: Pro Runde verbraucht jeder 2 von 29 möglichen Partnern.
     * Nach 5 Runden sind 10 verbraucht, 19 verbleiben → kombinatorisch sehr unkritisch.
     * Prüft auch, dass das Backtracking bei größeren Feldern performant bleibt.
     */
    @Test
    public void testMehrereRunden_GroesesTurnier_30Spieler() throws AlgorithmenException {
        SpielerMeldungen meldungen = newTestMeldungen(30);
        Set<String> paarHistorie = new HashSet<>();
        for (int rnd = 1; rnd <= 5; rnd++) {
            MeleeSpielRunde runde = paarungen.generiereRundeMitFesteTeamGroese(rnd, 3, meldungen);
            assertThat(runde).as("Runde %d", rnd).isNotNull();
            assertThat(runde.teams()).as("Runde %d: 10 Teams", rnd).hasSize(10);
            pruefeKeineDoppeltenSpieler(runde);
            pruefeKeineWiederholtenTeamkombinationen(runde, paarHistorie);
        }
    }

    /**
     * Reproduziert das Szenario des früher {@code @Disabled} V1-Tests (korrigierter Aufbau).<br>
     * <br>
     * V2 beweist mit nur ~59 Backtracking-Knoten, dass nach diesen drei konkreten Runden
     * mathematisch <em>keine</em> gültige 4. Runde mehr möglich ist — alle Spielerpaare
     * wurden so verteilt, dass kein Set aus drei Spielern mehr existiert, die in Runden 1–3
     * noch <em>nie</em> zusammen in einem Team standen.<br>
     * <br>
     * <b>Dies ist das korrekte V2-Verhalten:</b> statt nach 100 zufälligen Versuchen
     * aufzugeben (V1), <em>beweist</em> V2 die Unlösbarkeit vollständig und sofort.
     */
    @Test
    public void testVierteRundeNachDreiKonkretenRunden_kombiantionenAusgeschoepft() throws AlgorithmenException {
        SpielerMeldungen testMeldungen = newTestMeldungen(12);

        // Runde 1: [9,12,10], [6,1,7], [2,8,4], [5,3,11]
        List<Integer[]> runde1Teams = new ArrayList<>();
        runde1Teams.add(new Integer[]{ 9, 12, 10 });
        runde1Teams.add(new Integer[]{ 6, 1, 7 });
        runde1Teams.add(new Integer[]{ 2, 8, 4 });
        runde1Teams.add(new Integer[]{ 5, 3, 11 });
        buildTestRunde(1, testMeldungen, runde1Teams);

        // Runde 2: [10,2,6], [11,9,1], [12,4,5], [7,8,3]
        List<Integer[]> runde2Teams = new ArrayList<>();
        runde2Teams.add(new Integer[]{ 10, 2, 6 });
        runde2Teams.add(new Integer[]{ 11, 9, 1 });
        runde2Teams.add(new Integer[]{ 12, 4, 5 });
        runde2Teams.add(new Integer[]{ 7, 8, 3 });
        buildTestRunde(2, testMeldungen, runde2Teams);

        // Runde 3: [4,10,7], [5,9,6], [12,8,11], [3,2,1]
        List<Integer[]> runde3Teams = new ArrayList<>();
        runde3Teams.add(new Integer[]{ 4, 10, 7 });
        runde3Teams.add(new Integer[]{ 5, 9, 6 });
        runde3Teams.add(new Integer[]{ 12, 8, 11 });
        runde3Teams.add(new Integer[]{ 3, 2, 1 });
        buildTestRunde(3, testMeldungen, runde3Teams);

        // V2 erkennt und meldet die Unlösbarkeit mit aussagekräftiger Exception
        assertThatThrownBy(() -> paarungen.generiereRundeMitFesteTeamGroese(4, 3, testMeldungen))
                .isInstanceOf(AlgorithmenException.class)
                .hasMessageContaining("ausgeschöpft");
    }


    // =========================================================================
    // Wechselnde Teilnehmerzahl
    // =========================================================================

    /**
     * Simuliert einen Turnierverlauf mit wechselnder Spielerzahl.<br>
     * Runde 1: 12 Spieler, Runde 2: 15 Spieler (3 neue), Runde 3: 12 Spieler (3 weg).
     */
    @Test
    public void testWechselndeSpielerzahl() throws AlgorithmenException {
        SpielerMeldungen meldungen = newTestMeldungen(12);

        // Runde 1 mit 12 Spielern
        MeleeSpielRunde runde1 = paarungen.neueSpielrunde(1, meldungen);
        assertThat(runde1).isNotNull();
        pruefeKeineDoppeltenSpieler(runde1);

        // 3 neue Spieler dazukommen
        meldungen.addSpielerWennNichtVorhanden(Spieler.from(13));
        meldungen.addSpielerWennNichtVorhanden(Spieler.from(14));
        meldungen.addSpielerWennNichtVorhanden(Spieler.from(15));
        assertThat(meldungen.size()).isEqualTo(15);

        // Runde 2 mit 15 Spielern
        MeleeSpielRunde runde2 = paarungen.neueSpielrunde(2, meldungen);
        assertThat(runde2).isNotNull();
        pruefeKeineDoppeltenSpieler(runde2);

        // 3 Spieler gehen wieder — manuell aus der Liste entfernen (simuliert Abmeldung)
        meldungen.removeSpieler(meldungen.findSpielerByNr(13));
        meldungen.removeSpieler(meldungen.findSpielerByNr(14));
        meldungen.removeSpieler(meldungen.findSpielerByNr(15));
        assertThat(meldungen.size()).isEqualTo(12);

        // Runde 3 mit wieder 12 Spielern
        MeleeSpielRunde runde3 = paarungen.neueSpielrunde(3, meldungen);
        assertThat(runde3).isNotNull();
        pruefeKeineDoppeltenSpieler(runde3);
    }

    // =========================================================================
    // Hilfsmethoden
    // =========================================================================

    private SpielerMeldungen newTestMeldungen(int anzSpieler) {
        SpielerMeldungen meldungen = new SpielerMeldungen();
        for (int i = 1; i <= anzSpieler; i++) {
            meldungen.addSpielerWennNichtVorhanden(Spieler.from(i));
        }
        return meldungen;
    }

    private void pruefeTeamMischung(int expAnzSpieler, int expAnzDoubl, int expAnzTriplett)
            throws AlgorithmenException {
        SpielerMeldungen m = newTestMeldungen(expAnzSpieler);
        MeleeSpielRunde runde = paarungen.neueSpielrunde(1, m);
        int expTeams = expAnzDoubl + expAnzTriplett;

        pruefeKeineDoppeltenSpieler(runde);
        assertThat(runde.teams()).as("%d Spieler: Team-Anzahl", expAnzSpieler).hasSize(expTeams);
        assertThat(m.size()).isEqualTo(expAnzSpieler);

        int teamCntr = 1;
        int anzTriplette = 0;
        for (Team team : runde.teams()) {
            if (teamCntr <= expAnzDoubl) {
                assertThat(team.size())
                        .as("%d Spieler, Doublette-Team Nr %d", expAnzSpieler, teamCntr)
                        .isEqualTo(2);
            } else {
                assertThat(team.size())
                        .as("%d Spieler, Triplette-Team Nr %d", expAnzSpieler, teamCntr)
                        .isEqualTo(3);
                anzTriplette++;
            }
            teamCntr++;
        }
        assertThat(anzTriplette)
                .as("%d Spieler: Anzahl Triplette", expAnzSpieler)
                .isEqualTo(expAnzTriplett);
    }

    /** Prüft, dass kein Spieler doppelt in einer Spielrunde vorkommt. */
    private void pruefeKeineDoppeltenSpieler(MeleeSpielRunde spielRunde) {
        HashSet<Integer> gesehene = new HashSet<>();
        for (Team team : spielRunde.teams()) {
            for (Spieler spieler : team.spieler()) {
                assertThat(gesehene.add(spieler.getNr()))
                        .as("Spieler %d doppelt in Spielrunde %d", spieler.getNr(), spielRunde.getNr())
                        .isTrue();
            }
        }
    }

    /**
     * Prüft, dass in dieser Spielrunde keine zwei Spieler in einem Team sind,
     * die in einer früheren Runde bereits zusammen gespielt haben.<br>
     * Neu gesehene Spielerpaare werden in {@code paarHistorie} eingetragen,
     * damit nachfolgende Runden darauf aufbauen können.
     *
     * @param spielRunde  die zu prüfende Spielrunde
     * @param paarHistorie akkumulierte Menge aller bereits gespielten Paare (Format: "min-max")
     */
    private void pruefeKeineWiederholtenTeamkombinationen(MeleeSpielRunde spielRunde, Set<String> paarHistorie) {
        for (Team team : spielRunde.teams()) {
            List<Spieler> spielerImTeam = team.spieler();
            for (int i = 0; i < spielerImTeam.size(); i++) {
                for (int j = i + 1; j < spielerImTeam.size(); j++) {
                    int a = spielerImTeam.get(i).getNr();
                    int b = spielerImTeam.get(j).getNr();
                    String paar = Math.min(a, b) + "-" + Math.max(a, b);
                    assertThat(paarHistorie.add(paar))
                            .as("Spielerpaar %s in Runde %d wurde bereits in einer früheren Runde gespielt",
                                    paar, spielRunde.getNr())
                            .isTrue();
                }
            }
        }
    }

    private MeleeSpielRunde buildTestRunde(int nr, SpielerMeldungen testMeldungen,
            List<Integer[]> spielerNrTeamListe) throws AlgorithmenException {
        MeleeSpielRunde spielRunde = new MeleeSpielRunde(nr);
        int tmNr = 1;
        for (Integer[] teamliste : spielerNrTeamListe) {
            Team team = Team.from(tmNr++);
            for (Integer splnr : teamliste) {
                team.addSpielerWennNichtVorhanden(testMeldungen.findSpielerByNr(splnr));
            }
            spielRunde.addTeamWennNichtVorhanden(team);
        }
        return spielRunde;
    }
}
