/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper.upload;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

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

import de.petanqueturniermanager.comp.GlobalProperties.FtpServerEintrag;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.LoMainThread;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.konfigdialog.AbstractUnoDialog;

/**
 * Modaler Dialog zur Auswahl eines zentral konfigurierten FTP/SFTP-Servers vor dem Upload.
 * Vorselektiert den zuletzt für dieses Dokument verwendeten Server, falls noch vorhanden.
 *
 * <p>Kann aus einem Worker-Thread aufgerufen werden — der Dialog wird via
 * {@link LoMainThread#post} auf den LO-Main-Thread marshalled und blockiert
 * den aufrufenden Thread bis der Benutzer den Dialog schließt.
 */
final class FtpServerAuswahlDialog extends AbstractUnoDialog {

	private static final Logger logger = LogManager.getLogger(FtpServerAuswahlDialog.class);

	private final List<FtpServerEintrag> server;
	private final String letzteServerId;
	private final WorkingSpreadsheet ws;

	@Nullable private XControlContainer xcc;
	@Nullable private XDialog xDialog;
	@Nullable private FtpServerEintrag ausgewaehlt;

	private FtpServerAuswahlDialog(XComponentContext xContext, List<FtpServerEintrag> server,
			String letzteServerId, WorkingSpreadsheet ws) {
		super(xContext);
		this.server = server;
		this.letzteServerId = letzteServerId;
		this.ws = ws;
	}

	/**
	 * Zeigt den Auswahl-Dialog und blockiert bis der Benutzer antwortet.
	 *
	 * @return gewählter Server, oder {@link Optional#empty()} bei Abbruch
	 */
	static Optional<FtpServerEintrag> zeigen(WorkingSpreadsheet ws, List<FtpServerEintrag> server,
			String letzteServerId) throws GenerateException {
		var future = new CompletableFuture<Optional<FtpServerEintrag>>();
		LoMainThread.post(ws.getxContext(), () -> {
			try {
				var dialog = new FtpServerAuswahlDialog(ws.getxContext(), server, letzteServerId, ws);
				dialog.erstelleUndAusfuehren();
				future.complete(Optional.ofNullable(dialog.ausgewaehlt));
			} catch (Exception e) {
				logger.error("Fehler im FTP-Server-Auswahl-Dialog", e);
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
		return I18n.get("ftp.server.auswahl.dialog.titel");
	}

	@Override
	protected int getBreite() {
		return 220;
	}

	@Override
	protected int getHoehe() {
		return 90;
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

		label(xMSF, cont, "lblServer", I18n.get("ftp.server.auswahl.dialog.label"), 8, 8, 204, 10);

		String[] items = server.stream().map(FtpServerEintrag::anzeigeName).toArray(String[]::new);
		int vorauswahl = 0;
		for (int i = 0; i < server.size(); i++) {
			if (server.get(i).id().equals(letzteServerId)) {
				vorauswahl = i;
				break;
			}
		}
		listBox(xMSF, cont, "lstServer", items, vorauswahl, 8, 20, 204, 45);

		button(xMSF, cont, "btnOk", I18n.get("dialog.ok"), 40, 70, 55, 14, (short) PushButtonType.STANDARD_value);
		button(xMSF, cont, "btnAbbrechen", I18n.get("dialog.abbrechen"), 105, 70, 75, 14,
				(short) PushButtonType.CANCEL_value);

		var okCtrl = xcc.getControl("btnOk");
		if (okCtrl != null) {
			var btn = Lo.qi(XButton.class, okCtrl);
			if (btn != null) {
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
		}
	}

	private void beimOkGeklickt() {
		if (xcc == null || xDialog == null) {
			return;
		}
		var listCtrl = xcc.getControl("lstServer");
		if (listCtrl != null) {
			XListBox listBox = Lo.qi(XListBox.class, listCtrl);
			short pos = listBox == null ? -1 : listBox.getSelectedItemPos();
			if (pos >= 0 && pos < server.size()) {
				ausgewaehlt = server.get(pos);
			}
		}
		xDialog.endExecute();
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
			props.setPropertyValue("SelectedItems", new short[] { (short) vorauswahl });
		}
		cont.insertByName(name, model);
	}

	private static void button(XMultiServiceFactory xMSF, XNameContainer cont,
			String name, String label, int x, int y, int w, int h, short typ) throws com.sun.star.uno.Exception {
		var model = xMSF.createInstance("com.sun.star.awt.UnoControlButtonModel");
		var props = Lo.qi(XPropertySet.class, model);
		props.setPropertyValue("Label", label);
		props.setPropertyValue("PositionX", x);
		props.setPropertyValue("PositionY", y);
		props.setPropertyValue("Width", w);
		props.setPropertyValue("Height", h);
		props.setPropertyValue("PushButtonType", typ);
		cont.insertByName(name, model);
	}
}
