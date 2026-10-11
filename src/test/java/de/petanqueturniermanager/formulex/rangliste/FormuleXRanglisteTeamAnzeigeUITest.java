/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.formulex.rangliste;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.meldeliste.TeamAnzeige;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.formulex.konfiguration.FormuleXKonfigurationSheet;
import de.petanqueturniermanager.formulex.spielrunde.FormuleXTurnierTestDaten;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;

/** Regression: Im Modus NR muss die Namensspalte ausgeblendet werden (Breite 0 ist in LO wirkungslos). */
class FormuleXRanglisteTeamAnzeigeUITest extends BaseCalcUITest {

	@Test
	void namensspalteFolgtDerRanglistenTeamAnzeige() throws GenerateException {
		new FormuleXTurnierTestDaten(wkingSpreadsheet).generate(1, false);
		var konfig = new FormuleXKonfigurationSheet(wkingSpreadsheet);

		konfig.setRanglisteTeamAnzeige(TeamAnzeige.NR);
		new FormuleXRanglisteSheet(wkingSpreadsheet).run();
		assertThat(istSpalteSichtbar(rangliste(), FormuleXRanglisteSheet.TEAM_NAME_SPALTE)).isFalse();

		konfig.setRanglisteTeamAnzeige(TeamAnzeige.SPIELERNAMEN);
		new FormuleXRanglisteSheet(wkingSpreadsheet).run();
		assertThat(istSpalteSichtbar(rangliste(), FormuleXRanglisteSheet.TEAM_NAME_SPALTE)).isTrue();
		assertThat(sheetHlp.getTextFromCell(rangliste(),
				Position.from(FormuleXRanglisteSheet.TEAM_NAME_SPALTE, FormuleXRanglisteSheet.HEADER_ZEILE)))
				.isEqualTo(I18n.get("column.header.spieler"));
	}

	private XSpreadsheet rangliste() {
		return SheetMetadataHelper.findeSheetUndHeile(wkingSpreadsheet.getWorkingSpreadsheetDocument(),
				SheetMetadataHelper.SCHLUESSEL_FORMULEX_RANGLISTE, null);
	}
}
