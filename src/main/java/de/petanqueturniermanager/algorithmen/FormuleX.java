/**
 * Erstellung 27.04.2026 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.collections4.ListUtils;

import com.google.common.annotations.VisibleForTesting;

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
 *   <li>Rematch-Vermeidung durch Swap-Strategie</li>
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
	 * Sortiert eine Liste von Teamergebnissen nach den Wertungspunkten des Formule X Systems.
	 * <p>
	 * <b>Sortierreihenfolge:</b>
	 * <ol>
	 *   <li>Wertungsscore (absteigend)</li>
	 *   <li>Punktedifferenz (absteigend)</li>
	 *   <li>Eigene Punkte (absteigend)</li>
	 *   <li>TeamNr (aufsteigend, für stabile Sortierung)</li>
	 * </ol>
	 *
	 * @param ergebnisse die Teamergebnisse
	 * @param runden     Anzahl der Runden (für Wertungsberechnung)
	 * @return neue Liste, sortiert nach Wertungspunkten (bestes Team zuerst)
	 */
	public List<FormuleXErgebnis> sortiereNachWertung(List<FormuleXErgebnis> ergebnisse, int runden) {
		return ergebnisse.stream()
				.sorted(Comparator
						.comparingInt((FormuleXErgebnis e) -> berechneWertung(e, runden)).reversed()
						.thenComparing(Comparator.comparingInt(FormuleXErgebnis::punktedifferenz).reversed())
						.thenComparing(Comparator.comparingInt(FormuleXErgebnis::eigenePunkte).reversed())
						.thenComparingInt(FormuleXErgebnis::teamNr))
				.collect(Collectors.toList());
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
		Collections.shuffle(shuffled);

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
	 *   <li>Rematch-Vermeidung durch Swap-Strategie</li>
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

		List<TeamPaarung> paarungen = new ArrayList<>(paareLinearMitSwap(zuPaarende, rangliste));
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
	 * Paart Teams linear nach Rangliste mit Swap-Strategie bei Rematch.
	 * <p>
	 * <b>Algorithmus:</b>
	 * <ol>
	 *   <li>Gehe Liste paarweise durch (1vs2, 3vs4, ...)</li>
	 *   <li>Prüfe ob Paarung schon gespielt (Rematch)</li>
	 *   <li>Wenn Rematch: suche Swap-Partner (nächstes Paar, dann weiter)</li>
	 *   <li>Wenn kein Swap möglich: akzeptiere Rematch (Fail-Safe)</li>
	 * </ol>
	 *
	 * @param rangliste Teams in Ranglisten-Reihenfolge
	 * @param ergebnisse Ergebnisse für Rematch-Prüfung
	 * @return Liste der Paarungen
	 */
	@VisibleForTesting
	List<TeamPaarung> paareLinearMitSwap(List<Team> rangliste, List<FormuleXErgebnis> ergebnisse) {
		List<TeamPaarung> paarungen = new ArrayList<>();
		Map<Integer, FormuleXErgebnis> ergebnisMap = ergebnisse.stream()
				.collect(Collectors.toMap(FormuleXErgebnis::teamNr, e -> e));
		Set<Integer> verarbeitet = new HashSet<>();

		for (int i = 0; i < rangliste.size() - 1; i += 2) {
			if (verarbeitet.contains(i)) {
				continue;
			}

			Team teamA = rangliste.get(i);
			Team teamB = rangliste.get(i + 1);

			if (!hatGegeneinanderGespielt(teamA, teamB, ergebnisMap)) {
				paarungen.add(new TeamPaarung(teamA, teamB));
			} else {
				boolean swapErfolgreich = false;

				for (int j = i + 2; j + 1 < rangliste.size(); j += 2) {
					if (verarbeitet.contains(j)) {
						continue;
					}
					Team teamC = rangliste.get(j);
					Team teamD = rangliste.get(j + 1);

					// Teste Swap B↔D: (A,C) und (B,D)
					if (!hatGegeneinanderGespielt(teamA, teamC, ergebnisMap)
							&& !hatGegeneinanderGespielt(teamB, teamD, ergebnisMap)) {
						paarungen.add(new TeamPaarung(teamA, teamC));
						paarungen.add(new TeamPaarung(teamB, teamD));
						verarbeitet.add(j);
						swapErfolgreich = true;
						break;
					}

					// Teste Swap A↔C: (A,D) und (C,B)
					if (!hatGegeneinanderGespielt(teamA, teamD, ergebnisMap)
							&& !hatGegeneinanderGespielt(teamC, teamB, ergebnisMap)) {
						paarungen.add(new TeamPaarung(teamA, teamD));
						paarungen.add(new TeamPaarung(teamC, teamB));
						verarbeitet.add(j);
						swapErfolgreich = true;
						break;
					}
				}

				if (!swapErfolgreich) {
					// Fail-Safe: Rematch akzeptieren
					paarungen.add(new TeamPaarung(teamA, teamB));
				}
			}
		}

		return paarungen;
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
