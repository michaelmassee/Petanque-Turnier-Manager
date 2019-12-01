package de.petanqueturniermanager.model;

import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.ref.WeakReference;
import java.util.HashSet;

import com.google.common.base.MoreObjects;

import de.petanqueturniermanager.exception.AlgorithmenException;

/*
* Spieler.java
* Erstellung     : 31.08.2017 / massee
*
*/
public class Spieler extends NrComparable implements IMeldung {
	private int setzPos = 0; // spieler mit der gleiche setztposition dürfen nicht im gleichen Team
	private final HashSet<Integer> warImTeamMit = new HashSet<>();
	private final HashSet<Integer> gegner = new HashSet<>();
	private WeakReference<Team> wkRefteam;
	private boolean istInTeam = false;

	private Spieler(int nr) {
		super(nr);
	}

	public int anzahlMitSpieler() {
		return warImTeamMit.size();
	}

	public static Spieler from(int nr) {
		return new Spieler(nr);
	}

	/**
	 * Spieler mit der gleiche Setzpostion duerfen nicht zusammenspielen
	 *
	 * @param spieler
	 * @return true when getSetzPos > 0 and this.getSetzPos = spieler.getSetzPos
	 */
	public boolean gleicheSetzPos(Spieler spieler) {
		return getSetzPos() > 0 && spieler.getSetzPos() > 0 && getSetzPos() == spieler.getSetzPos();
	}

	public boolean warImTeamMit(int warimTeammit) {
		return warImTeamMit(Spieler.from(warimTeammit));
	}

	public boolean warImTeamMit(Spieler spieler) {
		checkNotNull(spieler, "spieler == null");
		return gleicheSetzPos(spieler) || warImTeamMit.contains(spieler.getNr());
	}

	public Spieler deleteTeam() throws AlgorithmenException {
		wkRefteam = null;
		setIstInTeam(false);
		return this;
	}

	public Spieler setTeam(Team team) throws AlgorithmenException {
		checkNotNull(team, "team == null");
		wkRefteam = new WeakReference<>(team);
		setIstInTeam(true);
		return this;
	}

	public Team getTeam() throws AlgorithmenException {
		validatewkRefteamStatus();
		if (wkRefteam != null) {
			return wkRefteam.get();
		}
		return null;
	}

	/**
	 * Spieler gegenseitig als gegner eintragen wenn nicht vorhanden
	 *
	 * @param spieler
	 * @return
	 */
	public Spieler addGegner(Spieler spieler) {
		checkNotNull(spieler, "spieler == null");
		if (!spieler.equals(this) && !gegner.contains(spieler.getNr())) {
			gegner.add(spieler.getNr());
			spieler.addGegner(this);
		}
		return this;
	}

	/**
	 * Spieler gegenseitig in der Liste war im team hinzufügen
	 *
	 * @param spieler
	 */

	public Spieler addWarImTeamMitWennNichtVorhanden(Spieler spieler) {
		checkNotNull(spieler, "spieler == null");
		if (!spieler.equals(this) && !warImTeamMit.contains(spieler.getNr())) {
			warImTeamMit.add(spieler.getNr());
			spieler.addWarImTeamMitWennNichtVorhanden(this);
		}
		return this;
	}

	public Spieler deleteWarImTeam(Spieler spieler) {
		checkNotNull(spieler, "spieler == null");
		warImTeamMit.remove(spieler.getNr());
		return this;
	}

	@Override
	public String toString() {

		String warImTeamInfo = "";
		for (Integer warImTeamSpielerNr : warImTeamMit) {
			if (warImTeamInfo.length() > 0) {
				warImTeamInfo += ",";
			}
			warImTeamInfo += warImTeamSpielerNr;
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
		for (Integer warImTeamSpielerNr : warImTeamMit) {

			if (!mitspielerStr.endsWith("(")) {
				mitspielerStr += ",";
			}

			mitspielerStr += warImTeamSpielerNr;
		}
		return mitspielerStr;
	}

	public int getSetzPos() {
		return setzPos;
	}

	public Spieler setSetzPos(int setzPos) {
		this.setzPos = setzPos;
		return this;
	}

	public boolean isIstInTeam() throws AlgorithmenException {
		validatewkRefteamStatus();
		return istInTeam;
	}

	private void setIstInTeam(boolean istInTeam) throws AlgorithmenException {
		this.istInTeam = istInTeam;
		validatewkRefteamStatus();
	}

	private void validatewkRefteamStatus() throws AlgorithmenException {
		if (istInTeam) {
			if (wkRefteam == null || wkRefteam.get() == null) {
				throw new AlgorithmenException("Ungültige Status in Spieler, istIntTeam = " + istInTeam + " wkRefteam.get()==null");
			}
		} else {
			if (wkRefteam != null && wkRefteam.get() != null) {
				throw new AlgorithmenException("Ungültige Status in Spieler, istIntTeam = " + istInTeam + " wkRefteam.get()!=null");
			}
		}
	}

}
