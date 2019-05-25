/**
 * Erstellung 24.05.2019 / Michael Massee
 */
package de.petanqueturniermanager.model;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Optional;

import com.google.common.base.MoreObjects;

/**
 * @author Michael Massee
 *
 */
public class TeamPaarung {

	private Team a;
	private Optional<Team> b;

	/**
	 * wenn b = null dann freilos
	 *
	 * @param a
	 * @param b
	 */
	public TeamPaarung(Team a, Team b) {
		this(a, Optional.of(b));
	}

	public TeamPaarung(Team a, Optional<Team> b) {
		checkNotNull(a);
		checkNotNull(b);
		if (b.isPresent()) {
			checkArgument(!a.equals(b.get()), "Team A == Team B");
		}
		this.a = a;
		this.b = b;
	}

	public Team getA() {
		return a;
	}

	public Team getB() {
		return getOptionalB().orElse(null);
	}

	public Optional<Team> getOptionalB() {
		return b;
	}

	/**
	 * @param b
	 */
	public void setB(Team b) {
		setB(Optional.of(b));
	}

	public void setB(Optional<Team> b) {
		checkNotNull(b);
		this.b = b;
	}

	@Override
	public int hashCode() {
		return a.hashCode() + b.get().hashCode();
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
		return getA().equals(teamPaarung.getA()) && b.get().equals(teamPaarung.getB());
	}

	@Override
	public String toString() {

		String teamsStr = "[";
		teamsStr += a.toString();
		teamsStr += ",";
		teamsStr += b.get().toString();
		teamsStr += "]";

		// @formatter:off
		return MoreObjects.toStringHelper(this)
				.add("Teams", teamsStr)
				.toString();
		// @formatter:on
	}

}
