package de.petanqueturniermanager.toolbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.sun.star.frame.XFrame;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.XEventListener;
import com.sun.star.lib.uno.helper.WeakBase;
import com.sun.star.uno.UnoRuntime;

/**
 * Ordnet einem Dokumentrahmen genau einen Wert zu.
 * <p>
 * Der Turniermodus ist ein Zustand des einzelnen Dokumentfensters, nicht des
 * LibreOffice-Prozesses. Frames werden per {@link UnoRuntime#areSame} verglichen,
 * da dieselbe UNO-Instanz über verschiedene Java-Proxys erreichbar sein kann.
 * Wird ein Frame geschlossen, verschwindet sein Eintrag automatisch.
 *
 * @param <T> zugeordneter Zustand
 */
final class FrameZuordnung<T> {

    private final List<Eintrag<T>> eintraege = new ArrayList<>();

    /** Ordnet dem Frame den Wert zu, falls er noch keinen hat; liefert {@code true} bei neuer Zuordnung. */
    synchronized boolean zuordnenFallsNeu(XFrame frame, T wert) {
        if (frame == null || finde(frame).isPresent()) return false;
        var eintrag = new Eintrag<>(frame, wert);
        eintraege.add(eintrag);
        frame.addEventListener(new FrameSchliessenListener(eintrag));
        return true;
    }

    synchronized Optional<T> wert(XFrame frame) {
        return frame == null ? Optional.empty() : finde(frame).map(Eintrag::wert);
    }

    synchronized Optional<T> entfernen(XFrame frame) {
        if (frame == null) return Optional.empty();
        Optional<Eintrag<T>> eintrag = finde(frame);
        eintrag.ifPresent(eintraege::remove);
        return eintrag.map(Eintrag::wert);
    }

    synchronized boolean istLeer() {
        return eintraege.isEmpty();
    }

    private synchronized void vergessen(Eintrag<T> eintrag) {
        eintraege.remove(eintrag);
    }

    private Optional<Eintrag<T>> finde(XFrame frame) {
        return eintraege.stream().filter(e -> UnoRuntime.areSame(e.frame(), frame)).findFirst();
    }

    private record Eintrag<T>(XFrame frame, T wert) {
    }

    /** Ein geschlossener Frame gibt seine UI-Elemente selbst frei; nur die Zuordnung muss weg. */
    private final class FrameSchliessenListener extends WeakBase implements XEventListener {

        private final Eintrag<T> eintrag;

        FrameSchliessenListener(Eintrag<T> eintrag) {
            this.eintrag = eintrag;
        }

        @Override
        public void disposing(EventObject source) {
            vergessen(eintrag);
        }
    }
}
