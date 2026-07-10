/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ki;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public record KiOptionen(
        String apiKey,
        String model,
        String baseUrl,
        int timeoutSekunden,
        boolean vollstaendigenKontextSenden) {

    public static final String DEFAULT_MODEL = "gpt-5.6-terra";
    public static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";
    public static final int DEFAULT_TIMEOUT_SEKUNDEN = 60;

    public enum KonfigurationsFehler {
        API_KEY,
        MODELL,
        BASE_URL,
        TIMEOUT
    }

    public KiOptionen {
        apiKey = apiKey == null ? "" : apiKey.trim();
        model = normalisiere(model, DEFAULT_MODEL);
        baseUrl = normalisiere(baseUrl, DEFAULT_BASE_URL);
        timeoutSekunden = Math.clamp(timeoutSekunden, 5, 300);
    }

    private static String normalisiere(String wert, String fallback) {
        String normalisiert = wert == null ? "" : wert.trim();
        return normalisiert.isEmpty() ? fallback : normalisiert;
    }

    public boolean istApiVollstaendig() {
        return apiKonfigurationsFehler().isEmpty();
    }

    public List<KonfigurationsFehler> apiKonfigurationsFehler() {
        List<KonfigurationsFehler> fehler = new ArrayList<>();
        if (apiKey.isBlank()) {
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
