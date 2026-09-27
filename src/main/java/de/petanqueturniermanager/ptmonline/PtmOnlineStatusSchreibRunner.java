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
 * Schreibt das Ergebnis eines im Hintergrund gesendeten Status-Abgleichs (neue Online-Zuordnungen,
 * Ausführungsrevisionen) in das Blatt „PTMOnline Sync“ – als eigener, stiller SheetRunner, damit Blattschutz-Scope
 * und „nur ein Lauf gleichzeitig“ gelten wie bei jedem Schreibzugriff.
 */
final class PtmOnlineStatusSchreibRunner extends SheetRunner {

    private final PtmOnlineRegistrationMapping mapping;
    private final PtmOnlineStatusAbgleich.Ergebnis ergebnis;

    PtmOnlineStatusSchreibRunner(WorkingSpreadsheet ws, TurnierSystem turnierSystem,
            PtmOnlineRegistrationMapping mapping, PtmOnlineStatusAbgleich.Ergebnis ergebnis) {
        super(ws, turnierSystem, "PTM-Online");
        this.mapping = mapping;
        this.ergebnis = ergebnis;
    }

    @Override
    protected IKonfigurationSheet getKonfigurationSheet() {
        return null;
    }

    @Override
    protected void doRun() throws GenerateException {
        PtmOnlineStatusAbgleich.schreiben(mapping, ergebnis);
    }
}
