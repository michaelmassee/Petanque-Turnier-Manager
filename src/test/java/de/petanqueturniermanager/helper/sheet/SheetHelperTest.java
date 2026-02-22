package de.petanqueturniermanager.helper.sheet;

import static org.mockito.ArgumentMatchers.any;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.text.XText;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.position.Position;

public class SheetHelperTest {

	private SheetHelper sheetHelper = null;
	private XSpreadsheet xSpreadsheetMock;
	private WorkingSpreadsheet workingSpreadsheetMock;

	@Before
	public void setUp() {
		xSpreadsheetMock = Mockito.mock(XSpreadsheet.class);
		workingSpreadsheetMock = Mockito.mock(WorkingSpreadsheet.class);
		sheetHelper = new SheetHelper(workingSpreadsheetMock);
	}

	@Test
	public void testSetStringValueInCellXSpreadsheetPositionStringBoolean() throws Exception {

		int zeile = 12;
		int spalte = 8;

		String testWert = "Testwert";

		XText xTextMock = Mockito.mock(XText.class);

		sheetHelper = new SheetHelper(workingSpreadsheetMock) {
			@SuppressWarnings("unchecked")
			@Override
			<C> C queryInterface(Class<C> clazz, Object arg) {
				return (C) xTextMock;
			}
		};
		Position testPos = Position.from(spalte, zeile);

		Mockito.when(xTextMock.getString()).thenReturn("     ");
		sheetHelper.setStringValueInCell(xSpreadsheetMock, testPos, testWert, true);// überschreiben
		Mockito.verify(xTextMock, Mockito.times(1)).setString(testWert);

		sheetHelper.setStringValueInCell(xSpreadsheetMock, testPos, testWert, false); // überschreiben weil leer
		Mockito.verify(xTextMock, Mockito.times(2)).setString(testWert);

		Mockito.when(xTextMock.getString()).thenReturn("bla bla");
		sheetHelper.setStringValueInCell(xSpreadsheetMock, testPos, testWert, true); // überschreiben
		Mockito.verify(xTextMock, Mockito.times(3)).setString(testWert);

		sheetHelper.setStringValueInCell(xSpreadsheetMock, testPos, testWert, false); // nicht überschreiben
		Mockito.verify(xTextMock, Mockito.times(3)).setString(any(String.class));
	}
}
