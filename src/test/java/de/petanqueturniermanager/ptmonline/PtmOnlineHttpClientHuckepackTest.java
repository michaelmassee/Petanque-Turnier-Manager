/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/** Huckepack-Header der Sync-Antworten von PTM-Online. */
class PtmOnlineHttpClientHuckepackTest {

    @Test
    void liestDenTurnierstatus() {
        assertThat(PtmOnlineHttpClient.turnierStatus(antwortMit(Map.of("X-PTM-Tournament-Status", List.of(" registration ")))))
                .isEqualTo("registration");
    }

    @Test
    void ohneOderMitLeeremHeaderKeinStatus() {
        assertThat(PtmOnlineHttpClient.turnierStatus(antwortMit(Map.of()))).as("älterer Server").isNull();
        assertThat(PtmOnlineHttpClient.turnierStatus(antwortMit(Map.of("X-PTM-Tournament-Status", List.of(" ")))))
                .isNull();
    }

    private static HttpResponse<?> antwortMit(Map<String, List<String>> header) {
        HttpResponse<?> antwort = mock(HttpResponse.class);
        when(antwort.headers()).thenReturn(HttpHeaders.of(header, (name, wert) -> true));
        return antwort;
    }
}
