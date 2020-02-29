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
 */
public class TeamPaarung implements Cloneable {

	private Team a;
	private Optional<Team> b;

	/**
	 * wenn b = null dann freilos
	 *
	 * @param a
	 * @param b
	 */
	public TeamPaarung(Team a, Team b) {
		this(checkNotNull(a), (b == null) ? Optional.empty() : Optional.of(b));
	}

	public TeamPaarung(Team a) {
		this(checkNotNull(a), Optional.empty());
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

	/**
	 * @param nrA
	 * @param nrB
	 */
	public TeamPaarung(int nrA, int nrB) {
		this(Team.from(nrA), Team.from(nrB));
	}

	/**
	 * @param nrA
	 */
	public TeamPaarung(int nrA) {
		this(Team.from(nrA));
	}

	/**
	 * nur wenn B vorhanden dann A<->B Tauschen
	 */
	public void flipTeams() {
		if (b.isPresent()) {
			Team oldA = a;
			a = b.get();
			setB(oldA);
		}
	}

	/**
	 * teams gegenseitig als gegner eintragen
	 *
	 * @return
	 */
	public TeamPaarung addGegner() {
		if (b.isPresent()) {
			a.addGegner(b.get()); // gegenseitig als gegner eintragen
		}
		return this;
	}

	/**
	 * teams gegenseitig als gegner entfernen
	 *
	 * @return
	 */
	public TeamPaarung removeGegner() {
		if (b.isPresent()) {
			a.removeGegner(b.get()); // gegenseitig als gegner entfernen
		}
		return this;
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

	public TeamPaarung setA(Team aTeam) {
		checkNotNull(aTeam);
		a = aTeam;
		return this;
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
		return a.hashCode() + (b.isPresent() ? b.get().hashCode() : 0);
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
		return getA().equals(teamPaarung.getA()) && bEquals(teamPaarung);
	}

	private boolean bEquals(TeamPaarung teamPaarung) {
		if (b.isPresent() && teamPaarung.getOptionalB().isPresent()) {
			return b.get().equals(teamPaarung.getOptionalB().get());
		}

		if (!b.isPresent() && !teamPaarung.getOptionalB().isPresent()) {
			return true;
		}
		return false;
	}

	@Override
	public String toString() {

		String teamsStr = "[";
		teamsStr += a.toString();
		teamsStr += ",";
		teamsStr += (b.isPresent()) ? b.get().toString() : null;
		teamsStr += "]";

		// @formatter:off
		return MoreObjects.toStringHelper(this)
				.add("Teams", teamsStr)
				.toString();
		// @formatter:on
	}

	@Override
	public Object clone() {
		if (b.isPresent()) {
			return new TeamPaarung(Team.from(a.nr), Team.from(b.get().nr));
		}
		return new TeamPaarung(Team.from(a.nr));
	}

	/**
	 * @param team to find
	 * @return true wenn team = a oder b
	 */
	public boolean isInPaarung(Team team) {
		boolean isA = getA().equals(team);
		boolean isB = b.isPresent() && b.get().equals(team);
		return team != null && isA || isB;
	}

	/**
	 * @param team1 gegner zurück liefern
	 * @return
	 */
	public Team getGegner(Team team1) {
		if (getA().equals(team1)) {
			return getB();
		}
		return getA();
	}

	/**
	 * @return
	 */
	public TeamPaarung setHatGegner() {
		if (b.isPresent()) {
			b.get().setHatGegner(true);
		}
		a.setHatGegner(true);

		return this;

	}

	public boolean hasB() {
		return b.isPresent();
	}

	/**
	 * flag entfernen
	 */
	public TeamPaarung removeHatGegner() {
		if (b.isPresent()) {
			b.get().setHatGegner(false);
		}
		a.setHatGegner(false);
		return this;
	}

}
