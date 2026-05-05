package de.petanqueturniermanager.jedergegenjeden;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;

/**
 * Teilt aktive JGJ-Meldungen in Gruppen auf.
 * <p>
 * Wird sowohl beim Aufbau des Spielplans als auch beim Schreiben der Rangliste verwendet,
 * damit beide stets dieselbe Gruppen-Zusammensetzung erzeugen.
 * <p>
 * Ohne gesetzte Setzpositionen werden die Teams blockweise in der Reihenfolge der Meldeliste
 * verteilt (Teams 1..k → Gruppe A, k+1..2k → Gruppe B, …) – das alte Verhalten bleibt erhalten.
 * <p>
 * Sobald mindestens ein Team eine Setzposition &gt; 0 trägt, werden Teams mit derselben
 * Setzposition garantiert in unterschiedliche Gruppen gelost. Reicht die Anzahl der Gruppen
 * nicht aus (zu viele Teams mit derselben Setzposition), landet das überzählige Team
 * in der kleinsten Gruppe – das ist ein Konfigurationsfehler des Anwenders.
 */
public final class JGJGruppenAufteiler {

	private JGJGruppenAufteiler() {
	}

	public static List<TeamMeldungen> teileInGruppen(TeamMeldungen meldungen, int gruppengroesse) {
		List<Team> teams = meldungen.teams();
		if (teams.isEmpty() || gruppengroesse <= 0) {
			return List.of(kopiere(teams));
		}
		boolean hatSetzPos = teams.stream().anyMatch(team -> team.getSetzPos() > 0);
		if (!hatSetzPos) {
			return blockweise(teams, gruppengroesse);
		}
		return mitSetzPositionen(teams, gruppengroesse);
	}

	private static List<TeamMeldungen> blockweise(List<Team> teams, int gruppengroesse) {
		List<TeamMeldungen> gruppen = new ArrayList<>();
		for (int i = 0; i < teams.size(); i += gruppengroesse) {
			TeamMeldungen gruppe = new TeamMeldungen();
			for (int j = i; j < Math.min(i + gruppengroesse, teams.size()); j++) {
				gruppe.addTeamWennNichtVorhanden(teams.get(j));
			}
			gruppen.add(gruppe);
		}
		return gruppen;
	}

	private static List<TeamMeldungen> mitSetzPositionen(List<Team> teams, int gruppengroesse) {
		int anzGruppen = (int) Math.ceil((double) teams.size() / gruppengroesse);
		List<TeamMeldungen> gruppen = new ArrayList<>(anzGruppen);
		for (int i = 0; i < anzGruppen; i++) {
			gruppen.add(new TeamMeldungen());
		}

		TreeMap<Integer, List<Team>> nachSetzPos = new TreeMap<>();
		List<Team> ohneSetzPos = new ArrayList<>();
		for (Team team : teams) {
			if (team.getSetzPos() > 0) {
				nachSetzPos.computeIfAbsent(team.getSetzPos(), key -> new ArrayList<>()).add(team);
			} else {
				ohneSetzPos.add(team);
			}
		}

		for (List<Team> bucket : nachSetzPos.values()) {
			Set<Integer> belegt = new HashSet<>();
			for (Team team : bucket) {
				int idx = kleinsteFreieGruppe(gruppen, belegt);
				gruppen.get(idx).addTeamWennNichtVorhanden(team);
				belegt.add(idx);
			}
		}

		for (Team team : ohneSetzPos) {
			int idx = kleinsteGruppe(gruppen);
			gruppen.get(idx).addTeamWennNichtVorhanden(team);
		}
		return gruppen;
	}

	private static int kleinsteGruppe(List<TeamMeldungen> gruppen) {
		int besterIdx = 0;
		int besteGroesse = gruppen.get(0).size();
		for (int i = 1; i < gruppen.size(); i++) {
			if (gruppen.get(i).size() < besteGroesse) {
				besteGroesse = gruppen.get(i).size();
				besterIdx = i;
			}
		}
		return besterIdx;
	}

	private static int kleinsteFreieGruppe(List<TeamMeldungen> gruppen, Set<Integer> bereitsBelegt) {
		int besterIdx = -1;
		int besteGroesse = Integer.MAX_VALUE;
		for (int i = 0; i < gruppen.size(); i++) {
			if (bereitsBelegt.contains(i)) {
				continue;
			}
			if (gruppen.get(i).size() < besteGroesse) {
				besteGroesse = gruppen.get(i).size();
				besterIdx = i;
			}
		}
		return besterIdx >= 0 ? besterIdx : kleinsteGruppe(gruppen);
	}

	private static TeamMeldungen kopiere(List<Team> teams) {
		TeamMeldungen einzige = new TeamMeldungen();
		for (Team team : teams) {
			einzige.addTeamWennNichtVorhanden(team);
		}
		return einzige;
	}
}
