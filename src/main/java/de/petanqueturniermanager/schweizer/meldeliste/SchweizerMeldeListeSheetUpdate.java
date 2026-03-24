/**
 * Erstellung : 01.03.2024 / Michael Massee
 **/

package de.petanqueturniermanager.schweizer.meldeliste;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeKonstanten;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.SortHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerKonfigurationSheet;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;
import de.petanqueturniermanager.helper.i18n.SheetNamen;

public class SchweizerMeldeListeSheetUpdate extends SheetRunner implements ISheet, MeldeListeKonstanten {

	private static final Logger logger = LogManager.getLogger(SchweizerMeldeListeSheetUpdate.class);

	protected static final int ERSTE_DATEN_ZEILE = SchweizerListeDelegate.ERSTE_DATEN_ZEILE;
	protected static final int MIN_ANZAHL_MELDUNGEN_ZEILEN = SchweizerListeDelegate.MIN_ANZAHL_MELDUNGEN_ZEILEN;

	public static final int AKTIV_WERT_NIMMT_TEIL = SchweizerListeDelegate.AKTIV_WERT_NIMMT_TEIL;
	public static final int AKTIV_WERT_AUSGESTIEGEN = SchweizerListeDelegate.AKTIV_WERT_AUSGESTIEGEN;

	private final SchweizerListeDelegate delegate;

	public SchweizerMeldeListeSheetUpdate(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.SCHWEIZER, "Schweizer-Meldeliste");
		delegate = new SchweizerListeDelegate(this);
	}

	private static final String METADATA_SCHLUESSEL = SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_MELDELISTE;

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return SheetMetadataHelper.findeSheetUndHeile(
				getWorkingSpreadsheet().getWorkingSpreadsheetDocument(), METADATA_SCHLUESSEL, SheetNamen.LEGACY_MELDELISTE);
	}

	@Override
	public TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}

	@Override
	protected SchweizerKonfigurationSheet getKonfigurationSheet() {
		return delegate.getKonfigurationSheet();
	}

	// ---------------------------------------------------------------
	// Forwarding-Methoden → Delegate
	// ---------------------------------------------------------------

	public void upDateSheet() throws GenerateException {
		delegate.upDateSheet();
	}

	public int getTeamNrSpalte() {
		return delegate.getTeamNrSpalte();
	}

	public int getTeamnameSpalte() throws GenerateException {
		return delegate.getTeamnameSpalte();
	}

	public int getSpielerNameErsteSpalte() throws GenerateException {
		return delegate.getSpielerNameErsteSpalte();
	}

	public int getVornameSpalte(int spielerIdx) throws GenerateException {
		return delegate.getVornameSpalte(spielerIdx);
	}

	public int getNachnameSpalte(int spielerIdx) throws GenerateException {
		return delegate.getNachnameSpalte(spielerIdx);
	}

	public int getVereinsnameSpalte(int spielerIdx) throws GenerateException {
		return delegate.getVereinsnameSpalte(spielerIdx);
	}

	public int getSetzPositionSpalte() throws GenerateException {
		return delegate.getSetzPositionSpalte();
	}

	public int getAktivSpalte() throws GenerateException {
		return delegate.getAktivSpalte();
	}

	public int getErsteDatenZiele() {
		return delegate.getErsteDatenZiele();
	}

	public TeamMeldungen getAktiveMeldungen() throws GenerateException {
		return delegate.getAktiveMeldungen();
	}

	public int getTeamNrByTeamname(String teamname) throws GenerateException {
		return delegate.getTeamNrByTeamname(teamname);
	}

	public String getTeamNameByNr(int teamNr) throws GenerateException {
		return delegate.getTeamNameByNr(teamNr);
	}

	public void setAktiveSpielRunde(SpielRundeNr spielRundeNr) throws GenerateException {
		delegate.setAktiveSpielRunde(spielRundeNr);
	}

	protected int letzteZeileMitDaten(XSpreadsheet xSheet) throws GenerateException {
		return delegate.letzteZeileMitDaten(xSheet);
	}

	protected void pruefeAufDoppelteTeamNr(XSpreadsheet xSheet) throws GenerateException {
		delegate.pruefeAufDoppelteTeamNr(xSheet);
	}

	// ---------------------------------------------------------------
	// Eigene Methoden
	// ---------------------------------------------------------------

	@Override
	protected void doRun() throws GenerateException {
		XSpreadsheet xSheet = getXSpreadSheet();
		if (xSheet == null) {
			logger.warn("Schweizer Meldeliste nicht gefunden");
			return;
		}
		stringsBesinigen(xSheet);
		teamnummernVergeben(xSheet);
		nachTeamNrSortieren(xSheet);
		pruefeAufDoppelteTeamNr(xSheet);
		upDateSheet();
	}

	private void stringsBesinigen(XSpreadsheet xSheet) throws GenerateException {
		Formation formation = getKonfigurationSheet().getMeldeListeFormation();
		boolean teamnameAktiv = getKonfigurationSheet().isMeldeListeTeamnameAnzeigen();
		boolean vereinsnameAktiv = getKonfigurationSheet().isMeldeListeVereinsnameAnzeigen();
		int letzteZeile = letzteZeileMitDaten(xSheet) + MIN_ANZAHL_MELDUNGEN_ZEILEN;
		for (int zeile = ERSTE_DATEN_ZEILE; zeile <= letzteZeile; zeile++) {
			if (teamnameAktiv) {
				bereinigeSpalte(xSheet, getTeamnameSpalte(), zeile);
			}
			for (int s = 0; s < formation.getAnzSpieler(); s++) {
				bereinigeSpalte(xSheet, getVornameSpalte(s), zeile);
				bereinigeSpalte(xSheet, getNachnameSpalte(s), zeile);
				if (vereinsnameAktiv) {
					bereinigeSpalte(xSheet, getVereinsnameSpalte(s), zeile);
				}
			}
		}
	}

	private void bereinigeSpalte(XSpreadsheet xSheet, int spalte, int zeile) throws GenerateException {
		if (spalte < 0) {
			return;
		}
		String original = getSheetHelper().getTextFromCell(xSheet, Position.from(spalte, zeile));
		if (original == null || original.isEmpty()) {
			return;
		}
		String bereinigt = original.replaceAll("[\\p{Cntrl}]", "").strip();
		if (!bereinigt.equals(original)) {
			getSheetHelper().setStringValueInCell(StringCellValue.from(xSheet, Position.from(spalte, zeile), bereinigt));
		}
	}

	private void teamnummernVergeben(XSpreadsheet xSheet) throws GenerateException {
		int letzteZeile = letzteZeileMitDaten(xSheet);
		if (letzteZeile < ERSTE_DATEN_ZEILE) {
			return;
		}
		int vornameSpalte = getVornameSpalte(0);
		// Nr-Spalte absteigend sortieren (höchste Nr zuerst, leere ans Ende)
		RangePosition range = RangePosition.from(getTeamNrSpalte(), ERSTE_DATEN_ZEILE,
				getSetzPositionSpalte(), letzteZeile);
		SortHelper.from(this, range).spalteToSort(getTeamNrSpalte()).abSteigendSortieren().doSort();

		// Höchste vorhandene Nr lesen
		int letztNr = Math.max(0,
				getSheetHelper().getIntFromCell(xSheet, Position.from(getTeamNrSpalte(), ERSTE_DATEN_ZEILE)));

		// Fehlende Nummern vergeben (nr <= 0 bedeutet: leer oder kein Wert vergeben)
		for (int zeile = ERSTE_DATEN_ZEILE; zeile <= letzteZeile; zeile++) {
			String vorname = getSheetHelper().getTextFromCell(xSheet, Position.from(vornameSpalte, zeile));
			if (vorname == null || vorname.isEmpty()) {
				continue;
			}
			int nr = getSheetHelper().getIntFromCell(xSheet, Position.from(getTeamNrSpalte(), zeile));
			if (nr <= 0) {
				letztNr++;
				getSheetHelper().setNumberValueInCell(
						NumberCellValue.from(xSheet, Position.from(getTeamNrSpalte(), zeile)).setValue(letztNr));
			}
		}
	}

	private void nachTeamNrSortieren(XSpreadsheet xSheet) throws GenerateException {
		int letzteZeile = letzteZeileMitDaten(xSheet);
		if (letzteZeile < ERSTE_DATEN_ZEILE) {
			return;
		}
		RangePosition range = RangePosition.from(getTeamNrSpalte(), ERSTE_DATEN_ZEILE,
				getSetzPositionSpalte(), letzteZeile);
		SortHelper.from(this, range).spalteToSort(getTeamNrSpalte()).aufSteigendSortieren(true).doSort();
	}

}
