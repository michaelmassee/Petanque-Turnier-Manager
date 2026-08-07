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
        return eintrag(port, "Anzeige " + port);
    }

    private static CompositeViewEintragRoh eintrag(int port, String name) {
        var panel = new PanelEintragRoh(
                PanelTyp.BLATT, "RANGLISTE", GlobalProperties.DEFAULT_ZOOM, "kein", "kein", false, "");
        return new CompositeViewEintragRoh(
                port, name, true, GlobalProperties.DEFAULT_ZOOM, true, "", List.of(panel),
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

    // ---- validiereEintraege: Namens-Validierung ----

    @Test
    void testValidiereEintraegeErkenntLeerenNamen() {
        var eintraege = List.of(eintrag(5001, ""));
        String fehler = CompositeViewsOptionsEventHandler.validiereEintraege(eintraege, GlobalProperties.get());
        assertNotNull(fehler);
    }

    @Test
    void testValidiereEintraegeErkenntNurLeerzeichenAlsLeerenNamen() {
        // Wird bereits im CompositeViewEintragRoh-Kompaktkonstruktor auf "" getrimmt.
        var eintraege = List.of(eintrag(5001, "   "));
        String fehler = CompositeViewsOptionsEventHandler.validiereEintraege(eintraege, GlobalProperties.get());
        assertNotNull(fehler);
    }

    @Test
    void testValidiereEintraegeErkenntDoppeltenNamen() {
        var eintraege = List.of(eintrag(5001, "Anzeige"), eintrag(5002, "Anzeige"));
        String fehler = CompositeViewsOptionsEventHandler.validiereEintraege(eintraege, GlobalProperties.get());
        assertNotNull(fehler);
    }

    @Test
    void testValidiereEintraegeIstCaseSensitivBeiNamen() {
        var eintraege = List.of(eintrag(5001, "Abc"), eintrag(5002, "abc"));
        String fehler = CompositeViewsOptionsEventHandler.validiereEintraege(eintraege, GlobalProperties.get());
        assertNull(fehler, "Abc und abc muessen als unterschiedliche Namen gelten (case-sensitiv)");
    }

    @Test
    void testValidiereEintraegeAkzeptiertEindeutigeNamen() {
        var eintraege = List.of(eintrag(5001, "Anzeige A"), eintrag(5002, "Anzeige B"));
        String fehler = CompositeViewsOptionsEventHandler.validiereEintraege(eintraege, GlobalProperties.get());
        assertNull(fehler);
    }

    // ---- CompositeViewEintragRoh: Namens-Trimmen ----

    @Test
    void testRecordTrimmtFuehrendeUndNachfolgendeLeerzeichenImNamen() {
        var e = eintrag(5001, " Foo ");
        assertEquals("Foo", e.name());
    }

    @Test
    void testRecordTrimmenMachtNamenMitUndOhneLeerzeichenZuDuplikat() {
        var eintraege = List.of(eintrag(5001, "Foo"), eintrag(5002, "Foo "));
        String fehler = CompositeViewsOptionsEventHandler.validiereEintraege(eintraege, GlobalProperties.get());
        assertNotNull(fehler, "\"Foo\" und \"Foo \" muessen nach dem Trimmen als Duplikat erkannt werden");
    }

    // ---- loeseImportPortKonflikteAuf ----

    @Test
    void testImportPortOhneKollisionBleibtUnveraendert() {
        var bestehende = List.of(eintrag(5001));
        var importiert = List.of(eintrag(6001));

        var ergebnis = CompositeViewsOptionsEventHandler.loeseImportPortKonflikteAuf(bestehende, importiert);

        assertEquals(1, ergebnis.size());
        assertEquals(6001, ergebnis.get(0).port());
    }

    @Test
    void testImportPortKollisionMitBestehendemEintragWirdAufgeloest() {
        var bestehende = List.of(eintrag(5001));
        var importiert = List.of(eintrag(5001));

        var ergebnis = CompositeViewsOptionsEventHandler.loeseImportPortKonflikteAuf(bestehende, importiert);

        assertEquals(1, ergebnis.size());
        assertNotEquals(5001, ergebnis.get(0).port());
    }

    @Test
    void testImportPortKollisionZwischenZweiImportiertenEintraegenWirdAufgeloest() {
        var bestehende = List.<CompositeViewEintragRoh>of();
        var importiert = List.of(eintrag(5001, "A"), eintrag(5001, "B"));

        var ergebnis = CompositeViewsOptionsEventHandler.loeseImportPortKonflikteAuf(bestehende, importiert);

        assertEquals(2, ergebnis.size());
        assertNotEquals(ergebnis.get(0).port(), ergebnis.get(1).port());
    }

    // ---- loeseImportNamenKonflikteAuf ----

    @Test
    void testImportNamenOhneKollisionBleibenUnveraendert() {
        var bestehende = List.of(eintrag(5001, "Bestehend"));
        var importiert = List.of(eintrag(6001, "Neu"));

        var ergebnis = CompositeViewsOptionsEventHandler.loeseImportNamenKonflikteAuf(bestehende, importiert);

        assertEquals("Neu", ergebnis.get(0).name());
    }

    @Test
    void testImportNamenKollisionMitBestehendemEintragBekommtSuffix() {
        var bestehende = List.of(eintrag(5001, "Anzeige"));
        var importiert = List.of(eintrag(6001, "Anzeige"));

        var ergebnis = CompositeViewsOptionsEventHandler.loeseImportNamenKonflikteAuf(bestehende, importiert);

        assertEquals("Anzeige (2)", ergebnis.get(0).name());
    }

    @Test
    void testImportLeererNameBekommtPortBasiertenPlatzhalter() {
        var bestehende = List.<CompositeViewEintragRoh>of();
        var importiert = List.of(eintrag(6001, ""), eintrag(6002, ""));

        var ergebnis = CompositeViewsOptionsEventHandler.loeseImportNamenKonflikteAuf(bestehende, importiert);

        // Unterschiedliche (bereits port-konfliktbereinigte) Ports ergeben unterschiedliche
        // Platzhalternamen, daher hier keine Namenskollision zwischen den beiden.
        assertEquals("View 6001", ergebnis.get(0).name());
        assertEquals("View 6002", ergebnis.get(1).name());
    }

    @Test
    void testImportLeererNamePlatzhalterKollidiertMitBestehendemNamenBekommtSuffix() {
        var bestehende = List.of(eintrag(6001, "View 6002"));
        var importiert = List.of(eintrag(6002, ""));

        var ergebnis = CompositeViewsOptionsEventHandler.loeseImportNamenKonflikteAuf(bestehende, importiert);

        assertEquals("View 6002 (2)", ergebnis.get(0).name());
    }

    @Test
    void testImportMehrfacheNamenKollisionErzeugtFortlaufendeSuffixe() {
        var bestehende = List.of(eintrag(5001, "Anzeige"));
        var importiert = List.of(eintrag(6001, "Anzeige"), eintrag(6002, "Anzeige"));

        var ergebnis = CompositeViewsOptionsEventHandler.loeseImportNamenKonflikteAuf(bestehende, importiert);

        assertEquals("Anzeige (2)", ergebnis.get(0).name());
        assertEquals("Anzeige (3)", ergebnis.get(1).name());
    }
}
