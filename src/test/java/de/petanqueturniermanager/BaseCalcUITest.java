package de.petanqueturniermanager;

import static org.assertj.core.api.Assertions.assertThat;

import javax.swing.JOptionPane;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XNamed;
import com.sun.star.sheet.XCalculatable;
import com.sun.star.util.CellProtection;
import com.sun.star.util.XProtectable;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.sun.star.frame.XComponentLoader;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.comp.OfficeDocumentHelper;
import de.petanqueturniermanager.comp.OfficeStarter;
import de.petanqueturniermanager.comp.PetanqueTurnierMngrSingleton;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzManager;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzRegistry;
import de.petanqueturniermanager.helper.sheet.blattschutz.IBlattschutzKonfiguration;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.toolbar.TurnierModus;

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
			.headless(Boolean.parseBoolean(System.getProperty("uitest.headless", "false")))
			.userInstallation("file://" + System.getProperty("user.home") + "/.config/libreoffice/4");
	protected static XComponentLoader loader;

	protected XSpreadsheetDocument doc;
	protected SheetHelper sheetHlp;
	protected WorkingSpreadsheet wkingSpreadsheet;
	protected DocumentPropertiesHelper docPropHelper;

	@BeforeAll
	public static void startup() {
		try {
			installExtension();
			BaseCalcUITest.loader = starter.loadOffice().getComponentLoader();
		} catch (RuntimeException e) {
			Assumptions.abort(
					"LibreOffice nicht verfügbar oder Extension nicht installierbar – UITest wird übersprungen. "
							+ "Bitte sicherstellen dass LibreOffice installiert ist und './gradlew buildOXT' ausgeführt wurde.");
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
		File oxtFile = resolveOxtFile(projectDir, distDir);

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

	private static File resolveOxtFile(File projectDir, File distDir) {
		File descriptionXml = new File(projectDir, "description.xml");
		if (!descriptionXml.exists()) {
			throw new RuntimeException("description.xml nicht gefunden: " + descriptionXml.getAbsolutePath());
		}
		try {
			javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(false);
			org.w3c.dom.NodeList versionNodes = factory.newDocumentBuilder().parse(descriptionXml)
					.getElementsByTagName("version");
			if (versionNodes.getLength() == 0) {
				throw new RuntimeException("Kein <version>-Element in description.xml gefunden");
			}
			String version = versionNodes.item(0).getAttributes().getNamedItem("value").getNodeValue();
			File oxtFile = new File(distDir, "PetanqueTurnierManager-" + version + ".oxt");
			if (!oxtFile.exists()) {
				throw new RuntimeException("OXT nicht gefunden (description.xml version=" + version + "): "
						+ oxtFile.getAbsolutePath() + " – bitte './gradlew buildOXT' ausführen");
			}
			logger.info("OXT-Version aus description.xml: " + version);
			return oxtFile;
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("description.xml konnte nicht gelesen werden: " + e.getMessage(), e);
		}
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

	@BeforeEach
	public void beforeTest() {
		// Sicherstellen, dass der statische Koordinator vor jedem Test im Ausgangszustand ist.
		// Verhindert, dass ein gestörter Zustand aus einem vorherigen Test (z.B. durch einen
		// LibreOffice-Event der async zwischen finally-Block und dem nächsten @BeforeEach feuert)
		// das laeuft-Flag auf true hält und run()-Aufrufe im nächsten Test als No-Op ausführt.
		SheetRunner.koordinator.zuruecksetzen();
		assertThat(loader).as("LibreOffice loader nicht verfügbar – UITest fehlgeschlagen").isNotNull();
		doc = OfficeDocumentHelper.from(loader).createCalc();
		assertThat(doc).as("LibreOffice Calc-Dokument konnte nicht erstellt werden – UITest fehlgeschlagen").isNotNull();

		if (!Boolean.parseBoolean(System.getProperty("uitest.headless", "false"))) {
			OfficeDocumentHelper.setVisible(doc, true);
		}
		wkingSpreadsheet = new WorkingSpreadsheet(starter.getxComponentContext(), doc);
		sheetHlp = new SheetHelper(starter.getxComponentContext(), doc);
		docPropHelper = new DocumentPropertiesHelper(wkingSpreadsheet);
		// I18n testseitig mit Deutsch-Fallback initialisieren (locale-unabhängig)
		I18n.init(null);
		// use force weil calc is clossed in afterTest
		var headless = Boolean.parseBoolean(System.getProperty("uitest.headless", "false"));
		ProcessBox.setHeadlessMode(headless);
		MessageBox.setDialogeUeberspringen(headless);
		ProcessBox.forceinit(starter.getxComponentContext());
	}

	@AfterAll
	public static void shutDown() {
		BaseCalcUITest.starter.closeOffice();
		// Static-State der Test-JVM zurücksetzen: turnierEventHandler-Dispatcher und sharedContext
		// zeigen sonst auf die soeben terminierte LO-Instanz und reißen die nächste Test-Klasse
		// mit DisposedException ab.
		PetanqueTurnierMngrSingleton.resetForTest();
	}

	@AfterEach
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

	protected void warten(String titel) {
		JOptionPane.showMessageDialog(null, "LibreOffice inspizieren, dann OK klicken.", titel,
				JOptionPane.INFORMATION_MESSAGE);
	}

	protected void validateWithJson(RangeData rangeData, InputStream jsonFile) {
		RangeJsonAssert.validate(rangeData, jsonFile);
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

	protected void recalcAll() {
		XCalculatable xCalc = Lo.qi(XCalculatable.class, doc);
		if (xCalc != null) {
			xCalc.calculateAll();
		}
	}

	/**
	 * Aktion, die unter Kiosk-Modus laufen soll. Darf {@link GenerateException} werfen,
	 * weil die meisten Produktiv-Pfade (doRun, generate, …) genau diese Checked Exception
	 * deklarieren.
	 */
	@FunctionalInterface
	protected interface KioskAktion {
		void ausfuehren() throws GenerateException;
	}

	/**
	 * Führt {@code aktion} unter aktivem TurnierModus + Blattschutz für das übergebene
	 * Turniersystem aus. Vorbild ist der bestehende {@code kioskModus_…}-Test in
	 * {@code RanglisteUpdaterUITest}. Schritte:
	 * <ol>
	 *   <li>{@link TurnierModus#setAktivForTest(boolean) setAktivForTest(true)}</li>
	 *   <li>{@link BlattschutzManager#schuetzen schuetzen()} (legt Schutz an)</li>
	 *   <li>{@link BlattschutzManager#beginCommandScope beginCommandScope()} (Lazy-Unprotect-Scope wie in {@code SheetRunner.run()})</li>
	 *   <li>Aktion ausführen</li>
	 *   <li>{@code endCommandScope()}</li>
	 *   <li>{@link #pruefeSchutzInvariante Schutz-Invariante prüfen}: alle Sheets müssen noch geschützt sein,
	 *       deklarierte editierbare Bereiche müssen weiterhin {@code IsLocked=false} aufweisen</li>
	 *   <li>{@link BlattschutzManager#entsperren entsperren()} + {@code setAktivForTest(false)} im finally-Block</li>
	 * </ol>
	 */
	protected void mitKioskModus(TurnierSystem ts, KioskAktion aktion) throws GenerateException {
		IBlattschutzKonfiguration konfig = BlattschutzRegistry.fuer(ts).orElseThrow(() ->
				new IllegalStateException("Keine BlattschutzKonfiguration für " + ts));
		TurnierModus.get().setAktivForTest(true);
		BlattschutzManager.get().schuetzen(konfig, wkingSpreadsheet);
		// Snapshot der Sheet-Namen, die zum Schutzzeitpunkt in der Konfig waren.
		// Operationen wie SpielrundePlan / EndranglisteSheet / JGJSpielPlanSheet legen
		// während des Laufs neue Sheets an oder ersetzen bestehende; die Invariante
		// soll nur über bereits initial vorhandene Schutz-Sheets greifen und die
		// Sheets per Name re-resolven, damit stale XSpreadsheet-Referenzen nicht
		// fälschlich fehlschlagen.
		var initialeSheetNamen = sheetNamen(konfig.berechneSchutzInfos(wkingSpreadsheet));
		try {
			BlattschutzManager.get().beginCommandScope(konfig, wkingSpreadsheet);
			try {
				aktion.ausfuehren();
			} finally {
				BlattschutzManager.get().endCommandScope();
			}
			pruefeSchutzInvariante(konfig, initialeSheetNamen);
		} finally {
			BlattschutzManager.get().entsperren(konfig, wkingSpreadsheet);
			TurnierModus.get().setAktivForTest(false);
		}
	}

	private java.util.Set<String> sheetNamen(
			java.util.List<de.petanqueturniermanager.helper.sheet.blattschutz.SheetSchutzInfo> infos) {
		var namen = new java.util.HashSet<String>();
		for (var info : infos) {
			var named = Lo.qi(com.sun.star.container.XNamed.class, info.sheet());
			if (named != null) {
				namen.add(named.getName());
			}
		}
		return namen;
	}

	/**
	 * Variante ohne Blattschutz – nur das {@code aktiv}-Flag wird gesetzt. Für Tests
	 * gedacht, deren Logik {@code TurnierModus.istAktiv()} abfragt, aber kein konkretes
	 * Turniersystem mit Sheets zur Verfügung steht (Helper-, Sidebar-, AddIn-Tests etc.).
	 */
	protected void mitKioskModusOhneSchutz(KioskAktion aktion) throws GenerateException {
		TurnierModus.get().setAktivForTest(true);
		try {
			aktion.ausfuehren();
		} finally {
			TurnierModus.get().setAktivForTest(false);
		}
	}

	/**
	 * Smoke-Check + Schutz-Invariante: für jedes Sheet, das beim Eintritt in den
	 * Kiosk-Modus in der Konfig war (per Name identifiziert) und das nach der Aktion
	 * noch im Dokument vorhanden ist, muss gelten: Sheet weiterhin geschützt,
	 * deklarierte editierbare Bereiche noch editierbar.
	 */
	private void pruefeSchutzInvariante(IBlattschutzKonfiguration konfig,
			java.util.Set<String> initialeSheetNamen) {
		var aktuelleInfos = konfig.berechneSchutzInfos(wkingSpreadsheet);
		SoftAssertions soft = new SoftAssertions();
		for (var info : aktuelleInfos) {
			var named = Lo.qi(com.sun.star.container.XNamed.class, info.sheet());
			if (named == null || !initialeSheetNamen.contains(named.getName())) {
				continue; // neu erzeugtes Sheet – nicht durch initiales schuetzen() abgedeckt
			}
			XProtectable prot = Lo.qi(XProtectable.class, info.sheet());
			if (prot == null) {
				soft.fail("Sheet ohne XProtectable angetroffen");
				continue;
			}
			String sheetName = named.getName();
			soft.assertThat(prot.isProtected())
					.as("Sheet '%s' muss nach Operation noch geschützt sein", sheetName)
					.isTrue();
			for (RangePosition bereich : info.editierbareBereich()) {
				int spalte = bereich.getStartSpalte();
				int zeile = bereich.getStartZeile();
				try {
					var cell = info.sheet().getCellByPosition(spalte, zeile);
					XPropertySet props = Lo.qi(XPropertySet.class, cell);
					CellProtection cellProt = (CellProtection) props.getPropertyValue("CellProtection");
					soft.assertThat(cellProt.IsLocked)
							.as("Sheet '%s' editierbarer Bereich (Spalte=%d, Zeile=%d) muss IsLocked=false bleiben",
									sheetName, spalte, zeile)
							.isFalse();
				} catch (Exception e) {
					soft.fail("Sheet '%s' Fehler beim Lesen von CellProtection (Spalte=%d, Zeile=%d): %s",
							sheetName, spalte, zeile, e.getMessage());
				}
			}
		}
		soft.assertAll();
	}

	/**
	 * Liefert alle Nicht-Score-Identitäts-Schlüssel ({@code __PTM_*}, ohne {@code __PTM_SCORE_*})
	 * gruppiert nach dem Blattnamen, auf den ihr Named Range aufgelöst wird. Grundlage für die
	 * Eindeutigkeits- und Korrektheitsprüfungen der PTM-Metadaten.
	 */
	protected Map<String, List<String>> identitaetsSchluesselProBlatt() {
		XSpreadsheetDocument xDoc = wkingSpreadsheet.getWorkingSpreadsheetDocument();
		Map<String, List<String>> proBlatt = new LinkedHashMap<>();
		for (String schluessel : SheetMetadataHelper.getSchluesselMitPrefix(xDoc, "__PTM_")) {
			if (schluessel.startsWith("__PTM_SCORE_")) {
				continue;
			}
			SheetMetadataHelper.findeSheet(xDoc, schluessel).ifPresent(sheet ->
					proBlatt.computeIfAbsent(Lo.qi(XNamed.class, sheet).getName(), k -> new ArrayList<>())
							.add(schluessel));
		}
		return proBlatt;
	}

	/** Liefert die Identitäts-Schlüssel, die auf das gegebene Blatt zeigen. */
	protected List<String> schluesselFuer(XSpreadsheet sheet) {
		String name = Lo.qi(XNamed.class, sheet).getName();
		return identitaetsSchluesselProBlatt().getOrDefault(name, List.of());
	}

	/**
	 * Prüft, dass nach vollständiger Turnier-Generierung jedes Blatt exakt seinen erwarteten
	 * PTM-Identitäts-Schlüssel trägt. Geprüft wird in beide Richtungen:
	 * <ol>
	 *   <li><b>Korrektheit:</b> jedes in {@code erwartung} genannte Blatt existiert und trägt
	 *       genau den dort hinterlegten Schlüssel (kein fehlender, kein fremder, kein zweiter).</li>
	 *   <li><b>Vollständigkeit:</b> kein darüber hinausgehendes Blatt trägt einen
	 *       Identitäts-Schlüssel.</li>
	 * </ol>
	 * Alle Abweichungen werden über {@link SoftAssertions} gesammelt, damit ein einzelner
	 * Testlauf das komplette Soll/Ist-Delta meldet.
	 *
	 * @param erwartung Soll-Zuordnung Blattname → erwarteter Identitäts-Schlüssel
	 */
	protected void pruefeJedesBlattTraegtKorrektenSchluessel(Map<String, String> erwartung) {
		Map<String, List<String>> proBlatt = identitaetsSchluesselProBlatt();
		SoftAssertions soft = new SoftAssertions();
		erwartung.forEach((blattName, erwarteterSchluessel) ->
				soft.assertThat(proBlatt.get(blattName))
						.as("Blatt '%s' muss genau den Identitäts-Schlüssel '%s' tragen",
								blattName, erwarteterSchluessel)
						.containsExactly(erwarteterSchluessel));
		soft.assertThat(proBlatt.keySet())
				.as("Nur die erwarteten Blätter dürfen einen Identitäts-Schlüssel tragen")
				.containsExactlyInAnyOrderElementsOf(erwartung.keySet());
		soft.assertAll();
	}

}
