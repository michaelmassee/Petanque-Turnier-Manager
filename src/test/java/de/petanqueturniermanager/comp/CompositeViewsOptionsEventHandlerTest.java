package de.petanqueturniermanager.comp;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import de.petanqueturniermanager.comp.GlobalProperties.CompositeViewEintragRoh;
import de.petanqueturniermanager.comp.GlobalProperties.PanelEintragRoh;
import de.petanqueturniermanager.comp.GlobalProperties.RegieZielRoh;
import de.petanqueturniermanager.webserver.PanelTyp;
import de.petanqueturniermanager.webserver.RandKonfiguration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testet {@link CompositeViewsOptionsEventHandler#migriereRegieZieleBeiPortAenderungen}, die reine
 * Datenoperation hinter der Webserver-Regie-Migration bei Composite-View-Portaenderungen. Die
 * Methode ist package-private und UNO-frei, daher ohne XComponentContext/XControlContainer direkt
 * aufrufbar - der restliche Handler (VCL-Callback-Verdrahtung) bleibt bewusst ungetestet, da dafuer
 * kein UNO-Test-Double existiert (Muster wie bei den anderen *OptionsEventHandler-Klassen).
 */
class CompositeViewsOptionsEventHandlerTest {

    private static String originalUserHome;

    @TempDir
    Path tempDir;

    @BeforeAll
    static void saveOriginalHome() {
        originalUserHome = System.getProperty("user.home");
    }

    @BeforeEach
    void setup() {
        GlobalProperties.resetForTest();
        System.setProperty("user.home", tempDir.toFile().getAbsolutePath());
    }

    @AfterEach
    void cleanup() {
        System.setProperty("user.home", originalUserHome);
    }

    private static CompositeViewEintragRoh eintrag(int port) {
        var panel = new PanelEintragRoh(
                PanelTyp.BLATT, "RANGLISTE", GlobalProperties.DEFAULT_ZOOM, "kein", "kein", false, "");
        return new CompositeViewEintragRoh(
                port, "Anzeige " + port, true, GlobalProperties.DEFAULT_ZOOM, true, "", List.of(panel),
                RandKonfiguration.KEINER);
    }

    @Test
    void testPortAenderungMigriertPassendesRegieZielInEinemSchritt() {
        var gp = GlobalProperties.get();
        gp.speichernWebserverRegie(true, GlobalProperties.WEBSERVER_REGIE_DEFAULT_PORT, List.of(
                new RegieZielRoh(null, "Ziel A", "", true, "composite:5001")));

        var alt = List.of(eintrag(5001));
        var neu = List.of(eintrag(5002));

        CompositeViewsOptionsEventHandler.migriereRegieZieleBeiPortAenderungen(alt, neu);

        assertEquals(List.of("composite:5002"),
                gp.getWebserverRegieZiele().stream().map(RegieZielRoh::viewId).toList());
    }

    @Test
    void testPortAenderungLaesstFremdesRegieZielAufSelbemZwischenPortUnberuehrt() {
        // Regressionstest fuer den in Commit 0dbd4784 gefundenen und in 9ea35ca9 behobenen Bug:
        // Eine verkettete Migration ueber einen Zwischen-Port (5001->5002->5003) haette hier
        // faelschlich auch das unabhaengige Ziel B (das legitim auf Port 5002 zeigt) mitmigriert.
        var gp = GlobalProperties.get();
        gp.speichernWebserverRegie(true, GlobalProperties.WEBSERVER_REGIE_DEFAULT_PORT, List.of(
                new RegieZielRoh(null, "Ziel A", "", true, "composite:5001"),
                new RegieZielRoh(null, "Ziel B", "", true, "composite:5002")));

        // Direkter Sprung 5001 -> 5003 (kein Zwischenschritt ueber 5002), wie es
        // persistiereUndBenachrichtige() liefert: alteEintraege ist immer der zuletzt
        // tatsaechlich persistierte Zustand, nie ein optimistischer Zwischenstand.
        var alt = List.of(eintrag(5001), eintrag(5002));
        var neu = List.of(eintrag(5003), eintrag(5002));

        CompositeViewsOptionsEventHandler.migriereRegieZieleBeiPortAenderungen(alt, neu);

        var viewIds = gp.getWebserverRegieZiele().stream().map(RegieZielRoh::viewId).toList();
        assertTrue(viewIds.contains("composite:5003"), "Ziel A muss migriert werden");
        assertTrue(viewIds.contains("composite:5002"), "Ziel B darf nicht angefasst werden");
    }

    @Test
    void testUnveraenderterPortMigriertNichts() {
        var gp = GlobalProperties.get();
        gp.speichernWebserverRegie(true, GlobalProperties.WEBSERVER_REGIE_DEFAULT_PORT, List.of(
                new RegieZielRoh(null, "Ziel A", "", true, "composite:5001")));

        var alt = List.of(eintrag(5001));
        var neu = List.of(eintrag(5001));

        CompositeViewsOptionsEventHandler.migriereRegieZieleBeiPortAenderungen(alt, neu);

        assertEquals(List.of("composite:5001"),
                gp.getWebserverRegieZiele().stream().map(RegieZielRoh::viewId).toList());
    }

    @Test
    void testUnterschiedlicheListengroesseMigriertNichts() {
        var gp = GlobalProperties.get();
        gp.speichernWebserverRegie(true, GlobalProperties.WEBSERVER_REGIE_DEFAULT_PORT, List.of(
                new RegieZielRoh(null, "Ziel A", "", true, "composite:5001")));

        var alt = List.of(eintrag(5001));
        var neu = List.of(eintrag(5002), eintrag(5003));

        CompositeViewsOptionsEventHandler.migriereRegieZieleBeiPortAenderungen(alt, neu);

        assertEquals(List.of("composite:5001"),
                gp.getWebserverRegieZiele().stream().map(RegieZielRoh::viewId).toList(),
                "bei Groessenunterschied darf keine (potenziell falsch zugeordnete) Migration erfolgen");
    }
}
