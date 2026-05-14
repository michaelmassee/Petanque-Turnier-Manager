/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.comp.newrelease;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;

/**
 * Lädt die jeweils letzte GitHub-Release-Information für ein Repository.
 *
 * <p>
 * Verwendet bewusst {@link java.net.http.HttpClient} (JDK-Standard) statt der
 * {@code org.kohsuke.github}-Library für den Read-Pfad – damit sind
 * {@code connectTimeout} und {@code readTimeout} klar konfigurierbar und es
 * gibt keinerlei statischen Library-State.
 *
 * <p>
 * Der Schreib-/Download-Pfad ({@link DirectUpdate}) nutzt weiter Asset-URLs
 * – diese werden aus dem {@link ReleaseInfo}-Record gelesen.
 */
public class GithubReleaseClient {

    private static final Logger logger = LogManager.getLogger(GithubReleaseClient.class);

    private static final URI DEFAULT_BASE_URI = URI.create("https://api.github.com");
    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(10);
    private static final String USER_AGENT = "PetanqueTurnierManager-UpdateCheck";

    private final HttpClient httpClient;
    private final URI baseUri;
    private final Duration readTimeout;
    private final Gson gson;
    private final String repository;

    public GithubReleaseClient(String repository) {
        this(repository, DEFAULT_BASE_URI, DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT);
    }

    GithubReleaseClient(String repository, URI baseUri, Duration connectTimeout, Duration readTimeout) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.baseUri = Objects.requireNonNull(baseUri, "baseUri");
        this.readTimeout = Objects.requireNonNull(readTimeout, "readTimeout");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Objects.requireNonNull(connectTimeout, "connectTimeout"))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.gson = new Gson();
    }

    /**
     * Holt das {@code /releases/latest} eines Repositories.
     * Gibt {@link Optional#empty()} bei jedem nicht-{@code 200}-Status, Timeout,
     * IO-Fehler oder Parsing-Problem zurück. Alle Fehler werden geloggt –
     * der Aufrufer entscheidet über Retry.
     */
    public Optional<ReleaseInfo> ladeLetztesRelease() {
        var url = baseUri.resolve("/repos/" + repository + "/releases/latest");
        var request = HttpRequest.newBuilder(url)
                .timeout(readTimeout)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", USER_AGENT)
                .GET()
                .build();
        try {
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                logger.warn("GitHub-Release-Abruf für {} lieferte HTTP {}", repository, response.statusCode());
                return Optional.empty();
            }
            return parseAntwort(response.body());
        } catch (IOException e) {
            logger.warn("GitHub-Release-Abruf für {} schlug fehl: {}", repository, e.getMessage());
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.debug("GitHub-Release-Abruf wurde unterbrochen");
            return Optional.empty();
        }
    }

    private Optional<ReleaseInfo> parseAntwort(String json) {
        try {
            var dto = gson.fromJson(json, ReleaseDto.class);
            if (dto == null || dto.tagName == null) {
                logger.warn("GitHub-Antwort enthielt kein tag_name-Feld");
                return Optional.empty();
            }
            var name = (dto.name != null && !dto.name.isBlank()) ? dto.name : dto.tagName;
            Instant publishedAt = parseInstant(dto.publishedAt);
            List<AssetInfo> assets = new ArrayList<>();
            if (dto.assets != null) {
                for (var asset : dto.assets) {
                    if (asset != null && asset.name != null && asset.browserDownloadUrl != null) {
                        assets.add(new AssetInfo(asset.name, asset.browserDownloadUrl));
                    }
                }
            }
            return Optional.of(new ReleaseInfo(dto.tagName, name, publishedAt, dto.prerelease, dto.body, assets));
        } catch (JsonSyntaxException e) {
            logger.warn("GitHub-Antwort war kein gültiges JSON", e);
            return Optional.empty();
        }
    }

    private static @Nullable Instant parseInstant(@Nullable String iso) {
        if (iso == null || iso.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(iso);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * DTO genau in der Struktur der GitHub-API-Antwort.
     * Felder dürfen {@code null} sein – Mapping erfolgt im Caller.
     */
    private static final class ReleaseDto {
        @SerializedName("tag_name")
        @Nullable
        String tagName;
        @Nullable
        String name;
        @SerializedName("published_at")
        @Nullable
        String publishedAt;
        boolean prerelease;
        @Nullable
        String body;
        @Nullable
        List<AssetDto> assets;
    }

    private static final class AssetDto {
        @Nullable
        String name;
        @SerializedName("browser_download_url")
        @Nullable
        String browserDownloadUrl;
    }
}
