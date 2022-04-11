/**
 * Erstellung 15.02.2020 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.collections4.ListUtils;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;

import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.model.TeamPaarung;

/**
 * @author Michael Massee
 *
 */
public class SchweizerSystem {

	private final List<Team> teamListe;
	private final boolean freiSpiel;
	private final int anzMeldungen;

	public SchweizerSystem(TeamMeldungen meldungen) {
		checkNotNull(meldungen);
		teamListe = meldungen.teams();
		anzMeldungen = meldungen.teams().size();
		freiSpiel = IsEvenOrOdd.IsOdd(anzMeldungen);
	}

	public int letzteMeldungNr() {
		return (freiSpiel) ? anzMeldungen + 1 : anzMeldungen;
	}

	public int anzPaarungen() {
		return letzteMeldungNr() / 2;
	}

	/**
	 * Die erste Runde wird frei ausgelost, wobei ein Setzen der vermeintlich stärksten Teams sinnvoll ist<br>
	 * (mit dem Ziel, diese nicht in der ersten Runde aufeinandertreffen zu lassen, und, bei ungerader Teilnehmerzahl,<br>
	 * diese nicht mit einem Freilos* beginnen zu lassen).
	 *
	 * @param setzPos ein Map mit TeamNr,Setzpos teams mit der gleiche Nummer werden nicht gegeneinander ausgelost
	 */
	public List<TeamPaarung> ersteRunde() {

		int anzTeamPaarungen = anzPaarungen();

		// first shuffle
		Collections.shuffle(teamListe);

		// now sort nach Setzpos
		List<Team> sortedTeamList = teamListe.stream().sorted((m1, m2) -> Integer.compare(m1.getSetzPos(), m2.getSetzPos())).collect(Collectors.toList());
		// split into 2 List Team A/Team B
		List<List<Team>> partition = ListUtils.partition(sortedTeamList, anzTeamPaarungen);

		// merge A+B list togehter in one list TeamPaarung
		List<Team> listATeams = partition.get(0);
		List<Team> listBTeams = partition.get(1);
		int listBSize = listBTeams.size();
		List<TeamPaarung> retList = IntStream.range(0, anzTeamPaarungen).mapToObj(i -> {
			return new TeamPaarung(listATeams.get(i), (((anzTeamPaarungen - i) <= listBSize) ? listBTeams.get(anzTeamPaarungen - i - 1) : null)).addGegner();
		}).sorted((tp1, tp2) -> {
			// Freilos an letzte Stelle
			if (tp1.getB() == null) {
				return 1;
			} else if (tp2.getB() == null) {
				return -1;
			}
			return Integer.compare(tp1.getA().getNr(), tp2.getA().getNr());
		}).collect(Collectors.toList());

		return retList;
	}

	/**
	 * die meldungen mussen in rangliste reihenfolge vorliegen
	 *
	 * @return
	 */

	public List<TeamPaarung> weitereRunde() {
		List<TeamPaarung> teamPaarungList = new ArrayList<>();

		// sicher gehen das hatgegner flag auf false
		teamListe.stream().forEach(team -> team.setHatGegner(false));

		// zuerst freilos vergeben
		if (freiSpiel) {
			Team freilosTeam = IntStream.range(0, teamListe.size()).mapToObj(i -> teamListe.get(teamListe.size() - i - 1)).filter(team -> team.isHatteFreilos() == false)
					.findFirst().orElse(teamListe.get(0));
			freilosTeam.setHatteFreilos(true);
			freilosTeam.setHatGegner(true);
			teamPaarungList.add(new TeamPaarung(freilosTeam));
		}

		for (Team team : teamListe) {
			if (!team.isHatGegner()) {
				List<Team> restTeams = teamListe.stream().filter(t -> !t.isHatGegner() && !team.equals(t)).collect(Collectors.toList());
				if (restTeams.size() > 0) {
					Team gegner = findeGegner(team, restTeams);
					if (gegner != null) {
						// gegner gefunden
						teamPaarungList.add(new TeamPaarung(team, gegner).addGegner().setHatGegner());
					} else {
						// ohne gegner ? versuchen ob wir tauschen können mit vorhanden team paarung aus der Liste
						// Invalid Paarung haben bereits gegen einander gespielt
						TeamPaarung invalid = new TeamPaarung(team, restTeams.get(0));

						TeamPaarung kannTauschenMit = kannTauschenMit(invalid, teamPaarungList);
						if (kannTauschenMit != null) {
							// wenn erfolgreich dann invalid == valid!
							tauschenTeamsInPaarung(invalid, kannTauschenMit);
							// gegner wieder herstellen von den invalid teams weil die in den vorrunden bereits gegen einander gespielt haben
							restTeams.get(0).addGegner(team);
						}
						// invalid oder wenn tausch statgefunden hat, Valid paarung hinzufügen
						teamPaarungList.add(invalid.addGegner().setHatGegner());
					}
				} else {
					// keine rest mehr vorhanden ?
					teamPaarungList.add(new TeamPaarung(team).setHatGegner());
				}
			}
		}
		return teamPaarungList.stream().sorted((tp1, tp2) -> {
			// Freilos an letzter Stelle
			if (tp1.getB() == null) {
				return 1;
			} else if (tp2.getB() == null) {
				return -1;
			}
			return 0;
		}).collect(Collectors.toList());
	}

	@VisibleForTesting
	boolean tauschenTeamsInPaarung(TeamPaarung paarA, TeamPaarung paarB) {
		if (!paarA.hasB() || !paarB.hasB()) {
			return false;
		}

		boolean didChange = false;

		// tausche B, A1:B2 <-> A2:B2
		if (!paarA.getA().hatAlsGegner(paarB.getB()) && !paarB.getA().hatAlsGegner(paarA.getB())) {
			paarA.removeGegner();
			paarB.removeGegner();
			Team A_Bteam = paarA.getB();
			paarA.setB(paarB.getB());
			paarB.setB(A_Bteam);
			didChange = true;
		}
		// tausche A+B, A1:A2 <-> B1:B2
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
	 * suche rückwärts in der liste nach ein gegnerteam-paarung zum tauschen
	 *
	 * @param team team ohne gegner
	 * @param paarungen bereits zugerodnete gegner
	 * @return
	 */
	@VisibleForTesting
	TeamPaarung kannTauschenMit(TeamPaarung invalidTeamPaarung, List<TeamPaarung> paarungen) {

		Team teamAInvalid = invalidTeamPaarung.getA();
		Team teamBInvalid = invalidTeamPaarung.getB();
		if (teamBInvalid == null) {
			return null;
		}

		// team suchen zum tauschen
		List<Team> reverseTeamliste = Lists.reverse(teamListe); // orginal wird nicht verändert

		Team tauschTeam = reverseTeamliste.stream().filter(teamRev -> {
			return teamRev.isHatGegner() && !teamRev.equals(teamAInvalid) && !teamRev.equals(teamBInvalid);
		}).filter(teamRev2 -> {
			// is a oder b Team
			boolean isA = isATeam(teamRev2, paarungen);
			// gegner vom potentielle tausch suchen
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
	 * finde paarung mit team1, and return true wenn A
	 *
	 * @param team1
	 * @param paarungen
	 * @return true wenn a false wenn b, wenn not in list found = true
	 */

	public boolean isATeam(Team team, List<TeamPaarung> paarungen) {
		return paarungen.stream().filter(teamPaarung -> {
			return teamPaarung.isInPaarung(team);
		}).findFirst().map(teamPaarung2 -> teamPaarung2.getA().equals(team)).orElse(true);
	}

	/**
	 * finde paarung mit team1, und return gegner
	 *
	 * @param team1
	 * @param paarungen
	 * @return der gegner von team1
	 */

	public Team findGegnerAusTeamPaarungen(Team team1, List<TeamPaarung> paarungen) {
		return paarungen.stream().filter(teamPaarung -> {
			return teamPaarung.isInPaarung(team1);
		}).findFirst().map(teamPaarung2 -> teamPaarung2.getGegner(team1)).orElse(null);
	}

	public List<Team> flattenTeampaarungen(List<TeamPaarung> paarungen) {
		return paarungen.stream().flatMap(teamPaarung -> Stream.of(teamPaarung.getA(), teamPaarung.getB())).sorted((team1, team2) -> {
			if (team1 == null) {
				return 1;
			} else if (team2 == null) {
				return 1;
			}
			return Integer.compare(team1.getNr(), team2.getNr());
		}).collect(Collectors.toList());
	}

	/**
	 * @param team
	 * @param restMeldungen
	 * @return
	 */
	@VisibleForTesting
	Team findeGegner(Team team, List<Team> restTeams) {
		return restTeams.stream().filter(teamAusRest -> {
			return !teamAusRest.equals(team) && !team.hatAlsGegner(teamAusRest) && !teamAusRest.hatAlsGegner(team);
		}).findFirst().orElse(null);
	}
}
