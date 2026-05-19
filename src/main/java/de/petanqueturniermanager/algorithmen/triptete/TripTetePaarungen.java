/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen.triptete;
import de.petanqueturniermanager.algorithmen.liga.JederGegenJeden;
import de.petanqueturniermanager.algorithmen.common.KoRundeTeamPaarungen;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import de.petanqueturniermanager.model.FormeSpielrunde;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.model.TeamPaarung;
import de.petanqueturniermanager.model.TeamRangliste;

/**
 * Erzeugt Begegnungs-Paarungen für Trip-Tête-Turniere.
 * <p>
 * Trip-Tête hat zwei sinnvolle Spielmodi:
 * <ul>
 *   <li><b>Liga / Gruppenphase:</b> Jeder gegen Jeden ({@link #jederGegenJeden(TeamMeldungen)}).</li>
 *   <li><b>KO-Runde:</b> Erster gegen Letzten anhand einer Rangliste ({@link #koRunde(TeamRangliste)}).</li>
 * </ul>
 * Beide Modi delegieren auf vorhandene Algo-Bausteine, kapseln aber die Trip-Tête-spezifische Sicht
 * (eine Paarung = eine Begegnung über drei Partien).
 *
 * @author Michael Massee
 */
public final class TripTetePaarungen {

	/** Pro Trip-Tête-Begegnung werden zwei Bahnen benötigt (Triplette, danach Doublette + Tête parallel). */
	public static final int BAHNEN_PRO_BEGEGNUNG = 2;

	private TripTetePaarungen() {
	}

	public static List<List<TeamPaarung>> jederGegenJeden(TeamMeldungen meldungen) {
		checkNotNull(meldungen, "meldungen == null");
		return new JederGegenJeden(meldungen).generate();
	}

	public static FormeSpielrunde koRunde(TeamRangliste rangliste) {
		checkNotNull(rangliste, "rangliste == null");
		return new KoRundeTeamPaarungen(rangliste).generiereSpielrunde();
	}
}
