package de.petanqueturniermanager.comp;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import de.petanqueturniermanager.comp.GlobalProperties.CompositeViewEintragRoh;
import de.petanqueturniermanager.comp.GlobalProperties.PanelEintragRoh;
import de.petanqueturniermanager.webserver.PanelTyp;
import de.petanqueturniermanager.webserver.RandKonfiguration;

import static org.junit.jupiter.api.Assertions.*;

class GlobalPropertiesTest {

    private static String originalUserHome;

    @TempDir
    Path tempDir;

    @BeforeAll
    static void saveOriginalHome() {
        originalUserHome = System.getProperty("user.home");
    }

    @BeforeEach
    void setup() {
        GlobalProperties.resetForTest(); // wichtig!
        System.setProperty("user.home", tempDir.toFile().getAbsolutePath());
    }

    @AfterEach
    void cleanup() {
        System.setProperty("user.home", originalUserHome);
    }

    @Test
    void testDefaults() {
        var gp = GlobalProperties.get();

        assertFalse(gp.isAutoSave());
        assertFalse(gp.isCreateBackup());
        assertFalse(gp.isWebserverAktiv());
        assertTrue(gp.isWebserverRegieAktiv());
        assertEquals(GlobalProperties.WEBSERVER_REGIE_DEFAULT_PORT, gp.getWebserverRegiePort());
        assertEquals("", gp.getLogLevel());
    }

    @Test
    void testWebserverRegieFallbackRoundtripOhneLibreOfficeKontext() throws Exception {
        var gp = GlobalProperties.get();

        gp.speichernWebserverRegie(false, 9191, List.of());

        GlobalProperties.resetForTest();
        var gp2 = GlobalProperties.get();

        assertFalse(gp2.isWebserverRegieAktiv());
        assertEquals(9191, gp2.getWebserverRegiePort());

        var file = tempDir.resolve("PetanqueTurnierManager.properties");
        var inhalt = java.nio.file.Files.readString(file);
        assertTrue(inhalt.contains("webserver_regie_aktiv=false"));
        assertTrue(inhalt.contains("webserver_regie_port=9191"));
    }

    @Test
    void testWebserverRegieUngueltigerLegacyPortVerwendetDefault() throws Exception {
        var file = tempDir.resolve("PetanqueTurnierManager.properties");
        java.nio.file.Files.writeString(file, "webserver_regie_port=99999\n");

        var gp = GlobalProperties.get();

        assertEquals(GlobalProperties.WEBSERVER_REGIE_DEFAULT_PORT, gp.getWebserverRegiePort());
    }

    @Test
    void testSpeichernUndLesen() {
        var gp = GlobalProperties.get();

        gp.speichern(true, true, false, true, true, true, "debug", true);

        GlobalProperties.resetForTest();
        var gp2 = GlobalProperties.get();

        assertTrue(gp2.isAutoSave());
        assertTrue(gp2.isCreateBackup());
        assertFalse(gp2.isNewVersionCheckImmerTrue());
        assertTrue(gp2.isPerformanceLogging());
        assertEquals("debug", gp2.getLogLevel());
    }

    @Test
    void testPerformanceLoggingDefaultUndRoundtrip() {
        var gp = GlobalProperties.get();
        assertFalse(gp.isPerformanceLogging(), "Default muss false sein");

        gp.speichern(false, false, false, true, true, true, "", true);
        GlobalProperties.resetForTest();
        assertTrue(GlobalProperties.get().isPerformanceLogging());

        GlobalProperties.get().speichern(false, false, false, true, true, false, "", true);
        GlobalProperties.resetForTest();
        assertFalse(GlobalProperties.get().isPerformanceLogging());
    }

    @Test
    void testCompositeEintraegeLeer() {
        var gp = GlobalProperties.get();

        var eintraege = gp.getCompositeViewEintraege();

        assertNotNull(eintraege);
        assertTrue(eintraege.isEmpty());
    }

    @Test
    void testLegacyEinzelPortPropertiesWerdenBeimStartEntfernt() throws Exception {
        var file = tempDir.resolve("PetanqueTurnierManager.properties");
        java.nio.file.Files.writeString(file,
                "webserver_ports=9001\n"
                        + "webserver_port_9001_sheet=rangliste\n"
                        + "webserver_port_9001_aktiv=true\n"
                        + "webserver_sheetnamen_anzeigen=true\n"
                        + "loglevel=info\n");

        var gp = GlobalProperties.get();
        assertNotNull(gp);
        assertEquals("info", gp.getLogLevel());

        // Datei darf die Legacy-Keys nicht mehr enthalten.
        var inhalt = java.nio.file.Files.readString(file);
        assertFalse(inhalt.contains("webserver_ports"));
        assertFalse(inhalt.contains("webserver_port_9001"));
        assertFalse(inhalt.contains("webserver_sheetnamen_anzeigen"));
    }

    @Test
    void testTabFarbeDefault() {
        var gp = GlobalProperties.get();

        int color = gp.getTabFarbe("Tab-Farbe Meldeliste", 0xFFFFFF);

        assertEquals(0xFFFFFF, color);
    }

    @Test
    void testDefekteDatei() throws Exception {
        File file = tempDir.resolve("PetanqueTurnierManager.properties").toFile();

        java.nio.file.Files.writeString(file.toPath(), "### kaputt ### \n = = =");

        var gp = GlobalProperties.get();

        assertNotNull(gp);
        assertFalse(gp.isAutoSave());
    }

    @Test
    void testSpeichernCompositeViewsMitFlags() {
        var gp = GlobalProperties.get();

        gp.speichernCompositeViews(true, List.of());

        GlobalProperties.resetForTest();
        var gp2 = GlobalProperties.get();

        assertTrue(gp2.isWebserverAktiv());
        assertTrue(gp2.getCompositeViewEintraege().isEmpty());
    }

    @Test
    void testCompositeRoundtripOhneLayout() throws Exception {
        var gp = GlobalProperties.get();
        var panel = new PanelEintragRoh(
                PanelTyp.BLATT, "RANGLISTE", GlobalProperties.DEFAULT_ZOOM, "kein", "kein", false, "");
        var eintrag = new CompositeViewEintragRoh(
                5001, "Anzeige", true, GlobalProperties.DEFAULT_ZOOM, true, "", List.of(panel), RandKonfiguration.KEINER);

        gp.speichernCompositeViews(true, List.of(eintrag));

        // Es darf keine Split-Layout-Property mehr geschrieben werden.
        var file = tempDir.resolve("PetanqueTurnierManager.properties");
        assertFalse(java.nio.file.Files.readString(file).contains("_layout"),
                "Composite-Speicherung darf keine Layout-Property mehr schreiben");

        // Roundtrip: Eintrag inkl. Panel bleibt ohne Layout erhalten.
        GlobalProperties.resetForTest();
        var gp2 = GlobalProperties.get();
        var gelesen = gp2.getCompositeViewEintraege();
        assertEquals(1, gelesen.size());
        assertEquals(1, gelesen.get(0).panels().size());
        assertEquals("RANGLISTE", gelesen.get(0).panels().get(0).sheetConfig());
        assertFalse(gp2.getCompositeViewKonfigurationen().isEmpty());
    }

    @Test
    void testCompositeRoundtripOhneRandLiefertKeinerAlsDefault() {
        var gp = GlobalProperties.get();
        var panel = new PanelEintragRoh(
                PanelTyp.BLATT, "RANGLISTE", GlobalProperties.DEFAULT_ZOOM, "kein", "kein", false, "");
        var eintrag = new CompositeViewEintragRoh(
                5001, "Anzeige", true, GlobalProperties.DEFAULT_ZOOM, true, "", List.of(panel), RandKonfiguration.KEINER);

        gp.speichernCompositeViews(true, List.of(eintrag));

        GlobalProperties.resetForTest();
        var gp2 = GlobalProperties.get();
        assertEquals(RandKonfiguration.KEINER, gp2.getCompositeViewEintraege().get(0).rand());
    }

    @Test
    void testCompositeRoundtripMitRandKonfiguration() {
        var gp = GlobalProperties.get();
        var panel = new PanelEintragRoh(
                PanelTyp.BLATT, "RANGLISTE", GlobalProperties.DEFAULT_ZOOM, "kein", "kein", false, "");
        var rand = new RandKonfiguration(4, RandKonfiguration.ART_DASHED, 0x336699, 25, RandKonfiguration.ANIMATION_PULSIEREN);
        var eintrag = new CompositeViewEintragRoh(
                5001, "Anzeige", true, GlobalProperties.DEFAULT_ZOOM, true, "", List.of(panel), rand);

        gp.speichernCompositeViews(true, List.of(eintrag));

        GlobalProperties.resetForTest();
        var gp2 = GlobalProperties.get();
        assertEquals(rand, gp2.getCompositeViewEintraege().get(0).rand());

        var konfigRand = gp2.getCompositeViewKonfigurationen().get(0).rand();
        assertEquals(rand, konfigRand);
    }

    @Test
    void testCompositeRoundtripMitLokalerStatischerDatei() throws Exception {
        var gp = GlobalProperties.get();
        var panel = new PanelEintragRoh(
                PanelTyp.STATISCHE_DATEI, "", GlobalProperties.DEFAULT_ZOOM, "kein", "kein", false,
                "/tmp/anzeige.html");
        var eintrag = new CompositeViewEintragRoh(
                5001, "Anzeige", true, GlobalProperties.DEFAULT_ZOOM, true, "", List.of(panel), RandKonfiguration.KEINER);

        gp.speichernCompositeViews(true, List.of(eintrag));

        GlobalProperties.resetForTest();
        var gp2 = GlobalProperties.get();
        var gelesenesPanel = gp2.getCompositeViewEintraege().get(0).panels().get(0);
        assertEquals(PanelTyp.STATISCHE_DATEI, gelesenesPanel.typ());
        assertEquals("/tmp/anzeige.html", gelesenesPanel.externeUrl());

        var konfigPanel = gp2.getCompositeViewKonfigurationen().get(0).panels().get(0);
        assertEquals(PanelTyp.STATISCHE_DATEI, konfigPanel.typ());
        assertEquals("/tmp/anzeige.html", konfigPanel.externeUrl());
    }

    @Test
    void testCompositeRoundtripMitTurnierstartseite() throws Exception {
        var gp = GlobalProperties.get();
        var panel = new PanelEintragRoh(
                PanelTyp.TURNIERSTARTSEITE, "", GlobalProperties.DEFAULT_ZOOM, "kein", "kein", false, "");
        var eintrag = new CompositeViewEintragRoh(
                5001, "Anzeige", true, GlobalProperties.DEFAULT_ZOOM, true, "", List.of(panel), RandKonfiguration.KEINER);

        gp.speichernCompositeViews(true, List.of(eintrag));

        GlobalProperties.resetForTest();
        var gp2 = GlobalProperties.get();
        var gelesenesPanel = gp2.getCompositeViewEintraege().get(0).panels().get(0);
        assertEquals(PanelTyp.TURNIERSTARTSEITE, gelesenesPanel.typ());
        assertEquals("", gelesenesPanel.sheetConfig());
        assertEquals("", gelesenesPanel.externeUrl());

        var konfigPanel = gp2.getCompositeViewKonfigurationen().get(0).panels().get(0);
        assertEquals(PanelTyp.TURNIERSTARTSEITE, konfigPanel.typ());
        assertEquals("", konfigPanel.sheetConfig());
        assertEquals("", konfigPanel.externeUrl());
    }

    @Test
    void testGespeichertesLayoutMitGroessenBleibtErhalten() throws Exception {
        var file = tempDir.resolve("PetanqueTurnierManager.properties");
        java.nio.file.Files.writeString(file,
                "webserver_composite_ports=5001\n"
                        + "webserver_composite_5001_aktiv=true\n"
                        + "webserver_composite_5001_layout={\"richtung\":\"H\",\"groesse\":35,\"links\":{\"panel\":0},\"rechts\":{\"panel\":1}}\n"
                        + "webserver_composite_5001_panel_count=2\n"
                        + "webserver_composite_5001_panel_0_typ=BLATT\n"
                        + "webserver_composite_5001_panel_0_sheet=RANGLISTE\n");
        java.nio.file.Files.writeString(file,
                java.nio.file.Files.readString(file)
                        + "webserver_composite_5001_panel_1_typ=BLATT\n"
                        + "webserver_composite_5001_panel_1_sheet=MELDELISTE\n");

        var gp = GlobalProperties.get();
        var eintraege = gp.getCompositeViewEintraege();
        assertEquals(1, eintraege.size(),
                "Eintrag darf trotz vorhandener Layout-Property nicht verworfen werden");
        assertEquals(2, eintraege.get(0).panels().size());
        assertTrue(eintraege.get(0).layoutJson().contains("\"groesse\":35"));
        assertFalse(gp.getCompositeViewKonfigurationen().isEmpty());

        gp.speichernCompositeViews(true, eintraege);
        String gespeichert = java.nio.file.Files.readString(file);
        assertTrue(gespeichert.contains("webserver_composite_5001_layout"));
        assertTrue(gespeichert.contains("groesse"));
    }

    @Test
    void testTabFarbeOhneLibreOfficeKontextLiefertDefault() {
        // Ohne LO-Kontext (wie in diesem Unit-Test) liefert getTabFarbe() immer defaultVal,
        // da die globalen Tab-Farben-Defaults ausschließlich in der LO-Konfiguration liegen.
        var gp = GlobalProperties.get();

        int farbe = gp.getTabFarbe("Tab-Farbe Meldeliste", 0xABCDEF);

        assertEquals(0xABCDEF, farbe);
    }

    @Test
    void testTabFarbenLegacyKeysWerdenBeimStartEntfernt() throws Exception {
        var file = tempDir.resolve("PetanqueTurnierManager.properties");
        java.nio.file.Files.writeString(file,
                "tabfarbe.meldeliste=2544dd\n"
                        + "tabfarbe.foo=ZZZZ\n"
                        + "loglevel=info\n");

        var gp = GlobalProperties.get();
        assertNotNull(gp);
        assertEquals("info", gp.getLogLevel());

        // Legacy-Tab-Farben-Keys werden verworfen, nicht übernommen.
        var inhalt = java.nio.file.Files.readString(file);
        assertFalse(inhalt.contains("tabfarbe."));

        // Ohne LO-Kontext liefert getTabFarbe() weiterhin nur den übergebenen Default.
        assertEquals(0xABCDEF, gp.getTabFarbe("Tab-Farbe Meldeliste", 0xABCDEF));
    }

    @Test
    void testUngueltigesLogLevel() throws Exception {
        var file = tempDir.resolve("PetanqueTurnierManager.properties");
        java.nio.file.Files.writeString(file, "loglevel=turbo\n");

        var gp = GlobalProperties.get();

        assertNotNull(gp);
        assertEquals("turbo", gp.getLogLevel());
    }

    @Test
    void testThreadSafety() throws Exception {
        var gp = GlobalProperties.get();

        Runnable lesend = () -> {
            for (int i = 0; i < 100; i++) {
                gp.isAutoSave();
                gp.getCompositeViewEintraege();
                gp.getLogLevel();
            }
        };

        Runnable schreibend = () -> {
            for (int i = 0; i < 20; i++) {
                gp.speichern(i % 2 == 0, i % 3 == 0, false, true, true, false, "info", true);
            }
        };

        Thread t1 = new Thread(lesend);
        Thread t2 = new Thread(schreibend);
        Thread t3 = new Thread(lesend);

        t1.start();
        t2.start();
        t3.start();

        t1.join();
        t2.join();
        t3.join();

        // Kein Crash und konsistenter Endzustand
        assertNotNull(gp.getLogLevel());
    }
}
