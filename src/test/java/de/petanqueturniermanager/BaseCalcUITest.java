package de.petanqueturniermanager;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.sun.star.frame.XComponentLoader;
import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.comp.OfficeDocumentHelper;
import de.petanqueturniermanager.comp.OfficeStarter;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.CellData;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;

/**
 * Erstellung 13.07.2022 / Michael Massee<br>
 * hier wird eine seperate Instance erstellt, nur ueber in Socket werden die Sheets angesteuert.<br>
 * das bedeutet das das Propertie Plugin nicht die gleiche Daten hat !<br>
 * siehe GlobalImpl.getDocumentPropertiesHelper()
 * 
 */

public abstract class BaseCalcUITest {

	private static final Logger logger = LogManager.getLogger(BaseCalcUITest.class);

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

	protected void validateWithJson(RangeData rangeData, InputStream jsonFile) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		RangeData refRangeData = gson.fromJson(new BufferedReader(new InputStreamReader(jsonFile)), RangeData.class);

		assertThat(rangeData).hasSameSizeAs(refRangeData);

		int idx = 0;
		// jede zeile vergleichen, wegen fehlermeldung 
		for (RowData data : refRangeData) {
			List<String> expected = refRangeData.get(idx).stream().map(c -> c.getStringVal())
					.collect(Collectors.toList());
			logger.info("Validate Zeile :" + expected);
			assertThat(data).extracting(CellData::getStringVal).containsExactlyElementsOf(expected);
			idx++;
		}
	}

	protected void writeToJson(String fileName, RangeData rangeData) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		try {
			File jsoFile = new File("/home/michael/tmp/", fileName);
			try (BufferedWriter fileStream = new BufferedWriter(new FileWriter(jsoFile))) {
				fileStream.write(gson.toJson(rangeData));
			}
		} catch (JsonIOException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
