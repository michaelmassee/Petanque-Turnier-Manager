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

	/**
	 * Gesetzt von {@link #start()}, um anzuzeigen dass der Koordinator bereits
	 * vor dem Thread-Start vorgemerkt wurde. Verhindert die Race-Condition zwischen
	 * {@code Thread.start()} und dem eigentlichen {@code run()}-Aufruf, in der
	 * {@code isRunning()} fälschlicherweise {@code false} zurückgibt.
	 */
	private volatile boolean koordinatorVorgekoppelt = false;

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

	public static void removeStateChangeListener(Runnable listener) {
		koordinator.removeZustandsListener(listener);
	}

	/**
	 * Startet den SheetRunner asynchron als Thread und schließt dabei die Race-Condition,
	 * die zwischen {@code Thread.start()} und dem eigentlichen {@code run()}-Einstieg entsteht:
	 * In diesem Zeitfenster sah {@code isRunning()} fälschlicherweise {@code false}, was
	 * dazu führen konnte, dass der {@link de.petanqueturniermanager.helper.rangliste.RanglisteRefreshListener}
	 * parallel einen zweiten Runner startete und damit das Prozess-Fenster zweimal öffnete.
	 * <p>
	 * Durch {@link #koordinatorVorgekoppelt} erkennt {@link #run()}, dass der Koordinator
	 * bereits gesetzt wurde, und überspringt die erneute Prüfung.
	 */
	@Override
	public final synchronized void start() {
		if (!koordinator.getAndSetLaeuft(true)) {
			koordinatorVorgekoppelt = true;
			super.start();
		} else {
			MessageBox.from(getxContext(), MessageBoxTypeEnum.WARN_OK)
					.caption(I18n.get("msg.caption.aktive.verarbeitung"))
					.message(I18n.get("msg.text.verarbeitung.laeuft")).show();
		}
	}

	@Override
	public final void run() {
		boolean laueftJetzt = koordinatorVorgekoppelt || !koordinator.getAndSetLaeuft(true);
		if (laueftJetzt) {
			logger.debug("Start SheetRunner");
			koordinator.setRunner(this);
			koordinator.benachrichtigeListener(); // Menü deaktivieren
			boolean isFehler = false;

			try {
				if (koordinatorVorgekoppelt) {
					processBox().run(); // Nur Menü-Aktionen: ProcessBox animieren und sichtbar halten
				}
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
				if (koordinatorVorgekoppelt || isFehler) {
					// Menü-Aktion oder Fehler: ProcessBox-Fenster sichtbar zeigen
					if (isFehler) {
						processBox().visible().fehler(I18n.get("processbox.fehler.status")).ready();
					} else {
						processBox().visible().info(I18n.get("processbox.fertig.status")).ready();
					}
				} else {
					// Listener-ausgelöst, kein Fehler: nur in ProcessBox loggen, Fenster NICHT aufpoppen
					processBox().info(I18n.get("processbox.fertig.status"));
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

	/**
	 * Signalisiert, dass das nächste {@code selectionChanged}-Ereignis zur Rangliste
	 * vom {@link de.petanqueturniermanager.helper.rangliste.RanglisteRefreshListener}
	 * ignoriert werden soll.
	 * <p>
	 * Muss direkt nach {@code getSheetHelper().setActiveSheet(sheet)} aufgerufen werden,
	 * wenn das Sheet die Rangliste ist. LibreOffice feuert {@code selectionChanged}
	 * asynchron – das Ereignis kann nach {@code setLaeuft(false)} ankommen und würde
	 * sonst einen unerwünschten zweiten Neuaufbau auslösen.
	 */
	public static void unterdrückeNaechstesSelectionChange() {
		koordinator.unterdrückeNaechstesSelectionChange();
	}

	/**
	 * Liest und löscht das Unterdrückungs-Flag atomar.
	 * Wird vom {@link de.petanqueturniermanager.helper.rangliste.RanglisteRefreshListener} genutzt.
	 *
	 * @return {@code true} wenn das nächste selectionChanged ignoriert werden soll
	 */
	public static boolean consumeSelectionChangeSuppression() {
		return koordinator.consumeSelectionChangeSuppression();
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
