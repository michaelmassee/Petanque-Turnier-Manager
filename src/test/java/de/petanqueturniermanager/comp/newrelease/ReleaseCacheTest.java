/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.comp.newrelease;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReleaseCacheTest {

    @TempDir
    Path tempDir;

    private Path cacheDatei;
    private ReleaseCache cache;

    @BeforeEach
    void setup() {
        cacheDatei = tempDir.resolve("release.info");
        cache = new ReleaseCache(cacheDatei);
    }

    @Test
    void schreibenUndLesenLiefertGleichesRelease() throws IOException {
        var release = beispielRelease();
        cache.schreibe(release);

        var geladen = cache.ladeWennFrisch(Duration.ofHours(24));

        assertThat(geladen).isPresent();
        assertThat(geladen.get().tagName()).isEqualTo("v1.2.3");
        assertThat(geladen.get().assets()).hasSize(1);
        assertThat(geladen.get().assets().get(0).downloadUrl())
                .isEqualTo("https://example.com/foo.oxt");
    }

    @Test
    void datenAelterAlsTtlGeltenAlsStale() throws IOException {
        var vorEinerWoche = Instant.now().minus(Duration.ofDays(7));
        cache.schreibeMitZeitstempel(beispielRelease(), vorEinerWoche);

        assertThat(cache.ladeWennFrisch(Duration.ofHours(6))).isEmpty();
    }

    @Test
    void ladeUnabhaengigVomAlterLiefertAuchAlteEintraege() throws IOException {
        var vorEinerWoche = Instant.now().minus(Duration.ofDays(7));
        cache.schreibeMitZeitstempel(beispielRelease(), vorEinerWoche);

        assertThat(cache.ladeUnabhaengigVomAlter()).isPresent();
    }

    @Test
    void fehlendeDateiLiefertEmpty() {
        assertThat(cache.ladeWennFrisch(Duration.ofHours(1))).isEmpty();
        assertThat(cache.ladeUnabhaengigVomAlter()).isEmpty();
    }

    @Test
    void kaputtesJsonWirdAlsKorruptUmbenannt() throws IOException {
        Files.writeString(cacheDatei, "{ das ist kein json ");

        assertThat(cache.ladeWennFrisch(Duration.ofHours(1))).isEmpty();
        assertThat(Files.exists(cacheDatei)).isFalse();
        var korrupt = listeKorruptDateien();
        assertThat(korrupt)
                .as("Erwartet eine .corrupt-*-Datei im Tempdir")
                .hasSize(1);
    }

    @Test
    void unvollstaendigeStrukturWirdAlsKorruptUmbenannt() throws IOException {
        // gültiges JSON aber ohne cachedAt
        Files.writeString(cacheDatei, "{ \"release\": {\"tagName\": \"v1.0\"} }");

        assertThat(cache.ladeWennFrisch(Duration.ofHours(1))).isEmpty();
        assertThat(Files.exists(cacheDatei)).isFalse();
        assertThat(listeKorruptDateien()).hasSize(1);
    }

    @Test
    void schreibenLegtVerzeichnisAn() throws IOException {
        var tieferPfad = tempDir.resolve("a/b/c/release.info");
        var tiefenCache = new ReleaseCache(tieferPfad);
        tiefenCache.schreibe(beispielRelease());
        assertThat(Files.exists(tieferPfad)).isTrue();
    }

    @Test
    void schreibenHinterlaesstKeineTmpDatei() throws IOException {
        cache.schreibe(beispielRelease());
        try (Stream<Path> stream = Files.list(tempDir)) {
            assertThat(stream)
                    .as(".tmp-Datei muss nach Move verschwunden sein")
                    .noneMatch(p -> p.getFileName().toString().endsWith(".tmp"));
        }
    }

    @Test
    void schreibenUeberschreibtAltenInhalt() throws IOException {
        cache.schreibe(beispielRelease());
        var neuesRelease = new ReleaseInfo(
                "v2.0.0", "v2.0.0", Instant.now(), false, null,
                List.of(new AssetInfo("ptm-2.0.0.oxt", "https://example.com/ptm-2.0.0.oxt")));
        cache.schreibe(neuesRelease);

        var geladen = cache.ladeWennFrisch(Duration.ofHours(1));
        assertThat(geladen).isPresent();
        assertThat(geladen.get().tagName()).isEqualTo("v2.0.0");
    }

    private static ReleaseInfo beispielRelease() {
        return new ReleaseInfo(
                "v1.2.3",
                "v1.2.3",
                Instant.parse("2026-04-01T10:00:00Z"),
                false,
                "Release-Notes",
                List.of(new AssetInfo("foo.oxt", "https://example.com/foo.oxt")));
    }

    private List<Path> listeKorruptDateien() throws IOException {
        try (Stream<Path> stream = Files.list(tempDir)) {
            return stream
                    .filter(p -> p.getFileName().toString().startsWith("release.info.corrupt-"))
                    .toList();
        }
    }
}
