/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ki;

final class KiClientFactory {

    private KiClientFactory() {
    }

    static KiClient erzeugeClient(KiOptionen optionen) {
        return switch (optionen.anbieter()) {
            case OPENAI -> new OpenAiClient(optionen);
            case GEMINI -> new GeminiClient(optionen);
            case CLAUDE -> new ClaudeClient(optionen);
        };
    }
}
