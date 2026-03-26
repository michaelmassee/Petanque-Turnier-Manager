package de.petanqueturniermanager.webserver;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeKonfigurationSheet;

/**
 * Löst die aktuelle Spielrunde vom <em>aktuellen Spieltag</em> für Supermelee auf.
 * <p>
 * Der aktive Spieltag wird zur Laufzeit aus dem {@link SuperMeleeKonfigurationSheet}
 * gelesen, sodass der Webserver immer die aktuelle Spielrunde des laufenden Spieltags
 * anzeigt – unabhängig davon, wie viele Spieltage bereits angelegt sind.
 * <p>
 * Innerhalb eines Spieltags wird die Spielrunde mit der höchsten Nummer ausgewählt
 * (z.B. "1.3. Spielrunde" wenn Spielrunden 1, 2, 3 existieren).
 */
public class SupermeleeAktiverSpielrundeSheetResolver implements SheetResolver {

    private static final Logger logger = LogManager.getLogger(SupermeleeAktiverSpielrundeSheetResolver.class);

    private volatile int letzterSpieltagNr = -1;
    private volatile int letzteRundeNr = -1;

    @Override
    public Optional<XSpreadsheet> resolve(WorkingSpreadsheet ws) {
        var doc = ws.getWorkingSpreadsheetDocument();
        if (doc == null) {
            return Optional.empty();
        }
        try {
            SpielTagNr spieltagNr = new SuperMeleeKonfigurationSheet(ws).getAktiveSpieltag();
            letzterSpieltagNr = spieltagNr.getNr();

            // Suche höchste Rundennummer für diesen Spieltag
            String[] schluessel = SheetMetadataHelper.getSchluesselMitPrefix(doc,
                    SheetMetadataHelper.SCHLUESSEL_SUPERMELEE_SPIELRUNDE_PREFIX);

            int hoesteRunde = -1;
            String hoesteSchluessel = null;

            for (String key : schluessel) {
                if (!key.endsWith(SheetMetadataHelper.SCHLUESSEL_SUFFIX)) {
                    continue;
                }
                // Schlüssel-Format: __PTM_SUPERMELEE_SPIELRUNDE_{spieltag}_{runde}__
                String mittelTeil = key.substring(
                        SheetMetadataHelper.SCHLUESSEL_SUPERMELEE_SPIELRUNDE_PREFIX.length(),
                        key.length() - SheetMetadataHelper.SCHLUESSEL_SUFFIX.length());

                String[] teile = mittelTeil.split("_");
                if (teile.length != 2) {
                    continue;
                }

                try {
                    int st = Integer.parseInt(teile[0]);
                    int runde = Integer.parseInt(teile[1]);

                    // Nur Spielrunden vom aktuellen Spieltag
                    if (st == letzterSpieltagNr && runde > hoesteRunde) {
                        hoesteRunde = runde;
                        hoesteSchluessel = key;
                    }
                } catch (NumberFormatException e) {
                    logger.debug("Keine gültigen Nummern in Schlüssel '{}'", key);
                }
            }

            if (hoesteSchluessel == null) {
                letzteRundeNr = -1;
                logger.debug("Keine Spielrunde für Spieltag {} gefunden", letzterSpieltagNr);
                return Optional.empty();
            }

            letzteRundeNr = hoesteRunde;
            logger.debug("Aktuelle Spielrunde: Spieltag {}, Runde {}", letzterSpieltagNr, letzteRundeNr);
            return SheetMetadataHelper.findeSheet(doc, hoesteSchluessel);
        } catch (Exception e) {
            logger.error("Fehler beim Auflösen der aktuellen Supermelee-Spielrunde", e);
            return Optional.empty();
        }
    }

    @Override
    public String getAnzeigeName() {
        return I18n.get("webserver.resolver.spieltag.aktuelle.spielrunde");
    }

    @Override
    public Optional<Integer> getNummer(XSpreadsheet sheet) {
        if (letzterSpieltagNr >= 0 && letzteRundeNr >= 0) {
            return Optional.of(letzteRundeNr);
        }
        return Optional.empty();
    }
}

