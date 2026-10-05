package de.petanqueturniermanager.toolbar;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.sun.star.awt.XTopWindow;
import com.sun.star.frame.XDispatchProvider;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XModel;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.frame.XStatusListener;
import com.sun.star.util.URL;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.comp.OfficeDocumentHelper;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.Lo;

class TurnierModusKioskFormatierungsSchutzUITest extends BaseCalcUITest {

    private XSpreadsheetDocument zweitesDokument;

    @AfterEach
    void schliesseZweitesDokument() {
        if (zweitesDokument != null) {
            OfficeDocumentHelper.closeDoc(zweitesDokument);
            zweitesDokument = null;
        }
    }

    @Test
    void formatierungenLoeschenIstImKioskModusDeaktiviert() throws Exception {
        var turnierModus = TurnierModus.get();
        turnierModus.aktivieren(wkingSpreadsheet);
        try {
            assertThat(turnierModus.istAktiv()).isTrue();
            assertThat(istBefehlAktiv(doc, ".uno:ResetAttributes"))
                    .as("Formatierungen löschen ist im Kioskmodus deaktiviert").isFalse();
        } finally {
            turnierModus.wiederherstellenAlleElemente(wkingSpreadsheet);
        }
    }

    /**
     * Regression: Der Turniermodus-Singleton hielt nur einen Interceptor. Wurde der Kiosk-Modus in
     * einem zweiten Dokument aktiviert bzw. dort beendet, verlor das erste Dokument seinen
     * Formatierungsschutz, obwohl es weiter im Kiosk-Modus war.
     */
    @Test
    void zweitesDokumentLaesstSchutzDesErstenDokumentsUnberuehrt() throws Exception {
        var turnierModus = TurnierModus.get();
        zweitesDokument = OfficeDocumentHelper.from(loader).createSichtbaresCalc();
        var zweitesWs = new WorkingSpreadsheet(starter.getxComponentContext(), zweitesDokument);

        turnierModus.aktivieren(wkingSpreadsheet);
        try {
            turnierModus.aktivieren(zweitesWs);
            fokussiere(zweitesDokument);
            assertThat(turnierModus.istKioskFormatierungsSchutzAktiv(wkingSpreadsheet))
                    .as("Aktivieren in Dokument B darf den Schutz von A nicht entfernen").isTrue();
            assertThat(istBefehlAktiv(doc, ".uno:ResetAttributes")).isFalse();

            turnierModus.wiederherstellenAlleElemente(zweitesWs);
            assertThat(turnierModus.istKioskFormatierungsSchutzAktiv(zweitesWs)).isFalse();
            assertThat(turnierModus.istKioskFormatierungsSchutzAktiv(wkingSpreadsheet))
                    .as("Beenden in Dokument B darf den Schutz von A nicht entfernen").isTrue();
            assertThat(istBefehlAktiv(doc, ".uno:ResetAttributes")).isFalse();
        } finally {
            turnierModus.wiederherstellenAlleElemente(wkingSpreadsheet);
        }
        assertThat(turnierModus.istKioskFormatierungsSchutzAktiv(wkingSpreadsheet)).isFalse();
    }

    @Test
    void einfuegenBleibtImKioskModusVerfuegbar() throws Exception {
        var turnierModus = TurnierModus.get();
        turnierModus.aktivieren(wkingSpreadsheet);
        try {
            var frame = Lo.qi(XModel.class, doc).getCurrentController().getFrame();
            var dispatch = Lo.qi(XDispatchProvider.class, frame).queryDispatch(url(".uno:Paste"), "_self", 0);
            assertThat(dispatch).as("Einfügen wird umgeleitet statt gesperrt").isNotNull();
        } finally {
            turnierModus.wiederherstellenAlleElemente(wkingSpreadsheet);
        }
    }

    private boolean istBefehlAktiv(XSpreadsheetDocument dokument, String befehl) {
        var frame = Lo.qi(XModel.class, dokument).getCurrentController().getFrame();
        var befehlsUrl = url(befehl);
        var dispatch = Lo.qi(XDispatchProvider.class, frame).queryDispatch(befehlsUrl, "_self", 0);
        assertThat(dispatch).isNotNull();
        final boolean[] enabled = {true};
        dispatch.addStatusListener(new XStatusListener() {
            @Override
            public void statusChanged(com.sun.star.frame.FeatureStateEvent event) {
                enabled[0] = event.IsEnabled;
            }

            @Override
            public void disposing(com.sun.star.lang.EventObject event) {
                // nichts zu tun
            }
        }, befehlsUrl);
        return enabled[0];
    }

    private static void fokussiere(XSpreadsheetDocument dokument) {
        XFrame frame = Lo.qi(XModel.class, dokument).getCurrentController().getFrame();
        frame.activate();
        XTopWindow topWindow = Lo.qi(XTopWindow.class, frame.getContainerWindow());
        if (topWindow != null) {
            topWindow.toFront();
        }
    }

    private URL url(String complete) {
        var url = new URL();
        url.Complete = complete;
        return url;
    }
}
