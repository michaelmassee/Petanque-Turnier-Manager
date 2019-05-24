/**
 * Erstellung 24.05.2019 / Michael Massee
 */
package de.petanqueturniermanager.model;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.MoreObjects;

/**
 * @author Michael Massee
 *
 */
public class TeamPaarung {
	private final Team a;
	private final Team b;

	public TeamPaarung(Team a, Team b) {
		checkNotNull(a);
		checkNotNull(b);
		checkArgument(!a.equals(b));
		this.a = a;
		this.b = b;
	}

	public Team getA() {
		return a;
	}

	public Team getB() {
		return b;
	}

	@Override
	public int hashCode() {
		return a.hashCode() + b.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof TeamPaarung)) {
			return false;
		}
		TeamPaarung teamPaarung = (TeamPaarung) obj;
		return getA().equals(teamPaarung.getA()) && getB().equals(teamPaarung.getB());
	}

	@Override
	public String toString() {

		String teamsStr = "[";
		teamsStr += a.toString();
		teamsStr += ",";
		teamsStr += b.toString();
		teamsStr += "]";

		// @formatter:off
		return MoreObjects.toStringHelper(this)
				.add("Teams", teamsStr)
				.toString();
		// @formatter:on
	}

}
