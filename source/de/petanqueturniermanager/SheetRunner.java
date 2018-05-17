/**

* Erstellung : 24.03.2018 / Michael Massee
**/

package de.petanqueturniermanager;

import static com.google.common.base.Preconditions.*;

import org.apache.logging.log4j.Logger;

import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.msgbox.ErrorMessageBox;
import de.petanqueturniermanager.helper.msgbox.QuestionBox;
import de.petanqueturniermanager.helper.msgbox.WarningBox;
import de.petanqueturniermanager.helper.sheet.SheetHelper;

public abstract class SheetRunner extends Thread implements Runnable {

	private static final String VERARBEITUNG_ABGEBROCHEN = "Verarbeitung abgebrochen";
	private final XComponentContext xContext;
	private final SheetHelper sheetHelper;
	private static volatile boolean isRunning = false; // nur 1 Sheetrunner gleichzeitig
	private static SheetRunner runner = null;

	public SheetRunner(XComponentContext xContext) {
		checkNotNull(xContext);
		this.xContext = xContext;
		this.sheetHelper = new SheetHelper(xContext);
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

	protected ErrorMessageBox newErrMsgBox() {
		return new ErrorMessageBox(getxContext());
	}

	protected WarningBox newWarningBox() {
		return new WarningBox(getxContext());
	}

	protected QuestionBox newQuestionBox() {
		return new QuestionBox(getxContext());
	}

	@Override
	public final void run() {
		if (!SheetRunner.isRunning) {
			SheetRunner.isRunning = true;
			SheetRunner.runner = this;

			try {
				doRun();
			} catch (GenerateException e) {
				handleGenerateException(e);
			} catch (Exception e) {
				getLogger().error(e.getMessage(), e);
			} finally {
				SheetRunner.isRunning = false;
				SheetRunner.runner = null;

				// TODO
				// CloseConnections.closeOfficeConnection(); // bringt Nix ?!?
			}
		} else {
			newWarningBox().showOk("Abbruch",
					"Die Verarbeitung wurde nicht gestartet, weil bereits eine Aktive vorhanden.");
		}
	}

	protected void handleGenerateException(GenerateException e) {
		if (VERARBEITUNG_ABGEBROCHEN.equals(e.getMessage())) {
			newWarningBox().showOk("Abbruch", e.getMessage());
		} else {
			getLogger().error(e.getMessage(), e);
			newErrMsgBox().showOk("Fehler", e.getMessage());
		}
	}

	public abstract Logger getLogger();

	protected abstract void doRun() throws GenerateException;

	public SheetHelper getSheetHelper() throws GenerateException {
		SheetRunner.testDoCancelTask();
		return this.sheetHelper;
	}

	public XComponentContext getxContext() {
		return this.xContext;
	}

}
