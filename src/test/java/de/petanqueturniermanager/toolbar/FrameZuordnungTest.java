package de.petanqueturniermanager.toolbar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sun.star.frame.XFrame;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.XEventListener;

class FrameZuordnungTest {

    private final FrameZuordnung<String> zuordnung = new FrameZuordnung<>();

    @Test
    void jederFrameHatEigenenWert() {
        XFrame a = mock(XFrame.class);
        XFrame b = mock(XFrame.class);

        zuordnung.zuordnenFallsNeu(a, "A");
        zuordnung.zuordnenFallsNeu(b, "B");

        assertThat(zuordnung.wert(a)).contains("A");
        assertThat(zuordnung.wert(b)).contains("B");
    }

    @Test
    void bestehendeZuordnungWirdNichtUeberschrieben() {
        XFrame frame = mock(XFrame.class);

        assertThat(zuordnung.zuordnenFallsNeu(frame, "erster")).isTrue();
        assertThat(zuordnung.zuordnenFallsNeu(frame, "zweiter")).isFalse();

        assertThat(zuordnung.wert(frame)).contains("erster");
    }

    @Test
    void entfernenBetrifftNurDenEigenenFrame() {
        XFrame a = mock(XFrame.class);
        XFrame b = mock(XFrame.class);
        zuordnung.zuordnenFallsNeu(a, "A");
        zuordnung.zuordnenFallsNeu(b, "B");

        assertThat(zuordnung.entfernen(b)).contains("B");

        assertThat(zuordnung.wert(a)).contains("A");
        assertThat(zuordnung.wert(b)).isEmpty();
        assertThat(zuordnung.istLeer()).isFalse();
    }

    @Test
    void entfernenDeregistriertDenCloseListener() {
        XFrame frame = mock(XFrame.class);
        zuordnung.zuordnenFallsNeu(frame, "A");
        var listener = ArgumentCaptor.forClass(XEventListener.class);
        verify(frame).addEventListener(listener.capture());

        zuordnung.entfernen(frame);

        verify(frame).removeEventListener(listener.getValue());
    }

    @Test
    void geschlossenerFrameWirdVergessen() {
        XFrame frame = mock(XFrame.class);
        zuordnung.zuordnenFallsNeu(frame, "A");
        var listener = ArgumentCaptor.forClass(XEventListener.class);
        verify(frame).addEventListener(listener.capture());

        listener.getValue().disposing(new EventObject(frame));

        assertThat(zuordnung.wert(frame)).isEmpty();
        assertThat(zuordnung.istLeer()).isTrue();
    }

    @Test
    void ohneFrameGibtEsKeinenZustand() {
        assertThat(zuordnung.zuordnenFallsNeu(null, "A")).isFalse();
        assertThat(zuordnung.wert(null)).isEmpty();
        assertThat(zuordnung.entfernen(null)).isEmpty();
    }
}
