package de.petanqueturniermanager.supermelee.spielrunde;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;

import org.junit.Before;
import org.junit.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleePropertiesSpalte;
import de.petanqueturniermanager.supermelee.meldeliste.TestMeldeListeErstellen;

public class SpielrundePlanUITest extends BaseCalcUITest {

	private TestMeldeListeErstellen testMeldeListe;

	@Before
	public void testMeldeListeErstelln() throws GenerateException {
		testMeldeListe = new TestMeldeListeErstellen(wkingSpreadsheet, doc);
	}

	@Test
	public void testDoRun60() throws Exception {
		testMeldeListe.initMitAlleDieSpielen(30);
		testMeldeListe.addMitAlleDieSpielen(30);

		SpielrundeSheet_Naechste spielrundeSheetNaechste = new SpielrundeSheet_Naechste(wkingSpreadsheet);
		spielrundeSheetNaechste.run();

		assertThat(docPropHelper.getIntProperty(SuperMeleePropertiesSpalte.KONFIG_PROP_NAME_SPIELTAG, 0)).isEqualTo(1);
		assertThat(docPropHelper.getIntProperty(SuperMeleePropertiesSpalte.KONFIG_PROP_NAME_SPIELRUNDE, 0))
				.isEqualTo(1);

		// feste Spielpaarungen wegen validierung
		InputStream jsonFile = SpielrundePlanUITest.class.getResourceAsStream("Spielrunde60Meldungen.json");
		new SpielrundeUIHelper(this).restoreSpielPaarungenFromJson(1, sheetHlp, wkingSpreadsheet, jsonFile);

		SpielrundePlan spielrundePlan = new SpielrundePlan(wkingSpreadsheet);
		spielrundePlan.run();

		// validate
		XSpreadsheet spielrundeplan1 = sheetHlp.findByName("1.1. " + SpielrundePlan.PREFIX_SHEET_NAMEN);
		assertThat(spielrundeplan1).isNotNull();

		RangePosition rangeSplrPlan = RangePosition.from(0, 0, 8, 32); // A1 I(8)33
		RangeHelper rngHlpr = RangeHelper.from(spielrundeplan1, wkingSpreadsheet.getWorkingSpreadsheetDocument(),
				rangeSplrPlan);
		RangeData data = rngHlpr.getDataFromRange();
		// writeToJson("SpielrundePlan60Meldungen", data);
		InputStream jsonFileSpielrundePlan60Meldungen = SpielrundePlanUITest.class
				.getResourceAsStream("SpielrundePlan60Meldungen.json");
		validateWithJson(data, jsonFileSpielrundePlan60Meldungen);

		// waitEnter();
	}

	@Test
	public void testDoRunUnten30() throws Exception {
		testMeldeListe.initMitAlleDieSpielen(28);

		SpielrundeSheet_Naechste spielrundeSheetNaechste = new SpielrundeSheet_Naechste(wkingSpreadsheet);
		spielrundeSheetNaechste.run();

		assertThat(docPropHelper.getIntProperty(SuperMeleePropertiesSpalte.KONFIG_PROP_NAME_SPIELTAG, 0)).isEqualTo(1);
		assertThat(docPropHelper.getIntProperty(SuperMeleePropertiesSpalte.KONFIG_PROP_NAME_SPIELRUNDE, 0))
				.isEqualTo(1);

		// 2 Spalten
		docPropHelper.setIntProperty(SuperMeleePropertiesSpalte.KONFIG_PROP_ANZ_SPIELER_IN_SPALTE, 15);

		SpielrundePlan spielrundePlan = new SpielrundePlan(wkingSpreadsheet);
		spielrundePlan.run();

		// waitEnter();
		// stichprobe
		Position letzteZeile = Position.from(0, 16); // A17 = letzte zeile
		Integer letzteZeileNr = sheetHlp.getIntFromCell(spielrundePlan.getXSpreadSheet(), letzteZeile);
		assertThat(letzteZeileNr).isNotNull().isGreaterThan(0);

		Position leereZeile = Position.from(0, 17);
		Integer leereZeileNr = sheetHlp.getIntFromCell(spielrundePlan.getXSpreadSheet(), leereZeile); // A19 = muss leer sein
		assertThat(leereZeileNr).isNotNull().isEqualTo(-1);

		// waitEnter();

	}
}