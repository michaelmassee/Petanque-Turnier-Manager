/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ki;

public enum KiAnbieter {

    OPENAI("gpt-5.5", "https://api.openai.com/v1", "https://platform.openai.com/api-keys"),
    GEMINI("gemini-flash-latest", "https://generativelanguage.googleapis.com/v1beta",
            "https://aistudio.google.com/apikey"),
    CLAUDE("claude-sonnet-5", "https://api.anthropic.com", "https://console.anthropic.com/settings/keys");

    private final String defaultModel;
    private final String defaultBaseUrl;
    private final String apiKeySeite;

    KiAnbieter(String defaultModel, String defaultBaseUrl, String apiKeySeite) {
        this.defaultModel = defaultModel;
        this.defaultBaseUrl = defaultBaseUrl;
        this.apiKeySeite = apiKeySeite;
    }

    public String defaultModel() {
        return defaultModel;
    }

    public String defaultBaseUrl() {
        return defaultBaseUrl;
    }

    public String apiKeySeite() {
        return apiKeySeite;
    }
}
