/**
 * Erstellung 24.05.2019 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;

import de.petanqueturniermanager.model.FormeSpielrunde;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamPaarung;
import de.petanqueturniermanager.model.TeamRangliste;

/**
 * @author Michael Massee
 *
 */
public class KoRundeTeamPaarungen {
	private final TeamRangliste teamRangListe;
	private boolean doppelteGespieltePaarungenVorhanden = false;
	private String doppelteGespieltePaarungen = "";

	public KoRundeTeamPaarungen(TeamRangliste teamRangListe) {
		checkNotNull(teamRangListe, "TeamRangliste == null");
		checkArgument(!teamRangListe.isEmpty(), "TeamRangliste.isEmpty");
		checkArgument(IsEvenOrOdd.IsEven(teamRangListe.size()), "Keine gerade anzahl Teams in der Liste");
		this.teamRangListe = teamRangListe;
	}

	public FormeSpielrunde generatSpielRunde() {
		FormeSpielrunde formeSpielrunde = new FormeSpielrunde(1);
		doppelteGespieltePaarungenVorhanden = false;

		// erste gegen letzte spielen usw.

		// orginal liste clonen zum arbeiten
		ArrayList<Team> teamRangListeWork = teamRangListe.getCloneTeamListe();
		int haelfte = teamRangListeWork.size() / 2;

		for (int cntr = 0; cntr < haelfte; cntr++) {
			Team teamA = teamRangListeWork.get(0);
			Team teamB = sucheGegnerFuer(teamA, teamRangListeWork);
			boolean hatgetauscht = false;

			if (teamB == null) {
				// kein gegner gefunden, kann tauschen mit andere B Team ?
				TauschTeams kanntauschenMit = kanntauschenMit(teamA, teamRangListeWork, formeSpielrunde);

				if (kanntauschenMit == null) {
					// immer noch null ? dann doppelt spielen lassen
					teamB = teamRangListeWork.get(teamRangListeWork.size() - 1);
					doppelteGespieltePaarungenVorhanden = true;
					setDoppelteGespieltePaarungen(getDoppelteGespieltePaarungen() + (" " + teamA.getNr() + ":" + teamB.getNr()));
				} else {
					// team austauschen in spielrunde
					hatgetauscht = true;
					Team teamBAusTeamPaarung = kanntauschenMit.getTeamPaarungTausch().getB();
					kanntauschenMit.getTeamPaarungTausch().setB(kanntauschenMit.getTeamAusRangliste());
					teamRangListeWork.remove(kanntauschenMit.getTeamAusRangliste());
					formeSpielrunde.addPaarungWennNichtVorhanden(new TeamPaarung(teamA, teamBAusTeamPaarung));
				}
			}

			if (!hatgetauscht) {
				formeSpielrunde.addPaarungWennNichtVorhanden(new TeamPaarung(teamA, teamB));
				teamRangListeWork.remove(teamB);
			}
			teamRangListeWork.remove(teamA);

		}

		return formeSpielrunde;
	}

	/**
	 * suche für ein rangliste höhere Team ein rangliste niedrige Team. Damit die A Teams möglichst lange im Turnier bleiben.<br>
	 * die teamRangListeWork wird rückwärts abgesucht
	 *
	 * @param teamA
	 * @param teamRangListeWork
	 * @return
	 */

	Team sucheGegnerFuer(final Team teamA, final List<Team> teamRangListeWork) {
		Team gegner = null;
		// rückwärts suchen
		List<Team> reverseteamRangListeWork = Lists.reverse(teamRangListeWork);
		for (Team teamAusRangliste : reverseteamRangListeWork) {
			if (!teamA.equals(teamAusRangliste) && !teamA.hatAlsGegner(teamAusRangliste)) {
				gegner = teamAusRangliste;
				break;
			}
		}
		return gegner;
	}

	TauschTeams kanntauschenMit(final Team teamA, final List<Team> teamRangListeWork, final FormeSpielrunde formeSpielrunde) {
		// suche in der Gruppe nach ein gegner
		List<Team> bTeams = formeSpielrunde.getBTeams();
		List<Team> reverseteamRangListeWork = Lists.reverse(teamRangListeWork);

		// suchen
		for (Team teamBAusSpielrunde : bTeams) {
			if (!teamA.hatAlsGegner(teamBAusSpielrunde)) {
				// Team A aus paarungen prüfen
				TeamPaarung teamPaarungTausch = formeSpielrunde.findTeamPaarung(teamBAusSpielrunde);
				// neuer B gegner für Team aus Paarungen ?
				// rückwarts suchen !
				for (Team teamAusRangliste : reverseteamRangListeWork) {
					if (!teamA.equals(teamAusRangliste) && !teamPaarungTausch.getA().hatAlsGegner(teamAusRangliste)) {
						return new TauschTeams(teamAusRangliste, teamPaarungTausch);
					}
				}
			}
		}
		return null;
	}

	public TeamRangliste getTeamRangListe() {
		return teamRangListe;
	}

	public boolean isDoppelteGespieltePaarungenVorhanden() {
		return doppelteGespieltePaarungenVorhanden;
	}

	public String getDoppelteGespieltePaarungen() {
		return doppelteGespieltePaarungen;
	}
}

class TauschTeams {

	private final Team teamAusRangliste;
	private final TeamPaarung teamPaarungTausch;

	public TauschTeams(Team teamAusRangliste, TeamPaarung teamPaarungTausch) {
		this.teamAusRangliste = teamAusRangliste;
		this.teamPaarungTausch = teamPaarungTausch;
	}

	public Team getTeamAusRangliste() {
		return teamAusRangliste;
	}

	public TeamPaarung getTeamPaarungTausch() {
		return teamPaarungTausch;
	}

}
