/**
* Erstellung : 03.05.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee.meldeliste;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

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
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.konfiguration.DocumentPropertiesHelper;
import de.petanqueturniermanager.konfiguration.KonfigurationSheet;

public class AbstractMeldeListeSheetTest {
	private static final Logger logger = LogManager.getLogger(AbstractMeldeListeSheetTest.class);

	AbstractMeldeListeSheet meldeSheet;
	XComponentContext xComponentContextMock;
	SheetHelper sheetHelperMock;
	DocumentPropertiesHelper documentPropertiesHelperMock;
	XSpreadsheet xSpreadsheetMock;
	KonfigurationSheet konfigurationSheetMock;

	@Before
	public void setup() {
		this.xComponentContextMock = PowerMockito.mock(XComponentContext.class);
		this.sheetHelperMock = PowerMockito.mock(SheetHelper.class);
		this.documentPropertiesHelperMock = PowerMockito.mock(DocumentPropertiesHelper.class);
		this.xSpreadsheetMock = PowerMockito.mock(XSpreadsheet.class);
		this.konfigurationSheetMock = PowerMockito.mock(KonfigurationSheet.class);

		this.meldeSheet = new AbstractMeldeListeSheet(this.xComponentContextMock) {

			@Override
			KonfigurationSheet newKonfigurationSheet(XComponentContext xContext) {
				return AbstractMeldeListeSheetTest.this.konfigurationSheetMock;
			}

			@Override
			protected void doRun() throws GenerateException {
				// nichts!
			}

			@Override
			DocumentPropertiesHelper newDocumentPropertiesHelper(XComponentContext xContext) {
				return AbstractMeldeListeSheetTest.this.documentPropertiesHelperMock;
			}

			@Override
			public SheetHelper getSheetHelper() {
				return AbstractMeldeListeSheetTest.this.sheetHelperMock;
			}

			@Override
			public XSpreadsheet getSheet() {
				return AbstractMeldeListeSheetTest.this.xSpreadsheetMock;
			}

			@Override
			public Formation getFormation() {
				return Formation.MELEE;
			}

			@Override
			protected Logger getLogger() {
				return logger;
			}
		};
	}

	@Test
	public void testCountAnzSpieltage() throws Exception {
		String[] header = { "Spieltag 1", "2. Spieltag", "Spieltag 3" };
		setupReturn_from_getHeaderStringFromCell(Arrays.asList(header));
		int result = this.meldeSheet.countAnzSpieltage();
		assertThat(result).isEqualTo(3);
	}

	private void setupReturn_from_getHeaderStringFromCell(List<String> headerList) {

		Position headerPos = Position.from(this.meldeSheet.spieltagSpalte(1), AbstractMeldeListeSheet.HEADER_ZEILE);
		headerList.forEach(header -> {
			PowerMockito
					.when(this.sheetHelperMock.getTextFromCell(any(XSpreadsheet.class), eq(Position.from(headerPos))))
					.thenReturn(header);
			headerPos.spaltePlusEins();
		});

	}

}
