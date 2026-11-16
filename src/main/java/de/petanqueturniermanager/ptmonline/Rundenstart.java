/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

/**
 * Anlass eines Rundenabgleichs mit PTM-Online ({@link PtmOnlineSpielrundeSync}): Nur ein ausdrücklicher Rundenstart
 * startet das Online-Turnier bzw. nimmt ein online zurückgesetztes oder abgeschlossenes Turnier wieder auf.
 */
public enum Rundenstart {

    /** Allererste Runde des Turniers (bei Supermelee: des Spieltags): Vorabcheck und Turnierstart. */
    ERSTE_RUNDE,

    /** Jede weitere neu angelegte Runde: nimmt ein online nicht laufendes Turnier wieder auf. */
    FOLGERUNDE,

    /** Kein Rundenstart, z.&nbsp;B. erneute Auslosung der aktuellen Runde: nur Teilnahme abgleichen. */
    KEINER;

    /** Anlass beim Anlegen einer neuen Runde. */
    public static Rundenstart neueRunde(boolean istErsteRunde) {
        return istErsteRunde ? ERSTE_RUNDE : FOLGERUNDE;
    }
}
