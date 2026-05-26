/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.poule.meldeliste;

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
import de.petanqueturniermanager.poule.konfiguration.PouleKonfigurationSheet;

/**
 * Checkin-Liste des Poule-Systems: kompakte Druckansicht zum Abhaken der Teams.
 */
public class PouleCheckinListeSheet extends AbstractTeilnehmerNamenCheckinListeSheet {

	private static final int MELDELISTE_ERSTE_DATEN_ZEILE = 3;

	private final PouleKonfigurationSheet konfigurationSheet;
	private final PouleMeldeListeSheetUpdate meldeliste;

	public PouleCheckinListeSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.POULE, "Checkin Liste");
		konfigurationSheet = new PouleKonfigurationSheet(workingSpreadsheet);
		meldeliste = new PouleMeldeListeSheetUpdate(workingSpreadsheet);
	}

	@Override
	protected PouleKonfigurationSheet getKonfigurationSheet() {
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
	protected short getSheetPos() {
		return DefaultSheetPos.POULE_WORK;
	}

	@Override
	protected String getCheckinSheetName() {
		return SheetNamen.checkinListe();
	}

	@Override
	protected String getMetadatenSchluessel() {
		return SheetMetadataHelper.SCHLUESSEL_POULE_CHECKIN_LISTE;
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return SheetMetadataHelper.findeSheetUndHeile(
				getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
				SheetMetadataHelper.SCHLUESSEL_POULE_CHECKIN_LISTE,
				getCheckinSheetName());
	}

	@Override
	public final TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}
}
