/**
* Erstellung : 03.05.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee.meldeliste;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.msgbox.ErrorMessageBox;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.konfiguration.KonfigurationSheet;
import de.petanqueturniermanager.supermelee.SpielTagNr;

public class AbstractSupermeleeMeldeListeSheetTest {
	private static final Logger logger = LogManager.getLogger(AbstractSupermeleeMeldeListeSheetTest.class);

	AbstractSupermeleeMeldeListeSheet meldeSheet;
	XComponentContext xComponentContextMock;
	SheetHelper sheetHelperMock;
	XSpreadsheet xSpreadsheetMock;
	KonfigurationSheet konfigurationSheetMock;
	ErrorMessageBox errorMessageBoxMock;

	@Before
	public void setup() {
		this.xComponentContextMock = PowerMockito.mock(XComponentContext.class);
		this.sheetHelperMock = PowerMockito.mock(SheetHelper.class);
		this.xSpreadsheetMock = PowerMockito.mock(XSpreadsheet.class);
		this.konfigurationSheetMock = PowerMockito.mock(KonfigurationSheet.class);
		this.errorMessageBoxMock = PowerMockito.mock(ErrorMessageBox.class);

		this.meldeSheet = new AbstractSupermeleeMeldeListeSheet(this.xComponentContextMock) {

			@Override
			protected ErrorMessageBox newErrMsgBox() {
				return AbstractSupermeleeMeldeListeSheetTest.this.errorMessageBoxMock;
			}

			@Override
			KonfigurationSheet newKonfigurationSheet(XComponentContext xContext) {
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
		};

		PowerMockito.when(this.errorMessageBoxMock.showOk(any(String.class), any(String.class))).thenReturn((short) 1);
	}

	@Test
	public void testCountAnzSpieltage() throws Exception {
		String[] header = { "Spieltag 1", "2. Spieltag", "Spieltag 3" };
		setupReturn_from_getHeaderStringFromCell(Arrays.asList(header));
		int result = this.meldeSheet.countAnzSpieltage();
		assertThat(result).isEqualTo(3);
	}

	private void setupReturn_from_getHeaderStringFromCell(List<String> headerList) throws GenerateException {

		Position headerPos = Position.from(this.meldeSheet.spieltagSpalte(SpielTagNr.from(1)),
				AbstractSupermeleeMeldeListeSheet.HEADER_ZEILE);
		headerList.forEach(header -> {
			PowerMockito
					.when(this.sheetHelperMock.getTextFromCell(any(XSpreadsheet.class), eq(Position.from(headerPos))))
					.thenReturn(header);
			headerPos.spaltePlusEins();
		});

	}

	@Test
	public void testTestDoppelteNamenIgnoreCase() throws Exception {

		String[] spielerDoppelteNameList = new String[] { "Anna", "heinz", "Klaus", "Heinz" };
		Integer[] spielerNrList = new Integer[] { 32, 10, 4, 12 };

		initDoppelteDaten(spielerDoppelteNameList, spielerNrList);

		boolean result = this.meldeSheet.testDoppelteDaten();
		assertThat(result).isEqualTo(true);
		verify(this.errorMessageBoxMock, times(1)).showOk(any(String.class), any(String.class));
	}

	@Test
	public void testTestDoppelteSpielrNr() throws Exception {

		String[] spielerNameList = new String[] { "Anna", "Petra", "Klaus", "Heinz" };
		Integer[] spielerDoppelteNrList = new Integer[] { 32, 12, 4, 12 };

		initDoppelteDaten(spielerNameList, spielerDoppelteNrList);

		boolean result = this.meldeSheet.testDoppelteDaten();
		assertThat(result).isEqualTo(true);
		verify(this.errorMessageBoxMock, times(1)).showOk(any(String.class), any(String.class));
	}

	@Test
	public void testDoppelteDaten_NoError() throws Exception {

		String[] spielerNameList = new String[] { "Anna", "Petra", "Klaus", "Heinz" };
		Integer[] spielerNrList = new Integer[] { 32, 10, 4, 12 };

		initDoppelteDaten(spielerNameList, spielerNrList);

		boolean result = this.meldeSheet.testDoppelteDaten();
		assertThat(result).isEqualTo(false);
	}

	private void initDoppelteDaten(String[] spielerNameList, Integer[] spielerDoppelteNrList) {

		Position spielerNrPos = Position.from(AbstractSupermeleeMeldeListeSheet.SPIELER_NR_SPALTE,
				AbstractSupermeleeMeldeListeSheet.ERSTE_DATEN_ZEILE);
		Position spielerNamePos = Position.from(this.meldeSheet.getSpielerNameSpalte(),
				AbstractSupermeleeMeldeListeSheet.ERSTE_DATEN_ZEILE);
		int zeileCntr = 0;
		for (String spielerName : spielerNameList) {
			int zeile = AbstractSupermeleeMeldeListeSheet.ERSTE_DATEN_ZEILE + zeileCntr;

			PowerMockito.when(this.sheetHelperMock.getTextFromCell(any(XSpreadsheet.class),
					eq(Position.from(spielerNamePos.zeile(zeile))))).thenReturn(spielerName);

			PowerMockito.when(this.sheetHelperMock.getIntFromCell(any(XSpreadsheet.class),
					eq(Position.from(spielerNrPos.zeile(zeile))))).thenReturn(spielerDoppelteNrList[zeileCntr]);
			zeileCntr++;
		}
	}

}
