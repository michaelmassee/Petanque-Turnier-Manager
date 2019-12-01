/**
 * Erstellung 10.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.google.common.annotations.VisibleForTesting;
import com.sun.star.awt.FontWeight;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.CellVertJustify2;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ColorHelper;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SortHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.model.IMeldungen;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * @author Michael Massee
 *
 */
public class MeldeListeHelper<MLDTYPE> implements MeldeListeKonstanten {

	private final IMeldeliste meldeListe;

	public MeldeListeHelper(IMeldeliste newMeldeListe) {
		meldeListe = checkNotNull(newMeldeListe);
	}

	/**
	 *
	 * @param spalteNr 0 = erste spalte
	 * @param isAscending
	 * @throws GenerateException
	 */

	public void doSort(int spalteNr, boolean isAscending) throws GenerateException {
		int letzteSpielZeile = meldeListe.getMeldungenSpalte().letzteZeileMitSpielerName();
		if (letzteSpielZeile > ERSTE_DATEN_ZEILE) { // daten vorhanden
			RangePosition rangeToSort = RangePosition.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE, meldeListe.letzteSpielTagSpalte(), letzteSpielZeile);
			SortHelper.from(getXSpreadSheet(), rangeToSort).spalteToSort(spalteNr).aufSteigendSortieren(isAscending).doSort();
		}
	}

	/**
	 * prüft auf doppelte spieler nr oder namen
	 *
	 * @return
	 * @throws GenerateException wenn doppelt daten
	 */
	public void testDoppelteMeldungen() throws GenerateException {
		meldeListe.processBoxinfo("Prüfe Doppelte Daten in Meldungen");
		XSpreadsheet xSheet = getXSpreadSheet();

		int letzteSpielZeile = meldeListe.getMeldungenSpalte().letzteZeileMitSpielerName();
		if (letzteSpielZeile <= ERSTE_DATEN_ZEILE) { // daten vorhanden ?
			return; // keine Daten
		}

		doSort(SPIELER_NR_SPALTE, false); // hoechste nummer oben, ohne nummer nach unten

		// doppelte spieler Nummer entfernen !?!?!
		HashSet<Integer> spielrNrInSheet = new HashSet<>();
		HashSet<String> spielrNamenInSheet = new HashSet<>();

		int spielrNr;
		String spielerName;
		NumberCellValue errCelVal = NumberCellValue.from(xSheet, Position.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE)).setCharColor(ColorHelper.CHAR_COLOR_RED);

		StringCellValue errStrCelVal = StringCellValue.from(xSheet, Position.from(meldeListe.getMeldungenSpalte().getSpielerNameErsteSpalte(), ERSTE_DATEN_ZEILE))
				.setCharColor(ColorHelper.CHAR_COLOR_RED);

		for (int spielerZeilecntr = ERSTE_DATEN_ZEILE; spielerZeilecntr <= letzteSpielZeile; spielerZeilecntr++) {
			// -------------------
			// Spieler nr testen
			// -------------------
			spielrNr = meldeListe.getSheetHelper().getIntFromCell(xSheet, Position.from(SPIELER_NR_SPALTE, spielerZeilecntr));
			if (spielrNr > -1) {
				if (spielrNrInSheet.contains(spielrNr)) {
					// RED Color
					meldeListe.getSheetHelper().setValInCell(errCelVal.setValue((double) spielrNr).zeile(spielerZeilecntr));
					throw new GenerateException("Meldeliste wurde nicht Aktualisiert.\r\nSpieler Nr. " + spielrNr + " ist doppelt in der Meldeliste !!!");
				}
				spielrNrInSheet.add(spielrNr);
			}

			// -------------------
			// spieler namen testen
			// -------------------
			// Supermelee hat nur ein name spalte
			// wird trim gemacht
			spielerName = meldeListe.getSheetHelper().getTextFromCell(xSheet, Position.from(meldeListe.getMeldungenSpalte().getSpielerNameErsteSpalte(), spielerZeilecntr));

			if (StringUtils.isNotEmpty(spielerName)) {
				if (spielrNamenInSheet.contains(cleanUpSpielerName(spielerName))) {
					// RED Color
					meldeListe.getSheetHelper().setStringValueInCell(errStrCelVal.setValue(spielerName).zeile(spielerZeilecntr));
					throw new GenerateException(
							"Meldeliste wurde nicht Aktualisiert.\r\nSpieler Namen " + spielerName + " ist doppelt in der Meldeliste. Zeile:" + spielerZeilecntr);
				}
				spielrNamenInSheet.add(cleanUpSpielerName(spielerName));
			}
		}
	}

	/**
	 * für ein vergleich ,.: und leerzeichen entfernen
	 *
	 * @param name
	 * @return
	 */
	@VisibleForTesting
	String cleanUpSpielerName(String name) {
		return name.replaceAll("[^a-zA-Z0-9öäüÄÖÜß]+", "").toLowerCase();
	}

	public XSpreadsheet getXSpreadSheet() {
		return NewSheet.from(meldeListe.getWorkingSpreadsheet(), SHEETNAME).useIfExist().hideGrid().pos(DefaultSheetPos.MELDELISTE).tabColor(SHEET_COLOR).create().getSheet();
	}

	public int getSpielerNameSpalte() {
		return meldeListe.getSpielerNameSpalte();
	}

	/**
	 * alle zeilen mit nummer ohne namen entfernen
	 *
	 * @throws GenerateException
	 */

	public void zeileOhneSpielerNamenEntfernen() throws GenerateException {
		meldeListe.processBoxinfo("Zeilen ohne Spielernamen entfernen");

		doSort(meldeListe.getMeldungenSpalte().getSpielerNameErsteSpalte(), true); // alle zeilen ohne namen nach unten sortieren, egal ob daten oder nicht
		int letzteNrZeile = meldeListe.neachsteFreieDatenOhneSpielerNrZeile();
		if (letzteNrZeile < ERSTE_DATEN_ZEILE) { // daten vorhanden ?
			return; // keine Daten
		}
		XSpreadsheet xSheet = getXSpreadSheet();

		StringCellValue emptyVal = StringCellValue.from(xSheet, Position.from(SPIELER_NR_SPALTE, 0)).setValue("");

		int letzteZeileMitSpielerName = meldeListe.letzteZeileMitSpielerName(); // erst ab zeilen ohne namen anfangen

		for (int spielerNrZeilecntr = letzteZeileMitSpielerName; spielerNrZeilecntr < letzteNrZeile; spielerNrZeilecntr++) {
			Position posSpielerName = Position.from(meldeListe.getMeldungenSpalte().getSpielerNameErsteSpalte(), spielerNrZeilecntr);
			String spielerNamen = meldeListe.getSheetHelper().getTextFromCell(xSheet, posSpielerName);
			// Achtung alle durchgehen weil eventuell lücken in der nr spalte!
			if (StringUtils.isBlank(spielerNamen)) { // null oder leer oder leerzeichen
				// nr ohne spieler namen entfernen
				meldeListe.getSheetHelper().setStringValueInCell(emptyVal.zeile(spielerNrZeilecntr));
			}
		}
	}

	public String formulaSverweisSpielernamen(String spielrNrAdresse) {
		String ersteZelleAddress = Position.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE).getAddressWith$();
		String letzteZelleAddress = Position.from(meldeListe.getMeldungenSpalte().getSpielerNameErsteSpalte(), 999).getAddressWith$();
		return "VLOOKUP(" + spielrNrAdresse + ";$'" + SHEETNAME + "'." + ersteZelleAddress + ":" + letzteZelleAddress + ";2;0)";
	}

	/**
	 *
	 * @param spieltag
	 * @param spielrundeGespielt list mit Flags. null für alle
	 * @return
	 * @throws GenerateException
	 */

	public IMeldungen<MLDTYPE> getMeldungen(final SpielTagNr spieltag, final List<SpielrundeGespielt> spielrundeGespielt, IMeldungen<MLDTYPE> meldungen) throws GenerateException {
		checkNotNull(spieltag, "spieltag == null");
		checkNotNull(meldungen, "meldungen == null");

		int letzteZeile = meldeListe.getMeldungenSpalte().getLetzteDatenZeile();

		if (letzteZeile >= ERSTE_DATEN_ZEILE) {
			// daten vorhanden
			int spieltagSpalte = spieltagSpalte(spieltag);

			// Use getDataArray to get an Array off all Spieler bis SpieltagSpalte
			RangePosition rangebisSpieltagSpalte = RangePosition.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE, spieltagSpalte, letzteZeile);
			RangeData meldungenDaten = RangeHelper.from(getXSpreadSheet(), rangebisSpieltagSpalte).getDataFromRange();

			// 0 = nr
			// 1 = name
			// 2 = setzpos
			// letzte ! spalte = aktuelle Spieltag status
			for (RowData meldungZeile : meldungenDaten) {
				int isAktivStatus = meldungZeile.getLast().getIntVal(-1);
				SpielrundeGespielt status = SpielrundeGespielt.findById(isAktivStatus);
				if (spielrundeGespielt == null || spielrundeGespielt.contains(status)) {
					meldungen.addNewWennNichtVorhanden(meldungZeile);
				}
			}
		}
		return meldungen;
	}

	public int setzPositionSpalte() {
		return meldeListe.getMeldungenSpalte().getSpielerNameErsteSpalte() + 1;
	}

	public int ersteSpieltagSpalte() {
		if (setzPositionSpalte() > -1) {
			return setzPositionSpalte() + 1;
		}
		return SPIELER_NR_SPALTE + meldeListe.getMeldungenSpalte().getAnzahlSpielerNamenSpalten();
	}

	public int spieltagSpalte(SpielTagNr spieltag) {
		return ersteSpieltagSpalte() + spieltag.getNr() - 1;
	}

	public void updateMeldungenNr() throws GenerateException {

		meldeListe.processBoxinfo("Aktualisiere Meldungen Nummer");

		int letzteSpielZeile = meldeListe.getMeldungenSpalte().letzteZeileMitSpielerName();
		if (letzteSpielZeile <= ERSTE_DATEN_ZEILE) { // daten vorhanden ?
			return; // nur 1 Meldung
		}
		XSpreadsheet xSheet = getXSpreadSheet();
		doSort(SPIELER_NR_SPALTE, false); // hoechste nummer oben, ohne nummer nach unten

		int letzteSpielerNr = 0;
		int spielrNr = meldeListe.getSheetHelper().getIntFromCell(xSheet, Position.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE));
		if (spielrNr > -1) {
			letzteSpielerNr = spielrNr;
		}

		// Zeile erste Meldung ohne Nummer, weil ohne nummer nach unten sortiert
		int ersteZeileOhneNummer = meldeListe.neachsteFreieDatenOhneSpielerNrZeile(); // letzte Zeile ohne Spieler Nr

		// lücken füllen
		NumberCellValue celVal = NumberCellValue.from(xSheet, Position.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE));
		for (int spielerZeilecntr = ersteZeileOhneNummer; spielerZeilecntr <= letzteSpielZeile; spielerZeilecntr++) {
			spielrNr = meldeListe.getSheetHelper().getIntFromCell(xSheet, Position.from(SPIELER_NR_SPALTE, spielerZeilecntr));
			if (spielrNr == -1) {
				meldeListe.getSheetHelper().setValInCell(celVal.setValue((double) ++letzteSpielerNr).zeile(spielerZeilecntr));
			}
		}
		// spieler nach Alphabet sortieren
		doSort(meldeListe.getMeldungenSpalte().getSpielerNameErsteSpalte(), true);
	}

	/**
	 * @param turnierSystem
	 * @throws GenerateException
	 */
	public void insertTurnierSystemInHeader(TurnierSystem turnierSystem) throws GenerateException {
		// oben links
		meldeListe.getSheetHelper()
				.setStringValueInCell(StringCellValue.from(getXSpreadSheet(), Position.from(0, 0), "Turniersystem: " + turnierSystem.getBezeichnung()).setEndPosMergeSpaltePlus(1)
						.setCharWeight(FontWeight.BOLD).setHoriJustify(CellHoriJustify.LEFT).setVertJustify(CellVertJustify2.TOP).setShrinkToFit(true).setCharColor("00599d"));
	}

}
