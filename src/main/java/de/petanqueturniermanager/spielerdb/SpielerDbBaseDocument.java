package de.petanqueturniermanager.spielerdb;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.beans.PropertyState;
import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertySet;
import com.sun.star.deployment.PackageInformationProvider;
import com.sun.star.deployment.XPackageInformationProvider;
import com.sun.star.frame.XComponentLoader;
import com.sun.star.frame.XStorable;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XSingleServiceFactory;
import com.sun.star.sdb.XDocumentDataSource;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.helper.Lo;

/**
 * Erzeugt und öffnet die {@code .odb}-Wrapper-Datei, die eine SQLite-Spieler-DB
 * per JDBC an LibreOffice Base anbindet.
 *
 * <p>Die {@code .odb} ist nur ein dünner Container für die DataSource-Metadaten
 * (URL, JDBC-Treiberklasse). Die eigentlichen Daten liegen weiterhin in der
 * SQLite-Datei. Damit kann die {@code .odb} gefahrlos neu erzeugt werden, wenn
 * sie defekt ist — User-spezifische Anpassungen wie gespeicherte Queries,
 * Reports und Forms werden aber bei einer Neuerzeugung verworfen, daher
 * gilt die Schutzregel: <b>nie automatisch überschreiben</b>.
 */
public final class SpielerDbBaseDocument {

    private static final Logger logger = LogManager.getLogger(SpielerDbBaseDocument.class);

    /**
     * URL-Prefix für LO Base. Der LO-JDBC-Connector („connectivity/jdbc")
     * registriert sich für URLs, die direkt mit {@code jdbc:} beginnen — ein
     * vorgesetztes {@code sdbc:} ist <b>kein</b> generischer Wrapper, sondern
     * Marker für native SDBC-Treiber (HSQLDB-Embedded, Firebird, dBase…).
     * Mit {@code sdbc:jdbc:sqlite:…} antwortet LO mit
     * „kein SDBC-Treiber für URL gefunden".
     */
    private static final String JDBC_URL_PREFIX = "jdbc:sqlite:";
    private static final String JDBC_DRIVER_CLASS = "org.sqlite.JDBC";
    private static final String SERVICE_DATABASE_CONTEXT = "com.sun.star.sdb.DatabaseContext";
    private static final String SERVICE_DESKTOP = "com.sun.star.frame.Desktop";
    /** Identifier aus {@code description.xml}. */
    private static final String EXTENSION_IDENTIFIER = "de.petanqueturniermanager";
    /**
     * Sub-Verzeichnis im OXT mit der Treiber-JAR speziell für LibreOffice Base
     * (Willena-Fork von sqlite-jdbc). Bewusst <i>nicht</i> {@code libs/}: das
     * Plugin verwendet weiter den schlanken xerial-Treiber, Base bekommt einen
     * separaten URLClassLoader.
     */
    private static final String BASE_DRIVER_DIR = "base-driver";
    /** Dateiname-Prefix der gebündelten Base-Treiber-JAR. */
    private static final String SQLITE_JAR_PREFIX = "sqlite-jdbc-base";

    private SpielerDbBaseDocument() {}

    /**
     * Schreibt eine neue {@code .odb} an {@code odbZiel}, die per JDBC auf
     * {@code sqliteDb} verweist. Eine eventuell bestehende Datei wird
     * überschrieben — die Aufrufer-Schicht ist für Backups verantwortlich.
     */
    public static void erzeugeOdb(Path sqliteDb, Path odbZiel, XComponentContext ctx)
            throws com.sun.star.uno.Exception {
        XMultiComponentFactory mcf = ctx.getServiceManager();
        Object dbContext = mcf.createInstanceWithContext(SERVICE_DATABASE_CONTEXT, ctx);
        XSingleServiceFactory factory = Lo.qi(XSingleServiceFactory.class, dbContext);
        if (factory == null) {
            throw new com.sun.star.uno.RuntimeException(
                    "DatabaseContext liefert keine XSingleServiceFactory");
        }
        Object dataSource = factory.createInstance();
        XPropertySet props = Lo.qi(XPropertySet.class, dataSource);
        if (props == null) {
            throw new com.sun.star.uno.RuntimeException(
                    "Neue DataSource liefert kein XPropertySet");
        }
        String jdbcUrl = JDBC_URL_PREFIX + sqliteDb.toAbsolutePath();
        props.setPropertyValue("URL", jdbcUrl);
        // JavaDriverClass redundant: einmal direkt auf der DataSource (wird von
        // neueren LO-Versionen ausgewertet) und einmal im Info-Array (für ältere
        // Builds, die das Top-Level-Property nicht kennen).
        try {
            props.setPropertyValue("JavaDriverClass", JDBC_DRIVER_CLASS);
        } catch (UnknownPropertyException e) {
            logger.debug("DataSource kennt JavaDriverClass nicht als Top-Level-Property — "
                    + "fallback nur via Info-Array");
        }
        // JavaDriverClassPath: Pfad zur sqlite-jdbc-JAR im OXT-Installationsordner.
        // Ohne diesen Eintrag findet LO Base den Treiber nicht — das gebündelte
        // JAR liegt im OXT-Classloader, nicht im LO-Java-Classpath, den der
        // JDBC-Connector („connectivity/jdbc") für Class.forName(...) nutzt.
        String classPath = ermittleSqliteTreiberClasspath(ctx);
        if (!classPath.isEmpty()) {
            try {
                props.setPropertyValue("JavaDriverClassPath", classPath);
            } catch (UnknownPropertyException e) {
                logger.debug("DataSource kennt JavaDriverClassPath nicht — nur via Info");
            }
        } else {
            logger.warn("sqlite-jdbc-JAR im OXT-Verzeichnis nicht gefunden — "
                    + "LO Base wird den Treiber org.sqlite.JDBC vermutlich nicht laden können");
        }
        PropertyValue[] info = new PropertyValue[] {
                infoProp("JavaDriverClass", JDBC_DRIVER_CLASS),
                infoProp("JavaDriverClassPath", classPath),
                infoProp("IsPasswordRequired", Boolean.FALSE)
        };
        props.setPropertyValue("Info", info);

        XDocumentDataSource docDs = Lo.qi(XDocumentDataSource.class, dataSource);
        if (docDs == null) {
            throw new com.sun.star.uno.RuntimeException(
                    "DataSource implementiert kein XDocumentDataSource");
        }
        XStorable storable = Lo.qi(XStorable.class, docDs.getDatabaseDocument());
        if (storable == null) {
            throw new com.sun.star.uno.RuntimeException(
                    "DatabaseDocument implementiert kein XStorable");
        }
        String odbUrl = odbZiel.toAbsolutePath().toUri().toString();
        storable.storeAsURL(odbUrl, new PropertyValue[0]);
        logger.info("Spieler-DB-.odb erzeugt: {} → {}", odbZiel, jdbcUrl);
    }

    /**
     * Öffnet die {@code .odb}-Datei in LibreOffice Base. Liefert das geöffnete
     * Dokument oder {@code null}, falls Base nicht verfügbar ist.
     */
    public static XComponent oeffneInBase(Path odbDatei, XComponentContext ctx)
            throws com.sun.star.uno.Exception {
        XMultiComponentFactory mcf = ctx.getServiceManager();
        Object desktop = mcf.createInstanceWithContext(SERVICE_DESKTOP, ctx);
        XComponentLoader loader = Lo.qi(XComponentLoader.class, desktop);
        if (loader == null) {
            throw new com.sun.star.uno.RuntimeException(
                    "Desktop liefert keinen XComponentLoader");
        }
        String url = odbDatei.toAbsolutePath().toUri().toString();
        return loader.loadComponentFromURL(url, "_blank", 0, new PropertyValue[0]);
    }

    /**
     * Sucht im Extension-Verzeichnis die mitgelieferte {@code sqlite-jdbc-*.jar}
     * und liefert deren {@code file:}-URL. Ergebnis ist Leerstring, wenn das
     * Verzeichnis nicht auffindbar oder leer ist — der Aufrufer muss damit
     * umgehen können (LO Base meldet dann den Treiber-Ladefehler).
     */
    private static String ermittleSqliteTreiberClasspath(XComponentContext ctx) {
        try {
            XPackageInformationProvider pip = PackageInformationProvider.get(ctx);
            String extUrl = pip.getPackageLocation(EXTENSION_IDENTIFIER);
            if (extUrl == null || extUrl.isEmpty()) {
                return "";
            }
            Path libsDir = Path.of(new URI(extUrl)).resolve(BASE_DRIVER_DIR);
            if (!Files.isDirectory(libsDir)) {
                return "";
            }
            try (Stream<Path> stream = Files.list(libsDir)) {
                return stream
                        .filter(p -> {
                            String name = p.getFileName().toString();
                            return name.startsWith(SQLITE_JAR_PREFIX) && name.endsWith(".jar");
                        })
                        .map(p -> p.toUri().toString())
                        .collect(Collectors.joining(" "));
            }
        } catch (URISyntaxException | java.io.IOException | RuntimeException e) {
            logger.warn("Sqlite-Treiber-Pfad nicht ermittelbar: {}", e.getMessage());
            return "";
        }
    }

    private static PropertyValue infoProp(String name, Object value) {
        PropertyValue pv = new PropertyValue();
        pv.Name = name;
        pv.Value = value;
        pv.State = PropertyState.DIRECT_VALUE;
        pv.Handle = -1;
        return pv;
    }
}
