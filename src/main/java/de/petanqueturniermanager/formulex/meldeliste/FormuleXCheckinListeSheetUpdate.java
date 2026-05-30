/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.formulex.meldeliste;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;

/**
 * Aktualisiert die Formule-X-Checkin-Liste ohne das Sheet neu zu erstellen.
 * <p>
 * Im Gegensatz zu {@link FormuleXCheckinListeSheet#generate()} (vollständiger Neuaufbau mit
 * {@code NewSheet.forceCreate()}) schreibt diese Klasse nur den Datenbereich neu. Wird vom
 * {@link de.petanqueturniermanager.helper.sheetsync.SheetSyncListener} verwendet, um bei einem
 * Tab-Wechsel zur Checkin-Liste deren Inhalt mit der Meldeliste zu synchronisieren.
 * <p>
 * Wenn die Checkin-Liste noch nicht existiert, wird der Update-Lauf still abgebrochen – das
 * initiale Anlegen erfolgt ausschließlich über den Menüpunkt, nicht über einen Listener.
 */
public class FormuleXCheckinListeSheetUpdate extends FormuleXCheckinListeSheet {

	public FormuleXCheckinListeSheetUpdate(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
	}

	@Override
	protected void doRun() throws GenerateException {
		if (getXSpreadSheet() == null) {
			return; // Erstaufbau erfolgt ausschließlich über Menü
		}
		processBoxinfo("processbox.checkin.aktualisieren");
		aktualisiereInhalt();
	}
}
