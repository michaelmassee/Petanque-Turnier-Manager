/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper.upload;

import java.io.IOException;

import org.jspecify.annotations.Nullable;

import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.PushButtonType;
import com.sun.star.awt.XActionListener;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XRadioButton;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XNameContainer;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.comp.GlobalProperties.FtpServerEintrag;
import de.petanqueturniermanager.helper.ColorHelper;
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
	private static final int DIALOG_HOEHE = 190;
	private static final int LABEL_X = 8;
	private static final int LABEL_W = 70;
	private static final int FELD_X = 82;
	private static final int FELD_W = 168;
	private static final int PASSWORT_FELD_W = 100;
	private static final int ZEILE_H = 14;
	private static final int TEST_BTN_W = 80;
	private static final int TEST_BTN_X = FELD_X + FELD_W - TEST_BTN_W;

	private static final int FARBE_TEST_UNGETESTET = ColorHelper.CHAR_COLOR_GRAY_SPIELER_NR;
	private static final int FARBE_TEST_ERFOLG = ColorHelper.CHAR_COLOR_GREEN;
	private static final int FARBE_TEST_FEHLER = ColorHelper.CHAR_COLOR_RED;

	@Nullable private final FtpServerEintrag initialerEintrag;
	@Nullable private final XWindowPeer parentPeer;

	@Nullable private XControlContainer xcc;
	@Nullable private XDialog xDialog;
	@Nullable private FtpServerEintrag ergebnis;

	/**
	 * @param parentPeer Peer der aufrufenden Optionsseite. Ohne Parent würde der Dialog vom
	 *                    Fenster-Manager als eigenständiges Top-Level-Fenster behandelt und wäre
	 *                    nicht modal gegenüber dem Optionen-Dialog.
	 */
	public FtpServerDetailDialog(XComponentContext xContext, @Nullable FtpServerEintrag initialerEintrag,
			@Nullable XWindowPeer parentPeer) {
		super(xContext);
		this.initialerEintrag = initialerEintrag;
		this.parentPeer = parentPeer;
	}

	@Override
	protected XWindowPeer holeParentPeer() {
		return parentPeer;
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
		textFeld(xMSF, cont, "txtPasswort", vorhanden == null ? "" : vorhanden.passwort(),
				FELD_X, y - 2, PASSWORT_FELD_W, 12, true);
		button(xMSF, cont, "btnPasswortAnzeigen", I18n.get("ftp.server.dialog.label.passwort.anzeigen"),
				FELD_X + PASSWORT_FELD_W + 4, y - 2, FELD_W - PASSWORT_FELD_W - 4, 12,
				(short) PushButtonType.STANDARD_value);

		y += ZEILE_H;
		label(xMSF, cont, "lblVerzeichnis", I18n.get("ftp.server.dialog.label.verzeichnis"), LABEL_X, y, LABEL_W, 10);
		textFeld(xMSF, cont, "txtVerzeichnis", vorhanden == null ? "" : vorhanden.remotePfad(),
				FELD_X, y - 2, FELD_W, 12, false);

		y += ZEILE_H + 8;
		button(xMSF, cont, "btnTest", I18n.get("ftp.server.dialog.btn.test"), TEST_BTN_X, y, TEST_BTN_W, ZEILE_H,
				(short) PushButtonType.STANDARD_value);
		setzeTestButtonFarbe(FARBE_TEST_UNGETESTET);

		y += ZEILE_H + 8;
		button(xMSF, cont, "btnOk", I18n.get("dialog.ok"), 90, y, 55, ZEILE_H, (short) PushButtonType.STANDARD_value);
		button(xMSF, cont, "btnAbbrechen", I18n.get("dialog.abbrechen"), 150, y, 80, ZEILE_H,
				(short) PushButtonType.CANCEL_value);

		registriereKlick(xcc, "btnOk", this::beimOkGeklickt);
		registriereKlick(xcc, "btnTest", this::beimTestGeklickt);
		registriereKlick(xcc, "btnPasswortAnzeigen", () -> beimPasswortAnzeigenGeklickt(xcc));
	}

	/**
	 * Schaltet bei jedem Klick die Maskierung (EchoChar) des Passwortfelds um und passt das
	 * Button-Label entsprechend an ("Anzeigen" / "Verbergen").
	 */
	private static void beimPasswortAnzeigenGeklickt(XControlContainer xcc) {
		var passwortCtrl = xcc.getControl("txtPasswort");
		var buttonCtrl = xcc.getControl("btnPasswortAnzeigen");
		if (passwortCtrl == null || buttonCtrl == null) {
			return;
		}
		var passwortProps = Lo.qi(XPropertySet.class, passwortCtrl.getModel());
		var buttonProps = Lo.qi(XPropertySet.class, buttonCtrl.getModel());
		if (passwortProps == null || buttonProps == null) {
			return;
		}
		try {
			short aktuellerEchoChar = (short) passwortProps.getPropertyValue("EchoChar");
			boolean sichtbar = aktuellerEchoChar == 0;
			passwortProps.setPropertyValue("EchoChar", sichtbar ? (short) '*' : (short) 0);
			erzwingeRepaint(passwortCtrl);
			buttonProps.setPropertyValue("Label", I18n.get(
					sichtbar ? "ftp.server.dialog.label.passwort.anzeigen" : "ftp.server.dialog.label.passwort.verbergen"));
		} catch (Exception ex) {
			// Anzeige-Umschaltung ist rein kosmetisch, Fehler ignorieren
		}
	}

	/**
	 * vcl {@code Edit::SetEchoChar()} invalidiert das Control nicht (LO-Bug) – ein Sichtbarkeits-Toggle
	 * des Peers erzwingt das nötige Repaint, sonst bleibt die alte Maskierung optisch sichtbar.
	 */
	private static void erzwingeRepaint(XControl ctrl) {
		var fenster = Lo.qi(XWindow.class, ctrl.getPeer());
		if (fenster == null) {
			return;
		}
		fenster.setVisible(false);
		fenster.setVisible(true);
	}

	private static void registriereKlick(XControlContainer xcc, String name, Runnable aktion) {
		var ctrl = xcc.getControl(name);
		if (ctrl == null) {
			return;
		}
		var btn = Lo.qi(XButton.class, ctrl);
		if (btn == null) {
			return;
		}
		btn.addActionListener(new XActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				aktion.run();
			}

			@Override
			public void disposing(EventObject e) {
				// nichts zu tun
			}
		});
	}

	/** Aus den Formularfeldern gelesene und validierte Werte. */
	private record FormularWerte(
			String name, UploadProtokoll protokoll, String host, int port,
			String benutzer, String passwort, String remotePfad) {
	}

	/**
	 * Liest die aktuellen Formularwerte und validiert sie. Zeigt bei ungültigen Eingaben eine
	 * Fehlermeldung an und liefert dann {@code null}.
	 */
	private @Nullable FormularWerte leseUndValidiere() {
		if (xcc == null) {
			return null;
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
			return null;
		}
		if (benutzer.isBlank()) {
			zeigeFehler(I18n.get("ftp.server.dialog.fehler.benutzer.leer"));
			return null;
		}
		if (remotePfad.isBlank()) {
			zeigeFehler(I18n.get("ftp.server.dialog.fehler.verzeichnis.leer"));
			return null;
		}
		int port = UploadKonfiguration.portOderStandard(portText, protokoll);
		if (!portText.isBlank()) {
			try {
				int eingegeben = Integer.parseInt(portText.trim());
				if (eingegeben < 1 || eingegeben > 65535) {
					zeigeFehler(I18n.get("ftp.server.dialog.fehler.port.ungueltig"));
					return null;
				}
				port = eingegeben;
			} catch (NumberFormatException e) {
				zeigeFehler(I18n.get("ftp.server.dialog.fehler.port.ungueltig"));
				return null;
			}
		}
		return new FormularWerte(name, protokoll, host, port, benutzer, passwort, remotePfad);
	}

	private void beimOkGeklickt() {
		if (xDialog == null) {
			return;
		}
		var werte = leseUndValidiere();
		if (werte == null) {
			return;
		}
		ergebnis = new FtpServerEintrag(
				initialerEintrag == null ? null : initialerEintrag.id(),
				werte.name(), werte.protokoll(), werte.host(), werte.port(),
				werte.benutzer(), werte.passwort(), werte.remotePfad());
		xDialog.endExecute();
	}

	private void beimTestGeklickt() {
		var werte = leseUndValidiere();
		if (werte == null) {
			return;
		}
		var konfig = new UploadKonfiguration(
				werte.protokoll(), werte.host(), werte.port(), werte.benutzer(), werte.remotePfad());
		try {
			// beimTestGeklickt() läuft synchron im UNO-Dialog-Event (btnTest-Klick) auf dem
			// LO-Main-Thread — aufMainThread=true, sonst würde SftpHostKeyUserInfo per
			// LoMainThread.post()+future.get() deadlocken.
			UploadServiceFactory.erstelle(konfig, xContext, true).testeVerbindung(werte.passwort());
			setzeTestButtonFarbe(FARBE_TEST_ERFOLG);
			MessageBox.from(xContext, MessageBoxTypeEnum.INFO_OK)
					.caption(I18n.get("ftp.server.dialog.test.titel"))
					.message(I18n.get("ftp.server.dialog.test.erfolg"))
					.show();
		} catch (IOException e) {
			setzeTestButtonFarbe(FARBE_TEST_FEHLER);
			MessageBox.from(xContext, MessageBoxTypeEnum.ERROR_OK)
					.caption(I18n.get("ftp.server.dialog.test.titel"))
					.message(I18n.get("ftp.server.dialog.test.fehler", e.getMessage()))
					.show();
		}
	}

	private void setzeTestButtonFarbe(int farbe) {
		if (xcc == null) {
			return;
		}
		var ctrl = xcc.getControl("btnTest");
		if (ctrl == null) {
			return;
		}
		var props = Lo.qi(XPropertySet.class, ctrl.getModel());
		if (props == null) {
			return;
		}
		try {
			props.setPropertyValue("BackgroundColor", farbe);
		} catch (com.sun.star.uno.Exception e) {
			// Farbmarkierung ist rein kosmetisch, Fehler ignorieren
		}
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
