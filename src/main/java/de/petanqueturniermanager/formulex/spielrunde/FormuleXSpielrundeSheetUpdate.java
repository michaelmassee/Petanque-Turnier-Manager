/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.formulex.spielrunde;

import java.util.List;

import de.petanqueturniermanager.algorithmen.FormuleXErgebnis;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzManager;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzRegistry;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.toolbar.TurnierModus;

/**
 * Aktualisiert die aktuelle Formule X Spielrunde (Formatierung und Paarungen neu aufbauen).
 */
public class FormuleXSpielrundeSheetUpdate extends FormuleXAbstractSpielrundeSheet {

    public FormuleXSpielrundeSheetUpdate(WorkingSpreadsheet workingSpreadsheet) {
        super(workingSpreadsheet);
    }

    @Override
    protected void doRun() throws GenerateException {
        getxCalculatable().enableAutomaticCalculation(false);
        if (TurnierModus.get().istAktiv()) {
            BlattschutzRegistry.fuer(getTurnierSystem())
                    .ifPresent(k -> BlattschutzManager.get().entsperren(k, getWorkingSpreadsheet()));
        }

        SpielRundeNr aktuelleSpielrunde = getKonfigurationSheet().getAktiveSpielRunde();
        processBoxinfo("processbox.aktuelle.spielrunde", aktuelleSpielrunde.getNr());
        setSpielRundeNrInSheet(aktuelleSpielrunde);
        getMeldeListe().upDateSheet();
        TeamMeldungen aktiveMeldungen = getMeldeListe().getAktiveMeldungen();

        if (!canStart(aktiveMeldungen)) {
            if (TurnierModus.get().istAktiv()) {
                BlattschutzRegistry.fuer(getTurnierSystem())
                        .ifPresent(k -> BlattschutzManager.get().schuetzen(k, getWorkingSpreadsheet()));
            }
            return;
        }

        List<FormuleXErgebnis> ergebnisse = gespieltenRundenEinlesen(aktiveMeldungen, 1,
                aktuelleSpielrunde.getNr() - 1);

        neueSpielrunde(aktiveMeldungen, aktuelleSpielrunde, ergebnisse);

        if (TurnierModus.get().istAktiv()) {
            BlattschutzRegistry.fuer(getTurnierSystem())
                    .ifPresent(k -> BlattschutzManager.get().schuetzen(k, getWorkingSpreadsheet()));
        }
    }
}
