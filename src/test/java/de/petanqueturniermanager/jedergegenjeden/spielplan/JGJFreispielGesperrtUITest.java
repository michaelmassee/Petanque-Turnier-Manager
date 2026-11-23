/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.jedergegenjeden.spielplan;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.blattschutz.FreilosSperrePruefung;
import de.petanqueturniermanager.jedergegenjeden.blattschutz.JGJBlattschutzKonfiguration;
import de.petanqueturniermanager.jedergegenjeden.rangliste.JGJTestMeldeListeErstellen;

/**
 * JGJ-Spielplan mit ungerader Teamzahl: Das vorbelegte Freispiel-Ergebnis bleibt im Turniermodus gesperrt.
 */
class JGJFreispielGesperrtUITest extends BaseCalcUITest {

	private static final int MAX_ZEILEN = 40;

	@Test
	void freispielErgebnisIstGesperrtUndNichtHervorgehoben() throws Exception {
		new JGJTestMeldeListeErstellen(wkingSpreadsheet, doc).run();
		JGJSpielPlanSheet spielPlan = new JGJSpielPlanSheet(wkingSpreadsheet);
		spielPlan.run();
		XSpreadsheet sheet = spielPlan.getXSpreadSheet();

		int freispielZeile = -1;
		int begegnungZeile = -1;
		for (int zeile = JGJSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE; zeile < MAX_ZEILEN; zeile++) {
			String nrB = sheetHlp.getTextFromCell(sheet, Position.from(JGJSpielPlanSheet.TEAM_B_NR_SPALTE, zeile));
			if ("0".equals(nrB) && freispielZeile < 0) {
				freispielZeile = zeile;
			} else if (!nrB.isBlank() && !"0".equals(nrB) && begegnungZeile < 0) {
				begegnungZeile = zeile;
			}
		}
		assertThat(freispielZeile).as("ungerade Teamzahl ergibt Freispiele").isNotNegative();
		assertThat(begegnungZeile).as("echte Begegnung vorhanden").isNotNegative();

		FreilosSperrePruefung.pruefe(wkingSpreadsheet, JGJBlattschutzKonfiguration.get(), sheet,
				List.of(JGJSpielPlanSheet.SPIELPNKT_A_SPALTE, JGJSpielPlanSheet.SPIELPNKT_B_SPALTE),
				freispielZeile, begegnungZeile);
	}
}
