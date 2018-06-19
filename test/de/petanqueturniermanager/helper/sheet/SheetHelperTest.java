package de.petanqueturniermanager.helper.sheet;

import static org.mockito.ArgumentMatchers.any;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.text.XText;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.helper.position.Position;

public class SheetHelperTest {

	private SheetHelper sheetHelper = null;
	private XComponentContext xComponentContextMock;
	private XSpreadsheet xSpreadsheetMock;

	@Before
	public void setUp() {
		xComponentContextMock = PowerMockito.mock(XComponentContext.class);
		xSpreadsheetMock = PowerMockito.mock(XSpreadsheet.class);
		sheetHelper = new SheetHelper(xComponentContextMock);
	}

	@Test
	public void testSetTextInCellXSpreadsheetPositionStringBoolean() throws Exception {

		int zeile = 12;
		int spalte = 8;

		String testWert = "Testwert";

		XText xTextMock = PowerMockito.mock(XText.class);

		sheetHelper = new SheetHelper(xComponentContextMock) {
			@Override
			<C> C queryInterface(Class<C> clazz, Object arg) {
				return (C) xTextMock;
			}
		};
		Position testPos = Position.from(spalte, zeile);

		PowerMockito.when(xTextMock.getString()).thenReturn("     ");
		sheetHelper.setTextInCell(xSpreadsheetMock, testPos, testWert, true);// überschreiben
		Mockito.verify(xTextMock, Mockito.times(1)).setString(testWert);

		sheetHelper.setTextInCell(xSpreadsheetMock, testPos, testWert, false); // überschreiben weil leer
		Mockito.verify(xTextMock, Mockito.times(2)).setString(testWert);

		PowerMockito.when(xTextMock.getString()).thenReturn("bla bla");
		sheetHelper.setTextInCell(xSpreadsheetMock, testPos, testWert, true); // überschreiben
		Mockito.verify(xTextMock, Mockito.times(3)).setString(testWert);

		sheetHelper.setTextInCell(xSpreadsheetMock, testPos, testWert, false); // nicht überschreiben
		Mockito.verify(xTextMock, Mockito.times(3)).setString(any(String.class));
	}
}
