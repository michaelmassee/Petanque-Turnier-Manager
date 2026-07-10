package de.petanqueturniermanager.ki;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class KiFehlerMeldenServiceTest {

    @Test
    void issueUrlBereitetGithubIssueMitTitelUndBodyVor() {
        String url = KiFehlerMeldenService.issueUrl(new Fehlerbericht(
                "Automatischer PTM Fehlerbericht",
                "Systeminformationen\nLogfile-Auszug"));

        assertThat(url)
                .startsWith("https://github.com/michaelmassee/Petanque-Turnier-Manager/issues/new?")
                .contains("title=Automatischer+PTM+Fehlerbericht")
                .contains("body=Systeminformationen%0ALogfile-Auszug");
    }

    @Test
    void issueUrlKuerztZuLangenBodyFuerBrowserUrl() {
        String url = KiFehlerMeldenService.issueUrl(new Fehlerbericht("Titel", "x".repeat(7000)));

        assertThat(url).contains("Bericht+wegen+Browser-URL-Laenge+gekuerzt");
    }
}
