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
 * Loest die Tagesrangliste des aktiven Supermelee-Spieltags auf.
 * <p>
 * Wichtig fuer Composite Views: Nach dem Wechsel auf einen neuen Spieltag darf
 * nicht die hoechste vorhandene alte Tagesrangliste stehen bleiben. Solange die
 * Rangliste des aktiven Spieltags noch nicht erzeugt wurde, muss das Panel als
 * fehlend gerendert werden.
 */
public class SupermeleeAktiveSpieltagRanglisteSheetResolver implements SheetResolver {

    private static final Logger logger = LogManager.getLogger(SupermeleeAktiveSpieltagRanglisteSheetResolver.class);

    private volatile int letzterSpieltagNr = -1;

    @Override
    public Optional<XSpreadsheet> resolve(WorkingSpreadsheet ws) {
        var doc = ws.getWorkingSpreadsheetDocument();
        if (doc == null) {
            return Optional.empty();
        }
        try {
            var konfigSheet = new SuperMeleeKonfigurationSheet(ws);
            letzterSpieltagNr = konfigSheet.getAktiveSpieltag().getNr();

            String schluessel = SheetMetadataHelper.schluesselSpieltagRangliste(letzterSpieltagNr);
            logger.debug("Aktuelle Tagesrangliste: Spieltag {}", letzterSpieltagNr);
            return SheetMetadataHelper.findeSheet(doc, schluessel);
        } catch (Exception e) {
            logger.error("Fehler beim Aufloesen der aktuellen Supermelee-Tagesrangliste", e);
            return Optional.empty();
        }
    }

    @Override
    public String getAnzeigeName() {
        return I18n.get("webserver.resolver.rangliste");
    }

    @Override
    public Optional<Integer> getNummer(XSpreadsheet sheet) {
        return letzterSpieltagNr >= 0 ? Optional.of(letzterSpieltagNr) : Optional.empty();
    }
}
