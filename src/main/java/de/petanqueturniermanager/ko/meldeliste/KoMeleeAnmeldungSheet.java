/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ko.meldeliste;

import de.petanqueturniermanager.basesheet.meldeliste.AbstractMeleeAnmeldungSheet;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.ko.konfiguration.KoKonfigurationSheet;

/**
 * Mêlée-Anmeldung des Ko-Systems: Vorstufe zur Meldeliste, in der lose Einzelspieler
 * erfasst und vor Ort eingecheckt werden. Die Übernahme in die Meldeliste erfolgt über
 * {@link KoMeleeAnmeldungUebernehmenSheet}.
 */
public class KoMeleeAnmeldungSheet extends AbstractMeleeAnmeldungSheet {

	private final KoKonfigurationSheet konfigurationSheet;

	public KoMeleeAnmeldungSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.KO, "KO-Mêlée-Anmeldung");
		konfigurationSheet = new KoKonfigurationSheet(workingSpreadsheet);
	}

	@Override
	protected KoKonfigurationSheet getKonfigurationSheet() {
		return konfigurationSheet;
	}

	@Override
	protected String getMetadatenSchluessel() {
		return SheetMetadataHelper.SCHLUESSEL_KO_MELEE_ANMELDUNG;
	}

	@Override
	protected String getSetzpositionKommentar() {
		return I18n.get("schweizer.meldeliste.comment.setzposition");
	}
}
