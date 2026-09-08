/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.poule.meldeliste;

import de.petanqueturniermanager.basesheet.meldeliste.AbstractMeleeAnmeldungSheet;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.poule.konfiguration.PouleKonfigurationSheet;

/**
 * Melee-Anmeldung des Poule-Systems: Vorstufe zur Meldeliste, in der lose Einzelspieler
 * erfasst und vor Ort eingecheckt werden. Die Übernahme in die Meldeliste erfolgt über
 * {@link PouleMeleeAnmeldungUebernehmenSheet}.
 */
public class PouleMeleeAnmeldungSheet extends AbstractMeleeAnmeldungSheet {

	private final PouleKonfigurationSheet konfigurationSheet;

	public PouleMeleeAnmeldungSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.POULE, "Poule-Melee-Anmeldung");
		konfigurationSheet = new PouleKonfigurationSheet(workingSpreadsheet);
	}

	@Override
	protected PouleKonfigurationSheet getKonfigurationSheet() {
		return konfigurationSheet;
	}

	@Override
	protected String getMetadatenSchluessel() {
		return SheetMetadataHelper.SCHLUESSEL_POULE_MELEE_ANMELDUNG;
	}

	@Override
	protected String getSetzpositionKommentar() {
		return I18n.get("poule.meldeliste.comment.setzposition");
	}
}
