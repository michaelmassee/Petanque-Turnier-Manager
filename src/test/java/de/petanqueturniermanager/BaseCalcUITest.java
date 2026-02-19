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
import org.junit.Assume;
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
	// (via: ./gradlew reinstallExtension)
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
		try {
			installExtension();
			BaseCalcUITest.loader = starter.loadOffice().getComponentLoader();
		} catch (RuntimeException e) {
			Assume.assumeNoException(
					"LibreOffice nicht verfügbar oder Extension nicht installierbar – UITest wird übersprungen. "
							+ "Bitte sicherstellen dass LibreOffice installiert ist und './gradlew buildOXT' ausgeführt wurde.",
					e);
		}
	}

	/**
	 * Deinstalliert die alte Extension-Version und installiert die aktuelle OXT.<br>
	 * Wird automatisch von Gradle via reinstallExtension gebaut (test dependsOn reinstallExtension).<br>
	 * Für IDE-Starts: OXT muss vorher mit './gradlew buildOXT' gebaut worden sein.
	 */
	private static synchronized void installExtension() {
		// Veraltete Lock-Datei entfernen, falls vorhanden
		File lockFile = new File(System.getProperty("user.home") + "/.config/libreoffice/4/.lock");
		if (lockFile.exists()) {
			logger.warn("Veraltete Lock-Datei gefunden, wird entfernt: " + lockFile.getAbsolutePath());
			lockFile.delete();
		}

		// OXT-Datei suchen
		File projectDir = new File(System.getProperty("user.dir"));
		File distDir = new File(projectDir, "build/distributions");
		File[] oxtFiles = distDir.listFiles((dir, name) -> name.startsWith("PetanqueTurnierManager") && name.endsWith(".oxt"));
		if (oxtFiles == null || oxtFiles.length == 0) {
			throw new RuntimeException("OXT nicht gefunden in: " + distDir.getAbsolutePath()
					+ " – bitte zuerst './gradlew buildOXT' ausführen");
		}
		File oxtFile = oxtFiles[0];

		// Alte Version entfernen (Fehler werden ignoriert, z.B. wenn noch nicht installiert)
		runUnokg(true, "remove", "de.petanqueturniermanager");

		// Neue Version installieren
		logger.info("Installiere Extension: " + oxtFile.getAbsolutePath());
		int exitCode = runUnokg(false, "add", "-f", oxtFile.getAbsolutePath());
		if (exitCode != 0) {
			throw new RuntimeException("unopkg add fehlgeschlagen mit Exit-Code: " + exitCode);
		}
		logger.info("Extension erfolgreich installiert");
	}

	private static int runUnokg(boolean ignoreError, String... args) {
		try {
			String[] cmd = new String[args.length + 1];
			cmd[0] = "unopkg";
			System.arraycopy(args, 0, cmd, 1, args.length);
			ProcessBuilder pb = new ProcessBuilder(cmd);
			pb.inheritIO();
			int exitCode = pb.start().waitFor();
			if (!ignoreError && exitCode != 0) {
				logger.warn("unopkg " + String.join(" ", args) + " fehlgeschlagen mit Exit-Code: " + exitCode);
			}
			return exitCode;
		} catch (IOException | InterruptedException e) {
			if (ignoreError) {
				logger.warn("unopkg " + String.join(" ", args) + " fehlgeschlagen: " + e.getMessage());
				return -1;
			}
			throw new RuntimeException("Fehler beim Ausführen von unopkg " + String.join(" ", args), e);
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
