/**
* Erstellung : 03.05.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee.meldeliste;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeKonstanten;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.SupermeleeTeamPaarungenSheet;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeKonfigurationSheet;

public class AbstractSupermeleeMeldeListeSheetTest {
	static final Logger logger = LogManager.getLogger(AbstractSupermeleeMeldeListeSheetTest.class);

	private AbstractSupermeleeMeldeListeSheet meldeSheet;
	private WorkingSpreadsheet workingSpreadsheetMock;
	SheetHelper sheetHelperMock;
	XSpreadsheet xSpreadsheetMock;
	SuperMeleeKonfigurationSheet konfigurationSheetMock;
	SupermeleeTeamPaarungenSheet supermeleeTeamPaarungenMock;

	@Before
	public void setup() {
		workingSpreadsheetMock = PowerMockito.mock(WorkingSpreadsheet.class);
		sheetHelperMock = PowerMockito.mock(SheetHelper.class);
		xSpreadsheetMock = PowerMockito.mock(XSpreadsheet.class);
		konfigurationSheetMock = PowerMockito.mock(SuperMeleeKonfigurationSheet.class);
		supermeleeTeamPaarungenMock = PowerMockito.mock(SupermeleeTeamPaarungenSheet.class);

		meldeSheet = new AbstractSupermeleeMeldeListeSheet(workingSpreadsheetMock) {

			@Override
			SupermeleeTeamPaarungenSheet initSupermeleeTeamPaarungenSheet() {
				return supermeleeTeamPaarungenMock;
			}

			@Override
			protected SuperMeleeKonfigurationSheet getKonfigurationSheet() {
				return konfigurationSheetMock;
			}

			@Override
			protected void doRun() throws GenerateException {
				// nichts!
			}

			@Override
			public XSpreadsheet getXSpreadSheet() {
				return xSpreadsheetMock;
			}

			@Override
			public SheetHelper getSheetHelper() {
				return sheetHelperMock;
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

	private void setupReturn_from_getHeaderStringFromCell(List<String> headerList) {

		Position headerPos = Position.from(meldeSheet.spieltagSpalte(SpielTagNr.from(1)), MeldeListeKonstanten.ZWEITE_HEADER_ZEILE);
		headerList.forEach(header -> {
			PowerMockito.when(sheetHelperMock.getTextFromCell(any(XSpreadsheet.class), eq(Position.from(headerPos)))).thenReturn(header);
			headerPos.spaltePlusEins();
		});

	}
}
