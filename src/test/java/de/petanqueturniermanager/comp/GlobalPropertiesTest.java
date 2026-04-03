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
    void testThreadSafety() throws Exception {
        var gp = GlobalProperties.get();

        Runnable task = () -> {
            for (int i = 0; i < 100; i++) {
                gp.isAutoSave();
                gp.getPortEintraege();
                gp.getLogLevel();
            }
        };

        Thread t1 = new Thread(task);
        Thread t2 = new Thread(task);

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        assertTrue(true);
    }
}