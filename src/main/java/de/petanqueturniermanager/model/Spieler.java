package de.petanqueturniermanager.model;

import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.ref.WeakReference;
import java.util.HashSet;

import com.google.common.base.MoreObjects;

import de.petanqueturniermanager.exception.AlgorithmenException;

/*
 * Spieler.java Erstellung : 31.08.2017 / massee
 *
 */
public class Spieler extends NrComparable implements IMeldung<Spieler> {
	private int setzPos; // spieler mit der gleiche setztposition dürfen nicht im gleichen Team.
	private final HashSet<Integer> warImTeamMit = new HashSet<>();
	private final HashSet<Integer> gegner = new HashSet<>();
	// Union aus Mitspielern und Gegnern: alle Spieler, mit denen man bereits in
	// einer Partie war (egal ob Team oder Gegner). Dient dem Supermêlée-Algorithmus
	// als weicher Constraint, um Crossover-Wiederholungen zu vermeiden.
	private final HashSet<Integer> warImSpielMit = new HashSet<>();
	private WeakReference<Team> wkRefteam;
	private boolean istInTeam;
	private boolean hatteFreilos; // Spieler hatte bereits ein freilos
	// Zähler, wie oft der Spieler in einem Team mit Ausnahmegröße (kleiner als die
	// Default-Modus-Größe) spielte. Wird vom Supermêlée-Paarungsalgorithmus verwendet,
	// um Doublette-Slots fair über die Runden zu verteilen.
	private int anzMalKleinesTeam;

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

	public boolean warGegnerVon(Spieler spieler) {
		checkNotNull(spieler, "spieler == null");
		return gegner.contains(spieler.getNr());
	}

	/**
	 * Wahr, wenn beide Spieler bereits in einer früheren Runde im selben Spiel
	 * waren – als Mitspieler oder als Gegner.
	 */
	public boolean warImSpielMit(Spieler spieler) {
		checkNotNull(spieler, "spieler == null");
		return warImSpielMit.contains(spieler.getNr());
	}

	/**
	 * Spieler gegenseitig als „war im selben Spiel" eintragen, wenn nicht
	 * vorhanden. Wird vom Supermêlée-Algorithmus nach Abschluss einer Runde
	 * für alle Paare im selben Spiel (Team ∪ Gegner) aufgerufen.
	 */
	public Spieler addWarImSpielMit(Spieler spieler) {
		checkNotNull(spieler, "spieler == null");
		if (!spieler.equals(this) && !warImSpielMit.contains(spieler.getNr())) {
			warImSpielMit.add(spieler.getNr());
			spieler.addWarImSpielMit(this);
		}
		return this;
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
	 * Spieler gegenseitig zur warImTeamMit-Liste hinzufügen, wenn nicht vorhanden
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

	public String mitSpielerStr() {
		StringBuilder mitspielerStr = new StringBuilder("(");
		for (Integer warImTeamSpielerNr : warImTeamMit) {
			if (mitspielerStr.length() > 1) {
				mitspielerStr.append(",");
			}
			mitspielerStr.append(warImTeamSpielerNr);
		}
		return mitspielerStr.toString();
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
				throw new AlgorithmenException(
						"Ungültige Status in Spieler, istIntTeam = " + istInTeam + " wkRefteam.get()==null");
			}
		} else {
			if (wkRefteam != null && wkRefteam.get() != null) {
				throw new AlgorithmenException(
						"Ungültige Status in Spieler, istIntTeam = " + istInTeam + " wkRefteam.get()!=null");
			}
		}
	}

	@Override
	public String toString() {

		StringBuilder warImTeamInfo = new StringBuilder();
		for (Integer warImTeamSpielerNr : warImTeamMit) {
			if (warImTeamInfo.length() > 0) {
				warImTeamInfo.append(",");
			}
			warImTeamInfo.append(warImTeamSpielerNr);
		}

		// @formatter:off
		return MoreObjects.toStringHelper(this)
				.add("nr", getNr())
				.add("\nwarImTeamMit", warImTeamInfo.toString())
				.toString();
		// @formatter:on

	}

	@Override
	public int getSetzPos() {
		return setzPos;
	}

	@Override
	public Spieler setSetzPos(int setzPos) {
		this.setzPos = setzPos;
		return this;
	}

	public int getAnzMalKleinesTeam() {
		return anzMalKleinesTeam;
	}

	public Spieler incAnzMalKleinesTeam() {
		anzMalKleinesTeam++;
		return this;
	}

	public Spieler resetAnzMalKleinesTeam() {
		anzMalKleinesTeam = 0;
		return this;
	}

	/**
	 * Setzt die komplette Paarungs-Historie des Spielers zurück: Mitspieler-,
	 * Gegner- und Spiel-Historie sowie den Counter für Ausnahme-Teamgrößen.
	 * Wird vom Lockerungs-Retry in der Supermelee-Paarungslogik genutzt, um die
	 * Historie aus weniger vergangenen Spieltagen neu aufbauen zu können.
	 */
	public Spieler resetHistorie() {
		warImTeamMit.clear();
		gegner.clear();
		warImSpielMit.clear();
		anzMalKleinesTeam = 0;
		return this;
	}

	@Override
	public boolean isHatteFreilos() {
		return hatteFreilos;
	}

	@Override
	public Spieler setHatteFreilos(boolean hatteFreilos) {
		this.hatteFreilos = hatteFreilos;
		return this;
	}

}
