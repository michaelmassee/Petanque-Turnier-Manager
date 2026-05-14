/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.comp.newrelease;

import java.util.Objects;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.deployment.PackageInformationProvider;
import com.sun.star.deployment.XPackageInformationProvider;
import com.sun.star.uno.XComponentContext;

/**
 * Value Object für die installierte Plugin-Version.
 *
 * <p>
 * Verkapselt das Lookup über {@link XPackageInformationProvider} defensiv:
 * Length-Checks auf jedes Sub-Array, klare Behandlung wenn die Extension
 * (noch) nicht in der Liste auftaucht (z.B. während früher Init-Phasen,
 * im Test oder wenn LO die Komponente noch nicht registriert hat).
 */
public final class InstallierteVersion {

    private static final Logger logger = LogManager.getLogger(InstallierteVersion.class);

    /** UNO-Extension-ID des Plugins. */
    public static final String EXTENSION_ID = "de.petanqueturniermanager";

    private final String raw;

    private InstallierteVersion(String raw) {
        this.raw = Objects.requireNonNull(raw, "raw");
    }

    /** Rohe Version, wie sie LibreOffice meldet (z.B. {@code 1.2.3}). */
    public String raw() {
        return raw;
    }

    /**
     * Versucht, die installierte Version aus dem LibreOffice-Extension-Manager
     * zu ermitteln. Liefert {@link Optional#empty()} wenn:
     * <ul>
     *   <li>der {@link XPackageInformationProvider} nicht verfügbar ist,</li>
     *   <li>die Extension-ID nicht in der Liste auftaucht,</li>
     *   <li>der Versions-Slot des Plugin-Eintrags leer oder nicht vorhanden ist.</li>
     * </ul>
     * Wirft keine Exception – Fehler werden auf DEBUG geloggt.
     */
    public static Optional<InstallierteVersion> ermitteln(XComponentContext context) {
        Objects.requireNonNull(context, "context");
        try {
            XPackageInformationProvider provider = PackageInformationProvider.get(context);
            if (provider == null) {
                logger.debug("XPackageInformationProvider nicht verfügbar");
                return Optional.empty();
            }
            return ausExtensionListe(provider.getExtensionList());
        } catch (RuntimeException e) {
            logger.debug("Fehler beim Ermitteln der installierten Version: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Test-Hook: trennt das reine Array-Parsing vom UNO-Provider-Lookup.
     * Defensiv gegen {@code null}-Einträge und zu kurze Sub-Arrays.
     */
    static Optional<InstallierteVersion> ausExtensionListe(String[][] extensions) {
        if (extensions == null) {
            logger.debug("Extension-Liste ist null");
            return Optional.empty();
        }
        for (String[] eintrag : extensions) {
            if (eintrag == null || eintrag.length < 2) {
                continue;
            }
            if (EXTENSION_ID.equals(eintrag[0])) {
                var versionsString = eintrag[1];
                if (versionsString == null || versionsString.isBlank()) {
                    logger.debug("Extension-Eintrag {} hat leeren Versions-Slot", EXTENSION_ID);
                    return Optional.empty();
                }
                return Optional.of(new InstallierteVersion(versionsString));
            }
        }
        logger.debug("Extension-ID {} nicht in der Liste gefunden", EXTENSION_ID);
        return Optional.empty();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof InstallierteVersion other && raw.equals(other.raw);
    }

    @Override
    public int hashCode() {
        return raw.hashCode();
    }

    @Override
    public String toString() {
        return "InstallierteVersion[" + raw + "]";
    }
}
