/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ki;

public record KiOptionen(
        String apiKey,
        String model,
        String baseUrl,
        int timeoutSekunden,
        boolean vollstaendigenKontextSenden) {

    public static final String DEFAULT_MODEL = "gpt-5.6-terra";
    public static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";
    public static final int DEFAULT_TIMEOUT_SEKUNDEN = 60;

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
}
