/*
 * Erstellung : 2026-05-11 / Michael Massee
 */
package de.petanqueturniermanager.helper.sheet;

import com.sun.star.frame.XModel;
import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.helper.Lo;

/**
 * AutoCloseable-Wrapper um {@link XModel#lockControllers()} / {@link XModel#unlockControllers()}.
 * <p>
 * Während des Lock-Zustands unterdrückt LibreOffice das Repaint der View nach jeder
 * UNO-Property-Änderung. Massenoperationen auf Zellen/Properties werden dadurch um
 * Größenordnungen schneller, weil pro Operation kein Redraw mehr ausgelöst wird.
 * <p>
 * Nutzung als try-with-resources:
 * <pre>{@code
 * try (ControllerLock ignored = ControllerLock.lock(getWorkingSpreadsheet().getWorkingSpreadsheetDocument())) {
 *     // viele schreibende UNO-Calls hier
 * }
 * }</pre>
 */
public final class ControllerLock implements AutoCloseable {

	private final XModel xModel;

	private ControllerLock(XModel xModel) {
		this.xModel = xModel;
	}

	/**
	 * Sperrt die Controller des Dokuments. Ist das Dokument oder Model {@code null},
	 * wird ein No-Op-Lock zurückgegeben (defensiv).
	 */
	public static ControllerLock lock(XSpreadsheetDocument doc) {
		XModel model = Lo.qi(XModel.class, doc);
		if (model != null) {
			model.lockControllers();
		}
		return new ControllerLock(model);
	}

	@Override
	public void close() {
		if (xModel != null) {
			xModel.unlockControllers();
		}
	}
}
