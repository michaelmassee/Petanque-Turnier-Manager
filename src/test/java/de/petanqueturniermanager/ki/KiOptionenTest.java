package de.petanqueturniermanager.ki;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class KiOptionenTest {

    @Test
    void meldetFehlendenApiKeyVorKiAufruf() {
        KiOptionen optionen = openAi("", "gpt-test", "https://api.openai.com/v1", 30, true);

        assertThat(optionen.istApiVollstaendig()).isFalse();
        assertThat(optionen.apiKonfigurationsFehler()).containsExactly(KiOptionen.KonfigurationsFehler.API_KEY);
    }

    @Test
    void meldetUngueltigeBaseUrlVorKiAufruf() {
        KiOptionen optionen = openAi("sk-test", "gpt-test", "keine-url", 30, true);

        assertThat(optionen.istApiVollstaendig()).isFalse();
        assertThat(optionen.apiKonfigurationsFehler()).containsExactly(KiOptionen.KonfigurationsFehler.BASE_URL);
    }

    @Test
    void akzeptiertVollstaendigeApiKonfiguration() {
        KiOptionen optionen = openAi("sk-test", "gpt-test", "https://api.openai.com/v1", 30, true);

        assertThat(optionen.istApiVollstaendig()).isTrue();
        assertThat(optionen.apiKonfigurationsFehler()).isEmpty();
    }

    @Test
    void fehlenderAnbieterFaelltAufOpenAiZurueck() {
        KiOptionen optionen = new KiOptionen(null, "sk-test", "", "", "", "", 30, true);

        assertThat(optionen.anbieter()).isEqualTo(KiAnbieter.OPENAI);
        assertThat(optionen.model()).isEqualTo(KiAnbieter.OPENAI.defaultModel());
        assertThat(optionen.baseUrl()).isEqualTo(KiAnbieter.OPENAI.defaultBaseUrl());
    }

    @Test
    void leeresModellUndBaseUrlNutzenAnbieterspezifischeDefaults() {
        KiOptionen optionen = new KiOptionen(KiAnbieter.GEMINI, "", "gm-key", "", "", "", 30, true);

        assertThat(optionen.model()).isEqualTo(KiAnbieter.GEMINI.defaultModel());
        assertThat(optionen.baseUrl()).isEqualTo(KiAnbieter.GEMINI.defaultBaseUrl());
    }

    @Test
    void apiKeyLiestDenKeyDesAktuellenAnbieters() {
        KiOptionen optionen = new KiOptionen(
                KiAnbieter.CLAUDE, "sk-openai", "gm-gemini", "sk-ant-claude", "model", "https://example.test", 30, true);

        assertThat(optionen.apiKey()).isEqualTo("sk-ant-claude");
        assertThat(optionen.apiKeyFuer(KiAnbieter.OPENAI)).isEqualTo("sk-openai");
        assertThat(optionen.apiKeyFuer(KiAnbieter.GEMINI)).isEqualTo("gm-gemini");
    }

    @Test
    void mitApiKeyFuerAendertNurDenZielAnbieterUndLaesstAndereUnveraendert() {
        KiOptionen optionen = new KiOptionen(
                KiAnbieter.OPENAI, "sk-openai", "gm-gemini", "sk-ant-claude", "model", "https://example.test", 30, true);

        KiOptionen geaendert = optionen.mitApiKeyFuer(KiAnbieter.GEMINI, "gm-neu");

        assertThat(geaendert.apiKeyFuer(KiAnbieter.GEMINI)).isEqualTo("gm-neu");
        assertThat(geaendert.apiKeyFuer(KiAnbieter.OPENAI)).isEqualTo("sk-openai");
        assertThat(geaendert.apiKeyFuer(KiAnbieter.CLAUDE)).isEqualTo("sk-ant-claude");
    }

    private static KiOptionen openAi(String apiKey, String model, String baseUrl, int timeoutSekunden, boolean fullContext) {
        return new KiOptionen(KiAnbieter.OPENAI, apiKey, "", "", model, baseUrl, timeoutSekunden, fullContext);
    }
}
