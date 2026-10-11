/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.formulex.spielrunde;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.MeldelisteUuidIndex;
import de.petanqueturniermanager.basesheet.meldeliste.TeamUuidFormel;
import de.petanqueturniermanager.basesheet.meldeliste.TeamnummerAendernSheet;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.formulex.rangliste.FormuleXRanglisteSheet;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.random.RandomSource;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;

/**
 * Teamzellen der Formule-X-Spielrunde tragen die UUID des Teams und zeigen die aktuelle Nummer. Nach „Teamnummer
 * ändern“ zeigt die Runde die neue Nummer, und die Rangliste rechnet das Ergebnis weiter demselben Team zu.
 */
public class FormuleXTeamnummerAendernUITest extends BaseCalcUITest {

	private static final int NEUE_NR = 99;

	@BeforeEach
	@Override
	public void beforeTest() {
		super.beforeTest();
		RandomSource.setSeed(42L);
	}

	@AfterEach
	public void resetRandom() {
		RandomSource.reset();
	}

	@Test
	public void geaenderteNummerErscheintInDerRundeUndDieRanglisteBehaeltDasErgebnis() throws GenerateException {
		new FormuleXTurnierTestDaten(wkingSpreadsheet).generate(1, false);
		XSpreadsheet runde = sheetHlp.findByName(SheetNamen.formulexSpielrunde(1));
		int zeile = FormuleXAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE;
		Position teamA = Position.from(FormuleXAbstractSpielrundeSheet.TEAM_A_SPALTE, zeile);
		String uuidA = TeamUuidFormel.findeUuid(sheetHlp.getFormulaFromCell(runde, teamA)).orElseThrow();
		int alteNr = MeldelisteUuidIndex.lese(wkingSpreadsheet).nr(uuidA).orElseThrow();

		RangeData ergebnis = new RangeData();
		ergebnis.addNewRow(13, 2);
		RangeHelper.from(runde, doc, ergebnis.getRangePosition(
				Position.from(FormuleXAbstractSpielrundeSheet.ERG_TEAM_A_SPALTE, zeile))).setDataInRange(ergebnis);

		// Das Property schreibt sonst erst SheetRunner.run(); der Test ruft doRun() direkt.
		docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM,
				TurnierSystem.FORMULEX.getId());
		new TeamnummerAendernSheet(wkingSpreadsheet).mitEingabe(alteNr, NEUE_NR).doRun();
		recalcAll();

		assertThat(sheetHlp.getTextFromCell(runde, teamA)).as("Die Runde zeigt die neue Nummer")
				.isEqualTo(String.valueOf(NEUE_NR));

		FormuleXRanglisteSheet rangliste = new FormuleXRanglisteSheet(wkingSpreadsheet);
		rangliste.doRun();
		assertThat(siegeVonTeam(rangliste.getXSpreadSheet(), NEUE_NR)).as("Der Sieg gehört dem Team mit der neuen Nummer").isEqualTo(1);
		assertThat(siegeVonTeam(rangliste.getXSpreadSheet(), alteNr)).as("Die alte Nummer gibt es nicht mehr").isEqualTo(-1);
	}

	/** @return Siege des Teams laut Rangliste, -1 wenn die Nummer nicht vorkommt */
	private int siegeVonTeam(XSpreadsheet rangliste, int teamNr) throws GenerateException {
		RangeData daten = RangeHelper.from(rangliste, doc, RangePosition.from(FormuleXRanglisteSheet.TEAM_NR_SPALTE,
				FormuleXRanglisteSheet.ERSTE_DATEN_ZEILE, FormuleXRanglisteSheet.SIEGE_SPALTE,
				FormuleXRanglisteSheet.ERSTE_DATEN_ZEILE + FormuleXTurnierTestDaten.ANZ_TEAMS)).getDataFromRange();
		int spalteSiege = FormuleXRanglisteSheet.SIEGE_SPALTE - FormuleXRanglisteSheet.TEAM_NR_SPALTE;
		return daten.stream().filter(z -> z.get(0).getIntVal(0) == teamNr).findFirst()
				.map(z -> z.get(spalteSiege).getIntVal(0)).orElse(-1);
	}
}
