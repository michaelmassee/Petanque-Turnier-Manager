/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.maastrichter.meldeliste;

import de.petanqueturniermanager.basesheet.meldeliste.AbstractMeleeAnmeldungSheet;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.maastrichter.konfiguration.MaastrichterKonfigurationSheet;

/**
 * Mêlée-Anmeldung des Maastrichter-Systems: Vorstufe zur Meldeliste, in der lose Einzelspieler
 * erfasst und vor Ort eingecheckt werden. Die Übernahme in die Meldeliste erfolgt über
 * {@link MaastrichterMeleeAnmeldungUebernehmenSheet}.
 */
public class MaastrichterMeleeAnmeldungSheet extends AbstractMeleeAnmeldungSheet {

	private final MaastrichterKonfigurationSheet konfigurationSheet;

	public MaastrichterMeleeAnmeldungSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.MAASTRICHTER, "Maastrichter-Mêlée-Anmeldung");
		konfigurationSheet = new MaastrichterKonfigurationSheet(workingSpreadsheet);
	}

	@Override
	protected MaastrichterKonfigurationSheet getKonfigurationSheet() {
		return konfigurationSheet;
	}

	@Override
	protected String getMetadatenSchluessel() {
		return SheetMetadataHelper.SCHLUESSEL_MAASTRICHTER_MELEE_ANMELDUNG;
	}

	@Override
	protected String getSetzpositionKommentar() {
		return I18n.get("schweizer.meldeliste.comment.setzposition");
	}
}
