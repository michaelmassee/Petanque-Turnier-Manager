/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ki;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public record KiOptionen(
        KiAnbieter anbieter,
        String apiKeyOpenAi,
        String apiKeyGemini,
        String apiKeyClaude,
        String model,
        String baseUrl,
        int timeoutSekunden,
        boolean vollstaendigenKontextSenden) {

    public static final KiAnbieter DEFAULT_ANBIETER = KiAnbieter.OPENAI;
    public static final String DEFAULT_MODEL = DEFAULT_ANBIETER.defaultModel();
    public static final String DEFAULT_BASE_URL = DEFAULT_ANBIETER.defaultBaseUrl();
    public static final int DEFAULT_TIMEOUT_SEKUNDEN = 60;

    public enum KonfigurationsFehler {
        API_KEY,
        MODELL,
        BASE_URL,
        TIMEOUT
    }

    public KiOptionen {
        anbieter = anbieter == null ? DEFAULT_ANBIETER : anbieter;
        apiKeyOpenAi = trim(apiKeyOpenAi);
        apiKeyGemini = trim(apiKeyGemini);
        apiKeyClaude = trim(apiKeyClaude);
        model = normalisiere(model, anbieter.defaultModel());
        baseUrl = normalisiere(baseUrl, anbieter.defaultBaseUrl());
        timeoutSekunden = Math.clamp(timeoutSekunden, 5, 300);
    }

    private static String trim(String wert) {
        return wert == null ? "" : wert.trim();
    }

    private static String normalisiere(String wert, String fallback) {
        String normalisiert = trim(wert);
        return normalisiert.isEmpty() ? fallback : normalisiert;
    }

    /** API-Key des aktuell gewählten Anbieters (siehe {@link #anbieter()}). */
    public String apiKey() {
        return apiKeyFuer(anbieter);
    }

    /** API-Key des angegebenen Anbieters, unabhängig vom aktuell gewählten Anbieter. */
    public String apiKeyFuer(KiAnbieter gesuchterAnbieter) {
        return switch (gesuchterAnbieter) {
            case OPENAI -> apiKeyOpenAi;
            case GEMINI -> apiKeyGemini;
            case CLAUDE -> apiKeyClaude;
        };
    }

    /** Liefert eine Kopie mit ausgetauschtem API-Key für den angegebenen Anbieter, sonst unverändert. */
    public KiOptionen mitApiKeyFuer(KiAnbieter zielAnbieter, String apiKey) {
        return new KiOptionen(
                anbieter,
                zielAnbieter == KiAnbieter.OPENAI ? apiKey : apiKeyOpenAi,
                zielAnbieter == KiAnbieter.GEMINI ? apiKey : apiKeyGemini,
                zielAnbieter == KiAnbieter.CLAUDE ? apiKey : apiKeyClaude,
                model,
                baseUrl,
                timeoutSekunden,
                vollstaendigenKontextSenden);
    }

    public boolean istApiVollstaendig() {
        return apiKonfigurationsFehler().isEmpty();
    }

    public List<KonfigurationsFehler> apiKonfigurationsFehler() {
        List<KonfigurationsFehler> fehler = new ArrayList<>();
        if (apiKey().isBlank()) {
            fehler.add(KonfigurationsFehler.API_KEY);
        }
        if (model.isBlank()) {
            fehler.add(KonfigurationsFehler.MODELL);
        }
        if (!istGueltigeHttpUrl(baseUrl)) {
            fehler.add(KonfigurationsFehler.BASE_URL);
        }
        if (timeoutSekunden < 5 || timeoutSekunden > 300) {
            fehler.add(KonfigurationsFehler.TIMEOUT);
        }
        return List.copyOf(fehler);
    }

    private static boolean istGueltigeHttpUrl(String wert) {
        try {
            URI uri = URI.create(wert);
            String schema = uri.getScheme();
            return uri.getHost() != null
                    && ("http".equalsIgnoreCase(schema) || "https".equalsIgnoreCase(schema));
        } catch (RuntimeException e) {
            return false;
        }
    }
}
