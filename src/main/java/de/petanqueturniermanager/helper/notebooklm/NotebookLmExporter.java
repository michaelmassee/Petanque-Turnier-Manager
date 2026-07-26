package de.petanqueturniermanager.helper.notebooklm;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.BrowserOeffner;
import de.petanqueturniermanager.helper.i18n.I18n;

/**
 * Baut den Dokumentations-/Quellen-Teil des NotebookLM-Exportbundles: kopiert die
 * per Gradle ({@code bundleDokumentation}) ins Extension-JAR gepackte Projektdokumentation
 * und schreibt eine Quellen.md mit Projekt- und Wikipedia-Links. Die eigentlichen
 * Turnierdaten exportiert {@code ProtocolHandler} separat über die bestehenden
 * {@code *ExportInVerzeichnis}-Klassen mit {@code ExportFormat.EIN_DOKUMENT_MD}.
 */
public final class NotebookLmExporter {

    private static final Logger logger = LogManager.getLogger(NotebookLmExporter.class);
    private static final DateTimeFormatter ORDNER_ZEITSTEMPEL = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm");

    static final String DOKU_CLASSPATH_BASIS = "de/petanqueturniermanager/doku/";
    static final String DOKU_INDEX_RESOURCE = DOKU_CLASSPATH_BASIS + "index.txt";
    static final String DOKUMENTATION_UNTERORDNER = "Dokumentation";
    static final String QUELLEN_DATEINAME = "Quellen.md";

    static final String GITHUB_REPO_URL = "https://github.com/michaelmassee/Petanque-Turnier-Manager";
    static final String GITHUB_WIKI_URL = "https://github.com/michaelmassee/Petanque-Turnier-Manager/wiki";
    static final String GEMINI_NOTEBOOK_URL = "https://notebooklm.google.com";

    private record TurniersystemQuelle(String bezeichnung, String url) {
    }

    private static final List<TurniersystemQuelle> ALLGEMEINE_TURNIERSYSTEM_QUELLEN = List.of(
            new TurniersystemQuelle("Schweizer System / Maastrichter / Arena-Pétanque",
                    "https://en.wikipedia.org/wiki/Swiss-system_tournament"),
            new TurniersystemQuelle("Liga / Jeder-gegen-Jeden (Round-robin)",
                    "https://en.wikipedia.org/wiki/Round-robin_tournament"),
            new TurniersystemQuelle("KO (Single-elimination)",
                    "https://en.wikipedia.org/wiki/Single-elimination_tournament"),
            new TurniersystemQuelle("Monrad-System / Dänisches System",
                    "https://en.wikipedia.org/wiki/Monrad_system"),
            new TurniersystemQuelle("Trip-Tête / Trio (Triplette)",
                    "https://fr.wikipedia.org/wiki/Triplette_(p%C3%A9tanque)"));

    private NotebookLmExporter() {
    }

    /**
     * Legt unterhalb von {@code basisVerzeichnis} einen neuen, zeitgestempelten
     * Export-Ordner (inkl. Dokumentation-Unterordner) an und gibt dessen Pfad zurück.
     */
    public static Path bereiteExportOrdnerVor(Path basisVerzeichnis) throws GenerateException {
        Path ordner = basisVerzeichnis.resolve("NotebookLM-Export-" + LocalDateTime.now().format(ORDNER_ZEITSTEMPEL));
        try {
            Files.createDirectories(ordner.resolve(DOKUMENTATION_UNTERORDNER));
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new GenerateException(e.getMessage());
        }
        return ordner;
    }

    /**
     * Kopiert die gebündelte Projektdokumentation und schreibt Quellen.md in {@code zielVerzeichnis}.
     * Erwartet, dass {@code zielVerzeichnis} bereits über {@link #bereiteExportOrdnerVor} angelegt wurde.
     */
    public static void exportiereDokumentationUndQuellen(Path zielVerzeichnis) throws GenerateException {
        kopiereDokumentation(zielVerzeichnis.resolve(DOKUMENTATION_UNTERORDNER));
        schreibeQuellen(zielVerzeichnis.resolve(QUELLEN_DATEINAME));
    }

    private static void kopiereDokumentation(Path zielOrdner) throws GenerateException {
        for (String relativerPfad : ladeDokuIndex()) {
            String resource = DOKU_CLASSPATH_BASIS + relativerPfad;
            try (InputStream in = NotebookLmExporter.class.getClassLoader().getResourceAsStream(resource)) {
                if (in == null) {
                    logger.warn("Dokumentations-Ressource nicht gefunden: {}", resource);
                    continue;
                }
                Path zieldateiname = Path.of(relativerPfad).getFileName();
                Files.copy(in, zielOrdner.resolve(zieldateiname), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
                throw new GenerateException(e.getMessage());
            }
        }
    }

    private static List<String> ladeDokuIndex() throws GenerateException {
        try (InputStream in = NotebookLmExporter.class.getClassLoader().getResourceAsStream(DOKU_INDEX_RESOURCE)) {
            if (in == null) {
                throw new GenerateException("Dokumentations-Index nicht gefunden: " + DOKU_INDEX_RESOURCE);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).lines()
                    .filter(zeile -> !zeile.isBlank())
                    .toList();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new GenerateException(e.getMessage());
        }
    }

    /**
     * Öffnet den Export-Ordner im Datei-Explorer/Finder des Betriebssystems. Rein
     * komfortbezogen: schlägt das fehl (z. B. {@code xdg-open} fehlt), wird nur
     * geloggt, der Export selbst gilt trotzdem als erfolgreich.
     */
    public static void oeffneExportOrdner(Path zielVerzeichnis) {
        try {
            BrowserOeffner.oeffne(zielVerzeichnis.toUri());
        } catch (IOException e) {
            logger.warn("Export-Ordner konnte nicht automatisch geöffnet werden: {}", zielVerzeichnis, e);
        }
    }

    /**
     * Öffnet Gemini Notebook (notebooklm.google.com) im Standardbrowser. Rein
     * komfortbezogen, siehe {@link #oeffneExportOrdner(Path)}.
     */
    public static void oeffneGeminiNotebookImBrowser() {
        try {
            BrowserOeffner.oeffne(GEMINI_NOTEBOOK_URL);
        } catch (IOException e) {
            logger.warn("Gemini Notebook konnte nicht automatisch im Browser geöffnet werden", e);
        }
    }

    private static void schreibeQuellen(Path zieldatei) throws GenerateException {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("# ").append(I18n.get("notebooklm.export.quellen.titel")).append("\n\n");

        sb.append("## ").append(I18n.get("notebooklm.export.quellen.projekt")).append("\n\n");
        sb.append("- GitHub: ").append(GITHUB_REPO_URL).append('\n');
        sb.append("- Wiki: ").append(GITHUB_WIKI_URL).append("\n\n");

        sb.append("## ").append(I18n.get("notebooklm.export.quellen.allgemein")).append("\n\n");
        for (var quelle : ALLGEMEINE_TURNIERSYSTEM_QUELLEN) {
            sb.append("- ").append(quelle.bezeichnung()).append(": ").append(quelle.url()).append('\n');
        }
        sb.append('\n');

        sb.append(I18n.get("notebooklm.export.quellen.hinweis")).append('\n');

        try {
            Files.writeString(zieldatei, sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new GenerateException(e.getMessage());
        }
    }
}
