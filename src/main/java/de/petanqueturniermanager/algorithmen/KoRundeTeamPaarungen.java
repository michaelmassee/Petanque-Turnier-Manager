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
 * Erzeugt Paarungen für eine K.O.-Runde nach dem „Erster gegen Letzten"-Prinzip.<br>
 * <br>
 * Algorithmus:
 * <ol>
 *   <li>Team an Rang 1 spielt gegen Team an Rang n, Rang 2 gegen Rang n-1, usw.</li>
 *   <li>Hat ein Team seinen natürlichen Gegner bereits gespielt, wird rückwärts ein
 *       freier Gegner gesucht ({@link #sucheGegnerFuer}).</li>
 *   <li>Existiert kein freier Gegner mehr, wird ein bereits zugeordnetes B-Team
 *       getauscht ({@link #kanntauschenMit}): Das bisherige B erhält einen neuen Gegner,
 *       das freigewordene B wird dem wartenden Team zugewiesen.</li>
 *   <li>Ist selbst das nicht möglich, wird eine Doppel-Paarung in Kauf genommen
 *       (Flag {@link #isDoppelteGespieltePaarungenVorhanden()}).</li>
 * </ol>
 *
 * @author Michael Massee
 */
public class KoRundeTeamPaarungen {

	private final TeamRangliste teamRangListe;
	private boolean doppelteGespieltePaarungenVorhanden = false;
	private final List<String> doppelteGespieltePaarungen = new ArrayList<>();

	public KoRundeTeamPaarungen(TeamRangliste teamRangListe) {
		checkNotNull(teamRangListe, "TeamRangliste == null");
		checkArgument(!teamRangListe.isEmpty(), "TeamRangliste.isEmpty");
		checkArgument(IsEvenOrOdd.IsEven(teamRangListe.size()), "Keine gerade anzahl Teams in der Liste");
		this.teamRangListe = teamRangListe;
	}

	public FormeSpielrunde generatSpielRunde() {
		FormeSpielrunde formeSpielrunde = new FormeSpielrunde(1);
		doppelteGespieltePaarungenVorhanden = false;
		doppelteGespieltePaarungen.clear();

		ArrayList<Team> teamRangListeWork = teamRangListe.getCloneTeamListe();
		int haelfte = teamRangListeWork.size() / 2;

		for (int cntr = 0; cntr < haelfte; cntr++) {
			Team teamA = teamRangListeWork.getFirst();
			Team teamB = sucheGegnerFuer(teamA, teamRangListeWork);

			if (teamB != null) {
				formeSpielrunde.addPaarungWennNichtVorhanden(new TeamPaarung(teamA, teamB));
				teamRangListeWork.remove(teamB);
			} else {
				TauschTeams tausch = kanntauschenMit(teamA, teamRangListeWork, formeSpielrunde);
				if (tausch != null) {
					Team freigegenerB = tausch.teamPaarungTausch().getB();
					tausch.teamPaarungTausch().setB(tausch.teamAusRangliste());
					teamRangListeWork.remove(tausch.teamAusRangliste());
					formeSpielrunde.addPaarungWennNichtVorhanden(new TeamPaarung(teamA, freigegenerB));
				} else {
					// kein valider Gegner und kein Tausch möglich → Doppel-Paarung
					teamB = teamRangListeWork.getLast();
					doppelteGespieltePaarungenVorhanden = true;
					doppelteGespieltePaarungen.add(teamA.getNr() + ":" + teamB.getNr());
					formeSpielrunde.addPaarungWennNichtVorhanden(new TeamPaarung(teamA, teamB));
					teamRangListeWork.remove(teamB);
				}
			}
			teamRangListeWork.remove(teamA);
		}

		return formeSpielrunde;
	}

	/**
	 * Sucht rückwärts in der Rangliste einen Gegner für teamA, gegen den teamA noch nicht gespielt hat.<br>
	 * Rückwärts-Suche stellt sicher, dass schwächere Teams (hinten in der Rangliste) bevorzugt als Gegner dienen.
	 *
	 * @param teamA             das Team, für das ein Gegner gesucht wird
	 * @param teamRangListeWork die noch verfügbaren Teams
	 * @return ein geeigneter Gegner oder {@code null} wenn keiner gefunden wurde
	 */
	Team sucheGegnerFuer(final Team teamA, final List<Team> teamRangListeWork) {
		return Lists.reverse(teamRangListeWork).stream()
				.filter(t -> !teamA.equals(t) && !teamA.hatAlsGegner(t))
				.findFirst()
				.orElse(null);
	}

	/**
	 * Versucht, einen Tausch zu finden: Ein bereits zugeordnetes B-Team wird mit einem
	 * noch verfügbaren Team ausgetauscht, so dass teamA den freigwordenen B-Platz übernehmen kann.
	 *
	 * @param teamA            das Team ohne gültigen Gegner
	 * @param teamRangListeWork die noch verfügbaren Teams
	 * @param formeSpielrunde  die bisher erzeugten Paarungen (Tauschkandidaten)
	 * @return ein {@link TauschTeams}-Objekt oder {@code null} wenn kein Tausch möglich ist
	 */
	TauschTeams kanntauschenMit(final Team teamA, final List<Team> teamRangListeWork, final FormeSpielrunde formeSpielrunde) {
		List<Team> reversedRangliste = Lists.reverse(teamRangListeWork);

		for (Team teamBAusSpielrunde : formeSpielrunde.getBTeams()) {
			if (!teamA.hatAlsGegner(teamBAusSpielrunde)) {
				TeamPaarung paarungTausch = formeSpielrunde.findTeamPaarung(teamBAusSpielrunde);
				for (Team teamAusRangliste : reversedRangliste) {
					if (!teamA.equals(teamAusRangliste) && !paarungTausch.getA().hatAlsGegner(teamAusRangliste)) {
						return new TauschTeams(teamAusRangliste, paarungTausch);
					}
				}
			}
		}
		return null;
	}

	public TeamRangliste getTeamRangListe() {
		return teamRangListe;
	}

	/** @return {@code true} wenn mindestens eine Doppel-Paarung (bereits gespielt) entstanden ist */
	public boolean isDoppelteGespieltePaarungenVorhanden() {
		return doppelteGespieltePaarungenVorhanden;
	}

	/** @return leerzeichengetrennte Liste der Doppel-Paarungen, z.B. {@code "1:4 2:5"} */
	public String getDoppelteGespieltePaarungen() {
		return String.join(" ", doppelteGespieltePaarungen);
	}
}

/** Kapselt einen gefundenen Tausch: das Team aus der Rangliste und die Paarung, deren B getauscht wird. */
record TauschTeams(Team teamAusRangliste, TeamPaarung teamPaarungTausch) {}
