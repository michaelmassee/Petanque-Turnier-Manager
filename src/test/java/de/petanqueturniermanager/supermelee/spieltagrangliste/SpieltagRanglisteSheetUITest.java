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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
import de.petanqueturniermanager.supermelee.RanglisteTestDaten;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.ergebnis.SpielerSpieltagErgebnis;
import de.petanqueturniermanager.supermelee.meldeliste.TestSuperMeleeMeldeListeErstellen;
import de.petanqueturniermanager.supermelee.spielrunde.SpielrundeSheet_Naechste;

public class SpieltagRanglisteSheetUITest extends BaseCalcUITest {

	private static final Logger logger = LogManager.getLogger(SpieltagRanglisteSheetUITest.class);

	private DocumentPropertiesHelper docPropHelper;
	private RanglisteTestDaten<SpieltagRanglisteSheetUITest> ranglisteTestDaten;

	@BeforeEach
	public void testMeldeListeErstelln() throws GenerateException {
		// erst mal eine meldeListe erstellen
		TestSuperMeleeMeldeListeErstellen testMeldeListeErstellen = new TestSuperMeleeMeldeListeErstellen(
				wkingSpreadsheet, doc);
		testMeldeListeErstellen.run();
		this.docPropHelper = new DocumentPropertiesHelper(wkingSpreadsheet);
		docPropHelper.setBooleanProperty(BasePropertiesSpalte.KONFIG_PROP_ZEIGE_ARBEITS_SPALTEN, true);
		ranglisteTestDaten = new RanglisteTestDaten<>(wkingSpreadsheet, sheetHlp, this);
	}

	@Test
	public void testRanglisteOK() throws GenerateException, IOException {
		ranglisteTestDaten.erstelleTestSpielrunden(4, true);

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
		ranglisteTestDaten.erstelleTestSpielrunden(2, false);

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
		ranglisteTestDaten.erstelleTestSpielrunden(2, false);

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

	private void validateSpielTagErgebnisseEinlesen(SpieltagRanglisteSheet ranglist) throws GenerateException {
		List<SpielerSpieltagErgebnis> ergebnisse = ranglist.spielTagErgebnisseEinlesen();
		assertThat(ergebnisse).hasSize(23); // 23 aktive meldungen

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