/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ko;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.ko.meldeliste.KoMeldeListeSheetTestDaten;

/**
 * Issue #23: Ein Ergebnis in einem Match, dessen Gegner noch nicht feststeht (Vorspiel offen), darf
 * keinen Sieger in die Folgerunde weiterreichen.
 */
class KoTurnierbaumOhneGegnerUITest extends BaseCalcUITest {

	private static final int ANZ_TEAMS = 8;

	private KoTurnierbaumSheet turnierbaum;
	private XSpreadsheet sheet;

	@Test
	void ergebnisOhneFeststehendenGegnerWirdNichtWeitergereicht() throws GenerateException {
		erstelleTurnierbaum();

		// Runde 1, Match 0 entschieden – Match 1 (liefert den Gegner für Runde 2) bleibt offen.
		trageErgebnisEin(1, 0, 13, 5);
		// Runde 2, Match 0: nur Team A steht fest, trotzdem Punkte eingetragen.
		trageErgebnisEin(2, 0, 13, 7);
		recalcAll();

		assertThat(teamB(2)).as("Gegner in Runde 2 steht noch nicht fest").isEmpty();
		assertThat(teamA(3)).as("ohne Gegner darf kein Sieger weitergereicht werden").isEmpty();
	}

	@Test
	void nachEntschiedenemVorspielWirdSiegerWeitergereicht() throws GenerateException {
		erstelleTurnierbaum();

		trageErgebnisEin(1, 0, 13, 5);
		trageErgebnisEin(2, 0, 13, 7);
		trageErgebnisEin(1, 1, 4, 13);
		recalcAll();

		assertThat(teamB(2)).as("Gegner in Runde 2 steht fest").isNotEmpty();
		assertThat(teamA(3)).as("Sieger aus Runde 2 Match 0")
				.isNotEmpty().isEqualTo(teamA(2));
	}

	private void erstelleTurnierbaum() throws GenerateException {
		new KoMeldeListeSheetTestDaten(wkingSpreadsheet, ANZ_TEAMS).erstelleMeldelisteWithTestdaten();
		turnierbaum = new KoTurnierbaumSheet(wkingSpreadsheet);
		turnierbaum.erstelleTurnierbaumOhneDialog();
		String praefix = SheetNamen.koTurnierbaumEinzel();
		String sheetName = Arrays.stream(doc.getSheets().getElementNames()).filter(n -> n.startsWith(praefix))
				.findFirst().orElseThrow();
		sheet = sheetHlp.findByName(sheetName);
	}

	private void trageErgebnisEin(int runde, int match, int punkteA, int punkteB) throws GenerateException {
		int spalte = turnierbaum.scoreSpalte(runde);
		sheetHlp.setNumberValueInCell(NumberCellValue.from(sheet,
				Position.from(spalte, turnierbaum.teamAZeile(runde, match))).setValue(punkteA));
		sheetHlp.setNumberValueInCell(NumberCellValue.from(sheet,
				Position.from(spalte, turnierbaum.teamBZeile(runde, match))).setValue(punkteB));
	}

	/** Team-A-Eintrag von Match 0 der angegebenen Runde. */
	private String teamA(int runde) {
		return teamText(runde, turnierbaum.teamAZeile(runde, 0));
	}

	/** Team-B-Eintrag von Match 0 der angegebenen Runde. */
	private String teamB(int runde) {
		return teamText(runde, turnierbaum.teamBZeile(runde, 0));
	}

	private String teamText(int runde, int zeile) {
		return sheetHlp.getTextFromCell(sheet, Position.from(turnierbaum.teamSpalte(runde), zeile));
	}
}
