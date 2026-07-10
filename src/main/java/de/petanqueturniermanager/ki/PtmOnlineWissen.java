/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ki;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

final class PtmOnlineWissen {

    private static final int MAX_ZEICHEN_PRO_SEITE = 6000;
    private static final int MAX_GESAMT_ZEICHEN = 18000;
    private static final List<String> URLS = List.of(
            "https://michaelmassee.github.io/Petanque-Turnier-Manager/",
            "https://github.com/michaelmassee/Petanque-Turnier-Manager/wiki",
            "https://github.com/michaelmassee/Petanque-Turnier-Manager/wiki/Makros-und-Formeln",
            "https://github.com/michaelmassee/Petanque-Turnier-Manager/wiki/Supermelee",
            "https://github.com/michaelmassee/Petanque-Turnier-Manager/wiki/Liga",
            "https://github.com/michaelmassee/Petanque-Turnier-Manager/wiki/Jeder-gegen-Jeden",
            "https://github.com/michaelmassee/Petanque-Turnier-Manager/wiki/Schweizer-System",
            "https://github.com/michaelmassee/Petanque-Turnier-Manager/wiki/KO-System",
            "https://github.com/michaelmassee/Petanque-Turnier-Manager/wiki/Maastrichter-System",
            "https://github.com/michaelmassee/Petanque-Turnier-Manager/wiki/Poule-AB",
            "https://github.com/michaelmassee/Petanque-Turnier-Manager/wiki/Kaskaden-KO",
            "https://github.com/michaelmassee/Petanque-Turnier-Manager/wiki/Formule-X",
            "https://github.com/michaelmassee/Petanque-Turnier-Manager/wiki/Trip-Tete");

    private PtmOnlineWissen() {
    }

    static String laden(KiOptionen optionen) {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.min(optionen.timeoutSekunden(), 15)))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        StringBuilder out = new StringBuilder();
        for (String url : URLS) {
            if (out.length() >= MAX_GESAMT_ZEICHEN) {
                break;
            }
            try {
                String body = lade(client, url, optionen);
                out.append("\nQuelle: ").append(url).append('\n')
                        .append(body, 0, Math.min(body.length(), MAX_ZEICHEN_PRO_SEITE))
                        .append('\n');
            } catch (IOException | InterruptedException | RuntimeException e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                out.append("\nQuelle nicht erreichbar: ").append(url).append('\n');
            }
        }
        if (out.length() > MAX_GESAMT_ZEICHEN) {
            return out.substring(0, MAX_GESAMT_ZEICHEN);
        }
        return out.toString();
    }

    private static String lade(HttpClient client, String url, KiOptionen optionen)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(Math.min(optionen.timeoutSekunden(), 20)))
                .header("User-Agent", "PetanqueTurnierManager-KI")
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("HTTP " + response.statusCode());
        }
        return htmlZuText(response.body());
    }

    private static String htmlZuText(String html) {
        return html
                .replaceAll("(?is)<script.*?</script>", " ")
                .replaceAll("(?is)<style.*?</style>", " ")
                .replaceAll("(?is)<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
