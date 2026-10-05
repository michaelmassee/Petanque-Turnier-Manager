package de.petanqueturniermanager.toolbar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.FeatureStateEvent;
import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XDispatchProvider;
import com.sun.star.frame.XStatusListener;
import com.sun.star.lang.EventObject;
import com.sun.star.util.URL;

class KioskFormatierungsSchutzTest {

    private static final String PASTE_UNFORMATTED = ".uno:PasteUnformatted";

    @Test
    void blockiertFormatierendeBefehle() {
        assertThat(KioskFormatierungsSchutz.istBlockierterBefehl(".uno:ResetAttributes")).isTrue();
        assertThat(KioskFormatierungsSchutz.istBlockierterBefehl(".uno:Delete")).isTrue();
        assertThat(KioskFormatierungsSchutz.istBlockierterBefehl(".uno:Cut")).isTrue();
        assertThat(KioskFormatierungsSchutz.istBlockierterBefehl(".uno:PasteSpecial")).isTrue();
    }

    @Test
    void einfuegenBleibtMoeglichWirdAberUmgeleitet() {
        assertThat(KioskFormatierungsSchutz.istBlockierterBefehl(".uno:Paste")).isFalse();
        assertThat(KioskFormatierungsSchutz.istUmgeleiteterBefehl(".uno:Paste")).isTrue();
    }

    @Test
    void leitetNichtFormatierendeEingabebefehleAnLibreOfficeWeiter() {
        var slave = mock(XDispatchProvider.class);
        var erwarteterDispatch = mock(XDispatch.class);
        var url = url(".uno:EnterString");
        when(slave.queryDispatch(any(), any(), anyInt())).thenReturn(erwarteterDispatch);

        var schutz = new KioskFormatierungsSchutz();
        schutz.setSlaveDispatchProvider(slave);

        assertThat(schutz.queryDispatch(url, "_self", 0)).isSameAs(erwarteterDispatch);
        verify(slave).queryDispatch(url, "_self", 0);
    }

    @Test
    void gibtFuerBlockierteBefehleKeinenLibreOfficeDispatchWeiter() {
        var slave = mock(XDispatchProvider.class);
        var schutz = new KioskFormatierungsSchutz();
        schutz.setSlaveDispatchProvider(slave);

        assertThat(schutz.queryDispatch(url(".uno:ResetAttributes"), "_self", 0)).isNotNull();
        verifyNoInteractions(slave);
    }

    @Test
    void einfuegenFuehrtUnformatiertesEinfuegenAus() {
        var slave = mock(XDispatchProvider.class);
        var unformatiert = mock(XDispatch.class);
        when(slave.queryDispatch(argThat(u -> u != null && PASTE_UNFORMATTED.equals(u.Complete)), any(), anyInt()))
                .thenReturn(unformatiert);
        var schutz = new KioskFormatierungsSchutz();
        schutz.setSlaveDispatchProvider(slave);

        XDispatch dispatch = schutz.queryDispatch(url(".uno:Paste"), "_self", 0);
        var args = new PropertyValue[0];
        dispatch.dispatch(url(".uno:Paste"), args);

        var ziel = ArgumentCaptor.forClass(URL.class);
        verify(unformatiert).dispatch(ziel.capture(), eq(args));
        assertThat(ziel.getValue().Complete).isEqualTo(PASTE_UNFORMATTED);
        assertThat(ziel.getValue().Protocol).isEqualTo(".uno:");
        assertThat(ziel.getValue().Path).isEqualTo("PasteUnformatted");
    }

    @Test
    void statusDesUmgeleitetenBefehlsWirdUnterDerUrsprungsUrlGemeldet() {
        var slave = mock(XDispatchProvider.class);
        var unformatiert = mock(XDispatch.class);
        when(slave.queryDispatch(any(), any(), anyInt())).thenReturn(unformatiert);
        var schutz = new KioskFormatierungsSchutz();
        schutz.setSlaveDispatchProvider(slave);
        var paste = url(".uno:Paste");
        var empfangen = new AtomicReference<FeatureStateEvent>();

        schutz.queryDispatch(paste, "_self", 0).addStatusListener(new XStatusListener() {
            @Override
            public void statusChanged(FeatureStateEvent event) {
                empfangen.set(event);
            }

            @Override
            public void disposing(EventObject source) {
                // nichts zu tun
            }
        }, paste);

        var weiterleitung = ArgumentCaptor.forClass(XStatusListener.class);
        verify(unformatiert).addStatusListener(weiterleitung.capture(), any());
        weiterleitung.getValue().statusChanged(
                new FeatureStateEvent(unformatiert, url(PASTE_UNFORMATTED), "", true, false, null));

        assertThat(empfangen.get().FeatureURL.Complete).isEqualTo(".uno:Paste");
        assertThat(empfangen.get().IsEnabled).isTrue();
    }

    @Test
    void einfuegenOhneUnformatiertVarianteWirdBlockiert() {
        var slave = mock(XDispatchProvider.class);
        var schutz = new KioskFormatierungsSchutz();
        schutz.setSlaveDispatchProvider(slave);

        assertThat(schutz.queryDispatch(url(".uno:Paste"), "_self", 0)).isNotNull();
    }

    private URL url(String complete) {
        var url = new URL();
        url.Complete = complete;
        return url;
    }
}
