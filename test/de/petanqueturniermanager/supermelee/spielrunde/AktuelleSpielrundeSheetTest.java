/**
* Erstellung : 24.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee.spielrunde;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.supermelee.meldeliste.AbstractMeldeListeSheet;

public class AktuelleSpielrundeSheetTest {

	AktuelleSpielrundeSheet aktuelleSpielrundeSheet;
	XComponentContext xComponentContextMock;
	AbstractMeldeListeSheet meldeListeSheetMock;
	SheetHelper sheetHelperMock;
	XSpreadsheet xSpreadsheetMock;

	@Before
	public void setup() {
		this.xComponentContextMock = PowerMockito.mock(XComponentContext.class);
		this.meldeListeSheetMock = PowerMockito.mock(AbstractMeldeListeSheet.class);
		this.sheetHelperMock = PowerMockito.mock(SheetHelper.class);
		this.xSpreadsheetMock = PowerMockito.mock(XSpreadsheet.class);

		this.aktuelleSpielrundeSheet = new AktuelleSpielrundeSheet(this.xComponentContextMock) {
			@Override
			AbstractMeldeListeSheet initMeldeListeSheet(XComponentContext xContext) {
				return AktuelleSpielrundeSheetTest.this.meldeListeSheetMock;
			}

			@Override
			public SheetHelper getSheetHelper() {
				return AktuelleSpielrundeSheetTest.this.sheetHelperMock;
			}

			@Override
			public XSpreadsheet getSpielRundeSheet(int spielrunde) {
				return AktuelleSpielrundeSheetTest.this.xSpreadsheetMock;
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

		List<SpielerSpielrundeErgebnis> result = this.aktuelleSpielrundeSheet.ergebnisseEinlesen(1)
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
		Mockito.verify(this.sheetHelperMock, Mockito.times(4 * 6)).getIntFromCell(any(XSpreadsheet.class),
				any(Position.class));

	}

	@Test
	public void testDoppelteSpielerNr() throws Exception {
		List<int[]> spielpaarungen = new ArrayList<>();
		int[] teamABLine1 = new int[] { 32, 20, 10, 4, 20, 8 };
		spielpaarungen.add(teamABLine1);
		setupReturn_from_getIntFromCell(spielpaarungen);

		try {
			this.aktuelleSpielrundeSheet.ergebnisseEinlesen(8).getSpielerSpielrundeErgebnis();
			fail("Erwarte GenerateException");
		} catch (GenerateException e) {
			assertThat(e.getMessage()).isEqualTo("Spieler mit der Nr. 20 ist doppelt");
		}

	}

	private void setupReturn_from_getIntFromCell(List<int[]> spielpaarungen) {
		Position spielerNrPos = Position.from(AktuelleSpielrundeSheet.ERSTE_SPIELERNR_SPALTE,
				AktuelleSpielrundeSheet.ERSTE_DATEN_ZEILE);
		spielpaarungen.forEach(spielpaarung -> {
			for (int spielerSpalte = 0; spielerSpalte < 6; spielerSpalte++) {
				if (spielpaarung[spielerSpalte] > 0) {
					PowerMockito.when(this.sheetHelperMock.getIntFromCell(any(XSpreadsheet.class),
							eq(Position.from(spielerNrPos)))).thenReturn(spielpaarung[spielerSpalte]);
				}
				spielerNrPos.spaltePlusEins();
			}
			spielerNrPos.spalte(AktuelleSpielrundeSheet.ERSTE_SPIELERNR_SPALTE).zeilePlusEins();
		});

	}

}
