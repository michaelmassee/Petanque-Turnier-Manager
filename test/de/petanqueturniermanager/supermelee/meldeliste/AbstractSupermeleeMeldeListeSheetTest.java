/**
* Erstellung : 03.05.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee.meldeliste;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.konfiguration.KonfigurationSheet;
import de.petanqueturniermanager.supermelee.SpielTagNr;

public class AbstractSupermeleeMeldeListeSheetTest {
	private static final Logger logger = LogManager.getLogger(AbstractSupermeleeMeldeListeSheetTest.class);

	private AbstractSupermeleeMeldeListeSheet meldeSheet;
	private WorkingSpreadsheet workingSpreadsheetMock;
	private SheetHelper sheetHelperMock;
	private XSpreadsheet xSpreadsheetMock;
	private KonfigurationSheet konfigurationSheetMock;

	@Before
	public void setup() {
		this.workingSpreadsheetMock = PowerMockito.mock(WorkingSpreadsheet.class);
		this.sheetHelperMock = PowerMockito.mock(SheetHelper.class);
		this.xSpreadsheetMock = PowerMockito.mock(XSpreadsheet.class);
		this.konfigurationSheetMock = PowerMockito.mock(KonfigurationSheet.class);

		this.meldeSheet = new AbstractSupermeleeMeldeListeSheet(workingSpreadsheetMock) {

			@Override
			KonfigurationSheet newKonfigurationSheet(WorkingSpreadsheet workingSpreadsheet) {
				return AbstractSupermeleeMeldeListeSheetTest.this.konfigurationSheetMock;
			}

			@Override
			protected void doRun() throws GenerateException {
				// nichts!
			}

			@Override
			public XSpreadsheet getSheet() {
				return AbstractSupermeleeMeldeListeSheetTest.this.xSpreadsheetMock;
			}

			@Override
			public SheetHelper getSheetHelper() {
				return AbstractSupermeleeMeldeListeSheetTest.this.sheetHelperMock;
			}

			@Override
			public Formation getFormation() {
				return Formation.MELEE;
			}

			@Override
			public Logger getLogger() {
				return logger;
			}

			@Override
			public void processBoxinfo(String infoMsg) {
				// do nothing here
			}
		};

	}

	@Test
	public void testCountAnzSpieltageInMeldeliste() throws Exception {
		String[] header = { "1. Spieltag", "2. Spieltag", "3. Spieltag", "Spieltag" };
		setupReturn_from_getHeaderStringFromCell(Arrays.asList(header));
		int result = this.meldeSheet.countAnzSpieltageInMeldeliste();
		assertThat(result).isEqualTo(3);
	}

	private void setupReturn_from_getHeaderStringFromCell(List<String> headerList) throws GenerateException {

		Position headerPos = Position.from(this.meldeSheet.spieltagSpalte(SpielTagNr.from(1)), AbstractSupermeleeMeldeListeSheet.ZWEITE_HEADER_ZEILE);
		headerList.forEach(header -> {
			PowerMockito.when(this.sheetHelperMock.getTextFromCell(any(XSpreadsheet.class), eq(Position.from(headerPos)))).thenReturn(header);
			headerPos.spaltePlusEins();
		});

	}

	@Test
	public void testTestDoppelteNamenMitUmlauten() throws Exception {

		SpielerNrName[] spielerNrNameList = new SpielerNrName[] { new SpielerNrName(32, "Müller"), new SpielerNrName(10, "Müller") };

		initReturnSpielerDaten(spielerNrNameList);

		try {
			this.meldeSheet.testDoppelteDaten();
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
			this.meldeSheet.testDoppelteDaten();
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
			this.meldeSheet.testDoppelteDaten();
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
			this.meldeSheet.testDoppelteDaten();
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
			this.meldeSheet.testDoppelteDaten();
		} catch (GenerateException exc) {
			fail("GenerateException", exc);
		}
	}

	private void initReturnSpielerDaten(SpielerNrName[] spielerNrnameList) {

		Position spielerNrPos = Position.from(AbstractSupermeleeMeldeListeSheet.SPIELER_NR_SPALTE, AbstractSupermeleeMeldeListeSheet.ERSTE_DATEN_ZEILE);
		Position spielerNamePos = Position.from(this.meldeSheet.getSpielerNameSpalte(), AbstractSupermeleeMeldeListeSheet.ERSTE_DATEN_ZEILE);
		int zeileCntr = 0;
		for (SpielerNrName spielerNrName : spielerNrnameList) {
			int zeile = AbstractSupermeleeMeldeListeSheet.ERSTE_DATEN_ZEILE + zeileCntr;
			PowerMockito.when(this.sheetHelperMock.getIntFromCell(any(XSpreadsheet.class), eq(Position.from(spielerNrPos.zeile(zeile))))).thenReturn(spielerNrName.nr);
			PowerMockito.when(this.sheetHelperMock.getTextFromCell(any(XSpreadsheet.class), eq(Position.from(spielerNamePos.zeile(zeile))))).thenReturn(spielerNrName.name);
			zeileCntr++;
		}
	}

	@Test
	public void testZeileOhneSpielerNamenEntfernen() throws Exception {

		SpielerNrName[] spielerNrNameList = new SpielerNrName[] { new SpielerNrName(32, "Anna"), new SpielerNrName(10, "Petra"), new SpielerNrName(4, ""),
				new SpielerNrName(12, "Heinz") };

		initReturnSpielerDaten(spielerNrNameList);
		this.meldeSheet.zeileOhneSpielerNamenEntfernen();
		verify(this.sheetHelperMock, times(1)).setTextInCell(any(StringCellValue.class));
	}

	@Test
	public void testCleanUpSpielerName() throws Exception {
		String result = meldeSheet.cleanUpSpielerName(" ÄÜö # + ösX  YSDdRfÖßßäslkg .,m..,m  ");
		assertThat(result).isEqualTo("äüöösxysddrfößßäslkgmm");
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
