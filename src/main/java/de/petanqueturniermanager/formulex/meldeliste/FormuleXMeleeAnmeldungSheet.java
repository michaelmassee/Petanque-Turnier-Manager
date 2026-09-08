/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.formulex.meldeliste;

import de.petanqueturniermanager.basesheet.meldeliste.AbstractMeleeAnmeldungSheet;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.formulex.konfiguration.FormuleXKonfigurationSheet;

/**
 * Melee-Anmeldung des FormuleX-Systems: Vorstufe zur Meldeliste, in der lose Einzelspieler
 * erfasst und vor Ort eingecheckt werden. Die Übernahme in die Meldeliste erfolgt über
 * {@link FormuleXMeleeAnmeldungUebernehmenSheet}.
 */
public class FormuleXMeleeAnmeldungSheet extends AbstractMeleeAnmeldungSheet {

	private final FormuleXKonfigurationSheet konfigurationSheet;

	public FormuleXMeleeAnmeldungSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.FORMULEX, "FormuleX-Melee-Anmeldung");
		konfigurationSheet = new FormuleXKonfigurationSheet(workingSpreadsheet);
	}

	@Override
	protected FormuleXKonfigurationSheet getKonfigurationSheet() {
		return konfigurationSheet;
	}

	@Override
	protected String getMetadatenSchluessel() {
		return SheetMetadataHelper.SCHLUESSEL_FORMULEX_MELEE_ANMELDUNG;
	}

	@Override
	protected String getSetzpositionKommentar() {
		return I18n.get("schweizer.meldeliste.comment.setzposition");
	}
}
