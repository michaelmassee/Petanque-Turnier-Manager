/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.formulex.meldeliste;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.basesheet.meldeliste.AbstractTeilnehmerNamenCheckinListeSheet;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.formulex.konfiguration.FormuleXKonfigurationSheet;

/**
 * Checkin-Liste des Formule-X-Systems: kompakte Druckansicht zum Abhaken der Teilnehmer.
 */
public class FormuleXCheckinListeSheet extends AbstractTeilnehmerNamenCheckinListeSheet {

	private static final int MELDELISTE_ERSTE_DATEN_ZEILE = 3;

	private final FormuleXKonfigurationSheet konfigurationSheet;
	private final FormuleXMeldeListeSheetUpdate meldeliste;

	public FormuleXCheckinListeSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.FORMULEX, "Checkin Liste");
		konfigurationSheet = new FormuleXKonfigurationSheet(workingSpreadsheet);
		meldeliste = new FormuleXMeldeListeSheetUpdate(workingSpreadsheet);
	}

	@Override
	protected FormuleXKonfigurationSheet getKonfigurationSheet() {
		return konfigurationSheet;
	}

	@Override
	protected void meldelisteVorbereiten() throws GenerateException {
		meldeliste.upDateSheet();
	}

	@Override
	protected ISheet getMeldelisteSheet() {
		return meldeliste;
	}

	@Override
	protected int getMeldelisteErsteDatenZeile() {
		return MELDELISTE_ERSTE_DATEN_ZEILE;
	}

	@Override
	protected Formation getFormation() {
		return konfigurationSheet.getMeldeListeFormation();
	}

	@Override
	protected boolean istTeamnameAktiv() {
		return konfigurationSheet.isMeldeListeTeamnameAnzeigen();
	}

	@Override
	protected boolean istVereinsnameAktiv() {
		return konfigurationSheet.isMeldeListeVereinsnameAnzeigen();
	}

	@Override
	protected boolean istProTeam() {
		return false;
	}

	@Override
	protected short getSheetPos() {
		return DefaultSheetPos.KO_TURNIERBAUM;
	}

	@Override
	protected String getCheckinSheetName() {
		return SheetNamen.checkinListe();
	}

	@Override
	protected String getMetadatenSchluessel() {
		return SheetMetadataHelper.SCHLUESSEL_FORMULEX_CHECKIN_LISTE;
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return SheetMetadataHelper.findeSheetUndHeile(
				getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
				SheetMetadataHelper.SCHLUESSEL_FORMULEX_CHECKIN_LISTE,
				getCheckinSheetName());
	}

	@Override
	public final TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}
}
