/*
 * Erstellung 24.05.2019 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen.common;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;

import de.petanqueturniermanager.model.FormeSpielrunde;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamPaarung;
import de.petanqueturniermanager.model.TeamRangliste;

/**
 * Erzeugt Paarungen für eine K.O.-Runde nach dem „Erster gegen Letzten"-Prinzip.<br>
 * <br>
 * Rang 1 spielt gegen Rang n, Rang 2 gegen Rang n-1, usw. Hat ein Team gegen seinen Gegner schon gespielt, nimmt
 * es den nächstschlechteren noch freien – aber nur, wenn danach für die ganze Runde die kleinstmögliche Zahl an
 * Wiederholungen erreichbar bleibt ({@link WiederholungsarmePaarung}). Ist eine Wiederholung unvermeidbar, wird sie
 * gemeldet ({@link #isDoppelteGespieltePaarungenVorhanden()}).
 *
 * @author Michael Massee
 */
public class KoRundeTeamPaarungen {

	private final TeamRangliste teamRangListe;
	private boolean doppelteGespieltePaarungenVorhanden = false;
	private final List<String> doppelteGespieltePaarungen = new ArrayList<>();

	public KoRundeTeamPaarungen(TeamRangliste teamRangListe) {
		checkNotNull(teamRangListe, "TeamRangliste == null");
		checkArgument(!teamRangListe.isEmpty(), "TeamRangliste.isEmpty");
		checkArgument(IsEvenOrOdd.isEven(teamRangListe.size()), "Keine gerade anzahl Teams in der Liste");
		this.teamRangListe = teamRangListe;
	}

	public FormeSpielrunde generiereSpielrunde() {
		FormeSpielrunde formeSpielrunde = new FormeSpielrunde(1);
		doppelteGespieltePaarungenVorhanden = false;
		doppelteGespieltePaarungen.clear();

		List<Team> rangliste = teamRangListe.getCloneTeamListe();
		for (WiederholungsarmePaarung.Paar paar : WiederholungsarmePaarung.paare(rangliste.size(),
				(a, b) -> rangliste.get(a).hatAlsGegner(rangliste.get(b)),
				WiederholungsarmePaarung.GegnerWahl.LETZTER_IN_RANGLISTE)) {
			Team teamA = rangliste.get(paar.a());
			Team teamB = rangliste.get(paar.b());
			if (paar.wiederholung()) {
				doppelteGespieltePaarungenVorhanden = true;
				doppelteGespieltePaarungen.add(teamA.getNr() + ":" + teamB.getNr());
			}
			formeSpielrunde.addPaarungWennNichtVorhanden(new TeamPaarung(teamA, teamB));
		}
		return formeSpielrunde;
	}

	public TeamRangliste getTeamRangListe() {
		return teamRangListe;
	}

	/** @return {@code true} wenn mindestens eine Doppel-Paarung (bereits gespielt) entstanden ist */
	public boolean isDoppelteGespieltePaarungenVorhanden() {
		return doppelteGespieltePaarungenVorhanden;
	}

	/** @return leerzeichengetrennte Liste der Doppel-Paarungen, z.B. {@code "1:4 2:5"} */
	public String getDoppelteGespieltePaarungen() {
		return String.join(" ", doppelteGespieltePaarungen);
	}
}
