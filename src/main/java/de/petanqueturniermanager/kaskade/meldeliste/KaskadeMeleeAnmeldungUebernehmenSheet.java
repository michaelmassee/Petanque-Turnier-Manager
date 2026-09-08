/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.kaskade.meldeliste;

import de.petanqueturniermanager.basesheet.meldeliste.AbstractMeleeAnmeldungUebernehmenSheet;
import de.petanqueturniermanager.basesheet.meldeliste.IFormationKonfiguration;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.kaskade.konfiguration.KaskadeKonfigurationSheet;

/**
 * Übernimmt die eingecheckten Mêlée-Anmeldungen des Kaskade-Systems als Teams in die Meldeliste.
 */
public class KaskadeMeleeAnmeldungUebernehmenSheet extends AbstractMeleeAnmeldungUebernehmenSheet {

	private final KaskadeKonfigurationSheet konfigurationSheet;
	private final KaskadeMeldeListeSheetUpdate meldeliste;

	public KaskadeMeleeAnmeldungUebernehmenSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.KASKADE, "Kaskade-Mêlée-Uebernahme");
		konfigurationSheet = new KaskadeKonfigurationSheet(workingSpreadsheet);
		meldeliste = new KaskadeMeldeListeSheetUpdate(workingSpreadsheet);
	}

	@Override
	protected KaskadeKonfigurationSheet getKonfigurationSheet() {
		return konfigurationSheet;
	}

	@Override
	protected IFormationKonfiguration getFormationKonfiguration() {
		return konfigurationSheet;
	}

	@Override
	protected String getMeleeMetadatenSchluessel() {
		return SheetMetadataHelper.SCHLUESSEL_KASKADE_MELEE_ANMELDUNG;
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
