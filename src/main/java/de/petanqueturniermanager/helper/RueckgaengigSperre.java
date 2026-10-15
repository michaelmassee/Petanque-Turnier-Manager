/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.document.EmptyUndoStackException;
import com.sun.star.document.XUndoManager;
import com.sun.star.document.XUndoManagerSupplier;
import com.sun.star.lang.DisposedException;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.util.InvalidStateException;
import com.sun.star.util.NotLockedException;

import de.petanqueturniermanager.exception.GenerateException;

/**
 * Hält automatische Schreibvorgänge (z.&nbsp;B. den Neuaufbau einer Rangliste durch die Sheet-Synchronisation)
 * als eigenen Eintrag aus der Rückgängig-Liste heraus. Ohne das würde ein Strg+Z direkt nach einem
 * Hintergrund-Neuaufbau zuerst diesen zurücknehmen statt der letzten Eingabe des Anwenders.
 * <p>
 * Umsetzung über einen <b>versteckten Undo-Kontext</b> ({@link XUndoManager#enterHiddenUndoContext()}): Die
 * Schreibvorgänge hängen unsichtbar an der obersten Aktion. Strg+Z nimmt die letzte Eingabe und den daraus
 * folgenden Neuaufbau gemeinsam zurück – die Rangliste bleibt konsistent.
 * <p>
 * {@link XUndoManager#lock()} allein genügt in Calc nicht: viele Calc-Operationen sichern ihren eigenen
 * Undo-Schalter und setzen ihn danach zurück ({@code ScDocument::EnableUndo(bOld)}), womit die Sperre aufgehoben
 * wäre. Sie dient nur als Notlösung bei leerer Rückgängig-Liste (dann gibt es keine Aktion zum Anhängen).
 * <p>
 * Gilt dokumentweit: Eingaben, die der Anwender genau während eines solchen Laufs macht, hängen ebenfalls am
 * versteckten Kontext.
 */
public final class RueckgaengigSperre {

	private static final Logger logger = LogManager.getLogger(RueckgaengigSperre.class);

	private RueckgaengigSperre() {
	}

	@FunctionalInterface
	public interface Aktion {
		void ausfuehren() throws GenerateException;
	}

	@FunctionalInterface
	public interface Berechnung<T> {
		T berechne() throws GenerateException;
	}

	/**
	 * Führt die Aktion mit gesperrter Rückgängig-Liste aus. Bietet das Dokument keinen Undo-Manager an, läuft die
	 * Aktion ohne Sperre.
	 */
	public static void ohneRueckgaengig(XSpreadsheetDocument xDoc, Aktion aktion) throws GenerateException {
		ohneRueckgaengig(xDoc, () -> {
			aktion.ausfuehren();
			return null;
		});
	}

	/** Wie {@link #ohneRueckgaengig(XSpreadsheetDocument, Aktion)}, mit Ergebnis. */
	public static <T> T ohneRueckgaengig(XSpreadsheetDocument xDoc, Berechnung<T> berechnung)
			throws GenerateException {
		XUndoManager undoManager = undoManager(xDoc);
		if (undoManager == null) {
			logger.debug("Kein Undo-Manager verfügbar, Aktion läuft ohne Rückgängig-Behandlung");
			return berechnung.berechne();
		}
		try {
			undoManager.enterHiddenUndoContext();
		} catch (EmptyUndoStackException e) {
			logger.debug("Rückgängig-Liste leer, Aktion läuft mit gesperrter Liste", e);
			return mitSperre(undoManager, berechnung);
		}
		try {
			return berechnung.berechne();
		} finally {
			verlasseKontext(undoManager);
		}
	}

	private static <T> T mitSperre(XUndoManager undoManager, Berechnung<T> berechnung) throws GenerateException {
		undoManager.lock();
		try {
			return berechnung.berechne();
		} finally {
			entsperre(undoManager);
		}
	}

	private static XUndoManager undoManager(XSpreadsheetDocument xDoc) {
		XUndoManagerSupplier supplier = Lo.qi(XUndoManagerSupplier.class, xDoc);
		return supplier == null ? null : supplier.getUndoManager();
	}

	private static void verlasseKontext(XUndoManager undoManager) {
		try {
			undoManager.leaveUndoContext();
		} catch (InvalidStateException e) {
			// Nur möglich, wenn fremder Code den Kontext zwischenzeitlich verlassen hat.
			logger.warn("Versteckter Undo-Kontext war bereits verlassen", e);
		} catch (DisposedException e) {
			logger.debug("Dokument beim Verlassen des Undo-Kontexts bereits geschlossen", e);
		}
	}

	private static void entsperre(XUndoManager undoManager) {
		try {
			undoManager.unlock();
		} catch (NotLockedException e) {
			// Nur möglich, wenn fremder Code die Sperre zwischenzeitlich aufgehoben hat.
			logger.warn("Rückgängig-Sperre war beim Entsperren bereits aufgehoben", e);
		} catch (DisposedException e) {
			logger.debug("Dokument beim Entsperren der Rückgängig-Liste bereits geschlossen", e);
		}
	}
}
