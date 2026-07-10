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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

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

    /**
     * Ruft alle {@link #URLS} parallel ab (unabhängige HTTP-Requests, keine gemeinsame Reihenfolge
     * noetig) statt sequenziell, damit die Gesamtlaufzeit durch den langsamsten einzelnen Request
     * begrenzt ist statt durch deren Summe.
     */
    static String laden(KiOptionen optionen) {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.min(optionen.timeoutSekunden(), 15)))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        Map<String, CompletableFuture<String>> abrufe = new LinkedHashMap<>();
        for (String url : URLS) {
            abrufe.put(url, ladeAsync(client, url, optionen));
        }
        CompletableFuture.allOf(abrufe.values().toArray(new CompletableFuture<?>[0])).join();

        StringBuilder out = new StringBuilder();
        for (CompletableFuture<String> abruf : abrufe.values()) {
            if (out.length() >= MAX_GESAMT_ZEICHEN) {
                break;
            }
            out.append(abruf.join());
        }
        if (out.length() > MAX_GESAMT_ZEICHEN) {
            return out.substring(0, MAX_GESAMT_ZEICHEN);
        }
        return out.toString();
    }

    private static CompletableFuture<String> ladeAsync(HttpClient client, String url, KiOptionen optionen) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(Math.min(optionen.timeoutSekunden(), 20)))
                .header("User-Agent", "PetanqueTurnierManager-KI")
                .GET()
                .build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        throw new CompletionException(new IOException("HTTP " + response.statusCode()));
                    }
                    String text = htmlZuText(response.body());
                    return "\nQuelle: " + url + "\n"
                            + text.substring(0, Math.min(text.length(), MAX_ZEICHEN_PRO_SEITE)) + "\n";
                })
                .exceptionally(e -> "\nQuelle nicht erreichbar: " + url + "\n");
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
