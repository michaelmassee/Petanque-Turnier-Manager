package de.petanqueturniermanager.helper.msgbox;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.helper.msgbox.ProcessBox.LinkEintrag;

/**
 * Smoke-Tests für die UNO-basierte {@link ProcessBox}.
 * Prüft Headless-No-Op, Dialog-Erzeugung, Throbber-Animation und Log-Append.
 */
public class ProcessBoxUITest extends BaseCalcUITest {

    @BeforeEach
    @Override
    public void beforeTest() {
        super.beforeTest();
    }

    @AfterEach
    public void afterTest() {
        // Singleton in einen sauberen Zustand zurückversetzen, damit Folge-Tests
        // (die ihrerseits forceinit aufrufen) nicht auf stehengelassener Sichtbarkeit aufsetzen.
        try {
            ProcessBox.from().hide();
        } catch (IllegalStateException ignored) {
            // bereits disposed
        }
    }

    @Test
    public void headlessModus_macht_keinen_Dialog() {
        ProcessBox.setHeadlessMode(true);
        try {
            ProcessBox.forceinit(starter.getxComponentContext());
            var pb = ProcessBox.from();
            assertThat(pb.getXWindow()).as("Headless: kein UNO-Window").isNull();
            assertThat(pb.getThrobberAnimation()).as("Headless: keine Throbber-Animation").isNull();

            // Diese Aufrufe dürfen nicht crashen
            pb.run();
            pb.info("hallo");
            pb.fehler("nein");
            pb.ready();
            pb.clear();
            pb.hide();
            pb.visible();

            assertThat(pb.istSichtbar()).as("Headless: nie sichtbar").isFalse();
        } finally {
            ProcessBox.setHeadlessMode(false);
        }
    }

    @Test
    public void dialog_wird_erzeugt_und_throbber_animiert() {
        ProcessBox.setHeadlessMode(false);
        ProcessBox.forceinit(starter.getxComponentContext());
        var pb = ProcessBox.from();

        assertThat(pb.getXWindow()).as("Nicht-headless: XWindow vorhanden").isNotNull();
        assertThat(pb.getThrobberAnimation()).as("Throbber-Control aufgelöst").isNotNull();

        var throbber = pb.getThrobberAnimation();
        assertThat(throbber.isAnimationRunning()).as("Throbber initial gestoppt").isFalse();

        pb.run();
        pb.flushUiUpdatesForTest();
        assertThat(throbber.isAnimationRunning()).as("Throbber läuft nach run()").isTrue();

        pb.ready();
        pb.flushUiUpdatesForTest();
        assertThat(throbber.isAnimationRunning()).as("Throbber gestoppt nach ready()").isFalse();
    }

    @Test
    public void info_haengt_an_log_an() {
        ProcessBox.setHeadlessMode(false);
        ProcessBox.forceinit(starter.getxComponentContext());
        var pb = ProcessBox.from();

        pb.clear();
        pb.flushUiUpdatesForTest();
        assertThat(pb.getLogText()).as("Log initial leer nach clear()").isEmpty();

        pb.info("erste Zeile");
        pb.info("zweite Zeile");
        pb.flushUiUpdatesForTest();

        String text = pb.getLogText();
        assertThat(text).contains("erste Zeile").contains("zweite Zeile");
        assertThat(text.lines().count()).as("zwei Log-Zeilen").isGreaterThanOrEqualTo(2);
    }

    @Test
    public void links_setzt_und_versteckt_slots() {
        ProcessBox.setHeadlessMode(false);
        ProcessBox.forceinit(starter.getxComponentContext());
        var pb = ProcessBox.from();

        pb.links(List.of(
                new LinkEintrag("Release-Seite öffnen", "https://example.com/release"),
                new LinkEintrag("ptm.oxt", "https://example.com/ptm.oxt")));
        pb.flushUiUpdatesForTest();

        assertThat(pb.getLinkForTest(0)).as("Slot 0 belegt")
                .isEqualTo(new LinkEintrag("Release-Seite öffnen", "https://example.com/release"));
        assertThat(pb.getLinkForTest(1)).as("Slot 1 belegt")
                .isEqualTo(new LinkEintrag("ptm.oxt", "https://example.com/ptm.oxt"));
        assertThat(pb.getLinkForTest(2)).as("Slot 2 unbelegt -> ausgeblendet").isNull();

        pb.links(List.of(new LinkEintrag("nur einer", "https://example.com/eins")));
        pb.flushUiUpdatesForTest();

        assertThat(pb.getLinkForTest(0)).as("Slot 0 neu belegt")
                .isEqualTo(new LinkEintrag("nur einer", "https://example.com/eins"));
        assertThat(pb.getLinkForTest(1)).as("Slot 1 wieder ausgeblendet").isNull();

        pb.clear();
        pb.flushUiUpdatesForTest();
        assertThat(pb.getLinkForTest(0)).as("clear() blendet alle Links aus").isNull();
    }
}
