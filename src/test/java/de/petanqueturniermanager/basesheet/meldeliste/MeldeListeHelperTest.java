package de.petanqueturniermanager.basesheet.meldeliste;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.model.Spieler;
import de.petanqueturniermanager.model.SpielerMeldungen;

public class MeldeListeHelperTest {
	private MeldeListeHelper<SpielerMeldungen, Spieler> meldeListeHelper;

	private IMeldeliste<SpielerMeldungen, Spieler> iMeldelisteMock;
	private SheetHelper sheetHelperMock;
	private MeldungenSpalte<SpielerMeldungen, Spieler> meldungenSpalteMock;
	XSpreadsheet xSpreadsheetMock;

	@SuppressWarnings("unchecked")
	@BeforeEach
	public void init() throws GenerateException {
		iMeldelisteMock = Mockito.mock(IMeldeliste.class);
		sheetHelperMock = Mockito.mock(SheetHelper.class);
		meldungenSpalteMock = Mockito.mock(MeldungenSpalte.class);
		xSpreadsheetMock = Mockito.mock(XSpreadsheet.class);

		Mockito.when(iMeldelisteMock.getMeldungenSpalte()).thenReturn(meldungenSpalteMock);
		Mockito.when(iMeldelisteMock.getSheetHelper()).thenReturn(sheetHelperMock);

		meldeListeHelper = new MeldeListeHelper<SpielerMeldungen, Spieler>(iMeldelisteMock) {
			@Override
			public void doSort(int spalteNr, boolean isAscending) throws GenerateException {
				// nichts
			}

			@Override
			public XSpreadsheet getXSpreadSheet() {
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

		SpielerNrName[] spielerNrNameList = new SpielerNrName[] { new SpielerNrName(32, "Müller"),
				new SpielerNrName(10, "Müller") };

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
		SpielerNrName[] spielerNrNameList = new SpielerNrName[] { new SpielerNrName(32, "Maja   Biene"),
				new SpielerNrName(10, "Maja, Biene") };
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

		SpielerNrName[] spielerNrNameList = new SpielerNrName[] { new SpielerNrName(32, "Anna"),
				new SpielerNrName(10, "heinz"), new SpielerNrName(4, "Klaus"), new SpielerNrName(12, "Heinz") };

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

		SpielerNrName[] spielerNrNameList = new SpielerNrName[] { new SpielerNrName(32, "Anna"),
				new SpielerNrName(12, "Petra"), new SpielerNrName(4, "Klaus"), new SpielerNrName(12, "Heinz") };
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

		SpielerNrName[] spielerNrNameList = new SpielerNrName[] { new SpielerNrName(32, "Anna"),
				new SpielerNrName(10, "Petra"), new SpielerNrName(4, "Klaus"), new SpielerNrName(12, "Heinz") };

		initReturnSpielerDaten(spielerNrNameList);

		try {
			meldeListeHelper.testDoppelteMeldungen();
		} catch (GenerateException exc) {
			fail("GenerateException", exc);
		}
	}

	@Test
	public void testZeileOhneSpielerNamenEntfernen() throws Exception {

		// Achtung: die liste wird Sortiert mit leeren namen nach unten
		SpielerNrName[] spielerNrNameList = new SpielerNrName[] { new SpielerNrName(32, "Anna"),
				new SpielerNrName(10, "Petra"), new SpielerNrName(12, "Heinz"),
				// --------------------
				new SpielerNrName(4, "") // muss entfernt werden
				// --------------------
		};

		initReturnSpielerDaten(spielerNrNameList);

		Mockito.when(iMeldelisteMock.letzteZeileMitSpielerName())
				.thenReturn(MeldeListeKonstanten.ERSTE_DATEN_ZEILE + spielerNrNameList.length - 2);
		Mockito.when(meldungenSpalteMock.letzteZeileMitSpielerName())
				.thenReturn(MeldeListeKonstanten.ERSTE_DATEN_ZEILE + spielerNrNameList.length - 2);

		meldeListeHelper.zeileOhneSpielerNamenEntfernen();
		verify(sheetHelperMock, times(1)).clearValInCell(any(XSpreadsheet.class), any(Position.class));
	}

	private void initReturnSpielerDaten(SpielerNrName[] spielerNrnameList) throws GenerateException {

		Position spielerNrPos = Position.from(MeldeListeKonstanten.SPIELER_NR_SPALTE,
				MeldeListeKonstanten.ERSTE_DATEN_ZEILE);
		Position spielerNamePos = Position.from(meldeListeHelper.getSpielerNameSpalte(),
				MeldeListeKonstanten.ERSTE_DATEN_ZEILE);
		int zeileCntr = 0;
		for (SpielerNrName spielerNrName : spielerNrnameList) {
			int zeile = MeldeListeKonstanten.ERSTE_DATEN_ZEILE + zeileCntr;
			Mockito.when(sheetHelperMock.getIntFromCell(any(XSpreadsheet.class),
					eq(Position.from(spielerNrPos.zeile(zeile))))).thenReturn(spielerNrName.nr);
			Mockito.when(sheetHelperMock.getTextFromCell(any(XSpreadsheet.class),
					eq(Position.from(spielerNamePos.zeile(zeile))))).thenReturn(spielerNrName.name);
			zeileCntr++;
		}

		Mockito.when(iMeldelisteMock.letzteZeileMitSpielerName())
				.thenReturn(MeldeListeKonstanten.ERSTE_DATEN_ZEILE + spielerNrnameList.length - 1);
		Mockito.when(meldungenSpalteMock.letzteZeileMitSpielerName())
				.thenReturn(MeldeListeKonstanten.ERSTE_DATEN_ZEILE + spielerNrnameList.length - 1);

		Mockito.when(iMeldelisteMock.naechsteFreieDatenZeileInSpielerNrSpalte())
				.thenReturn(MeldeListeKonstanten.ERSTE_DATEN_ZEILE + spielerNrnameList.length);
		Mockito.when(meldungenSpalteMock.naechsteFreieDatenZeileInSpielerNrSpalte())
				.thenReturn(MeldeListeKonstanten.ERSTE_DATEN_ZEILE + spielerNrnameList.length);
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
