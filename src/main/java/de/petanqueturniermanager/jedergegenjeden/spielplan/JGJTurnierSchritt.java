package de.petanqueturniermanager.jedergegenjeden.spielplan;

/**
 * Ergebnis der JGJ-Status-Auswertung: wie viele Spiele der Hin- und Rückrunde gespielt wurden.
 */
public record JGJTurnierSchritt(boolean spielplanVorhanden,
                                int hrGespielt, int hrGesamt,
                                int rrGespielt, int rrGesamt) {

    public boolean alleGespielt() {
        return hrGesamt > 0 && hrGespielt >= hrGesamt && rrGespielt >= rrGesamt;
    }
}
