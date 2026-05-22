/*
 * 
 * Erstellung : 24.03.2018 / Michael Massee
 **/

package de.petanqueturniermanager;

import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.XTopWindow;
import com.sun.star.frame.XController;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XModel;
import com.sun.star.lang.DisposedException;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XEventListener;
import com.sun.star.sheet.XCalculatable;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.comp.GlobalProperties;
import de.petanqueturniermanager.comp.PetanqueTurnierMngrSingleton;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.webserver.WebServerManager;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.perflog.PerfLog;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.helper.sheet.ControllerLock;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzManager;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzRegistry;
import de.petanqueturniermanager.helper.sheet.io.BackUp;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.toolbar.TurnierModus;

public abstract class SheetRunner extends Thread {

	private static final Logger logger = LogManager.getLogger(SheetRunner.class);

	static final String VERARBEITUNG_ABGEBROCHEN = "Verarbeitung abgebrochen";
	private final WorkingSpreadsheet workingSpreadsheet;
	private final SheetHelper sheetHelper;
	private final TurnierSystem turnierSystem;
	// package-private: damit SheetRunnerTest den Koordinator austauschen kann.
	// volatile garantiert Sichtbarkeit zwischen Threads.
	// Kein vollständiger Thread-Safety-Mechanismus!
	// Ausreichend, da Setzen nur in Tests vor Start erfolgt.
	static volatile SheetRunnerKoordinator koordinator = new SheetRunnerKoordinator();

	/**
	 * Gesetzt von {@link #start()}, um anzuzeigen dass der Koordinator bereits
	 * vor dem Thread-Start vorgemerkt wurde. Verhindert die Race-Condition zwischen
	 * {@code Thread.start()} und dem eigentlichen {@code run()}-Aufruf, in der
	 * {@code isRunning()} fälschlicherweise {@code false} zurückgibt.
	 */
	private volatile boolean koordinatorVorgekoppelt = false;

	/**
	 * Gesetzt von {@link #startSilent()}: Listener-/Hintergrund-Lauf ohne ProcessBox-Popup
	 * und ohne Busy-MessageBox bei Kollision. Verhalten in {@link #run()} entspricht
	 * dem Listener-Pfad, aber mit asynchroner Ausführung (geschlossene Race über
	 * vorab gesetztes {@code Laeuft}-Flag).
	 */
	private volatile boolean silentBackground = false;

	private String logPrefix = null;

	protected String getLogPrefix() {
		return logPrefix;
	}

	private volatile boolean backupDocumentAfterRun;

	/**
	 * Wird {@code true}, sobald das Dokument während der Laufzeit des Runners disposed wird
	 * (entweder via UNO-Disposing-Listener oder durch eine gefangene {@link DisposedException}).
	 * Verhindert weitere UNO-Aufrufe auf das tote Proxy-Objekt.
	 */
	private volatile boolean documentDisposed = false;

	private final XEventListener disposingListener = new XEventListener() {
		@Override
		public void disposing(EventObject event) {
			documentDisposed = true;
			logger.debug("Dokument disposed während SheetRunner läuft");
		}
	};

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
	 * dazu führen konnte, dass der {@link de.petanqueturniermanager.helper.sheetsync.SheetSyncListener}
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

	/**
	 * Startet den Runner asynchron im Hintergrund ohne sichtbare ProcessBox-Animation
	 * und ohne Busy-MessageBox bei Kollision. Vorgesehen für Listener-getriggerte
	 * Refreshes (z.B. Tab-Wechsel zur Rangliste): Der aufrufende UI-Event-Handler
	 * kehrt sofort zurück, sodass LO den Tab-Wechsel ungestört abschließen kann.
	 * <p>
	 * Wie {@link #start()} setzt diese Methode den {@code Laeuft}-Flag synchron vor dem
	 * Thread-Start und schließt damit das Race-Fenster für Doppel-Trigger. Wenn der
	 * Koordinator bereits läuft, wird stillschweigend nichts getan (kein Popup).
	 */
	public final synchronized void startSilent() {
		if (!koordinator.getAndSetLaeuft(true)) {
			koordinatorVorgekoppelt = true;
			silentBackground = true;
			super.start();
		}
	}

	@Override
	public final void run() {
		boolean laueftJetzt = koordinatorVorgekoppelt || !koordinator.getAndSetLaeuft(true);
		if (laueftJetzt) {
			logger.debug("Start SheetRunner");
			long runStartNs = System.nanoTime();
			PerfLog.log(logger, "[WORKER-TIMING] SheetRunner.run START class={} thread={}",
					this.getClass().getSimpleName(), Thread.currentThread().getName());
			registerDisposingListener();
			koordinator.setRunner(this);
			koordinator.benachrichtigeListener(); // Menü deaktivieren
			boolean isFehler = false;

			// Renderpfad des Calc-Dokuments für die Dauer des Laufs sperren: LO
			// unterdrückt das Repaint zwischen den (potentiell hunderten) UNO-
			// Property-Writes. Auf Windows mit D3D-Backend ist das die Schutzklammer
			// gegen native Renderer-Crashes (scfiltlo / D3DScreenUpdateManager,
			// nachgewiesen via Minidump beim Anwender). Der Lock umschließt
			// bewusst auch das finally – endCommandScope() macht den Großteil
			// der Protect/Unprotect- und CellStyle-Last und muss ebenfalls unter
			// dem Lock laufen.
			try (ControllerLock _ = ControllerLock.lock(workingSpreadsheet.getWorkingSpreadsheetDocument())) {
				try {
					if (koordinatorVorgekoppelt && !silentBackground) {
						processBox().run(); // Nur Menü-Aktionen: ProcessBox animieren und sichtbar halten
					}
					if (turnierSystem != TurnierSystem.KEIN && isUpdateKonfigurationSheetBeforeDoRun()
							&& isDocumentAlive()) {
						updateKonfigurationSheet();
					}
					// Lazy-Unprotect-Scope öffnen: ein einziges entsperren/schuetzen pro Kommando
					// statt mehrfaches Toggle in jeder Sub-Operation. Echte Entsperrung passiert
					// erst beim ersten Style-/CF-Trigger (ConditionalFormatHelper / RangeHelper.clearRange).
					if (turnierSystem != TurnierSystem.KEIN && TurnierModus.get().istAktiv()) {
						BlattschutzRegistry.fuer(turnierSystem)
								.ifPresent(k -> BlattschutzManager.get().beginCommandScope(k, workingSpreadsheet));
					}
					if (isDocumentAlive()) {
						doRun();
						WebServerManager.get().sseRefreshSenden(workingSpreadsheet);
						// Während des Runners eingetroffene Modify-Events wurden vom Listener
						// zwar als dirty markiert, aber nicht eingeplant. Hier nachholen,
						// damit kein Benutzer-Event verloren geht.
						WebServerManager.get().getModifyListener().markDirtyAndSchedule();
					}
				} catch (DisposedException e) {
					documentDisposed = true;
					logger.debug("Dokument disposed während SheetRunner – sauberer Abbruch", e);
				} catch (GenerateException e) {
					handleGenerateException(e);
				} catch (Exception e) {
					isFehler = true;
					processBox().fehler(I18n.get("processbox.interner.fehler", e.getClass().getName())).fehler(e.getMessage())
							.fehler(I18n.get("processbox.log.hinweis"));
					getLogger().error(e.getMessage(), e);
				} finally {
					koordinator.setLaeuft(false); // Immer an erste stelle diesen flag zurück
					// Lazy-Unprotect-Scope schließen: wenn unterwegs entsperrt wurde, jetzt einmal schützen.
					// Idempotent (No-Op falls kein Scope offen war), eigene try/finally-Robustheit im Manager.
					BlattschutzManager.get().endCommandScope();
					koordinator.setRunner(null);
					koordinator.benachrichtigeListener(); // Menü reaktivieren
					if (documentDisposed) {
						logger.debug("SheetRunner-Cleanup: Dokument bereits disposed, keine UI-Updates");
					} else if ((koordinatorVorgekoppelt && !silentBackground) || isFehler) {
						// Menü-Aktion oder Fehler: ProcessBox-Fenster sichtbar zeigen
						if (isFehler) {
							processBox().visible().fehler(I18n.get("processbox.fehler.status")).ready();
						} else {
							processBox().visibleWennAutomatisch().info(I18n.get("processbox.fertig.status")).ready();
						}
					} else {
						// Listener-ausgelöst oder silent-Background: nur in ProcessBox loggen, Fenster NICHT aufpoppen
						processBox().info(I18n.get("processbox.fertig.status"));
					}
					if (isDocumentAlive()) {
						try {
							getxCalculatable().enableAutomaticCalculation(true); // falls abgeschaltet wurde
						} catch (DisposedException e) {
							documentDisposed = true;
							logger.debug("Dokument disposed bei enableAutomaticCalculation", e);
						}
					}
				}
			}
			// Während des Laufs koaleszierte TurnierEvents jetzt einmal feuern.
			// isRunning() ist hier bereits false (im finally gesetzt), der Dispatch
			// erfolgt über AsyncCallback auf den LO-Main-Thread und kollidiert daher
			// nicht mit den Aufräumarbeiten dieses Worker-Threads.
			if (isDocumentAlive()) {
				try {
					PetanqueTurnierMngrSingleton.flushPendingTurnierEvent();
				} catch (RuntimeException e) {
					getLogger().warn("Fehler beim flushPendingTurnierEvent: " + e.getMessage(), e);
				}
			}
			if (isDocumentAlive()) {
				try {
					autoSave();
					if (!isFehler && backupDocumentAfterRun) {
						backUpDocument("2"); // after run
					}
				} catch (DisposedException e) {
					documentDisposed = true;
					logger.debug("Dokument disposed während Post-Run-Operationen", e);
				} catch (Exception e) {
					getLogger().warn("Fehler bei Post-Run-Operationen: " + e.getMessage(), e);
				}
			}
			unregisterDisposingListener();

			// Fokus deterministisch auf das Arbeits-Dokument zurückgeben.
			// ProcessBox (Singleton, parentless Top-Window) klaut beim run() den
			// Fokus und gibt ihn nach setVisible(false) an das älteste sichtbare
			// Calc-Fenster zurück (= doc1 statt des tatsächlich bearbeiteten doc).
			// Folge ohne diesen Aufruf: alle Menü-StatusListener werten gegen den
			// "falschen" aktiven Frame, die System-Toolbar-States in doc2 bleiben
			// alt, User sieht doc1 im Vordergrund.
			if (isDocumentAlive()) {
				fokussiereArbeitsDokument();
			}

			long gesamtMs = (System.nanoTime() - runStartNs) / 1_000_000L;
			PerfLog.log(logger, "[WORKER-TIMING] SheetRunner.run ENDE class={} dauer={} ms fehler={}",
					this.getClass().getSimpleName(), gesamtMs, isFehler);

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
	 * Aktiviert den Frame des {@link #workingSpreadsheet} via {@link XFrame#activate()}
	 * und {@link XTopWindow#toFront()}. Wird am Ende eines jeden Runs aufgerufen, damit
	 * der Fokus nach Sheet-Operationen am bearbeiteten Doc bleibt — siehe Bug-Repro
	 * unter {@code tools/linux/fokus_neue_meldeliste.py}.
	 */
	private void fokussiereArbeitsDokument() {
		try {
			XModel xModel = Lo.qi(XModel.class, workingSpreadsheet.getWorkingSpreadsheetDocument());
			if (xModel == null) return;
			XController controller = xModel.getCurrentController();
			if (controller == null) return;
			XFrame frame = controller.getFrame();
			if (frame == null) return;
			logger.info("[FOKUS-TRACE] SheetRunner.fokussiereArbeitsDokument: activate+toFront frame#{} class={}",
					System.identityHashCode(frame), this.getClass().getSimpleName());
			frame.activate();
			XTopWindow top = Lo.qi(XTopWindow.class, frame.getContainerWindow());
			if (top != null) {
				top.toFront();
			}
		} catch (DisposedException e) {
			logger.debug("fokussiereArbeitsDokument: Dokument disposed");
		} catch (Exception e) {
			logger.debug("fokussiereArbeitsDokument fehlgeschlagen", e);
		}
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

	/**
	 * Prüft, ob das zugrundeliegende UNO-Dokument noch lebt. Kombiniert den intern via
	 * Disposing-Listener gesetzten Flag mit einem leichten UNO-Probe-Aufruf, damit auch
	 * Dispose-Ereignisse erkannt werden, die zwischen Listener-Registrierung und Lauf
	 * passieren oder bei denen der Listener nicht zustande kam.
	 */
	private boolean isDocumentAlive() {
		if (documentDisposed) {
			return false;
		}
		try {
			var doc = workingSpreadsheet.getWorkingSpreadsheetDocument();
			if (doc == null) {
				return false;
			}
			doc.getSheets();
			return true;
		} catch (DisposedException e) {
			documentDisposed = true;
			return false;
		}
	}

	private void registerDisposingListener() {
		try {
			XComponent xComp = Lo.qi(XComponent.class, workingSpreadsheet.getWorkingSpreadsheetDocument());
			if (xComp != null) {
				xComp.addEventListener(disposingListener);
			}
		} catch (DisposedException e) {
			documentDisposed = true;
		} catch (Exception e) {
			logger.debug("Konnte Disposing-Listener nicht registrieren", e);
		}
	}

	private void unregisterDisposingListener() {
		try {
			XComponent xComp = Lo.qi(XComponent.class, workingSpreadsheet.getWorkingSpreadsheetDocument());
			if (xComp != null) {
				xComp.removeEventListener(disposingListener);
			}
		} catch (DisposedException e) {
			// Dokument bereits weg – nichts zu tun
		} catch (Exception e) {
			logger.debug("Konnte Disposing-Listener nicht entfernen", e);
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
	 * vom {@link de.petanqueturniermanager.helper.sheetsync.SheetSyncListener}
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
	 * Wird vom {@link de.petanqueturniermanager.helper.sheetsync.SheetSyncListener} genutzt.
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
