/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.siegergeld;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.PushButtonType;
import com.sun.star.awt.XActionListener;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XListBox;
import com.sun.star.awt.XNumericField;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XNameContainer;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XMultiServiceFactory;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.LoMainThread;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.konfigdialog.AbstractUnoDialog;

final class SiegergeldStartDialog extends AbstractUnoDialog {

	private static final Logger logger = LogManager.getLogger(SiegergeldStartDialog.class);
	private static final Duration DIALOG_TIMEOUT = Duration.ofMinutes(30);

	private static final SiegergeldModus[] MODI = SiegergeldModus.values();
	private static final SiegergeldVerteilungsVorlage[] VORLAGEN = SiegergeldVerteilungsVorlage.values();

	private final WorkingSpreadsheet ws;

	private XControlContainer xcc;
	private XDialog xDialog;
	private SiegergeldStartOptionen ausgewaehlt;

	private SiegergeldStartDialog(WorkingSpreadsheet ws) {
		super(ws.getxContext());
		this.ws = ws;
	}

	static Optional<SiegergeldStartOptionen> zeigen(WorkingSpreadsheet ws) throws GenerateException {
		var future = new CompletableFuture<Optional<SiegergeldStartOptionen>>();
		LoMainThread.post(ws.getxContext(), () -> {
			try {
				var dialog = new SiegergeldStartDialog(ws);
				dialog.erstelleUndAusfuehren();
				future.complete(Optional.ofNullable(dialog.ausgewaehlt));
			} catch (Exception e) {
				logger.error("Fehler im Siegergeld-Startdialog", e);
				future.completeExceptionally(e);
			}
		});
		try {
			return future.get(DIALOG_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return Optional.empty();
		} catch (ExecutionException e) {
			Throwable cause = e.getCause();
			throw new GenerateException(cause != null ? cause.getMessage() : e.getMessage());
		} catch (TimeoutException e) {
			logger.warn("Siegergeld-Startdialog hat nicht rechtzeitig geantwortet", e);
			throw new GenerateException("Siegergeld-Startdialog hat nicht rechtzeitig geantwortet");
		}
	}

	@Override
	protected String getTitel() {
		return I18n.get("siegergeld.dialog.titel");
	}

	@Override
	protected int getBreite() {
		return 250;
	}

	@Override
	protected int getHoehe() {
		return 145;
	}

	@Override
	protected XWindowPeer holeParentPeer() {
		return ws.getContainerWindowPeer();
	}

	@Override
	protected void erstelleFelder(XMultiComponentFactory mcf, XMultiServiceFactory xMSF,
			XNameContainer cont, XToolkit xToolkit, XWindowPeer peer,
			XPropertySet dlgProps, XDialog dialog) throws com.sun.star.uno.Exception {
		this.xDialog = dialog;
		this.xcc = Lo.qi(XControlContainer.class, dialog);

		label(xMSF, cont, "lblModus", I18n.get("siegergeld.dialog.modus"), 8, 8, 90, 10);
		listBox(xMSF, cont, "lstModus", modusItems(), 0, 105, 6, 135, 32);
		label(xMSF, cont, "lblGruppen", I18n.get("siegergeld.dialog.gruppen"), 8, 42, 90, 10);
		numericField(xMSF, cont, "nfGruppen", SiegergeldStartOptionen.DEFAULT_GRUPPEN_ANZAHL, 1,
				SiegergeldStartOptionen.MAX_GRUPPEN, 105, 40, 45, 12);
		label(xMSF, cont, "lblPlaetze", I18n.get("siegergeld.dialog.plaetze"), 8, 62, 90, 10);
		numericField(xMSF, cont, "nfPlaetze", SiegergeldStartOptionen.DEFAULT_PLAETZE, 1,
				SiegergeldStartOptionen.MAX_PLAETZE, 105, 60, 45, 12);
		label(xMSF, cont, "lblTopX", I18n.get("siegergeld.dialog.topx"), 8, 82, 90, 10);
		numericField(xMSF, cont, "nfTopX", SiegergeldStartOptionen.DEFAULT_PLAETZE, 1,
				SiegergeldStartOptionen.MAX_PLAETZE, 105, 80, 45, 12);
		label(xMSF, cont, "lblVorlage", I18n.get("siegergeld.dialog.vorlage"), 8, 104, 90, 10);
		listBox(xMSF, cont, "lstVorlage", vorlagenItems(), 0, 105, 102, 135, 32);

		button(xMSF, cont, "btnOk", I18n.get("dialog.ok"), 55, 124, 55, 14,
				(short) PushButtonType.STANDARD_value);
		button(xMSF, cont, "btnAbbrechen", I18n.get("dialog.abbrechen"), 120, 124, 75, 14,
				(short) PushButtonType.CANCEL_value);
		registriereOkButton();
	}

	private void registriereOkButton() {
		var okCtrl = xcc.getControl("btnOk");
		if (okCtrl == null) {
			return;
		}
		var btn = Lo.qi(XButton.class, okCtrl);
		if (btn == null) {
			return;
		}
		btn.addActionListener(new XActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				beimOkGeklickt();
			}

			@Override
			public void disposing(EventObject e) {
				// nichts zu tun
			}
		});
	}

	private void beimOkGeklickt() {
		SiegergeldModus modus = MODI[Math.max(0, Math.min(MODI.length - 1, listBoxIndex("lstModus")))];
		SiegergeldVerteilungsVorlage vorlage =
				VORLAGEN[Math.max(0, Math.min(VORLAGEN.length - 1, listBoxIndex("lstVorlage")))];
		ausgewaehlt = new SiegergeldStartOptionen(modus,
				numericValue("nfGruppen", SiegergeldStartOptionen.DEFAULT_GRUPPEN_ANZAHL),
				numericValue("nfPlaetze", SiegergeldStartOptionen.DEFAULT_PLAETZE),
				numericValue("nfTopX", SiegergeldStartOptionen.DEFAULT_PLAETZE),
				vorlage);
		xDialog.endExecute();
	}

	private int listBoxIndex(String name) {
		var ctrl = xcc.getControl(name);
		XListBox listBox = ctrl == null ? null : Lo.qi(XListBox.class, ctrl);
		return listBox == null ? 0 : listBox.getSelectedItemPos();
	}

	private int numericValue(String name, int fallback) {
		var ctrl = xcc.getControl(name);
		XNumericField numericField = ctrl == null ? null : Lo.qi(XNumericField.class, ctrl);
		return numericField == null ? fallback : (int) Math.round(numericField.getValue());
	}

	private static String[] modusItems() {
		return new String[] {
				I18n.get("siegergeld.dialog.modus.gruppen"),
				I18n.get("siegergeld.dialog.modus.rangliste")
		};
	}

	private static String[] vorlagenItems() {
		String[] items = new String[VORLAGEN.length];
		for (int i = 0; i < VORLAGEN.length; i++) {
			items[i] = switch (VORLAGEN[i]) {
			case STANDARD_60_30_10 -> I18n.get("siegergeld.dialog.vorlage.60_30_10");
			case STAFFEL_50_30_20 -> I18n.get("siegergeld.dialog.vorlage.50_30_20");
			case GLEICHMAESSIG -> I18n.get("siegergeld.dialog.vorlage.gleich");
			};
		}
		return items;
	}

	private static void label(XMultiServiceFactory xMSF, XNameContainer cont,
			String name, String text, int x, int y, int w, int h) throws com.sun.star.uno.Exception {
		var model = xMSF.createInstance("com.sun.star.awt.UnoControlFixedTextModel");
		var props = Lo.qi(XPropertySet.class, model);
		props.setPropertyValue("Label", text);
		props.setPropertyValue("PositionX", x);
		props.setPropertyValue("PositionY", y);
		props.setPropertyValue("Width", w);
		props.setPropertyValue("Height", h);
		cont.insertByName(name, model);
	}

	private static void listBox(XMultiServiceFactory xMSF, XNameContainer cont,
			String name, String[] items, int vorauswahl, int x, int y, int w, int h)
			throws com.sun.star.uno.Exception {
		var model = xMSF.createInstance("com.sun.star.awt.UnoControlListBoxModel");
		var props = Lo.qi(XPropertySet.class, model);
		props.setPropertyValue("PositionX", x);
		props.setPropertyValue("PositionY", y);
		props.setPropertyValue("Width", w);
		props.setPropertyValue("Height", h);
		props.setPropertyValue("StringItemList", items);
		props.setPropertyValue("MultiSelection", Boolean.FALSE);
		props.setPropertyValue("Dropdown", Boolean.FALSE);
		if (items.length > 0) {
			props.setPropertyValue("SelectedItems", new short[] { (short) Math.max(0, vorauswahl) });
		}
		cont.insertByName(name, model);
	}

	private static void numericField(XMultiServiceFactory xMSF, XNameContainer cont,
			String name, int wert, int min, int max, int x, int y, int w, int h)
			throws com.sun.star.uno.Exception {
		var model = xMSF.createInstance("com.sun.star.awt.UnoControlNumericFieldModel");
		var props = Lo.qi(XPropertySet.class, model);
		props.setPropertyValue("PositionX", x);
		props.setPropertyValue("PositionY", y);
		props.setPropertyValue("Width", w);
		props.setPropertyValue("Height", h);
		props.setPropertyValue("Value", (double) wert);
		props.setPropertyValue("ValueMin", (double) min);
		props.setPropertyValue("ValueMax", (double) max);
		props.setPropertyValue("DecimalAccuracy", Short.valueOf((short) 0));
		props.setPropertyValue("Spin", Boolean.TRUE);
		cont.insertByName(name, model);
	}

	private static void button(XMultiServiceFactory xMSF, XNameContainer cont,
			String name, String text, int x, int y, int w, int h, short pushButtonType)
			throws com.sun.star.uno.Exception {
		var model = xMSF.createInstance("com.sun.star.awt.UnoControlButtonModel");
		var props = Lo.qi(XPropertySet.class, model);
		props.setPropertyValue("Label", text);
		props.setPropertyValue("PositionX", x);
		props.setPropertyValue("PositionY", y);
		props.setPropertyValue("Width", w);
		props.setPropertyValue("Height", h);
		props.setPropertyValue("PushButtonType", pushButtonType);
		cont.insertByName(name, model);
	}
}
