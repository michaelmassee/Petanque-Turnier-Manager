package de.petanqueturniermanager.toolbar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sun.star.frame.XDispatchProviderInterception;
import com.sun.star.frame.XDispatchProviderInterceptor;
import com.sun.star.frame.XFrame;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.XEventListener;

import de.petanqueturniermanager.helper.Lo;

/**
 * Regression: Der Turniermodus kann in zwei gleichzeitig offenen Dokumenten aktiv sein. Früher
 * hielt der Singleton nur einen Interceptor – das Aktivieren in Dokument B entfernte den Schutz
 * von Dokument A.
 */
class KioskFormatierungsSchutzVerwaltungTest {

    private final KioskFormatierungsSchutzVerwaltung verwaltung = new KioskFormatierungsSchutzVerwaltung();

    @Test
    void aktivierenInZweitemDokumentLaesstErstesDokumentGeschuetzt() {
        XFrame frameA = frame();
        XFrame frameB = frame();

        verwaltung.aktivieren(frameA);
        verwaltung.aktivieren(frameB);

        assertThat(verwaltung.istAktiv(frameA)).isTrue();
        assertThat(verwaltung.istAktiv(frameB)).isTrue();
        verify(interception(frameA), never()).releaseDispatchProviderInterceptor(any());
    }

    @Test
    void deaktivierenEntferntNurDenSchutzDesEigenenDokuments() {
        XFrame frameA = frame();
        XFrame frameB = frame();
        verwaltung.aktivieren(frameA);
        verwaltung.aktivieren(frameB);

        verwaltung.deaktivieren(frameB);

        assertThat(verwaltung.istAktiv(frameA)).isTrue();
        assertThat(verwaltung.istAktiv(frameB)).isFalse();
        verify(interception(frameA), never()).releaseDispatchProviderInterceptor(any());
        verify(interception(frameB)).releaseDispatchProviderInterceptor(any());
    }

    @Test
    void doppeltesAktivierenRegistriertNurEinmal() {
        XFrame frame = frame();

        verwaltung.aktivieren(frame);
        verwaltung.aktivieren(frame);

        verify(interception(frame), times(1)).registerDispatchProviderInterceptor(any());
    }

    @Test
    void geschlossenerFrameWirdVergessen() {
        XFrame frame = frame();
        verwaltung.aktivieren(frame);
        var listener = ArgumentCaptor.forClass(XEventListener.class);
        verify(frame).addEventListener(listener.capture());

        listener.getValue().disposing(new EventObject(frame));

        assertThat(verwaltung.istAktiv(frame)).isFalse();
    }

    @Test
    void registriertDenSchutzAlsInterceptor() {
        XFrame frame = frame();

        verwaltung.aktivieren(frame);

        verify(interception(frame)).registerDispatchProviderInterceptor(any(XDispatchProviderInterceptor.class));
    }

    private static XFrame frame() {
        return mock(XFrame.class, withSettings().extraInterfaces(XDispatchProviderInterception.class));
    }

    private static XDispatchProviderInterception interception(XFrame frame) {
        return Lo.qi(XDispatchProviderInterception.class, frame);
    }
}
