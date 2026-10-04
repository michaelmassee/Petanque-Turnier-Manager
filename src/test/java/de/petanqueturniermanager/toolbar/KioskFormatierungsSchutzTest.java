package de.petanqueturniermanager.toolbar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XDispatchProvider;
import com.sun.star.util.URL;

class KioskFormatierungsSchutzTest {

    @Test
    void blockiertFormatierungenLoeschenUndDenDeleteDialog() {
        assertThat(KioskFormatierungsSchutz.istBlockierterBefehl(".uno:ResetAttributes")).isTrue();
        assertThat(KioskFormatierungsSchutz.istBlockierterBefehl(".uno:Delete")).isTrue();
        assertThat(KioskFormatierungsSchutz.istBlockierterBefehl(".uno:Paste")).isTrue();
    }

    @Test
    void leitetNichtFormatierendeEingabebefehleAnLibreOfficeWeiter() {
        var slave = mock(XDispatchProvider.class);
        var erwarteterDispatch = mock(XDispatch.class);
        var url = url(".uno:EnterString");
        when(slave.queryDispatch(any(), any(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(erwarteterDispatch);

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

    private URL url(String complete) {
        var url = new URL();
        url.Complete = complete;
        return url;
    }
}
