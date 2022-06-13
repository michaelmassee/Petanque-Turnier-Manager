/**

* Erstellung : 24.03.2018 / Michael Massee
**/

package de.petanqueturniermanager;

import static com.google.common.base.Preconditions.checkNotNull;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.XStorable;
import com.sun.star.io.IOException;
import com.sun.star.sheet.XCalculatable;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.comp.GlobalProperties;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.comp.newrelease.NewReleaseChecker;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

public abstract class SheetRunner extends Thread implements Runnable {

	private static final Logger logger = LogManager.getLogger(SheetRunner.class);

	private static final String VERARBEITUNG_ABGEBROCHEN = "Verarbeitung abgebrochen";
	private final WorkingSpreadsheet workingSpreadsheet;
	private final SheetHelper sheetHelper;
	private final TurnierSystem turnierSystem;
	private static AtomicBoolean isRunning = new AtomicBoolean(); // nur 1 Sheetrunner gleichzeitig
	private static volatile SheetRunner runner = null;
	private String logPrefix = null;
	private boolean backupDocumentAfterRun;

	public SheetRunner(WorkingSpreadsheet workingSpreadsheet, TurnierSystem spielSystem, String logPrefix) {
		this.workingSpreadsheet = checkNotNull(workingSpreadsheet, "WorkingSpreadsheet==null");
		turnierSystem = checkNotNull(spielSystem, "SpielSystem==null");
		sheetHelper = new SheetHelper(workingSpreadsheet);
		this.logPrefix = logPrefix;
		this.backupDocumentAfterRun = false;
	}

	public SheetRunner(WorkingSpreadsheet workingSpreadsheet, TurnierSystem spielSystem) {
		this(workingSpreadsheet, spielSystem, null);
	}

	/**
	 * wenn thread is interrupted dann ein Abbruch Exception werfen
	 *
	 * @throws GenerateException
	 */

	public static final void testDoCancelTask() throws GenerateException {
		if (runner != null && runner.isInterrupted()) {
			throw new GenerateException(VERARBEITUNG_ABGEBROCHEN);
		}
	}

	public final static void cancelRunner() {
		if (runner != null) {
			runner.interrupt();
		}
	}

	@Override
	public final void run() {
		if (!SheetRunner.isRunning.getAndSet(true)) {
			logger.debug("Start SheetRunner");
			SheetRunner.runner = this;
			boolean isFehler = false;

			try {
				ProcessBox().run();
				if (turnierSystem != TurnierSystem.KEIN) {
					updateKonfigurationSheet();
				}
				doRun();
			} catch (GenerateException e) {
				handleGenerateException(e);
			} catch (Exception e) {
				isFehler = true;
				ProcessBox().fehler("Interner Fehler " + e.getClass().getName()).fehler(e.getMessage())
						.fehler("Siehe log für weitere Infos");
				getLogger().error(e.getMessage(), e);
			} finally {
				SheetRunner.isRunning.set(false); // Immer an erste stelle diesen flag zurück
				SheetRunner.runner = null;
				if (isFehler) {
					ProcessBox().visible().fehler("!! FEHLER !!").ready();
				} else {
					ProcessBox().visible().info("**FERTIG**").ready();
				}
				getxCalculatable().enableAutomaticCalculation(true); // falls abgeschaltet wurde
			}
			new NewReleaseChecker().udateNewReleaseInfo(getxContext());
			autoSave();
			if (!isFehler && backupDocumentAfterRun) {
				backUpDocument("2"); // after run
			}

		} else {
			MessageBox.from(getxContext(), MessageBoxTypeEnum.WARN_OK).caption("Abbruch")
					.message("Die Verarbeitung wurde nicht gestartet, weil bereits eine Aktive vorhanden.").show();
		}
	}

	public SheetRunner backupDocumentAfterRun() {
		backupDocumentAfterRun = true;
		return this;
	}

	/**
	 * https://api.libreoffice.org/docs/idl/ref/interfacecom_1_1sun_1_1star_1_1frame_1_1XStorable.html#af5d1fdcbfe78592afb590a4c244acf20
	 * https://wikinew.openoffice.org/wiki/Documentation/DevGuide/Spreadsheets/Saving_Spreadsheet_Documents#Storing
	 */

	public SheetRunner backUpDocument() {
		backUpDocument("1"); // before run
		return this;
	}

	private void backUpDocument(String backupPrefix) {
		if (GlobalProperties.get().isCreateBackup()) {
			XStorable xStorable = workingSpreadsheet.getXStorable();
			String location = xStorable.getLocation();

			try {
				if (!StringUtils.isAllBlank(location)) {
					// doc wurde bereits gespeichert
					URL docUrl = new URL(location);
					Path path = Path.of(docUrl.toURI());
					Path fileName = path.getFileName();
					Path dir = path.getParent();

					// generate file name
					String dateStmp = DateFormatUtils.format(new Date(), "ddMMyyyy_HHmmss");
					String orgFileName = fileName.toString();
					String newFileName = dateStmp + (StringUtils.isEmpty(backupPrefix) ? "" : "_" + backupPrefix)
							+ (StringUtils.isEmpty(logPrefix) ? "" : "_" + logPrefix) + "_" + orgFileName;
					Path newLocation = dir.resolve(newFileName);
					logger.info("Erstelle Backup :" + newLocation.toUri());
					PropertyValue[] newProperties = new PropertyValue[0];
					xStorable.storeToURL(newLocation.toUri().toString(), newProperties);
				}
			} catch (MalformedURLException | URISyntaxException | IOException e) {
				logger.error(e.getMessage(), e);
			}
		}

	}

	private void autoSave() {
		if (GlobalProperties.get().isAutoSave()) {
			XStorable xStorable = workingSpreadsheet.getXStorable();
			String location = xStorable.getLocation();
			if (!StringUtils.isAllBlank(location)) {
				logger.info("Autosave :" + location);
				try {
					xStorable.store();
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
				}
			}
		}
	}

	protected void handleGenerateException(GenerateException e) {
		if (VERARBEITUNG_ABGEBROCHEN.equals(e.getMessage())) {
			ProcessBox().info("Verarbeitung abgebrochen");
			MessageBox.from(getxContext(), MessageBoxTypeEnum.WARN_OK).caption("Abbruch").message(e.getMessage())
					.show();
		} else {
			ProcessBox().fehler(e.getMessage());
			getLogger().error(e.getMessage(), e);
			MessageBox.from(getxContext(), MessageBoxTypeEnum.ERROR_OK).caption("Fehler").message(e.getMessage())
					.show();
		}
	}

	protected abstract IKonfigurationSheet getKonfigurationSheet();

	private void updateKonfigurationSheet() throws GenerateException {
		IKonfigurationSheet konfigurationSheet = getKonfigurationSheet();
		checkNotNull(konfigurationSheet, "IKonfigurationSheet == null");
		konfigurationSheet.update();
	}

	public abstract Logger getLogger();

	protected abstract void doRun() throws GenerateException;

	public SheetHelper getSheetHelper() throws GenerateException {
		SheetRunner.testDoCancelTask();
		return sheetHelper;
	}

	public XComponentContext getxContext() {
		return getWorkingSpreadsheet().getxContext();
	}

	public XCalculatable getxCalculatable() {
		return getWorkingSpreadsheet().getxCalculatable();
	}

	public static boolean isRunning() {
		return isRunning.get();
	}

	// for mocking
	public void processBoxinfo(String infoMsg) {
		ProcessBox().info(infoMsg);
	}

	public ProcessBox ProcessBox() {
		return ProcessBox.from().prefix(logPrefix);
	}

	/**
	 * @return the workingSpreadsheet
	 */
	public WorkingSpreadsheet getWorkingSpreadsheet() {
		return workingSpreadsheet;
	}

	/**
	 * @return the TurnierSystem
	 */
	public final TurnierSystem getTurnierSystem() {
		return turnierSystem;
	}
}
