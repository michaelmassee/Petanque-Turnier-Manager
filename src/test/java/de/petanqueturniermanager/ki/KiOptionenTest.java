package de.petanqueturniermanager.ki;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class KiOptionenTest {

    @Test
    void meldetFehlendenApiKeyVorKiAufruf() {
        KiOptionen optionen = new KiOptionen("", "gpt-test", "https://api.openai.com/v1", 30, true);

        assertThat(optionen.istApiVollstaendig()).isFalse();
        assertThat(optionen.apiKonfigurationsFehler()).containsExactly(KiOptionen.KonfigurationsFehler.API_KEY);
    }

    @Test
    void meldetUngueltigeBaseUrlVorKiAufruf() {
        KiOptionen optionen = new KiOptionen("sk-test", "gpt-test", "keine-url", 30, true);

        assertThat(optionen.istApiVollstaendig()).isFalse();
        assertThat(optionen.apiKonfigurationsFehler()).containsExactly(KiOptionen.KonfigurationsFehler.BASE_URL);
    }

    @Test
    void akzeptiertVollstaendigeApiKonfiguration() {
        KiOptionen optionen = new KiOptionen("sk-test", "gpt-test", "https://api.openai.com/v1", 30, true);

        assertThat(optionen.istApiVollstaendig()).isTrue();
        assertThat(optionen.apiKonfigurationsFehler()).isEmpty();
    }
}
