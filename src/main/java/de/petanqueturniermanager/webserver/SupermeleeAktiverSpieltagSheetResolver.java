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
 * Löst den Supermelee-Teilnehmer-Sheet für den <em>aktuellen</em> Spieltag auf.
 * <p>
 * Der aktive Spieltag wird zur Laufzeit aus dem {@link SuperMeleeKonfigurationSheet}
 * gelesen, sodass der Webserver immer den laufenden Spieltag anzeigt – unabhängig
 * davon, wie viele Spieltag-Sheets bereits angelegt sind.
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
            return SheetMetadataHelper.findeSheet(doc, schluessel);
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
}
