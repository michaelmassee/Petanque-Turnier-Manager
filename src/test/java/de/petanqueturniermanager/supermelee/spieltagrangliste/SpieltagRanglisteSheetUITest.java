package de.petanqueturniermanager.supermelee.spieltagrangliste;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.CellData;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.supermelee.meldeliste.TestMeldeListeErstellen;
import de.petanqueturniermanager.supermelee.spielrunde.SpielrundeSheet_Naechste;

public class SpieltagRanglisteSheetUITest extends BaseCalcUITest {

	private static final Logger logger = LogManager.getLogger(SpieltagRanglisteSheetUITest.class);

	private DocumentPropertiesHelper docPropHelper;

	@Before
	public void testMeldeListeErstelln() throws GenerateException {
		// erst mal eine meldeListe erstellen
		TestMeldeListeErstellen testMeldeListeErstellen = new TestMeldeListeErstellen(wkingSpreadsheet, doc);
		int anzMeldungen = testMeldeListeErstellen.run();
		this.docPropHelper = new DocumentPropertiesHelper(wkingSpreadsheet);
		docPropHelper.setBooleanProperty(BasePropertiesSpalte.KONFIG_PROP_ZEIGE_ARBEITS_SPALTEN, true);
	}

	@Test
	public void testRangliste() throws GenerateException, IOException {

		SpielrundeSheet_Naechste spielrundeSheetNaechste = new SpielrundeSheet_Naechste(wkingSpreadsheet);

		for (int spielrundeNr = 1; spielrundeNr < 5; spielrundeNr++) {

			spielrundeSheetNaechste.run(); // no thread 1. Runde

			XSpreadsheet spielrunde = sheetHlp.findByName("1." + spielrundeNr + ". Spielrunde");
			assertThat(spielrunde).isNotNull();

			RangePosition rangeSplrNr = RangePosition.from(SpielrundeSheet_Naechste.ERSTE_SPIELERNR_SPALTE,
					SpielrundeSheet_Naechste.ERSTE_DATEN_ZEILE, SpielrundeSheet_Naechste.ERSTE_SPIELERNR_SPALTE + 5, // Team A + B
					SpielrundeSheet_Naechste.ERSTE_DATEN_ZEILE + 3); // 4 paarungen
			RangeHelper rngHlpr = RangeHelper.from(spielrunde, wkingSpreadsheet.getWorkingSpreadsheetDocument(),
					rangeSplrNr);

			// feste spielrunden aus json dateien laden
			InputStream jsonFile = SpieltagRanglisteSheetUITest.class
					.getResourceAsStream("runde" + spielrundeNr + ".json");
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			RangeData rundeSpielerNrData = gson.fromJson(new BufferedReader(new InputStreamReader(jsonFile)),
					RangeData.class);

			rngHlpr.setDataInRange(rundeSpielerNrData);

			// Ergebnisse eintragen
			RangePosition rangeSpielErg = RangePosition.from(SpielrundeSheet_Naechste.ERSTE_SPALTE_ERGEBNISSE,
					SpielrundeSheet_Naechste.ERSTE_DATEN_ZEILE, SpielrundeSheet_Naechste.ERSTE_SPALTE_ERGEBNISSE + 1, // Team A + B
					SpielrundeSheet_Naechste.ERSTE_DATEN_ZEILE + 3); // 4 paarungen
			RangeHelper rngHlprSpielErg = RangeHelper.from(spielrunde, wkingSpreadsheet.getWorkingSpreadsheetDocument(),
					rangeSpielErg);

			RangeData ergebnisseDataRunde = new RangeData(getErgebnisse(spielrundeNr));
			rngHlprSpielErg.setDataInRange(ergebnisseDataRunde);

			// validate vertikal ergebnisse
			// writeVertikalToJson(spielrunde, spielrundeNr);
			validatVertikalWithJson(spielrunde, spielrundeNr);
		}

		// Rangliste erstellen
		SpieltagRanglisteSheet ranglist = new SpieltagRanglisteSheet(wkingSpreadsheet);
		ranglist.run();
		// Validate
		writeSpieltagRanglisteToJson(ranglist);

		waitEnter();
	}

	private Object[][] getErgebnisse(int spielrunde) {
		Object[][] testDataRunde = null;
		switch (spielrunde) {
		case 1:
			//@formatter:off
				testDataRunde = new Object [][] {
						{13,10}, 
						{8,13}, 
						{13,6},
						{13,0}
						};
			//@formatter:on
			break;
		case 2:
			//@formatter:off
				testDataRunde = new Object [][] {
						{4,13}, 
						{13,1}, 
						{13,9},
						{13,3}
						};
			//@formatter:on
			break;
		case 3:
			//@formatter:off
				testDataRunde = new Object [][] {
						{13,11}, 
						{4,13}, 
						{0,13},
						{13,7}
						};
			//@formatter:on
			break;
		case 4:
			//@formatter:off
				testDataRunde = new Object [][] {
						{13,12}, 
						{13,5}, 
						{9,13},
						{8,13}
						};
			//@formatter:on
			break;
		}

		return testDataRunde;
	}

	@Test
	@Ignore
	public void generateJsonFilesIntmp() throws GenerateException {
		SpielrundeSheet_Naechste spielrundeSheetNaechste = new SpielrundeSheet_Naechste(wkingSpreadsheet);

		for (int rundeCntr = 1; rundeCntr < 5; rundeCntr++) {
			spielrundeSheetNaechste.run(); // no thread naechste Runde

			XSpreadsheet spielrunde1 = sheetHlp.findByName("1." + rundeCntr + ". Spielrunde");
			assertThat(spielrunde1).isNotNull();

			RangePosition rangeSplrNr = RangePosition.from(SpielrundeSheet_Naechste.ERSTE_SPIELERNR_SPALTE,
					SpielrundeSheet_Naechste.ERSTE_DATEN_ZEILE, SpielrundeSheet_Naechste.ERSTE_SPIELERNR_SPALTE + 5, // Team A + B
					SpielrundeSheet_Naechste.ERSTE_DATEN_ZEILE + 3); // 4 paarungen
			RangeHelper rngHlpr = RangeHelper.from(spielrunde1, wkingSpreadsheet.getWorkingSpreadsheetDocument(),
					rangeSplrNr);
			RangeData runde1SplrNrRange = rngHlpr.getDataFromRange();

			Gson gson = new GsonBuilder().setPrettyPrinting().create();

			try {
				File jsoFile = new File("/home/michael/tmp/runde" + rundeCntr + ".json");
				try (BufferedWriter fileStream = new BufferedWriter(new FileWriter(jsoFile))) {
					fileStream.write(gson.toJson(runde1SplrNrRange));
				}
			} catch (JsonIOException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
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

	private void writeVertikalToJson(XSpreadsheet spielrunde, int rundeCntr) throws GenerateException {
		RangeData vertikalErgRange = getVertikalErgRange(spielrunde).getDataFromRange();

		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		try {
			File jsoFile = new File("/home/michael/tmp/runde" + rundeCntr + "-vertikal.json");
			try (BufferedWriter fileStream = new BufferedWriter(new FileWriter(jsoFile))) {
				fileStream.write(gson.toJson(vertikalErgRange));
			}
		} catch (JsonIOException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void validatVertikalWithJson(XSpreadsheet spielrunde, int rundeCntr) throws GenerateException {

		RangeData vertikalErgRangeVonSpielrunde = getVertikalErgRange(spielrunde).getDataFromRange();

		InputStream jsonFile = SpieltagRanglisteSheetUITest.class
				.getResourceAsStream("runde" + rundeCntr + "-vertikal.json");
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		RangeData refSpielergData = gson.fromJson(new BufferedReader(new InputStreamReader(jsonFile)), RangeData.class);

		assertThat(vertikalErgRangeVonSpielrunde).hasSameSizeAs(refSpielergData);

		int idx = 0;
		// jede zeile vergleichen, wegen fehlermeldung 
		for (RowData data : vertikalErgRangeVonSpielrunde) {
			List<String> expected1 = refSpielergData.get(idx).stream().map(c -> c.getStringVal())
					.collect(Collectors.toList());
			assertThat(data).extracting(CellData::getStringVal).containsExactlyElementsOf(expected1);
			idx++;
		}
	}

	private void writeSpieltagRanglisteToJson(SpieltagRanglisteSheet ranglist) throws GenerateException {
		RangeData vertikalErgRange = getSpieltagRanglisteRange(ranglist).getDataFromRange();

		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		try {
			File jsoFile = new File("/home/michael/tmp/SpieltagRangliste.json");
			try (BufferedWriter fileStream = new BufferedWriter(new FileWriter(jsoFile))) {
				fileStream.write(gson.toJson(vertikalErgRange));
			}
		} catch (JsonIOException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private RangeHelper getSpieltagRanglisteRange(SpieltagRanglisteSheet ranglist) throws GenerateException {
		RangePosition rangeSplrErg = RangePosition.from(ranglist.getErsteSpalte(), ranglist.getErsteDatenZiele(),
				ranglist.getManuellSortSpalte(), ranglist.getLetzteDatenZeile()); // 22 Aktive Spieler Meldungen
		assertThat(rangeSplrErg.getAddress()).isEqualTo("S3:W26");
		RangeHelper rngHlpr = RangeHelper.from(ranglist.getXSpreadSheet(),
				wkingSpreadsheet.getWorkingSpreadsheetDocument(), rangeSplrErg);
		return rngHlpr;
	}

}