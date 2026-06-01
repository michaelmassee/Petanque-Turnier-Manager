/*
 * Erstellung : 2026-02-24 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen.supermelee;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.annotations.VisibleForTesting;

import de.petanqueturniermanager.helper.random.RandomSource;
import de.petanqueturniermanager.exception.AlgorithmenException;
import de.petanqueturniermanager.helper.i18n.I18n;
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
    /** Gegner-Wiederholungen dominieren Crossover-Tie-Breaker in der Gegner-Paarung. */
    private static final int GEGNER_WIEDERHOLUNG_GEWICHT = 10_000;
    /** Crossover bleibt nur Tie-Breaker, wenn die Gegner-Wiederholungszahl gleich ist. */
    private static final int CROSSOVER_GEWICHT = 1;
    /** Anzahl Shuffle-Versuche in {@link #generiereRundeMitFesteTeamGroese}. */
    @VisibleForTesting
    static final int MAX_SHUFFLE_VERSUCHE = 10;
    /** Begrenzung der Best-Effort-Optimierung, damit die Zusatzbewertung performant bleibt. */
    private static final int MAX_BEWERTETE_LAYOUTS_PRO_PASS = 128;
    /** Bis zu dieser Gruppengroesse wird die simulierte Gegner-Paarung exakt bewertet. */
    private static final int MAX_EXAKT_BEWERTETE_GRUPPENTEAMS = 12;

    private static final class BacktrackingErgebnis {
        private List<List<Integer>> besteTeams;
        private int besterScore = Integer.MAX_VALUE;
        private int bewerteteLayouts;
        private boolean limitErreicht;
        private boolean bewertungslimitErreicht;
        private boolean perfekteLoesungGefunden;

        private boolean hatLoesung() {
            return besteTeams != null;
        }
    }

    private static final class RundenKandidat {
        private List<List<Integer>> teams;
        private List<Spieler> spieler;
        private int score = Integer.MAX_VALUE;
        private int passPrioritaet = Integer.MAX_VALUE;

        private boolean hatLoesung() {
            return teams != null;
        }
    }

    /**
     * Maximale Backtracking-Knoten pro Pass, abhängig von der Spieleranzahl {@code n}.<br>
     * <br>
     * Kleine Turniere (n ≤ 24) finden Lösungen in &lt;&lt;1 000 Knoten — ein hohes Budget
     * verlängert dort nur unnötig das Erkennen echter Unlösbarkeit. Große Turniere
     * (n &gt; 36) brauchen mehr Suchraum, weil die Constraint-Dichte steigt und der
     * statische MCV-Ansatz mehr Fehlpfade erkunden muss.
     */
    @VisibleForTesting
    static int maxKnotenProPass(int n) {
        if (n <= 24) {
            return 50_000;
        }
        if (n <= 36) {
            return 200_000;
        }
        if (n <= 48) {
            return 400_000;
        }
        return 600_000;
    }

    /**
     * Reduziertes Knotenbudget fuer Fairness-Schwelle-Versuche innerhalb von
     * {@link #generiereRundeMitFairnessConstraint}.<br>
     * <br>
     * Ist ein fairness-constrained Problem loesbar, findet MCV+Forward-Checking
     * die Loesung in &lt;&lt;1 000 Knoten — das reduzierte Budget ist mehr als genug.
     * Ist es unlösbar (bei dichter Teamhistorie + Fairness-Constraints), beweist
     * das Forward-Checking die Unlösbarkeit ebenfalls schnell. Das volle Budget
     * wäre in beiden Fällen verschwendet.
     */
    @VisibleForTesting
    static int maxKnotenFairnessVersuch(int n) {
        return Math.max(20_000, maxKnotenProPass(n) / 8);
    }

    /** Spieltag-Nummer ausschließlich zur Anreicherung der Log-Meldungen; {@code 0} = nicht gesetzt. */
    private int spieltagNrFuerLog;

    // =========================================================================
    // Öffentliche API — kompatibel zu SuperMeleePaarungen (V1)
    // =========================================================================

    /**
     * Setzt die Spieltag-Nummer, die ausschließlich zur Anreicherung der
     * Log-Meldungen verwendet wird (z. B. bei ausgeschöpften Kombinationen).
     * Ohne Aufruf bleiben die Log-Meldungen ohne Spieltag-Angabe.
     *
     * @param spieltagNr Spieltag-Nummer für die Log-Ausgabe
     */
    public void setSpieltagNrFuerLog(int spieltagNr) {
        this.spieltagNrFuerLog = spieltagNr;
    }

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
     * @return fertig ausgeloste Spielrunde
     * @throws AlgorithmenException wenn keine gültige Paarung möglich ist oder die Spieleranzahl ungültig ist
     */
    public MeleeSpielRunde neueSpielrundeDoubletteMode(int rndNr, SpielerMeldungen meldungen, boolean nurTriplette)
            throws AlgorithmenException {
        checkNotNull(meldungen, "Meldungen = null");
        SuperMeleeTeamRechner teamRechner = new SuperMeleeTeamRechner(meldungen.spieler().size(), SuperMeleeMode.Doublette);

        if (!teamRechner.valideAnzahlSpieler()) {
            throw new AlgorithmenException(I18n.get("error.algorithmus.spieleranzahl.doublette", meldungen.spieler().size()));
        }
        if (nurTriplette && !teamRechner.isNurTripletteMoeglich()) {
            throw new AlgorithmenException(I18n.get("error.algorithmus.keine.triplette"));
        }

        // Doublette-Hauptmodus: das Dummy-Team wird nach Cleanup zum Doublette = Default-Größe.
        // Die "Ausnahme" sind hier die dummylosen Triplettes. Spieler im Dummy-Team auf
        // anzMalKleinesTeam zu sperren wäre kontraproduktiv → Fairness-Constraint deaktiviert.
        MeleeSpielRunde spielRunde = nurTriplette
                ? generiereRundeMitFesteTeamGroese(rndNr, 3, meldungen)
                : generiereRundeMitDummies(rndNr, 3, teamRechner.getAnzDoublette(), meldungen, false);
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
     * @return fertig ausgeloste Spielrunde
     * @throws AlgorithmenException wenn keine gültige Paarung möglich ist oder die Spieleranzahl ungültig ist
     */
    public MeleeSpielRunde neueSpielrundeTripletteMode(int rndNr, SpielerMeldungen meldungen, boolean nurDoublette)
            throws AlgorithmenException {
        checkNotNull(meldungen, "Meldungen = null");
        SuperMeleeTeamRechner teamRechner = new SuperMeleeTeamRechner(meldungen.spieler().size(), SuperMeleeMode.Triplette);

        if (!teamRechner.valideAnzahlSpieler()) {
            throw new AlgorithmenException(I18n.get("error.algorithmus.spieleranzahl.triplette", meldungen.spieler().size()));
        }
        if (nurDoublette && !teamRechner.isNurDoubletteMoeglich()) {
            throw new AlgorithmenException(I18n.get("error.algorithmus.keine.doublette"));
        }

        // Triplette-Hauptmodus: Dummy-Teams werden nach Cleanup zu Doublettes = Ausnahme.
        // Fairness-Constraint aktivieren, damit niemand wiederholt im Doublette landet.
        MeleeSpielRunde spielRunde = nurDoublette
                ? generiereRundeMitFesteTeamGroese(rndNr, 2, meldungen)
                : generiereRundeMitDummies(rndNr, 3, teamRechner.getAnzDoublette(), meldungen, true);
        return finalizeRunde(spielRunde);
    }

    /**
     * Notnagel-Paarung: erzeugt eine <b>rein zufällige</b> Spielrunde ohne jegliche
     * Constraint-Prüfung (Mitspieler-/Gegner-Wiederholungen und Setz-Positionen
     * werden ignoriert). Wird vom Auslosungs-Retry in {@code SpielrundeDelegate}
     * aufgerufen, wenn auch nach vollständiger Lockerung der Spieltag-Historie
     * keine konfliktfreie Paarung gefunden werden kann.
     *
     * @param rndNr           Rundennummer
     * @param meldungen       gemeldete Spieler (Historie/Team werden zurückgesetzt)
     * @param doubletteRunde  {@code true}: 2er-Modus, sonst Triplette-Modus
     * @return zufällig befüllte Spielrunde
     * @throws AlgorithmenException nur bei ungültiger Spieleranzahl
     */
    public MeleeSpielRunde erzeugeZufallsRunde(int rndNr, SpielerMeldungen meldungen, boolean doubletteRunde)
            throws AlgorithmenException {
        checkNotNull(meldungen, "Meldungen = null");
        SuperMeleeMode mode = doubletteRunde ? SuperMeleeMode.Doublette : SuperMeleeMode.Triplette;
        SuperMeleeTeamRechner rechner = new SuperMeleeTeamRechner(meldungen.spieler().size(), mode);
        if (!rechner.valideAnzahlSpieler()) {
            throw new AlgorithmenException(I18n.get(
                    doubletteRunde ? "error.algorithmus.spieleranzahl.doublette"
                            : "error.algorithmus.spieleranzahl.triplette",
                    meldungen.spieler().size()));
        }

        meldungen.resetTeam();
        List<Spieler> shuffled = new ArrayList<>(meldungen.spieler());
        Collections.shuffle(shuffled, RandomSource.asJavaRandom());

        MeleeSpielRunde runde = new MeleeSpielRunde(rndNr);
        int idx = 0;
        for (int t = 0; t < rechner.getAnzTriplette(); t++) {
            Team team = runde.newTeam();
            for (int s = 0; s < 3; s++) {
                team.addSpielerWennNichtVorhanden(shuffled.get(idx++));
            }
        }
        for (int t = 0; t < rechner.getAnzDoublette(); t++) {
            Team team = runde.newTeam();
            for (int s = 0; s < 2; s++) {
                team.addSpielerWennNichtVorhanden(shuffled.get(idx++));
            }
        }
        return runde;
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
        protokolliereWarImSpielMit(spielRunde);
        return spielRunde;
    }

    /**
     * Trägt für jedes Spielerpaar innerhalb einer Partie (TeamA ∪ TeamB)
     * gegenseitig {@link Spieler#addWarImSpielMit} ein. Diese Statistik wird in
     * Folgerunden als weicher Constraint im Backtracking-Value-Ordering genutzt,
     * um Crossover-Wiederholungen (gleiches Paar mehrfach im selben Spiel, egal
     * ob als Team oder als Gegner) zu vermeiden.
     */
    private void protokolliereWarImSpielMit(MeleeSpielRunde spielRunde) {
        List<Team> teams = spielRunde.teams();
        for (int i = 0; i < teams.size() - 1; i += 2) {
            Team teamA = teams.get(i);
            Team teamB = teams.get(i + 1);
            List<Spieler> alle = new ArrayList<>(teamA.spieler().size() + teamB.spieler().size());
            alle.addAll(teamA.spieler());
            alle.addAll(teamB.spieler());
            for (int x = 0; x < alle.size(); x++) {
                for (int y = x + 1; y < alle.size(); y++) {
                    alle.get(x).addWarImSpielMit(alle.get(y));
                }
            }
        }
    }

    /**
     * Ordnet die Teams so um, dass Gegner-Wiederholungen minimiert werden (2. Rang) und
     * gleichzeitig Doublette gegen Triplette (5er-Partie) nur dort entsteht, wo die
     * Parität es zwingend erfordert.<br>
     * <br>
     * Algorithmus:
     * <ol>
     *   <li>Teams nach Größe in zwei Gruppen aufteilen (Doublettes / Triplettes).</li>
     *   <li>Sind beide Gruppen ungerade, wird genau eine 5er-Partie gebildet —
     *       das D-/T-Paar mit minimalem Gegner-Score.</li>
     *   <li>Innerhalb jeder Gruppe wird die bisherige Greedy-Paarung
     *       (Gegner-Score-Minimierung) angewendet.</li>
     * </ol>
     * Anschließend werden die Gegner paarweise in die Spieler-Objekte eingetragen
     * ({@link Spieler#addGegner}).
     *
     * @param spielRunde die zu optimierende Spielrunde (Teams bereits nach Größe sortiert)
     */
    private void optimiereGegnerPaarung(MeleeSpielRunde spielRunde) throws AlgorithmenException {
        List<Team> doublettes = new ArrayList<>();
        List<Team> triplettes = new ArrayList<>();
        for (Team team : spielRunde.teams()) {
            (team.size() == 2 ? doublettes : triplettes).add(team);
        }

        // Bei ungerader Parität in beiden Gruppen: genau eine 5er-Partie bilden.
        // Laut SuperMeleeTeamRechner fallen D- und T-Parität immer zusammen, daher
        // entsteht maximal eine gemischte Paarung pro Runde.
        Team[] mixedPaar = null;
        if (doublettes.size() % 2 != 0 && triplettes.size() % 2 != 0) {
            mixedPaar = besteMischpaarung(doublettes, triplettes);
            doublettes.remove(mixedPaar[0]);
            triplettes.remove(mixedPaar[1]);
        }

        // Reihenfolge: erst alle D-Paare, dann ggf. 5er-Partie, dann alle T-Paare —
        // bleibt damit aufsteigend nach Teamgröße sortiert.
        List<Team> ergebnis = new ArrayList<>(spielRunde.teams().size());
        paareInnerhalbGruppe(doublettes, ergebnis);
        if (mixedPaar != null) {
            ergebnis.add(mixedPaar[0]);
            ergebnis.add(mixedPaar[1]);
        }
        paareInnerhalbGruppe(triplettes, ergebnis);

        spielRunde.setzeTeamReihenfolge(ergebnis);

        if (ergebnis.size() % 2 != 0) {
            throw new AlgorithmenException(
                    "Ungerade Teamanzahl (" + ergebnis.size() + ") in optimiereGegnerPaarung – " +
                    "SuperMeleeTeamRechner muss immer eine gerade Anzahl Teams liefern.");
        }

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
     * Paarung innerhalb einer Größen-Gruppe via vollständiger Aufzählung aller
     * möglichen Pairings (Min-Sum über {@link #berechneGegnerScore}).
     * <br>
     * Die Anzahl der Pairings auf {@code k} Teams ist {@code (k-1)!! = 1·3·5·…·(k-1)}.
     * Für k=8 sind das 105 Pairings — bei Spielerzahlen bis ~30 (k ≤ 10 = 945
     * Pairings) immer noch günstig im Vergleich zum Greedy-Verfahren, welches
     * lokale Optima nicht verlassen kann. Die Gruppe muss gerade Anzahl Teams
     * enthalten (durch Aufrufer sichergestellt).
     */
    private void paareInnerhalbGruppe(List<Team> gruppe, List<Team> ergebnis) {
        if (gruppe.isEmpty()) {
            return;
        }
        List<Team> arbeitskopie = new ArrayList<>(gruppe);
        List<Team> besteReihenfolge = new ArrayList<>(arbeitskopie.size());
        int[] bestScore = {Integer.MAX_VALUE};
        List<Team> aktuelle = new ArrayList<>(arbeitskopie.size());
        suchePaarungenRekursiv(arbeitskopie, aktuelle, 0, besteReihenfolge, bestScore);
        ergebnis.addAll(besteReihenfolge);
    }

    /**
     * Rekursive Aufzählung aller perfekten Matchings auf {@code teams}: fixiert
     * das erste ungepaarte Team und probiert jeden möglichen Partner; minimiert
     * über die Summe der {@link #berechneGegnerScore}-Werte aller Paare.
     */
    private void suchePaarungenRekursiv(List<Team> teams, List<Team> aktuelle, int aktScore,
            List<Team> besteReihenfolge, int[] bestScore) {
        if (teams.isEmpty()) {
            if (aktScore < bestScore[0]) {
                bestScore[0] = aktScore;
                besteReihenfolge.clear();
                besteReihenfolge.addAll(aktuelle);
            }
            return;
        }
        if (aktScore >= bestScore[0]) {
            return; // Branch-Cut: aktueller Pfad kann nicht besser werden
        }
        Team team1 = teams.getFirst();
        for (int i = 1; i < teams.size(); i++) {
            Team partner = teams.get(i);
            int score = berechneGegnerScore(team1, partner);
            List<Team> remaining = new ArrayList<>(teams.size() - 2);
            for (int j = 1; j < teams.size(); j++) {
                if (j != i) {
                    remaining.add(teams.get(j));
                }
            }
            aktuelle.add(team1);
            aktuelle.add(partner);
            suchePaarungenRekursiv(remaining, aktuelle, aktScore + score, besteReihenfolge, bestScore);
            aktuelle.removeLast();
            aktuelle.removeLast();
        }
    }

    /**
     * Wählt aus dem kartesischen Produkt von Doublettes × Triplettes das Paar
     * mit minimalem {@link #berechneGegnerScore(Team, Team)} — bildet die
     * unvermeidbare 5er-Partie mit der geringsten gemeinsamen Geschichte.
     */
    private Team[] besteMischpaarung(List<Team> doublettes, List<Team> triplettes) {
        Team bestD = doublettes.getFirst();
        Team bestT = triplettes.getFirst();
        int bestScore = Integer.MAX_VALUE;
        for (Team d : doublettes) {
            for (Team t : triplettes) {
                int score = berechneGegnerScore(d, t);
                if (score < bestScore) {
                    bestScore = score;
                    bestD = d;
                    bestT = t;
                }
            }
        }
        return new Team[] { bestD, bestT };
    }

    /**
     * Berechnet den gewichteten Score zweier Teams für die Gegnerpaarung.
     * Gegner-Wiederholung bleibt dominant, Crossover (Spieler-Paar war
     * in einer früheren Runde gemeinsam im selben Spiel — egal ob Team oder Gegner)
     * wird als Tie-Breaker hinzugerechnet. Gegner-Wiederholung und Crossover
     * werden exklusiv bewertet: eine echte Gegner-Wiederholung zählt nicht
     * zusätzlich als Crossover.
     */
    private int berechneGegnerScore(Team team1, Team team2) {
        int score = 0;
        for (Spieler s1 : team1.spieler()) {
            for (Spieler s2 : team2.spieler()) {
                score += berechneSpielerPaarScore(s1, s2);
            }
        }
        return score;
    }

    private int berechneSpielerPaarScore(Spieler s1, Spieler s2) {
        if (s1.warGegnerVon(s2)) {
            return GEGNER_WIEDERHOLUNG_GEWICHT;
        }
        if (s1.warImSpielMit(s2)) {
            return CROSSOVER_GEWICHT;
        }
        return 0;
    }

    /**
     * Bewertet ein vollstaendiges Backtracking-Layout ohne Modell-Mutation. Die
     * Bewertung simuliert die spaetere Gegner-Paarung inklusive Doublette/Triplette-
     * Gruppierung, damit die Teambildung nicht nur die erstbeste gueltige Loesung nimmt.
     */
    private int berechneLayoutGegnerScore(List<List<Integer>> teams, List<Spieler> spieler) {
        List<List<Integer>> doublettes = new ArrayList<>();
        List<List<Integer>> triplettes = new ArrayList<>();
        for (List<Integer> team : teams) {
            List<Integer> effektivesTeam = effektivesTeamOhneDummies(team, spieler);
            if (effektivesTeam.isEmpty()) {
                continue;
            }
            (effektivesTeam.size() == 2 ? doublettes : triplettes).add(effektivesTeam);
        }

        int score = berechneTeamInternenCrossoverScore(doublettes, spieler)
                + berechneTeamInternenCrossoverScore(triplettes, spieler);
        if (doublettes.size() % 2 != 0 && triplettes.size() % 2 != 0) {
            int[] mixed = besteMischpaarungIndex(doublettes, triplettes, spieler);
            score += mixed[2];
            doublettes.remove(mixed[0]);
            triplettes.remove(mixed[1]);
        }
        score += berechneBesteGruppenPaarungScore(doublettes, spieler);
        score += berechneBesteGruppenPaarungScore(triplettes, spieler);
        return score;
    }

    private int berechneTeamInternenCrossoverScore(List<List<Integer>> teams, List<Spieler> spieler) {
        int score = 0;
        for (List<Integer> team : teams) {
            for (int i = 0; i < team.size(); i++) {
                for (int j = i + 1; j < team.size(); j++) {
                    Spieler a = spieler.get(team.get(i));
                    Spieler b = spieler.get(team.get(j));
                    if (a.warImSpielMit(b)) {
                        score += CROSSOVER_GEWICHT;
                    }
                }
            }
        }
        return score;
    }

    private List<Integer> effektivesTeamOhneDummies(List<Integer> team, List<Spieler> spieler) {
        List<Integer> effektiv = new ArrayList<>(team.size());
        for (int spielerIdx : team) {
            if (spieler.get(spielerIdx).getNr() < DUMMY_SPIELER_START_NR) {
                effektiv.add(spielerIdx);
            }
        }
        return effektiv;
    }

    private int[] besteMischpaarungIndex(List<List<Integer>> doublettes, List<List<Integer>> triplettes,
            List<Spieler> spieler) {
        int bestD = 0;
        int bestT = 0;
        int bestScore = Integer.MAX_VALUE;
        for (int d = 0; d < doublettes.size(); d++) {
            for (int t = 0; t < triplettes.size(); t++) {
                int score = berechneIndexTeamScore(doublettes.get(d), triplettes.get(t), spieler);
                if (score < bestScore) {
                    bestScore = score;
                    bestD = d;
                    bestT = t;
                }
            }
        }
        return new int[] { bestD, bestT, bestScore };
    }

    private int berechneBesteGruppenPaarungScore(List<List<Integer>> gruppe, List<Spieler> spieler) {
        if (gruppe.size() < 2) {
            return 0;
        }
        if (gruppe.size() > MAX_EXAKT_BEWERTETE_GRUPPENTEAMS) {
            return berechneGreedyGruppenPaarungScore(gruppe, spieler);
        }
        int[] bestScore = {Integer.MAX_VALUE};
        sucheIndexPaarungenRekursiv(new ArrayList<>(gruppe), 0, bestScore, spieler);
        return bestScore[0];
    }

    private int berechneGreedyGruppenPaarungScore(List<List<Integer>> gruppe, List<Spieler> spieler) {
        List<List<Integer>> offen = new ArrayList<>(gruppe);
        int score = 0;
        while (offen.size() > 1) {
            int teamIndex = findeTeamMitHoechstemKonfliktpotenzial(offen, spieler);
            List<Integer> team = offen.remove(teamIndex);
            int besterPartnerIndex = 0;
            int besterPartnerScore = Integer.MAX_VALUE;
            for (int i = 0; i < offen.size(); i++) {
                int partnerScore = berechneIndexTeamScore(team, offen.get(i), spieler);
                if (partnerScore < besterPartnerScore) {
                    besterPartnerScore = partnerScore;
                    besterPartnerIndex = i;
                }
            }
            score += besterPartnerScore;
            offen.remove(besterPartnerIndex);
        }
        return score;
    }

    private int findeTeamMitHoechstemKonfliktpotenzial(List<List<Integer>> teams, List<Spieler> spieler) {
        int besterIndex = 0;
        int hoechstesPotenzial = -1;
        for (int i = 0; i < teams.size(); i++) {
            int potenzial = 0;
            for (int j = 0; j < teams.size(); j++) {
                if (i != j) {
                    potenzial += berechneIndexTeamScore(teams.get(i), teams.get(j), spieler);
                }
            }
            if (potenzial > hoechstesPotenzial) {
                hoechstesPotenzial = potenzial;
                besterIndex = i;
            }
        }
        return besterIndex;
    }

    private void sucheIndexPaarungenRekursiv(List<List<Integer>> teams, int aktScore, int[] bestScore,
            List<Spieler> spieler) {
        if (teams.isEmpty()) {
            bestScore[0] = Math.min(bestScore[0], aktScore);
            return;
        }
        if (teams.size() == 1) {
            bestScore[0] = Math.min(bestScore[0], aktScore);
            return;
        }
        if (aktScore >= bestScore[0]) {
            return;
        }
        if (teams.size() % 2 != 0) {
            for (int i = 0; i < teams.size(); i++) {
                List<List<Integer>> remaining = new ArrayList<>(teams.size() - 1);
                for (int j = 0; j < teams.size(); j++) {
                    if (j != i) {
                        remaining.add(teams.get(j));
                    }
                }
                sucheIndexPaarungenRekursiv(remaining, aktScore, bestScore, spieler);
            }
            return;
        }
        List<Integer> team1 = teams.getFirst();
        for (int i = 1; i < teams.size(); i++) {
            List<Integer> partner = teams.get(i);
            int score = berechneIndexTeamScore(team1, partner, spieler);
            List<List<Integer>> remaining = new ArrayList<>(teams.size() - 2);
            for (int j = 1; j < teams.size(); j++) {
                if (j != i) {
                    remaining.add(teams.get(j));
                }
            }
            sucheIndexPaarungenRekursiv(remaining, aktScore + score, bestScore, spieler);
        }
    }

    private int berechneIndexTeamScore(List<Integer> team1, List<Integer> team2, List<Spieler> spieler) {
        int score = 0;
        for (int idx1 : team1) {
            for (int idx2 : team2) {
                score += berechneSpielerPaarScore(spieler.get(idx1), spieler.get(idx2));
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
            SpielerMeldungen meldungen, boolean dummyTeamIstAusnahme) throws AlgorithmenException {
        // Snapshot der realen Spieler VOR dem Hinzufügen der Dummies — die Fairness-Schwellen
        // dürfen sich nicht auf Dummies stützen.
        List<Spieler> realeSpieler = new ArrayList<>(meldungen.spieler());

        Spieler[] dummies = new Spieler[anzDummies];
        for (int i = 0; i < anzDummies; i++) {
            dummies[i] = Spieler.from(DUMMY_SPIELER_START_NR + i).setSetzPos(DUMMY_SPIELER_SETZPOS);
            meldungen.addSpielerWennNichtVorhanden(dummies[i]);
        }
        try {
            MeleeSpielRunde spielRunde = generiereRundeMitFairnessConstraint(rndNr, teamSize, meldungen, dummies,
                    realeSpieler, dummyTeamIstAusnahme);
            for (Spieler dummy : dummies) {
                spielRunde.removeSpieler(dummy);
                // Dummy-Einträge aus warImTeamMit der echten Spieler entfernen:
                // Dummies sind künstliche Platzhalter — ihre Teamzuordnung soll keine
                // Paarungsconstraints für echte Spieler in späteren Runden erzeugen.
                for (Spieler spieler : meldungen.spieler()) {
                    spieler.deleteWarImTeam(dummy);
                }
            }
            if (dummyTeamIstAusnahme) {
                // Buchhaltung: jeder reale Spieler in einem Team kleiner als teamSize hat in
                // dieser Runde im Ausnahme-Team (Doublette) gespielt. Konsistent zum Eintrag
                // beim Wieder-Einlesen der Sheets ({@code SpielrundeDelegate.gespieltenRundenEinlesen}).
                for (Team team : spielRunde.teams()) {
                    if (team.size() > 0 && team.size() < teamSize) {
                        for (Spieler s : team.spieler()) {
                            s.incAnzMalKleinesTeam();
                        }
                    }
                }
            }
            return spielRunde;
        } finally {
            // Garantiertes Cleanup — auch bei Exception
            for (Spieler dummy : dummies) {
                meldungen.removeSpieler(dummy);
            }
        }
    }

    /**
     * Generiert eine Runde mit fest definierter Teamgröße und sorgt — falls
     * {@code dummyTeamIstAusnahme} gesetzt ist — dafür, dass Spieler, die in
     * früheren Runden bereits am häufigsten im Ausnahme-Team waren, möglichst
     * nicht erneut in einem Dummy-Team landen. Realisiert via Hard-Constraint
     * zwischen Dummy und "Vielspielern", die schrittweise relaxiert wird, wenn
     * die Backtracking-Suche kein gültiges Layout findet.
     */
    private MeleeSpielRunde generiereRundeMitFairnessConstraint(int rndNr, int teamSize,
            SpielerMeldungen meldungen, Spieler[] dummies, List<Spieler> realeSpieler,
            boolean dummyTeamIstAusnahme) throws AlgorithmenException {
        if (!dummyTeamIstAusnahme || dummies.length == 0 || realeSpieler.isEmpty()) {
            return generiereRundeMitFesteTeamGroese(rndNr, teamSize, meldungen);
        }

        int maxKlein = realeSpieler.stream().mapToInt(Spieler::getAnzMalKleinesTeam).max().orElse(0);
        int minKlein = realeSpieler.stream().mapToInt(Spieler::getAnzMalKleinesTeam).min().orElse(0);
        // Reduziertes Budget fuer Fairness-Versuche: loesbare Faelle werden in <<1000 Knoten gefunden,
        // unloesbare Faelle (dichte Teamhistorie + Fairness-Constraints) werden damit schnell erkannt.
        // Das volle Budget wird nur fuer den uneingeschraenkten Fallback-Call danach verwendet.
        int fairnessMaxKnoten = maxKnotenFairnessVersuch(meldungen.spieler().size());

        AlgorithmenException letzteException = null;
        // Von strikt nach locker, in Richtung „minimaler Counter bevorzugt":
        // Schwelle = minKlein + 1 sperrt alle Spieler, die mehr als minKlein mal im
        // Ausnahme-Team waren — übrig bleiben nur die Spieler mit dem niedrigsten Zähler.
        // Findet das Backtracking keine Lösung, wird die Schwelle schrittweise angehoben
        // (mehr Spieler werden zugelassen). So landen Doublette-Slots immer zuerst bei
        // den am wenigsten belasteten Spielern.
        for (int schwelle = minKlein + 1; schwelle <= maxKlein; schwelle++) {
            try {
                for (Spieler dummy : dummies) {
                    for (Spieler s : realeSpieler) {
                        if (s.getAnzMalKleinesTeam() >= schwelle) {
                            dummy.addWarImTeamMitWennNichtVorhanden(s);
                        }
                    }
                }
                return generiereRunde(rndNr, teamSize, meldungen, fairnessMaxKnoten, "Fairness-Schwelle " + schwelle);
            } catch (AlgorithmenException ex) {
                letzteException = ex;
                logger.debug("Spielrunde {}: Fairness-Schwelle {} nicht lösbar — lockere.", rndNr, schwelle);
                // Dummy-Konflikte vor nächstem Versuch zurücksetzen — beide Seiten, weil
                // addWarImTeamMitWennNichtVorhanden symmetrisch ist, deleteWarImTeam aber nicht.
                for (Spieler dummy : dummies) {
                    for (Spieler s : realeSpieler) {
                        dummy.deleteWarImTeam(s);
                        s.deleteWarImTeam(dummy);
                    }
                }
            }
        }
        // Schwelle vollständig zurückgenommen → identisches Verhalten zum Algorithmus
        // vor der Fairness-Erweiterung. Vorhandene Dummies haben hier keine zusätzlichen
        // Constraints mehr.
        try {
            return generiereRundeMitFesteTeamGroese(rndNr, teamSize, meldungen);
        } catch (AlgorithmenException ex) {
            // Wenn auch das nicht klappt: die ursprüngliche Exception ist informativer.
            if (letzteException != null) {
                throw letzteException;
            }
            throw ex;
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
        return generiereRunde(rndNr, teamSize, meldungen, maxKnotenProPass(meldungen.spieler().size()), "");
    }

    private MeleeSpielRunde generiereRunde(int rndNr, int teamSize, SpielerMeldungen meldungen, int maxKnoten,
            String kontext) throws AlgorithmenException {

        // SpielerMeldungen.spieler() liefert eine Kopie — sicher zum Sortieren
        List<Spieler> spieler = new ArrayList<>(meldungen.spieler());
        int n = spieler.size();
        int numTeams = n / teamSize;

        // Mehrere Shuffle-Versuche mit budget-begrenztem Backtracking pro Pass. Existiert
        // eine Lösung, findet MCV+Forward-Checking sie typischerweise in <<1 000 Knoten im
        // ersten Versuch. Ist die Aufgabe unlösbar, scheitert jeder Versuch schnell.
        // Das Budget skaliert mit n: kleine Turniere erkennen Unlösbarkeit schneller,
        // grosse Turniere erhalten mehr Suchraum.
        boolean irgendeinLimitErreicht = false;
        RundenKandidat besterKandidat = new RundenKandidat();

        // Pass-1-Tauglichkeit: warImSpielMit-Beziehungen aendern sich zwischen Shuffles nicht.
        // Daher reicht eine einmalige Pruefung im ersten Versuch.
        // Pass 1 ist strukturell unloesbar, wenn ein Spieler so viele Soft-Konflikte hat,
        // dass er keine (teamSize-1) kompatiblen Mitspieler mehr finden kann.
        boolean pass1NichtLoesbar = false;

        for (int versuch = 0; versuch < MAX_SHUFFLE_VERSUCHE; versuch++) {
            // Schritt 1: Zufaellig mischen — jeder Versuch startet mit anderer Spieler-Reihenfolge
            Collections.shuffle(spieler, RandomSource.asJavaRandom());

            // Schritt 2: Adjazenz-Matrix aufbauen + MCV-Grade in einem einzigen O(n^2)-Durchlauf.
            // Parallel: Soft-Matrix fuer "war schon im selben Spiel" (Value-Ordering-Heuristik)
            // und Soft-Grade fuer die Pass-1-Tauglichkeitspruefung.
            boolean[][] matrix = new boolean[n][n];
            boolean[][] softMatrix = new boolean[n][n];
            int[] degrees = new int[n];
            int[] softDegrees = new int[n];
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    if (spieler.get(i).warImTeamMit(spieler.get(j))) {
                        matrix[i][j] = matrix[j][i] = true;
                        degrees[i]++;
                        degrees[j]++;
                    }
                    if (spieler.get(i).warImSpielMit(spieler.get(j))) {
                        softMatrix[i][j] = softMatrix[j][i] = true;
                        softDegrees[i]++;
                        softDegrees[j]++;
                    }
                }
            }

            // Einmalig im ersten Versuch pruefen: kann Pass 1 ueberhaupt eine Loesung finden?
            // Bedingung: jeder Spieler braucht mindestens (teamSize-1) Soft-kompatible Partner.
            // softDegrees[i] > n - teamSize bedeutet: weniger als (teamSize-1) Partner verfuegbar.
            if (versuch == 0) {
                for (int i = 0; i < n; i++) {
                    if (softDegrees[i] > n - teamSize) {
                        pass1NichtLoesbar = true;
                        logger.debug("Spielrunde {}: Pass 1 uebersprungen — Spieler-Index {} hat {} Soft-Konflikte"
                                + " (Schwelle: {}), kein Zero-Crossover-Team moeglich.",
                                rndNr, i, softDegrees[i], n - teamSize);
                        break;
                    }
                }
            }

            // Schritt 3: MCV-Sortierung — Index-Array nach Constraint-Grad absteigend sortieren
            Integer[] order = new Integer[n];
            for (int i = 0; i < n; i++) {
                order[i] = i;
            }
            Arrays.sort(order, (a, b) -> Integer.compare(degrees[b], degrees[a]));

            List<List<Integer>> teams = new ArrayList<>(numTeams);
            for (int i = 0; i < numTeams; i++) {
                teams.add(new ArrayList<>(teamSize));
            }

            // Pass 1: Soft-Constraint als zusaetzlichen harten Constraint behandeln —
            // Adjazenz wird zur Union (matrix OR softMatrix). Findet das Backtracking
            // hier eine Loesung, hat sie 0 Mitspieler-Crossover (keine zwei Spieler im
            // selben Team waren bereits gemeinsam in einer frueheren Partie als Team-
            // Partner oder Gegner). Damit ist das vom Anwender als stoerend empfundene
            // "Team-dann-Gegner"-Muster vermieden.
            // Uebersprungen wenn strukturell unloesbar (zu dichte Soft-Matrix).
            if (!pass1NichtLoesbar) {
                boolean[][] unionMatrix = new boolean[n][n];
                for (int i = 0; i < n; i++) {
                    for (int j = 0; j < n; j++) {
                        unionMatrix[i][j] = matrix[i][j] || softMatrix[i][j];
                    }
                }
                int[] zaehler1 = {0};
                BacktrackingErgebnis ergebnis1 = new BacktrackingErgebnis();
                backtrackBeste(order, 0, teams, teamSize, unionMatrix, softMatrix, zaehler1, maxKnoten,
                        spieler, ergebnis1);
                if (ergebnis1.hatLoesung()) {
                    aktualisiereBestenKandidaten(besterKandidat, ergebnis1, spieler, 1);
                    if (ergebnis1.perfekteLoesungGefunden) {
                        return buildSpielRunde(rndNr, besterKandidat.teams, besterKandidat.spieler, meldungen);
                    }
                }
                if (ergebnis1.limitErreicht || ergebnis1.bewertungslimitErreicht) {
                    irgendeinLimitErreicht = true;
                }
                for (List<Integer> team : teams) {
                    team.clear();
                }
            }

            // Pass 2: Soft-Constraint nur noch als Value-Ordering-Tie-Breaker (nicht zum Pruning).
            // Bekommt ein eigenes Budget — unabhaengig davon, ob Pass 1 ausgefuehrt wurde.
            int[] zaehler2 = {0};
            BacktrackingErgebnis ergebnis2 = new BacktrackingErgebnis();
            backtrackBeste(order, 0, teams, teamSize, matrix, softMatrix, zaehler2, maxKnoten,
                    spieler, ergebnis2);
            if (ergebnis2.hatLoesung()) {
                aktualisiereBestenKandidaten(besterKandidat, ergebnis2, spieler, 2);
                if (pass1NichtLoesbar && ergebnis2.perfekteLoesungGefunden) {
                    return buildSpielRunde(rndNr, besterKandidat.teams, besterKandidat.spieler, meldungen);
                }
            }
            if (ergebnis2.limitErreicht || ergebnis2.bewertungslimitErreicht) {
                irgendeinLimitErreicht = true;
            }
        }

        if (besterKandidat.hatLoesung()) {
            return buildSpielRunde(rndNr, besterKandidat.teams, besterKandidat.spieler, meldungen);
        }

        if (irgendeinLimitErreicht) {
            var pass1Info = pass1NichtLoesbar ? " [Pass 1 wegen Soft-Dichte uebersprungen]" : "";
            var spieltagPraefix = spieltagNrFuerLog > 0 ? "Spieltag " + spieltagNrFuerLog + ", " : "";
            var kontextInfo = kontext.isEmpty() ? "" : " [" + kontext + "]";
            logger.warn("{}Spielrunde {}: Knotenlimit ({} Knoten/Pass x {} Paesse x {} Versuche) erreicht ({} Spieler).{}{}",
                    spieltagPraefix, rndNr, maxKnoten, pass1NichtLoesbar ? 1 : 2, MAX_SHUFFLE_VERSUCHE, n, pass1Info, kontextInfo);
            throw new AlgorithmenException(
                    "Keine gueltige Spielrunde fuer Runde " + rndNr + " moeglich — "
                    + "Knotenlimit (" + maxKnoten + " x " + (pass1NichtLoesbar ? 1 : 2) + " Paesse x " + MAX_SHUFFLE_VERSUCHE + " Versuche) erreicht. "
                    + "Moeglicherweise muessen Wiederholungen in den Regeln zugelassen werden.");
        }

        var spieltagPraefix = spieltagNrFuerLog > 0 ? "Spieltag " + spieltagNrFuerLog + ", " : "";
        logger.warn("{}Spielrunde {}: Alle möglichen Spielerkombinationen ausgeschöpft ({} Spieler).",
                spieltagPraefix, rndNr, n);
        throw new AlgorithmenException(
                "Keine gültige Spielrunde für Runde " + rndNr + " möglich — "
                + "alle Spielerkombinationen sind ausgeschöpft. "
                + "Möglicherweise müssen Wiederholungen in den Regeln zugelassen werden.");
    }

    private void aktualisiereBestenKandidaten(RundenKandidat kandidat, BacktrackingErgebnis ergebnis,
            List<Spieler> spieler, int passPrioritaet) {
        if (passPrioritaet < kandidat.passPrioritaet
                || (passPrioritaet == kandidat.passPrioritaet && ergebnis.besterScore < kandidat.score)) {
            kandidat.passPrioritaet = passPrioritaet;
            kandidat.score = ergebnis.besterScore;
            kandidat.teams = kopiereTeams(ergebnis.besteTeams);
            kandidat.spieler = new ArrayList<>(spieler);
        }
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
     * @param order         MCV-sortierte Original-Indizes (stärker eingeschränkt → früher)
     * @param idx           aktuelle Position in {@code order}
     * @param teams         partielle Team-Zuweisung als Index-Listen (in-place, rückgängig gemacht)
     * @param teamSize      Zielgröße jedes Teams
     * @param matrix        vorberechnete Adjazenz-Matrix; {@code matrix[i][j]==true} bedeutet Konflikt
     * @param softMatrix    vorberechnete Crossover-Matrix; nur Value-Ordering-Heuristik, kein Pruning
     * @param knotenZaehler einelementiges Array zum Mitzählen der Backtracking-Knoten (Safety-Limit)
     * @param knotenLimit   maximale Knotenanzahl pro Pass (abhängig von n, berechnet via {@link #maxKnotenProPass})
     */
    private void backtrackBeste(Integer[] order, int idx, List<List<Integer>> teams,
            int teamSize, boolean[][] matrix, boolean[][] softMatrix, int[] knotenZaehler, int knotenLimit,
            List<Spieler> spieler, BacktrackingErgebnis ergebnis) {
        if (ergebnis.perfekteLoesungGefunden || ergebnis.limitErreicht || ergebnis.bewertungslimitErreicht) {
            return;
        }
        if (idx == order.length) {
            int score = berechneLayoutGegnerScore(teams, spieler);
            ergebnis.bewerteteLayouts++;
            if (score < ergebnis.besterScore) {
                ergebnis.besterScore = score;
                ergebnis.besteTeams = kopiereTeams(teams);
                ergebnis.perfekteLoesungGefunden = (score == 0);
            }
            if (ergebnis.bewerteteLayouts >= MAX_BEWERTETE_LAYOUTS_PRO_PASS) {
                ergebnis.bewertungslimitErreicht = true;
            }
            return;
        }

        if (++knotenZaehler[0] >= knotenLimit) {
            ergebnis.limitErreicht = true;
            return; // Sicherheitsnetz: Knotenlimit pro Pass erreicht
        }

        int currentOrigIdx = order[idx];

        // Value-Ordering: Teams nach Crossover-Score zum aktuellen Spieler aufsteigend
        // sortieren (Soft-Constraint). Leere Teams haben Score 0 und liegen damit vorn
        // — die Symmetriebrechung filtert davon nur das erste leere Team.
        List<List<Integer>> sortierteTeams = new ArrayList<>(teams);
        sortierteTeams.sort(Comparator.comparingInt(t -> crossoverScoreTeam(currentOrigIdx, t, softMatrix)));

        boolean triedEmptyTeam = false;
        for (List<Integer> team : sortierteTeams) {
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
                boolean teamVoll = (team.size() >= teamSize);
                if (vorwaertsCheckInkrementell(currentOrigIdx, order, idx + 1, teams, teamSize, matrix, teamVoll)) {
                    backtrackBeste(order, idx + 1, teams, teamSize, matrix, softMatrix, knotenZaehler, knotenLimit,
                            spieler, ergebnis);
                }
                team.removeLast(); // Backtrack: Zuweisung rückgängig machen
                if (ergebnis.perfekteLoesungGefunden || ergebnis.limitErreicht || ergebnis.bewertungslimitErreicht) {
                    return;
                }
            }
        }
    }

    /**
     * Summiert die Crossover-Konflikte von {@code spielerIdx} zu allen bisherigen
     * Mitgliedern von {@code team} (Soft-Constraint im Backtracking-Value-Ordering).
     */
    private int crossoverScoreTeam(int spielerIdx, List<Integer> team, boolean[][] softMatrix) {
        int score = 0;
        for (int memberIdx : team) {
            if (softMatrix[spielerIdx][memberIdx]) {
                score++;
            }
        }
        return score;
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
     * Inkrementelles Forward-Checking: Prüft nur die Spieler, die von der soeben
     * erfolgten Zuweisung von {@code zugewiesenerIdx} betroffen sein können.<br>
     * <br>
     * Ein zukünftiger Spieler {@code x} kann nur dann einen Slot verloren haben, wenn
     * {@code matrix[zugewiesenerIdx][x] == true} (direkter Konflikt) oder das Team durch
     * die Zuweisung voll geworden ist (kein Slot im gerade befüllten Team mehr frei).<br>
     * <br>
     * Im Normalfall (Team noch nicht voll) werden nur Spieler mit direktem Konflikt geprüft
     * — typischerweise deutlich weniger als alle verbleibenden Spieler.<br>
     * Wenn das Team voll wurde, muss jeder verbleibende Spieler neu geprüft werden, da
     * dieser Slot nun für alle gesperrt ist (Fallback auf vollständigen Check).
     *
     * @param zugewiesenerIdx  Original-Index des soeben zugewiesenen Spielers
     * @param order            MCV-sortierte Indizes
     * @param startIdx         Index des ersten noch nicht zugewiesenen Spielers
     * @param teams            aktuelle partielle Team-Zuweisung
     * @param teamSize         Zielgröße jedes Teams
     * @param matrix           vorberechnete Adjazenz-Matrix
     * @param teamWurdeVoll    {@code true} wenn das Team durch die Zuweisung seine Zielgröße erreicht hat
     * @return {@code false} wenn mindestens ein betroffener Spieler keinen gültigen Slot mehr hat
     */
    private boolean vorwaertsCheckInkrementell(int zugewiesenerIdx, Integer[] order, int startIdx,
            List<List<Integer>> teams, int teamSize, boolean[][] matrix, boolean teamWurdeVoll) {
        for (int i = startIdx; i < order.length; i++) {
            int futureSpielerIdx = order[i];
            // Nur prüfen wenn: direkter Konflikt ODER Team wurde voll (Slot für alle verloren)
            if (!teamWurdeVoll && !matrix[zugewiesenerIdx][futureSpielerIdx]) {
                continue;
            }
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

    private List<List<Integer>> kopiereTeams(List<List<Integer>> teams) {
        List<List<Integer>> kopie = new ArrayList<>(teams.size());
        for (List<Integer> team : teams) {
            kopie.add(new ArrayList<>(team));
        }
        return kopie;
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
