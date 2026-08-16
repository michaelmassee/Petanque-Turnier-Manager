/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.comp.newrelease;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.sun.star.uno.XComponentContext;

class ReleaseSeiteUrlTest {

    @AfterEach
    void teardown() {
        ReleaseUpdateService.resetSingletonFuerTest();
    }

    @Test
    void liefertHtmlUrlDesAktuellenReleases() {
        var release = new ReleaseInfo("v2.0.0", "v2.0.0", Instant.now(), false, null, List.of(),
                "https://github.com/foo/bar/releases/tag/v2.0.0");
        ReleaseUpdateService.ersetzeSingletonFuerTest(serviceMitRelease(release));

        assertThat(ReleaseSeiteUrl.ermitteln()).isEqualTo("https://github.com/foo/bar/releases/tag/v2.0.0");
    }

    @Test
    void fallbackWennKeinReleaseVorliegt() {
        ReleaseUpdateService.ersetzeSingletonFuerTest(serviceMitRelease(null));

        assertThat(ReleaseSeiteUrl.ermitteln()).isEqualTo(ReleaseSeiteUrl.FALLBACK_URL);
    }

    @Test
    void fallbackWennServiceNichtInitialisiert() {
        ReleaseUpdateService.resetSingletonFuerTest();

        assertThat(ReleaseSeiteUrl.ermitteln()).isEqualTo(ReleaseSeiteUrl.FALLBACK_URL);
    }

    private static ReleaseUpdateService serviceMitRelease(ReleaseInfo release) {
        var context = mock(XComponentContext.class);
        var client = new GithubReleaseClient("test/test") {
            @Override
            public Optional<ReleaseInfo> ladeLetztesRelease() {
                return Optional.empty();
            }
        };
        var service = new ReleaseUpdateService(context, client, List.of());
        if (release != null) {
            setzeAktuellesRelease(service, release);
        }
        return service;
    }

    private static void setzeAktuellesRelease(ReleaseUpdateService service, ReleaseInfo release) {
        try {
            Field f = ReleaseUpdateService.class.getDeclaredField("aktuellesRelease");
            f.setAccessible(true);
            f.set(service, Optional.of(release));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
