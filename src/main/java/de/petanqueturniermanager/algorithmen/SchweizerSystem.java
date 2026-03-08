/**
 * Erstellung 15.02.2020 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen;

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
import com.google.common.collect.Lists;

import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamPaarung;

/**
 * Implementiert die Logik für ein Turnier nach Schweizer System.
 * <p>
 * Das Schweizer System ist ein Turniermodus, der es ermöglicht, mit einer
 * großen Anzahl von Teilnehmern in relativ wenigen Runden einen Sieger zu
 * ermitteln. Alle Teams spielen in jeder Runde mit.
 * <p>
 * Ab der zweiten Runde spielt man idealerweise immer gegen Gegner, die in etwa
 * gleich erfolgreich sind (z. B. spielen Sieger gegen Sieger).
 *
 * @see <a href="SchweizerTurnierSystem.md">SchweizerTurnierSystem.md</a>
 */
public class SchweizerSystem {

	/**
	 * Sortiert eine Liste von Teamergebnissen nach den Auswertungskriterien des Schweizer Systems:
	 * <ol>
	 *   <li>Anzahl der Siege (absteigend)</li>
	 *   <li>Buchholz / BHZ = Summe der Siege aller Gegner (absteigend)</li>
	 *   <li>Feinbuchholz / FBHZ = Summe der BHZ-Werte aller Gegner (absteigend)</li>
	 *   <li>Punktedifferenz = erzielte minus kassierte Punkte (absteigend)</li>
	 * </ol>
	 * Hinweis: Direktvergleich (Kriterium 5) und Los (Kriterium 6) werden hier nicht berechnet
	 * und müssen bei Bedarf nachgelagert behandelt werden.
	 *
	 * @param ergebnisse die Teamergebnisse mit Siegen, Punktedifferenz und Gegnerliste
	 * @return neue Liste, sortiert nach Auswertungskriterien (beste Team zuerst)
	 */
	public List<SchweizerTeamErgebnis> sortiereNachAuswertungskriterien(List<SchweizerTeamErgebnis> ergebnisse) {
		// BHZ (Buchholz) für jedes Team berechnen: Summe der Siege aller Gegner
		Map<Integer, Integer> bhzMap = berechneBuchholz(ergebnisse);

		// FBHZ (Feinbuchholz) berechnen: Summe der BHZ-Werte aller Gegner
		Map<Integer, Integer> fbhzMap = berechneFeinbuchholz(ergebnisse, bhzMap);

		Comparator<SchweizerTeamErgebnis> comparator = Comparator
				.comparingInt(SchweizerTeamErgebnis::siege).reversed()
				.thenComparing(Comparator.comparingInt(
						(SchweizerTeamErgebnis e) -> bhzMap.getOrDefault(e.teamNr(), 0)).reversed())
				.thenComparing(Comparator.comparingInt(
						(SchweizerTeamErgebnis e) -> fbhzMap.getOrDefault(e.teamNr(), 0)).reversed())
				.thenComparing(Comparator.comparingInt(SchweizerTeamErgebnis::punktedifferenz).reversed());

		return ergebnisse.stream().sorted(comparator).collect(Collectors.toList());
	}

	/**
	 * Berechnet den Buchholz-Wert für jedes Team.
	 * BHZ = Summe der Siege aller Gegner am Turnierend.
	 *
	 * @param ergebnisse alle Teamergebnisse
	 * @return Map von teamNr → BHZ-Wert
	 */
	@VisibleForTesting
	Map<Integer, Integer> berechneBuchholz(List<SchweizerTeamErgebnis> ergebnisse) {
		Map<Integer, Integer> siegeMap = ergebnisse.stream()
				.collect(Collectors.toMap(SchweizerTeamErgebnis::teamNr, SchweizerTeamErgebnis::siege));

		return ergebnisse.stream().collect(Collectors.toMap(
				SchweizerTeamErgebnis::teamNr,
				e -> e.gegnerNrn().stream()
						.mapToInt(gegnerNr -> siegeMap.getOrDefault(gegnerNr, 0))
						.sum()));
	}

	/**
	 * Berechnet den Feinbuchholz-Wert für jedes Team.
	 * FBHZ = Summe der BHZ-Werte aller Gegner.
	 *
	 * @param ergebnisse alle Teamergebnisse
	 * @param bhzMap     vorberechnete BHZ-Werte
	 * @return Map von teamNr → FBHZ-Wert
	 */
	@VisibleForTesting
	Map<Integer, Integer> berechneFeinbuchholz(List<SchweizerTeamErgebnis> ergebnisse,
			Map<Integer, Integer> bhzMap) {
		return ergebnisse.stream().collect(Collectors.toMap(
				SchweizerTeamErgebnis::teamNr,
				e -> e.gegnerNrn().stream()
						.mapToInt(gegnerNr -> bhzMap.getOrDefault(gegnerNr, 0))
						.sum()));
	}

	/**
	 * Die erste Runde wird frei ausgelost, wobei ein Setzen der vermeintlich stärksten Teams sinnvoll ist
	 * (mit dem Ziel, diese nicht in der ersten Runde aufeinandertreffen zu lassen, und, bei ungerader Teilnehmerzahl,
	 * diese nicht mit einem Freilos beginnen zu lassen).
	 *
	 * @param teams die Liste der teilnehmenden Teams; Teams mit gleicher SetzPos werden nicht gegeneinander ausgelost
	 * @return eine Liste der Paarungen für die erste Runde
	 */
	public List<TeamPaarung> ersteRunde(List<Team> teams) {
		boolean freiSpiel = IsEvenOrOdd.IsOdd(teams.size());
		int letzteMeldungNr = freiSpiel ? teams.size() + 1 : teams.size();
		int anzTeamPaarungen = letzteMeldungNr / 2;

		// first shuffle
		List<Team> shuffled = new ArrayList<>(teams);
		Collections.shuffle(shuffled);

		// now sort nach Setzpos
		List<Team> sortedTeamList = shuffled.stream()
				.sorted((m1, m2) -> Integer.compare(m1.getSetzPos(), m2.getSetzPos())).toList();

		// split into 2 List Team A/Team B
		List<List<Team>> partition = ListUtils.partition(sortedTeamList, anzTeamPaarungen);

		// merge A+B list together in one list TeamPaarung
		List<Team> listATeams = partition.get(0);
		List<Team> listBTeams = partition.get(1);
		int listBSize = listBTeams.size();
		return IntStream.range(0, anzTeamPaarungen).mapToObj(i -> {
			return new TeamPaarung(listATeams.get(i),
					(((anzTeamPaarungen - i) <= listBSize) ? listBTeams.get(anzTeamPaarungen - i - 1) : null))
					.addGegner();
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
	 * Ermittelt die Paarungen für die nächste Runde nach den Regeln des Schweizer Systems.
	 * <p>
	 * <b>Paarungs-Regel: nur Siege entscheiden über die Gruppenzugehörigkeit.</b>
	 * Buchholz (BHZ) und Feinbuchholz (FBHZ) sind <em>ausschließlich</em> Ranglisten-Kriterien
	 * (Tie-Breaks) und beeinflussen das Pairing <em>nicht direkt</em>.
	 * <p>
	 * <b>Ablauf:</b>
	 * <ol>
	 *   <li>Teams werden anhand ihrer Siegzahl in <b>Sieggruppen</b> eingeteilt
	 *       (z.B. „3 Siege", „2 Siege", …).</li>
	 *   <li>Innerhalb jeder Gruppe werden Teams nach ihrer Ranglistenposition sortiert,
	 *       die durch {@code sortiereNachAuswertungskriterien()} (Siege→BHZ→FBHZ→Punktediff)
	 *       bestimmt wird. BHZ wirkt also <em>indirekt</em>: es legt fest, wer in der Gruppe
	 *       oben steht und damit gegen wen gepaart wird – aber nicht, in welche Gruppe ein Team fällt.</li>
	 *   <li>Ist eine Gruppe ungerade, wird das rangniedrigste Team in die nächsttiefere
	 *       Gruppe „gefloatet" (Carry-Over).</li>
	 * </ol>
	 * <p>
	 * Gibt es keine Ergebnis-Information ({@code ergebnisse} leer), landen alle Teams in einer
	 * Gruppe – identisches Verhalten wie die klassische globale Paarung.
	 *
	 * @param teams      die Teams in aktueller Ranglisten-Reihenfolge (Siege absteigend,
	 *                   Tie-Breaks via {@code sortiereNachAuswertungskriterien()})
	 * @param ergebnisse bisherige Spielergebnisse (nur Siegzahl wird für Gruppen verwendet);
	 *                   leer = globale Paarung ohne Gruppenbildung
	 * @return eine Liste der Paarungen für die nächste Runde (Freilos am Ende)
	 */
	public List<TeamPaarung> weitereRunde(List<Team> teams, List<SchweizerTeamErgebnis> ergebnisse) {
		boolean freiSpiel = IsEvenOrOdd.IsOdd(teams.size());
		List<TeamPaarung> teamPaarungList = new ArrayList<>();

		// sicher gehen dass hatgegner flag auf false
		teams.forEach(team -> team.setHatGegner(false));

		// zuerst freilos vergeben
		if (freiSpiel) {
			Team freilosTeam = IntStream.range(0, teams.size())
					.mapToObj(i -> teams.get(teams.size() - i - 1))
					.filter(team -> !team.isHatteFreilos()).findFirst().orElse(teams.get(0));
			freilosTeam.setHatteFreilos(true);
			freilosTeam.setHatGegner(true);
			teamPaarungList.add(new TeamPaarung(freilosTeam));
		}

		// Sieggruppen bauen und innerhalb paaren
		Map<Integer, Integer> siegeProTeam = ergebnisse.stream()
				.collect(Collectors.toMap(SchweizerTeamErgebnis::teamNr, SchweizerTeamErgebnis::siege));

		List<Team> aktivTeams = teams.stream().filter(t -> !t.isHatGegner()).toList();
		List<List<Team>> gruppen = baueSiegeGruppen(aktivTeams, siegeProTeam);
		paareGruppen(gruppen, teamPaarungList, teams);

		return teamPaarungList.stream().sorted((tp1, tp2) -> {
			// Freilos an letzter Stelle
			if (tp1.isFreilos()) {
				return 1;
			} else if (tp2.isFreilos()) {
				return -1;
			}
			return 0;
		}).toList();
	}

	/**
	 * Gruppiert die sortierten Teams nach Siegzahl.
	 * Ungerade Gruppen werden durch Float des letzten Teams in die nächste Gruppe ausgeglichen.
	 * <p>
	 * Gibt es keine Sieginformation (leere siegeProTeam-Map), landen alle Teams in einer Gruppe
	 * und der Algorithmus verhält sich wie die klassische globale Paarung.
	 *
	 * @param sortierteTeams Teams in Ranglistenreihenfolge (Siegzahl absteigend)
	 * @param siegeProTeam   Map teamNr → Siegzahl
	 * @return Liste von Sieggruppen; jede Gruppe hat gerade Größe (nach Float-Ausgleich)
	 */
	@VisibleForTesting
	List<List<Team>> baueSiegeGruppen(List<Team> sortierteTeams, Map<Integer, Integer> siegeProTeam) {
		List<List<Team>> gruppen = new ArrayList<>();
		List<Team> aktuelleGruppe = null;
		int letztesSiege = Integer.MIN_VALUE;

		for (Team team : sortierteTeams) {
			int siege = siegeProTeam.getOrDefault(team.getNr(), 0);
			if (aktuelleGruppe == null || siege != letztesSiege) {
				aktuelleGruppe = new ArrayList<>();
				gruppen.add(aktuelleGruppe);
				letztesSiege = siege;
			}
			aktuelleGruppe.add(team);
		}

		// Ungerade Gruppen: schwächstes Team in nächsttiefere Gruppe floaten (Carry-Over)
		for (int i = 0; i < gruppen.size() - 1; i++) {
			List<Team> gruppe = gruppen.get(i);
			if (gruppe.size() % 2 != 0) {
				Team floatTeam = gruppe.removeLast();
				gruppen.get(i + 1).addFirst(floatTeam); // Float-Team wird zuerst gepaart
			}
		}
		gruppen.removeIf(List::isEmpty);
		return gruppen;
	}

	/**
	 * Paart Teams innerhalb jeder Sieggruppe.
	 * Greedy: erstes unpaartes Team bekommt den ersten erlaubten Gegner aus seiner Gruppe.
	 * Falls kein direkter Gegner frei: Tausch-Versuch mit bereits gepaartem Team.
	 *
	 * @param gruppen        Sieggruppen (jede gerade Größe)
	 * @param teamPaarungList Ergebnisliste (wird befüllt)
	 * @param alleTeams      alle Teams (für Tauschsuche über Gruppengrenzen)
	 */
	private void paareGruppen(List<List<Team>> gruppen, List<TeamPaarung> teamPaarungList, List<Team> alleTeams) {
		for (List<Team> gruppe : gruppen) {
			for (Team team : gruppe) {
				if (!team.isHatGegner()) {
					List<Team> restTeams = gruppe.stream().filter(t -> !t.isHatGegner() && !team.equals(t)).toList();
					if (!restTeams.isEmpty()) {
						Team gegner = findeGegner(team, restTeams);
						if (gegner != null) {
							teamPaarungList.add(new TeamPaarung(team, gegner).addGegner().setHatGegner());
						} else {
							// alle in der Gruppe bereits Gegner – Tausch versuchen
							TeamPaarung invalid = new TeamPaarung(team, restTeams.getFirst());
							TeamPaarung kannTauschenMit = kannTauschenMit(invalid, teamPaarungList, alleTeams);
							if (kannTauschenMit != null) {
								tauschenTeamsInPaarung(invalid, kannTauschenMit);
								restTeams.getFirst().addGegner(team);
							}
							teamPaarungList.add(invalid.addGegner().setHatGegner());
						}
					}
				}
			}
		}
	}

	@VisibleForTesting
	boolean tauschenTeamsInPaarung(TeamPaarung paarA, TeamPaarung paarB) {
		if (!paarA.hasB() || !paarB.hasB()) {
			return false;
		}

		boolean didChange = false;

		// tausche B: A1:B2 <-> A2:B1
		if (!paarA.getA().hatAlsGegner(paarB.getB()) && !paarB.getA().hatAlsGegner(paarA.getB())) {
			paarA.removeGegner();
			paarB.removeGegner();
			Team A_Bteam = paarA.getB();
			paarA.setB(paarB.getB());
			paarB.setB(A_Bteam);
			didChange = true;
		}
		// tausche A+B: A1:A2 <-> B1:B2
		if (!didChange && !paarA.getA().hatAlsGegner(paarB.getA()) && !paarB.getB().hatAlsGegner(paarA.getB())) {
			paarA.removeGegner();
			paarB.removeGegner();
			Team A_Bteam = paarA.getB();
			paarA.setB(paarB.getA());
			paarB.setA(A_Bteam);
			didChange = true;
		}

		if (didChange) {
			paarA.setHatGegner();
			paarA.addGegner();
			paarB.setHatGegner();
			paarB.addGegner();
		}

		return didChange;
	}

	/**
	 * Suche rückwärts in der Teamliste nach einer Gegner-Paarung zum Tauschen.
	 *
	 * @param invalidTeamPaarung Paarung, die noch keinen gültigen Gegner hat
	 * @param paarungen          bereits zugeordnete Paarungen
	 * @param teamListe          alle Teams in Reihenfolge (für rückwärtige Suche)
	 * @return eine Paarung, mit der getauscht werden kann, oder null
	 */
	@VisibleForTesting
	TeamPaarung kannTauschenMit(TeamPaarung invalidTeamPaarung, List<TeamPaarung> paarungen, List<Team> teamListe) {

		Team teamAInvalid = invalidTeamPaarung.getA();
		Team teamBInvalid = invalidTeamPaarung.getB();
		if (teamBInvalid == null) {
			return null;
		}

		// team suchen zum tauschen
		List<Team> reverseTeamliste = Lists.reverse(teamListe); // original wird nicht verändert

		Team tauschTeam = reverseTeamliste.stream().filter(teamRev -> {
			return teamRev.isHatGegner() && !teamRev.equals(teamAInvalid) && !teamRev.equals(teamBInvalid);
		}).filter(teamRev2 -> {
			// ist a- oder b-Team?
			boolean isA = isATeam(teamRev2, paarungen);
			// Gegner vom potenziellen Tauschteam suchen
			Team gegnerVonteamRev2 = findGegnerAusTeamPaarungen(teamRev2, paarungen);

			if (gegnerVonteamRev2 == null) {
				return false;
			}

			if (isA) {
				// kann gegen Spielen, A Invalid kann getauscht werden
				return teamRev2.hatAlsGegner(teamBInvalid) && !gegnerVonteamRev2.hatAlsGegner(teamAInvalid);
			}
			// kann gegen Spielen, B Invalid kann getauscht werden
			return !teamRev2.hatAlsGegner(teamAInvalid) && !gegnerVonteamRev2.hatAlsGegner(teamBInvalid);
		}).findFirst().orElse(null);

		if (tauschTeam != null) {
			return paarungen.stream().filter(paarung -> paarung.isInPaarung(tauschTeam)).findFirst().orElse(null);
		}
		return null;
	}

	/**
	 * Findet die Paarung mit team und gibt zurück, ob es das A-Team ist.
	 *
	 * @param team      das gesuchte Team
	 * @param paarungen die Liste der Paarungen
	 * @return true wenn A, false wenn B; wenn nicht in Liste gefunden: true
	 */
	@VisibleForTesting
	boolean isATeam(Team team, List<TeamPaarung> paarungen) {
		return paarungen.stream()
				.filter(teamPaarung -> teamPaarung.isInPaarung(team))
				.findFirst()
				.map(teamPaarung2 -> teamPaarung2.getA().equals(team))
				.orElse(true);
	}

	/**
	 * Findet die Paarung mit team1 und gibt den Gegner zurück.
	 *
	 * @param team1     das Team, dessen Gegner gesucht wird
	 * @param paarungen die Liste der Paarungen
	 * @return der Gegner von team1, oder null wenn kein Gegner oder nicht gefunden
	 */
	@VisibleForTesting
	Team findGegnerAusTeamPaarungen(Team team1, List<TeamPaarung> paarungen) {
		return paarungen.stream()
				.filter(teamPaarung -> teamPaarung.isInPaarung(team1))
				.findFirst()
				.map(teamPaarung2 -> teamPaarung2.getGegner(team1))
				.orElse(null);
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
				.flatMap(tp -> Stream.concat(Stream.of(tp.getA()), tp.getOptionalB().stream()))
				.sorted(Comparator.comparingInt(Team::getNr))
				.toList();
	}

	/**
	 * Findet einen geeigneten Gegner für ein Team aus der Restliste.
	 * Ein Gegner ist geeignet, wenn die Teams noch nicht gegeneinander gespielt haben.
	 *
	 * @param team      das Team, das einen Gegner sucht
	 * @param restTeams mögliche Gegner
	 * @return ein geeignetes Gegnerteam, oder null wenn keines gefunden
	 */
	@VisibleForTesting
	Team findeGegner(Team team, List<Team> restTeams) {
		return restTeams.stream().filter(teamAusRest -> {
			return !teamAusRest.equals(team) && !team.hatAlsGegner(teamAusRest) && !teamAusRest.hatAlsGegner(team);
		}).findFirst().orElse(null);
	}
}
