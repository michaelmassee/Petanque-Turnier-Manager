/**
 * Erstellung : 01.03.2024 / Michael Massee
 **/

package de.petanqueturniermanager.schweizer.meldeliste;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.SortHelper;

public class SchweizerMeldeListeSheetUpdate extends AbstractSchweizerMeldeListeSheet {
	private static final Logger logger = LogManager.getLogger(SchweizerMeldeListeSheetUpdate.class);

	public SchweizerMeldeListeSheetUpdate(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
	}

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

		// Fehlende Nummern vergeben
		for (int zeile = ERSTE_DATEN_ZEILE; zeile <= letzteZeile; zeile++) {
			String vorname = getSheetHelper().getTextFromCell(xSheet, Position.from(vornameSpalte, zeile));
			if (vorname == null || vorname.isEmpty()) {
				continue;
			}
			int nr = getSheetHelper().getIntFromCell(xSheet, Position.from(getTeamNrSpalte(), zeile));
			if (nr == -1) {
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

	@Override
	public Logger getLogger() {
		return logger;
	}

}
