package de.petanqueturniermanager.webserver;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeKonfigurationSheet;

/**
 * Löst die aktuelle Spielrunde vom <em>aktuellen Spieltag</em> für Supermelee auf.
 * <p>
 * Spieltag und Spielrunde werden direkt aus den Properties des
 * {@link SuperMeleeKonfigurationSheet} gelesen (Property "Spieltag" und "Spielrunde"),
 * sodass der Webserver immer exakt die aktive Spielrunde anzeigt.
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
            var konfigSheet = new SuperMeleeKonfigurationSheet(ws);
            letzterSpieltagNr = konfigSheet.getAktiveSpieltag().getNr();
            letzteRundeNr = konfigSheet.getAktiveSpielRunde().getNr();

            String schluessel = SheetMetadataHelper.schluesselSupermeleeSpielrunde(letzterSpieltagNr, letzteRundeNr);
            logger.debug("Aktuelle Spielrunde: Spieltag {}, Runde {}", letzterSpieltagNr, letzteRundeNr);
            return SheetMetadataHelper.findeSheet(doc, schluessel);
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

