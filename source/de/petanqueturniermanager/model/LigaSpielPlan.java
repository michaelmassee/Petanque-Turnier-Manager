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

	public LigaSpielPlan(List<Meldung> meldungen) {
		checkNotNull(meldungen);
		spielPlan = new JederGegenJeden(meldungen).generate();
	}

	public List<List<TeamPaarung>> getSpielPlan() {
		return spielPlan;
	}

	public List<List<TeamPaarung>> getSpielSchuflePlan() {
		Collections.shuffle(spielPlan);
		return spielPlan;
	}

}
