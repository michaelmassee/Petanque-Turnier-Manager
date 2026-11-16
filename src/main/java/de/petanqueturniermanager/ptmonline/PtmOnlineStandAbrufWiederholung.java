/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.util.OptionalLong;

/**
 * Wiederholung des Online-Zustandsabrufs, solange die online vorhandenen Runden noch nicht abgeglichen sind. Ohne sie
 * blieben überzählige Online-Runden nach einem Netzfehler bestehen, wenn danach keine Dokumentänderung mehr kommt.
 * Der Abstand wächst unabhängig vom Versand-Backoff, den jeder erfolgreiche Versand zurücksetzt.
 * <p>
 * Nicht thread-sicher: nur vom Thread „PTM-Online-Live“ verwendet.
 */
final class PtmOnlineStandAbrufWiederholung {

    private final long startAbstandMs;
    private final long maxAbstandMs;
    private long naechsterAbstandMs;

    PtmOnlineStandAbrufWiederholung(long startAbstandMs, long maxAbstandMs) {
        this.startAbstandMs = startAbstandMs;
        this.maxAbstandMs = maxAbstandMs;
        naechsterAbstandMs = startAbstandMs;
    }

    /**
     * Ergebnis eines Abrufs auswerten.
     *
     * @param rundenOffen      der Abgleich der Online-Runden steht noch aus
     * @param abrufErfolgreich der Online-Zustand wurde abgerufen
     * @return Abstand bis zum nächsten Versuch; leer, wenn keiner nötig ist
     */
    OptionalLong nachAbruf(boolean rundenOffen, boolean abrufErfolgreich) {
        if (abrufErfolgreich) {
            naechsterAbstandMs = startAbstandMs;
            return OptionalLong.empty();
        }
        if (!rundenOffen) {
            return OptionalLong.empty();
        }
        long abstand = naechsterAbstandMs;
        naechsterAbstandMs = Math.min(naechsterAbstandMs * 2, maxAbstandMs);
        return OptionalLong.of(abstand);
    }
}
