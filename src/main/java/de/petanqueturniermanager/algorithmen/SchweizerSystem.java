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

import org.apache.commons.collections4.ListUtils;

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
		List<Team> sortedTeamList = teamListe.stream().sorted((m1, m2) -> Integer.compare(m1.getSetzpos(), m2.getSetzpos())).collect(Collectors.toList());
		// split into 2 List Team A/Team B
		List<List<Team>> partition = ListUtils.partition(sortedTeamList, anzTeamPaarungen);

		// merge A+B list togehter in one list TeamPaarung
		List<Team> listATeams = partition.get(0);
		List<Team> listBTeams = partition.get(1);
		int listBSize = listBTeams.size();
		List<TeamPaarung> retList = IntStream.range(0, anzTeamPaarungen).mapToObj(i -> {
			return new TeamPaarung(listATeams.get(i), (((anzTeamPaarungen - i) <= listBSize) ? listBTeams.get(anzTeamPaarungen - i - 1) : null));
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
		List<TeamPaarung> retList = null;

		List<Team> restTeams = new ArrayList<>(teamListe);

		for (Team team : teamListe) {
			Team gegner = findeGegner(team, restTeams);

		}

		return retList;
	}

	/**
	 * @param team
	 * @param restMeldungen
	 * @return
	 */
	Team findeGegner(Team team, List<Team> restTeams) {
		return restTeams.stream().filter(teamAusRest -> {
			return !teamAusRest.equals(team) && !team.hatAlsGegner(teamAusRest) && !teamAusRest.hatAlsGegner(team);
		}).findFirst().orElse(null);
	}
}
