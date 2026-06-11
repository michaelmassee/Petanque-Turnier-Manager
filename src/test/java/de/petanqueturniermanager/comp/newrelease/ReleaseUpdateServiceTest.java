/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.comp.newrelease;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.sun.star.uno.XComponentContext;

class ReleaseUpdateServiceTest {

    @TempDir
    java.nio.file.Path tempDir;

    private XComponentContext context;
    private ReleaseCache cache;

    @BeforeEach
    void setup() {
        context = mock(XComponentContext.class);
        cache = new ReleaseCache(tempDir.resolve("release.info"));
    }

    @AfterEach
    void teardown() {
        ReleaseUpdateService.resetSingletonFuerTest();
    }

    @Test
    void frischerCacheVerhindertNetzCall() throws Exception {
        var release = stabilesRelease("v2.0.0");
        cache.schreibe(release);

        var client = new ZaehlenderClient();
        var service = serviceMitInstallierterVersion("1.0.0", client);
        triggerInit(service);

        wartenBisStatusNicht(service, UpdateStatus.UNBEKANNT);

        assertThat(service.getStatus()).isEqualTo(UpdateStatus.UPDATE_VERFUEGBAR);
        assertThat(client.aufrufe.get()).isZero();
    }

    @Test
    void leererCacheTriggertNetzCall() throws Exception {
        var release = stabilesRelease("v1.0.0");
        var client = new FesterClient(Optional.of(release));
        var service = serviceMitInstallierterVersion("1.0.0", client);
        triggerInit(service);

        wartenBisStatusEntweder(service, UpdateStatus.KEIN_UPDATE, UpdateStatus.UPDATE_VERFUEGBAR);

        assertThat(service.getStatus()).isEqualTo(UpdateStatus.KEIN_UPDATE);
        assertThat(service.getAktuellesRelease()).isPresent();
        assertThat(cache.ladeUnabhaengigVomAlter()).isPresent();
    }

    @Test
    void neueVersionFuehrtZuUpdateVerfuegbar() throws Exception {
        var release = stabilesRelease("v2.0.0");
        var client = new FesterClient(Optional.of(release));
        var service = serviceMitInstallierterVersion("1.0.0", client);
        triggerInit(service);

        wartenBisStatusEntweder(service, UpdateStatus.UPDATE_VERFUEGBAR);
        assertThat(service.getStatus()).isEqualTo(UpdateStatus.UPDATE_VERFUEGBAR);
        assertThat(service.isUpdateVerfuegbar()).isTrue();
    }

    @Test
    void preReleaseWirdNichtAlsUpdateGemeldet() throws Exception {
        var release = new ReleaseInfo("v2.0.0-rc1", "v2.0.0-rc1",
                Instant.now(), true, null, List.of());
        var client = new FesterClient(Optional.of(release));
        var service = serviceMitInstallierterVersion("1.0.0", client);
        triggerInit(service);

        wartenBisStatusEntweder(service, UpdateStatus.KEIN_UPDATE);
        assertThat(service.getStatus()).isEqualTo(UpdateStatus.KEIN_UPDATE);
    }

    @Test
    void retryBeiFehlernUndDannErfolg() throws Exception {
        var release = stabilesRelease("v2.0.0");
        var client = new ScriptedClient(
                Optional.empty(),
                Optional.of(release)
        );
        var service = serviceMitInstallierterVersion("1.0.0", client, Duration.ofMillis(1));
        triggerInit(service);

        wartenBisStatusEntweder(service, UpdateStatus.UPDATE_VERFUEGBAR, UpdateStatus.KEIN_UPDATE,
                UpdateStatus.NICHT_VERFUEGBAR);
        assertThat(service.getStatus()).isEqualTo(UpdateStatus.UPDATE_VERFUEGBAR);
        assertThat(client.aufrufe.get()).isEqualTo(2);
    }

    @Test
    void alleRetriesFehlgeschlagenFuehrtZuNichtVerfuegbar() throws Exception {
        var client = new FesterClient(Optional.empty());
        var service = serviceMitInstallierterVersion("1.0.0", client, Duration.ofMillis(1));
        triggerInit(service);

        wartenBisStatusEntweder(service, UpdateStatus.NICHT_VERFUEGBAR);
        assertThat(service.getStatus()).isEqualTo(UpdateStatus.NICHT_VERFUEGBAR);
        assertThat(client.aufrufe.get()).isEqualTo(ReleaseUpdateService.DEFAULT_RETRY_BACKOFFS.size());
    }

    @Test
    void listenerWerdenNachUpdateBenachrichtigt() throws Exception {
        var release = stabilesRelease("v2.0.0");
        var client = new FesterClient(Optional.of(release));
        var service = serviceMitInstallierterVersion("1.0.0", client);

        var benachrichtigungen = new AtomicInteger();
        var latch = new CountDownLatch(1);
        Runnable listener = () -> {
            benachrichtigungen.incrementAndGet();
            if (service.getStatus() == UpdateStatus.UPDATE_VERFUEGBAR) {
                latch.countDown();
            }
        };
        service.addStatusListener(listener);

        triggerInit(service);
        assertThat(latch.await(2, TimeUnit.SECONDS))
                .as("Listener wurde nicht innerhalb von 2s mit UPDATE_VERFUEGBAR aufgerufen")
                .isTrue();
        assertThat(benachrichtigungen.get()).isGreaterThanOrEqualTo(1);

        service.removeStatusListener(listener);
        assertThat(service.listenerCountFuerTest()).isZero();
    }

    @Test
    void listenerExceptionBricktAndereNichtAb() throws Exception {
        var client = new FesterClient(Optional.of(stabilesRelease("v1.0.0")));
        var service = serviceMitInstallierterVersion("1.0.0", client);

        var ok = new AtomicInteger();
        service.addStatusListener(() -> { throw new RuntimeException("absichtlich"); });
        service.addStatusListener(ok::incrementAndGet);

        triggerInit(service);
        wartenBisStatusEntweder(service, UpdateStatus.KEIN_UPDATE);

        assertThat(ok.get()).as("Zweiter Listener muss trotz Fehler aufgerufen werden").isPositive();
    }

    @Test
    void disposeIstIdempotent() {
        var service = serviceMitInstallierterVersion("1.0.0", new FesterClient(Optional.empty()));
        service.dispose();
        service.dispose();
        assertThat(service.getStatus()).isIn(UpdateStatus.UNBEKANNT, UpdateStatus.LAEUFT,
                UpdateStatus.NICHT_VERFUEGBAR);
    }

    @Test
    void triggerRefreshNachDisposeMachtNichts() {
        var service = serviceMitInstallierterVersion("1.0.0", new FesterClient(Optional.empty()));
        service.dispose();
        service.triggerRefresh(true);
        // Kein Crash erwartet.
    }

    @Test
    void initIstNichtBlockierend() {
        var langsam = new LangsamerClient(Duration.ofMillis(500), Optional.of(stabilesRelease("v2.0.0")));
        var service = serviceMitInstallierterVersion("1.0.0", langsam);
        var start = System.nanoTime();
        triggerInit(service);
        var dauer = Duration.ofNanos(System.nanoTime() - start);
        assertThat(dauer)
                .as("init() muss sofort zurückkehren – kein Warten auf den Netz-Call")
                .isLessThan(Duration.ofMillis(200));
    }

    // ── Hilfsmittel ────────────────────────────────────────────────

    private ReleaseUpdateService serviceMitInstallierterVersion(String version, GithubReleaseClient client) {
        return serviceMitInstallierterVersion(version, client, null);
    }

    private ReleaseUpdateService serviceMitInstallierterVersion(String version, GithubReleaseClient client,
            Duration kurzerBackoff) {
        var backoffs = (kurzerBackoff != null)
                ? List.of(kurzerBackoff, kurzerBackoff, kurzerBackoff)
                : ReleaseUpdateService.DEFAULT_RETRY_BACKOFFS;
        var service = new ReleaseUpdateService(context, cache, client, backoffs);
        setzeInstallierteVersion(service, version);
        return service;
    }

    private static void setzeInstallierteVersion(ReleaseUpdateService service, String version) {
        try {
            Field f = ReleaseUpdateService.class.getDeclaredField("installierteVersion");
            f.setAccessible(true);
            f.set(service, Optional.of(version));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void triggerInit(ReleaseUpdateService service) {
        // Wir umgehen den Static-Init und rufen den privaten Refresh manuell aus dem Test-Thread.
        try {
            var m = ReleaseUpdateService.class.getDeclaredMethod("starteInitialenRefresh");
            m.setAccessible(true);
            m.invoke(service);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void wartenBisStatusNicht(ReleaseUpdateService service, UpdateStatus unerwuenscht)
            throws InterruptedException {
        var deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (service.getStatus() == unerwuenscht && System.nanoTime() < deadline) {
            Thread.sleep(20);
        }
    }

    private static void wartenBisStatusEntweder(ReleaseUpdateService service, UpdateStatus... erwartet)
            throws InterruptedException {
        var deadline = System.nanoTime() + Duration.ofSeconds(120).toNanos();
        var erwartetSet = java.util.Set.of(erwartet);
        while (!erwartetSet.contains(service.getStatus()) && System.nanoTime() < deadline) {
            Thread.sleep(50);
        }
    }

    private static ReleaseInfo stabilesRelease(String tag) {
        return new ReleaseInfo(tag, tag, Instant.now(), false, null,
                List.of(new AssetInfo("ptm.oxt", "https://example.com/" + tag + "/ptm.oxt")));
    }

    // ── Test-Clients ─────────────────────────────────────────────────

    private static class FesterClient extends GithubReleaseClient {
        final AtomicInteger aufrufe = new AtomicInteger();
        final Optional<ReleaseInfo> antwort;

        FesterClient(Optional<ReleaseInfo> antwort) {
            super("test/test", URI.create("http://localhost"), Duration.ofMillis(1), Duration.ofMillis(1));
            this.antwort = antwort;
        }

        @Override
        public Optional<ReleaseInfo> ladeLetztesRelease() {
            aufrufe.incrementAndGet();
            return antwort;
        }
    }

    private static class ZaehlenderClient extends FesterClient {
        ZaehlenderClient() {
            super(Optional.empty());
        }
    }

    private static class ScriptedClient extends GithubReleaseClient {
        final AtomicInteger aufrufe = new AtomicInteger();
        private final List<Optional<ReleaseInfo>> skript;

        @SafeVarargs
        ScriptedClient(Optional<ReleaseInfo>... skript) {
            super("test/test", URI.create("http://localhost"), Duration.ofMillis(1), Duration.ofMillis(1));
            // Arrays.stream statt List.of: das Varargs-Array nicht an die nächste
            // Varargs-Methode durchreichen, sonst warnt javac vor Heap Pollution.
            this.skript = Arrays.stream(skript).toList();
        }

        @Override
        public Optional<ReleaseInfo> ladeLetztesRelease() {
            int idx = aufrufe.getAndIncrement();
            return idx < skript.size() ? skript.get(idx) : skript.get(skript.size() - 1);
        }
    }

    private static class LangsamerClient extends GithubReleaseClient {
        final Duration latenz;
        final Optional<ReleaseInfo> antwort;
        final AtomicReference<Thread> aufruferThread = new AtomicReference<>();

        LangsamerClient(Duration latenz, Optional<ReleaseInfo> antwort) {
            super("test/test", URI.create("http://localhost"), Duration.ofMillis(1), Duration.ofMillis(1));
            this.latenz = latenz;
            this.antwort = antwort;
        }

        @Override
        public Optional<ReleaseInfo> ladeLetztesRelease() {
            aufruferThread.set(Thread.currentThread());
            try {
                Thread.sleep(latenz.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return antwort;
        }
    }

    @SuppressWarnings("unused")
    private static void unbenutzt() throws IOException {
        // damit der IOException-Import nicht meckert
    }
}
