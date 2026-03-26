package de.petanqueturniermanager.webserver;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.uno.UnoRuntime;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;

/**
 * Löst das allgemeine Teilnehmer-Sheet (ohne Nummer) für alle Turniersysteme auf.
 * <p>
 * Wird für Schweizer, JGJ, K.-O. und andere Systeme verwendet.
 * Das Sheet existiert genau einmal pro Turnier (nicht spieltagabhängig).
 * <p>
 * Findet das Sheet primär über den Metadaten-Schlüssel (sprachenunabhängig).
 * Falls nicht vorhanden, versucht Fallback-Namen in verschiedenen Sprachen
 * (robust gegen Sprachenwechsel und alte Dokumente).
 */
public class TeilnehmerSheetResolver implements SheetResolver {

    private static final Logger logger = LogManager.getLogger(TeilnehmerSheetResolver.class);

    private final String metadatenSchluessel;
    private final String anzeigeName;
    private final String[] fallbackNamen;

    /**
     * Erstellt einen Resolver für ein Teilnehmer-Sheet.
     *
     * @param metadatenSchluessel Sprachenunabhängiger Named-Range-Schlüssel
     * @param anzeigeName         Anzeigename in der Webserver-Konfiguration
     * @param fallbackNamen       Fallback-Sheets in verschiedenen Sprachen
     *                            (z.B. "Schweizer Teilnehmer", "Schweizer Participants", etc.)
     */
    public TeilnehmerSheetResolver(String metadatenSchluessel, String anzeigeName, String[] fallbackNamen) {
        this.metadatenSchluessel = metadatenSchluessel;
        this.anzeigeName = anzeigeName;
        this.fallbackNamen = fallbackNamen != null ? fallbackNamen : new String[0];
    }

    @Override
    public Optional<XSpreadsheet> resolve(WorkingSpreadsheet ws) {
        var doc = ws.getWorkingSpreadsheetDocument();
        if (doc == null) {
            return Optional.empty();
        }
        try {
            // 1. Primär: Metadaten-Lookup (sprachenunabhängig)
            Optional<XSpreadsheet> found = SheetMetadataHelper.findeSheet(doc, metadatenSchluessel);
            if (found.isPresent()) {
                logger.debug("Teilnehmer-Sheet über Metadaten gefunden");
                return found;
            }

            // 2. Fallback: Sheet-Namen in verschiedenen Sprachen versuchen
            var sheets = doc.getSheets();
            for (String fallbackName : fallbackNamen) {
                if (sheets.hasByName(fallbackName)) {
                    XSpreadsheet sheet = UnoRuntime.queryInterface(
                            XSpreadsheet.class,
                            sheets.getByName(fallbackName));
                    if (sheet != null) {
                        // 3. Datei heilen: Metadaten nachschreiben
                        try {
                            SheetMetadataHelper.schreibeSheetMetadaten(doc, sheet, metadatenSchluessel);
                            logger.debug("Teilnehmer-Sheet geheilt mit Fallback-Name '{}'", fallbackName);
                        } catch (Exception e) {
                            logger.warn("Konnte Metadaten nicht schreiben für Teilnehmer-Sheet '{}': {}",
                                    fallbackName, e.getMessage());
                        }
                        return Optional.of(sheet);
                    }
                }
            }

            logger.warn("Teilnehmer-Sheet nicht gefunden (weder Metadaten noch Fallback-Namen)");
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Fehler beim Auflösen des Teilnehmer-Sheets", e);
            return Optional.empty();
        }
    }

    @Override
    public String getAnzeigeName() {
        return anzeigeName;
    }

    @Override
    public Optional<Integer> getNummer(XSpreadsheet sheet) {
        return Optional.empty();
    }
}

