package de.petanqueturniermanager;

import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.sun.star.frame.XComponentLoader;
import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.comp.OfficeDocumentHelper;
import de.petanqueturniermanager.comp.OfficeStarter;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.helper.sheet.SheetHelper;

/**
 * Erstellung 13.07.2022 / Michael Massee
 */

public abstract class BaseCalcUITest {

	protected static XComponentLoader loader;
	protected static XSpreadsheetDocument doc;
	protected static OfficeStarter starter = OfficeStarter.from();
	protected static SheetHelper sheetHlp;
	protected static WorkingSpreadsheet wkingSpreadsheet;

	@BeforeClass
	public static void startup() {
		BaseCalcUITest.loader = starter.loadOffice().getComponentLoader();
		BaseCalcUITest.doc = OfficeDocumentHelper.from(loader).createCalc();
		if (BaseCalcUITest.doc == null) {
			System.out.println("Document creation failed");
			return;
		}
		OfficeDocumentHelper.setVisible(BaseCalcUITest.doc, true);
		BaseCalcUITest.wkingSpreadsheet = new WorkingSpreadsheet(BaseCalcUITest.starter.getxComponentContext(),
				BaseCalcUITest.doc);
		BaseCalcUITest.sheetHlp = new SheetHelper(BaseCalcUITest.starter.getxComponentContext(), BaseCalcUITest.doc);
		// use force weil office is clossed in shutdown
		ProcessBox.forceinit(BaseCalcUITest.starter.getxComponentContext());
	}

	@AfterClass
	public static void shutDown() {
		if (BaseCalcUITest.doc != null) {
			OfficeDocumentHelper.closeDoc(BaseCalcUITest.doc);
		}
		BaseCalcUITest.starter.closeOffice();
	}

	protected void waitEnter() throws IOException {
		System.out.println("Press Enter .....");
		System.in.read();
	}

}
