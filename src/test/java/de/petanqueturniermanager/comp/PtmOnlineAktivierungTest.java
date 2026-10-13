package de.petanqueturniermanager.comp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.uno.XComponentContext;
import com.sun.star.beans.XPropertySet;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.util.XChangesBatch;
import de.petanqueturniermanager.ptmonline.PtmOnlineLiveBeobachter;

class PtmOnlineAktivierungTest {
    @AfterEach
    void reset() {
        PtmOnlineLiveBeobachter.zuruecksetzenFuerTest();
        LibreOfficePtmOnlineSpeicher.setZugangsdatenForTest(null);
    }

    @Test
    void optionIstStandardmaessigAktivAberOhneKeyKeinOnline() {
        var ohneKey = new LibreOfficePtmOnlineSpeicher.Zugangsdaten("", "https://ptmonline.org");
        assertThat(ohneKey.verbindungAktiv()).isTrue();
        LibreOfficePtmOnlineSpeicher.setZugangsdatenForTest(ohneKey);
        assertThat(PtmOnlineAktivierung.istAktiv()).isFalse();
    }

    @Test
    void keyUndOptionSchaltenFreigabeInBeideRichtungen() {
        LibreOfficePtmOnlineSpeicher.setZugangsdatenForTest(
                new LibreOfficePtmOnlineSpeicher.Zugangsdaten("test", "https://ptmonline.org"));
        assertThat(PtmOnlineAktivierung.istAktiv()).isTrue();
        LibreOfficePtmOnlineSpeicher.setZugangsdatenForTest(
                new LibreOfficePtmOnlineSpeicher.Zugangsdaten("test", "https://ptmonline.org", false));
        assertThat(PtmOnlineAktivierung.istAktiv()).isFalse();
        LibreOfficePtmOnlineSpeicher.setZugangsdatenForTest(
                new LibreOfficePtmOnlineSpeicher.Zugangsdaten("test", "https://ptmonline.org", true));
        assertThat(PtmOnlineAktivierung.istAktiv()).isTrue();
        LibreOfficePtmOnlineSpeicher.setZugangsdatenForTest(
                new LibreOfficePtmOnlineSpeicher.Zugangsdaten("  ", "https://ptmonline.org", true));
        assertThat(PtmOnlineAktivierung.istAktiv()).isFalse();
    }

    @Test
    void deaktivierterBeobachterBeruehrtKeinDokumentOderUnoKontext() {
        LibreOfficePtmOnlineSpeicher.setZugangsdatenForTest(
                new LibreOfficePtmOnlineSpeicher.Zugangsdaten("", "https://ptmonline.org"));
        XComponentContext context = mock(XComponentContext.class);
        XSpreadsheetDocument doc = mock(XSpreadsheetDocument.class);
        var beobachter = PtmOnlineLiveBeobachter.init(context);
        beobachter.onLoad(doc);
        PtmOnlineLiveBeobachter.anstossen(doc);
        verifyNoInteractions(context, doc);
    }

    @Test
    void aktivOptionWirdGeladenUndGespeichert() throws Exception {
        XComponentContext context = mock(XComponentContext.class);
        XMultiComponentFactory factory = mock(XMultiComponentFactory.class);
        XMultiServiceFactory provider = mock(XMultiServiceFactory.class);
        XPropertySet props = mock(XPropertySet.class, withSettings().extraInterfaces(XChangesBatch.class));
        when(context.getServiceManager()).thenReturn(factory);
        when(factory.createInstanceWithContext("com.sun.star.configuration.ConfigurationProvider", context))
                .thenReturn(provider);
        when(provider.createInstanceWithArguments(anyString(), any())).thenReturn(props);
        when(props.getPropertyValue("ApiKey")).thenReturn("test");
        when(props.getPropertyValue("BaseUrl")).thenReturn("https://ptmonline.org");
        // Ein älteres Profil ohne die Eigenschaft erhält den Default true.
        assertThat(new LibreOfficePtmOnlineSpeicher(context).laden().verbindungAktiv()).isTrue();
        when(props.getPropertyValue("VerbindungAktiv")).thenReturn(false);
        assertThat(new LibreOfficePtmOnlineSpeicher(context).laden().isConfigured()).isFalse();

        new LibreOfficePtmOnlineSpeicher(context).speichern("test", "https://ptmonline.org", false);

        verify(props).setPropertyValue("VerbindungAktiv", false);
        verify((XChangesBatch) props).commitChanges();
        assertThat(PtmOnlineAktivierung.istAktiv()).isFalse();
    }

    @Test
    void menueUndToolbarWerdenAusgeblendetUndWiederSichtbar() throws Exception {
        var methode = ProtocolHandler.class.getDeclaredMethod("setzeCommandState",
                com.sun.star.frame.FeatureStateEvent.class, String.class, XSpreadsheetDocument.class);
        methode.setAccessible(true);
        for (boolean aktiv : new boolean[] {false, true, false, true}) {
            LibreOfficePtmOnlineSpeicher.setZugangsdatenForTest(
                    new LibreOfficePtmOnlineSpeicher.Zugangsdaten("test", "https://ptmonline.org", aktiv));
            for (String command : new String[] {ProtocolHandler.CMD_PTMONLINE_MENU,
                    ProtocolHandler.CMD_PTMONLINE_SYNC_PAUSIEREN, ProtocolHandler.CMD_PTMONLINE_SYNC_FORTSETZEN}) {
                var event = new com.sun.star.frame.FeatureStateEvent();
                methode.invoke(null, event, command, null);
                assertThat(event.State).isInstanceOf(com.sun.star.frame.status.Visibility.class);
                assertThat(((com.sun.star.frame.status.Visibility) event.State).bVisible).isEqualTo(aktiv);
            }
        }
    }
}
