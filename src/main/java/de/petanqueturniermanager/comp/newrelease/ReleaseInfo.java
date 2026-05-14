/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.comp.newrelease;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

/**
 * Reduzierte, projektinterne Darstellung einer GitHub-Release-Antwort.
 *
 * <p>
 * Bewusst eigene Struktur (kein {@code GHRelease} aus {@code org.kohsuke.github})
 * – stabil gegen API-Änderungen der Library und sauber JSON-serialisierbar
 * für den lokalen Cache.
 *
 * @param tagName roher Tag-Name vom Release, z.B. {@code v1.2.3} oder {@code 1.2.3}.
 * @param name Anzeigename des Releases (oft identisch zum Tag).
 * @param publishedAt Zeitpunkt der Veröffentlichung; kann {@code null} sein, falls
 *                    GitHub das Feld nicht liefert (Draft-Release).
 * @param prerelease Markierung, ob es sich um ein Pre-Release handelt.
 * @param body Markdown-Release-Notes ({@code body} aus der GitHub-API); kann {@code null}/leer sein.
 * @param assets Liste der hochgeladenen Asset-Dateien (kann leer sein).
 */
public record ReleaseInfo(
        String tagName,
        String name,
        @Nullable Instant publishedAt,
        boolean prerelease,
        @Nullable String body,
        List<AssetInfo> assets) {

    public ReleaseInfo {
        Objects.requireNonNull(tagName, "tagName");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(assets, "assets");
        assets = List.copyOf(assets);
    }

    /**
     * Liefert das erste Asset, dessen Dateiname auf das gegebene Suffix endet
     * und mit dem Präfix (case-insensitiv) beginnt.
     */
    public Optional<AssetInfo> findeAsset(String prefix, String suffix) {
        Objects.requireNonNull(prefix, "prefix");
        Objects.requireNonNull(suffix, "suffix");
        var prefixLower = prefix.toLowerCase();
        var suffixLower = suffix.toLowerCase();
        return assets.stream()
                .filter(asset -> {
                    var lower = asset.name().toLowerCase();
                    return lower.startsWith(prefixLower) && lower.endsWith(suffixLower);
                })
                .findFirst();
    }
}
