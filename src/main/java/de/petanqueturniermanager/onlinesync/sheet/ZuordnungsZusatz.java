/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.onlinesync.sheet;

/**
 * Technischer Zusatz einer Zuordnung im Blatt „PTMOnline Sync“ (T-15, T-17, T-18), in ausgeblendeten Spalten.
 *
 * @param besetzung zuletzt abgeglichene Besetzung samt Benutzer-IDs als Zellinhalt; {@code null} beim Schreiben = unverändert
 * @param vermerk   sprachneutrale Vermerke (z.&nbsp;B. „lokal entfernt“); {@code null} beim Schreiben = unverändert
 * @param teamname  zuletzt abgeglichener Teamname als Zellinhalt (leer = noch nie abgeglichen); {@code null} beim
 *                  Schreiben = unverändert
 */
public record ZuordnungsZusatz(String besetzung, String vermerk, String teamname) {

    public static ZuordnungsZusatz nurBesetzung(String besetzung) {
        return new ZuordnungsZusatz(besetzung, null, null);
    }

    public static ZuordnungsZusatz nurVermerk(String vermerk) {
        return new ZuordnungsZusatz(null, vermerk, null);
    }

    public static ZuordnungsZusatz besetzungUndTeamname(String besetzung, String teamname) {
        return new ZuordnungsZusatz(besetzung, null, teamname);
    }
}
