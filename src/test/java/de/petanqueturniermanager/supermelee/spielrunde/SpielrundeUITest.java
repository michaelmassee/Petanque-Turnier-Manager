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

import com.sun.star.sheet.XSpreadsheet;

/**
 * Erstellung 17.07.2022 / Michael Massee
 */

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.CellData;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleePropertiesSpalte;
import de.petanqueturniermanager.supermelee.meldeliste.TestMeldeListeErstellen;

/**
 * Mehrere Spielrunden testen + rangliste
 *
 */
public class SpielrundeUITest extends BaseCalcUITest {

	private static final Logger logger = LogManager.getLogger(SpielrundeUITest.class);

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
	public void testSpielrundenOk() throws IOException, GenerateException {
		SpielrundeSheet_Naechste spielrundeSheetNaechste = new SpielrundeSheet_Naechste(wkingSpreadsheet);
		for (int runde = 1; runde < 5; runde++) { // 4 runden
			spielrundeSheetNaechste.run(); // no thread
			validateAnzSpielrundeInMeldeliste(runde, docPropHelper);
		}
		// waitEnter();
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

		// 2 sheets manipulieren um doppelte aulosung zu faken
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

		assertThat(nrzumTauschen.isPresent());
		assertThat(nrzumTauschen.get().isPresent());

		Optional<CellData> nrzumTauschenOpt = nrzumTauschen.get();
		CellData cellDataZumTauschen = nrzumTauschenOpt.get();
		int intValNr = cellDataZumTauschen.getIntVal(-1);

		assertThat(intValNr).isNotEqualTo(-1);

		// spielernr 2 Zelle suchen
		Optional<CellData> splrNr2 = splrNrRange.stream().flatMap(r -> r.stream())
				.filter(c -> c.getIntVal(-1) == nrSpieler2).findFirst();
		assertThat(splrNr2.isPresent());
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
}
