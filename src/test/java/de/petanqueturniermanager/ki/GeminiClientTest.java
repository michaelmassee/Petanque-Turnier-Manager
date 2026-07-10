package de.petanqueturniermanager.ki;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;

class GeminiClientTest {

    @Test
    void requestJsonNutztGenerateContentContentsStruktur() {
        var optionen = new KiOptionen(
                KiAnbieter.GEMINI, "", "gm-test", "", "gemini-test",
                "https://generativelanguage.googleapis.com/v1beta", 30, true);
        var json = JsonParser.parseString(new GeminiClient(optionen).requestJson("Hallo")).getAsJsonObject();

        var parts = json.getAsJsonArray("contents").get(0).getAsJsonObject().getAsJsonArray("parts");
        assertThat(parts.get(0).getAsJsonObject().get("text").getAsString()).isEqualTo("Hallo");
    }

    @Test
    void responseTextLiestCandidatesContentParts() {
        String body = """
                {
                  "candidates": [
                    {"content": {"parts": [{"text": "Antwort"}]}}
                  ]
                }
                """;

        assertThat(GeminiClient.responseText(body)).isEqualTo("Antwort");
    }

    @Test
    void modelIdsFiltertAufGenerateContentUndEntferntPrefix() {
        String body = """
                {
                  "models": [
                    {"name": "models/gemini-flash-latest", "supportedGenerationMethods": ["generateContent"]},
                    {"name": "models/text-embedding-004", "supportedGenerationMethods": ["embedContent"]}
                  ]
                }
                """;

        assertThat(GeminiClient.modelIds(body)).containsExactly("gemini-flash-latest");
    }
}
