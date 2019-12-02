/**
* Erstellung : 04.09.2017 / Michael Massee
**/

package de.petanqueturniermanager.model;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.google.common.base.MoreObjects;

import de.petanqueturniermanager.exception.AlgorithmenException;

public class Team extends NrComparable {
	private final ArrayList<Spieler> spielerList;
	private final HashSet<Integer> gegner = new HashSet<>();

	private Team(int nr) {
		super(nr);
		spielerList = new ArrayList<>();
	}

	public static Team from(int nr) {
		return new Team(nr);
	}

	/**
	 * Teams gegenseitig als gegner eintragen wenn nicht vorhanden
	 *
	 * @param team
	 * @return
	 */

	public Team addGegner(Team team) {
		checkNotNull(team, "team == null");
		if (!team.equals(this) && !gegner.contains(team.getNr())) {
			gegner.add(team.getNr());
			team.addGegner(this);
		}
		return this;
	}

	public boolean hatAlsGegner(Team team) {
		checkNotNull(team, "team == null");
		if (team.equals(this)) {
			return false;
		}
		return gegner.contains(team.getNr());
	}

	public Team addSpielerWennNichtVorhanden(List<Spieler> spielerlist) throws AlgorithmenException {
		checkNotNull(spielerlist, "spielerlist == null");
		checkArgument(!spielerlist.isEmpty(), "spielerlist ist leer");

		for (Spieler spielerAusList : spielerlist) {
			addSpielerWennNichtVorhanden(spielerAusList);
		}
		return this;
	}

	public Team addSpielerWennNichtVorhanden(Spieler spieler) throws AlgorithmenException {
		checkNotNull(spieler, "spieler == null");

		if (!spielerList.contains(spieler)) {
			spielerList.add(spieler);
			addMitspieler(spieler);
			spieler.setTeam(this);
		}
		return this;
	}

	private Team addMitspieler(Spieler spieler) {
		checkNotNull(spieler, "spieler == null");

		for (Spieler spielerausList : spielerList) {
			spielerausList.addWarImTeamMitWennNichtVorhanden(spieler);
			spieler.addWarImTeamMitWennNichtVorhanden(spielerausList);
		}
		return this;
	}

	public int size() {
		return spielerList.size();
	}

	public Team removeAlleSpieler() throws AlgorithmenException {
		List<Spieler> spielerListClone = spieler();
		for (Spieler spielerausList : spielerListClone) {
			removeSpieler(spielerausList);
		}
		return this;
	}

	public Team removeSpieler(Spieler spieler) throws AlgorithmenException {
		checkNotNull(spieler, "spieler == null");
		spielerList.remove(spieler);
		spieler.deleteTeam();
		for (Spieler spielerausList : spielerList) {
			spieler.deleteWarImTeam(spielerausList);
			spielerausList.deleteWarImTeam(spieler);
		}
		return this;
	}

	public List<Spieler> spieler() {
		return new ArrayList<>(spielerList);
	}

	public boolean istSpielerImTeam(Spieler spieler) {
		return spielerList.contains(spieler);
	}

	public Spieler findSpielerByNr(int nr) {
		Spieler spieler = null;
		for (Spieler spielerausList : spielerList) {
			if (spielerausList.getNr() == nr) {
				spieler = spielerausList;
				break;
			}
		}
		return spieler;
	}

	public List<Spieler> listeVonSpielerOhneSpieler(Spieler spieler) {
		checkNotNull(spieler, "spieler == null");

		List<Spieler> spielerteamOhneSpieler = new ArrayList<>(spielerList);
		spielerteamOhneSpieler.remove(spieler);
		return spielerteamOhneSpieler;
	}

	public boolean hatZusammenGespieltMit(Spieler spieler) {
		checkNotNull(spieler, "spieler == null");
		boolean hatzusammenGespielt = false;

		for (Spieler spielerAusTeam : spielerList) {
			if (!spielerAusTeam.equals(spieler)) { // nicht sich selbst vergleichen
				if (spielerAusTeam.warImTeamMit(spieler)) {
					hatzusammenGespielt = true;
					break;
				}
			}
		}
		return hatzusammenGespielt;
	}

	@Override
	public String toString() {

		String spielerNr = "[";
		for (Spieler spielerAusTeam : spielerList) {
			if (spielerNr.length() > 1) {
				spielerNr += ",";
			}
			spielerNr += spielerAusTeam.getNr();
		}
		spielerNr += "]";

		// @formatter:off
		return MoreObjects.toStringHelper(this)
				.add("nr", nr)
				.add("Spieler", spielerNr)
				.toString();
		// @formatter:on

	}

}
