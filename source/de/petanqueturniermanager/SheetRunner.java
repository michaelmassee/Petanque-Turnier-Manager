/**

* Erstellung : 24.03.2018 / Michael Massee
**/

package de.petanqueturniermanager;

import static com.google.common.base.Preconditions.*;

import org.apache.logging.log4j.Logger;

import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.helper.sheet.CloseConnections;
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
	public void run() {
		if (!SheetRunner.isRunning) {
			SheetRunner.isRunning = true;
			try {
				doRun();
			} catch (Exception e) {
				getLogger().error(e.getMessage(), e);
			} finally {
				SheetRunner.isRunning = false;
				CloseConnections.closeOfficeConnection();
			}
		}
	}

	protected abstract Logger getLogger();

	protected abstract void doRun();

	public SheetHelper getSheetHelper() {
		return this.sheetHelper;
	}

	public XComponentContext getxContext() {
		return this.xContext;
	}

}
