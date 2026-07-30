/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.whatsapp;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.PushButtonType;
import com.sun.star.awt.XActionListener;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XListBox;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XNameContainer;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.comp.GlobalProperties.WhatsAppChatEintrag;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.LoMainThread;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.konfigdialog.AbstractUnoDialog;

public final class WhatsAppChatAuswahlDialog extends AbstractUnoDialog {

	private static final Logger logger = LogManager.getLogger(WhatsAppChatAuswahlDialog.class);

	private final WorkingSpreadsheet ws;
	private final List<WhatsAppChatEintrag> chats;
	private final String letzterChatId;

	private XControlContainer xcc;
	private XDialog xDialog;
	private WhatsAppChatEintrag ausgewaehlt;

	private WhatsAppChatAuswahlDialog(WorkingSpreadsheet ws, List<WhatsAppChatEintrag> chats, String letzterChatId) {
		super(ws.getxContext());
		this.ws = ws;
		this.chats = chats;
		this.letzterChatId = letzterChatId;
	}

	public static Optional<WhatsAppChatEintrag> zeigen(WorkingSpreadsheet ws, List<WhatsAppChatEintrag> chats,
			String letzterChatId) throws GenerateException {
		var future = new CompletableFuture<Optional<WhatsAppChatEintrag>>();
		LoMainThread.post(ws.getxContext(), () -> {
			try {
				var dialog = new WhatsAppChatAuswahlDialog(ws, chats, letzterChatId);
				dialog.erstelleUndAusfuehren();
				future.complete(Optional.ofNullable(dialog.ausgewaehlt));
			} catch (Exception e) {
				logger.error("Fehler im WhatsApp-Chat-Auswahl-Dialog", e);
				future.completeExceptionally(e);
			}
		});
		try {
			return future.get();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return Optional.empty();
		} catch (ExecutionException e) {
			Throwable cause = e.getCause();
			throw new GenerateException(cause != null ? cause.getMessage() : e.getMessage());
		}
	}

	@Override
	protected String getTitel() {
		return I18n.get("whatsapp.chat.auswahl.dialog.titel");
	}

	@Override
	protected int getBreite() {
		return 240;
	}

	@Override
	protected int getHoehe() {
		return 95;
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
		label(xMSF, cont, "lblChat", I18n.get("whatsapp.chat.auswahl.dialog.label"), 8, 8, 224, 10);
		String[] items = chats.stream().map(WhatsAppChatAuswahlDialog::anzeigeZeile).toArray(String[]::new);
		listBox(xMSF, cont, "lstChat", items, vorauswahl(), 8, 20, 224, 45);
		button(xMSF, cont, "btnOk", I18n.get("dialog.ok"), 52, 70, 55, 14, (short) PushButtonType.STANDARD_value);
		button(xMSF, cont, "btnAbbrechen", I18n.get("dialog.abbrechen"), 117, 70, 75, 14,
				(short) PushButtonType.CANCEL_value);
		registriereOkButton();
	}

	private int vorauswahl() {
		for (int i = 0; i < chats.size(); i++) {
			if (chats.get(i).id().equals(letzterChatId)) {
				return i;
			}
		}
		return 0;
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
		var listCtrl = xcc.getControl("lstChat");
		if (listCtrl != null) {
			XListBox listBox = Lo.qi(XListBox.class, listCtrl);
			short pos = listBox == null ? -1 : listBox.getSelectedItemPos();
			if (pos >= 0 && pos < chats.size()) {
				ausgewaehlt = chats.get(pos);
			}
		}
		xDialog.endExecute();
	}

	private static String anzeigeZeile(WhatsAppChatEintrag e) {
		String typ = e.chatTyp().isBlank() ? "Chat" : e.chatTyp();
		return e.anzeigeName() + " (" + typ + ")";
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
		if (items.length > 0) {
			props.setPropertyValue("SelectedItems", new short[] { (short) Math.max(0, vorauswahl) });
		}
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
