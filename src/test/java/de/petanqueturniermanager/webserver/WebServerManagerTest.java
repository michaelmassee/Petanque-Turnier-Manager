package de.petanqueturniermanager.webserver;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.sun.star.lang.DisposedException;

import org.junit.jupiter.api.Test;

class WebServerManagerTest {

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
    void dokumentViewIdKodiertDokumentIdUndErhaeltBasisViewId() {
        String dokumentId = "url:file:///tmp/Turnier A.ods";
        String viewId = WebServerManager.dokumentViewId(dokumentId, WebServerManager.compositeViewId(8081));

        assertThat(viewId).startsWith("doc:");
        assertThat(viewId).endsWith(":composite:8081");
        assertThat(WebServerManager.dokumentIdAusViewId(viewId)).contains(dokumentId);
    }

    @Test
    void dokumentIdAusViewIdIgnoriertLegacyUndUngueltigeIds() {
        assertThat(WebServerManager.dokumentIdAusViewId(WebServerManager.compositeViewId(8081))).isEmpty();
        assertThat(WebServerManager.dokumentIdAusViewId("doc:@@@:composite:8081")).isEmpty();
    }

    @Test
    void regieQuelleInfoBleibtRueckwaertskompatibelFuerMasterQuellen() {
        var quelle = new RegieQuelleInfo("composite:8081", "Rangliste", 8081, true);

        assertThat(quelle.dokumentName()).isEmpty();
        assertThat(quelle.master()).isTrue();
    }

    @Test
    void dokumentRegieQuelleBehaeltCacheBeiCompositeKonfigurationswechsel() {
        var alt = compositeKonfiguration(8081, "Alt", 100);
        var neu = compositeKonfiguration(8081, "Neu", 125);
        var quelle = new DokumentRegieQuelle("doc:abc:composite:8081", "Doc - Alt", 8081, alt);
        quelle.setCachedInitJson("{\"state\":\"gerendert\"}");

        quelle.aktualisiereKonfiguration("Doc - Neu", neu);

        assertThat(quelle.getAnzeigeName()).isEqualTo("Doc - Neu");
        assertThat(quelle.getKonfiguration()).isEqualTo(neu);
        assertThat(quelle.getCachedInitJson()).isEqualTo("{\"state\":\"gerendert\"}");
        assertThat(quelle.laeuft()).isTrue();
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

    private static CompositeViewKonfiguration compositeKonfiguration(int port, String name, int zoom) {
        var panel = new PanelKonfiguration(PanelTyp.STATISCHE_DATEI, "", null, 100,
                "kein", "kein", false, "https://example.invalid/panel.html");
        return new CompositeViewKonfiguration(port, name, zoom, new SplitBlatt(0), List.of(panel),
                false, RandKonfiguration.KEINER);
    }
}
