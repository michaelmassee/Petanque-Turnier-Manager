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
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
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

	private static final String METADATA_SCHLUESSEL = SheetMetadataHelper.SCHLUESSEL_KO_MELDELISTE;

	private final KoListeDelegate delegate;

	public KoMeldeListeSheetUpdate(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.KO, "KO-Meldeliste");
		delegate = new KoListeDelegate(this);
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return SheetMetadataHelper.findeSheetUndHeile(
				getWorkingSpreadsheet().getWorkingSpreadsheetDocument(), METADATA_SCHLUESSEL, SHEETNAME);
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

	public int getTeamnameSpalte() throws GenerateException {
		return delegate.getTeamnameSpalte();
	}

	public int getVornameSpalte(int spielerIdx) throws GenerateException {
		return delegate.getVornameSpalte(spielerIdx);
	}

	public int getNachnameSpalte(int spielerIdx) throws GenerateException {
		return delegate.getNachnameSpalte(spielerIdx);
	}

	public int getAktivSpalte() throws GenerateException {
		return delegate.getAktivSpalte();
	}

	public int getRanglisteSpalte() throws GenerateException {
		return delegate.getRanglisteSpalte();
	}

	public int getErsteDatenZeile() {
		return delegate.getErsteDatenZeile();
	}

	public TeamMeldungen getAktiveMeldungen() throws GenerateException {
		return delegate.getAktiveMeldungen();
	}

	public TeamMeldungen getMeldungenSortiertNachRangliste() throws GenerateException {
		return delegate.getMeldungenSortiertNachRangliste();
	}

	public int letzteZeileMitDaten(XSpreadsheet xSheet) throws GenerateException {
		return delegate.letzteZeileMitDaten(xSheet);
	}

	/**
	 * Prüft ob alle aktiven Teams einen gültigen, eindeutigen Rang haben.
	 *
	 * @return null wenn OK, sonst eine Fehlermeldung.
	 */
	public String validiereRangSpalte() throws GenerateException {
		return delegate.validiereRangSpalte();
	}

	private void stringsBesinigen(XSpreadsheet xSheet) throws GenerateException {
		delegate.stringsBesinigen(xSheet);
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
			logger.warn("K.-O. Meldeliste nicht gefunden");
			return;
		}
		stringsBesinigen(xSheet);
		teamnummernVergeben(xSheet);
		aktivDefaultSetzen(xSheet);
		pruefeAufDoppelteTeamNr(xSheet);
		nachRangSortieren(xSheet);
		upDateSheet();
	}

	/**
	 * Setzt Aktiv=1 für Zeilen mit Spielernamen aber leerem Aktiv-Wert (nur für neue Zeilen).
	 */
	private void aktivDefaultSetzen(XSpreadsheet xSheet) throws GenerateException {
		int letzteZeile = delegate.letzteZeileMitDaten(xSheet);
		if (letzteZeile < ERSTE_DATEN_ZEILE) {
			return;
		}
		int vornameSpalte = delegate.getVornameSpalte(0);
		int aktivSpalte = delegate.getAktivSpalte();

		for (int zeile = ERSTE_DATEN_ZEILE; zeile <= letzteZeile; zeile++) {
			String vorname = getSheetHelper().getTextFromCell(xSheet, Position.from(vornameSpalte, zeile));
			if (vorname == null || vorname.isBlank()) {
				continue;
			}
			int aktiv = getSheetHelper().getIntFromCell(xSheet, Position.from(aktivSpalte, zeile));
			if (aktiv <= 0) {
				getSheetHelper().setNumberValueInCell(
						NumberCellValue.from(xSheet, Position.from(aktivSpalte, zeile))
								.setValue(AKTIV_WERT_NIMMT_TEIL));
			}
		}
	}

	/**
	 * Vergibt fehlende Nummern an Zeilen mit Spieler-Vornamen aber ohne Nr.
	 */
	private void teamnummernVergeben(XSpreadsheet xSheet) throws GenerateException {
		int letzteZeile = delegate.letzteZeileMitDaten(xSheet);
		if (letzteZeile < ERSTE_DATEN_ZEILE) {
			return;
		}

		int nrSpalte = getNrSpalte();
		int vornameSpalte = delegate.getVornameSpalte(0);

		// Höchste vorhandene Nr lesen (absteigend sortieren, dann erste Zeile lesen)
		RangePosition sortRange = RangePosition.from(nrSpalte, ERSTE_DATEN_ZEILE, delegate.getAktivSpalte(),
				letzteZeile);
		SortHelper.from(this, sortRange).spalteToSort(nrSpalte).abSteigendSortieren().doSort();

		int letztNr = Math.max(0,
				getSheetHelper().getIntFromCell(xSheet, Position.from(nrSpalte, ERSTE_DATEN_ZEILE)));

		// Fehlende Nummern vergeben (nr <= 0 = leer/kein Wert)
		for (int zeile = ERSTE_DATEN_ZEILE; zeile <= letzteZeile; zeile++) {
			String vorname = getSheetHelper().getTextFromCell(xSheet, Position.from(vornameSpalte, zeile));
			if (vorname == null || vorname.isBlank()) {
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
	 * Sortiert die Datenzeilen aufsteigend nach Rang (RNG-Spalte).
	 */
	private void nachRangSortieren(XSpreadsheet xSheet) throws GenerateException {
		int letzteZeile = delegate.letzteZeileMitDaten(xSheet);
		if (letzteZeile < ERSTE_DATEN_ZEILE) {
			return;
		}
		RangePosition range = RangePosition.from(getNrSpalte(), ERSTE_DATEN_ZEILE,
				delegate.getAktivSpalte(), letzteZeile);
		SortHelper.from(this, range).spalteToSort(delegate.getRanglisteSpalte())
				.aufSteigendSortieren(true).doSort();
	}

}
