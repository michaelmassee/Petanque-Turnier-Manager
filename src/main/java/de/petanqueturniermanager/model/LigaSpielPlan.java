/**
 * Erstellung 01.12.2019 / Michael Massee
 */
package de.petanqueturniermanager.model;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import de.petanqueturniermanager.algorithmen.JederGegenJeden;

/**
 * @author Michael Massee
 *
 */
public class LigaSpielPlan {

	private final List<List<TeamPaarung>> spielPlan;

	public LigaSpielPlan(TeamMeldungen meldungen) {
		checkNotNull(meldungen);
		spielPlan = new JederGegenJeden(meldungen).generate();
	}

	public List<List<TeamPaarung>> getSpielPlan() {
		return spielPlan;
	}

	public List<List<TeamPaarung>> getSpielPlanClone() {
		return spielPlan.stream().map(teamparungList -> {
			return teamparungList.stream().map(teamPaarung -> {
				return (TeamPaarung) teamPaarung.clone();
			}).collect(Collectors.toList());
		}).collect(Collectors.toList());
	}

	public LigaSpielPlan schufflePlan() {
		for (int i = 0; i < 5; i++) { // 5 mal durchmischen
			Collections.shuffle(spielPlan);
		}
		return this;
	}

	/**
	 * wegen heim und gast spiel, einmal parungen A und B umdrehen <br>
	 * macht nur sinn wenn ungerade anzahl an spielen
	 *
	 * @return
	 */
	public LigaSpielPlan flipTeams() {
		getSpielPlan().stream().flatMap(Collection::stream).forEach(teamPaarung -> {
			// nur wenn team B vorhanden
			teamPaarung.flipTeams();
		});
		return this;
	}

	public int anzBegnungenProRunde() {
		if (spielPlan != null && spielPlan.size() > 0) {
			return spielPlan.get(0).size();
		}
		return 0;
	}

	public int anzRunden() {
		return spielPlan.size();
	}

}
