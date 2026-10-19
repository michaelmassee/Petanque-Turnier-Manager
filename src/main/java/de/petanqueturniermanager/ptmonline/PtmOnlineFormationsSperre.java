/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.onlinesync.SpieltagKontext;

/**
 * Ist das Dokument mit einem Online-Turnier verbunden und sind ihm bereits Anmeldungen zugeordnet, bleibt die
 * Formation fest – die Anmeldungen passen nur zur Teamgröße, unter der sie abgegeben wurden. PTM-Online sperrt die
 * Formation in diesem Fall ebenso. Die Prüfung arbeitet nur mit dem Blatt „PTMOnline Sync“, also auch offline.
 */
public final class PtmOnlineFormationsSperre {

    private static final Logger logger = LogManager.getLogger(PtmOnlineFormationsSperre.class);

    private PtmOnlineFormationsSperre() {}

    /** Ob das Dokument (bzw. der aktive Spieltag) verbunden ist und ihm Anmeldungen zugeordnet sind. */
    public static boolean istGesperrt(WorkingSpreadsheet ws, TurnierSystem turnierSystem) throws GenerateException {
        if (new DocumentPropertiesHelper(ws).getTurnierSystemAusDocument() != turnierSystem) {
            return false;
        }
        PtmOnlineRegistrationMapping mapping = new PtmOnlineRegistrationMapping(ws, turnierSystem,
                SpieltagKontext.aktiverSpieltagOderNull(ws, turnierSystem));
        return mapping.getTournamentId().isPresent() && mapping.hatZuordnungen();
    }

    /**
     * Für den Konfigurationsdialog: bedienbar, solange nicht gesperrt. Lässt sich der Zustand nicht lesen, bleibt die
     * Einstellung bedienbar – PTM-Online lehnt eine unzulässige Änderung dann selbst ab.
     */
    public static boolean istAenderbar(WorkingSpreadsheet ws, TurnierSystem turnierSystem) {
        try {
            return !istGesperrt(ws, turnierSystem);
        } catch (GenerateException e) {
            logger.error("PTM-Online: Formationssperre konnte nicht ermittelt werden", e);
            return true;
        }
    }

    /**
     * Bricht ab, wenn die Formation gesperrt ist und {@code neueFormation} von der bisherigen abweicht. Aufzurufen,
     * bevor das Dokument geändert wird.
     */
    public static void pruefeAenderung(WorkingSpreadsheet ws, TurnierSystem turnierSystem, Formation neueFormation)
            throws GenerateException {
        if (!istGesperrt(ws, turnierSystem)) {
            return;
        }
        Optional<Formation> bisherige = PtmOnlineFormation.ausDokument(ws, turnierSystem);
        if (bisherige.isPresent() && bisherige.get() != neueFormation) {
            throw new GenerateException(I18n.get("ptmonline.formation.gesperrt", bisherige.get().getBezeichnung()));
        }
    }
}
