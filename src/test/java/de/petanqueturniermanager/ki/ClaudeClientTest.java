package de.petanqueturniermanager.ki;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;

class ClaudeClientTest {

    @Test
    void requestJsonNutztMessagesApiStrukturMitMaxTokens() {
        var optionen = new KiOptionen(
                KiAnbieter.CLAUDE, "", "", "sk-ant-test", "claude-test", "https://api.anthropic.com", 30, true);
        var json = JsonParser.parseString(new ClaudeClient(optionen).requestJson("Hallo")).getAsJsonObject();

        assertThat(json.get("model").getAsString()).isEqualTo("claude-test");
        assertThat(json.get("max_tokens").getAsInt()).isPositive();
        var message = json.getAsJsonArray("messages").get(0).getAsJsonObject();
        assertThat(message.get("role").getAsString()).isEqualTo("user");
        assertThat(message.get("content").getAsString()).isEqualTo("Hallo");
    }

    @Test
    void responseTextLiestContentTextBloecke() {
        String body = """
                {
                  "content": [
                    {"type": "text", "text": "Antwort"}
                  ]
                }
                """;

        assertThat(ClaudeClient.responseText(body)).isEqualTo("Antwort");
    }

    @Test
    void modelIdsLiestUndSortiertDataIds() {
        String body = """
                {
                  "data": [
                    {"id": "claude-sonnet-5"},
                    {"id": "claude-haiku-4-5"}
                  ]
                }
                """;

        assertThat(ClaudeClient.modelIds(body)).containsExactly("claude-haiku-4-5", "claude-sonnet-5");
    }
}
