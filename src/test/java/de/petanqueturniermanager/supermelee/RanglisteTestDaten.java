package de.petanqueturniermanager.supermelee;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.CellData;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.supermelee.spielrunde.SpielrundeSheet_Naechste;
import de.petanqueturniermanager.supermelee.spielrunde.SpielrundeSheet_TestDaten;
import de.petanqueturniermanager.supermelee.spieltagrangliste.SpieltagRanglisteSheetUITest;

/**
 * Erstellung 02.04.2023 / Michael Massee
 * 
 * @param <C>
 */

public class RanglisteTestDaten<C> {

	private final SheetHelper sheetHlp;
	private final WorkingSpreadsheet wkingSpreadsheet;
	private final C testClazz;

	public RanglisteTestDaten(WorkingSpreadsheet wkingSpreadsheet, SheetHelper sheetHlp, C testClazz) {
		this.sheetHlp = checkNotNull(sheetHlp);
		this.wkingSpreadsheet = checkNotNull(wkingSpreadsheet);
		this.testClazz = testClazz;
	}

	/**
	 * daten von json dateien einlesen
	 * 
	 * @param anzRunden
	 * @param validateRunden mit vertikal json dateien vergleichen
	 * @throws GenerateException
	 */
	public void erstelleTestSpielrunden(int anzRunden, boolean validateRunden) throws GenerateException {
		erstelleTestSpielrunden(anzRunden, validateRunden, SpielTagNr.from(1));
	}

	/**
	 * daten von json dateien einlesen
	 * 
	 * @param anzRunden
	 * @param validateRunden mit vertikal json dateien vergleichen
	 * 
	 * @throws GenerateException
	 */
	public void erstelleTestSpielrunden(int anzRunden, boolean validateRunden, SpielTagNr spieltag)
			throws GenerateException {
		SpielrundeSheet_Naechste spielrundeSheetNaechste = new SpielrundeSheet_Naechste(wkingSpreadsheet);

		for (int spielrundeNrCntr = 1; spielrundeNrCntr <= anzRunden; spielrundeNrCntr++) {

			SpielRundeNr spielrundeNr = SpielRundeNr.from(spielrundeNrCntr);

			spielrundeSheetNaechste.run(); // no thread 1. Runde
			spielrundeSheetNaechste.setSpielRundeNr(spielrundeNr);
			Position letzteErgbnissPosition = spielrundeSheetNaechste.letzteErgbnissPosition(); // ist auch die letzte Zeile in der Tabelle

			XSpreadsheet spielrunde = sheetHlp.findByName(spieltag.getNr() + "." + spielrundeNrCntr + ". Spielrunde");
			assertThat(spielrunde).isNotNull();

			// ---------------------------------------------------------------------------------------------------------
			// Spielrunden eintragen
			// ---------------------------------------------------------------------------------------------------------
			RangeHelper rngHlpr = spielRundeRangeHlpr(letzteErgbnissPosition, spielrunde);

			// feste spielrunden aus json dateien laden
			InputStream jsonFile = testClazz.getClass()
					.getResourceAsStream(getSpielrundePaarungenJsonName(spielrundeNr, spieltag));
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			RangeData rundeSpielerNrData = gson.fromJson(new BufferedReader(new InputStreamReader(jsonFile)),
					RangeData.class);

			rngHlpr.setDataInRange(rundeSpielerNrData);

			// ---------------------------------------------------------------------------------------------------------
			// Ergebnisse eintragen
			// ---------------------------------------------------------------------------------------------------------
			RangeHelper rngHlprErg = ergebnisseRangeHlpr(letzteErgbnissPosition, spielrunde);
			RangeData ergebnisseDataRunde;
			ergebnisseDataRunde = getErgebnisseFromJson(spielrundeNr, spieltag);
			rngHlprErg.setDataInRange(ergebnisseDataRunde);
			// ---------------------------------------------------------------------------------------------------------

			// validate vertikal ergebnisse
			if (validateRunden) {
				validatVertikalWithJson(spielrunde, spielrundeNr, spieltag);
			}
		}
	}

	private void validatVertikalWithJson(XSpreadsheet spielrunde, SpielRundeNr spielrundeNr, SpielTagNr spieltag)
			throws GenerateException {

		RangeData vertikalErgRangeVonSpielrunde = getVertikalErgRange(spielrunde).getDataFromRange();

		InputStream jsonFile = SpieltagRanglisteSheetUITest.class.getResourceAsStream(
				"spieltag" + spieltag.getNr() + "-runde" + spielrundeNr.getNr() + "-vertikal.json");
		Gson gson = new GsonBuilder().create();
		RangeData refSpielergData = gson.fromJson(new BufferedReader(new InputStreamReader(jsonFile)), RangeData.class);

		assertThat(vertikalErgRangeVonSpielrunde).hasSameSizeAs(refSpielergData);

		int idx = 0;
		// jede zeile vergleichen, wegen fehlermeldung 
		for (RowData data : vertikalErgRangeVonSpielrunde) {
			List<String> expected = refSpielergData.get(idx).stream().map(c -> c.getStringVal()).toList();
			assertThat(data).extracting(CellData::getStringVal).containsExactlyElementsOf(expected);
			idx++;
		}
	}

	private RangeHelper getVertikalErgRange(XSpreadsheet spielrunde) throws GenerateException {
		RangePosition rangeSplrErg = RangePosition.from(SpielrundeSheet_Naechste.ERSTE_SPALTE_VERTIKALE_ERGEBNISSE,
				SpielrundeSheet_Naechste.ERSTE_DATEN_ZEILE, SpielrundeSheet_Naechste.SPALTE_VERTIKALE_ERGEBNISSE_BA_NR,
				SpielrundeSheet_Naechste.ERSTE_DATEN_ZEILE + 23); // 22 Aktive Spieler Meldungen
		assertThat(rangeSplrErg.getAddress()).isEqualTo("S3:W26");
		RangeHelper rngHlpr = RangeHelper.from(spielrunde, wkingSpreadsheet.getWorkingSpreadsheetDocument(),
				rangeSplrErg);
		return rngHlpr;

	}

	private String getVertikalJsonName(SpielRundeNr spielRundeNr, SpielTagNr spielTagNr) {
		return "spieltag" + spielTagNr.getNr() + "-runde" + spielRundeNr.getNr() + "-vertikal.json";
	}

	public void writeVertikalToJson(XSpreadsheet spielrunde, SpielRundeNr spielRundeNr, SpielTagNr spielTagNr)
			throws GenerateException, IOException {
		RangeData vertikalErgRange = getVertikalErgRange(spielrunde).getDataFromRange();
		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		File jsoFile = new File("/home/michael/tmp/", getVertikalJsonName(spielRundeNr, spielTagNr));
		try (BufferedWriter fileStream = new BufferedWriter(new FileWriter(jsoFile))) {
			fileStream.write(gson.toJson(vertikalErgRange));
		}
	}

	private String getSpielrundePaarungenJsonName(SpielRundeNr spielRundeNr, SpielTagNr spielTagNr) {
		return "spieltag" + spielTagNr.getNr() + "-runde" + spielRundeNr.getNr() + "-paarungen.json";
	}

	private String getSpielrundeErgJsonName(SpielRundeNr spielRundeNr, SpielTagNr spielTagNr) {
		return "spieltag" + spielTagNr.getNr() + "-runde" + spielRundeNr.getNr() + "-ergebnisse.json";
	}

	/**
	 * generiere komplette spielrunden, und speichern die ergebnisse und spielparungen in json dateien
	 * 
	 * @param anzRunde
	 * @param spielTagNr
	 * @throws GenerateException
	 * @throws IOException
	 */

	public void generateSpielrundenJsonFilesIntmp(int anzRunde, SpielTagNr spielTagNr)
			throws GenerateException, IOException {
		SpielrundeSheet_Naechste spielrundeSheetNaechste = new SpielrundeSheet_Naechste(wkingSpreadsheet);
		spielrundeSheetNaechste.setSpielTag(spielTagNr);

		SpielrundeSheet_TestDaten spielrundeSheetTestDaten = new SpielrundeSheet_TestDaten(wkingSpreadsheet);
		spielrundeSheetTestDaten.setSpielTag(spielTagNr);
		spielrundeSheetTestDaten.genRunden(anzRunde, false);

		for (int rundeCntr = 1; rundeCntr <= anzRunde; rundeCntr++) {

			XSpreadsheet spielrunde = sheetHlp.findByName(spielTagNr.getNr() + "." + rundeCntr + ". Spielrunde");
			assertThat(spielrunde).isNotNull();

			// Paarungen Block
			// ------------------------------------------------------------------------------
			spielrundeSheetNaechste.setSpielRundeNr(SpielRundeNr.from(rundeCntr));
			Position letzteErgbnissPosition = spielrundeSheetNaechste.letzteErgbnissPosition(); // ist auch die letzte Zeile in der Tabelle

			RangeHelper rngHlpr = spielRundeRangeHlpr(letzteErgbnissPosition, spielrunde);
			RangeData rundeSplrNrRange = rngHlpr.getDataFromRange();

			Gson gson = new GsonBuilder().setPrettyPrinting().create();

			File jsoFile = new File(
					"/home/michael/tmp/" + getSpielrundePaarungenJsonName(SpielRundeNr.from(rundeCntr), spielTagNr));
			try (BufferedWriter fileStream = new BufferedWriter(new FileWriter(jsoFile))) {
				fileStream.write(gson.toJson(rundeSplrNrRange));
			}

			// ------------------------------------------------------------------------------
			// Ergebnisse Block
			// ------------------------------------------------------------------------------
			RangeHelper rngHlprErg = ergebnisseRangeHlpr(letzteErgbnissPosition, spielrunde);
			RangeData rundeErgRange = rngHlprErg.getDataFromRange();

			jsoFile = new File(
					"/home/michael/tmp/" + getSpielrundeErgJsonName(SpielRundeNr.from(rundeCntr), spielTagNr));
			try (BufferedWriter fileStream = new BufferedWriter(new FileWriter(jsoFile))) {
				fileStream.write(gson.toJson(rundeErgRange));
			}

		}
	}

	private RangeHelper spielRundeRangeHlpr(Position letzteErgbnissPosition, XSpreadsheet spielrunde)
			throws GenerateException {
		RangePosition rangeSplrNr = RangePosition.from(SpielrundeSheet_Naechste.ERSTE_SPIELERNR_SPALTE,
				SpielrundeSheet_Naechste.ERSTE_DATEN_ZEILE, SpielrundeSheet_Naechste.ERSTE_SPIELERNR_SPALTE + 5, // Team A + B
				letzteErgbnissPosition.getZeile());
		RangeHelper rngHlpr = RangeHelper.from(spielrunde, wkingSpreadsheet.getWorkingSpreadsheetDocument(),
				rangeSplrNr);
		return rngHlpr;

	}

	private RangeHelper ergebnisseRangeHlpr(Position letzteErgbnissPosition, XSpreadsheet spielrunde)
			throws GenerateException {
		return RangeHelper.from(spielrunde, wkingSpreadsheet.getWorkingSpreadsheetDocument(),
				RangePosition.from(SpielrundeSheet_Naechste.ERSTE_SPALTE_ERGEBNISSE,
						SpielrundeSheet_Naechste.ERSTE_DATEN_ZEILE, letzteErgbnissPosition));
	}

	public RangeData getErgebnisseFromJson(SpielRundeNr spielrunde, SpielTagNr spieltag) {

		InputStream jsonFile = testClazz.getClass().getResourceAsStream(getSpielrundeErgJsonName(spielrunde, spieltag));
		Gson gson = new GsonBuilder().create();
		return gson.fromJson(new BufferedReader(new InputStreamReader(jsonFile)), RangeData.class);

	}

}
