/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.formulex.meldeliste;

import de.petanqueturniermanager.basesheet.meldeliste.AbstractMeleeAnmeldungUebernehmenSheet;
import de.petanqueturniermanager.basesheet.meldeliste.IFormationKonfiguration;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.formulex.konfiguration.FormuleXKonfigurationSheet;

/**
 * Übernimmt die eingecheckten Mêlée-Anmeldungen des FormuleX-Systems als Teams in die Meldeliste.
 */
public class FormuleXMeleeAnmeldungUebernehmenSheet extends AbstractMeleeAnmeldungUebernehmenSheet {

	private final FormuleXKonfigurationSheet konfigurationSheet;
	private final FormuleXMeldeListeSheetUpdate meldeliste;

	public FormuleXMeleeAnmeldungUebernehmenSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.FORMULEX, "FormuleX-Mêlée-Uebernahme");
		konfigurationSheet = new FormuleXKonfigurationSheet(workingSpreadsheet);
		meldeliste = new FormuleXMeldeListeSheetUpdate(workingSpreadsheet);
	}

	@Override
	protected FormuleXKonfigurationSheet getKonfigurationSheet() {
		return konfigurationSheet;
	}

	@Override
	protected IFormationKonfiguration getFormationKonfiguration() {
		return konfigurationSheet;
	}

	@Override
	protected String getMeleeMetadatenSchluessel() {
		return SheetMetadataHelper.SCHLUESSEL_FORMULEX_MELEE_ANMELDUNG;
	}

	@Override
	protected ISheet getMeldeliste() {
		return meldeliste;
	}

	@Override
	protected int naechsteFreieMeldelisteZeile() throws GenerateException {
		return meldeliste.naechsteFreieDatenZeileInSpielerNrSpalte();
	}

	@Override
	protected void meldelisteAktualisieren() throws GenerateException {
		meldeliste.upDateSheet();
	}
}
