package de.petanqueturniermanager.basesheet.meldeliste;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.SheetHelper;

public class MeldeListeHelperTest {
	private MeldeListeHelper meldeListeHelper;

	private IMeldeliste iMeldelisteMock;
	private SheetHelper sheetHelperMock;
	private MeldungenSpalte meldungenSpalteMock;
	XSpreadsheet xSpreadsheetMock;

	@Before
	public void init() throws GenerateException {
		iMeldelisteMock = PowerMockito.mock(IMeldeliste.class);
		sheetHelperMock = PowerMockito.mock(SheetHelper.class);
		meldungenSpalteMock = PowerMockito.mock(MeldungenSpalte.class);
		xSpreadsheetMock = PowerMockito.mock(XSpreadsheet.class);

		PowerMockito.when(iMeldelisteMock.getMeldungenSpalte()).thenReturn(meldungenSpalteMock);
		PowerMockito.when(iMeldelisteMock.getSheetHelper()).thenReturn(sheetHelperMock);

		meldeListeHelper = new MeldeListeHelper(iMeldelisteMock) {
			@Override
			public void doSort(int spalteNr, boolean isAscending) throws GenerateException {
				// nichts
			}

			@Override
			public XSpreadsheet getSheet() {
				return xSpreadsheetMock;
			}
		};
	}

	@Test
	public void testCleanUpSpielerName() throws Exception {
		String result = meldeListeHelper.cleanUpSpielerName(" ÄÜö # + ösX YSDdRfÖßßäslkg .,m..,m ");
		assertThat(result).isEqualTo("äüöösxysddrfößßäslkgmm");
	}

	@Test
	public void testTestDoppelteNamenMitUmlauten() throws Exception {

		SpielerNrName[] spielerNrNameList = new SpielerNrName[] { new SpielerNrName(32, "Müller"), new SpielerNrName(10, "Müller") };

		initReturnSpielerDaten(spielerNrNameList);

		try {
			meldeListeHelper.testDoppelteMeldungen();
			fail("Erwarte GenerateException");
		} catch (GenerateException exc) {
			assertThat(exc.getMessage()).containsOnlyOnce("Spieler Namen Müller ist doppelt in der Meldeliste");
		}
	}

	@Test
	public void testTestDoppelteNamenMitZonderZeichenundLeerstellen() throws Exception {
		SpielerNrName[] spielerNrNameList = new SpielerNrName[] { new SpielerNrName(32, "Maja   Biene"), new SpielerNrName(10, "Maja, Biene") };
		initReturnSpielerDaten(spielerNrNameList);

		try {
			meldeListeHelper.testDoppelteMeldungen();
			fail("Erwarte GenerateException");
		} catch (GenerateException exc) {
			assertThat(exc.getMessage()).containsOnlyOnce("Spieler Namen Maja, Biene ist doppelt in der Meldeliste");
		}
	}

	@Test
	public void testTestDoppelteNamenIgnoreCase() throws Exception {

		SpielerNrName[] spielerNrNameList = new SpielerNrName[] { new SpielerNrName(32, "Anna"), new SpielerNrName(10, "heinz"), new SpielerNrName(4, "Klaus"),
				new SpielerNrName(12, "Heinz") };

		initReturnSpielerDaten(spielerNrNameList);

		try {
			meldeListeHelper.testDoppelteMeldungen();
			fail("Erwarte GenerateException");
		} catch (GenerateException exc) {
			assertThat(exc.getMessage()).containsOnlyOnce("Spieler Namen Heinz ist doppelt in der Meldeliste");
		}
	}

	@Test
	public void testTestDoppelteSpielrNr() throws Exception {

		SpielerNrName[] spielerNrNameList = new SpielerNrName[] { new SpielerNrName(32, "Anna"), new SpielerNrName(12, "Petra"), new SpielerNrName(4, "Klaus"),
				new SpielerNrName(12, "Heinz") };
		initReturnSpielerDaten(spielerNrNameList);

		try {
			meldeListeHelper.testDoppelteMeldungen();
			fail("Erwarte GenerateException");
		} catch (GenerateException exc) {
			assertThat(exc.getMessage()).containsOnlyOnce("Spieler Nr. 12 ist doppelt");
		}
	}

	@Test
	public void testDoppelteDaten_NoError() throws Exception {

		SpielerNrName[] spielerNrNameList = new SpielerNrName[] { new SpielerNrName(32, "Anna"), new SpielerNrName(10, "Petra"), new SpielerNrName(4, "Klaus"),
				new SpielerNrName(12, "Heinz") };

		initReturnSpielerDaten(spielerNrNameList);

		try {
			meldeListeHelper.testDoppelteMeldungen();
		} catch (GenerateException exc) {
			fail("GenerateException", exc);
		}
	}

	@Test
	public void testZeileOhneSpielerNamenEntfernen() throws Exception {

		SpielerNrName[] spielerNrNameList = new SpielerNrName[] { new SpielerNrName(32, "Anna"), new SpielerNrName(10, "Petra"),
				// --------------------
				new SpielerNrName(4, ""), // muss entfernt werden
				// --------------------
				new SpielerNrName(12, "Heinz") };

		initReturnSpielerDaten(spielerNrNameList);
		meldeListeHelper.zeileOhneSpielerNamenEntfernen();
		verify(sheetHelperMock, times(1)).setTextInCell(any(StringCellValue.class));
	}

	private void initReturnSpielerDaten(SpielerNrName[] spielerNrnameList) throws GenerateException {

		Position spielerNrPos = Position.from(MeldeListeKonstanten.SPIELER_NR_SPALTE, MeldeListeKonstanten.ERSTE_DATEN_ZEILE);
		Position spielerNamePos = Position.from(meldeListeHelper.getSpielerNameSpalte(), MeldeListeKonstanten.ERSTE_DATEN_ZEILE);
		int zeileCntr = 0;
		for (SpielerNrName spielerNrName : spielerNrnameList) {
			int zeile = MeldeListeKonstanten.ERSTE_DATEN_ZEILE + zeileCntr;
			PowerMockito.when(sheetHelperMock.getIntFromCell(any(XSpreadsheet.class), eq(Position.from(spielerNrPos.zeile(zeile))))).thenReturn(spielerNrName.nr);
			PowerMockito.when(sheetHelperMock.getTextFromCell(any(XSpreadsheet.class), eq(Position.from(spielerNamePos.zeile(zeile))))).thenReturn(spielerNrName.name);
			zeileCntr++;
		}
		PowerMockito.when(meldungenSpalteMock.letzteZeileMitSpielerName()).thenReturn(MeldeListeKonstanten.ERSTE_DATEN_ZEILE + spielerNrnameList.length - 1);
		PowerMockito.when(meldungenSpalteMock.neachsteFreieDatenZeile()).thenReturn(MeldeListeKonstanten.ERSTE_DATEN_ZEILE + spielerNrnameList.length);
	}
}

class SpielerNrName {
	public final int nr;
	public final String name;

	SpielerNrName(int nr, String name) {
		this.nr = nr;
		this.name = name;
	}

}
