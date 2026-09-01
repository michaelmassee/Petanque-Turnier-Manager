package de.petanqueturniermanager.webserver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.sun.star.lang.DisposedException;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.uno.XComponentContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class WebServerManagerTest {

    @AfterEach
    void ownerZustandZuruecksetzen() throws Exception {
        setLaeuft(false);
        setOwnerDocument(null);
    }

    @Test
    void startseiteLogoUrlIstRelativFuerRegieZiele() {
        assertThat(WebServerManager.startseiteLogoUrl("/tmp/logo.png", 7))
                .isEqualTo("turnierlogo?v=7");
    }

    @Test
    void startseiteLogoUrlIstLeerOhneLogoQuelle() {
        assertThat(WebServerManager.startseiteLogoUrl("", 7)).isEmpty();
        assertThat(WebServerManager.startseiteLogoUrl(null, 7)).isEmpty();
    }

    @Test
    void erkenntGeschlossenesUnoDokumentAuchAlsUrsache() {
        var exception = new RuntimeException("Refresh fehlgeschlagen",
                new DisposedException("Dokument geschlossen", null));

        assertThat(WebServerManager.istDokumentGeschlossen(exception)).isTrue();
        assertThat(WebServerManager.istDokumentGeschlossen(new RuntimeException("anderer Fehler"))).isFalse();
    }

    @Test
    void konfigurationGeaendertWartetNichtAufManagerMonitor() throws Exception {
        var manager = WebServerManager.get();
        var lockGehalten = new CountDownLatch(1);
        var freigeben = new CountDownLatch(1);
        var lockThread = new Thread(() -> {
            synchronized (manager) {
                lockGehalten.countDown();
                try {
                    freigeben.await(2, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "test-webserver-manager-lock");
        lockThread.start();
        assertThat(lockGehalten.await(1, TimeUnit.SECONDS)).isTrue();

        var aufrufer = Executors.newSingleThreadExecutor();
        try {
            var future = aufrufer.submit(manager::konfigurationGeaendert);
            future.get(200, TimeUnit.MILLISECONDS);
        } finally {
            freigeben.countDown();
            lockThread.join(1_000);
            aufrufer.shutdownNow();
        }
    }

    @Test
    void bindetOwnerNachtraeglichBeiOwnerlosemStart() throws Exception {
        var manager = WebServerManager.get();
        var dokument = mock(XSpreadsheetDocument.class);
        setLaeuft(true);
        setOwnerDocument(null);

        manager.starten(mock(XComponentContext.class), dokument);

        assertThat(manager.istOwnerDocument(dokument)).isTrue();
    }

    @Test
    void bindetOwnerNichtErneutWennBereitsGesetzt() throws Exception {
        var manager = WebServerManager.get();
        var bestehenderOwner = mock(XSpreadsheetDocument.class);
        var anderesDokument = mock(XSpreadsheetDocument.class);
        setLaeuft(true);
        setOwnerDocument(bestehenderOwner);

        manager.starten(mock(XComponentContext.class), anderesDokument);

        assertThat(manager.istOwnerDocument(bestehenderOwner)).isTrue();
        assertThat(manager.istOwnerDocument(anderesDokument)).isFalse();
    }

    private static void setLaeuft(boolean wert) throws Exception {
        Field feld = WebServerManager.class.getDeclaredField("laeuft");
        feld.setAccessible(true);
        feld.set(WebServerManager.get(), wert);
    }

    private static void setOwnerDocument(XSpreadsheetDocument dokument) throws Exception {
        Field feld = WebServerManager.class.getDeclaredField("ownerDocument");
        feld.setAccessible(true);
        feld.set(WebServerManager.get(), dokument);
    }
}
