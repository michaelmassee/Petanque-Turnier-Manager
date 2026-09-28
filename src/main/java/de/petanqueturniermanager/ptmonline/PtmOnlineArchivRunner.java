/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;

/**
 * Archiviert das Blatt „PTMOnline Sync“, nachdem das verbundene Online-Turnier in PTM-Online gelöscht wurde – als
 * eigener, stiller SheetRunner, damit Blattschutz-Scope und „nur ein Lauf gleichzeitig“ gelten.
 */
final class PtmOnlineArchivRunner extends SheetRunner {

    private final PtmOnlineRegistrationMapping mapping;

    PtmOnlineArchivRunner(WorkingSpreadsheet ws, TurnierSystem turnierSystem, PtmOnlineRegistrationMapping mapping) {
        super(ws, turnierSystem, "PTM-Online");
        this.mapping = mapping;
    }

    @Override
    protected IKonfigurationSheet getKonfigurationSheet() {
        return null;
    }

    @Override
    protected void doRun() throws GenerateException {
        mapping.archivieren();
    }
}
