/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.onlinesync.speicher;

/**
 * Eine neue Zuordnung lokale Meldung ↔ Online-Anmeldung samt der lesbaren Online-Details – damit
 * {@link PtmOnlineSyncSpeicher#addMappings} alle neuen Zuordnungen in einem Zugriff speichern kann.
 *
 * @param rohStatus unübersetzter Online-Anmeldestatus (Storno-Erkennung)
 * @param besetzung zuletzt abgeglichene Besetzung samt Benutzer-IDs (siehe {@link ZuordnungsZusatz})
 * @param vermerk   sprachneutrale Vermerke der Zuordnung
 * @param teamname  zuletzt abgeglichener Teamname (siehe {@link ZuordnungsZusatz}); leer = unbekannt
 */
public record NeueZuordnung(String lokaleUuid, String onlineId, int executionRevision, String lokaleBezeichnung,
        String onlineStatus, String tarife, String fragen, String rohStatus, String besetzung, String vermerk,
        String teamname) {
}
