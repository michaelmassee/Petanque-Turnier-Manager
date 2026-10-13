package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.sun.star.uno.XComponentContext;

class PtmOnlineLiveBeobachterTest {

    @AfterEach
    void aufraeumen() {
        PtmOnlineLiveBeobachter.zuruecksetzenFuerTest();
    }

    @Test
    void officeWechselErzeugtBeobachterMitNeuemKontext() throws Exception {
        XComponentContext ersterKontext = mock(XComponentContext.class);
        PtmOnlineLiveBeobachter erster = PtmOnlineLiveBeobachter.init(ersterKontext);

        PtmOnlineLiveBeobachter.zuruecksetzenFuerTest();
        PtmOnlineLiveBeobachter.zuruecksetzenFuerTest(); // wiederholter Teardown bleibt erlaubt

        XComponentContext neuerKontext = mock(XComponentContext.class);
        PtmOnlineLiveBeobachter neuer = PtmOnlineLiveBeobachter.init(neuerKontext);
        assertThat(neuer).isNotSameAs(erster);
        var kontextFeld = PtmOnlineLiveBeobachter.class.getDeclaredField("xContext");
        kontextFeld.setAccessible(true);
        assertThat(kontextFeld.get(neuer)).isSameAs(neuerKontext);
        assertThat(executor(erster).isTerminated()).isTrue();
    }

    @Test
    void officeWechselVerwirftNochGeplanteArbeit() throws Exception {
        PtmOnlineLiveBeobachter beobachter = PtmOnlineLiveBeobachter.init(mock(XComponentContext.class));
        AtomicBoolean ausgefuehrt = new AtomicBoolean();
        var geplant = executor(beobachter).schedule(() -> ausgefuehrt.set(true), 1, TimeUnit.HOURS);

        PtmOnlineLiveBeobachter.zuruecksetzenFuerTest();

        assertThat(geplant.isCancelled()).isTrue();
        assertThat(ausgefuehrt).isFalse();
        assertThat(executor(beobachter).isTerminated()).isTrue();
    }

    @Test
    void officeWechselWartetAuchAufAufraeumarbeitenDesKindThreads() throws Exception {
        pruefeKindThreadShutdown(true);
    }

    @Test
    void pluginShutdownWartetNichtAufDenLibreOfficeKindThread() throws Exception {
        pruefeKindThreadShutdown(false);
    }

    @Test
    void pluginShutdownVerwirftGeplanteArbeitUndGibtDenKontextFrei() throws Exception {
        XComponentContext kontext = mock(XComponentContext.class);
        PtmOnlineLiveBeobachter beobachter = PtmOnlineLiveBeobachter.init(kontext);
        AtomicBoolean ausgefuehrt = new AtomicBoolean();
        var geplant = executor(beobachter).schedule(() -> ausgefuehrt.set(true), 1, TimeUnit.HOURS);

        PtmOnlineLiveBeobachter.dispose();
        PtmOnlineLiveBeobachter.dispose();

        assertThat(executor(beobachter).awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        assertThat(geplant.isCancelled()).isTrue();
        assertThat(ausgefuehrt).isFalse();
        verifyNoInteractions(kontext);
        assertThat(PtmOnlineLiveBeobachter.init(mock(XComponentContext.class))).isNotSameAs(beobachter);
    }

    private void pruefeKindThreadShutdown(boolean synchronWarten) throws Exception {
        PtmOnlineLiveBeobachter beobachter = PtmOnlineLiveBeobachter.init(mock(XComponentContext.class));
        CountDownLatch kindGestartet = new CountDownLatch(1);
        CountDownLatch schedulerGestartet = new CountDownLatch(1);
        CountDownLatch kindRaeumtAuf = new CountDownLatch(1);
        CountDownLatch aufraeumenErlaubt = new CountDownLatch(1);
        AtomicBoolean kindFertig = new AtomicBoolean();
        Thread kind = new Thread(() -> {
            kindGestartet.countDown();
            try {
                new CountDownLatch(1).await();
            } catch (InterruptedException e) {
                kindRaeumtAuf.countDown();
                try {
                    aufraeumenErlaubt.await();
                    kindFertig.set(true);
                } catch (InterruptedException erneut) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        kind.setDaemon(true);
        kind.start();
        CompletableFuture<Void> reset = null;
        try {
            assertThat(kindGestartet.await(5, TimeUnit.SECONDS)).isTrue();
            executor(beobachter).submit(() -> {
                schedulerGestartet.countDown();
                try {
                    PtmOnlineLiveBeobachter.warteAufRunner(kind);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            assertThat(schedulerGestartet.await(5, TimeUnit.SECONDS)).isTrue();
            reset = CompletableFuture.runAsync(synchronWarten
                    ? PtmOnlineLiveBeobachter::zuruecksetzenFuerTest : PtmOnlineLiveBeobachter::dispose);
            assertThat(kindRaeumtAuf.await(5, TimeUnit.SECONDS)).isTrue();
            if (synchronWarten) {
                assertThat(reset.isDone()).as("Office darf nicht vor dem Kind-Thread beendet werden").isFalse();
            } else {
                reset.get(5, TimeUnit.SECONDS);
                assertThat(kindFertig).as("Plugin-Shutdown kehrt vor dem Ende der Aufräumarbeiten zurück").isFalse();
                assertThatThrownBy(() -> PtmOnlineLiveBeobachter.init(mock(XComponentContext.class)))
                        .isInstanceOf(IllegalStateException.class);
            }
            aufraeumenErlaubt.countDown();
            reset.get(5, TimeUnit.SECONDS);
            assertThat(executor(beobachter).awaitTermination(5, TimeUnit.SECONDS)).isTrue();
            assertThat(kindFertig).isTrue();
            assertThat(kind.isAlive()).isFalse();
            assertThat(executor(beobachter).isTerminated()).isTrue();
        } finally {
            aufraeumenErlaubt.countDown();
            if (kindRaeumtAuf.getCount() > 0) {
                kind.interrupt();
            }
            kind.join(5000);
            if (reset != null) {
                reset.get(5, TimeUnit.SECONDS);
            }
        }
    }

    private static ScheduledExecutorService executor(PtmOnlineLiveBeobachter beobachter) throws Exception {
        var feld = PtmOnlineLiveBeobachter.class.getDeclaredField("ausfuehrung");
        feld.setAccessible(true);
        return (ScheduledExecutorService) feld.get(beobachter);
    }
}
