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
import java.util.Optional;
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
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.ergebnis.SpielerSpieltagErgebnis;
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
	public void testRanglisteOK() throws GenerateException, IOException {

		erstelleTestSpielrunden(4, true);

		// Rangliste erstellen
		SpieltagRanglisteSheet ranglist = new SpieltagRanglisteSheet(wkingSpreadsheet);
		ranglist.run();

		assertThat(ranglist.getAnzahlRunden()).isEqualTo(4);
		assertThat(ranglist.sucheLetzteZeileMitSpielerNummer()).isEqualTo(25);
		assertThat(ranglist.getLetzteMitDatenZeileInSpielerNrSpalte()).isEqualTo(28);
		assertThat(ranglist.getRangListeSpalte().getRangListeSpalte()).isEqualTo(2);
		assertThat(ranglist.getErsteDatenZiele()).isEqualTo(3);
		assertThat(ranglist.getManuellSortSpalte()).isEqualTo(18);
		assertThat(ranglist.countNumberOfRanglisten()).isEqualTo(1);

		// Validate
		// writeSpieltagRanglisteToJson(ranglist);
		validateSpieltagRanglisteToJson(ranglist, 1);

		validateSpielTagErgebnisseEinlesen(ranglist);
		// waitEnter();
	}

	/**
	 * Wenn eine neue Spielrunde und Rangliste vorhanden, dann muss die Rangliste mit der neue Spielrunde, neu erstellt werden.
	 * 
	 * @throws GenerateException
	 * @throws IOException
	 */

	@Test
	public void testUpdateRanglisteWennNeueSpielrunde() throws GenerateException, IOException {
		SpielrundeSheet_Naechste spielrundeSheetNaechste = new SpielrundeSheet_Naechste(wkingSpreadsheet);
		erstelleTestSpielrunden(2, false);

		SpieltagRanglisteSheet ranglist = new SpieltagRanglisteSheet(wkingSpreadsheet);
		ranglist.run();// rangliste erstellen
		assertThat(ranglist.countNumberOfSpielrundenInSheet()).isEqualTo(2);

		// testen ob rangliste mit neue Spieltag aktualisiert wird
		// Weitere leeren Spielrunde erstellen, mit rangliste update !
		spielrundeSheetNaechste.run(); // 3 Runde ohne Daten !!!
		assertThat(ranglist.countNumberOfSpielrundenInSheet()).isEqualTo(3);

		// sortieren
		new SpieltagRanglisteSheet_SortOnly(wkingSpreadsheet).run();
		// validieren
		// writeSpieltagRanglisteToJson(ranglist);
		validateSpieltagRanglisteToJson(ranglist, 2);
		// waitEnter();
	}

	@Test
	public void testSortRanglisteMitUngleicheAnzahlAnSpielrunden() throws GenerateException, IOException {
		SpielrundeSheet_Naechste spielrundeSheetNaechste = new SpielrundeSheet_Naechste(wkingSpreadsheet);
		erstelleTestSpielrunden(2, false);

		SpieltagRanglisteSheet ranglist = new SpieltagRanglisteSheet(wkingSpreadsheet);
		ranglist.run();// rangliste erstellen

		spielrundeSheetNaechste.run(); // 3 Runde ohne Daten !!!

		assertThat(ranglist.countNumberOfSpielrundenInSheet()).isEqualTo(3);

		// sortieren
		SpieltagRanglisteSheet_SortOnly spieltagRanglisteSheet_SortOnly = new SpieltagRanglisteSheet_SortOnly(
				wkingSpreadsheet);
		spieltagRanglisteSheet_SortOnly.run();

		assertThat(spieltagRanglisteSheet_SortOnly
				.istDieAnzahlSpieltageInDerRanglisteGleichMitDerAnzahlderSpieltagesheets()).isTrue();

		// 1 Spieltag 1 SpielRunde löschen
		String spielrundeName = spielrundeSheetNaechste.getSheetName(SpielTagNr.from(1), SpielRundeNr.from(1));
		XSpreadsheet spielrunde1 = sheetHlp.findByName(spielrundeName);
		assertThat(spielrunde1).isNotNull();
		sheetHlp.removeSheet(spielrundeName);
		spielrunde1 = sheetHlp.findByName(spielrundeName);
		assertThat(spielrunde1).isNull();

		// sortieren
		// spieltagRanglisteSheet_SortOnly.run(); fehler box
		assertThat(spieltagRanglisteSheet_SortOnly
				.istDieAnzahlSpieltageInDerRanglisteGleichMitDerAnzahlderSpieltagesheets()).isFalse();

		// waitEnter();
	}

	private void erstelleTestSpielrunden(int anzRunden, boolean validateRunden) throws GenerateException {
		SpielrundeSheet_Naechste spielrundeSheetNaechste = new SpielrundeSheet_Naechste(wkingSpreadsheet);

		for (int spielrundeNr = 1; spielrundeNr <= anzRunden; spielrundeNr++) {

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
			if (validateRunden) {
				validatVertikalWithJson(spielrunde, spielrundeNr);
			}
		}
	}

	private void validateSpielTagErgebnisseEinlesen(SpieltagRanglisteSheet ranglist) throws GenerateException {
		List<SpielerSpieltagErgebnis> ergebnisse = ranglist.spielTagErgebnisseEinlesen();
		assertThat(ergebnisse.size()).isEqualTo(23); // 23 aktive meldungen

		// stichprobe 10 = Hoffmann, Arne
		Optional<SpielerSpieltagErgebnis> spieler10 = ergebnisse.stream().filter(sp -> {
			return sp.getSpielerNr() == 10;
		}).findFirst();

		assertThat(spieler10).isPresent();

		SpielerSpieltagErgebnis spieler10SpieltagErgebnis = spieler10.get();
		assertThat(spieler10SpieltagErgebnis.getPosPunktePlus().getAddress()).isEqualTo("O5");
		assertThat(spieler10SpieltagErgebnis.getPosPunkteMinus().getAddress()).isEqualTo("P5");
		assertThat(spieler10SpieltagErgebnis.getPunktePlus()).isEqualTo(49);
		assertThat(spieler10SpieltagErgebnis.getPunkteMinus()).isEqualTo(27);
		assertThat(spieler10SpieltagErgebnis.getPunkteDiv()).isEqualTo(22);

		assertThat(spieler10SpieltagErgebnis.getPosSpielPlus().getAddress()).isEqualTo("L5");
		assertThat(spieler10SpieltagErgebnis.getPosSpielMinus().getAddress()).isEqualTo("M5");
		assertThat(spieler10SpieltagErgebnis.getSpielPlus()).isEqualTo(3);
		assertThat(spieler10SpieltagErgebnis.getSpielMinus()).isEqualTo(1);
		assertThat(spieler10SpieltagErgebnis.getSpielDiv()).isEqualTo(2);
		assertThat(spieler10SpieltagErgebnis.getSpielTagNr()).isEqualTo(1);
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

	private void validateSpieltagRanglisteToJson(SpieltagRanglisteSheet ranglist, int ranglisteNr)
			throws GenerateException {
		logger.info("validateSpieltagRanglisteToJson");

		RangeHelper spieltagRanglisteRange = getSpieltagRanglisteRange(ranglist);
		if (ranglisteNr == 1) {
			assertThat(spieltagRanglisteRange.getRangePos().getAddress()).isEqualTo("A4:V26");
		}
		RangeData spieltagRangliste = spieltagRanglisteRange.getDataFromRange();

		InputStream jsonFile = SpieltagRanglisteSheetUITest.class
				.getResourceAsStream("SpieltagRangliste_" + ranglisteNr + ".json");
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		RangeData refspieltagRangliste = gson.fromJson(new BufferedReader(new InputStreamReader(jsonFile)),
				RangeData.class);

		assertThat(spieltagRangliste).hasSameSizeAs(refspieltagRangliste);

		int idx = 0;
		// jede zeile vergleichen, wegen fehlermeldung 
		for (RowData data : spieltagRangliste) {
			List<String> expected = refspieltagRangliste.get(idx).stream().map(c -> c.getStringVal())
					.collect(Collectors.toList());
			logger.info("Validate Zeile :" + expected);
			assertThat(data).extracting(CellData::getStringVal).containsExactlyElementsOf(expected);
			idx++;
		}
	}

	private RangeHelper getSpieltagRanglisteRange(SpieltagRanglisteSheet ranglist) throws GenerateException {
		RangePosition rangeSplrErg = RangePosition.from(ranglist.getErsteSpalte(), ranglist.getErsteDatenZiele(),
				ranglist.getManuellSortSpalte() + 3, ranglist.sucheLetzteZeileMitSpielerNummer()); // 22 Aktive Spieler Meldungen
		RangeHelper rngHlpr = RangeHelper.from(ranglist.getXSpreadSheet(),
				wkingSpreadsheet.getWorkingSpreadsheetDocument(), rangeSplrErg);
		return rngHlpr;
	}

}