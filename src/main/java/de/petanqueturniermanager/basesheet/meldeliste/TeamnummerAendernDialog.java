/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.PushButtonType;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XNumericField;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XNameContainer;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XMultiServiceFactory;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.LoMainThread;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.konfigdialog.AbstractUnoDialog;

/**
 * Fragt alte und neue Teamnummer für „Teamnummer ändern“ ab. Muss aus einem Hintergrund-Thread (SheetRunner)
 * aufgerufen werden: der Dialog läuft auf dem LO-Main-Thread, der Aufrufer wartet auf das Ergebnis.
 */
final class TeamnummerAendernDialog extends AbstractUnoDialog {

	private static final Logger logger = LogManager.getLogger(TeamnummerAendernDialog.class);
	private static final Duration DIALOG_TIMEOUT = Duration.ofMinutes(30);
	private static final int MAX_NR = 999;

	/** Eingabe des Dialogs. */
	record Wunsch(int alteNr, int neueNr) {
	}

	private final WorkingSpreadsheet ws;
	private XControlContainer xcc;
	private Wunsch wunsch;

	private TeamnummerAendernDialog(WorkingSpreadsheet ws) {
		super(ws.getxContext());
		this.ws = ws;
	}

	/** @return die Eingabe, leer bei Abbruch */
	static Optional<Wunsch> zeigen(WorkingSpreadsheet ws) throws GenerateException {
		var future = new CompletableFuture<Optional<Wunsch>>();
		LoMainThread.post(ws.getxContext(), () -> {
			try {
				var dialog = new TeamnummerAendernDialog(ws);
				dialog.erstelleUndAusfuehren();
				future.complete(Optional.ofNullable(dialog.wunsch));
			} catch (Exception e) {
				logger.error("Fehler im Dialog Teamnummer ändern", e);
				future.completeExceptionally(e);
			}
		});
		try {
			return future.get(DIALOG_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return Optional.empty();
		} catch (ExecutionException e) {
			Throwable ursache = e.getCause() != null ? e.getCause() : e;
			throw new GenerateException(ursache.getMessage());
		} catch (TimeoutException e) {
			logger.warn("Dialog Teamnummer ändern hat nicht rechtzeitig geantwortet", e);
			return Optional.empty();
		}
	}

	@Override
	protected String getTitel() {
		return I18n.get("teamnummer.aendern.dialog.titel");
	}

	@Override
	protected int getBreite() {
		return 200;
	}

	@Override
	protected int getHoehe() {
		return 104;
	}

	@Override
	protected XWindowPeer holeParentPeer() {
		return ws.getContainerWindowPeer();
	}

	@Override
	protected void erstelleFelder(XMultiComponentFactory mcf, XMultiServiceFactory xMSF, XNameContainer cont,
			XToolkit xToolkit, XWindowPeer peer, XPropertySet dlgProps, XDialog dialog)
			throws com.sun.star.uno.Exception {
		xcc = Lo.qi(XControlContainer.class, dialog);

		label(xMSF, cont, "lblHinweis", I18n.get("teamnummer.aendern.dialog.hinweis"), new Rahmen(8, 6, 184, 34),
				true);
		label(xMSF, cont, "lblAlt", I18n.get("teamnummer.aendern.dialog.alte"), new Rahmen(8, 46, 100, 10), false);
		numericField(xMSF, cont, "nfAlt", 1, 1, MAX_NR, new Rahmen(120, 44, 45, 12));
		label(xMSF, cont, "lblNeu", I18n.get("teamnummer.aendern.dialog.neue"), new Rahmen(8, 64, 100, 10), false);
		numericField(xMSF, cont, "nfNeu", 1, 1, MAX_NR, new Rahmen(120, 62, 45, 12));

		button(xMSF, cont, "btnOk", I18n.get("dialog.ok"), new Rahmen(50, 84, 55, 14), (short) PushButtonType.OK_value);
		button(xMSF, cont, "btnAbbrechen", I18n.get("dialog.abbrechen"), new Rahmen(112, 84, 75, 14),
				(short) PushButtonType.CANCEL_value);
	}

	@Override
	protected void beiOkGeklickt() {
		wunsch = new Wunsch(zahl("nfAlt"), zahl("nfNeu"));
	}

	private int zahl(String name) {
		XNumericField feld = Lo.qi(XNumericField.class, xcc.getControl(name));
		return feld == null ? 0 : (int) Math.round(feld.getValue());
	}
}
