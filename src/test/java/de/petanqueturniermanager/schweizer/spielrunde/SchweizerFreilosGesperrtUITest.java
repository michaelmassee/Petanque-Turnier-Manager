package de.petanqueturniermanager.schweizer.spielrunde;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.meldeliste.TeamAnzeige;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.blattschutz.FreilosSperrePruefung;
import de.petanqueturniermanager.schweizer.blattschutz.SchweizerBlattschutzKonfiguration;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetTestDaten;

/**
 * Das Freilos-Ergebnis ist per Freispiel-Property vorbelegt: Es darf weder als editierbar markiert noch im
 * Turniermodus entsperrt sein – für Nummern- und Namensanzeige.
 */
class SchweizerFreilosGesperrtUITest extends BaseCalcUITest {

	private static final int ANZ_TEAMS = 7;
	private static final int FREILOS_ZEILE = SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE + ANZ_TEAMS / 2;

	@ParameterizedTest
	@EnumSource(value = TeamAnzeige.class, names = { "NR", "NAME" })
	void freilosErgebnisIstGesperrtUndNichtHervorgehoben(TeamAnzeige anzeige) throws Exception {
		new SchweizerMeldeListeSheetTestDaten(wkingSpreadsheet, ANZ_TEAMS).doRun();
		var spielrunde = new SchweizerSpielrundeSheetNaechste(wkingSpreadsheet);
		spielrunde.getKonfigurationSheet().setSpielplanTeamAnzeige(anzeige);
		spielrunde.doRun();

		XSpreadsheet sheet = spielrunde.getXSpreadSheet();
		assertThat(sheetHlp.getTextFromCell(sheet,
				Position.from(SchweizerAbstractSpielrundeSheet.TEAM_B_SPALTE, FREILOS_ZEILE)))
				.as("letzte Zeile ist das Freilos").isBlank();

		FreilosSperrePruefung.pruefe(wkingSpreadsheet, SchweizerBlattschutzKonfiguration.get(), sheet,
				List.of(SchweizerAbstractSpielrundeSheet.ERG_TEAM_A_SPALTE,
						SchweizerAbstractSpielrundeSheet.ERG_TEAM_B_SPALTE),
				FREILOS_ZEILE, SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE);
	}
}
