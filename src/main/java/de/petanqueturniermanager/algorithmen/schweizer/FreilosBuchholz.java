/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen.schweizer;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import de.petanqueturniermanager.schweizer.konfiguration.SchweizerFreilosBuchholzModus;

/**
 * Buchholz-Beitrag eines Freiloses im Schweizer System.
 * <p>
 * Bei {@link SchweizerFreilosBuchholzModus#NEUTRAL} zählt das Freilos als fiktiver Durchschnittsgegner: Er verliert
 * in der Freilos-Runde gegen das eigene Team und gewinnt in den übrigen {@code r − 1} Runden rechnerisch die Hälfte.
 * Pro Freilos ergibt das
 * <ul>
 *   <li>BHZ-Beitrag {@code (r − 1) / 2} (Siege des fiktiven Gegners)</li>
 *   <li>FBHZ-Beitrag {@code eigeneSiege + (r − 1) · r / 2} (BHZ des fiktiven Gegners: das eigene Team plus
 *       {@code r − 1} Durchschnittsgegner mit je {@code r / 2} Siegen)</li>
 * </ul>
 * Nach Runde 1 hat ein Freilos-Team damit BHZ 0 und FBHZ 1 – genau wie jeder andere Sieger.
 * {@link SchweizerFreilosBuchholzModus#NULL} liefert immer 0 (BPV-Regel).
 *
 * @param modus     Freilos-Buchholz-Modus
 * @param anzRunden Anzahl gewerteter Runden mit eingetragenen Ergebnissen ({@code r})
 */
public record FreilosBuchholz(SchweizerFreilosBuchholzModus modus, int anzRunden) {

	/** Freilos ohne Buchholz-Beitrag (BPV-Regel). */
	public static final FreilosBuchholz OHNE_BEITRAG = new FreilosBuchholz(SchweizerFreilosBuchholzModus.NULL, 0);

	public FreilosBuchholz {
		checkNotNull(modus);
		checkArgument(anzRunden >= 0, "anzRunden darf nicht negativ sein");
	}

	/**
	 * @return BHZ-Beitrag eines einzelnen Freiloses
	 */
	public double bhzBeitrag() {
		if (!liefertBeitrag()) {
			return 0;
		}
		return (anzRunden - 1) / 2.0;
	}

	/**
	 * @param eigeneSiege Siege des Freilos-Teams
	 * @return FBHZ-Beitrag eines einzelnen Freiloses
	 */
	public double fbhzBeitrag(int eigeneSiege) {
		if (!liefertBeitrag()) {
			return 0;
		}
		return eigeneSiege + (anzRunden - 1) * anzRunden / 2.0;
	}

	private boolean liefertBeitrag() {
		return modus == SchweizerFreilosBuchholzModus.NEUTRAL && anzRunden > 0;
	}
}
