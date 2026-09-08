/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ko.meldeliste;

import de.petanqueturniermanager.basesheet.meldeliste.AbstractMeleeAnmeldungUebernehmenSheet;
import de.petanqueturniermanager.basesheet.meldeliste.IFormationKonfiguration;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.ko.konfiguration.KoKonfigurationSheet;

/**
 * Übernimmt die eingecheckten Mêlée-Anmeldungen des Ko-Systems als Teams in die Meldeliste.
 */
public class KoMeleeAnmeldungUebernehmenSheet extends AbstractMeleeAnmeldungUebernehmenSheet {

	private final KoKonfigurationSheet konfigurationSheet;
	private final KoMeldeListeSheetUpdate meldeliste;

	public KoMeleeAnmeldungUebernehmenSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.KO, "KO-Mêlée-Uebernahme");
		konfigurationSheet = new KoKonfigurationSheet(workingSpreadsheet);
		meldeliste = new KoMeldeListeSheetUpdate(workingSpreadsheet);
	}

	@Override
	protected KoKonfigurationSheet getKonfigurationSheet() {
		return konfigurationSheet;
	}

	@Override
	protected IFormationKonfiguration getFormationKonfiguration() {
		return konfigurationSheet;
	}

	@Override
	protected String getMeleeMetadatenSchluessel() {
		return SheetMetadataHelper.SCHLUESSEL_KO_MELEE_ANMELDUNG;
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
