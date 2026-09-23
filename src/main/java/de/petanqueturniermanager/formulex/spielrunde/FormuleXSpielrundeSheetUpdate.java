/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.formulex.spielrunde;

import java.util.HashSet;
import java.util.Set;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.ptmonline.PtmOnlineSpielrundeSync;
import de.petanqueturniermanager.supermelee.SpielRundeNr;

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

        SpielRundeNr aktuelleSpielrunde = getKonfigurationSheet().getAktiveSpielRunde();
        processBoxinfo("processbox.aktuelle.spielrunde", aktuelleSpielrunde.getNr());
        setSpielRundeNrInSheet(aktuelleSpielrunde);
        getMeldeListe().upDateSheet();
        TeamMeldungen aktivUndAusgesetztVorSync = getMeldeListe().getAktiveUndAusgesetztMeldungen();
        Set<Integer> aktiveNrVorSync = PtmOnlineSpielrundeSync.nummern(getMeldeListe().getAktiveMeldungen());
        Set<Integer> ausgestiegeneNr = new HashSet<>(PtmOnlineSpielrundeSync.nummern(aktivUndAusgesetztVorSync));
        ausgestiegeneNr.removeAll(aktiveNrVorSync);
        PtmOnlineSpielrundeSync.abgleichen(getWorkingSpreadsheet(), getTurnierSystem(), false,
                PtmOnlineSpielrundeSync.nummern(getMeldeListe().getAlleMeldungen()), aktiveNrVorSync, ausgestiegeneNr);

        getMeldeListe().upDateSheet();
        TeamMeldungen aktiveMeldungen = getMeldeListe().getAktiveMeldungen();

        if (!canStart(aktiveMeldungen)) {
            return;
        }

        SpielrundenAkkumulation akkumulierung = gespieltenRundenEinlesen(aktiveMeldungen, 1,
                aktuelleSpielrunde.getNr() - 1);

        neueSpielrunde(aktiveMeldungen, aktuelleSpielrunde, akkumulierung);

    }
}
