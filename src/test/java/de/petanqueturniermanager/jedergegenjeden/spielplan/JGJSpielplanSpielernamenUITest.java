/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.jedergegenjeden.spielplan;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.TeamAnzeige;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.ErwarteteSpielernamen;
import de.petanqueturniermanager.jedergegenjeden.meldeliste.JGJMeldeListeSheetTestDaten;

/**
 * Regressionstest: Mit aktiver Teamname-Spalte las die Spielernamen-Anzeige im JGJ-Spielplan den
 * Teamnamen als Vornamen von Spieler 1 und verschob alle Spielerspalten.
 */
class JGJSpielplanSpielernamenUITest extends BaseCalcUITest {

	private static final int ANZ_TEAMS = 4;
	private static final Formation FORMATION = Formation.DOUBLETTE;
	/** Wie {@code JGJMeldeListeDelegate.ERSTE_DATEN_ZEILE} (package-private). */
	private static final int MELDELISTE_ERSTE_DATEN_ZEILE = 3;
	/** Wie {@code JGJSpielPlanSheet.NAME_A_SPALTE} (private). */
	private static final int NAME_A_SPALTE = 1;

	@Test
	void spielernamenMitTeamnameSpalteBeginnenBeiSpieler1() throws GenerateException {
		new JGJMeldeListeSheetTestDaten(wkingSpreadsheet, FORMATION, ANZ_TEAMS, 0, true, TeamAnzeige.SPIELERNAMEN)
				.erstellenUndBefuellen();
		JGJSpielPlanSheet spielPlan = new JGJSpielPlanSheet(wkingSpreadsheet);
		spielPlan.run();

		Map<Integer, String> erwartet = ErwarteteSpielernamen.ausMeldeliste(
				sheetHlp.findByName(SheetNamen.meldeliste()), doc, MELDELISTE_ERSTE_DATEN_ZEILE, ANZ_TEAMS,
				FORMATION.getAnzSpieler(), false);
		XSpreadsheet sheet = spielPlan.getXSpreadSheet();
		int nrA = sheetHlp.getIntFromCell(sheet,
				Position.from(JGJSpielPlanSheet.TEAM_A_NR_SPALTE, JGJSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE));

		assertThat(erwartet).as("Teamnummer Team A").containsKey(nrA);
		assertThat(sheetHlp.getTextFromCell(sheet,
				Position.from(NAME_A_SPALTE, JGJSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE)))
				.as("Spielernamen-Anzeige muss bei Spieler 1 beginnen, nicht beim Teamnamen")
				.isEqualTo(erwartet.get(nrA));
	}
}
