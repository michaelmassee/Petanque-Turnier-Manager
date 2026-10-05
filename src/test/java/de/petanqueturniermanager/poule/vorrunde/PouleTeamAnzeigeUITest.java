/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.poule.vorrunde;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.meldeliste.TeamAnzeige;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.poule.PouleTurnierTestDaten;
import de.petanqueturniermanager.poule.konfiguration.PouleKonfigurationSheet;
import de.petanqueturniermanager.poule.rangliste.PouleVorrundenRanglisteSheet;

/** Sichtbarkeit der Namensspalten in Poule-Vorrunde und -Rangliste je Team-Anzeige. */
class PouleTeamAnzeigeUITest extends BaseCalcUITest {

	private static final int ANZ_TEAMS = 8;

	@Test
	void vorrundeZeigtStandardmaessigNamenWieBisher() throws GenerateException {
		new PouleTurnierTestDaten(wkingSpreadsheet, ANZ_TEAMS).generate();

		XSpreadsheet vorrunde = sheet(SheetMetadataHelper.SCHLUESSEL_POULE_VORRUNDE);
		assertThat(istSpalteSichtbar(vorrunde, AbstractPouleVorrundeSheet.SPALTE_TEAM_A_NAME))
				.as("Bestehende Poule-Turniere ohne gespeicherte Spielplan-Anzeige zeigen weiterhin Namen")
				.isTrue();
	}

	@Test
	void ranglisteBlendetNamensspalteImNummernModusAus() throws GenerateException {
		new PouleTurnierTestDaten(wkingSpreadsheet, ANZ_TEAMS).generate();
		var konfig = new PouleKonfigurationSheet(wkingSpreadsheet);

		konfig.setRanglisteTeamAnzeige(TeamAnzeige.NR);
		new PouleVorrundenRanglisteSheet(wkingSpreadsheet).run();
		XSpreadsheet rangliste = sheet(SheetMetadataHelper.SCHLUESSEL_POULE_VORRUNDEN_RANGLISTE);
		assertThat(istSpalteSichtbar(rangliste, PouleVorrundenRanglisteSheet.SPALTE_NAME)).isFalse();

		konfig.setRanglisteTeamAnzeige(TeamAnzeige.SPIELERNAMEN);
		new PouleVorrundenRanglisteSheet(wkingSpreadsheet).run();
		rangliste = sheet(SheetMetadataHelper.SCHLUESSEL_POULE_VORRUNDEN_RANGLISTE);
		assertThat(istSpalteSichtbar(rangliste, PouleVorrundenRanglisteSheet.SPALTE_NAME)).isTrue();
	}

	private XSpreadsheet sheet(String schluessel) {
		XSpreadsheet sheet = SheetMetadataHelper.findeSheetUndHeile(wkingSpreadsheet.getWorkingSpreadsheetDocument(),
				schluessel, null);
		assertThat(sheet).as("Sheet %s", schluessel).isNotNull();
		return sheet;
	}
}
