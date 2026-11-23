/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.triptete.spielplan;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.blattschutz.FreilosSperrePruefung;
import de.petanqueturniermanager.triptete.blattschutz.TripTeteBlattschutzKonfiguration;
import de.petanqueturniermanager.triptete.meldeliste.TripTeteMeldeListeSheetTestDaten;

/**
 * Trip-Tête-Spielplan mit ungerader Teamzahl: Freispiel-Zeilen haben kein Ergebnis und bleiben im Turniermodus
 * gesperrt und ohne Editierfarbe.
 */
class TripTeteFreispielGesperrtUITest extends BaseCalcUITest {

	private static final int ANZ_TEAMS = 5;
	private static final int MAX_ZEILEN = 40;

	@Test
	void freispielErgebnisIstGesperrtUndNichtHervorgehoben() throws Exception {
		new TripTeteMeldeListeSheetTestDaten(wkingSpreadsheet, ANZ_TEAMS).generate();
		TripTeteSpielPlanSheet spielPlan = new TripTeteSpielPlanSheet(wkingSpreadsheet);
		spielPlan.run();
		XSpreadsheet sheet = spielPlan.getXSpreadSheet();

		int freispielZeile = -1;
		int begegnungZeile = -1;
		for (int zeile = TripTeteSpielPlanSheet.ERSTE_DATEN_ZEILE; zeile < MAX_ZEILEN; zeile++) {
			String nrB = sheetHlp.getTextFromCell(sheet,
					Position.from(TripTeteSpielPlanSheet.TEAM_B_NR_SPALTE, zeile));
			if ("0".equals(nrB) && freispielZeile < 0) {
				freispielZeile = zeile;
			} else if (!nrB.isBlank() && !"0".equals(nrB) && begegnungZeile < 0) {
				begegnungZeile = zeile;
			}
		}
		assertThat(freispielZeile).as("ungerade Teamzahl ergibt Freispiele").isNotNegative();
		assertThat(begegnungZeile).as("echte Begegnung vorhanden").isNotNegative();

		FreilosSperrePruefung.pruefe(wkingSpreadsheet, TripTeteBlattschutzKonfiguration.get(), sheet,
				List.of(TripTeteSpielPlanSheet.TRI_A_SPALTE, TripTeteSpielPlanSheet.DOU_B_SPALTE,
						TripTeteSpielPlanSheet.TETE_B_SPALTE),
				freispielZeile, begegnungZeile);
	}
}
