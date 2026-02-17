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
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.comp.OfficeDocumentHelper;
import de.petanqueturniermanager.comp.OfficeStarter;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
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

	// Verwende die UserInstallation ~/.config/libreoffice/4, wo das Plugin installiert ist
	// (via: unopkg add --force build/distributions/PetanqueTurnierManager-1.0.0.oxt)
	final protected static OfficeStarter starter = OfficeStarter.from()
			.headless(true)
			.userInstallation("file://" + System.getProperty("user.home") + "/.config/libreoffice/4");
	protected static XComponentLoader loader;

	protected XSpreadsheetDocument doc;
	protected SheetHelper sheetHlp;
	protected WorkingSpreadsheet wkingSpreadsheet;
	protected DocumentPropertiesHelper docPropHelper;

	@BeforeClass
	public static void startup() {
		installExtension();
		BaseCalcUITest.loader = starter.loadOffice().getComponentLoader();
	}

	/**
	 * Installiert das Plugin (OXT) in LibreOffice bevor die Tests starten.
	 * Die OXT wird vom Gradle buildOXT Task gebaut (test dependsOn buildOXT).
	 */
	private static void installExtension() {
		File projectDir = new File(System.getProperty("user.dir"));
		File oxtFile = new File(projectDir, "build/distributions/PetanqueTurnierManager-1.0.0.oxt");

		if (!oxtFile.exists()) {
			throw new RuntimeException("OXT nicht gefunden: " + oxtFile.getAbsolutePath()
					+ " - bitte zuerst './gradlew buildOXT' ausführen");
		}

		logger.info("Installiere Extension: " + oxtFile.getAbsolutePath());
		try {
			ProcessBuilder pb = new ProcessBuilder("unopkg", "add", "--force", oxtFile.getAbsolutePath());
			pb.inheritIO();
			Process process = pb.start();
			int exitCode = process.waitFor();
			if (exitCode != 0) {
				throw new RuntimeException("unopkg add fehlgeschlagen mit Exit-Code: " + exitCode);
			}
			logger.info("Extension erfolgreich installiert");
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException("Fehler beim Installieren der Extension", e);
		}
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
		docPropHelper = new DocumentPropertiesHelper(wkingSpreadsheet);
		docPropHelper.setBooleanProperty(BasePropertiesSpalte.KONFIG_PROP_ZEIGE_ARBEITS_SPALTEN, true);
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

		assertThat(jsonFile).isNotNull();
		assertThat(rangeData).isNotNull().isNotEmpty();

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		RangeData refRangeData = gson.fromJson(new BufferedReader(new InputStreamReader(jsonFile)), RangeData.class);

		assertThat(refRangeData).isNotNull().isNotEmpty();
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

	public void writeToJson(String fileName, RangePosition rangePosition, XSpreadsheet xSpreadsheet,
			XSpreadsheetDocument xSpreadsheetDocument) {
		writeToJson(fileName, rangeDateFromRangePosition(rangePosition, xSpreadsheet, xSpreadsheetDocument));
	}

	public RangeData rangeDateFromRangePosition(RangePosition rangePosition, XSpreadsheet xSpreadsheet,
			XSpreadsheetDocument xSpreadsheetDocument) {
		RangeHelper rngHlpr = RangeHelper.from(xSpreadsheet, xSpreadsheetDocument, rangePosition);
		return rngHlpr.getDataFromRange();
	}

	public void writeToJson(String fileName, RangeData rangeData) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		try {
			String currentUsersHomeDir = System.getProperty("user.home");
			String baseDir = currentUsersHomeDir + File.separator;
			File jsonFile = new File(baseDir, fileName);
			System.out.println("Write : " + jsonFile);

			try (BufferedWriter fileStream = new BufferedWriter(new FileWriter(jsonFile))) {
				fileStream.write(gson.toJson(rangeData));
			}

		} catch (JsonIOException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
