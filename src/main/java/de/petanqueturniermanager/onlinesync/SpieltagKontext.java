/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.onlinesync;

import org.jspecify.annotations.Nullable;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeKonfigurationSheet;

/**
 * Ermittelt den fuer eine PTM-Online-Verbindung massgeblichen Spieltag: nur bei Supermelee (siehe
 * {@link TurnierSystem#hatMehrereSpielTage()}) wird pro Spieltag eine eigene Verbindung/eigenes
 * Sync-Blatt gefuehrt (siehe {@link de.petanqueturniermanager.onlinesync.sheet.PtmOnlineSyncSheet}),
 * fuer alle anderen Turniersysteme gibt es genau eine Verbindung fuer das ganze Dokument.
 */
public final class SpieltagKontext {

	private SpieltagKontext() {}

	public static @Nullable Integer aktiverSpieltagOderNull(WorkingSpreadsheet ws, TurnierSystem ts) throws GenerateException {
		if (!ts.hatMehrereSpielTage()) {
			return null;
		}
		return new SuperMeleeKonfigurationSheet(ws).getAktiveSpieltag().getNr();
	}
}
