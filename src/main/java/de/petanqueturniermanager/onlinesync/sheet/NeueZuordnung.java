/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.onlinesync.sheet;

/**
 * Eine neue Zeile der Zuordnung lokale Meldung ↔ Online-Anmeldung im Blatt „PTMOnline Sync“, samt der lesbaren
 * Online-Details – damit {@link PtmOnlineSyncSheet#addMappings} alle neuen Zeilen in einem Zugriff schreiben kann.
 *
 * @param nummerFormel Formel der angezeigten Team-/Spielernummer (ohne führendes {@code =})
 * @param rohStatus    unübersetzter Online-Anmeldestatus (Storno-Erkennung)
 * @param besetzung    zuletzt abgeglichene Besetzung samt Benutzer-IDs (Zellinhalt, siehe {@link ZuordnungsZusatz})
 * @param vermerk      sprachneutrale Vermerke der Zuordnung
 */
public record NeueZuordnung(String lokaleUuid, String onlineId, String nummerFormel, int executionRevision,
        String lokaleBezeichnung, String onlineStatus, String tarife, String fragen, String rohStatus,
        String besetzung, String vermerk) {
}
