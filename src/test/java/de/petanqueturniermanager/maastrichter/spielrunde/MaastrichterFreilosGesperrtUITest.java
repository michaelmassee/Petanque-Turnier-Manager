/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.maastrichter.spielrunde;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.blattschutz.FreilosSperrePruefung;
import de.petanqueturniermanager.maastrichter.blattschutz.MaastrichterBlattschutzKonfiguration;
import de.petanqueturniermanager.maastrichter.meldeliste.MaastrichterMeldeListeSheetTestDaten;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerAbstractSpielrundeSheet;

/**
 * Maastrichter-Vorrunde: Das vorbelegte Freilos-Ergebnis bleibt im Turniermodus gesperrt.
 */
class MaastrichterFreilosGesperrtUITest extends BaseCalcUITest {

	private static final int ANZ_TEAMS = 7;
	private static final int FREILOS_ZEILE = SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE + ANZ_TEAMS / 2;

	@Test
	void freilosErgebnisIstGesperrtUndNichtHervorgehoben() throws Exception {
		new MaastrichterMeldeListeSheetTestDaten(wkingSpreadsheet, ANZ_TEAMS).erstelleTestdaten();
		var vorrunde = new MaastrichterSpielrundeSheetNaechste(wkingSpreadsheet);
		vorrunde.doRun();

		XSpreadsheet sheet = vorrunde.getXSpreadSheet();
		assertThat(sheetHlp.getTextFromCell(sheet,
				Position.from(SchweizerAbstractSpielrundeSheet.TEAM_B_SPALTE, FREILOS_ZEILE)))
				.as("letzte Zeile ist das Freilos").isBlank();

		FreilosSperrePruefung.pruefe(wkingSpreadsheet, MaastrichterBlattschutzKonfiguration.get(), sheet,
				List.of(SchweizerAbstractSpielrundeSheet.ERG_TEAM_A_SPALTE,
						SchweizerAbstractSpielrundeSheet.ERG_TEAM_B_SPALTE),
				FREILOS_ZEILE, SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE);
	}
}
