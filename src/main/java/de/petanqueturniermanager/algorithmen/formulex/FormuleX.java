/*
 * Erstellung 27.04.2026 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen.formulex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.collections4.ListUtils;

import com.google.common.annotations.VisibleForTesting;

import de.petanqueturniermanager.algorithmen.common.WiederholungsarmePaarung;
import de.petanqueturniermanager.helper.random.RandomSource;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamPaarung;

/**
 * Implementiert die Logik für ein Turnier nach Formule X System.
 * <p>
 * Formule X ist ein Rundensystem mit Wertungspunkten, das zeitlich effizient ist
 * und eine vollständige Rangliste erstellt. Alle Teams spielen immer gleich viele Runden.
 * <p>
 * <b>Wertungsformel:</b>
 * <ul>
 *   <li>Sieger: Siegaufschlag + eigene Punkte + Differenzpunkte</li>
 *   <li>Verlierer: eigene Punkte</li>
 *   <li>Freilos (BYE): 126 Punkte (fix)</li>
 * </ul>
 * <p>
 * <b>Paarung:</b>
 * <ul>
 *   <li>Runde 1: freie Losung</li>
 *   <li>Runde ≥2: 1vs2, 3vs4, 5vs6, ... nach Wertungspunkten sortiert</li>
 *   <li>Gegnerwiederholungen nur, wenn unvermeidbar ({@link WiederholungsarmePaarung})</li>
 * </ul>
 *
 * @see <a href="../turniersysteme/08_Formule_X.md">08_Formule_X.md</a>
 */
public class FormuleX {

	/**
	 * Berechnet den Wertungsscore für ein Ergebnis nach der Formule X Formel.
	 * <p>
	 * <b>Formel:</b>
	 * <ul>
	 *   <li>Freilos: 126 Punkte (fix)</li>
	 *   <li>Verlierer: eigene Punkte</li>
	 *   <li>Sieger: Siegaufschlag + eigene Punkte + Differenz</li>
	 * </ul>
	 *
	 * @param ergebnis das Team-Ergebnis
	 * @param runden   Anzahl der bisher gespielten Runden (für Siegaufschlag)
	 * @return berechneter Wertungsscore
	 */
	public int berechneWertung(FormuleXErgebnis ergebnis, int runden) {
		if (ergebnis.hatteFreilos()) {
			return 126;
		}

		if (!ergebnis.istSieger()) {
			return ergebnis.eigenePunkte();
		}

		int siegaufschlag = getSiegaufschlag(runden);
		return siegaufschlag + ergebnis.eigenePunkte() + ergebnis.punktedifferenz();
	}

	/**
	 * Ermittelt den Siegaufschlag basierend auf der Anzahl der Runden.
	 * <p>
	 * Laut Formule X Spezifikation:
	 * <ul>
	 *   <li>bis 4 Runden: 100 Punkte</li>
	 *   <li>5–8 Runden: 200 Punkte</li>
	 *   <li>9–12 Runden: 300 Punkte</li>
	 * </ul>
	 *
	 * @param runden Anzahl der Runden
	 * @return Siegaufschlag (100, 200 oder 300)
	 */
	public int getSiegaufschlag(int runden) {
		if (runden <= 4) {
			return 100;
		}
		if (runden <= 8) {
			return 200;
		}
		return 300;
	}

	/**
	 * Die erste Runde wird frei ausgelost.
	 * <p>
	 * Teams werden zufällig gemischt und dann paarweise gepaart.
	 * Bei ungerader Teilnehmerzahl bekommt das letzte Team ein Freilos.
	 *
	 * @param teams die Liste der teilnehmenden Teams
	 * @return eine Liste der Paarungen für die erste Runde
	 */
	public List<TeamPaarung> ersteRunde(List<Team> teams) {
		boolean freiSpiel = teams.size() % 2 != 0;
		int letzteMeldungNr = freiSpiel ? teams.size() + 1 : teams.size();
		int anzTeamPaarungen = letzteMeldungNr / 2;

		// shuffle für zufällige Losung
		List<Team> shuffled = new ArrayList<>(teams);
		Collections.shuffle(shuffled, RandomSource.asJavaRandom());

		// split into 2 lists
		List<List<Team>> partition = ListUtils.partition(shuffled, anzTeamPaarungen);

		List<Team> listATeams = partition.get(0);
		List<Team> listBTeams = partition.get(1);
		int listBSize = listBTeams.size();

		return IntStream.range(0, anzTeamPaarungen).mapToObj(i -> {
			Team teamA = listATeams.get(i);
			Team teamB = (((anzTeamPaarungen - i) <= listBSize) ? listBTeams.get(anzTeamPaarungen - i - 1) : null);
			if (teamB == null) {
				teamA.setHatteFreilos(true);
			}
			return new TeamPaarung(teamA, teamB).addGegner();
		}).sorted((tp1, tp2) -> {
			// Freilos an letzte Stelle
			if (tp1.getB() == null) {
				return 1;
			} else if (tp2.getB() == null) {
				return -1;
			}
			return Integer.compare(tp1.getA().getNr(), tp2.getA().getNr());
		}).toList();
	}

	/**
	 * Ermittelt die Paarungen für die nächste Runde nach den Regeln des Formule X Systems.
	 * <p>
	 * <b>Ablauf:</b>
	 * <ol>
	 *   <li>Rangliste nach Wertungspunkten sortieren</li>
	 *   <li>Paarung: 1vs2, 3vs4, 5vs6, ...</li>
	 *   <li>Gegnerwiederholungen nur, wenn unvermeidbar</li>
	 *   <li>Freilos (BYE) für schlechtest platziertes Team ohne vorheriges BYE</li>
	 * </ol>
	 *
	 * @param rangliste sortierte Liste der Teamergebnisse (nach Wertung)
	 * @return eine Liste der Paarungen für die nächste Runde
	 */
	public List<TeamPaarung> weitereRunde(List<FormuleXErgebnis> rangliste) {
		List<Team> teamsInRangfolge = rangliste.stream()
				.map(e -> Team.from(e.teamNr()))
				.toList();

		List<Team> zuPaarende;
		TeamPaarung freilosPaarung = null;

		if (teamsInRangfolge.size() % 2 != 0) {
			Team byeTeam = findeByeTeam(rangliste, teamsInRangfolge);
			byeTeam.setHatteFreilos(true);
			freilosPaarung = new TeamPaarung(byeTeam);
			zuPaarende = teamsInRangfolge.stream()
					.filter(t -> t.getNr() != byeTeam.getNr())
					.collect(Collectors.toList());
		} else {
			zuPaarende = new ArrayList<>(teamsInRangfolge);
		}

		List<TeamPaarung> paarungen = new ArrayList<>(paareNachRangliste(zuPaarende, rangliste));
		if (freilosPaarung != null) {
			paarungen.add(freilosPaarung);
		}
		return paarungen;
	}

	/**
	 * Ermittelt das Team für das Freilos (BYE).
	 * <p>
	 * <b>Regel:</b>
	 * <ul>
	 *   <li>Primär: schlechtest platziertes Team ohne vorheriges BYE</li>
	 *   <li>Fallback: letztes Team wenn alle schon BYE hatten</li>
	 *   <li>Zusatz: Team darf nicht zweimal hintereinander BYE bekommen</li>
	 * </ul>
	 *
	 * @param rangliste sortierte Rangliste
	 * @param teams     Teams in Ranglisten-Reihenfolge
	 * @return das Team das BYE erhält
	 */
	@VisibleForTesting
	Team findeByeTeam(List<FormuleXErgebnis> rangliste, List<Team> teams) {
		// von hinten nach vorne suchen (schlechtest platzierte zuerst)
		for (int i = rangliste.size() - 1; i >= 0; i--) {
			FormuleXErgebnis ergebnis = rangliste.get(i);
			if (!ergebnis.hatteFreilos()) {
				return teams.get(i);
			}
		}
		// Fallback: alle hatten schon BYE → letztes Team
		return teams.getLast();
	}

	/**
	 * Paart die Teams nach Rangliste 1–2, 3–4, …; Gegnerwiederholungen nur, wenn sie unvermeidbar sind, und dann so
	 * wenige wie möglich ({@link WiederholungsarmePaarung}).
	 *
	 * @param rangliste  Teams in Ranglisten-Reihenfolge (gerade Anzahl)
	 * @param ergebnisse Ergebnisse mit den bisherigen Gegnern
	 * @return Liste der Paarungen, nach dem besser platzierten Team geordnet
	 */
	@VisibleForTesting
	List<TeamPaarung> paareNachRangliste(List<Team> rangliste, List<FormuleXErgebnis> ergebnisse) {
		Map<Integer, FormuleXErgebnis> ergebnisMap = ergebnisse.stream()
				.collect(Collectors.toMap(FormuleXErgebnis::teamNr, e -> e));
		return WiederholungsarmePaarung.paare(rangliste.size(),
				(a, b) -> hatGegeneinanderGespielt(rangliste.get(a), rangliste.get(b), ergebnisMap),
				WiederholungsarmePaarung.GegnerWahl.NAECHSTER_IN_RANGLISTE).stream()
				.map(paar -> new TeamPaarung(rangliste.get(paar.a()), rangliste.get(paar.b()))).toList();
	}

	/**
	 * Prüft ob zwei Teams bereits gegeneinander gespielt haben.
	 *
	 * @param teamA       erstes Team
	 * @param teamB       zweites Team
	 * @param ergebnisMap Map von teamNr zu Ergebnis (für Gegner-Prüfung)
	 * @return true wenn schon gegeneinander gespielt, false otherwise
	 */
	@VisibleForTesting
	boolean hatGegeneinanderGespielt(Team teamA, Team teamB, Map<Integer, FormuleXErgebnis> ergebnisMap) {
		FormuleXErgebnis ergebnisA = ergebnisMap.get(teamA.getNr());
		if (ergebnisA == null) {
			return false;
		}
		return ergebnisA.gegnerNrn().contains(teamB.getNr());
	}

	/**
	 * Flacht eine Liste von Paarungen zu einer sortierten Teamliste ab.
	 * Freilos-Paarungen (kein Team B) werden dabei übersprungen.
	 *
	 * @param paarungen die Paarungsliste
	 * @return flache, sortierte Teamliste (ohne Freilos-Einträge)
	 */
	public List<Team> flattenTeampaarungen(List<TeamPaarung> paarungen) {
		return paarungen.stream()
				.filter(tp -> !tp.isFreilos())
				.flatMap(tp -> Stream.concat(Stream.of(tp.getA()), tp.getOptionalB().stream()))
				.sorted(Comparator.comparingInt(Team::getNr))
				.toList();
	}
}
