/**
 * 
 * Erstellung : 24.03.2018 / Michael Massee
 **/

package de.petanqueturniermanager;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XCalculatable;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.comp.GlobalProperties;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.comp.newrelease.NewReleaseChecker;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.io.BackUp;
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

	protected String getLogPrefix() {
		return logPrefix;
	}

	private boolean backupDocumentAfterRun;

	protected SheetRunner(WorkingSpreadsheet workingSpreadsheet, TurnierSystem spielSystem, String logPrefix) {
		this.workingSpreadsheet = checkNotNull(workingSpreadsheet, "WorkingSpreadsheet==null");
		turnierSystem = checkNotNull(spielSystem, "SpielSystem==null");
		sheetHelper = new SheetHelper(workingSpreadsheet);
		this.logPrefix = logPrefix;
		this.backupDocumentAfterRun = false;
	}

	protected SheetRunner(WorkingSpreadsheet workingSpreadsheet, TurnierSystem spielSystem) {
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

	public static final void cancelRunner() {
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
				processBox().run();
				if (turnierSystem != TurnierSystem.KEIN) {
					updateKonfigurationSheet();
				}
				doRun();
			} catch (GenerateException e) {
				handleGenerateException(e);
			} catch (Exception e) {
				isFehler = true;
				processBox().fehler("Interner Fehler " + e.getClass().getName()).fehler(e.getMessage())
						.fehler("Siehe log für weitere Infos");
				getLogger().error(e.getMessage(), e);
			} finally {
				SheetRunner.isRunning.set(false); // Immer an erste stelle diesen flag zurück
				SheetRunner.runner = null;
				if (isFehler) {
					processBox().visible().fehler("!! FEHLER !!").ready();
				} else {
					processBox().visible().info("**FERTIG**").ready();
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
	 * prüft ob ein Turnier vorhanden. Wenn nicht dann Fehlermeldung und Exception. Abbruch.
	 * 
	 * @return
	 * @throws GenerateException
	 */
	public SheetRunner testTurnierVorhanden() throws GenerateException {
		TurnierSystem turnierSystemAusDocument = new DocumentPropertiesHelper(workingSpreadsheet)
				.getTurnierSystemAusDocument();

		if (turnierSystemAusDocument == TurnierSystem.KEIN) {
			MessageBox.from(workingSpreadsheet.getxContext(), MessageBoxTypeEnum.ERROR_OK)
					.caption("Kein Turnier-Dokument").message("Kein Turnier vorhanden").show();
			throw new GenerateException(VERARBEITUNG_ABGEBROCHEN);
		}
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
			BackUp.from(workingSpreadsheet).prefix1(backupPrefix).prefix2(logPrefix).doBackUp();
		}
	}

	private void autoSave() {
		if (GlobalProperties.get().isAutoSave()) {
			BackUp.from(workingSpreadsheet).doSave();
		}
	}

	protected void handleGenerateException(GenerateException e) {
		if (VERARBEITUNG_ABGEBROCHEN.equals(e.getMessage())) {
			processBox().info(VERARBEITUNG_ABGEBROCHEN);
			MessageBox.from(getxContext(), MessageBoxTypeEnum.WARN_OK).caption("Abbruch").message(e.getMessage())
					.show();
		} else {
			processBox().fehler(e.getMessage());
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
		processBox().info(infoMsg);
	}

	public ProcessBox processBox() {
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
