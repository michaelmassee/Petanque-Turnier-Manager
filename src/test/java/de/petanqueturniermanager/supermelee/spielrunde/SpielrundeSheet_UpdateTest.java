/**
 * Erstellung : 24.04.2018 / Michael Massee
 **/

package de.petanqueturniermanager.supermelee.spielrunde;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.cellstyle.SpielrundeHintergrundFarbeGeradeStyle;
import de.petanqueturniermanager.helper.cellstyle.SpielrundeHintergrundFarbeUnGeradeStyle;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeKonfigurationSheet;
import de.petanqueturniermanager.supermelee.meldeliste.AbstractSupermeleeMeldeListeSheet;

public class SpielrundeSheet_UpdateTest {

	private SpielrundeSheet_Update aktuelleSpielrundeSheet;
	private WorkingSpreadsheet workingSpreadsheetMock;
	AbstractSupermeleeMeldeListeSheet meldeListeSheetMock;
	SheetHelper sheetHelperMock;
	XSpreadsheet xSpreadsheetMock;
	SuperMeleeKonfigurationSheet konfigurationSheetMock;

	@BeforeEach
	public void setup() throws Exception {
		workingSpreadsheetMock = Mockito.mock(WorkingSpreadsheet.class);
		meldeListeSheetMock = Mockito.mock(AbstractSupermeleeMeldeListeSheet.class);
		sheetHelperMock = Mockito.mock(SheetHelper.class);
		xSpreadsheetMock = Mockito.mock(XSpreadsheet.class);
		konfigurationSheetMock = Mockito.mock(SuperMeleeKonfigurationSheet.class);
		Mockito.when(konfigurationSheetMock.getSpielRundeHintergrundFarbeGeradeStyle())
				.thenReturn(new SpielrundeHintergrundFarbeGeradeStyle(BasePropertiesSpalte.DEFAULT_GERADE_BACK_COLOR));
		Mockito.when(konfigurationSheetMock.getSpielRundeHintergrundFarbeUnGeradeStyle())
				.thenReturn(new SpielrundeHintergrundFarbeUnGeradeStyle(BasePropertiesSpalte.DEFAULT_UNGERADE_BACK_COLOR));

		aktuelleSpielrundeSheet = new SpielrundeSheet_Update(workingSpreadsheetMock) {

			@Override
			protected SuperMeleeKonfigurationSheet newSuperMeleeKonfigurationSheet(
					WorkingSpreadsheet workingSpreadsheet) {
				return konfigurationSheetMock;
			}

			@Override
			public SuperMeleeKonfigurationSheet getKonfigurationSheet() {
				return konfigurationSheetMock;
			}

			@Override
			AbstractSupermeleeMeldeListeSheet initMeldeListeSheet(WorkingSpreadsheet xContext) {
				return meldeListeSheetMock;
			}

			@Override
			public SheetHelper getSheetHelper() {
				return sheetHelperMock;
			}

			@Override
			public XSpreadsheet getXSpreadSheet() {
				return xSpreadsheetMock;
			}

			@Override
			public SpielTagNr getSpielTag() {
				return SpielTagNr.from(1);
			}

			@Override
			public SpielRundeNr getSpielRundeNr() {
				return SpielRundeNr.from(1);
			}
		};
	}

	@Test
	public void testErgebnisseEinlesen() throws Exception {

		// testdaten zurückliefern
		// ----------------------------------------
		List<int[]> spielpaarungen = new ArrayList<>();

		int[] teamABLine1 = new int[] { 32, 20, 10, 4, 6, 8 };
		int[] teamABLine2 = new int[] { 0, 28, 1, 23, 5, 0 }; // 0 = not set
		int[] teamABLine3 = new int[] { 90, 91, 92, 31, 35, 0 }; // 0 = not set

		spielpaarungen.add(teamABLine1);
		spielpaarungen.add(teamABLine2);
		spielpaarungen.add(teamABLine3);
		setupReturn_from_getIntFromCell(spielpaarungen);
		// ----------------------------------------

		List<SpielerSpielrundeErgebnis> result = aktuelleSpielrundeSheet.ergebnisseEinlesen()
				.getSpielerSpielrundeErgebnis();

		// ----------------------------------------
		// Validate
		// ----------------------------------------

		assertThat(result).isNotEmpty();
		assertThat(result.size()).isEqualTo(15);
		// validate line 1
		for (int i = 0; i < 6; i++) {
			assertThat(result.get(i).getSpielerNr()).isEqualTo(teamABLine1[i]);
		}

		// validate line 2
		assertThat(result.get(6).getSpielerNr()).isEqualTo(teamABLine2[1]);
		assertThat(result.get(7).getSpielerNr()).isEqualTo(teamABLine2[2]);
		assertThat(result.get(8).getSpielerNr()).isEqualTo(teamABLine2[3]);
		assertThat(result.get(9).getSpielerNr()).isEqualTo(teamABLine2[4]);
		// validate line 3
		assertThat(result.get(10).getSpielerNr()).isEqualTo(teamABLine3[0]);
		assertThat(result.get(11).getSpielerNr()).isEqualTo(teamABLine3[1]);
		assertThat(result.get(12).getSpielerNr()).isEqualTo(teamABLine3[2]);
		assertThat(result.get(13).getSpielerNr()).isEqualTo(teamABLine3[3]);
		assertThat(result.get(14).getSpielerNr()).isEqualTo(teamABLine3[4]);

		// Maximal 4 zeilen, nach der erste leere zeile sollte abgebrochen werden
		Mockito.verify(sheetHelperMock, Mockito.times(4 * 6)).getIntFromCell(any(XSpreadsheet.class),
				any(Position.class));

	}

	@Test
	public void testDoppelteSpielerNr() throws Exception {
		List<int[]> spielpaarungen = new ArrayList<>();
		int[] teamABLine1 = new int[] { 32, 20, 10, 4, 20, 8 };
		spielpaarungen.add(teamABLine1);
		setupReturn_from_getIntFromCell(spielpaarungen);

		try {
			aktuelleSpielrundeSheet.ergebnisseEinlesen().getSpielerSpielrundeErgebnis();
			fail("Erwarte GenerateException");
		} catch (GenerateException e) {
			assertThat(e.getMessage()).isEqualTo("Spieler mit der Nr. 20 ist doppelt");
		}

	}

	private void setupReturn_from_getIntFromCell(List<int[]> spielpaarungen) {
		Position spielerNrPos = Position.from(AbstractSpielrundeSheet.ERSTE_SPIELERNR_SPALTE,
				AbstractSpielrundeSheet.ERSTE_DATEN_ZEILE);
		spielpaarungen.forEach(spielpaarung -> {
			for (int spielerSpalte = 0; spielerSpalte < 6; spielerSpalte++) {
				if (spielpaarung[spielerSpalte] > 0) {
					Mockito.when(
							sheetHelperMock.getIntFromCell(any(XSpreadsheet.class), eq(Position.from(spielerNrPos))))
							.thenReturn(spielpaarung[spielerSpalte]);
				}
				spielerNrPos.spaltePlusEins();
			}
			spielerNrPos.spalte(AbstractSpielrundeSheet.ERSTE_SPIELERNR_SPALTE).zeilePlusEins();
		});

	}

}
