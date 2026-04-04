package de.petanqueturniermanager.comp;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

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
        assertEquals("", gp.getLogLevel());
    }

    @Test
    void testSpeichernUndLesen() {
        var gp = GlobalProperties.get();

        gp.speichern(true, true, false, "debug");

        GlobalProperties.resetForTest();
        var gp2 = GlobalProperties.get();

        assertTrue(gp2.isAutoSave());
        assertTrue(gp2.isCreateBackup());
        assertFalse(gp2.isNewVersionCheckImmerTrue());
        assertEquals("debug", gp2.getLogLevel());
    }

    @Test
    void testPortEintraegeLeer() {
        var gp = GlobalProperties.get();

        List<GlobalProperties.PortEintragRoh> ports = gp.getPortEintraege();

        assertNotNull(ports);
        assertTrue(ports.isEmpty());
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
    void testSpeichernWebserver() {
        var gp = GlobalProperties.get();
        var eintraege = List.of(
                new GlobalProperties.PortEintragRoh(9001, "rangliste", true, 120, false),
                new GlobalProperties.PortEintragRoh(9002, "spielrunde", false, 100, true));

        gp.speichernWebserver(true, true, eintraege);

        GlobalProperties.resetForTest();
        var gp2 = GlobalProperties.get();

        assertTrue(gp2.isWebserverAktiv());
        assertTrue(gp2.isSheetnamenKopfzeileAnzeigen());
        var geladen = gp2.getPortEintraege();
        assertEquals(2, geladen.size());
        assertEquals(9001, geladen.get(0).port());
        assertEquals("rangliste", geladen.get(0).sheetConfig());
        assertTrue(geladen.get(0).aktiv());
        assertEquals(120, geladen.get(0).zoom());
        assertFalse(geladen.get(0).zentrieren());
        assertEquals(9002, geladen.get(1).port());
        assertFalse(geladen.get(1).aktiv());
        assertTrue(geladen.get(1).zentrieren());
    }

    @Test
    void testUngueltigerZoomWert() throws Exception {
        var file = tempDir.resolve("PetanqueTurnierManager.properties");
        java.nio.file.Files.writeString(file,
                "webserver_ports=9001\n" +
                "webserver_port_9001_sheet=rangliste\n" +
                "webserver_port_9001_aktiv=true\n" +
                "webserver_port_9001_zoom=abc\n");

        var gp = GlobalProperties.get();
        var eintraege = gp.getPortEintraege();

        assertFalse(eintraege.isEmpty());
        assertEquals(GlobalProperties.DEFAULT_ZOOM, eintraege.get(0).zoom());
    }

    @Test
    void testUngueltigerPortNummer() throws Exception {
        var file = tempDir.resolve("PetanqueTurnierManager.properties");
        java.nio.file.Files.writeString(file,
                "webserver_ports=KAPUTT,9001\n" +
                "webserver_port_9001_sheet=rangliste\n" +
                "webserver_port_9001_aktiv=true\n");

        var gp = GlobalProperties.get();
        var eintraege = gp.getPortEintraege();

        assertEquals(1, eintraege.size());
        assertEquals(9001, eintraege.get(0).port());
    }

    @Test
    void testUngueltigeHexFarbe() throws Exception {
        var file = tempDir.resolve("PetanqueTurnierManager.properties");
        java.nio.file.Files.writeString(file, "tabfarbe.foo=ZZZZ\n");

        var gp = GlobalProperties.get();
        int farbe = gp.getTabFarbe("Tab-Farbe foo", 0xABCDEF);

        assertEquals(0xABCDEF, farbe);
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
                gp.getPortEintraege();
                gp.getLogLevel();
            }
        };

        Runnable schreibend = () -> {
            for (int i = 0; i < 20; i++) {
                gp.speichern(i % 2 == 0, i % 3 == 0, false, "info");
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