package de.petanqueturniermanager.jedergegenjeden.spielplan;

/**
 * Ergebnis der JGJ-Status-Auswertung: wie viele Spiele der Hin- und Rückrunde gespielt wurden.
 */
public record JGJTurnierSchritt(boolean spielplanVorhanden,
                                int hrGespielt, int hrGesamt,
                                int rrGespielt, int rrGesamt,
                                boolean finalrundeVorhanden,
                                boolean finalrundeBeendet) {

    public JGJTurnierSchritt(boolean spielplanVorhanden,
                             int hrGespielt, int hrGesamt,
                             int rrGespielt, int rrGesamt) {
        this(spielplanVorhanden, hrGespielt, hrGesamt, rrGespielt, rrGesamt, false, false);
    }

    public boolean alleGespielt() {
        boolean spielplanBeendet = hrGesamt > 0 && hrGespielt >= hrGesamt && rrGespielt >= rrGesamt;
        return spielplanBeendet && (!finalrundeVorhanden || finalrundeBeendet);
    }
}
