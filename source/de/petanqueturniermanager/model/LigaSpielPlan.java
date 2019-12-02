/**
 * Erstellung 01.12.2019 / Michael Massee
 */
package de.petanqueturniermanager.model;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collections;
import java.util.List;

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

	public LigaSpielPlan schufflePlan() {
		for (int i = 0; i < 5; i++) { // 5 mal durchmischen
			Collections.shuffle(spielPlan);
		}
		return this;
	}

}
