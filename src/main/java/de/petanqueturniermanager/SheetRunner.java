/**
 * 
 * Erstellung : 24.03.2018 / Michael Massee
 **/

package de.petanqueturniermanager;

import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XCalculatable;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.comp.GlobalProperties;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.webserver.WebServerManager;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.io.BackUp;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

public abstract class SheetRunner extends Thread {

	private static final Logger logger = LogManager.getLogger(SheetRunner.class);

	static final String VERARBEITUNG_ABGEBROCHEN = "Verarbeitung abgebrochen";
	private final WorkingSpreadsheet workingSpreadsheet;
	private final SheetHelper sheetHelper;
	private final TurnierSystem turnierSystem;
	// package-private: damit SheetRunnerTest den Koordinator austauschen kann
	static SheetRunnerKoordinator koordinator = new SheetRunnerKoordinator();

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
		koordinator.abbrechenPruefen();
	}

	public static final void cancelRunner() {
		koordinator.abbrechen();
	}

	public static void addStateChangeListener(Runnable listener) {
		koordinator.addZustandsListener(listener);
	}

	@Override
	public final void run() {
		if (!koordinator.getAndSetLaeuft(true)) {
			logger.debug("Start SheetRunner");
			koordinator.setRunner(this);
			koordinator.benachrichtigeListener(); // Menü deaktivieren
			boolean isFehler = false;

			try {
				processBox().run();
				if (turnierSystem != TurnierSystem.KEIN && isUpdateKonfigurationSheetBeforeDoRun()) {
					updateKonfigurationSheet();
				}
				doRun();
				WebServerManager.get().sseRefreshSenden(workingSpreadsheet);
			} catch (GenerateException e) {
				handleGenerateException(e);
			} catch (Exception e) {
				isFehler = true;
				processBox().fehler(I18n.get("processbox.interner.fehler", e.getClass().getName())).fehler(e.getMessage())
						.fehler(I18n.get("processbox.log.hinweis"));
				getLogger().error(e.getMessage(), e);
			} finally {
				koordinator.setLaeuft(false); // Immer an erste stelle diesen flag zurück
				koordinator.setRunner(null);
				koordinator.benachrichtigeListener(); // Menü reaktivieren
				if (isFehler) {
					processBox().visible().fehler(I18n.get("processbox.fehler.status")).ready();
				} else {
					processBox().visible().info(I18n.get("processbox.fertig.status")).ready();
				}
				getxCalculatable().enableAutomaticCalculation(true); // falls abgeschaltet wurde
			}
			try {
				autoSave();
				if (!isFehler && backupDocumentAfterRun) {
					backUpDocument("2"); // after run
				}
			} catch (Exception e) {
				getLogger().warn("Fehler bei Post-Run-Operationen: " + e.getMessage(), e);
			}

		} else {
			MessageBox.from(getxContext(), MessageBoxTypeEnum.WARN_OK)
					.caption(I18n.get("msg.caption.aktive.verarbeitung"))
					.message(I18n.get("msg.text.verarbeitung.laeuft")).show();
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
					.caption(I18n.get("msg.caption.kein.turnier.dok"))
					.message(I18n.get("msg.text.kein.turnier")).show();
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
			processBox().info(I18n.get("msg.text.verarbeitung.abgebrochen"));
			MessageBox.from(getxContext(), MessageBoxTypeEnum.WARN_OK)
					.caption(I18n.get("msg.caption.abbruch"))
					.message(I18n.get("msg.text.verarbeitung.abgebrochen")).show();
		} else {
			processBox().fehler(e.getMessage());
			getLogger().error(e.getMessage(), e);
			MessageBox.from(getxContext(), MessageBoxTypeEnum.ERROR_OK)
					.caption(I18n.get("msg.caption.fehler"))
					.message(e.getMessage()).show();
		}
	}

	protected abstract IKonfigurationSheet getKonfigurationSheet();

	/**
	 * Steuert, ob {@link #updateKonfigurationSheet()} VOR {@code doRun()} aufgerufen wird.
	 * <p>
	 * Standardmäßig {@code true} – bestehende Turniersysteme ändern ihr Verhalten nicht.
	 * Subklassen, die in {@code doRun()} einen Bestätigungsdialog zeigen, können {@code false}
	 * zurückgeben und {@link #getKonfigurationSheet()}{@code .update()} selbst nach der
	 * Bestätigung aufrufen.
	 */
	protected boolean isUpdateKonfigurationSheetBeforeDoRun() {
		return true;
	}

	private void updateKonfigurationSheet() throws GenerateException {
		IKonfigurationSheet konfigurationSheet = getKonfigurationSheet();
		if (konfigurationSheet != null) {
			konfigurationSheet.update();
		}
	}

	public Logger getLogger() {
		return LogManager.getLogger(this.getClass());
	}

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
		return koordinator.isRunning();
	}

	// for mocking
	public void processBoxinfo(String i18nKey, Object... args) {
		processBox().info(I18n.get(i18nKey, args));
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
