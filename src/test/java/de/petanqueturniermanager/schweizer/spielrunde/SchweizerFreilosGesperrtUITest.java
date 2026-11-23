package de.petanqueturniermanager.schweizer.spielrunde;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.sun.star.beans.XPropertySet;
import com.sun.star.sheet.XSheetCondition;
import com.sun.star.sheet.XSheetConditionalEntries;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.util.CellProtection;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.meldeliste.TeamAnzeige;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.EditierbaresZelleFormatHelper;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzManager;
import de.petanqueturniermanager.schweizer.blattschutz.SchweizerBlattschutzKonfiguration;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetTestDaten;
import de.petanqueturniermanager.toolbar.TurnierModus;

/**
 * Das Freilos-Ergebnis ist per Freispiel-Property vorbelegt: Es darf weder als editierbar markiert noch im
 * Turniermodus entsperrt sein, die Ergebnisse der echten Begegnungen dagegen schon – für Nummern- und
 * Namensanzeige.
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
		Position freilosErgA = Position.from(SchweizerAbstractSpielrundeSheet.ERG_TEAM_A_SPALTE, FREILOS_ZEILE);
		Position freilosErgB = Position.from(SchweizerAbstractSpielrundeSheet.ERG_TEAM_B_SPALTE, FREILOS_ZEILE);
		Position begegnungErgA = Position.from(SchweizerAbstractSpielrundeSheet.ERG_TEAM_A_SPALTE,
				SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE);

		assertThat(sheetHlp.getTextFromCell(sheet,
				Position.from(SchweizerAbstractSpielrundeSheet.TEAM_B_SPALTE, FREILOS_ZEILE)))
				.as("letzte Zeile ist das Freilos").isBlank();
		assertThat(hatEditierfarbe(sheet, freilosErgA)).as("Freilos Ergebnis A ohne Editierfarbe").isFalse();
		assertThat(hatEditierfarbe(sheet, freilosErgB)).as("Freilos Ergebnis B ohne Editierfarbe").isFalse();
		assertThat(hatEditierfarbe(sheet, begegnungErgA)).as("Begegnung behält die Editierfarbe").isTrue();

		TurnierModus.get().setAktivForTest(true);
		try {
			BlattschutzManager.get().schuetzen(SchweizerBlattschutzKonfiguration.get(), wkingSpreadsheet);
			assertThat(istGesperrt(sheet, freilosErgA)).as("Freilos Ergebnis A gesperrt").isTrue();
			assertThat(istGesperrt(sheet, freilosErgB)).as("Freilos Ergebnis B gesperrt").isTrue();
			assertThat(istGesperrt(sheet, begegnungErgA)).as("Begegnung editierbar").isFalse();
		} finally {
			BlattschutzManager.get().entsperren(SchweizerBlattschutzKonfiguration.get(), wkingSpreadsheet);
			TurnierModus.get().setAktivForTest(false);
		}
	}

	private static boolean istGesperrt(XSpreadsheet sheet, Position position) throws Exception {
		XPropertySet props = Lo.qi(XPropertySet.class,
				sheet.getCellByPosition(position.getSpalte(), position.getZeile()));
		return ((CellProtection) props.getPropertyValue("CellProtection")).IsLocked;
	}

	private static boolean hatEditierfarbe(XSpreadsheet sheet, Position position) throws Exception {
		XPropertySet props = Lo.qi(XPropertySet.class,
				sheet.getCellByPosition(position.getSpalte(), position.getZeile()));
		XSheetConditionalEntries entries = Lo.qi(XSheetConditionalEntries.class,
				props.getPropertyValue("ConditionalFormat"));
		for (int i = 0; i < entries.getCount(); i++) {
			XSheetCondition condition = Lo.qi(XSheetCondition.class, entries.getByIndex(i));
			if (condition.getFormula1().contains(EditierbaresZelleFormatHelper.PROPERTY_KEY)) {
				return true;
			}
		}
		return false;
	}
}
