package de.petanqueturniermanager.algorithmen;

import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Erstellung 23.06.2022 / Michael Massee
 */

public class Direktvergleich {
	static final Logger logger = LogManager.getLogger(Direktvergleich.class);

	private final int teamA;
	private final int teamB;
	private final int[][] paarungen;
	private final int[][] siege;
	private final int[][] spielpunkte;

	public Direktvergleich(int teamA, int teamB, int[][] paarungen, int[][] spielpunkte, int[][] siege) {
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

		int summSpPnktA = 0;
		int summSpPnktB = 0;
		int summSiegeA = 0;
		int summSiegeB = 0;

		for (int idx = 0; idx < paarungen.length; idx++) {
			if (idx < siege.length && idx < spielpunkte.length) {
				if (paarungen[idx][0] == teamA && paarungen[idx][1] == teamB) {
					summSpPnktA += spielpunkte[idx][0];
					summSpPnktB += spielpunkte[idx][1];
					summSiegeA += siege[idx][0];
					summSiegeB += siege[idx][1];
				} else if (paarungen[idx][1] == teamA && paarungen[idx][0] == teamB) {
					summSpPnktA += spielpunkte[idx][1];
					summSpPnktB += spielpunkte[idx][0];
					summSiegeA += siege[idx][1];
					summSiegeB += siege[idx][0];
				}
			} else {
				break;
			}
		}

		if (summSiegeA == 0 && summSiegeB == 0 && summSpPnktA == 0 && summSpPnktB == 0) {
			return DirektvergleichResult.KEINERGEBNIS;
		}

		if (summSiegeA > summSiegeB) {
			return DirektvergleichResult.GEWONNEN;
		}

		if (summSiegeA < summSiegeB) {
			return DirektvergleichResult.VERLOREN;
		}

		if (summSiegeA == summSiegeB) {
			if (summSpPnktA > summSpPnktB) {
				return DirektvergleichResult.GEWONNEN;
			}

			if (summSpPnktA < summSpPnktB) {
				return DirektvergleichResult.VERLOREN;
			}

			if (summSpPnktA == summSpPnktB) {
				return DirektvergleichResult.GLEICH;
			}
		}
		return DirektvergleichResult.KEINERGEBNIS;

	}

	private boolean validate() {
		if (teamA < 1) {
			logger.error("teamA not valid:" + teamA);
			return false;
		}

		if (teamB < 1) {
			logger.error("teamB not valid:" + teamB);
			return false;
		}

		if (paarungen.length == 0) {
			logger.error("paarungen.length=0");
			return false;
		}

		if (siege.length == 0) {
			logger.error("siege.length=0");
			return false;
		}

		if (spielpunkte.length == 0) {
			logger.error("spielpunkte.length=0");
			return false;
		}

		if (spielpunkte.length != paarungen.length) {
			logger.error("spielpunkte.length != paarungen.length");
			return false;
		}

		if (siege.length != paarungen.length) {
			logger.error("siege.length != paarungen.length");
			return false;
		}

		return true;

	}

}
