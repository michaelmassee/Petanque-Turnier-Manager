/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.onlinesync.sheet;

import java.time.Instant;
import java.util.Optional;

/**
 * Zustand einer PTM-Online-Verbindung laut Blatt „PTMOnline Sync“ – für Anzeigen wie die Sidebar.
 *
 * @param verbunden  das Dokument (bzw. der Spieltag) ist mit einem Online-Turnier verbunden
 * @param pausiert   der Sync ist pausiert: kein Abgleich, kein Rundenstart-Sync
 * @param letzterSync Zeitpunkt des letzten vollständigen Meldungsabgleichs
 */
public record PtmOnlineSyncStatus(boolean verbunden, boolean pausiert, Optional<Instant> letzterSync) {

    public static final PtmOnlineSyncStatus NICHT_VERBUNDEN = new PtmOnlineSyncStatus(false, false, Optional.empty());
}
