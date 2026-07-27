package de.petanqueturniermanager.helper.notebooklm;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.petanqueturniermanager.basesheet.konfiguration.KonfigurationSingleton;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.comp.newrelease.ExtensionsHelper;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.BrowserOeffner;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.konfigdialog.ConfigProperty;

/**
 * Baut den Quellen-Teil des NotebookLM-Exportbundles: schreibt eine Quellen.md
 * mit Links auf das Projekt-Wiki und allgemeine Turniersystem-Quellen. Die
 * eigentlichen Turnierdaten exportiert {@code ProtocolHandler} separat über die
 * bestehenden {@code *ExportInVerzeichnis}-Klassen mit {@code ExportFormat.EIN_DOKUMENT_MD}.
 */
public final class NotebookLmExporter {

    private static final Logger logger = LogManager.getLogger(NotebookLmExporter.class);
    private static final DateTimeFormatter ORDNER_ZEITSTEMPEL = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss");

    static final String QUELLEN_DATEINAME = "Quellen.md";
    static final String SYSTEM_INFO_DATEINAME = "System-Info.md";

    static final String GITHUB_REPO_URL = "https://github.com/michaelmassee/Petanque-Turnier-Manager";
    static final String GITHUB_WIKI_URL = "https://github.com/michaelmassee/Petanque-Turnier-Manager/wiki";
    static final String GEMINI_NOTEBOOK_URL = "https://notebooklm.google.com";

    private record Quelle(String bezeichnung, String url) {
    }

    private static final List<Quelle> ALLGEMEINE_TURNIERSYSTEM_QUELLEN = List.of(
            new Quelle("Schweizer System / Maastrichter / Arena-Pétanque",
                    "https://en.wikipedia.org/wiki/Swiss-system_tournament"),
            new Quelle("Liga / Jeder-gegen-Jeden (Round-robin)",
                    "https://en.wikipedia.org/wiki/Round-robin_tournament"),
            new Quelle("KO (Single-elimination)",
                    "https://en.wikipedia.org/wiki/Single-elimination_tournament"),
            new Quelle("Monrad-System / Dänisches System",
                    "https://en.wikipedia.org/wiki/Monrad_system"),
            new Quelle("Trip-Tête / Trio (Triplette)",
                    "https://fr.wikipedia.org/wiki/Triplette_(p%C3%A9tanque)"));

    private static final List<Quelle> LIBREOFFICE_CALC_QUELLEN = List.of(
            new Quelle("Anwender – Calc-Hilfe (Online)",
                    "https://help.libreoffice.org/latest/de/text/scalc/main0000.html"),
            new Quelle("Anwender – Calc-Handbuch",
                    "https://documentation.libreoffice.org/en/english-documentation/calc/"),
            new Quelle("Anwender – Community-Forum (Ask LibreOffice)", "https://ask.libreoffice.org/"),
            new Quelle("Makros – Basic-IDE & Basic-Hilfe",
                    "https://help.libreoffice.org/latest/de/text/sbasic/shared/main0601.html"),
            new Quelle("Makros – BASIC Guide (Wiki)", "https://wiki.documentfoundation.org/Documentation/BASIC_Guide"),
            new Quelle("Makros – Calc-Funktionen im Detail",
                    "https://help.libreoffice.org/latest/de/text/scalc/01/04060100.html"),
            new Quelle("Entwicklung – Developer's Guide", "https://wiki.documentfoundation.org/Documentation/DevGuide"),
            new Quelle("Entwicklung – UNO API-Referenz", "https://api.libreoffice.org/"),
            new Quelle("Entwicklung – LibreOffice-Quellcode", "https://github.com/LibreOffice/core"));

    private NotebookLmExporter() {
    }

    /**
     * Legt unterhalb von {@code basisVerzeichnis} einen neuen, zeitgestempelten
     * Export-Ordner an und gibt dessen Pfad zurück.
     */
    public static Path bereiteExportOrdnerVor(Path basisVerzeichnis) throws GenerateException {
        String zeitstempel = LocalDateTime.now().format(ORDNER_ZEITSTEMPEL);
        Path ordner = basisVerzeichnis.resolve("NotebookLM-Export-" + zeitstempel);
        for (int suffix = 2; Files.exists(ordner); suffix++) {
            // Zwei Exporte innerhalb derselben Sekunde: eindeutigen Ordner statt stillem
            // Überschreiben von Quellen.md/Turnierdaten des ersten Laufs.
            ordner = basisVerzeichnis.resolve("NotebookLM-Export-" + zeitstempel + "-" + suffix);
        }
        try {
            Files.createDirectories(ordner);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new GenerateException(e.getMessage());
        }
        return ordner;
    }

    /**
     * Schreibt Quellen.md in {@code zielVerzeichnis}. Erwartet, dass {@code zielVerzeichnis}
     * bereits über {@link #bereiteExportOrdnerVor} angelegt wurde.
     */
    public static void exportiereQuellen(Path zielVerzeichnis) throws GenerateException {
        schreibeQuellen(zielVerzeichnis.resolve(QUELLEN_DATEINAME));
    }

    /**
     * Schreibt System-Info.md (Plugin-/Systeminformationen sowie alle aktuellen Turnier-Properties
     * des Dokuments) in {@code zielVerzeichnis}. Erwartet, dass {@code zielVerzeichnis} bereits über
     * {@link #bereiteExportOrdnerVor} angelegt wurde.
     */
    public static void exportiereSystemInfo(Path zielVerzeichnis, WorkingSpreadsheet ws) throws GenerateException {
        schreibeSystemInfo(zielVerzeichnis.resolve(SYSTEM_INFO_DATEINAME), ws);
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

        sb.append("## ").append(I18n.get("notebooklm.export.quellen.libreoffice")).append("\n\n");
        for (var quelle : LIBREOFFICE_CALC_QUELLEN) {
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

    private static void schreibeSystemInfo(Path zieldatei, WorkingSpreadsheet ws) throws GenerateException {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("# ").append(I18n.get("notebooklm.export.systeminfo.titel")).append("\n\n");

        sb.append("## ").append(I18n.get("notebooklm.export.systeminfo.plugin")).append("\n\n");
        sb.append("- PTM-Version: ").append(ermittleVersion(ws)).append('\n');
        sb.append("- Java: ").append(System.getProperty("java.version")).append(" (")
                .append(System.getProperty("java.vendor")).append(")\n");
        sb.append("- Betriebssystem: ").append(System.getProperty("os.name")).append(' ')
                .append(System.getProperty("os.version")).append(" (").append(System.getProperty("os.arch"))
                .append(")\n");
        sb.append("- Prozessoren: ").append(Runtime.getRuntime().availableProcessors()).append('\n');
        sb.append("- Max. Speicher (MB): ").append(Runtime.getRuntime().maxMemory() / (1024 * 1024)).append("\n\n");

        TurnierSystem turnierSystem = new DocumentPropertiesHelper(ws).getTurnierSystemAusDocument();
        sb.append("## ").append(I18n.get("notebooklm.export.systeminfo.turnier")).append("\n\n");
        sb.append("- Turniersystem: ").append(turnierSystem.getBezeichnung()).append("\n\n");

        sb.append("## ").append(I18n.get("notebooklm.export.systeminfo.eigenschaften")).append("\n\n");
        List<ConfigProperty<?>> konfigProperties = KonfigurationSingleton.getKonfigProperties(ws);
        if (konfigProperties == null || konfigProperties.isEmpty()) {
            sb.append(I18n.get("notebooklm.export.systeminfo.keine.eigenschaften")).append('\n');
        } else {
            DocumentPropertiesHelper docPropHelper = new DocumentPropertiesHelper(ws);
            for (ConfigProperty<?> konfigProperty : konfigProperties) {
                if (konfigProperty.isIntern()) {
                    continue;
                }
                sb.append("- ").append(konfigProperty.getKey()).append(": ")
                        .append(aktuellerWert(konfigProperty, docPropHelper)).append('\n');
            }
        }

        try {
            Files.writeString(zieldatei, sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new GenerateException(e.getMessage());
        }
    }

    private static String ermittleVersion(WorkingSpreadsheet ws) {
        try {
            return ExtensionsHelper.from(ws.getxContext()).versionNummerOptional().orElse("unbekannt");
        } catch (RuntimeException e) {
            logger.warn("PTM-Version konnte nicht ermittelt werden", e);
            return "unbekannt";
        }
    }

    private static String aktuellerWert(ConfigProperty<?> konfigProperty, DocumentPropertiesHelper docPropHelper) {
        return switch (konfigProperty.getType()) {
            case BOOLEAN -> Boolean.toString(
                    docPropHelper.getBooleanProperty(konfigProperty.getKey(), (Boolean) konfigProperty.getDefaultVal()));
            case INTEGER -> Integer.toString(
                    docPropHelper.getIntProperty(konfigProperty.getKey(), (Integer) konfigProperty.getDefaultVal()));
            case COLOR -> String.format("#%06X",
                    docPropHelper.getIntProperty(konfigProperty.getKey(), (Integer) konfigProperty.getDefaultVal()));
            case STRING -> docPropHelper.getStringProperty(konfigProperty.getKey(), (String) konfigProperty.getDefaultVal());
        };
    }
}
