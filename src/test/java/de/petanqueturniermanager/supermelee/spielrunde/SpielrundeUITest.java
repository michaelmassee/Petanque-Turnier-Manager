package de.petanqueturniermanager.supermelee.spielrunde;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.IntStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import com.google.common.primitives.Ints;
import com.sun.star.sheet.XSpreadsheet;

/**
 * Erstellung 17.07.2022 / Michael Massee
 */

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.CellData;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeMode;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleePropertiesSpalte;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_New;
import de.petanqueturniermanager.supermelee.meldeliste.TestSuperMeleeMeldeListeErstellen;

/**
 * Mehrere Spielrunden testen + rangliste
 *
 */
public class SpielrundeUITest extends BaseCalcUITest {

	private static final Logger logger = LogManager.getLogger(SpielrundeUITest.class);
	private MeldeListeSheet_New meldeListeSheetNew;

	@Before
	public void testMeldeListeErstelln() throws GenerateException {
		// erst mal eine meldeListe erstellen
		TestSuperMeleeMeldeListeErstellen testMeldeListe = new TestSuperMeleeMeldeListeErstellen(wkingSpreadsheet, doc);
		testMeldeListe.run();
		meldeListeSheetNew = testMeldeListe.getMeldeListeSheetNew();

	}

	@Test
	public void testSpielrundenOk() throws IOException, GenerateException {
		SpielrundeSheet_Naechste spielrundeSheetNaechste = new SpielrundeSheet_Naechste(wkingSpreadsheet);
		for (int runde = 1; runde < 5; runde++) { // 4 runden
			spielrundeSheetNaechste.run(); // no thread
			validateAnzSpielrundeInMeldeliste(runde, docPropHelper);
			Position letztePositionRechtsUnten = spielrundeSheetNaechste.letztePositionRechtsUnten(); // I6
			assertThat(letztePositionRechtsUnten.getAddress()).isEqualTo("I6");
		}

		// die maximale anzahl an Spieltage die bei der neu auslosung eingelesen werden
		assertThat(spielrundeSheetNaechste.getMaxAnzGespielteSpieltage()).isEqualTo(99);
		assertThat(spielrundeSheetNaechste.getSpielTag().getNr()).isEqualTo(1);
		assertThat(docPropHelper.getIntProperty(SuperMeleePropertiesSpalte.KONFIG_PROP_NAME_SPIELTAG, 999))
				.isEqualTo(1);

		int anzSpielRunden = new SpielrundeSheet_Update(wkingSpreadsheet)
				.countNumberOfSpielRundenSheets(spielrundeSheetNaechste.getSpielTag());
		assertThat(anzSpielRunden).isEqualTo(4);

		//waitEnter();
	}

	@Test
	public void testSpielrundeDoublette() throws IOException, GenerateException {
		docPropHelper.setStringProperty(SuperMeleePropertiesSpalte.KONFIG_PROP_SUPERMELEE_MODE,
				SuperMeleeMode.Doublette.getKey());
		SpielrundeSheet_Naechste spielrundeSheetNaechste = new SpielrundeSheet_Naechste(wkingSpreadsheet);
		spielrundeSheetNaechste.run(); // no thread

		Position letztePositionRechtsUnten = spielrundeSheetNaechste.letztePositionRechtsUnten();
		assertThat(letztePositionRechtsUnten.getAddress()).isEqualTo("I7");

		// paarungen einlesen
		XSpreadsheet spielrunde1 = sheetHlp.findByName("1.1. Spielrunde");
		assertThat(spielrunde1).isNotNull();

		RangePosition rangeSplrNr = RangePosition.from(SpielrundeSheet_Naechste.ERSTE_SPIELERNR_SPALTE,
				SpielrundeSheet_Naechste.ERSTE_DATEN_ZEILE, SpielrundeSheet_Naechste.ERSTE_SPIELERNR_SPALTE + 5, // Team A + B
				SpielrundeSheet_Naechste.ERSTE_DATEN_ZEILE + 4); // 5 paarungen weil Doublette mode
		RangeHelper rngHlpr = RangeHelper.from(spielrunde1, wkingSpreadsheet.getWorkingSpreadsheetDocument(),
				rangeSplrNr);
		RangeData data = rngHlpr.getDataFromRange();
		assertThat(data).isNotNull().isNotEmpty().hasSize(5);
		// paarungen 1-3 nur Doublette
		for (int i = 0; i < 4; i++) {
			assertThat(data.get(i).get(0).getStringVal()).isNotBlank();
			assertThat(data.get(i).get(1).getStringVal()).isNotBlank();
			assertThat(data.get(i).get(2).getStringVal()).isBlank();
			assertThat(data.get(4).get(3).getStringVal()).isNotBlank();
			assertThat(data.get(4).get(4).getStringVal()).isNotBlank();
			assertThat(data.get(i).get(5).getStringVal()).isBlank();
		}

		// paarungen 5 Tripeltes
		assertThat(data.get(4).get(0).getStringVal()).isNotBlank();
		assertThat(data.get(4).get(1).getStringVal()).isNotBlank();
		assertThat(data.get(4).get(2).getStringVal()).isNotBlank();
		assertThat(data.get(4).get(3).getStringVal()).isNotBlank();
		assertThat(data.get(4).get(4).getStringVal()).isNotBlank();
		assertThat(data.get(4).get(5).getStringVal()).isNotBlank();
		// waitEnter();
	}

	/**
	 * testet in Triplette modus ob nur Doublette Runde möglich
	 * 
	 * @throws IOException
	 * @throws GenerateException
	 */
	@Test
	public void testSpielrundeModeTripletteNurDoublettesRunde() throws IOException, GenerateException {
		docPropHelper.setStringProperty(SuperMeleePropertiesSpalte.KONFIG_PROP_SUPERMELEE_MODE,
				SuperMeleeMode.Triplette.getKey());
		docPropHelper.setBooleanProperty(SuperMeleePropertiesSpalte.KONFIG_PROP_FRAGE_GLEICHE_PAARUNGEN, true);

		// erste 2 Meldungen auf inaktiv damit nur doublette paarungen moeglich
		int ersteDatenZeile = meldeListeSheetNew.getMeldungenSpalte().getErsteDatenZiele();
		int spielerNameErsteSpalte = meldeListeSheetNew.getMeldungenSpalte().getErsteMeldungNameSpalte();
		int aktivSpalte = spielerNameErsteSpalte + 2;
		sheetHlp.clearValInCell(meldeListeSheetNew.getXSpreadSheet(), Position.from(aktivSpalte, ersteDatenZeile));
		sheetHlp.clearValInCell(meldeListeSheetNew.getXSpreadSheet(), Position.from(aktivSpalte, ersteDatenZeile + 1));

		SpielrundeSheet_Naechste spielrundeSheetNaechste = new SpielrundeSheet_Naechste(wkingSpreadsheet);
		spielrundeSheetNaechste.setForceOk(true); // DialogBox returns OK
		spielrundeSheetNaechste.run(); // no thread 

		Position letztePositionRechtsUnten = spielrundeSheetNaechste.letztePositionRechtsUnten();
		assertThat(letztePositionRechtsUnten.getAddress()).isEqualTo("I7");

		// paarungen einlesen
		XSpreadsheet spielrunde1 = sheetHlp.findByName("1.1. Spielrunde");
		assertThat(spielrunde1).isNotNull();

		RangePosition rangeSplrNr = RangePosition.from(SpielrundeSheet_Naechste.ERSTE_SPIELERNR_SPALTE,
				SpielrundeSheet_Naechste.ERSTE_DATEN_ZEILE, SpielrundeSheet_Naechste.ERSTE_SPIELERNR_SPALTE + 5, // Team A + B
				SpielrundeSheet_Naechste.ERSTE_DATEN_ZEILE + 4); // 5 paarungen weil Doublette mode
		RangeHelper rngHlpr = RangeHelper.from(spielrunde1, wkingSpreadsheet.getWorkingSpreadsheetDocument(),
				rangeSplrNr);
		RangeData data = rngHlpr.getDataFromRange();
		assertThat(data).isNotNull().isNotEmpty().hasSize(5);
		// paarungen 1-3 nur Doublette
		for (int i = 0; i < 5; i++) {
			assertThat(data.get(i).get(0).getStringVal()).isNotBlank();
			assertThat(data.get(i).get(1).getStringVal()).isNotBlank();
			assertThat(data.get(i).get(2).getStringVal()).isBlank();
			assertThat(data.get(4).get(3).getStringVal()).isNotBlank();
			assertThat(data.get(4).get(4).getStringVal()).isNotBlank();
			assertThat(data.get(i).get(5).getStringVal()).isBlank();
		}
	}

	/**
	 * 
	 * @param runde
	 * @param docPropHelper
	 */

	private void validateAnzSpielrundeInMeldeliste(int runde, DocumentPropertiesHelper docPropHelper) {
		int spielrunde = docPropHelper.getIntProperty(SuperMeleePropertiesSpalte.KONFIG_PROP_NAME_SPIELRUNDE, -1);
		assertThat(runde).isEqualTo(spielrunde);

	}

	@Test
	public void testSpielrundenError() throws IOException, GenerateException {
		SpielrundeSheet_Naechste spielrundeSheetNaechste = new SpielrundeSheet_Naechste(wkingSpreadsheet) {
			@Override
			protected void handleGenerateException(GenerateException e) {
				// keine dialog box
				throw new RuntimeException(e);
			}
		};
		for (int runde = 1; runde < 4; runde++) { // 3 runden
			spielrundeSheetNaechste.run(); // no thread
			validateAnzSpielrundeInMeldeliste(runde, docPropHelper);
		}

		// 2 sheets manipulieren um doppelte auslosung zu faken
		XSpreadsheet spielrunde2 = sheetHlp.findByName("1.2. Spielrunde");
		assertThat(spielrunde2).isNotNull();

		Position posErsteSpielrNr = Position.from(SpielrundeSheet_Naechste.ERSTE_SPIELERNR_SPALTE,
				SpielrundeSheet_Naechste.ERSTE_DATEN_ZEILE);
		posErsteSpielrNr.zeilePlusEins();

		Integer nrSpieler1 = sheetHlp.getIntFromCell(spielrunde2, posErsteSpielrNr);
		logger.info("nrSpieler1 " + nrSpieler1 + " Pos" + posErsteSpielrNr.getAddress());

		Integer nrSpieler2 = sheetHlp.getIntFromCell(spielrunde2, posErsteSpielrNr.spaltePlusEins());
		logger.info("nrSpieler2 " + nrSpieler2 + " Pos" + posErsteSpielrNr.getAddress());

		XSpreadsheet spielrunde3 = sheetHlp.findByName("1.3. Spielrunde");
		assertThat(spielrunde3).isNotNull();

		// data grid einlesen mit spieler Nr
		RangePosition rangeSplrNr = RangePosition.from(SpielrundeSheet_Naechste.ERSTE_SPIELERNR_SPALTE,
				SpielrundeSheet_Naechste.ERSTE_DATEN_ZEILE, SpielrundeSheet_Naechste.ERSTE_SPIELERNR_SPALTE + 5, // Team A + B
				SpielrundeSheet_Naechste.ERSTE_DATEN_ZEILE + 3); // 4 paarungen
		RangeHelper rngHlpr = RangeHelper.from(spielrunde3, wkingSpreadsheet.getWorkingSpreadsheetDocument(),
				rangeSplrNr);
		RangeData splrNrRange = rngHlpr.getDataFromRange();

		// finde die nachbar Zelle spielrnr zum tauschen mit nrSpieler2
		Optional<Optional<CellData>> nrzumTauschen = IntStream.range(0, 5).filter(i -> { // in welche spalte ? 
			return splrNrRange.stream().filter(r -> r.get(i).getIntVal(-1) == nrSpieler1).findFirst().isPresent();
		}).mapToObj(i -> {
			return splrNrRange.stream().filter(r -> r.get(i).getIntVal(-1) == nrSpieler1).findFirst().map(r -> {
				if (i == 0 || i == 3) {
					return r.get(i + 1);
				}
				return r.get(i - 1);
			});
		}).findFirst();

		assertThat(nrzumTauschen).isPresent();
		assertThat(nrzumTauschen.get()).isPresent();

		Optional<CellData> nrzumTauschenOpt = nrzumTauschen.get();
		CellData cellDataZumTauschen = nrzumTauschenOpt.get();
		int intValNr = cellDataZumTauschen.getIntVal(-1);

		assertThat(intValNr).isNotEqualTo(-1);

		// spielernr 2 Zelle suchen
		Optional<CellData> splrNr2 = splrNrRange.stream().flatMap(r -> r.stream())
				.filter(c -> c.getIntVal(-1) == nrSpieler2).findFirst();
		assertThat(splrNr2).isPresent();
		// wow endlich tauschen
		logger.info("switch " + intValNr + " with " + nrSpieler2);
		splrNr2.get().setIntVal(intValNr);
		cellDataZumTauschen.setIntVal(nrSpieler2);

		// daten wieder zurueck schreiben
		rngHlpr.setDataInRange(splrNrRange);

		// waitEnter();
		// weitere runde muss fehlermeldung 
		try {
			spielrundeSheetNaechste.run(); // no thread
			fail("Excepton doppelter auslosung fehlt");
		} catch (Exception e) {
			assertThat(e).hasCauseInstanceOf(GenerateException.class);
		}
		// rundenr muss weiter gezaehlt werden ! weil Runde wurde trotz Validate Fehler erstellt
		validateAnzSpielrundeInMeldeliste(4, docPropHelper);
		// waitEnter();
	}

	@Test
	public void testSpielrundeErsteSpalteMitUnterschiedlicheBahnKonfiguration() throws IOException, GenerateException {
		SpielrundeSheet_Naechste spielrundeSheetNaechste = new SpielrundeSheet_Naechste(wkingSpreadsheet);
		Position headerpos = Position.from(0, 0);

		spielrundeSheetNaechste.run(); // no thread
		// default ist nur lfd nr A3:A6
		RangePosition rangeNrSpalte = RangePosition.from(SpielrundeSheet_Naechste.NUMMER_SPALTE_RUNDESPIELPLAN,
				SpielrundeSheet_Naechste.ERSTE_DATEN_ZEILE, SpielrundeSheet_Naechste.NUMMER_SPALTE_RUNDESPIELPLAN,
				SpielrundeSheet_Naechste.ERSTE_DATEN_ZEILE + 4); // 4 paarungen + leer zeile

		XSpreadsheet spielrunde1 = sheetHlp.findByName("1.1. Spielrunde");
		assertThat(spielrunde1).isNotNull();
		RangeHelper rngHlpr = RangeHelper.from(spielrunde1, wkingSpreadsheet.getWorkingSpreadsheetDocument(),
				rangeNrSpalte);
		RangeData nrData = rngHlpr.getDataFromRange();
		assertThat(nrData).isNotNull().isNotEmpty().hasSize(5);
		assertThat(nrData).extracting(t -> t.get(0).getIntVal(-1)).containsExactly(1, 2, 3, 4, -1);
		String headerStr = sheetHlp.getTextFromCell(spielrunde1, headerpos);
		assertThat(headerStr).isEmpty();

		//**************************************************************************************************

		assertThat(spielrundeSheetNaechste.getKonfigurationSheet().getSpielrundeSpielbahn())
				.isEqualTo(SpielrundeSpielbahn.X);

		spielrundeSheetNaechste.getKonfigurationSheet().setSpielrundeSpielbahn(SpielrundeSpielbahn.L); // leere spalte
		spielrundeSheetNaechste.run(); // no thread
		XSpreadsheet spielrunde2 = sheetHlp.findByName("1.2. Spielrunde");
		assertThat(spielrunde2).isNotNull();
		headerStr = sheetHlp.getTextFromCell(spielrunde2, headerpos);
		assertThat(headerStr).isNotNull().isEqualTo("Bahn");

		//**************************************************************************************************		

		spielrundeSheetNaechste.getKonfigurationSheet().setSpielrundeSpielbahn(SpielrundeSpielbahn.N); // Durchnummerieren
		spielrundeSheetNaechste.run(); // no thread
		XSpreadsheet spielrunde3 = sheetHlp.findByName("1.3. Spielrunde");
		assertThat(spielrunde3).isNotNull();
		nrData = rngHlpr.getDataFromRange();
		assertThat(nrData).isNotNull().isNotEmpty().hasSize(5);
		assertThat(nrData).extracting(t -> t.get(0).getIntVal(-1)).containsExactly(1, 2, 3, 4, -1);
		headerStr = sheetHlp.getTextFromCell(spielrunde3, headerpos);
		assertThat(headerStr).isNotNull().isEqualTo("Bahn");

		//**************************************************************************************************

		spielrundeSheetNaechste.getKonfigurationSheet().setSpielrundeSpielbahn(SpielrundeSpielbahn.R); // random
		spielrundeSheetNaechste.run(); // no thread
		XSpreadsheet spielrunde4 = sheetHlp.findByName("1.4. Spielrunde");
		assertThat(spielrunde4).isNotNull();
		nrData = rngHlpr.getDataFromRange();
		assertThat(nrData).isNotNull().isNotEmpty().hasSize(5);
		assertThat(nrData).extracting(t -> t.get(0).getIntVal(-1)).containsAll(Ints.asList(1, 2, 3, 4, -1));
		headerStr = sheetHlp.getTextFromCell(spielrunde4, headerpos);
		assertThat(headerStr).isNotNull().isEqualTo("Bahn");

		// waitEnter();

	}

}
