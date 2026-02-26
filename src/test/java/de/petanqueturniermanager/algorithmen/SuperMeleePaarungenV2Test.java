/**
 * Erstellung : 2026-02-24 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
            pruefeMinTeamGroese(runde, 2);
            pruefeAlleSpielerInTeam(runde, meldungen);
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
            pruefeMinTeamGroese(runde, 2);
            pruefeAlleSpielerInTeam(runde, meldungen);
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
            pruefeMinTeamGroese(runde, 2);
            pruefeAlleSpielerInTeam(runde, meldungen);
            pruefeKeineWiederholtenTeamkombinationen(runde, paarHistorie);
        }
    }

    /**
     * 3 Runden mit 12 Spielern im Doublette-Modus über {@code neueSpielrundeDoubletteMode}.<br>
     * Bei 12 Spielern (gerade, durch 6 teilbar) werden intern 6 Dummy-Spieler ergänzt, sodass
     * 6 Dreier-Teams entstehen; nach Entfernung je eines Dummys pro Team bleiben 6 Doubletten.<br>
     * Prüft zusätzlich, dass {@code meldungen.size()} nach jeder Runde unverändert 12 ist
     * (Dummies vollständig entfernt).
     */
    @Test
    public void testMehrereRunden_DoubletteMode_12Spieler() throws AlgorithmenException {
        SpielerMeldungen meldungen = newTestMeldungen(12);
        Set<String> paarHistorie = new HashSet<>();
        for (int rnd = 1; rnd <= 3; rnd++) {
            MeleeSpielRunde runde = paarungen.neueSpielrundeDoubletteMode(rnd, meldungen, false);
            assertThat(runde).as("Runde %d", rnd).isNotNull();
            assertThat(runde.teams()).as("Runde %d: 6 Doubletten erwartet", rnd).hasSize(6);
            final int rndNr = rnd;
            runde.teams().forEach(t -> assertThat(t.size())
                    .as("Runde %d: nur Doubletten erwartet", rndNr).isEqualTo(2));
            pruefeKeineDoppeltenSpieler(runde);
            pruefeMinTeamGroese(runde, 2);
            pruefeAlleSpielerInTeam(runde, meldungen);
            pruefeKeineWiederholtenTeamkombinationen(runde, paarHistorie);
            assertThat(meldungen.size())
                    .as("Runde %d: Dummy-Spieler müssen nach der Runde entfernt sein", rnd)
                    .isEqualTo(12);
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
            pruefeMinTeamGroese(runde, 2);
            pruefeAlleSpielerInTeam(runde, meldungen);
            pruefeKeineWiederholtenTeamkombinationen(runde, paarHistorie);
        }
    }

    /**
     * Vollständige Doublette-Round-Robin mit 8 Spielern.<br>
     * <br>
     * Bei 8 Spielern (gerade, durch 4 teilbar) erzeugt {@code neueSpielrundeDoubletteMode}
     * pro Runde exakt 4 Doubletten. Intern werden 4 Dummy-Spieler ergänzt, sodass 4
     * Dreier-Teams entstehen; nach Entfernung der Dummies bleiben 4 Doubletten übrig.<br>
     * <br>
     * C(8,2) = 28 Paare, 7 Runden × 4 Paare = 28 → nach 7 Runden hat jeder Spieler
     * genau einmal mit jedem anderen zusammengespielt.<br>
     * Da jeder Spieler pro Runde exakt einen neuen Partner kennenlernt (kein reines
     * Real-Team), ist keine vorzeitige Partner-Erschöpfung möglich — der Backtracking-
     * Algorithmus löst jeden Schritt zuverlässig.
     */
    @Test
    public void testVollstaendigeRoundRobin_Doublette_8Spieler() throws AlgorithmenException {
        int anzSpieler = 8;
        int erwarteteRunden = 7; // C(8,2)=28 Paare / 4 Paare pro Runde
        SpielerMeldungen meldungen = newTestMeldungen(anzSpieler);
        Map<String, Integer> paarZaehler = new HashMap<>();

        for (int rnd = 1; rnd <= erwarteteRunden; rnd++) {
            MeleeSpielRunde runde = paarungen.neueSpielrundeDoubletteMode(rnd, meldungen, false);
            assertThat(runde).as("Runde %d", rnd).isNotNull();
            assertThat(runde.teams()).as("Runde %d: 4 Teams erwartet", rnd).hasSize(4);
            final int rndNr = rnd;
            runde.teams().forEach(t -> assertThat(t.size())
                    .as("Runde %d: nur Doubletten erwartet", rndNr).isEqualTo(2));
            pruefeKeineDoppeltenSpieler(runde);
            pruefeMinTeamGroese(runde, 2);
            pruefeAlleSpielerInTeam(runde, meldungen);
            zaehlePaare(runde, paarZaehler);
        }

        // Jedes der C(8,2)=28 Paare muss genau einmal gespielt haben
        int erwarteteGesamtPaare = anzSpieler * (anzSpieler - 1) / 2; // = 28
        assertThat(paarZaehler)
                .as("Anzahl eindeutiger Paare nach %d Runden", erwarteteRunden)
                .hasSize(erwarteteGesamtPaare);
        paarZaehler.forEach((paar, count) ->
                assertThat(count)
                        .as("Paar %s muss genau einmal gespielt haben", paar)
                        .isEqualTo(1));
    }

    /**
     * Mehrere Runden Triplette-Modus mit Doublette-Auffüllung für 11 Spieler.<br>
     * <br>
     * Bei 11 Spielern (ungerade, nicht durch 6 teilbar) erzeugt
     * {@code neueSpielrundeTripletteMode} pro Runde exakt 3 Tripletten + 1 Doublette
     * (= 10 Paare/Runde). Intern wird 1 Dummy-Spieler ergänzt, sodass 4 Dreier-Teams
     * entstehen; nach Entfernung des Dummys wird das betroffene Team zur Doublette.<br>
     * <br>
     * 4 Runden × 10 Paare = 40 Paare — kein Paar darf doppelt vorkommen.<br>
     * Da die maximale Partnerzahl nach 4 Runden ≤ 8 von 10 möglichen beträgt
     * (Grad ≤ 4 + k mit k ≤ 4 Triplette-Runden), ist keine vorzeitige
     * Partner-Erschöpfung möglich.
     */
    @Test
    public void testMehrereRunden_Triplette_MitDoublette_11Spieler() throws AlgorithmenException {
        int anzSpieler = 11;
        int testeRunden = 4; // Paare/Runde = 3×C(3,2) + 1×C(2,2) = 10
        SpielerMeldungen meldungen = newTestMeldungen(anzSpieler);
        Map<String, Integer> paarZaehler = new HashMap<>();

        for (int rnd = 1; rnd <= testeRunden; rnd++) {
            MeleeSpielRunde runde = paarungen.neueSpielrundeTripletteMode(rnd, meldungen, false);
            assertThat(runde).as("Runde %d", rnd).isNotNull();
            assertThat(runde.teams()).as("Runde %d: 4 Teams erwartet", rnd).hasSize(4);
            long anzTripletten = runde.teams().stream().filter(t -> t.size() == 3).count();
            long anzDoubletten = runde.teams().stream().filter(t -> t.size() == 2).count();
            assertThat(anzTripletten).as("Runde %d: 3 Tripletten erwartet", rnd).isEqualTo(3L);
            assertThat(anzDoubletten).as("Runde %d: 1 Doublette erwartet", rnd).isEqualTo(1L);
            pruefeKeineDoppeltenSpieler(runde);
            pruefeMinTeamGroese(runde, 2);
            pruefeAlleSpielerInTeam(runde, meldungen);
            zaehlePaare(runde, paarZaehler);
        }

        // 4 Runden × 10 Paare = 40 eindeutige Paare
        int erwarteteGesamtPaare = testeRunden * 10; // = 40
        assertThat(paarZaehler)
                .as("Anzahl eindeutiger Paare nach %d Runden", testeRunden)
                .hasSize(erwarteteGesamtPaare);
        paarZaehler.forEach((paar, count) ->
                assertThat(count)
                        .as("Paar %s muss genau einmal gespielt haben", paar)
                        .isEqualTo(1));
    }

    /**
     * Stellt sicher, dass Dummy-Spieler nach jeder Runde vollständig aus den Meldungen
     * entfernt werden — unabhängig davon, ob Doublette- oder Triplette-Modus verwendet wird.<br>
     * <br>
     * Bei 9 Spielern (DoubletteMode) werden intern 3 Dummies ergänzt (Nrn. 10000–10002).<br>
     * Bei 11 Spielern (TripletteMode) wird intern 1 Dummy ergänzt (Nr. 10000).<br>
     * Nach jedem {@code neueSpielrunde*}-Aufruf muss {@code meldungen.size()} unverändert sein.
     */
    @Test
    public void testDummySpielerNichtInMeldungenNachRunde() throws AlgorithmenException {
        // DoubletteMode mit 9 Spielern: intern 3 Dummies (10000, 10001, 10002)
        SpielerMeldungen meldungen9 = newTestMeldungen(9);
        assertThat(meldungen9.size()).isEqualTo(9);

        MeleeSpielRunde runde9a = paarungen.neueSpielrundeDoubletteMode(1, meldungen9, false);
        assertThat(runde9a).isNotNull();
        assertThat(meldungen9.size())
                .as("Nach DoubletteMode Runde 1: Dummies müssen entfernt sein")
                .isEqualTo(9);

        MeleeSpielRunde runde9b = paarungen.neueSpielrundeDoubletteMode(2, meldungen9, false);
        assertThat(runde9b).isNotNull();
        assertThat(meldungen9.size())
                .as("Nach DoubletteMode Runde 2: Dummies müssen entfernt sein")
                .isEqualTo(9);

        // TripletteMode mit 11 Spielern: intern 1 Dummy (10000)
        SpielerMeldungen meldungen11 = newTestMeldungen(11);
        assertThat(meldungen11.size()).isEqualTo(11);

        MeleeSpielRunde runde11a = paarungen.neueSpielrundeTripletteMode(1, meldungen11, false);
        assertThat(runde11a).isNotNull();
        assertThat(meldungen11.size())
                .as("Nach TripletteMode Runde 1: Dummy muss entfernt sein")
                .isEqualTo(11);

        MeleeSpielRunde runde11b = paarungen.neueSpielrundeTripletteMode(2, meldungen11, false);
        assertThat(runde11b).isNotNull();
        assertThat(meldungen11.size())
                .as("Nach TripletteMode Runde 2: Dummy muss entfernt sein")
                .isEqualTo(11);
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
    // SetzPos-Constraint
    // =========================================================================

    /**
     * Einfacher SetzPos-Test: 9 Spieler in 3 Gruppen à 3 (SetzPos 1/2/3).<br>
     * Kein Team darf zwei Spieler mit gleicher SetzPos enthalten.
     * Über eine Runde wird sichergestellt, dass der Constraint eingehalten wird.
     */
    @Test
    public void testSetzPosConstraint_eineRunde_keineGleicheSetzPosImTeam() throws AlgorithmenException {
        // je 3 Spieler mit SetzPos 1, 2, 3 — müssen auf verschiedene Teams verteilt werden
        SpielerMeldungen meldungen = new SpielerMeldungen();
        for (int setzPos = 1; setzPos <= 3; setzPos++) {
            for (int j = 0; j < 3; j++) {
                meldungen.addSpielerWennNichtVorhanden(
                        Spieler.from((setzPos - 1) * 3 + j + 1).setSetzPos(setzPos));
            }
        }

        MeleeSpielRunde runde = paarungen.generiereRundeMitFesteTeamGroese(1, 3, meldungen);
        assertThat(runde).isNotNull();
        assertThat(runde.teams()).hasSize(3);
        pruefeSetzPosConstraint(runde);
        pruefeKeineDoppeltenSpieler(runde);
        pruefeAlleSpielerInTeam(runde, meldungen);
    }

    /**
     * SetzPos-Test über mehrere Runden: 12 Spieler, davon 3 SetzPos-Paare.<br>
     * Spieler 1/2 (SetzPos 1), 3/4 (SetzPos 2), 5/6 (SetzPos 3) dürfen nie im selben
     * Team sein. Die restlichen 6 Spieler haben keine SetzPos-Einschränkung.
     * Der Constraint muss über alle Runden hinweg eingehalten werden.
     */
    @Test
    public void testSetzPosConstraint_mehrereRunden_keineGleicheSetzPosImTeam() throws AlgorithmenException {
        SpielerMeldungen meldungen = new SpielerMeldungen();
        meldungen.addSpielerWennNichtVorhanden(Spieler.from(1).setSetzPos(1));
        meldungen.addSpielerWennNichtVorhanden(Spieler.from(2).setSetzPos(1));
        meldungen.addSpielerWennNichtVorhanden(Spieler.from(3).setSetzPos(2));
        meldungen.addSpielerWennNichtVorhanden(Spieler.from(4).setSetzPos(2));
        meldungen.addSpielerWennNichtVorhanden(Spieler.from(5).setSetzPos(3));
        meldungen.addSpielerWennNichtVorhanden(Spieler.from(6).setSetzPos(3));
        for (int i = 7; i <= 12; i++) {
            meldungen.addSpielerWennNichtVorhanden(Spieler.from(i));
        }

        Set<String> paarHistorie = new HashSet<>();
        for (int rnd = 1; rnd <= 3; rnd++) {
            MeleeSpielRunde runde = paarungen.generiereRundeMitFesteTeamGroese(rnd, 3, meldungen);
            assertThat(runde).as("Runde %d", rnd).isNotNull();
            assertThat(runde.teams()).as("Runde %d: 4 Teams", rnd).hasSize(4);
            pruefeSetzPosConstraint(runde);
            pruefeKeineDoppeltenSpieler(runde);
            pruefeAlleSpielerInTeam(runde, meldungen);
            pruefeKeineWiederholtenTeamkombinationen(runde, paarHistorie);
        }
    }

    /**
     * SetzPos macht jede Paarung unmöglich: 4 Spieler alle mit SetzPos 1
     * können keine 2er-Teams bilden — kein Spieler darf mit einem anderen zusammen.<br>
     * V2 erkennt dies vollständig und wirft eine {@link AlgorithmenException}.
     */
    @Test
    public void testSetzPosConstraint_alleGleicheSetzPos_wirftException() {
        SpielerMeldungen meldungen = new SpielerMeldungen();
        for (int i = 1; i <= 4; i++) {
            meldungen.addSpielerWennNichtVorhanden(Spieler.from(i).setSetzPos(1));
        }
        assertThatThrownBy(() -> paarungen.generiereRundeMitFesteTeamGroese(1, 2, meldungen))
                .isInstanceOf(AlgorithmenException.class)
                .hasMessageContaining("ausgeschöpft");
    }

    // =========================================================================
    // Minimale Spielerzahl (Grenzfälle)
    // =========================================================================

    /**
     * Grenzfall: genau 2 Spieler → 1 Doublette (kleinstmögliche Eingabe für teamSize=2).
     */
    @Test
    public void testGeneriereRunde_2Spieler_1Doublette() throws AlgorithmenException {
        SpielerMeldungen meldungen = newTestMeldungen(2);
        MeleeSpielRunde runde = paarungen.generiereRundeMitFesteTeamGroese(1, 2, meldungen);
        assertThat(runde).isNotNull();
        assertThat(runde.teams()).hasSize(1);
        assertThat(runde.teams().get(0).size()).isEqualTo(2);
        pruefeKeineDoppeltenSpieler(runde);
        pruefeAlleSpielerInTeam(runde, meldungen);
    }

    /**
     * Grenzfall: genau 3 Spieler → 1 Triplette (kleinstmögliche Eingabe für teamSize=3).
     */
    @Test
    public void testGeneriereRunde_3Spieler_1Triplette() throws AlgorithmenException {
        SpielerMeldungen meldungen = newTestMeldungen(3);
        MeleeSpielRunde runde = paarungen.generiereRundeMitFesteTeamGroese(1, 3, meldungen);
        assertThat(runde).isNotNull();
        assertThat(runde.teams()).hasSize(1);
        assertThat(runde.teams().get(0).size()).isEqualTo(3);
        pruefeKeineDoppeltenSpieler(runde);
        pruefeAlleSpielerInTeam(runde, meldungen);
    }

    // =========================================================================
    // Vollständiges Triplette-Round-Robin
    // =========================================================================

    /**
     * Vollständiges Triplette-Round-Robin mit 9 Spielern.<br>
     * <br>
     * 9 Spieler in 3 Dreier-Teams: pro Runde werden 3 × C(3,2) = 9 Paare abgedeckt.<br>
     * C(9,2) = 36 mögliche Paare — 36 / 9 = 4 Runden für ein vollständiges Round-Robin.<br>
     * Nach 4 Runden hat jeder Spieler genau einmal mit jedem anderen zusammengespielt.<br>
     * Prüft, dass V2 eine vollständige Lösung ohne Paarwiederholung findet.
     */
    @Test
    public void testVollstaendigesRoundRobin_Triplette_9Spieler() throws AlgorithmenException {
        int anzSpieler = 9;
        int erwarteteRunden = 4; // C(9,2)=36 Paare / 9 Paare pro Runde
        SpielerMeldungen meldungen = newTestMeldungen(anzSpieler);
        Map<String, Integer> paarZaehler = new HashMap<>();

        for (int rnd = 1; rnd <= erwarteteRunden; rnd++) {
            MeleeSpielRunde runde = paarungen.generiereRundeMitFesteTeamGroese(rnd, 3, meldungen);
            assertThat(runde).as("Runde %d", rnd).isNotNull();
            assertThat(runde.teams()).as("Runde %d: 3 Teams erwartet", rnd).hasSize(3);
            final int rndNr = rnd;
            runde.teams().forEach(t -> assertThat(t.size())
                    .as("Runde %d: nur Tripletten erwartet", rndNr).isEqualTo(3));
            pruefeKeineDoppeltenSpieler(runde);
            pruefeAlleSpielerInTeam(runde, meldungen);
            zaehlePaare(runde, paarZaehler);
        }

        // Jedes der C(9,2)=36 Paare muss genau einmal gespielt haben
        int erwarteteGesamtPaare = anzSpieler * (anzSpieler - 1) / 2; // = 36
        assertThat(paarZaehler)
                .as("Anzahl eindeutiger Paare nach %d Runden", erwarteteRunden)
                .hasSize(erwarteteGesamtPaare);
        paarZaehler.forEach((paar, count) ->
                assertThat(count)
                        .as("Paar %s muss genau einmal gespielt haben", paar)
                        .isEqualTo(1));
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
        pruefeMinTeamGroese(runde1, 2);
        pruefeAlleSpielerInTeam(runde1, meldungen);

        // 3 neue Spieler dazukommen
        meldungen.addSpielerWennNichtVorhanden(Spieler.from(13));
        meldungen.addSpielerWennNichtVorhanden(Spieler.from(14));
        meldungen.addSpielerWennNichtVorhanden(Spieler.from(15));
        assertThat(meldungen.size()).isEqualTo(15);

        // Runde 2 mit 15 Spielern
        MeleeSpielRunde runde2 = paarungen.neueSpielrunde(2, meldungen);
        assertThat(runde2).isNotNull();
        pruefeKeineDoppeltenSpieler(runde2);
        pruefeMinTeamGroese(runde2, 2);
        pruefeAlleSpielerInTeam(runde2, meldungen);

        // 3 Spieler gehen wieder — manuell aus der Liste entfernen (simuliert Abmeldung)
        meldungen.removeSpieler(meldungen.findSpielerByNr(13));
        meldungen.removeSpieler(meldungen.findSpielerByNr(14));
        meldungen.removeSpieler(meldungen.findSpielerByNr(15));
        assertThat(meldungen.size()).isEqualTo(12);

        // Runde 3 mit wieder 12 Spielern
        MeleeSpielRunde runde3 = paarungen.neueSpielrunde(3, meldungen);
        assertThat(runde3).isNotNull();
        pruefeKeineDoppeltenSpieler(runde3);
        pruefeMinTeamGroese(runde3, 2);
        pruefeAlleSpielerInTeam(runde3, meldungen);
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

    /** Prüft, dass jedes Team der Spielrunde mindestens {@code minGroese} Spieler hat. */
    private void pruefeMinTeamGroese(MeleeSpielRunde spielRunde, int minGroese) {
        for (Team team : spielRunde.teams()) {
            assertThat(team.size())
                    .as("Team in Spielrunde %d muss mindestens %d Spieler haben",
                            spielRunde.getNr(), minGroese)
                    .isGreaterThanOrEqualTo(minGroese);
        }
    }

    /**
     * Prüft, dass jeder Spieler aus {@code meldungen} in der Spielrunde genau einem Team
     * zugeordnet ist — kein Spieler darf fehlen, und kein Spieler darf in mehreren Teams stehen.
     * Durch Verwendung einer Liste (statt Set) erkennt {@code containsExactlyInAnyOrderElementsOf}
     * auch doppelte Einträge als Fehler.
     */
    private void pruefeAlleSpielerInTeam(MeleeSpielRunde spielRunde, SpielerMeldungen meldungen) {
        List<Integer> inRunde = new ArrayList<>();
        for (Team team : spielRunde.teams()) {
            for (Spieler spieler : team.spieler()) {
                inRunde.add(spieler.getNr());
            }
        }
        List<Integer> erwartet = new ArrayList<>();
        for (Spieler spieler : meldungen.spieler()) {
            erwartet.add(spieler.getNr());
        }
        assertThat(inRunde)
                .as("Spielrunde %d: jeder Spieler muss genau einem Team zugeordnet sein",
                        spielRunde.getNr())
                .containsExactlyInAnyOrderElementsOf(erwartet);
    }

    /**
     * Zählt alle Spielerpaare der Spielrunde in {@code paarZaehler}.<br>
     * Der Schlüssel ist "min-max" (Spielernummer), der Wert die Häufigkeit.
     * Wird über mehrere Runden akkumuliert, um am Ende "genau einmal" zu prüfen.
     */
    private void zaehlePaare(MeleeSpielRunde spielRunde, Map<String, Integer> paarZaehler) {
        for (Team team : spielRunde.teams()) {
            List<Spieler> spielerImTeam = team.spieler();
            for (int i = 0; i < spielerImTeam.size(); i++) {
                for (int j = i + 1; j < spielerImTeam.size(); j++) {
                    int a = spielerImTeam.get(i).getNr();
                    int b = spielerImTeam.get(j).getNr();
                    String paar = Math.min(a, b) + "-" + Math.max(a, b);
                    paarZaehler.merge(paar, 1, Integer::sum);
                }
            }
        }
    }

    /**
     * Prüft, dass kein Team zwei Spieler mit gleicher SetzPos > 0 enthält.
     */
    private void pruefeSetzPosConstraint(MeleeSpielRunde spielRunde) {
        for (Team team : spielRunde.teams()) {
            List<Spieler> spielerImTeam = team.spieler();
            for (int i = 0; i < spielerImTeam.size(); i++) {
                for (int j = i + 1; j < spielerImTeam.size(); j++) {
                    Spieler a = spielerImTeam.get(i);
                    Spieler b = spielerImTeam.get(j);
                    if (a.getSetzPos() > 0 && b.getSetzPos() > 0) {
                        assertThat(a.getSetzPos())
                                .as("Spieler %d (SetzPos %d) und Spieler %d (SetzPos %d) "
                                        + "dürfen nicht im selben Team sein (Runde %d)",
                                        a.getNr(), a.getSetzPos(), b.getNr(), b.getSetzPos(),
                                        spielRunde.getNr())
                                .isNotEqualTo(b.getSetzPos());
                    }
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
