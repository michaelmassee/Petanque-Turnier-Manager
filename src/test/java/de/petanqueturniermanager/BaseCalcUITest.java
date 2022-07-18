package de.petanqueturniermanager;

import java.io.IOException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
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

	protected static OfficeStarter starter = OfficeStarter.from();
	protected static XComponentLoader loader;

	protected XSpreadsheetDocument doc;
	protected SheetHelper sheetHlp;
	protected WorkingSpreadsheet wkingSpreadsheet;

	@BeforeClass
	public static void startup() {
		BaseCalcUITest.loader = starter.loadOffice().getComponentLoader();
	}

	@Before
	public void beforeTest() {
		doc = OfficeDocumentHelper.from(loader).createCalc();
		if (doc == null) {
			System.out.println("Document creation failed");
			return;
		}

		OfficeDocumentHelper.setVisible(doc, true);
		wkingSpreadsheet = new WorkingSpreadsheet(starter.getxComponentContext(), doc);
		sheetHlp = new SheetHelper(starter.getxComponentContext(), doc);
		// use force weil calc is clossed in afterTest
		ProcessBox.forceinit(starter.getxComponentContext());
	}

	@AfterClass
	public static void shutDown() {
		BaseCalcUITest.starter.closeOffice();
	}

	@After
	public void afterTest() {
		ProcessBox.dispose();
		if (doc != null) {
			OfficeDocumentHelper.closeDoc(doc);
		}
	}

	protected void waitEnter() throws IOException {
		System.out.println("Press Enter .....");
		System.in.read();
	}

}
