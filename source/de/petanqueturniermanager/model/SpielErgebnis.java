/**
 * Erstellung 08.12.2019 / Michael Massee
 */
package de.petanqueturniermanager.model;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.MoreObjects;

/**
 * @author Michael Massee
 *
 */
public class SpielErgebnis {
	private final int spielPunkteA;
	private final int spielPunkteB;

	public SpielErgebnis(int teamA, int teamB) {
		checkArgument(teamA > -1);
		checkArgument(teamB > -1);
		spielPunkteA = teamA;
		spielPunkteB = teamB;
	}

	public final boolean siegA() {
		return spielPunkteA > spielPunkteB;
	}

	public final boolean siegB() {
		return spielPunkteB > spielPunkteA;
	}

	/**
	 * @return the spielPunkteA
	 */
	public final int getSpielPunkteA() {
		return spielPunkteA;
	}

	/**
	 * @return the spielPunkteB
	 */
	public final int getSpielPunkteB() {
		return spielPunkteB;
	}

	@Override
	public String toString() {
		// @formatter:off
		return MoreObjects.toStringHelper(this)
				.add("A", spielPunkteA)
				.add("B", spielPunkteB)
				.toString();
		// @formatter:on
	}

}
