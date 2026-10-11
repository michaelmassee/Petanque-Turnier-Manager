/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.maastrichter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.meldeliste.MeldelisteUuidIndex;
import de.petanqueturniermanager.basesheet.meldeliste.TeamAnzeige;
import de.petanqueturniermanager.basesheet.meldeliste.TeamUuidFormel;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.random.RandomSource;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.schweizer.rangliste.SchweizerRanglisteSheet;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerAbstractSpielrundeSheet;

/**
 * Maastrichter-Vorrunden nutzen die Schweizer Spielrunden. Mit Namensanzeige müssen auch hier die
 * Teamzellen die UUID des Teams tragen, die Folgerunde, Vorrunden-Rangliste und Finalrunden-Einteilung
 * auswerten.
 */
public class MaastrichterVorrundeNamensanzeigeUITest extends BaseCalcUITest {

	private static final int ANZ_TEAMS = 12;
	private static final int ANZ_VORRUNDEN = 2;
	private static final int GRUPPEN_GROESSE = 16;

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
	public void vorrundenMitSpielernamenWerdenUeberDieTeamUuidAusgewertet() throws GenerateException {
		var testDaten = new MaastrichterTurnierTestDaten(wkingSpreadsheet, ANZ_TEAMS, ANZ_VORRUNDEN,
				GRUPPEN_GROESSE).mitSpielplanTeamAnzeige(TeamAnzeige.SPIELERNAMEN);
		testDaten.generate(ANZ_VORRUNDEN, true);

		for (int runde = 1; runde <= ANZ_VORRUNDEN; runde++) {
			XSpreadsheet vorrunde = SheetMetadataHelper.findeSheetUndHeile(
					wkingSpreadsheet.getWorkingSpreadsheetDocument(),
					SheetMetadataHelper.schluesselMaastrichterVorrunde(runde), SheetNamen.spielrunde(runde));
			assertThat(vorrunde).as("Vorrunde %d muss vorhanden sein", runde).isNotNull();
			pruefeVorrunde(vorrunde, runde);
		}

		XSpreadsheet rangliste = testDaten.ranglisteSheet.getXSpreadSheet();
		assertThat(rangliste).as("Die Vorrunden-Rangliste muss erzeugt werden").isNotNull();
		assertThat(summeSiege(rangliste))
				.as("Die Vorrunden-Rangliste muss die Siege aller Vorrunden über die Team-UUIDs zählen")
				.isEqualTo(ANZ_VORRUNDEN * ANZ_TEAMS / 2);
	}

	private void pruefeVorrunde(XSpreadsheet vorrunde, int runde) throws GenerateException {
		assertThat(sheetHlp.getTextFromCell(vorrunde, Position.from(SchweizerAbstractSpielrundeSheet.TEAM_A_SPALTE,
				SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE)))
				.as("Vorrunde %d muss zusammengesetzte Spielernamen anzeigen", runde)
				.contains(" / ");

		MeldelisteUuidIndex uuidIndex = MeldelisteUuidIndex.lese(wkingSpreadsheet);
		String[][] teamFormeln = sheetHlp.getFormulaArrayFromRange(vorrunde, RangePosition.from(
				SchweizerAbstractSpielrundeSheet.TEAM_A_SPALTE, SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE,
				SchweizerAbstractSpielrundeSheet.TEAM_B_SPALTE,
				SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE + ANZ_TEAMS / 2 - 1));
		for (String[] paarung : teamFormeln) {
			for (String formel : paarung) {
				String uuid = TeamUuidFormel.findeUuid(formel).orElse("");
				assertThat(uuidIndex.nr(uuid).orElse(0))
						.as("Vorrunde %d: Teamzelle muss die UUID einer Meldung tragen: %s", runde, formel)
						.isBetween(1, ANZ_TEAMS);
			}
		}
	}

	private int summeSiege(XSpreadsheet rangliste) throws GenerateException {
		RangeData siege = RangeHelper.from(rangliste, wkingSpreadsheet.getWorkingSpreadsheetDocument(),
				RangePosition.from(SchweizerRanglisteSheet.SIEGE_SPALTE, SchweizerRanglisteSheet.ERSTE_DATEN_ZEILE,
						SchweizerRanglisteSheet.SIEGE_SPALTE, SchweizerRanglisteSheet.ERSTE_DATEN_ZEILE + ANZ_TEAMS - 1))
				.getDataFromRange();
		return siege.stream().mapToInt(zeile -> zeile.get(0).getIntVal(0)).sum();
	}
}
