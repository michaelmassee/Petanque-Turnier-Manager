package de.petanqueturniermanager.webserver;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheets;
import com.sun.star.uno.UnoRuntime;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeKonfigurationSheet;

/**
 * Löst den Supermelee-Teilnehmer-Sheet für den <em>aktuellen</em> Spieltag auf.
 * <p>
 * Der aktive Spieltag wird zur Laufzeit aus dem {@link SuperMeleeKonfigurationSheet}
 * gelesen, sodass der Webserver immer den laufenden Spieltag anzeigt – unabhängig
 * davon, wie viele Spieltag-Sheets bereits angelegt sind.
 * <p>
 * Findet das Sheet primär über den Metadaten-Schlüssel (sprachenunabhängig).
 * Falls nicht vorhanden, versucht Fallback-Namen in verschiedenen Sprachen
 * (robust gegen Sprachenwechsel).
 */
public class SupermeleeAktiverSpieltagSheetResolver implements SheetResolver {

    private static final Logger logger = LogManager.getLogger(SupermeleeAktiverSpieltagSheetResolver.class);

    private volatile int letzterSpieltagNr = 0;

    @Override
    public Optional<XSpreadsheet> resolve(WorkingSpreadsheet ws) {
        var doc = ws.getWorkingSpreadsheetDocument();
        if (doc == null) {
            return Optional.empty();
        }
        try {
            SpielTagNr spieltagNr = new SuperMeleeKonfigurationSheet(ws).getAktiveSpieltag();
            letzterSpieltagNr = spieltagNr.getNr();
            String schluessel = SheetMetadataHelper.schluesselSupermeleeTeilnehmer(letzterSpieltagNr);

            // 1. Primär: Metadaten-Lookup (sprachenunabhängig)
            Optional<XSpreadsheet> found = SheetMetadataHelper.findeSheet(doc, schluessel);
            if (found.isPresent()) {
                logger.debug("Supermelee-Teilnehmer-Sheet {} über Metadaten gefunden", letzterSpieltagNr);
                return found;
            }

            // 2. Fallback: Sheet-Namen in verschiedenen Sprachen versuchen
            // (robust gegen Sprachenwechsel in bestehenden Dokumenten)
            var sheets = doc.getSheets();
            String[] fallbackNames = {
                    formatTeilnehmerName("{0}. Spieltag Teilnehmer", letzterSpieltagNr),        // DE
                    formatTeilnehmerName("{0}. Game day Participants", letzterSpieltagNr),      // EN
                    formatTeilnehmerName("{0}. Jour de jeu Participants", letzterSpieltagNr),   // FR
                    formatTeilnehmerName("{0}. Día de juego Participantes", letzterSpieltagNr), // ES
                    formatTeilnehmerName("{0}. Speeldag Deelnemers", letzterSpieltagNr)         // NL
            };

            for (String fallbackName : fallbackNames) {
                if (sheets.hasByName(fallbackName)) {
                    XSpreadsheet sheet = UnoRuntime.queryInterface(
                            XSpreadsheet.class,
                            sheets.getByName(fallbackName));
                    if (sheet != null) {
                        // Datei heilen: Metadaten nachschreiben
                        try {
                            SheetMetadataHelper.schreibeSheetMetadaten(doc, sheet, schluessel);
                            logger.debug("Supermelee-Teilnehmer-Sheet {} geheilt mit Fallback-Name '{}'",
                                    letzterSpieltagNr, fallbackName);
                        } catch (Exception e) {
                            logger.warn("Konnte Metadaten nicht schreiben für Sheet '{}': {}",
                                    fallbackName, e.getMessage());
                        }
                        return Optional.of(sheet);
                    }
                }
            }

            logger.warn("Supermelee-Teilnehmer-Sheet {} nicht gefunden (weder Metadaten noch Fallback-Namen)",
                    letzterSpieltagNr);
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Fehler beim Auflösen des Supermelee-Teilnehmer-Sheets", e);
            return Optional.empty();
        }
    }

    @Override
    public String getAnzeigeName() {
        return I18n.get("webserver.resolver.spieltag.teilnehmer");
    }

    @Override
    public Optional<Integer> getNummer(XSpreadsheet sheet) {
        return letzterSpieltagNr > 0 ? Optional.of(letzterSpieltagNr) : Optional.empty();
    }

    /**
     * Formatiert den Teilnehmer-Sheet-Namen mit der Spieltag-Nummer.
     */
    private String formatTeilnehmerName(String muster, int spieltagNr) {
        return new MessageFormat(muster, Locale.ROOT).format(new Object[]{spieltagNr});
    }
}
