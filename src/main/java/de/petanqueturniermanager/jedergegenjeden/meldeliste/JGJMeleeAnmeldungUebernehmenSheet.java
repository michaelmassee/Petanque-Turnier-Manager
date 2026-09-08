/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.jedergegenjeden.meldeliste;

import de.petanqueturniermanager.basesheet.meldeliste.AbstractMeleeAnmeldungUebernehmenSheet;
import de.petanqueturniermanager.basesheet.meldeliste.IFormationKonfiguration;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.jedergegenjeden.konfiguration.JGJKonfigurationSheet;

/**
 * Übernimmt die eingecheckten Mêlée-Anmeldungen des JGJ-Systems als Teams in die Meldeliste.
 */
public class JGJMeleeAnmeldungUebernehmenSheet extends AbstractMeleeAnmeldungUebernehmenSheet {

	private final JGJKonfigurationSheet konfigurationSheet;
	private final JGJMeldeListeSheet_Update meldeliste;

	public JGJMeleeAnmeldungUebernehmenSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.JGJ, "JGJ-Mêlée-Uebernahme");
		konfigurationSheet = new JGJKonfigurationSheet(workingSpreadsheet);
		meldeliste = new JGJMeldeListeSheet_Update(workingSpreadsheet);
	}

	@Override
	protected JGJKonfigurationSheet getKonfigurationSheet() {
		return konfigurationSheet;
	}

	@Override
	protected IFormationKonfiguration getFormationKonfiguration() {
		return konfigurationSheet;
	}

	@Override
	protected String getMeleeMetadatenSchluessel() {
		return SheetMetadataHelper.SCHLUESSEL_JGJ_MELEE_ANMELDUNG;
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
