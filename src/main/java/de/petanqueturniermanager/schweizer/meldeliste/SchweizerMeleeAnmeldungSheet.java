/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.schweizer.meldeliste;

import de.petanqueturniermanager.basesheet.meldeliste.AbstractMeleeAnmeldungSheet;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerKonfigurationSheet;

/**
 * Melee-Anmeldung des Schweizer-Systems: Vorstufe zur Meldeliste, in der lose Einzelspieler
 * erfasst und vor Ort eingecheckt werden. Die Übernahme in die Meldeliste erfolgt über
 * {@link SchweizerMeleeAnmeldungUebernehmenSheet}.
 */
public class SchweizerMeleeAnmeldungSheet extends AbstractMeleeAnmeldungSheet {

	private final SchweizerKonfigurationSheet konfigurationSheet;

	public SchweizerMeleeAnmeldungSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.SCHWEIZER, "Schweizer-Melee-Anmeldung");
		konfigurationSheet = new SchweizerKonfigurationSheet(workingSpreadsheet);
	}

	@Override
	protected SchweizerKonfigurationSheet getKonfigurationSheet() {
		return konfigurationSheet;
	}

	@Override
	protected String getMetadatenSchluessel() {
		return SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_MELEE_ANMELDUNG;
	}

	@Override
	protected String getSetzpositionKommentar() {
		return I18n.get("schweizer.meldeliste.comment.setzposition");
	}
}
