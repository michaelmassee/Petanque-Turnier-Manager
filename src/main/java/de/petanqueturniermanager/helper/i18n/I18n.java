/**
 * Erstellung 20.03.2026 / Michael Massee
 */
package de.petanqueturniermanager.helper.i18n;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.container.XHierarchicalNameAccess;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.helper.Lo;

/**
 * Lokalisierungs-Helfer. Liest die LibreOffice-UI-Sprache und stellt Texte auf Deutsch und Englisch bereit.
 * Fallback-Reihenfolge: aktuelle Sprache → Englisch → Schlüssel selbst.
 * Initialisierung über {@link #init(XComponentContext)} beim Start des Plugins.
 */
public final class I18n {

    private static final Logger logger = LogManager.getLogger(I18n.class);
    private static final String BUNDLE = "de/petanqueturniermanager/i18n/messages";
    private static final AtomicBoolean initialisiert = new AtomicBoolean(false);
    private static volatile ResourceBundle bundle;
    private static volatile ResourceBundle fallback;

    private I18n() {
    }

    /**
     * Einmalige Initialisierung. Ermittelt die Locale aus dem LibreOffice-Kontext und lädt das passende Bundle.
     *
     * @param xContext LibreOffice-Komponentenkontext
     */
    public static void init(XComponentContext xContext) {
        if (initialisiert.getAndSet(true)) {
            return;
        }
        var utf8 = utf8Control();
        fallback = ResourceBundle.getBundle(BUNDLE, Locale.ENGLISH, utf8);
        Locale ziel = ermittleLocale(xContext);
        bundle = Locale.ENGLISH.getLanguage().equals(ziel.getLanguage())
                ? fallback
                : getOrFallback(ziel, utf8);
        logger.info("I18n initialisiert: {}", ziel);
    }

    /**
     * Initialisiert I18n für Tests erzwungen mit einer bestimmten Locale.
     * Im Gegensatz zu {@link #init(XComponentContext)} überschreibt diese Methode
     * eine vorherige Initialisierung — ausschließlich für Testcode gedacht.
     *
     * @param locale gewünschte Locale
     */
    public static void initFuerTest(Locale locale) {
        initialisiert.set(false);
        var utf8 = utf8Control();
        fallback = ResourceBundle.getBundle(BUNDLE, Locale.ENGLISH, utf8);
        bundle = Locale.ENGLISH.getLanguage().equals(locale.getLanguage())
                ? fallback
                : getOrFallback(locale, utf8);
        initialisiert.set(true);
        logger.info("I18n (Test) initialisiert: {}", locale);
    }

    /**
     * Gibt den lokalisierten Text für den angegebenen Schlüssel zurück.
     * Ist kein Bundle geladen, wird der Schlüssel selbst zurückgegeben.
     *
     * @param key I18n-Schlüssel
     * @return lokalisierter Text oder Schlüssel als Fallback
     */
    public static String get(String key) {
        if (bundle == null) {
            return key;
        }
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            if (bundle != fallback && fallback != null) {
                try {
                    return fallback.getString(key);
                } catch (MissingResourceException ex) {
                    // ignorieren
                }
            }
            logger.warn("Fehlende Übersetzung: '{}'", key);
            return key;
        }
    }

    /**
     * Gibt den lokalisierten Text mit MessageFormat-Parametern zurück.
     *
     * @param key  I18n-Schlüssel
     * @param args Parameter für MessageFormat (z.B. Rundennummer)
     * @return formatierter lokalisierter Text
     */
    public static String get(String key, Object... args) {
        return MessageFormat.format(get(key), args);
    }

    private static ResourceBundle getOrFallback(Locale ziel, ResourceBundle.Control ctrl) {
        try {
            return ResourceBundle.getBundle(BUNDLE, ziel, ctrl);
        } catch (MissingResourceException e) {
            return fallback;
        }
    }

    private static Locale ermittleLocale(XComponentContext xContext) {
        if (xContext == null) {
            return Locale.GERMAN;
        }
        try {
            XMultiServiceFactory msf = Lo.qi(XMultiServiceFactory.class,
                    xContext.getServiceManager().createInstanceWithContext(
                            "com.sun.star.configuration.ConfigurationProvider", xContext));
            var pv = new com.sun.star.beans.PropertyValue(
                    "nodepath", 0, "/org.openoffice.Setup/L10N",
                    com.sun.star.beans.PropertyState.DIRECT_VALUE);
            Object acc = msf.createInstanceWithArguments(
                    "com.sun.star.configuration.ConfigurationAccess", new Object[] { pv });
            String ooLocale = (String) Lo.qi(XHierarchicalNameAccess.class, acc)
                    .getByHierarchicalName("ooLocale");
            return Locale.forLanguageTag(ooLocale.replace('_', '-'));
        } catch (Exception e) {
            logger.error("Locale-Ermittlung fehlgeschlagen: {}", e.getMessage(), e);
            return Locale.GERMAN;
        }
    }

    private static ResourceBundle.Control utf8Control() {
        return new ResourceBundle.Control() {
            @Override
            public ResourceBundle newBundle(String base, Locale locale, String format,
                    ClassLoader loader, boolean reload) throws java.io.IOException {
                String name = toBundleName(base, locale) + ".properties";
                var stream = loader.getResourceAsStream(name);
                if (stream == null) {
                    return null;
                }
                return new PropertyResourceBundle(new InputStreamReader(stream, StandardCharsets.UTF_8));
            }
        };
    }
}
