package de.petanqueturniermanager.helper.sheet;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeMode;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_New;

public class LeeresDefaultSheetCleanupUITest extends BaseCalcUITest {

	@Test
	void entferntLeeresDefaultSheetMitAktuellerSprache() {
		sheetHlp.removeAllSheetsExclude();
		assertThat(sheetHlp.findByName("leer")).isNotNull();

		sheetHlp.newIfNotExist("Tournament", 1);
		sheetHlp.entferneLeeresDefaultSheetWennMoeglich("Tournament");

		assertThat(sheetHlp.findByName("leer")).isNull();
		assertThat(sheetHlp.findByName("Tournament")).isNotNull();
	}

	@Test
	void entferntLeeresDefaultSheetAuchWennAktuelleSpracheAndersIst() {
		sheetHlp.newIfNotExist("Vacía", 1);
		sheetHlp.removeAllSheetsExclude("Vacía");

		sheetHlp.newIfNotExist("Turnier", 2);
		sheetHlp.entferneLeeresDefaultSheetWennMoeglich("Turnier");

		assertThat(sheetHlp.findByName("Vacía")).isNull();
		assertThat(sheetHlp.findByName("Turnier")).isNotNull();
	}

	@Test
	void entferntDefaultSheetNichtWennEsInhaltHat() {
		sheetHlp.removeAllSheetsExclude();
		XSpreadsheet empty = sheetHlp.findByName("leer");
		sheetHlp.setStringValueInCell(empty, Position.from(0, 0), "notes", true);

		sheetHlp.newIfNotExist("Tournament", 1);
		sheetHlp.entferneLeeresDefaultSheetWennMoeglich("Tournament");

		assertThat(sheetHlp.findByName("leer")).isNotNull();
		assertThat(sheetHlp.findByName("Tournament")).isNotNull();
	}

	@Test
	void newSheetCreateEntferntLeeresDefaultSheetNachEchtemTurnierSheet() throws Exception {
		new MeldeListeSheet_New(wkingSpreadsheet).createMeldelisteWithParams(SuperMeleeMode.Triplette);

		assertThat(sheetHlp.findByName("leer")).isNull();
		assertThat(sheetHlp.findByName(SheetNamen.meldeliste())).isNotNull();
	}
}
