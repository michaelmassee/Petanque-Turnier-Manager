/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.auftrag.versand;

/** Warum ein Versandlauf endet. Nur {@link #FERTIG} heißt: alle übergebenen Aufträge sind erledigt. */
public enum VersandStopp {

    /** Alle Aufträge gesendet. */
    FERTIG(false),
    /** PTM-Online nicht erreichbar; später erneut. */
    NETZ(true),
    /** Serverfehler oder Drosselung (5xx, 429); später erneut. */
    SERVERFEHLER(true),
    /** API-Schlüssel abgelehnt (401/403); später erneut, der Nutzer muss den Zugang prüfen. */
    NICHT_BERECHTIGT(true),
    /** Veralteter Schreibzähler: eine Kopie des Dokuments schreibt parallel (E-24). */
    DOKUMENT_GEFORKT(false),
    /** Ein anderes Dokument hat das Online-Turnier übernommen, oder es ist nicht mehr gebunden. */
    BINDUNG_ABGELOEST(false),
    /** Das Online-Turnier wurde gelöscht. */
    TURNIER_GELOESCHT(false);

    private final boolean spaeterErneut;

    VersandStopp(boolean spaeterErneut) {
        this.spaeterErneut = spaeterErneut;
    }

    /** Ob ein späterer Versuch ohne Eingreifen der Turnierleitung Erfolg haben kann. */
    public boolean spaeterErneut() {
        return spaeterErneut;
    }
}
