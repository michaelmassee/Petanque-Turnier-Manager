/**

* Erstellung : 24.03.2018 / Michael Massee
**/

package de.petanqueturniermanager;

import static com.google.common.base.Preconditions.*;

import org.apache.logging.log4j.Logger;

import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.msgbox.ErrorMessageBox;
import de.petanqueturniermanager.helper.sheet.SheetHelper;

public abstract class SheetRunner extends Thread implements Runnable {

	private final XComponentContext xContext;
	private final SheetHelper sheetHelper;
	public static boolean isRunning = false;

	public SheetRunner(XComponentContext xContext) {
		checkNotNull(xContext);
		this.xContext = xContext;
		this.sheetHelper = new SheetHelper(xContext);
	}

	@Override
	public final void run() {
		if (!SheetRunner.isRunning) {
			SheetRunner.isRunning = true;
			try {
				doRun();
			} catch (GenerateException e) {
				handleGenerateException(e);
			} catch (Exception e) {
				getLogger().error(e.getMessage(), e);
			} finally {
				SheetRunner.isRunning = false;
				// TODO
				// CloseConnections.closeOfficeConnection(); // bringt Nix ?!?
			}
		}
	}

	protected void handleGenerateException(GenerateException e) {
		getLogger().error(e.getMessage(), e);
		ErrorMessageBox errMsg = new ErrorMessageBox(getxContext());
		errMsg.showOk("Fehler", e.getMessage());

	}

	public abstract Logger getLogger();

	protected abstract void doRun() throws GenerateException;

	public SheetHelper getSheetHelper() {
		return this.sheetHelper;
	}

	public XComponentContext getxContext() {
		return this.xContext;
	}

}
