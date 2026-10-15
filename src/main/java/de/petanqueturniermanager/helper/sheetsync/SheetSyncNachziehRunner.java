/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper.sheetsync;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;

/**
 * Stiller Hintergrundlauf, der die veralteten Sync-Blätter eines Dokuments nachzieht
 * ({@link NachziehModus#NUR_VORHANDENE}). Für Aufrufer außerhalb eines SheetRunners, z.&nbsp;B. den
 * Webserver-Refresh.
 */
public final class SheetSyncNachziehRunner extends SheetRunner {

	public SheetSyncNachziehRunner(WorkingSpreadsheet ws) {
		super(ws, new DocumentPropertiesHelper(ws.getWorkingSpreadsheetDocument()).getTurnierSystemAusDocument(),
				"Sheet-Sync nachziehen");
	}

	@Override
	protected IKonfigurationSheet getKonfigurationSheet() {
		return null;
	}

	@Override
	protected void doRun() throws GenerateException {
		SheetSyncNachzieher.get().zieheNach(getWorkingSpreadsheet(), NachziehModus.NUR_VORHANDENE);
	}
}
