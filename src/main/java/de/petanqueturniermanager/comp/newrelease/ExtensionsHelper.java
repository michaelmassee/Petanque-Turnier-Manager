/*
 * Erstellung 29.12.2019 / Michael Massee
 */
package de.petanqueturniermanager.comp.newrelease;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.jspecify.annotations.Nullable;
import org.apache.logging.log4j.Logger;

import com.sun.star.deployment.PackageInformationProvider;
import com.sun.star.deployment.XPackageInformationProvider;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.helper.StringTools;

/**
 * Wrapper um {@link XPackageInformationProvider} mit defensiver Programmierung.
 *
 * <p>
 * <b>Hinweis:</b> Reiche Logik (Vergleiche, Caching) gehört nicht hierher
 * – siehe {@link InstallierteVersion} und {@link ReleaseUpdateService}.
 */
public final class ExtensionsHelper {

    private static final Logger logger = LogManager.getLogger(ExtensionsHelper.class);

    public static final String EXTENSION_ID = InstallierteVersion.EXTENSION_ID;

    private final XComponentContext xComponentContext;

    private ExtensionsHelper(XComponentContext xComponentContext) {
        this.xComponentContext = checkNotNull(xComponentContext);
    }

    public static ExtensionsHelper from(XComponentContext xComponentContext) {
        return new ExtensionsHelper(xComponentContext);
    }

    private @Nullable XPackageInformationProvider getXPackageInformationProvider() {
        try {
            return PackageInformationProvider.get(xComponentContext);
        } catch (RuntimeException e) {
            logger.debug("XPackageInformationProvider nicht verfügbar: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Liefert die installierte Plugin-Version oder {@code null}, wenn sie nicht
     * ermittelbar ist (LibreOffice-Init noch nicht abgeschlossen, Extension nicht
     * registriert, Test ohne installiertes Plugin).
     *
     * <p>
     * Wirft keine Exception. Aufrufer müssen den Null-Fall behandeln –
     * neuere Codepfade verwenden besser {@link #versionNummerOptional()}.
     */
    public @Nullable String getVersionNummer() {
        return versionNummerOptional().orElse(null);
    }

    /**
     * Bevorzugte API für neuen Code: liefert {@link Optional#empty()} statt {@code null}.
     */
    public Optional<String> versionNummerOptional() {
        return InstallierteVersion.ermitteln(xComponentContext).map(InstallierteVersion::raw);
    }

    /**
     * Liefert das Image-URL-Verzeichnis der Extension (mit abschließendem {@code /images/}).
     * Leerer String, falls die Extension nicht gefunden wird – damit String-Konkatenationen
     * im Aufruferkontext (UI) nicht in NPE laufen.
     */
    public String getImageUrlDir() {
        var provider = getXPackageInformationProvider();
        if (provider == null) {
            return "";
        }
        try {
            var location = provider.getPackageLocation(EXTENSION_ID);
            if (location == null || location.isBlank()) {
                logger.debug("PackageLocation für {} ist leer", EXTENSION_ID);
                return "";
            }
            return StringTools.appendIfMissing(location, "/") + "images/";
        } catch (RuntimeException e) {
            logger.debug("Fehler beim Ermitteln von ImageUrlDir: {}", e.getMessage());
            return "";
        }
    }
}
