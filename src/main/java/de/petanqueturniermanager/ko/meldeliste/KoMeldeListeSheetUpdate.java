/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ko.meldeliste;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeKonstanten;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.SortHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.ko.konfiguration.KoKonfigurationSheet;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Aktualisiert die K.-O.-Meldeliste:
 * Nummern vergeben → nach Nr sortieren → Blatt formatieren.
 */
public class KoMeldeListeSheetUpdate extends SheetRunner implements ISheet, MeldeListeKonstanten {

	private static final Logger logger = LogManager.getLogger(KoMeldeListeSheetUpdate.class);

	protected static final int ERSTE_DATEN_ZEILE = KoListeDelegate.ERSTE_DATEN_ZEILE;
	protected static final int MIN_ANZAHL_MELDUNGEN_ZEILEN = KoListeDelegate.MIN_ANZAHL_MELDUNGEN_ZEILEN;

	public static final int AKTIV_WERT_NIMMT_TEIL = KoListeDelegate.AKTIV_WERT_NIMMT_TEIL;
	public static final int AKTIV_WERT_AUSGESTIEGEN = KoListeDelegate.AKTIV_WERT_AUSGESTIEGEN;

	private final KoListeDelegate delegate;

	public KoMeldeListeSheetUpdate(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.KO, "KO-Meldeliste");
		delegate = new KoListeDelegate(this);
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return getSheetHelper().findByName(SHEETNAME);
	}

	@Override
	public TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}

	@Override
	protected KoKonfigurationSheet getKonfigurationSheet() {
		return delegate.getKonfigurationSheet();
	}

	// ---------------------------------------------------------------
	// Forwarding-Methoden → Delegate
	// ---------------------------------------------------------------

	public void upDateSheet() throws GenerateException {
		delegate.upDateSheet();
	}

	public int getNrSpalte() {
		return delegate.getNrSpalte();
	}

	public int getTeamnameSpalte() {
		return delegate.getTeamnameSpalte();
	}

	public int getAktivSpalte() {
		return delegate.getAktivSpalte();
	}

	public int getErsteDatenZeile() {
		return delegate.getErsteDatenZeile();
	}

	public TeamMeldungen getAktiveMeldungen() throws GenerateException {
		return delegate.getAktiveMeldungen();
	}

	// ---------------------------------------------------------------
	// Eigene Methoden
	// ---------------------------------------------------------------

	@Override
	protected void doRun() throws GenerateException {
		XSpreadsheet xSheet = getXSpreadSheet();
		if (xSheet == null) {
			logger.warn("K.-O. Meldeliste nicht gefunden");
			return;
		}
		teamnummernVergeben(xSheet);
		nachTeamNrSortieren(xSheet);
		upDateSheet();
	}

	/**
	 * Vergibt fehlende Nummern an Zeilen mit Teamnamen aber ohne Nr.
	 */
	private void teamnummernVergeben(XSpreadsheet xSheet) throws GenerateException {
		int letzteZeile = letzteZeileMitTeamname(xSheet);
		if (letzteZeile < ERSTE_DATEN_ZEILE) {
			return;
		}

		int nrSpalte = getNrSpalte();
		int teamnameSpalte = getTeamnameSpalte();

		// Höchste vorhandene Nr lesen (absteigend sortieren, dann erste Zeile lesen)
		RangePosition sortRange = RangePosition.from(nrSpalte, ERSTE_DATEN_ZEILE, getAktivSpalte(), letzteZeile);
		SortHelper.from(this, sortRange).spalteToSort(nrSpalte).abSteigendSortieren().doSort();

		int letztNr = Math.max(0,
				getSheetHelper().getIntFromCell(xSheet, Position.from(nrSpalte, ERSTE_DATEN_ZEILE)));

		// Fehlende Nummern vergeben (nr <= 0 = leer/kein Wert)
		for (int zeile = ERSTE_DATEN_ZEILE; zeile <= letzteZeile; zeile++) {
			String teamname = getSheetHelper().getTextFromCell(xSheet, Position.from(teamnameSpalte, zeile));
			if (teamname == null || teamname.isBlank()) {
				continue;
			}
			int nr = getSheetHelper().getIntFromCell(xSheet, Position.from(nrSpalte, zeile));
			if (nr <= 0) {
				letztNr++;
				getSheetHelper().setNumberValueInCell(
						NumberCellValue.from(xSheet, Position.from(nrSpalte, zeile)).setValue(letztNr));
			}
		}
	}

	/**
	 * Sortiert die Datenzeilen aufsteigend nach Team-Nr.
	 */
	private void nachTeamNrSortieren(XSpreadsheet xSheet) throws GenerateException {
		int letzteZeile = letzteZeileMitTeamname(xSheet);
		if (letzteZeile < ERSTE_DATEN_ZEILE) {
			return;
		}
		RangePosition range = RangePosition.from(getNrSpalte(), ERSTE_DATEN_ZEILE,
				getAktivSpalte(), letzteZeile);
		SortHelper.from(this, range).spalteToSort(getNrSpalte()).aufSteigendSortieren(true).doSort();
	}

	/**
	 * Liefert die letzte Zeile, die einen Teamnamen enthält.
	 */
	private int letzteZeileMitTeamname(XSpreadsheet xSheet) throws GenerateException {
		int teamnameSpalte = getTeamnameSpalte();
		int letzte = ERSTE_DATEN_ZEILE - 1;
		for (int zeile = ERSTE_DATEN_ZEILE; zeile < ERSTE_DATEN_ZEILE + 9999; zeile++) {
			String name = getSheetHelper().getTextFromCell(xSheet, Position.from(teamnameSpalte, zeile));
			if (name != null && !name.isBlank()) {
				letzte = zeile;
			} else {
				// Nach der ersten leeren Zeile ohne vorherige Nr auch abbrechen,
				// falls dahinter nichts mehr kommt
				Integer nr = getSheetHelper().getIntFromCell(xSheet, Position.from(getNrSpalte(), zeile));
				if (nr == null || nr <= 0) {
					break;
				}
			}
		}
		return letzte;
	}

}
