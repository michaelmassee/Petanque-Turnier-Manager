/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper.upload;

import org.jspecify.annotations.Nullable;

import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.PushButtonType;
import com.sun.star.awt.XActionListener;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XRadioButton;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XNameContainer;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.comp.GlobalProperties.FtpServerEintrag;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.konfigdialog.AbstractUnoDialog;

/**
 * Modaler Dialog zum Anlegen/Bearbeiten eines einzelnen zentralen FTP/SFTP-Servers
 * (Extras &gt; Optionen &gt; PétTurnMngr &gt; FTP-Server).
 */
public final class FtpServerDetailDialog extends AbstractUnoDialog {

	private static final int DIALOG_BREITE = 260;
	private static final int DIALOG_HOEHE = 168;
	private static final int LABEL_X = 8;
	private static final int LABEL_W = 70;
	private static final int FELD_X = 82;
	private static final int FELD_W = 168;
	private static final int ZEILE_H = 14;

	@Nullable private final FtpServerEintrag initialerEintrag;

	@Nullable private XControlContainer xcc;
	@Nullable private XDialog xDialog;
	@Nullable private FtpServerEintrag ergebnis;

	public FtpServerDetailDialog(XComponentContext xContext, @Nullable FtpServerEintrag initialerEintrag) {
		super(xContext);
		this.initialerEintrag = initialerEintrag;
	}

	/**
	 * Zeigt den Dialog und liefert den konfigurierten Server, oder {@code null} bei Abbruch.
	 */
	public @Nullable FtpServerEintrag zeigen() throws com.sun.star.uno.Exception {
		erstelleUndAusfuehren();
		return ergebnis;
	}

	@Override
	protected String getTitel() {
		return I18n.get("ftp.server.dialog.detail.titel");
	}

	@Override
	protected int getBreite() {
		return DIALOG_BREITE;
	}

	@Override
	protected int getHoehe() {
		return DIALOG_HOEHE;
	}

	@Override
	protected void erstelleFelder(
			XMultiComponentFactory mcf, XMultiServiceFactory xMSF,
			XNameContainer cont, XToolkit xToolkit, XWindowPeer peer,
			XPropertySet dlgProps, XDialog dialog) throws com.sun.star.uno.Exception {
		this.xDialog = dialog;
		this.xcc = Lo.qi(XControlContainer.class, dialog);

		var vorhanden = initialerEintrag;
		boolean istSftp = vorhanden == null || vorhanden.protokoll() == UploadProtokoll.SFTP;

		int y = 8;
		label(xMSF, cont, "lblName", I18n.get("ftp.server.dialog.label.name"), LABEL_X, y, LABEL_W, 10);
		textFeld(xMSF, cont, "txtName", vorhanden == null ? "" : vorhanden.name(), FELD_X, y - 2, FELD_W, 12, false);

		y += ZEILE_H;
		label(xMSF, cont, "lblProtokoll", I18n.get("ftp.server.dialog.label.protokoll"), LABEL_X, y, LABEL_W, 10);
		radioButton(xMSF, cont, "radFtp", "FTP", FELD_X, y, 55, 10, !istSftp);
		radioButton(xMSF, cont, "radSftp", "SFTP", FELD_X + 60, y, 55, 10, istSftp);

		y += ZEILE_H;
		label(xMSF, cont, "lblHost", I18n.get("ftp.server.dialog.label.host"), LABEL_X, y, LABEL_W, 10);
		textFeld(xMSF, cont, "txtHost", vorhanden == null ? "" : vorhanden.host(), FELD_X, y - 2, FELD_W, 12, false);

		y += ZEILE_H;
		label(xMSF, cont, "lblPort", I18n.get("ftp.server.dialog.label.port"), LABEL_X, y, LABEL_W, 10);
		textFeld(xMSF, cont, "txtPort", vorhanden == null ? "" : String.valueOf(vorhanden.port()),
				FELD_X, y - 2, FELD_W, 12, false);

		y += ZEILE_H;
		label(xMSF, cont, "lblBenutzer", I18n.get("ftp.server.dialog.label.benutzer"), LABEL_X, y, LABEL_W, 10);
		textFeld(xMSF, cont, "txtBenutzer", vorhanden == null ? "" : vorhanden.benutzer(), FELD_X, y - 2, FELD_W, 12, false);

		y += ZEILE_H;
		label(xMSF, cont, "lblPasswort", I18n.get("ftp.server.dialog.label.passwort"), LABEL_X, y, LABEL_W, 10);
		textFeld(xMSF, cont, "txtPasswort", vorhanden == null ? "" : vorhanden.passwort(), FELD_X, y - 2, FELD_W, 12, true);

		y += ZEILE_H;
		label(xMSF, cont, "lblVerzeichnis", I18n.get("ftp.server.dialog.label.verzeichnis"), LABEL_X, y, LABEL_W, 10);
		textFeld(xMSF, cont, "txtVerzeichnis", vorhanden == null ? "" : vorhanden.remotePfad(),
				FELD_X, y - 2, FELD_W, 12, false);

		y += ZEILE_H + 8;
		button(xMSF, cont, "btnOk", I18n.get("dialog.ok"), 90, y, 55, ZEILE_H, (short) PushButtonType.STANDARD_value);
		button(xMSF, cont, "btnAbbrechen", I18n.get("dialog.abbrechen"), 150, y, 80, ZEILE_H,
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
		String name = text(xcc, "txtName");
		String host = text(xcc, "txtHost");
		String portText = text(xcc, "txtPort");
		String benutzer = text(xcc, "txtBenutzer");
		String passwort = text(xcc, "txtPasswort");
		String remotePfad = text(xcc, "txtVerzeichnis");
		UploadProtokoll protokoll = istRadioGewaehlt(xcc, "radSftp") ? UploadProtokoll.SFTP : UploadProtokoll.FTP;

		if (host.isBlank()) {
			zeigeFehler(I18n.get("ftp.server.dialog.fehler.host.leer"));
			return;
		}
		if (benutzer.isBlank()) {
			zeigeFehler(I18n.get("ftp.server.dialog.fehler.benutzer.leer"));
			return;
		}
		if (remotePfad.isBlank()) {
			zeigeFehler(I18n.get("ftp.server.dialog.fehler.verzeichnis.leer"));
			return;
		}
		int port = UploadKonfiguration.portOderStandard(portText, protokoll);
		if (!portText.isBlank()) {
			try {
				int eingegeben = Integer.parseInt(portText.trim());
				if (eingegeben < 1 || eingegeben > 65535) {
					zeigeFehler(I18n.get("ftp.server.dialog.fehler.port.ungueltig"));
					return;
				}
				port = eingegeben;
			} catch (NumberFormatException e) {
				zeigeFehler(I18n.get("ftp.server.dialog.fehler.port.ungueltig"));
				return;
			}
		}

		ergebnis = new FtpServerEintrag(
				initialerEintrag == null ? null : initialerEintrag.id(),
				name, protokoll, host, port, benutzer, passwort, remotePfad);
		xDialog.endExecute();
	}

	private void zeigeFehler(String meldung) {
		MessageBox.from(xContext, MessageBoxTypeEnum.ERROR_OK)
				.caption(I18n.get("ftp.server.dialog.fehler.titel"))
				.message(meldung)
				.show();
	}

	// ---- UNO-Control-Hilfsmethoden ----

	private static String text(XControlContainer xcc, String name) {
		var ctrl = xcc.getControl(name);
		if (ctrl == null) {
			return "";
		}
		var tc = Lo.qi(XTextComponent.class, ctrl);
		return tc == null ? "" : tc.getText().trim();
	}

	private static boolean istRadioGewaehlt(XControlContainer xcc, String name) {
		var ctrl = xcc.getControl(name);
		if (ctrl == null) {
			return false;
		}
		XRadioButton radio = Lo.qi(XRadioButton.class, ctrl);
		return radio != null && radio.getState();
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

	private static void textFeld(XMultiServiceFactory xMSF, XNameContainer cont,
			String name, String text, int x, int y, int w, int h, boolean maskiert)
			throws com.sun.star.uno.Exception {
		var model = xMSF.createInstance("com.sun.star.awt.UnoControlEditModel");
		var props = Lo.qi(XPropertySet.class, model);
		props.setPropertyValue("PositionX", x);
		props.setPropertyValue("PositionY", y);
		props.setPropertyValue("Width", w);
		props.setPropertyValue("Height", h);
		props.setPropertyValue("Text", text);
		props.setPropertyValue("MultiLine", Boolean.FALSE);
		if (maskiert) {
			props.setPropertyValue("EchoChar", (short) '*');
		}
		cont.insertByName(name, model);
	}

	private static void radioButton(XMultiServiceFactory xMSF, XNameContainer cont,
			String name, String label, int x, int y, int w, int h, boolean gewaehlt)
			throws com.sun.star.uno.Exception {
		var model = xMSF.createInstance("com.sun.star.awt.UnoControlRadioButtonModel");
		var props = Lo.qi(XPropertySet.class, model);
		props.setPropertyValue("Label", label);
		props.setPropertyValue("PositionX", x);
		props.setPropertyValue("PositionY", y);
		props.setPropertyValue("Width", w);
		props.setPropertyValue("Height", h);
		props.setPropertyValue("State", (short) (gewaehlt ? 1 : 0));
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
