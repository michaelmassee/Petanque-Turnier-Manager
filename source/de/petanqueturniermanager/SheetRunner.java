/**

* Erstellung : 24.03.2018 / Michael Massee
**/

package de.petanqueturniermanager;

import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XCalculatable;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.konfiguration.KonfigurationSheet;

public abstract class SheetRunner extends Thread implements Runnable {

	private static final String VERARBEITUNG_ABGEBROCHEN = "Verarbeitung abgebrochen";
	private final WorkingSpreadsheet workingSpreadsheet;
	private final SheetHelper sheetHelper;
	private static volatile boolean isRunning = false; // nur 1 Sheetrunner gleichzeitig
	private static SheetRunner runner = null;

	private String logPrefix = null;

	public SheetRunner(WorkingSpreadsheet workingSpreadsheet, String logPrefix) {
		this(workingSpreadsheet);
		this.logPrefix = logPrefix;
	}

	public SheetRunner(WorkingSpreadsheet workingSpreadsheet) {
		this.workingSpreadsheet = checkNotNull(workingSpreadsheet, "xContext==null");
		sheetHelper = new SheetHelper(workingSpreadsheet);
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
		if (!SheetRunner.isRunning) {
			SheetRunner.isRunning = true;
			SheetRunner.runner = this;
			boolean isFehler = false;

			try {
				ProcessBox.from().run();
				updateKonfigurationSheet();
				doRun();
			} catch (GenerateException e) {
				handleGenerateException(e);
			} catch (Exception e) {
				isFehler = true;
				ProcessBox.from().fehler("Interner Fehler " + e.getClass().getName()).fehler(e.getMessage()).fehler("Siehe log für weitere Infos");
				getLogger().error(e.getMessage(), e);
			} finally {
				SheetRunner.isRunning = false; // Immer an erste stelle diesen flag zurück
				SheetRunner.runner = null;
				if (isFehler) {
					ProcessBox.from().visible().fehler("!! FEHLER !!").ready();
				} else {
					ProcessBox.from().visible().info("**FERTIG**").ready();
				}
				getxCalculatable().enableAutomaticCalculation(true); // falls abgeschaltet wurde
			}
		} else {
			MessageBox.from(getxContext(), MessageBoxTypeEnum.WARN_OK).caption("Abbruch").message("Die Verarbeitung wurde nicht gestartet, weil bereits eine Aktive vorhanden.")
					.show();
		}
	}

	protected void handleGenerateException(GenerateException e) {
		if (VERARBEITUNG_ABGEBROCHEN.equals(e.getMessage())) {
			ProcessBox.from().info("Verarbeitung abgebrochen");
			MessageBox.from(getxContext(), MessageBoxTypeEnum.WARN_OK).caption("Abbruch").message(e.getMessage()).show();
		} else {
			ProcessBox.from().fehler(e.getMessage());
			getLogger().error(e.getMessage(), e);
			MessageBox.from(getxContext(), MessageBoxTypeEnum.ERROR_OK).caption("Fehler").message(e.getMessage()).show();
		}
	}

	private void updateKonfigurationSheet() throws GenerateException {
		new KonfigurationSheet(getWorkingSpreadsheet()).update();
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
		XSpreadsheetDocument doc = getWorkingSpreadsheet().getWorkingSpreadsheetDocument();
		return UnoRuntime.queryInterface(XCalculatable.class, doc);
	}

	public static boolean isRunning() {
		return isRunning;
	}

	// for mocking
	public void processBoxinfo(String infoMsg) {
		ProcessBox.from().prefix(logPrefix).info(infoMsg);
	}

	/**
	 * @return the workingSpreadsheet
	 */
	public WorkingSpreadsheet getWorkingSpreadsheet() {
		return workingSpreadsheet;
	}

}
