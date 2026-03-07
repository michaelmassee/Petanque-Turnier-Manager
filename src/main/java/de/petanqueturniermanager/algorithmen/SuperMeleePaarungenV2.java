/**
 * Erstellung : 2026-02-24 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.annotations.VisibleForTesting;

import de.petanqueturniermanager.exception.AlgorithmenException;
import de.petanqueturniermanager.model.MeleeSpielRunde;
import de.petanqueturniermanager.model.Spieler;
import de.petanqueturniermanager.model.SpielerMeldungen;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.supermelee.SuperMeleeTeamRechner;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeMode;

/**
 * V2: Robuste SuperMelee-Paarungsgenerierung via vollständigem Backtracking.<br>
 * <br>
 * V1 verwendet zufälliges Shuffle + Greedy-Zuweisung + bis zu 100 Neuversuche.
 * Dieser Ansatz kann scheitern, obwohl eine gültige Lösung existiert — besonders
 * in späten Runden mit dichter Spielhistorie.<br>
 * <br>
 * V2 ersetzt das durch einen <b>vollständigen Backtracking-Algorithmus</b> mit:
 * <ul>
 *   <li><b>MCV-Heuristik (Most Constrained Variable)</b>: Spieler mit den meisten
 *       historischen Teamkollegen werden zuerst platziert. Dadurch werden Sackgassen
 *       früh erkannt, bevor viel Arbeit in einem ungültigen Ast investiert wurde.</li>
 *   <li><b>Forward-Checking</b>: Nach jeder Zuweisung wird geprüft, ob noch mindestens
 *       ein gültiger Team-Slot für jeden verbleibenden Spieler existiert. Sackgassen
 *       werden so ohne vollständige Tiefensuche abgeschnitten.</li>
 *   <li><b>Symmetriebrechung</b>: Leere Teams sind strukturell äquivalent. Pro
 *       Rekursionsebene wird nur das erste leere Team versucht, was den Suchraum
 *       erheblich reduziert.</li>
 *   <li><b>Randomisierung</b>: Die Spielerliste wird vor der MCV-Sortierung gemischt.
 *       Gleichrangige Spieler werden so zufällig geordnet, sodass jede Runde eine
 *       andere gültige Paarung liefern kann.</li>
 * </ul>
 * <br>
 * <b>Performance (aus V3 übernommen):</b> Constraints werden als {@code boolean[][]}-Adjazenz-Matrix
 * vorberechnet — einmalig pro Shuffle-Versuch in einem O(n²)-Durchlauf, der gleichzeitig die
 * MCV-Grade berechnet. Alle Constraint-Prüfungen im Backtracking sind danach O(1)-Array-Zugriffe
 * statt HashSet-Lookups, was besonders bei größeren Turnieren (~30+ Spieler) spürbar schneller ist.<br>
 * <br>
 * <b>Vollständigkeit:</b> Wenn das Backtracking ohne Knotenlimit-Überschreitung
 * beendet wird und keine Lösung gefunden hat, ist bewiesen, dass keine gültige
 * Paarung mehr existiert — d. h. alle Kombinationen sind ausgeschöpft.<br>
 * <br>
 * <b>Dummy-Mechanismus (identisch zu V1):</b> Ist die Spieleranzahl nicht durch
 * {@code teamSize} teilbar, werden temporäre Dummy-Spieler mit
 * SetzPos={@value #DUMMY_SPIELER_SETZPOS} eingefügt. Da {@link Spieler#warImTeamMit}
 * auch {@code gleicheSetzPos} prüft, verhindert das Modell automatisch, dass zwei
 * Dummies im selben Team landen — kein separater Constraint nötig.<br>
 * <br>
 * <b>Wechselnde Teilnehmerzahl:</b> Der Algorithmus liest die aktuelle Spielerliste
 * bei jedem Aufruf neu aus {@code SpielerMeldungen} — Zu- und Abgänge zwischen
 * Runden werden automatisch berücksichtigt.<br>
 * <br>
 * Die öffentliche API ist kompatibel zu {@link SuperMeleePaarungen} (V1).
 *
 * @author Michael Massee
 * @see SuperMeleePaarungen
 */
public class SuperMeleePaarungenV2 {

    private static final Logger logger = LogManager.getLogger(SuperMeleePaarungenV2.class);

    private static final int DUMMY_SPIELER_START_NR = 10000;
    /** SetzPos der Dummy-Spieler; verhindert via {@code gleicheSetzPos}, dass zwei Dummies ins selbe Team gelost werden. */
    private static final int DUMMY_SPIELER_SETZPOS = 999;

    // =========================================================================
    // Öffentliche API — kompatibel zu SuperMeleePaarungen (V1)
    // =========================================================================

    /**
     * Erzeugt eine neue Spielrunde im Standard-Triplette-Modus.
     *
     * @param rndNr     Rundennummer
     * @param meldungen Liste aller gemeldeten Spieler
     * @return fertig ausgeloste Spielrunde, oder {@code null} bei ungültiger Spieleranzahl
     * @throws AlgorithmenException wenn keine gültige Paarung mehr möglich ist
     */
    public MeleeSpielRunde neueSpielrunde(int rndNr, SpielerMeldungen meldungen) throws AlgorithmenException {
        return neueSpielrundeTripletteMode(rndNr, meldungen, false);
    }

    /**
     * Erzeugt eine Spielrunde im Doublette-Modus (2er-Teams).<br>
     * Wenn die Spieleranzahl nicht durch 2 teilbar ist, werden einzelne Teams
     * auf 3 Spieler (Triplette) aufgefüllt.
     *
     * @param rndNr        Rundennummer
     * @param meldungen    Liste aller gemeldeten Spieler
     * @param nurTriplette {@code true}: nur reine Triplette-Runde erzwingen
     * @return fertig ausgeloste Spielrunde, oder {@code null} bei ungültiger Spieleranzahl
     * @throws AlgorithmenException wenn keine gültige Paarung möglich ist
     */
    public MeleeSpielRunde neueSpielrundeDoubletteMode(int rndNr, SpielerMeldungen meldungen, boolean nurTriplette)
            throws AlgorithmenException {
        checkNotNull(meldungen, "Meldungen = null");
        SuperMeleeTeamRechner teamRechner = new SuperMeleeTeamRechner(meldungen.spieler().size(), SuperMeleeMode.Doublette);

        if (!teamRechner.valideAnzahlSpieler()) {
            return null;
        }
        if (nurTriplette && !teamRechner.isNurTripletteMoeglich()) {
            throw new AlgorithmenException("Keine Triplette Spielrunde möglich");
        }

        MeleeSpielRunde spielRunde = nurTriplette
                ? generiereRundeMitFesteTeamGroese(rndNr, 3, meldungen)
                : generiereRundeMitDummies(rndNr, 3, teamRechner.getAnzDoublette(), meldungen);
        return finalizeRunde(spielRunde);
    }

    /**
     * Erzeugt eine Spielrunde im Triplette-Modus (3er-Teams).<br>
     * Wenn die Spieleranzahl nicht durch 3 teilbar ist, werden einzelne Teams
     * auf 2 Spieler (Doublette) reduziert.
     *
     * @param rndNr        Rundennummer
     * @param meldungen    Liste aller gemeldeten Spieler
     * @param nurDoublette {@code true}: nur reine Doublette-Runde erzwingen
     * @return fertig ausgeloste Spielrunde, oder {@code null} bei ungültiger Spieleranzahl
     * @throws AlgorithmenException wenn keine gültige Paarung möglich ist
     */
    public MeleeSpielRunde neueSpielrundeTripletteMode(int rndNr, SpielerMeldungen meldungen, boolean nurDoublette)
            throws AlgorithmenException {
        checkNotNull(meldungen, "Meldungen = null");
        SuperMeleeTeamRechner teamRechner = new SuperMeleeTeamRechner(meldungen.spieler().size(), SuperMeleeMode.Triplette);

        if (!teamRechner.valideAnzahlSpieler()) {
            return null;
        }
        if (nurDoublette && !teamRechner.isNurDoubletteMoeglich()) {
            throw new AlgorithmenException("Keine Doublette Spielrunde möglich");
        }

        MeleeSpielRunde spielRunde = nurDoublette
                ? generiereRundeMitFesteTeamGroese(rndNr, 2, meldungen)
                : generiereRundeMitDummies(rndNr, 3, teamRechner.getAnzDoublette(), meldungen);
        return finalizeRunde(spielRunde);
    }

    /**
     * Sortiert die Teams nach Größe, validiert die Spieler-Team-Zuordnung und
     * optimiert die Gegner-Paarung (2. Rang: Gegner-Wiederholungen minimieren).
     * Wird von allen öffentlichen Methoden nach der Generierung aufgerufen.
     */
    private MeleeSpielRunde finalizeRunde(MeleeSpielRunde spielRunde) throws AlgorithmenException {
        spielRunde.sortiereTeamsNachGroese();
        spielRunde.validateSpielerTeam(null);
        optimiereGegnerPaarung(spielRunde);
        return spielRunde;
    }

    /**
     * Ordnet die Teams so um, dass Gegner-Wiederholungen minimiert werden (2. Rang).<br>
     * <br>
     * Greedy-Algorithmus: Nimm jeweils das erste ungepaarte Team, wähle als Gegner das
     * ungepaarte Team mit dem geringsten Gegner-Score. Nach der Umsortierung werden die
     * Gegner paarweise in die Spieler-Objekte eingetragen ({@link Spieler#addGegner}).
     *
     * @param spielRunde die zu optimierende Spielrunde (Teams bereits nach Größe sortiert)
     */
    private void optimiereGegnerPaarung(MeleeSpielRunde spielRunde) {
        List<Team> unpaired = new ArrayList<>(spielRunde.teams());
        List<Team> ergebnis = new ArrayList<>(unpaired.size());

        while (!unpaired.isEmpty()) {
            Team team1 = unpaired.remove(0);
            Team besteGegner = unpaired.stream()
                    .min(Comparator.comparingInt(t -> berechneGegnerScore(team1, t)))
                    .orElse(null);
            ergebnis.add(team1);
            if (besteGegner != null) {
                unpaired.remove(besteGegner);
                ergebnis.add(besteGegner);
            }
        }

        spielRunde.setzeTeamReihenfolge(ergebnis);

        // Gegner paarweise eintragen: ergebnis[2i] vs ergebnis[2i+1]
        for (int i = 0; i < ergebnis.size() - 1; i += 2) {
            Team teamA = ergebnis.get(i);
            Team teamB = ergebnis.get(i + 1);
            for (Spieler sA : teamA.spieler()) {
                for (Spieler sB : teamB.spieler()) {
                    sA.addGegner(sB);
                }
            }
        }
    }

    /**
     * Berechnet den Gegner-Score zweier Teams: Anzahl der Spielerpaare (sA, sB),
     * bei denen {@code sA.warGegnerVon(sB)} gilt — je höher, desto mehr Wiederholungen.
     */
    private int berechneGegnerScore(Team team1, Team team2) {
        int score = 0;
        for (Spieler s1 : team1.spieler()) {
            for (Spieler s2 : team2.spieler()) {
                if (s1.warGegnerVon(s2)) {
                    score++;
                }
            }
        }
        return score;
    }

    // =========================================================================
    // Dummy-Mechanismus
    // =========================================================================

    /**
     * Erzeugt eine Spielrunde mit temporären Dummy-Spielern für nicht durch {@code teamSize}
     * teilbare Spieleranzahlen.<br>
     * Die Dummies werden nach der Generierung garantiert entfernt ({@code try-finally}).
     * Ihr {@code SetzPos} ({@value #DUMMY_SPIELER_SETZPOS}) verhindert automatisch, dass
     * zwei Dummies im selben Team landen, weil {@link Spieler#warImTeamMit} die SetzPos prüft.
     *
     * @param rndNr      Rundennummer
     * @param teamSize   Ziel-Teamgröße (nach Dummy-Entfernung entstehen kleinere Teams)
     * @param anzDummies Anzahl einzufügender Dummy-Spieler
     * @param meldungen  Meldungsliste, temporär um Dummies erweitert
     * @return Spielrunde ohne Dummy-Spieler
     * @throws AlgorithmenException wenn keine gültige Paarung generiert werden konnte
     */
    private MeleeSpielRunde generiereRundeMitDummies(int rndNr, int teamSize, int anzDummies,
            SpielerMeldungen meldungen) throws AlgorithmenException {
        Spieler[] dummies = new Spieler[anzDummies];
        for (int i = 0; i < anzDummies; i++) {
            dummies[i] = Spieler.from(DUMMY_SPIELER_START_NR + i).setSetzPos(DUMMY_SPIELER_SETZPOS);
            meldungen.addSpielerWennNichtVorhanden(dummies[i]);
        }
        try {
            MeleeSpielRunde spielRunde = generiereRundeMitFesteTeamGroese(rndNr, teamSize, meldungen);
            for (Spieler dummy : dummies) {
                spielRunde.removeSpieler(dummy);
            }
            return spielRunde;
        } finally {
            // Garantiertes Cleanup — auch bei Exception
            for (Spieler dummy : dummies) {
                meldungen.removeSpieler(dummy);
            }
        }
    }

    // =========================================================================
    // Backtracking-Koordination
    // =========================================================================

    /**
     * Erzeugt eine Spielrunde, bei der alle Teams exakt {@code teamSize} Spieler haben,
     * via vollständigem Backtracking-Algorithmus mit Adjazenz-Matrix, MCV-Heuristik
     * und Forward-Checking.<br>
     * <br>
     * Ablauf:
     * <ol>
     *   <li>Spieler zufällig mischen (unterschiedliche Lösungen in verschiedenen Runden).</li>
     *   <li>Adjazenz-Matrix aufbauen und dabei gleichzeitig MCV-Grade berechnen
     *       (einziger O(n²)-Durchlauf).</li>
     *   <li>MCV-Sortierung: stärker eingeschränkte Spieler zuerst platzieren.</li>
     *   <li>Backtracking mit Forward-Checking: findet garantiert eine Lösung, falls eine existiert.</li>
     *   <li>Vollständige Suche ohne Lösung → sofortige Exception.</li>
     * </ol>
     *
     * @param rndNr     Rundennummer
     * @param teamSize  gewünschte Teamgröße (2 = Doublette, 3 = Triplette)
     * @param meldungen Spielerliste; Anzahl muss ohne Rest durch {@code teamSize} teilbar sein
     * @return vollständig befüllte Spielrunde
     * @throws AlgorithmenException wenn keine gültige Paarung mehr möglich ist
     */
    @VisibleForTesting
    MeleeSpielRunde generiereRundeMitFesteTeamGroese(int rndNr, int teamSize, SpielerMeldungen meldungen)
            throws AlgorithmenException {

        // SpielerMeldungen.spieler() liefert eine Kopie — sicher zum Sortieren
        List<Spieler> spieler = new ArrayList<>(meldungen.spieler());
        int n = spieler.size();
        int numTeams = n / teamSize;

        // Schritt 1: Zufällig mischen — für unterschiedliche Lösungen je Runde
        Collections.shuffle(spieler);

        // Schritt 2: Adjazenz-Matrix aufbauen + MCV-Grade in einem einzigen O(n²)-Durchlauf
        boolean[][] matrix = new boolean[n][n];
        int[] degrees = new int[n];
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (spieler.get(i).warImTeamMit(spieler.get(j))) {
                    matrix[i][j] = matrix[j][i] = true;
                    degrees[i]++;
                    degrees[j]++;
                }
            }
        }

        // Schritt 3: MCV-Sortierung — Index-Array nach Constraint-Grad absteigend sortieren
        // (Shuffle hat Gleichstände bereits zufällig gebrochen)
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) {
            order[i] = i;
        }
        Arrays.sort(order, (a, b) -> Integer.compare(degrees[b], degrees[a]));

        // Schritt 4: Vollständiges Backtracking
        List<List<Integer>> teams = new ArrayList<>(numTeams);
        for (int i = 0; i < numTeams; i++) {
            teams.add(new ArrayList<>(teamSize));
        }

        if (backtrack(order, 0, teams, teamSize, matrix)) {
            return buildSpielRunde(rndNr, teams, spieler, meldungen);
        }

        logger.warn("Spielrunde {}: Alle möglichen Spielerkombinationen ausgeschöpft ({} Spieler).",
                rndNr, n);
        throw new AlgorithmenException(
                "Keine gültige Spielrunde für Runde " + rndNr + " möglich — "
                + "alle Spielerkombinationen sind ausgeschöpft. "
                + "Möglicherweise müssen Wiederholungen in den Regeln zugelassen werden.");
    }

    // =========================================================================
    // Backtracking-Kern
    // =========================================================================

    /**
     * Rekursiver Backtracking-Kern: weist den Spieler an Position {@code idx} in
     * {@code order} einem Team zu.<br>
     * <br>
     * Teams speichern während des Backtrackings Original-Indizes (aus der gemischten
     * Spielerliste), keine {@link Spieler}-Objekte. Das erlaubt O(1)-Matrix-Lookups
     * ohne Rückwärts-Mapping. Die Konvertierung zu {@link Spieler}-Objekten erfolgt
     * erst am Ende in {@link #buildSpielRunde}.<br>
     * <br>
     * Angewandte Heuristiken:
     * <ul>
     *   <li><b>Symmetriebrechung</b>: Leere Teams sind strukturell äquivalent — pro
     *       Rekursionsebene wird nur das erste leere Team versucht.</li>
     *   <li><b>Forward-Checking</b>: Nach jeder Zuweisung wird geprüft, ob alle noch
     *       nicht zugewiesenen Spieler mindestens einen validen Team-Slot haben.</li>
     * </ul>
     *
     * @param order    MCV-sortierte Original-Indizes (stärker eingeschränkt → früher)
     * @param idx      aktuelle Position in {@code order}
     * @param teams    partielle Team-Zuweisung als Index-Listen (in-place, rückgängig gemacht)
     * @param teamSize Zielgröße jedes Teams
     * @param matrix   vorberechnete Adjazenz-Matrix; {@code matrix[i][j]==true} bedeutet Konflikt
     * @return {@code true} wenn eine vollständige gültige Zuweisung gefunden wurde
     */
    private boolean backtrack(Integer[] order, int idx, List<List<Integer>> teams,
            int teamSize, boolean[][] matrix) {
        if (idx == order.length) {
            return true; // Alle Spieler erfolgreich zugewiesen
        }

        int currentOrigIdx = order[idx];
        boolean triedEmptyTeam = false;

        for (List<Integer> team : teams) {
            if (team.size() >= teamSize) {
                continue; // Team bereits voll
            }

            // Symmetriebrechung: leere Teams sind äquivalent — nur das erste versuchen
            if (team.isEmpty()) {
                if (triedEmptyTeam) {
                    continue;
                }
                triedEmptyTeam = true;
            }

            if (kannTeamBeitreten(currentOrigIdx, team, matrix)) {
                team.add(currentOrigIdx);
                if (vorwaertsCheck(order, idx + 1, teams, teamSize, matrix)
                        && backtrack(order, idx + 1, teams, teamSize, matrix)) {
                    return true;
                }
                team.remove(team.size() - 1); // Backtrack: Zuweisung rückgängig machen
            }
        }

        return false; // kein gültiger Platz gefunden — eine Ebene zurückgehen
    }

    /**
     * Prüft, ob der Spieler mit Original-Index {@code spielerIdx} dem {@code team}
     * beitreten kann, ohne einen Constraint zu verletzen.<br>
     * <br>
     * Nutzt O(1)-Array-Zugriff auf die vorberechnete Adjazenz-Matrix. Die Matrix
     * kodiert alle Constraints: Spielhistorie ({@code warImTeamMit}) und SetzPos-Gleichheit
     * ({@code gleicheSetzPos}), inklusive des Dummy-Constraints (zwei Dummies teilen
     * SetzPos {@value #DUMMY_SPIELER_SETZPOS}).
     *
     * @param spielerIdx  Original-Index des Spielers in der gemischten Spielerliste
     * @param team        aktuell gebildetes Team als Liste von Original-Indizes
     * @param matrix      vorberechnete Adjazenz-Matrix
     * @return {@code false} wenn ein Constraint verletzt wird
     */
    private boolean kannTeamBeitreten(int spielerIdx, List<Integer> team, boolean[][] matrix) {
        for (int memberIdx : team) {
            if (matrix[spielerIdx][memberIdx]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Forward-Checking: Prüft, ob jeder noch nicht zugewiesene Spieler mindestens
     * einen gültigen Team-Slot hat (hinreichende Bedingung für Fortsetzbarkeit).<br>
     * Erkennt Sackgassen frühzeitig und reduziert damit die Backtracking-Tiefe erheblich,
     * insbesondere bei dichter Spielhistorie in späten Runden.
     *
     * @param order    MCV-sortierte Indizes
     * @param startIdx Index des ersten noch nicht zugewiesenen Spielers
     * @param teams    aktuelle partielle Team-Zuweisung
     * @param teamSize Zielgröße jedes Teams
     * @param matrix   vorberechnete Adjazenz-Matrix
     * @return {@code false} wenn mindestens ein Spieler keinen gültigen Slot hat
     */
    private boolean vorwaertsCheck(Integer[] order, int startIdx, List<List<Integer>> teams,
            int teamSize, boolean[][] matrix) {
        for (int i = startIdx; i < order.length; i++) {
            int futureSpielerIdx = order[i];
            boolean kannPlatziert = false;
            for (List<Integer> team : teams) {
                if (team.size() < teamSize && kannTeamBeitreten(futureSpielerIdx, team, matrix)) {
                    kannPlatziert = true;
                    break;
                }
            }
            if (!kannPlatziert) {
                return false;
            }
        }
        return true;
    }

    // =========================================================================
    // Hilfsmethoden
    // =========================================================================

    /**
     * Baut die {@link MeleeSpielRunde} aus der vom Backtracking gefundenen Team-Zuweisung.<br>
     * <br>
     * Konvertiert die internen Index-Listen (Original-Indizes in {@code spieler}) zurück zu
     * {@link Spieler}-Objekten. Setzt zunächst alle Team-Zuordnungen zurück ({@code resetTeam}),
     * dann committed {@link Team#addSpielerWennNichtVorhanden} die {@code warImTeamMit}-Geschichte
     * für diese Runde in die Spieler-Objekte — diese History wird in späteren Runden als
     * Constraint in der Adjazenz-Matrix kodiert.
     *
     * @param rndNr   Rundennummer
     * @param teams   gefundene Team-Zuweisung als Original-Indizes
     * @param spieler gemischte Spielerliste (Index-Basis für {@code teams})
     * @param meldungen Spielerliste (für resetTeam)
     * @return vollständige und committed Spielrunde
     * @throws AlgorithmenException bei Modell-Validierungsfehlern
     */
    private MeleeSpielRunde buildSpielRunde(int rndNr, List<List<Integer>> teams,
            List<Spieler> spieler, SpielerMeldungen meldungen) throws AlgorithmenException {
        meldungen.resetTeam();
        MeleeSpielRunde spielrunde = new MeleeSpielRunde(rndNr);
        for (List<Integer> teamIndices : teams) {
            Team team = spielrunde.newTeam();
            for (int idx : teamIndices) {
                team.addSpielerWennNichtVorhanden(spieler.get(idx));
            }
        }
        return spielrunde;
    }
}
