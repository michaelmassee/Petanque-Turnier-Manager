package de.petanqueturniermanager.supermelee.spielrunde;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import org.junit.Before;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.model.Meldungen;
import de.petanqueturniermanager.model.Spieler;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.SpielTagNr;

public class SpielrundeSheet_NaechsteTest {

	private SpielrundeSheet_Naechste spielrundeSheet;
	private XComponentContext xComponentContextMock;
	private SheetHelper sheetHelperMock;

	@Before
	public void setup() {
		xComponentContextMock = PowerMockito.mock(XComponentContext.class);
		sheetHelperMock = PowerMockito.mock(SheetHelper.class);
		spielrundeSheet = new SpielrundeSheet_Naechste(xComponentContextMock) {
			@Override
			public SpielTagNr getSpielTag() throws GenerateException {
				return SpielTagNr.from(1);
			}

			@Override
			public SheetHelper getSheetHelper() {
				return sheetHelperMock;
			}
		};
	}

	@Test
	public void testGespieltenRundenEinlesen() throws Exception {

		XSpreadsheet spielTag1Runde1Mock = PowerMockito.mock(XSpreadsheet.class);

		Position pospielerNr = Position.from(AbstractSpielrundeSheet.ERSTE_SPIELERNR_SPALTE, AbstractSpielrundeSheet.ERSTE_DATEN_ZEILE);

		int[] spielerNr = { 3, 4, 8, // Team A
				12, 6, 9 }; // Team B
		int idx = 0;
		for (int teamCntr = 1; teamCntr <= 2; teamCntr++) { // Team A & B
			for (int spielerCntr = 1; spielerCntr <= 3; spielerCntr++) {
				pospielerNr.spalte(AbstractSpielrundeSheet.ERSTE_SPIELERNR_SPALTE + ((teamCntr - 1) * 3) + spielerCntr - 1);
				PowerMockito.when(sheetHelperMock.getIntFromCell(any(XSpreadsheet.class), eq(Position.from(pospielerNr)))).thenReturn(spielerNr[idx++]);
			}
		}
		PowerMockito.when(sheetHelperMock.findByName(spielrundeSheet.getSheetName(SpielTagNr.from(1), SpielRundeNr.from(1)))).thenReturn(spielTag1Runde1Mock);

		Meldungen meldungen = new Meldungen();
		for (idx = 0; idx < spielerNr.length; idx++) {
			meldungen.addSpielerWennNichtVorhanden(Spieler.from(spielerNr[idx]));
		}
		spielrundeSheet.gespieltenRundenEinlesen(meldungen, 1, 1);

		assertThat(meldungen.size()).isEqualTo(6);
		assertThat(meldungen.findSpielerByNr(1)).isNull();
		validateSpieler(meldungen, 3, 4, 8); // Team A
		validateSpieler(meldungen, 4, 3, 8);
		validateSpieler(meldungen, 8, 4, 3);

		validateSpieler(meldungen, 12, 6, 9); // Team B
		validateSpieler(meldungen, 6, 12, 9);
		validateSpieler(meldungen, 9, 12, 6);
	}

	private void validateSpieler(Meldungen meldungen, int nr, int warimTeammitA, int warimTeammitB) {
		assertThat(meldungen.findSpielerByNr(nr)).isNotNull();
		assertThat(meldungen.findSpielerByNr(nr)).isEqualTo(Spieler.from(nr));
		assertThat(meldungen.findSpielerByNr(nr).anzahlMitSpieler()).isEqualTo(2);
		assertThat(meldungen.findSpielerByNr(nr).warImTeamMit(warimTeammitA));
		assertThat(meldungen.findSpielerByNr(nr).warImTeamMit(warimTeammitB));

	}

}
