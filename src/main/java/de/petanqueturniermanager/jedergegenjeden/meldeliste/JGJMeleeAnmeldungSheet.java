/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.jedergegenjeden.meldeliste;

import de.petanqueturniermanager.basesheet.meldeliste.AbstractMeleeAnmeldungSheet;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.jedergegenjeden.konfiguration.JGJKonfigurationSheet;

/**
 * Mêlée-Anmeldung des JGJ-Systems: Vorstufe zur Meldeliste, in der lose Einzelspieler
 * erfasst und vor Ort eingecheckt werden. Die Übernahme in die Meldeliste erfolgt über
 * {@link JGJMeleeAnmeldungUebernehmenSheet}.
 */
public class JGJMeleeAnmeldungSheet extends AbstractMeleeAnmeldungSheet {

	private final JGJKonfigurationSheet konfigurationSheet;

	public JGJMeleeAnmeldungSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.JGJ, "JGJ-Mêlée-Anmeldung");
		konfigurationSheet = new JGJKonfigurationSheet(workingSpreadsheet);
	}

	@Override
	protected JGJKonfigurationSheet getKonfigurationSheet() {
		return konfigurationSheet;
	}

	@Override
	protected String getMetadatenSchluessel() {
		return SheetMetadataHelper.SCHLUESSEL_JGJ_MELEE_ANMELDUNG;
	}

	@Override
	protected String getSetzpositionKommentar() {
		return I18n.get("schweizer.meldeliste.comment.setzposition");
	}
}
