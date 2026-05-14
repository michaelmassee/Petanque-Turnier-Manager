/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.comp.newrelease;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

/**
 * Persistenter JSON-Cache für die zuletzt von GitHub geholte
 * {@link ReleaseInfo}.
 *
 * <p>
 * Eigenschaften:
 * <ul>
 *   <li><b>Atomares Schreiben:</b> Daten landen zuerst in einer temporären
 *       Datei, dann wird mit {@link StandardCopyOption#ATOMIC_MOVE} überschrieben
 *       (Fallback {@link StandardCopyOption#REPLACE_EXISTING}, falls FS atomic move
 *       nicht unterstützt). So bleibt nie eine halb geschriebene JSON liegen.</li>
 *   <li><b>Staleness-Marker:</b> Jeder Cache-Eintrag trägt {@code cachedAt} (ISO-8601).
 *       {@link #ladeWennFrisch(Duration)} liefert nur Einträge, deren Alter unterhalb
 *       der gegebenen TTL liegt.</li>
 *   <li><b>Korruptions-Recovery:</b> Unleserliches JSON wird in
 *       {@code release.info.corrupt-&lt;timestamp&gt;} umbenannt und {@link Optional#empty()}
 *       geliefert – damit der nächste Refresh die Datei sauber neu schreibt.</li>
 *   <li>Verzeichnis wird lazy via {@link Files#createDirectories(Path, java.nio.file.attribute.FileAttribute[])}
 *       angelegt.</li>
 * </ul>
 */
public final class ReleaseCache {

    private static final Logger logger = LogManager.getLogger(ReleaseCache.class);
    private static final String DEFAULT_DATEINAME = "release.info";
    private static final String VERZEICHNIS_IM_HOME = ".petanqueturniermanager";

    private final Path cacheDatei;
    private final Gson gson;

    public ReleaseCache() {
        this(defaultPfad());
    }

    public ReleaseCache(Path cacheDatei) {
        this.cacheDatei = Objects.requireNonNull(cacheDatei, "cacheDatei");
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
                .create();
    }

    private static Path defaultPfad() {
        var home = System.getProperty("user.home");
        return Paths.get(home, VERZEICHNIS_IM_HOME, DEFAULT_DATEINAME);
    }

    /**
     * Liefert den Pfad der Cache-Datei (für Tests/Logging).
     */
    public Path pfad() {
        return cacheDatei;
    }

    private String dateiname() {
        var fileName = cacheDatei.getFileName();
        return fileName != null ? fileName.toString() : DEFAULT_DATEINAME;
    }

    /**
     * Schreibt den Cache-Eintrag atomar auf die Platte.
     * Aktueller Zeitstempel wird als {@code cachedAt} gesetzt.
     */
    public void schreibe(ReleaseInfo release) throws IOException {
        Objects.requireNonNull(release, "release");
        schreibeMitZeitstempel(release, Instant.now());
    }

    /**
     * Test-Hook: Erlaubt das Setzen eines expliziten Zeitstempels.
     */
    void schreibeMitZeitstempel(ReleaseInfo release, Instant cachedAt) throws IOException {
        Objects.requireNonNull(release, "release");
        Objects.requireNonNull(cachedAt, "cachedAt");
        var parent = cacheDatei.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        var tmp = (parent != null ? parent : Paths.get("."))
                .resolve(dateiname() + ".tmp");
        var huelle = new CacheHuelle(cachedAt, release);
        try (BufferedWriter writer = Files.newBufferedWriter(tmp)) {
            gson.toJson(huelle, CacheHuelle.class, writer);
        }
        try {
            Files.move(tmp, cacheDatei,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            logger.debug("ATOMIC_MOVE nicht unterstützt – Fallback auf REPLACE_EXISTING", e);
            Files.move(tmp, cacheDatei, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    /**
     * Liefert die im Cache liegende {@link ReleaseInfo}, falls die Datei
     * existiert, lesbar und nicht älter als {@code maxAlter} ist.
     */
    public Optional<ReleaseInfo> ladeWennFrisch(Duration maxAlter) {
        Objects.requireNonNull(maxAlter, "maxAlter");
        var huelle = laden().orElse(null);
        if (huelle == null) {
            return Optional.empty();
        }
        var alter = Duration.between(huelle.cachedAt, Instant.now());
        if (alter.isNegative() || alter.compareTo(maxAlter) <= 0) {
            return Optional.of(huelle.release);
        }
        logger.debug("Cache-Eintrag ist {} alt (max {}) – als stale verworfen", alter, maxAlter);
        return Optional.empty();
    }

    /**
     * Liefert den Cache-Inhalt ohne Frische-Check – auch alte Einträge werden
     * zurückgegeben. Nutzbar, um bei fehlgeschlagenem Online-Refresh wenigstens
     * den letzten bekannten Stand anzeigen zu können.
     */
    public Optional<ReleaseInfo> ladeUnabhaengigVomAlter() {
        return laden().map(huelle -> huelle.release);
    }

    private Optional<CacheHuelle> laden() {
        if (!(Files.exists(cacheDatei) && Files.isReadable(cacheDatei))) {
            return Optional.empty();
        }
        try (BufferedReader reader = Files.newBufferedReader(cacheDatei)) {
            var huelle = gson.fromJson(reader, CacheHuelle.class);
            if (huelle == null || huelle.cachedAt == null || huelle.release == null) {
                korruptUmbenennen("unvollstaendige-struktur");
                return Optional.empty();
            }
            return Optional.of(huelle);
        } catch (JsonSyntaxException e) {
            logger.warn("Cache-Datei {} enthält ungültiges JSON – wird als korrupt markiert", cacheDatei, e);
            korruptUmbenennen("json-syntax");
            return Optional.empty();
        } catch (IOException e) {
            logger.warn("Cache-Datei {} konnte nicht gelesen werden", cacheDatei, e);
            return Optional.empty();
        } catch (RuntimeException e) {
            // Record-Compact-Constructor wirft NullPointerException/IllegalArgumentException
            // wenn Pflichtfelder im JSON fehlen → wie Korruption behandeln.
            logger.warn("Cache-Datei {} hat unerwartete Struktur – wird als korrupt markiert", cacheDatei, e);
            korruptUmbenennen("struktur-mismatch");
            return Optional.empty();
        }
    }

    private void korruptUmbenennen(String grund) {
        var ziel = cacheDatei.resolveSibling(
                dateiname() + ".corrupt-" + Instant.now().toEpochMilli());
        try {
            Files.move(cacheDatei, ziel, StandardCopyOption.REPLACE_EXISTING);
            logger.info("Korrupte Cache-Datei nach {} verschoben (Grund: {})", ziel, grund);
        } catch (IOException ioe) {
            logger.warn("Konnte korrupte Cache-Datei {} nicht umbenennen", cacheDatei, ioe);
        }
    }

    /**
     * Hülle für die Serialisierung – trennt den Zeitstempel klar vom
     * Release-Datenmodell.
     */
    static final class CacheHuelle {
        final Instant cachedAt;
        final ReleaseInfo release;

        CacheHuelle(Instant cachedAt, ReleaseInfo release) {
            this.cachedAt = cachedAt;
            this.release = release;
        }
    }

    /**
     * Gson-TypeAdapter für {@link Instant} – schreibt/liest als ISO-8601-String.
     */
    private static final class InstantTypeAdapter extends TypeAdapter<Instant> {
        @Override
        public void write(JsonWriter out, @Nullable Instant value) throws IOException {
            if (value == null) {
                out.nullValue();
                return;
            }
            out.value(DateTimeFormatter.ISO_INSTANT.format(value));
        }

        @Override
        public @Nullable Instant read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            var raw = in.nextString();
            try {
                return Instant.parse(raw);
            } catch (DateTimeParseException e) {
                throw new JsonSyntaxException("Ungültiger Instant-Wert: " + raw, e);
            }
        }
    }
}
