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

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.SuperMeleeKonfigurationSheet;

public class AbstractSupermeleeMeldeListeSheetTest {
	static final Logger logger = LogManager.getLogger(AbstractSupermeleeMeldeListeSheetTest.class);

	private AbstractSupermeleeMeldeListeSheet meldeSheet;
	private WorkingSpreadsheet workingSpreadsheetMock;
	SheetHelper sheetHelperMock;
	XSpreadsheet xSpreadsheetMock;
	SuperMeleeKonfigurationSheet konfigurationSheetMock;

	@Before
	public void setup() {
		workingSpreadsheetMock = PowerMockito.mock(WorkingSpreadsheet.class);
		sheetHelperMock = PowerMockito.mock(SheetHelper.class);
		xSpreadsheetMock = PowerMockito.mock(XSpreadsheet.class);
		konfigurationSheetMock = PowerMockito.mock(SuperMeleeKonfigurationSheet.class);

		meldeSheet = new AbstractSupermeleeMeldeListeSheet(workingSpreadsheetMock) {

			@Override
			protected SuperMeleeKonfigurationSheet getKonfigurationSheet() {
				return konfigurationSheetMock;
			}

			@Override
			protected void doRun() throws GenerateException {
				// nichts!
			}

			@Override
			public XSpreadsheet getSheet() {
				return xSpreadsheetMock;
			}

			@Override
			public SheetHelper getSheetHelper() {
				return sheetHelperMock;
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
		int result = meldeSheet.countAnzSpieltageInMeldeliste();
		assertThat(result).isEqualTo(3);
	}

	private void setupReturn_from_getHeaderStringFromCell(List<String> headerList) throws GenerateException {

		Position headerPos = Position.from(meldeSheet.spieltagSpalte(SpielTagNr.from(1)), AbstractSupermeleeMeldeListeSheet.ZWEITE_HEADER_ZEILE);
		headerList.forEach(header -> {
			PowerMockito.when(sheetHelperMock.getTextFromCell(any(XSpreadsheet.class), eq(Position.from(headerPos)))).thenReturn(header);
			headerPos.spaltePlusEins();
		});

	}

	@Test
	public void testTestDoppelteNamenMitUmlauten() throws Exception {

		SpielerNrName[] spielerNrNameList = new SpielerNrName[] { new SpielerNrName(32, "Müller"), new SpielerNrName(10, "Müller") };

		initReturnSpielerDaten(spielerNrNameList);

		try {
			meldeSheet.testDoppelteDaten();
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
			meldeSheet.testDoppelteDaten();
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
			meldeSheet.testDoppelteDaten();
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
			meldeSheet.testDoppelteDaten();
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
			meldeSheet.testDoppelteDaten();
		} catch (GenerateException exc) {
			fail("GenerateException", exc);
		}
	}

	private void initReturnSpielerDaten(SpielerNrName[] spielerNrnameList) {

		Position spielerNrPos = Position.from(AbstractSupermeleeMeldeListeSheet.SPIELER_NR_SPALTE, AbstractSupermeleeMeldeListeSheet.ERSTE_DATEN_ZEILE);
		Position spielerNamePos = Position.from(meldeSheet.getSpielerNameSpalte(), AbstractSupermeleeMeldeListeSheet.ERSTE_DATEN_ZEILE);
		int zeileCntr = 0;
		for (SpielerNrName spielerNrName : spielerNrnameList) {
			int zeile = AbstractSupermeleeMeldeListeSheet.ERSTE_DATEN_ZEILE + zeileCntr;
			PowerMockito.when(sheetHelperMock.getIntFromCell(any(XSpreadsheet.class), eq(Position.from(spielerNrPos.zeile(zeile))))).thenReturn(spielerNrName.nr);
			PowerMockito.when(sheetHelperMock.getTextFromCell(any(XSpreadsheet.class), eq(Position.from(spielerNamePos.zeile(zeile))))).thenReturn(spielerNrName.name);
			zeileCntr++;
		}
	}

	@Test
	public void testZeileOhneSpielerNamenEntfernen() throws Exception {

		SpielerNrName[] spielerNrNameList = new SpielerNrName[] { new SpielerNrName(32, "Anna"), new SpielerNrName(10, "Petra"), new SpielerNrName(4, ""),
				new SpielerNrName(12, "Heinz") };

		initReturnSpielerDaten(spielerNrNameList);
		meldeSheet.zeileOhneSpielerNamenEntfernen();
		verify(sheetHelperMock, times(1)).setTextInCell(any(StringCellValue.class));
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
