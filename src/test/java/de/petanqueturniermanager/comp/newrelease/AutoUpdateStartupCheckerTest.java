/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.comp.newrelease;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.sun.star.uno.XComponentContext;

/**
 * Prüft die Anzeige-Bedingungslogik von {@link AutoUpdateStartupChecker}, ohne den
 * Dialog tatsächlich anzuzeigen (das würde eine echte LibreOffice-UI-Umgebung
 * benötigen). {@link AutoUpdateStartupChecker#sollDialogGezeigtWerden()} muss
 * false liefern, bevor jemals UI-Code erreicht wird.
 */
class AutoUpdateStartupCheckerTest {

    @TempDir
    java.nio.file.Path tempDir;

    private XComponentContext context;

    @BeforeEach
    void setup() {
        context = mock(XComponentContext.class);
        AutoUpdateStartupChecker.resetFuerTest();
    }

    @AfterEach
    void teardown() {
        ReleaseUpdateService.resetSingletonFuerTest();
        AutoUpdateStartupChecker.resetFuerTest();
    }

    @Test
    void keinDialogWennReleaseUpdateServiceNichtInitialisiert() {
        ReleaseUpdateService.resetSingletonFuerTest();

        var checker = new AutoUpdateStartupChecker(context);

        assertThat(checker.sollDialogGezeigtWerden()).isFalse();
    }

    @Test
    void keinDialogWennKeinUpdateVerfuegbar() throws Exception {
        var cache = new ReleaseCache(tempDir.resolve("release.info"));
        var client = new GithubReleaseClient("owner/repo") {
            @Override
            public java.util.Optional<ReleaseInfo> ladeLetztesRelease() {
                return java.util.Optional.empty();
            }
        };
        var service = new ReleaseUpdateService(context, cache, client, java.util.List.of());
        ReleaseUpdateService.ersetzeSingletonFuerTest(service);

        var checker = new AutoUpdateStartupChecker(context);

        assertThat(checker.sollDialogGezeigtWerden()).isFalse();
    }
}
