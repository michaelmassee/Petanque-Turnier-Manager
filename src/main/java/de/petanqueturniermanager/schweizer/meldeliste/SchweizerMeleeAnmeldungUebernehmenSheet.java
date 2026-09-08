/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.schweizer.meldeliste;

import de.petanqueturniermanager.basesheet.meldeliste.AbstractMeleeAnmeldungUebernehmenSheet;
import de.petanqueturniermanager.basesheet.meldeliste.IFormationKonfiguration;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerKonfigurationSheet;

/**
 * Übernimmt die eingecheckten Mêlée-Anmeldungen des Schweizer-Systems als Teams in die Meldeliste.
 */
public class SchweizerMeleeAnmeldungUebernehmenSheet extends AbstractMeleeAnmeldungUebernehmenSheet {

	private final SchweizerKonfigurationSheet konfigurationSheet;
	private final SchweizerMeldeListeSheetUpdate meldeliste;

	public SchweizerMeleeAnmeldungUebernehmenSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.SCHWEIZER, "Schweizer-Mêlée-Uebernahme");
		konfigurationSheet = new SchweizerKonfigurationSheet(workingSpreadsheet);
		meldeliste = new SchweizerMeldeListeSheetUpdate(workingSpreadsheet);
	}

	@Override
	protected SchweizerKonfigurationSheet getKonfigurationSheet() {
		return konfigurationSheet;
	}

	@Override
	protected IFormationKonfiguration getFormationKonfiguration() {
		return konfigurationSheet;
	}

	@Override
	protected String getMeleeMetadatenSchluessel() {
		return SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_MELEE_ANMELDUNG;
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
