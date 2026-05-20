/*
 * Erstellung : 2026-05-11 / Michael Massee
 */
package de.petanqueturniermanager.helper.sheet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

	private static final Logger LOGGER = LogManager.getLogger(ControllerLock.class);

	private final XModel xModel;

	private ControllerLock(XModel xModel) {
		this.xModel = xModel;
	}

	/**
	 * Sperrt die Controller des Dokuments. Ist das Dokument oder Model {@code null}
	 * – oder ist das Model bereits disposed – wird ein No-Op-Lock zurückgegeben.
	 * {@code DisposedException} und sonstige {@link RuntimeException} aus
	 * {@code lockControllers()} treten beim LO-Shutdown / Dokumentwechsel real auf
	 * und sollen den Lauf nicht abbrechen.
	 */
	public static ControllerLock lock(XSpreadsheetDocument doc) {
		XModel model = Lo.qi(XModel.class, doc);
		if (model == null) {
			return new ControllerLock(null);
		}
		try {
			model.lockControllers();
			return new ControllerLock(model);
		} catch (RuntimeException e) {
			LOGGER.debug("lockControllers() fehlgeschlagen – No-Op-Lock", e);
			return new ControllerLock(null);
		}
	}

	@Override
	public void close() {
		if (xModel == null) {
			return;
		}
		try {
			xModel.unlockControllers();
		} catch (RuntimeException e) {
			LOGGER.debug("unlockControllers() fehlgeschlagen – ignoriert", e);
		}
	}
}
