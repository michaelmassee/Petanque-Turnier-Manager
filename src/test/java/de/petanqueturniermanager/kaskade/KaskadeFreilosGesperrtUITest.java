/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.kaskade;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.blattschutz.FreilosSperrePruefung;
import de.petanqueturniermanager.kaskade.blattschutz.KaskadeBlattschutzKonfiguration;
import de.petanqueturniermanager.kaskade.spielrunde.KaskadeSpielrundeSheet;

/**
 * Kaskadenrunde: Das vorbelegte Freilos-Ergebnis (Team B leer) bleibt im Turniermodus gesperrt.
 */
class KaskadeFreilosGesperrtUITest extends BaseCalcUITest {

	private static final int ANZ_TEAMS = 7;
	private static final int ANZ_KASKADEN = 1;
	private static final int MAX_ZEILEN = 20;

	@Test
	void freilosErgebnisIstGesperrtUndNichtHervorgehoben() throws Exception {
		new KaskadeTurnierTestDaten(wkingSpreadsheet, ANZ_TEAMS, ANZ_KASKADEN).generate(1, false);
		XSpreadsheet sheet = sheetHlp.findByName(SheetNamen.kaskadenRunde(1));
		assertThat(sheet).as("Kaskadenrunde 1").isNotNull();

		int freilosZeile = -1;
		int begegnungZeile = -1;
		for (int zeile = KaskadeSpielrundeSheet.ERSTE_DATEN_ZEILE; zeile < MAX_ZEILEN; zeile++) {
			boolean teamA = !sheetHlp.getTextFromCell(sheet,
					Position.from(KaskadeSpielrundeSheet.TEAM_A_SPALTE, zeile)).isBlank();
			boolean teamB = !sheetHlp.getTextFromCell(sheet,
					Position.from(KaskadeSpielrundeSheet.TEAM_B_SPALTE, zeile)).isBlank();
			if (teamA && !teamB && freilosZeile < 0) {
				freilosZeile = zeile;
			} else if (teamA && teamB && begegnungZeile < 0) {
				begegnungZeile = zeile;
			}
		}
		assertThat(freilosZeile).as("7 Teams ergeben ein Freilos").isNotNegative();
		assertThat(begegnungZeile).as("echte Begegnung vorhanden").isNotNegative();

		FreilosSperrePruefung.pruefe(wkingSpreadsheet, KaskadeBlattschutzKonfiguration.get(), sheet,
				List.of(KaskadeSpielrundeSheet.ERG_TEAM_A_SPALTE, KaskadeSpielrundeSheet.ERG_TEAM_B_SPALTE),
				freilosZeile, begegnungZeile);
	}
}
