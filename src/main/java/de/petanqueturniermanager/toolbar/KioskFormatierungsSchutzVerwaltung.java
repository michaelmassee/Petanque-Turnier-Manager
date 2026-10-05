package de.petanqueturniermanager.toolbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.frame.XDispatchProviderInterception;
import com.sun.star.frame.XFrame;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.XEventListener;
import com.sun.star.lib.uno.helper.WeakBase;
import com.sun.star.uno.UnoRuntime;

import de.petanqueturniermanager.helper.Lo;

/**
 * Hält je Dokumentrahmen höchstens einen {@link KioskFormatierungsSchutz}.
 * <p>
 * Der Turniermodus kann in mehreren gleichzeitig geöffneten Dokumenten aktiv
 * sein. Jede Registrierung gehört deshalb fest zu ihrem Frame: Aktivieren oder
 * Deaktivieren in einem Dokument lässt den Schutz der anderen Dokumente
 * unberührt. Wird ein Frame geschlossen, verschwindet seine Registrierung
 * automatisch.
 */
final class KioskFormatierungsSchutzVerwaltung {

    private static final Logger logger = LogManager.getLogger(KioskFormatierungsSchutzVerwaltung.class);

    private final List<Registrierung> registrierungen = new ArrayList<>();

    /** Registriert den Schutz am Frame; ist er dort bereits aktiv, passiert nichts. */
    synchronized void aktivieren(XFrame frame) {
        if (frame == null || finde(frame).isPresent()) return;
        var interception = Lo.qi(XDispatchProviderInterception.class, frame);
        if (interception == null) {
            logger.warn("Dispatch-Interception nicht verfügbar: Kiosk-Formatierungsschutz bleibt inaktiv");
            return;
        }
        var schutz = new KioskFormatierungsSchutz();
        interception.registerDispatchProviderInterceptor(schutz);
        var registrierung = new Registrierung(frame, interception, schutz);
        registrierungen.add(registrierung);
        frame.addEventListener(new FrameSchliessenListener(registrierung));
    }

    /** Entfernt den Schutz dieses Frames; andere Frames bleiben geschützt. */
    synchronized void deaktivieren(XFrame frame) {
        if (frame == null) return;
        finde(frame).ifPresent(registrierung -> {
            registrierungen.remove(registrierung);
            try {
                registrierung.interception().releaseDispatchProviderInterceptor(registrierung.schutz());
            } catch (RuntimeException e) {
                logger.warn("Kiosk-Formatierungsschutz konnte nicht entfernt werden: {}", e.getMessage(), e);
            }
        });
    }

    synchronized boolean istAktiv(XFrame frame) {
        return frame != null && finde(frame).isPresent();
    }

    private synchronized void vergessen(Registrierung registrierung) {
        registrierungen.remove(registrierung);
    }

    private Optional<Registrierung> finde(XFrame frame) {
        return registrierungen.stream().filter(r -> UnoRuntime.areSame(r.frame(), frame)).findFirst();
    }

    private record Registrierung(XFrame frame, XDispatchProviderInterception interception,
            KioskFormatierungsSchutz schutz) {
    }

    /** Ein geschlossener Frame gibt seine Interceptoren selbst frei; nur die Registrierung muss weg. */
    private final class FrameSchliessenListener extends WeakBase implements XEventListener {

        private final Registrierung registrierung;

        FrameSchliessenListener(Registrierung registrierung) {
            this.registrierung = registrierung;
        }

        @Override
        public void disposing(EventObject source) {
            vergessen(registrierung);
        }
    }
}
