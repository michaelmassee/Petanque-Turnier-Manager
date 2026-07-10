package de.petanqueturniermanager.ki;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;

class OpenAiClientTest {

    @Test
    void requestJsonNutztResponsesApiParameterOhneServerStorage() {
        var optionen = new KiOptionen(
                KiAnbieter.OPENAI, "sk-test", "", "", "gpt-test", "https://api.openai.com/v1", 30, true);
        var json = JsonParser.parseString(new OpenAiClient(optionen).requestJson("Hallo")).getAsJsonObject();

        assertThat(json.get("model").getAsString()).isEqualTo("gpt-test");
        assertThat(json.get("store").getAsBoolean()).isFalse();
        assertThat(json.get("input").getAsString()).isEqualTo("Hallo");
    }

    @Test
    void responseTextLiestOutputContent() {
        String body = """
                {
                  "output": [
                    {"content": [{"type": "output_text", "text": "Antwort"}]}
                  ]
                }
                """;

        assertThat(OpenAiClient.responseText(body)).isEqualTo("Antwort");
    }

    @Test
    void modelIdsLiestUndSortiertDataIds() {
        String body = """
                {
                  "data": [
                    {"id": "gpt-5.5"},
                    {"id": "gpt-4o"}
                  ]
                }
                """;

        assertThat(OpenAiClient.modelIds(body)).containsExactly("gpt-4o", "gpt-5.5");
    }
}
