/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.comp.newrelease;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.ProcessBox.LinkEintrag;

class ReleaseInfosAnzeigenTest {

    @BeforeEach
    void init() {
        I18n.init(null);
    }

    @Test
    void releaseSeiteZuerstDannAssets() {
        var release = new ReleaseInfo("v1.2.3", "v1.2.3", null, false, "Notes",
                List.of(new AssetInfo("ptm.oxt", "https://example.com/ptm.oxt")),
                "https://github.com/foo/bar/releases/tag/v1.2.3");

        var links = ReleaseInfosAnzeigen.releaseLinks(release);

        assertThat(links).containsExactly(
                new LinkEintrag("Release-Seite öffnen", "https://github.com/foo/bar/releases/tag/v1.2.3"),
                new LinkEintrag("ptm.oxt", "https://example.com/ptm.oxt"));
    }

    @Test
    void ohneHtmlUrlNurAssets() {
        var release = new ReleaseInfo("v1.2.3", "v1.2.3", null, false, "Notes",
                List.of(new AssetInfo("ptm.oxt", "https://example.com/ptm.oxt")), null);

        var links = ReleaseInfosAnzeigen.releaseLinks(release);

        assertThat(links).containsExactly(new LinkEintrag("ptm.oxt", "https://example.com/ptm.oxt"));
    }

    @Test
    void ohneAssetsUndHtmlUrlLeer() {
        var release = new ReleaseInfo("v1.2.3", "v1.2.3", null, false, "Notes", List.of(), null);

        assertThat(ReleaseInfosAnzeigen.releaseLinks(release)).isEmpty();
    }
}
