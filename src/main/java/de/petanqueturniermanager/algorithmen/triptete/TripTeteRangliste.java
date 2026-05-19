/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen.triptete;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.petanqueturniermanager.model.Team;

/**
 * Berechnet die Trip-Tête-Rangliste aus verbuchten Begegnungs-Ergebnissen.
 * <p>
 * Sortier-Reihenfolge wird durch {@link TripTeteTeamErgebnis#compareTo(TripTeteTeamErgebnis)} bestimmt:
 * Begegnungssiege ↓ → Partiensiege ↓ → Kugel-Δ ↓ → Σ Kugeln+ ↓.
 *
 * @author Michael Massee
 */
public class TripTeteRangliste {

	private final Map<Integer, TripTeteTeamErgebnis> ergebnisseProTeam = new HashMap<>();

	public TripTeteRangliste addTeam(Team team) {
		checkNotNull(team, "team == null");
		ergebnisseProTeam.computeIfAbsent(team.getNr(), nr -> new TripTeteTeamErgebnis(team));
		return this;
	}

	/**
	 * Verbucht eine Begegnung beidseitig. Beide Teams müssen vorher per {@link #addTeam(Team)}
	 * registriert sein (wird sonst automatisch nachgeholt).
	 */
	public TripTeteRangliste addBegegnung(TripTeteBegegnungErgebnis ergebnis) {
		checkNotNull(ergebnis, "ergebnis == null");
		addTeam(ergebnis.getTeamA());
		addTeam(ergebnis.getTeamB());
		ergebnisseProTeam.get(ergebnis.getTeamA().getNr()).verbucheBegegnung(true, ergebnis);
		ergebnisseProTeam.get(ergebnis.getTeamB().getNr()).verbucheBegegnung(false, ergebnis);
		return this;
	}

	/** @return sortierte Rangliste (Platz 1 zuerst). */
	public List<TripTeteTeamErgebnis> getRangliste() {
		List<TripTeteTeamErgebnis> liste = new ArrayList<>(ergebnisseProTeam.values());
		Collections.sort(liste);
		return liste;
	}
}
