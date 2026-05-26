/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.maastrichter.meldeliste;

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
import de.petanqueturniermanager.maastrichter.konfiguration.MaastrichterKonfigurationSheet;

/**
 * Checkin-Liste des Maastrichter-Systems: kompakte Druckansicht zum Abhaken der Teams.
 */
public class MaastrichterCheckinListeSheet extends AbstractTeilnehmerNamenCheckinListeSheet {

	private static final int MELDELISTE_ERSTE_DATEN_ZEILE = 3;

	private final MaastrichterKonfigurationSheet konfigurationSheet;
	private final MaastrichterMeldeListeSheetUpdate meldeliste;

	public MaastrichterCheckinListeSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.MAASTRICHTER, "Checkin Liste");
		konfigurationSheet = new MaastrichterKonfigurationSheet(workingSpreadsheet);
		meldeliste = new MaastrichterMeldeListeSheetUpdate(workingSpreadsheet);
	}

	@Override
	protected MaastrichterKonfigurationSheet getKonfigurationSheet() {
		return konfigurationSheet;
	}

	@Override
	protected void meldelisteVorbereiten() throws GenerateException {
		meldeliste.upDateSheet();
	}

	@Override
	protected void meldelisteSortieren(int spalteNr, boolean aufsteigend) throws GenerateException {
		meldeliste.doSort(spalteNr, aufsteigend);
	}

	@Override
	protected int getNachnameSpalteMeldeliste() throws GenerateException {
		return meldeliste.getNachnameSpalte(0);
	}

	@Override
	protected int getNummerSpalteMeldeliste() {
		return meldeliste.getTeamNrSpalte();
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
		return DefaultSheetPos.MAASTRICHTER_WORK;
	}

	@Override
	protected String getCheckinSheetName() {
		return SheetNamen.checkinListe();
	}

	@Override
	protected String getMetadatenSchluessel() {
		return SheetMetadataHelper.SCHLUESSEL_MAASTRICHTER_CHECKIN_LISTE;
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return SheetMetadataHelper.findeSheetUndHeile(
				getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
				SheetMetadataHelper.SCHLUESSEL_MAASTRICHTER_CHECKIN_LISTE,
				getCheckinSheetName());
	}

	@Override
	public final TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}
}
