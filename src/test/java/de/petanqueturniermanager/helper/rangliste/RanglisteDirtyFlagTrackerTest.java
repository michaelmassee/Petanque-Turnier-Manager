package de.petanqueturniermanager.helper.rangliste;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.sun.star.sheet.XSpreadsheetDocument;

class RanglisteDirtyFlagTrackerTest {

    private RanglisteDirtyFlagTracker tracker;
    private XSpreadsheetDocument xDoc1;
    private XSpreadsheetDocument xDoc2;

    @BeforeEach
    void setUp() {
        tracker = new RanglisteDirtyFlagTracker();
        xDoc1 = Mockito.mock(XSpreadsheetDocument.class);
        xDoc2 = Mockito.mock(XSpreadsheetDocument.class);
    }

    // ── Initialer Zustand ───────────────────────────────────────────────────

    @Test
    void neuRegistriertesDokument_IstInitialDirty() {
        tracker.registriereDocument(xDoc1);

        assertThat(tracker.isDirtyUndConsume(xDoc1))
                .as("Neues Dokument muss initial dirty=true sein")
                .isTrue();
    }

    @Test
    void nichtRegistriertesDokument_IstImmerDirty() {
        assertThat(tracker.isDirtyUndConsume(xDoc1))
                .as("Unregistriertes Dokument muss dirty=true (Failsafe) liefern")
                .isTrue();

        assertThat(tracker.isDirtyUndConsume(xDoc1))
                .as("Unregistriertes Dokument muss bei jedem Aufruf dirty=true liefern")
                .isTrue();
    }

    // ── isDirtyUndConsume – Consume-Semantik ───────────────────────────────

    @Test
    void isDirtyUndConsume_SetzFlagZurueck() {
        tracker.registriereDocument(xDoc1);

        boolean ersterAufruf = tracker.isDirtyUndConsume(xDoc1);
        boolean zweiterAufruf = tracker.isDirtyUndConsume(xDoc1);

        assertThat(ersterAufruf).as("Erster Aufruf: war dirty").isTrue();
        assertThat(zweiterAufruf).as("Zweiter Aufruf ohne Änderung: clean").isFalse();
    }

    @Test
    void isDirtyUndConsume_BleibtFalseOhneAenderung() {
        tracker.registriereDocument(xDoc1);
        tracker.isDirtyUndConsume(xDoc1);  // consume initial dirty

        for (int i = 0; i < 5; i++) {
            assertThat(tracker.isDirtyUndConsume(xDoc1))
                    .as("Aufruf %d ohne Änderung dazwischen muss false sein", i + 1)
                    .isFalse();
        }
    }

    // ── markiereDirty ──────────────────────────────────────────────────────

    @Test
    void markiereDirty_NachConsume_SetzFlag() {
        tracker.registriereDocument(xDoc1);
        tracker.isDirtyUndConsume(xDoc1);  // consume → clean

        tracker.markiereDirty(xDoc1);

        assertThat(tracker.isDirtyUndConsume(xDoc1))
                .as("Nach markiereDirty muss isDirtyUndConsume true liefern")
                .isTrue();
    }

    @Test
    void markiereDirty_MehrmalsHintereinander_BleibtDirty() {
        tracker.registriereDocument(xDoc1);
        tracker.isDirtyUndConsume(xDoc1);  // consume initial

        tracker.markiereDirty(xDoc1);
        tracker.markiereDirty(xDoc1);
        tracker.markiereDirty(xDoc1);

        assertThat(tracker.isDirtyUndConsume(xDoc1))
                .as("Mehrfaches markiereDirty muss dirty=true erhalten")
                .isTrue();
        assertThat(tracker.isDirtyUndConsume(xDoc1))
                .as("Nach einmaligem Consume muss clean sein")
                .isFalse();
    }

    @Test
    void markiereDirty_UnbekanntesDokument_KeinFehler() {
        tracker.markiereDirty(xDoc1);

        // Bleibt unregistriert → Failsafe dirty=true
        assertThat(tracker.isDirtyUndConsume(xDoc1))
                .as("Unregistriertes Dokument bleibt Failsafe dirty=true")
                .isTrue();
    }

    // ── Mehrere Dokumente ──────────────────────────────────────────────────

    @Test
    void mehrereDokumente_WerdenUnabhaengigVerfolgt() {
        tracker.registriereDocument(xDoc1);
        tracker.registriereDocument(xDoc2);

        assertThat(tracker.isDirtyUndConsume(xDoc1)).isTrue();
        assertThat(tracker.isDirtyUndConsume(xDoc2)).isTrue();

        tracker.markiereDirty(xDoc1);

        assertThat(tracker.isDirtyUndConsume(xDoc1))
                .as("Doc1: dirty nach markiereDirty")
                .isTrue();
        assertThat(tracker.isDirtyUndConsume(xDoc2))
                .as("Doc2: clean – nur doc1 wurde markiert")
                .isFalse();
    }

    @Test
    void mehrereDokumente_ConsumeEinesBeeinflussAnderesNicht() {
        tracker.registriereDocument(xDoc1);
        tracker.registriereDocument(xDoc2);

        tracker.isDirtyUndConsume(xDoc1);  // consume doc1

        assertThat(tracker.isDirtyUndConsume(xDoc1))
                .as("Doc1: clean nach Consume")
                .isFalse();
        assertThat(tracker.isDirtyUndConsume(xDoc2))
                .as("Doc2: noch dirty (initial), nicht betroffen von doc1-Consume")
                .isTrue();
    }

    // ── registriereDocument-Idempotenz ─────────────────────────────────────

    @Test
    void registriereDocument_Zweimal_BehältVorhandeneCleanFlag() {
        tracker.registriereDocument(xDoc1);
        tracker.isDirtyUndConsume(xDoc1);  // consume initial → clean

        tracker.registriereDocument(xDoc1);  // zweiter Aufruf

        assertThat(tracker.isDirtyUndConsume(xDoc1))
                .as("Zweites registriere darf vorhandenes clean-Flag nicht überschreiben")
                .isFalse();
    }

    @Test
    void registriereDocument_Zweimal_BehältVorhandenesDirtyFlag() {
        tracker.registriereDocument(xDoc1);
        // initial dirty, NICHT consumed

        tracker.registriereDocument(xDoc1);  // zweiter Aufruf

        assertThat(tracker.isDirtyUndConsume(xDoc1))
                .as("Zweites registriere darf vorhandenes dirty-Flag nicht zurücksetzen")
                .isTrue();
    }

    // ── entferneDocument ───────────────────────────────────────────────────

    @Test
    void entferneDocument_DanachFailsafeDirty() {
        tracker.registriereDocument(xDoc1);
        tracker.isDirtyUndConsume(xDoc1);  // consume → clean

        tracker.entferneDocument(xDoc1);

        assertThat(tracker.isDirtyUndConsume(xDoc1))
                .as("Nach entferneDocument: Failsafe dirty=true")
                .isTrue();
    }

    @Test
    void entferneDocument_UnbekanntesDokument_KeinFehler() {
        tracker.entferneDocument(xDoc1);
    }

    @Test
    void entferneDocument_AnderesDokumentUnberührt() {
        tracker.registriereDocument(xDoc1);
        tracker.registriereDocument(xDoc2);
        tracker.isDirtyUndConsume(xDoc1);  // consume doc1
        tracker.isDirtyUndConsume(xDoc2);  // consume doc2

        tracker.entferneDocument(xDoc1);

        assertThat(tracker.isDirtyUndConsume(xDoc2))
                .as("Doc2 darf nach entferneDocument(doc1) nicht verändert sein")
                .isFalse();
    }

    // ── Thread-Sicherheit ──────────────────────────────────────────────────

    @Test
    void threadSicherheit_ParalleleMarkiereDirtyUndConsume() throws InterruptedException {
        tracker.registriereDocument(xDoc1);
        tracker.isDirtyUndConsume(xDoc1);  // start clean

        int anzThreads = 20;
        int iterationenProThread = 1000;
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch fertig = new CountDownLatch(anzThreads);
        AtomicInteger trueZaehler = new AtomicInteger(0);

        ExecutorService pool = Executors.newFixedThreadPool(anzThreads);
        for (int t = 0; t < anzThreads; t++) {
            boolean istMarkierer = (t % 2 == 0);
            pool.submit(() -> {
                try {
                    startSignal.await();
                    for (int i = 0; i < iterationenProThread; i++) {
                        if (istMarkierer) {
                            tracker.markiereDirty(xDoc1);
                        } else {
                            if (tracker.isDirtyUndConsume(xDoc1)) {
                                trueZaehler.incrementAndGet();
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    fertig.countDown();
                }
            });
        }

        startSignal.countDown();
        assertThat(fertig.await(10, TimeUnit.SECONDS))
                .as("Alle Threads müssen innerhalb von 10 Sekunden fertig sein")
                .isTrue();
        pool.shutdown();
        // Kein Deadlock, kein NPE, kein IllegalStateException → Thread-sicher
    }

    @Test
    void threadSicherheit_ParalleleRegistrierung() throws InterruptedException {
        int anzThreads = 10;
        CountDownLatch fertig = new CountDownLatch(anzThreads);
        ExecutorService pool = Executors.newFixedThreadPool(anzThreads);

        for (int t = 0; t < anzThreads; t++) {
            pool.submit(() -> {
                try {
                    tracker.registriereDocument(xDoc1);
                } finally {
                    fertig.countDown();
                }
            });
        }

        fertig.await(5, TimeUnit.SECONDS);
        pool.shutdown();

        // Nach paralleler Registrierung: genau ein Flag → einmal dirty, danach clean
        assertThat(tracker.isDirtyUndConsume(xDoc1)).isTrue();
        assertThat(tracker.isDirtyUndConsume(xDoc1)).isFalse();
    }
}
