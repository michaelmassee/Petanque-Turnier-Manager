package de.petanqueturniermanager.toolbar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.frame.XDispatchProviderInterception;
import com.sun.star.frame.XFrame;

import de.petanqueturniermanager.helper.Lo;

/**
 * Hält je Dokumentrahmen höchstens einen {@link KioskFormatierungsSchutz}.
 * <p>
 * Der Turniermodus kann in mehreren gleichzeitig geöffneten Dokumenten aktiv
 * sein. Jede Registrierung gehört deshalb fest zu ihrem Frame: Aktivieren oder
 * Deaktivieren in einem Dokument lässt den Schutz der anderen Dokumente
 * unberührt.
 */
final class KioskFormatierungsSchutzVerwaltung {

    private static final Logger logger = LogManager.getLogger(KioskFormatierungsSchutzVerwaltung.class);

    private final FrameZuordnung<Registrierung> registrierungen = new FrameZuordnung<>();

    /** Registriert den Schutz am Frame; ist er dort bereits aktiv, passiert nichts. */
    synchronized void aktivieren(XFrame frame) {
        if (frame == null || istAktiv(frame)) return;
        var interception = Lo.qi(XDispatchProviderInterception.class, frame);
        if (interception == null) {
            logger.warn("Dispatch-Interception nicht verfügbar: Kiosk-Formatierungsschutz bleibt inaktiv");
            return;
        }
        var schutz = new KioskFormatierungsSchutz();
        interception.registerDispatchProviderInterceptor(schutz);
        registrierungen.zuordnenFallsNeu(frame, new Registrierung(interception, schutz));
    }

    /** Entfernt den Schutz dieses Frames; andere Frames bleiben geschützt. */
    synchronized void deaktivieren(XFrame frame) {
        registrierungen.entfernen(frame).ifPresent(registrierung -> {
            try {
                registrierung.interception().releaseDispatchProviderInterceptor(registrierung.schutz());
            } catch (RuntimeException e) {
                logger.warn("Kiosk-Formatierungsschutz konnte nicht entfernt werden: {}", e.getMessage(), e);
            }
        });
    }

    synchronized boolean istAktiv(XFrame frame) {
        return registrierungen.wert(frame).isPresent();
    }

    private record Registrierung(XDispatchProviderInterception interception, KioskFormatierungsSchutz schutz) {
    }
}
