package de.petanqueturniermanager.model;

import static com.google.common.base.Preconditions.*;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Objects;

import com.google.common.base.MoreObjects;

import de.petanqueturniermanager.algorithmen.AlgorithmenException;

/*
* Spieler.java
* Erstellung     : 31.08.2017 / massee
*
*/

public class Spieler implements Comparable<Spieler> {
	private int setzPos = 0; // spieler mit der gleiche setztposition dürfen nicht im gleichen Team
	private int nr;
	private final HashMap<Integer, Spieler> warImTeamMit = new HashMap<>();
	private WeakReference<Team> wkRefteam;
	private boolean istInTeam = false;

	public Spieler(int nr) {
		checkArgument(nr > 0, "spieler nr <1, %d", nr);
		this.setNr(nr);
	}

	public int anzahlMitSpieler() {
		return getWarImTeamMit().size();
	}

	public static Spieler from(int nr) {
		return new Spieler(nr);
	}

	public static Spieler from(Spieler spieler) {
		checkNotNull(spieler, "spieler == null");
		return new Spieler(spieler.getNr()).copyAttr(spieler);
	}

	private Spieler copyAttr(Spieler spieler) {
		checkNotNull(spieler, "spieler == null");
		this.setzPos = spieler.getSetzPos();
		this.warImTeamMit.putAll(spieler.getWarImTeamMit());
		return this;
	}

	/**
	 * Spieler mit der gleiche Setzpostion duerfen nicht zusammenspielen
	 *
	 * @param spieler
	 * @return true when getSetzPos > 0 and this.getSetzPos = spieler.getSetzPos
	 */
	public boolean gleicheSetzPos(Spieler spieler) {
		return this.getSetzPos() > 0 && spieler.getSetzPos() > 0 && this.getSetzPos() == spieler.getSetzPos();
	}

	public boolean warImTeamMit(Spieler spieler) {
		checkNotNull(spieler, "spieler == null");
		return gleicheSetzPos(spieler) || getWarImTeamMit().containsKey(spieler.getNr());
	}

	public Spieler deleteTeam() throws AlgorithmenException {
		this.wkRefteam = null;
		this.setIstInTeam(false);
		return this;
	}

	public Spieler setTeam(Team team) throws AlgorithmenException {
		checkNotNull(team, "team == null");
		this.wkRefteam = new WeakReference<Team>(team);
		this.setIstInTeam(true);
		return this;
	}

	public Team getTeam() throws AlgorithmenException {
		validatewkRefteamStatus();
		if (this.wkRefteam != null) {
			return this.wkRefteam.get();
		}
		return null;
	}

	/**
	 * Spieler gegenseitig in der Liste war im team hinzufügen
	 *
	 * @param spieler
	 */

	public Spieler addWarImTeamMitWennNichtVorhanden(Spieler spieler) {
		checkNotNull(spieler, "spieler == null");
		if (!spieler.equals(this) && !getWarImTeamMit().containsKey(spieler.getNr())) {
			getWarImTeamMit().put(spieler.getNr(), spieler);
			spieler.addWarImTeamMitWennNichtVorhanden(this);
		}
		return this;
	}

	public Spieler deleteWarImTeam(Spieler spieler) {
		checkNotNull(spieler, "spieler == null");
		getWarImTeamMit().remove(spieler);
		return this;
	}

	public int getNr() {
		return this.nr;
	}

	@Override
	public int compareTo(Spieler spieler) {
		if (spieler == null) {
			return 1;
		}

		if (spieler.getNr() < getNr()) {
			return 1;
		}
		if (spieler.getNr() > getNr()) {
			return -1;
		}
		return 0;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}

		if (obj == this) {
			return true;
		}
		if (!(obj instanceof Spieler)) {
			return false;
		}
		return getNr() == ((Spieler) obj).getNr();
	}

	@Override
	public int hashCode() {
		return Objects.hash(getNr());
	}

	@Override
	public String toString() {

		String warImTeamInfo = "";
		for (Spieler warImTeamSpieler : getWarImTeamMit().values()) {
			if (warImTeamInfo.length() > 0) {
				warImTeamInfo += ",";
			}
			warImTeamInfo += warImTeamSpieler.getNr();
		}

		// @formatter:off
		return MoreObjects.toStringHelper(this)
				.add("nr", getNr())
				.add("\nwarImTeamMit", warImTeamInfo)
				.toString();
		// @formatter:on

	}

	public String mitSpielerStr() {
		String mitspielerStr = "(";
		for (Spieler spielerWarImTeam : getWarImTeamMit().values()) {

			if (!mitspielerStr.endsWith("(")) {
				mitspielerStr += ",";
			}

			mitspielerStr += spielerWarImTeam.getNr();
		}
		return mitspielerStr;
	}

	public int getSetzPos() {
		return this.setzPos;
	}

	public Spieler setSetzPos(int setzPos) {
		this.setzPos = setzPos;
		return this;
	}

	public HashMap<Integer, Spieler> getWarImTeamMit() {
		return this.warImTeamMit;
	}

	public Spieler setNr(int nr) {
		checkArgument(nr > 0, "spieler nr <1, %s", nr);
		this.nr = nr;
		return this;
	}

	public boolean isIstInTeam() throws AlgorithmenException {
		validatewkRefteamStatus();
		return this.istInTeam;
	}

	private void setIstInTeam(boolean istInTeam) throws AlgorithmenException {
		this.istInTeam = istInTeam;
		validatewkRefteamStatus();
	}

	private void validatewkRefteamStatus() throws AlgorithmenException {
		if (this.istInTeam) {
			if (this.wkRefteam == null || this.wkRefteam.get() == null) {
				throw new AlgorithmenException(
						"Ungültige Status in Spieler, istIntTeam = " + this.istInTeam + " wkRefteam.get()==null");
			}
		} else {
			if (this.wkRefteam != null && this.wkRefteam.get() != null) {
				throw new AlgorithmenException(
						"Ungültige Status in Spieler, istIntTeam = " + this.istInTeam + " wkRefteam.get()!=null");
			}
		}
	}

}
