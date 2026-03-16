package de.petanqueturniermanager.algorithmen;

import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Erstellung 23.06.2022 / Michael Massee
 * <p>
 * Berechnet den Direktvergleich zweier Teams als Tiebreaker bei Punktgleichstand.<br>
 * Verglichen werden alle Spiele, in denen teamA und teamB direkt gegeneinander angetreten sind.<br>
 * Kriterien in absteigender Prioritaet:
 * <ol>
 *   <li>Summe der Siege (mehr Siege gewinnt)</li>
 *   <li>Summe der Spielpunkte (mehr Spielpunkte gewinnt)</li>
 *   <li>Unentschieden, falls beides gleich</li>
 * </ol>
 * Gibt {@link DirektvergleichResult#KEINERGEBNIS} zurueck, wenn kein Spiel zwischen den Teams gefunden wurde,
 * und {@link DirektvergleichResult#FEHLER} bei ungueltigen Eingaben.
 */
public class Direktvergleich {
	static final Logger logger = LogManager.getLogger(Direktvergleich.class);

	private final int teamA;
	private final int teamB;
	private final int[][] paarungen;
	private final int[][] siege;
	private final int[][] spielpunkte;

	public Direktvergleich(int teamA, int teamB, int[][] paarungen, int[][] siege, int[][] spielpunkte) {
		this.teamA = teamA;
		this.teamB = teamB;
		this.paarungen = Objects.requireNonNull(paarungen);
		this.siege = Objects.requireNonNull(siege);
		this.spielpunkte = Objects.requireNonNull(spielpunkte);
	}

	public final DirektvergleichResult calc() {
		if (!validate()) {
			return DirektvergleichResult.FEHLER;
		}

		int summSpPnktA = 0, summSpPnktB = 0, summSiegeA = 0, summSiegeB = 0;

		for (int idx = 0; idx < paarungen.length; idx++) {
			if (paarungen[idx][0] == teamA && paarungen[idx][1] == teamB) {
				summSiegeA += siege[idx][0];
				summSiegeB += siege[idx][1];
				summSpPnktA += spielpunkte[idx][0];
				summSpPnktB += spielpunkte[idx][1];
			} else if (paarungen[idx][1] == teamA && paarungen[idx][0] == teamB) {
				summSiegeA += siege[idx][1];
				summSiegeB += siege[idx][0];
				summSpPnktA += spielpunkte[idx][1];
				summSpPnktB += spielpunkte[idx][0];
			}
		}

		if (summSiegeA == 0 && summSiegeB == 0 && summSpPnktA == 0 && summSpPnktB == 0) {
			return DirektvergleichResult.KEINERGEBNIS;
		}

		if (summSiegeA != summSiegeB) {
			return summSiegeA > summSiegeB ? DirektvergleichResult.GEWONNEN : DirektvergleichResult.VERLOREN;
		}

		if (summSpPnktA != summSpPnktB) {
			return summSpPnktA > summSpPnktB ? DirektvergleichResult.GEWONNEN : DirektvergleichResult.VERLOREN;
		}

		return DirektvergleichResult.GLEICH;
	}

	private boolean validate() {
		if (teamA < 1) {
			logger.error("teamA nicht gueltig: {}", teamA);
			return false;
		}
		if (teamB < 1) {
			logger.error("teamB nicht gueltig: {}", teamB);
			return false;
		}
		if (paarungen.length == 0) {
			logger.error("paarungen ist leer");
			return false;
		}
		if (siege.length != paarungen.length) {
			logger.error("siege.length ({}) != paarungen.length ({})", siege.length, paarungen.length);
			return false;
		}
		if (spielpunkte.length != paarungen.length) {
			logger.error("spielpunkte.length ({}) != paarungen.length ({})", spielpunkte.length, paarungen.length);
			return false;
		}
		return true;
	}
}
