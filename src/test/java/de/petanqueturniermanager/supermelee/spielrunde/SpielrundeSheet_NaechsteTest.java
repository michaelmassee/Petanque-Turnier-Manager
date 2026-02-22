package de.petanqueturniermanager.supermelee.spielrunde;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.cellstyle.MeldungenHintergrundFarbeGeradeStyle;
import de.petanqueturniermanager.helper.cellstyle.SpielrundeHintergrundFarbeGeradeStyle;
import de.petanqueturniermanager.helper.cellstyle.SpielrundeHintergrundFarbeUnGeradeStyle;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.model.Spieler;
import de.petanqueturniermanager.model.SpielerMeldungen;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.konfiguration.ISuperMeleePropertiesSpalte;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeKonfigurationSheet;
import de.petanqueturniermanager.supermelee.meldeliste.AbstractSupermeleeMeldeListeSheet;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_New;

public class SpielrundeSheet_NaechsteTest {

	private SpielrundeSheet_Naechste spielrundeSheet;
	private WorkingSpreadsheet workingSpreadsheetMock;
	MeldeListeSheet_New MeldeListeSheet_NewMock;
	SheetHelper sheetHelperMock;
	SuperMeleeKonfigurationSheet konfigurationSheetMock;
	ISuperMeleePropertiesSpalte iSuperMeleePropertiesSpalteMock;

	@BeforeEach
	public void setup() {
		workingSpreadsheetMock = Mockito.mock(WorkingSpreadsheet.class);
		sheetHelperMock = Mockito.mock(SheetHelper.class);
		MeldeListeSheet_NewMock = Mockito.mock(MeldeListeSheet_New.class);
		konfigurationSheetMock = Mockito.mock(SuperMeleeKonfigurationSheet.class);
		iSuperMeleePropertiesSpalteMock = Mockito.mock(ISuperMeleePropertiesSpalte.class);

		Mockito.when(konfigurationSheetMock.getPropertiesSpalte()).thenReturn(iSuperMeleePropertiesSpalteMock);
		Mockito.when(iSuperMeleePropertiesSpalteMock.getMeldeListeHintergrundFarbeGeradeStyle())
				.thenReturn(new MeldungenHintergrundFarbeGeradeStyle(BasePropertiesSpalte.DEFAULT_GERADE_BACK_COLOR));
		Mockito.when(konfigurationSheetMock.getSpielRundeHintergrundFarbeGeradeStyle())
				.thenReturn(new SpielrundeHintergrundFarbeGeradeStyle(BasePropertiesSpalte.DEFAULT_GERADE_BACK_COLOR));
		Mockito.when(konfigurationSheetMock.getSpielRundeHintergrundFarbeUnGeradeStyle())
				.thenReturn(
						new SpielrundeHintergrundFarbeUnGeradeStyle(BasePropertiesSpalte.DEFAULT_UNGERADE_BACK_COLOR));

		spielrundeSheet = new SpielrundeSheet_Naechste(workingSpreadsheetMock) {

			@Override
			public SuperMeleeKonfigurationSheet getKonfigurationSheet() {
				return konfigurationSheetMock;
			}

			@Override
			protected SuperMeleeKonfigurationSheet newSuperMeleeKonfigurationSheet(
					WorkingSpreadsheet workingSpreadsheet) {
				return konfigurationSheetMock;
			}

			@Override
			AbstractSupermeleeMeldeListeSheet initMeldeListeSheet(WorkingSpreadsheet workingSpreadsheet) {
				return MeldeListeSheet_NewMock;
			}

			@Override
			public SpielTagNr getSpielTag() {
				return SpielTagNr.from(1);
			}

			@Override
			public SheetHelper getSheetHelper() {
				return sheetHelperMock;
			}

			@Override
			public Integer getMaxAnzGespielteSpieltage() throws GenerateException {
				return 1;
			}

			@Override
			public void processBoxinfo(String infoMsg) {
				// nothing
			}
		};
	}

	@Test
	public void testGespieltenRundenEinlesen() throws Exception {

		XSpreadsheet spielTag1Runde1Mock = Mockito.mock(XSpreadsheet.class);

		Position pospielerNr = Position.from(AbstractSpielrundeSheet.ERSTE_SPIELERNR_SPALTE,
				AbstractSpielrundeSheet.ERSTE_DATEN_ZEILE);

		int[] spielerNr = { 3, 4, 8, // Team A
				12, 6, 9 }; // Team B
		int idx = 0;
		for (int teamCntr = 1; teamCntr <= 2; teamCntr++) { // Team A & B
			for (int spielerCntr = 1; spielerCntr <= 3; spielerCntr++) {
				pospielerNr.spalte(
						AbstractSpielrundeSheet.ERSTE_SPIELERNR_SPALTE + ((teamCntr - 1) * 3) + spielerCntr - 1);
				Mockito
						.when(sheetHelperMock.getIntFromCell(any(XSpreadsheet.class), eq(Position.from(pospielerNr))))
						.thenReturn(spielerNr[idx++]);
			}
		}
		Mockito
				.when(sheetHelperMock
						.findByName(spielrundeSheet.getSheetName(SpielTagNr.from(1), SpielRundeNr.from(1))))
				.thenReturn(spielTag1Runde1Mock);

		// PAARUNG_CNTR_SPALTE in der nächsten Zeile = -1 → Schleife endet nach der 1.
		// Paarung
		Position posPaarungCntr = Position.from(AbstractSpielrundeSheet.PAARUNG_CNTR_SPALTE,
				AbstractSpielrundeSheet.ERSTE_DATEN_ZEILE + 1);
		Mockito.when(sheetHelperMock.getIntFromCell(any(XSpreadsheet.class), eq(posPaarungCntr)))
				.thenReturn(-1);

		SpielerMeldungen meldungen = new SpielerMeldungen();
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

	private void validateSpieler(SpielerMeldungen meldungen, int nr, int warimTeammitA, int warimTeammitB) {
		assertThat(meldungen.findSpielerByNr(nr)).isNotNull();
		assertThat(meldungen.findSpielerByNr(nr)).isEqualTo(Spieler.from(nr));
		assertThat(meldungen.findSpielerByNr(nr).anzahlMitSpieler()).isEqualTo(2);
		assertThat(meldungen.findSpielerByNr(nr).warImTeamMit(warimTeammitA)).isTrue();
		assertThat(meldungen.findSpielerByNr(nr).warImTeamMit(warimTeammitB)).isTrue();

	}

}
