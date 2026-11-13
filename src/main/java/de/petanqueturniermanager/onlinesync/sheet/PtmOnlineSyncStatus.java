/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.onlinesync.sheet;

import java.time.Instant;
import java.util.Optional;

import de.petanqueturniermanager.helper.i18n.I18n;

/**
 * Zustand einer PTM-Online-Verbindung laut Blatt „PTMOnline Sync“ – für Anzeigen wie die Sidebar.
 *
 * @param verbunden    das Dokument (bzw. der Spieltag) ist mit einem Online-Turnier verbunden
 * @param pausiert     der Sync ist pausiert: kein Abgleich, kein Rundenstart-Sync
 * @param letzterSync  Zeitpunkt des letzten vollständigen Meldungsabgleichs
 * @param onlineStatus zuletzt bekannter Status des Online-Turniers, sprachneutral wie PTM-Online ihn meldet
 *                     ({@code draft}, {@code registration}, {@code running}, {@code finished}); leer, solange
 *                     unbekannt
 */
public record PtmOnlineSyncStatus(boolean verbunden, boolean pausiert, Optional<Instant> letzterSync,
        Optional<String> onlineStatus) {

    public static final PtmOnlineSyncStatus NICHT_VERBUNDEN = new PtmOnlineSyncStatus(false, false, Optional.empty(),
            Optional.empty());

    /** Übersetzter Online-Status; leer, solange er unbekannt ist. */
    public Optional<String> onlineStatusAnzeige() {
        return onlineStatus.map(PtmOnlineSyncStatus::statusAnzeige);
    }

    /** Übersetzt einen Status von PTM-Online; ein unbekannter Status erscheint unverändert. */
    public static String statusAnzeige(String onlineStatus) {
        return switch (onlineStatus) {
            case "draft", "registration", "running", "finished" -> I18n.get("ptmonline.onlinestatus." + onlineStatus);
            default -> onlineStatus;
        };
    }
}
