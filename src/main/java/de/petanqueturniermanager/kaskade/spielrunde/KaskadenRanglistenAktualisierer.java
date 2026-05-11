/*
 * Erstellung: 2026-05-11 / Michael Massee
 */
package de.petanqueturniermanager.kaskade.spielrunde;

import static com.google.common.base.Preconditions.checkNotNull;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.rangliste.IRanglistenAktualisierer;
import de.petanqueturniermanager.helper.sheet.SheetHelper;

/**
 * Aktualisierer-Strategie für das Kaskaden-System: refresht die
 * {@link KaskadeGruppenRanglisteSheet}, falls sie bereits angelegt wurde.
 * Wird vom Hook in {@link KaskadeSpielrundeSheet#doRun()} vor dem Anlegen
 * der nächsten Kaskadenrunde aufgerufen.
 */
public class KaskadenRanglistenAktualisierer implements IRanglistenAktualisierer {

	private final WorkingSpreadsheet workingSpreadsheet;
	private final SheetHelper sheetHelper;

	public KaskadenRanglistenAktualisierer(WorkingSpreadsheet workingSpreadsheet, SheetHelper sheetHelper) {
		this.workingSpreadsheet = checkNotNull(workingSpreadsheet);
		this.sheetHelper = checkNotNull(sheetHelper);
	}

	@Override
	public void aktualisiereRanglisten() throws GenerateException {
		String name = SheetNamen.kaskadeGruppenrangliste();
		if (sheetHelper.findByName(name) != null) {
			new KaskadeGruppenRanglisteSheetUpdate(workingSpreadsheet).doRun();
		}
	}
}
