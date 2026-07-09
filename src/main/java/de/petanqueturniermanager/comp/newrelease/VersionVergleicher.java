/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.comp.newrelease;

import java.util.Locale;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import io.github.g00fy2.versioncompare.Version;

/**
 * Reine Versions-Vergleichslogik – ohne IO, ohne Logger, ohne UNO-Abhängigkeiten.
 *
 * <p>
 * Verantwortlichkeiten:
 * <ul>
 *   <li>Normalisieren des Tag-Strings (Strip von führendem {@code v}/{@code V},
 *       Whitespace).</li>
 *   <li>Erkennen von Pre-Release-Markern ({@code -rc}, {@code -beta},
 *       {@code -alpha}, {@code -SNAPSHOT}, {@code -pre}, {@code -m},
 *       jeweils case-insensitiv).</li>
 *   <li>Vergleich der normalisierten Versionen via
 *       {@link io.github.g00fy2.versioncompare.Version}.</li>
 * </ul>
 *
 * <p>
 * Pre-Releases auf der „verfügbar"-Seite werden grundsätzlich nicht als
 * Update gemeldet – wir wollen nutzern nur stabile Releases empfehlen.
 */
public final class VersionVergleicher {

    private static final Set<String> PRE_RELEASE_MARKER = Set.of(
            "-rc", "-beta", "-alpha", "-snapshot", "-pre", "-m", "-dev");

    private VersionVergleicher() {
    }

    /**
     * Liefert {@code true}, wenn {@code verfuegbar} eine echte Update-Empfehlung
     * gegenüber {@code installiert} darstellt.
     *
     * <p>
     * Regeln:
     * <ul>
     *   <li>Ist eine der Versionen {@code null} oder leer, wird {@code false} geliefert.</li>
     *   <li>Ist {@code verfuegbar} ein Pre-Release, wird {@code false} geliefert –
     *       unabhängig vom Versionsnummer-Vergleich.</li>
     *   <li>Andernfalls wird semantisch verglichen.</li>
     * </ul>
     */
    public static boolean istNeuer(@Nullable String installiert, @Nullable String verfuegbar) {
        var normInst = normalisieren(installiert);
        var normNeu = normalisieren(verfuegbar);
        if (normInst == null || normNeu == null) {
            return false;
        }
        if (istPreRelease(normNeu)) {
            return false;
        }
        return new Version(normNeu).isHigherThan(normInst);
    }

    /**
     * Entfernt führendes {@code v} oder {@code V} und Whitespace.
     * Liefert {@code null} bei leeren/Null-Eingaben.
     */
    static @Nullable String normalisieren(@Nullable String version) {
        if (version == null) {
            return null;
        }
        var trimmed = version.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.charAt(0) == 'v' || trimmed.charAt(0) == 'V') {
            trimmed = trimmed.substring(1).trim();
        }
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Prüft, ob die (bereits normalisierte) Version einen Pre-Release-Marker enthält.
     */
    static boolean istPreRelease(String normalisierteVersion) {
        var lower = normalisierteVersion.toLowerCase(Locale.ROOT);
        for (var marker : PRE_RELEASE_MARKER) {
            if (lower.contains(marker)) {
                return true;
            }
        }
        return false;
    }
}
