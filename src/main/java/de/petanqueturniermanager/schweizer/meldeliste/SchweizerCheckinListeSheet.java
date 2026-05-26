/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.schweizer.meldeliste;

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
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerKonfigurationSheet;

/**
 * Checkin-Liste des Schweizer-Systems: kompakte Druckansicht zum Abhaken der Teams.
 */
public class SchweizerCheckinListeSheet extends AbstractTeilnehmerNamenCheckinListeSheet {

	private static final int MELDELISTE_ERSTE_DATEN_ZEILE = 3;

	private final SchweizerKonfigurationSheet konfigurationSheet;
	private final SchweizerMeldeListeSheetUpdate meldeliste;

	public SchweizerCheckinListeSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.SCHWEIZER, "Checkin Liste");
		konfigurationSheet = new SchweizerKonfigurationSheet(workingSpreadsheet);
		meldeliste = new SchweizerMeldeListeSheetUpdate(workingSpreadsheet);
	}

	@Override
	protected SchweizerKonfigurationSheet getKonfigurationSheet() {
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
		return true;
	}

	@Override
	protected short getSheetPos() {
		return DefaultSheetPos.SCHWEIZER_WORK;
	}

	@Override
	protected String getCheckinSheetName() {
		return SheetNamen.checkinListe();
	}

	@Override
	protected String getMetadatenSchluessel() {
		return SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_CHECKIN_LISTE;
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return SheetMetadataHelper.findeSheetUndHeile(
				getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
				SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_CHECKIN_LISTE,
				getCheckinSheetName());
	}

	@Override
	public final TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}
}
