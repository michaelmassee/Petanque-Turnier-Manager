/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import de.petanqueturniermanager.exception.GenerateException;

/**
 * Vorgemerkter Online-Turnierstart der ersten Spielrunde (KP-05) oder Wiederaufnahme eines online zurückgesetzten
 * bzw. abgeschlossenen Turniers durch eine Folgerunde. Der Übergang zu {@code running} und der Teilnahme-Stand dieser
 * Runde entstehen erst mit {@link #bestaetigen()}, also nachdem die Runde tatsächlich erstellt ist. Scheitert die
 * Erstellung an einer späteren Vorprüfung (zu wenige Meldungen, Abbruch), bleibt das Online-Turnier, wie es ist – kein
 * Startauftrag, kein ausstehender Start.
 * <p>
 * Ohne Verbindung und ohne nötigen Start ist der Abgleich bereits vollständig erfasst; {@link #bestaetigen()} tut
 * dann nichts.
 */
public final class PtmOnlineTurnierstart {

    /** Erfasst die zurückgehaltenen Schreibaufträge. */
    @FunctionalInterface
    interface Erfassung {
        void erfasse() throws GenerateException;
    }

    private static final Erfassung NICHTS = () -> {
        // bereits vollständig erfasst
    };
    private static final PtmOnlineTurnierstart KEINER = new PtmOnlineTurnierstart(NICHTS);

    private Erfassung offen;

    private PtmOnlineTurnierstart(Erfassung offen) {
        this.offen = offen;
    }

    /** Kein Start vorgemerkt: alles ist bereits erfasst. */
    static PtmOnlineTurnierstart keiner() {
        return KEINER;
    }

    /** Start vorgemerkt: die Aufträge entstehen erst mit {@link #bestaetigen()}. */
    static PtmOnlineTurnierstart vorgemerkt(Erfassung erfassung) {
        return new PtmOnlineTurnierstart(erfassung);
    }

    /**
     * Die Runde ist erstellt: Startauftrag und Teilnahme-Stand erfassen. Höchstens einmal wirksam.
     */
    public synchronized void bestaetigen() throws GenerateException {
        Erfassung erfassung = offen;
        offen = NICHTS;
        erfassung.erfasse();
    }
}
