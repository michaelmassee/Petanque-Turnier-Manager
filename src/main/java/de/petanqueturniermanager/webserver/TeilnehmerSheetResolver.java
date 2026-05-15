package de.petanqueturniermanager.webserver;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.uno.UnoRuntime;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeKonfigurationSheet;

/**
 * Löst das Teilnehmer-Sheet für alle Turniersysteme auf nach der Regel
 * <em>„Teilnehmer des aktuellen Spieltags"</em>.
 * <p>
 * Single-Sheet-Systeme (Schweizer, JGJ, K.-O., Maastrichter, Kaskaden,
 * Formule-X, Liga) führen genau ein Sheet mit dem Schlüssel
 * {@link SheetMetadataHelper#SCHLUESSEL_TEILNEHMER}; Supermelee führt je
 * Spieltag ein eigenes Sheet mit dem Schlüssel
 * {@link SheetMetadataHelper#schluesselSupermeleeTeilnehmer(int)}. Beides
 * wird hier abgedeckt: erst der Supermelee-Per-Spieltag-Schlüssel des
 * aktiven Spieltags (sofern das Dokument einen liefert), dann der
 * Single-Schlüssel; abschließend Fallback-Namen in mehreren Sprachen für
 * alte Dokumente ohne Metadaten.
 */
public class TeilnehmerSheetResolver implements SheetResolver {

    private static final Logger logger = LogManager.getLogger(TeilnehmerSheetResolver.class);

    private static final String[] SINGLE_FALLBACK_NAMEN = {
            "Teilnehmer",
            "Participants", "Participantes", "Deelnemers",
            "Schweizer Teilnehmer", "Schweizer Participants",
            "JGJ Teilnehmer", "JGJ Participants",
            "KO Teilnehmer", "KO Participants",
            "Maastrichter Teilnehmer", "Maastrichter Participants",
            "Kaskaden Teilnehmer", "Kaskaden Participants",
            "Ligas Teilnehmer", "Ligas Participants"
    };

    private static final String[] SPIELTAG_FALLBACK_MUSTER = {
            "{0}. Spieltag Teilnehmer",       // DE
            "{0}. Game day Participants",     // EN
            "{0}. Jour de jeu Participants",  // FR
            "{0}. Día de juego Participantes",// ES
            "{0}. Speeldag Deelnemers"        // NL
    };

    private final String anzeigeName;
    private volatile int letzterSpieltagNr;

    public TeilnehmerSheetResolver(String anzeigeName) {
        this.anzeigeName = anzeigeName;
    }

    @Override
    public Optional<XSpreadsheet> resolve(WorkingSpreadsheet ws) {
        var doc = ws.getWorkingSpreadsheetDocument();
        if (doc == null) {
            return Optional.empty();
        }
        try {
            // 1. Supermelee: per-Spieltag-Schlüssel des aktiven Spieltags
            int spieltagNr = ermittleAktivenSpieltag(ws);
            if (spieltagNr > 0) {
                String schluessel = SheetMetadataHelper.schluesselSupermeleeTeilnehmer(spieltagNr);
                Optional<XSpreadsheet> ueberMeta = SheetMetadataHelper.findeSheet(doc, schluessel);
                if (ueberMeta.isPresent()) {
                    letzterSpieltagNr = spieltagNr;
                    logger.debug("Teilnehmer-Sheet (Supermelee Spieltag {}) über Metadaten gefunden", spieltagNr);
                    return ueberMeta;
                }
                Optional<XSpreadsheet> ueberName = sucheNachFallbackNamen(doc, spieltagNr, schluessel);
                if (ueberName.isPresent()) {
                    letzterSpieltagNr = spieltagNr;
                    return ueberName;
                }
            }

            // 2. Single-Sheet-Systeme: globaler Teilnehmer-Schlüssel
            Optional<XSpreadsheet> single = SheetMetadataHelper.findeSheet(doc, SheetMetadataHelper.SCHLUESSEL_TEILNEHMER);
            if (single.isPresent()) {
                letzterSpieltagNr = 0;
                logger.debug("Teilnehmer-Sheet (Single) über Metadaten gefunden");
                return single;
            }

            // 3. Letzter Fallback: Single-Sheet über bekannte Sheet-Namen
            var sheets = doc.getSheets();
            for (String fallbackName : SINGLE_FALLBACK_NAMEN) {
                if (sheets.hasByName(fallbackName)) {
                    XSpreadsheet sheet = UnoRuntime.queryInterface(
                            XSpreadsheet.class, sheets.getByName(fallbackName));
                    if (sheet != null) {
                        try {
                            SheetMetadataHelper.schreibeSheetMetadaten(doc, sheet,
                                    SheetMetadataHelper.SCHLUESSEL_TEILNEHMER);
                            logger.debug("Teilnehmer-Sheet geheilt mit Fallback-Name '{}'", fallbackName);
                        } catch (Exception e) {
                            logger.warn("Konnte Metadaten nicht schreiben für Teilnehmer-Sheet '{}': {}",
                                    fallbackName, e.getMessage());
                        }
                        letzterSpieltagNr = 0;
                        return Optional.of(sheet);
                    }
                }
            }

            logger.warn("Teilnehmer-Sheet nicht gefunden (weder Supermelee-Spieltag {} noch Single-Schlüssel noch Fallback-Namen)",
                    spieltagNr);
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Fehler beim Auflösen des Teilnehmer-Sheets", e);
            return Optional.empty();
        }
    }

    private int ermittleAktivenSpieltag(WorkingSpreadsheet ws) {
        try {
            var nr = new SuperMeleeKonfigurationSheet(ws).getAktiveSpieltag();
            return nr != null ? nr.getNr() : 0;
        } catch (Exception e) {
            // Kein Supermelee-Konfigsheet vorhanden – Single-Sheet-Pfad versuchen.
            return 0;
        }
    }

    private Optional<XSpreadsheet> sucheNachFallbackNamen(
            com.sun.star.sheet.XSpreadsheetDocument doc, int spieltagNr, String metadatenSchluessel)
            throws com.sun.star.container.NoSuchElementException, com.sun.star.lang.WrappedTargetException {
        var sheets = doc.getSheets();
        for (String muster : SPIELTAG_FALLBACK_MUSTER) {
            String name = new MessageFormat(muster, Locale.ROOT).format(new Object[]{spieltagNr});
            if (sheets.hasByName(name)) {
                XSpreadsheet sheet = UnoRuntime.queryInterface(
                        XSpreadsheet.class, sheets.getByName(name));
                if (sheet != null) {
                    try {
                        SheetMetadataHelper.schreibeSheetMetadaten(doc, sheet, metadatenSchluessel);
                        logger.debug("Teilnehmer-Sheet (Spieltag {}) geheilt mit Fallback-Name '{}'",
                                spieltagNr, name);
                    } catch (Exception e) {
                        logger.warn("Konnte Metadaten nicht schreiben für Sheet '{}': {}",
                                name, e.getMessage());
                    }
                    return Optional.of(sheet);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public String getAnzeigeName() {
        return anzeigeName;
    }

    @Override
    public Optional<Integer> getNummer(XSpreadsheet sheet) {
        return letzterSpieltagNr > 0 ? Optional.of(letzterSpieltagNr) : Optional.empty();
    }
}
