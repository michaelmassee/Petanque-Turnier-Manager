/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.live;

import de.petanqueturniermanager.exception.GenerateException;

/** Liest den Live-Stand eines Turniersystems aus seinen Spielrunden- und Ranglisten-Blättern. */
@FunctionalInterface
public interface LiveStandQuelle {

    LiveTurnierStand lese() throws GenerateException;
}
