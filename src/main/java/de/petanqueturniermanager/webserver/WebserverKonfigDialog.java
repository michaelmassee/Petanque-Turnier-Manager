package de.petanqueturniermanager.webserver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.PushButtonType;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XCheckBox;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XNameContainer;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.comp.DocumentHelper;
import de.petanqueturniermanager.comp.GlobalProperties;
import de.petanqueturniermanager.comp.GlobalProperties.PortEintragRoh;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.konfigdialog.AbstractUnoDialog;

/**
 * Modaler Konfigurationsdialog für den eingebetteten Webserver.
 * <p>
 * Zeigt eine Tabellen-UI: jede Port-Konfiguration als eigene Zeile mit
 * Port-Eingabefeld, editierbarer ComboBox für den Sheet-Typ und Aktiv-Checkbox.
 * Zeilen können per ✕-Button gelöscht und per „+ Hinzufügen" ergänzt werden.
 * Validierung erfolgt beim OK-Klick; bei Fehler bleibt der Dialog offen.
 */
public class WebserverKonfigDialog extends AbstractUnoDialog {

	private static final Logger logger = LogManager.getLogger(WebserverKonfigDialog.class);

	// ---- Bekannte Sheet-Typ-Schlüssel (zentral definiert in SheetResolverFactory) ----
	private static final String DEFAULT_TYP = SheetResolverFactory.DEFAULT_SHEET_TYP;
	private static final int DEFAULT_PORT = 9000;
	private static final String[] SHEET_TYPEN = SheetResolverFactory.SHEET_TYPEN;

	// ---- Layout-Konstanten ----
	private static final int DIALOG_BREITE = 270;
	private static final int DIALOG_HOEHE = 250;
	private static final int MAX_ZEILEN = 10;
	private static final int ZEILE_H = 14;
	private static final int PORT_X = 5;
	private static final int PORT_W = 45;
	private static final int TYP_X = 55;
	private static final int TYP_W = 130;
	private static final int AKTIV_X = 190;
	private static final int AKTIV_W = 25;
	private static final int DEL_X = 220;
	private static final int DEL_W = 18;
	private static final int ZEILE_Y_START = 33;
	private static final int ZEILE_ABSTAND = 16;
	private static final int FOOTER_Y = 230;
	private static final int OK_X = 145;
	private static final int OK_W = 50;
	private static final int ABBRECHEN_X = 200;
	private static final int ABBRECHEN_W = 65;

	// ---- UNO-Referenzen (gesetzt in erstelleFelder) ----
	private XMultiServiceFactory xMSF;
	private XNameContainer cont;
	private XControlContainer xcc;
	private XDialog xDialog;
	private XCheckBox cbAktiv;

	// ---- Dialog-Zustand ----
	private List<ZeilenDaten> zeilenDaten = new ArrayList<>();
	private final List<String> dynamischeControlNamen = new ArrayList<>();
	private String[] komboBoxItems;

	private record ZeilenDaten(String port, String typ, boolean aktiv) {
	}

	/** Interne Exception für Validierungsfehler – bleibt im Dialog. */
	private static final class UngueltigeEingabeException extends Exception {
		UngueltigeEingabeException(String meldung) {
			super(meldung);
		}
	}

	public WebserverKonfigDialog(XComponentContext xContext) {
		super(xContext);
	}

	public void zeigen() throws com.sun.star.uno.Exception {
		erstelleUndAusfuehren();
	}

	@Override
	protected String getTitel() {
		return I18n.get("webserver.konfig.dialog.titel");
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
	protected boolean istVeraenderbar() {
		return false;
	}

	@Override
	protected void erstelleFelder(
			XMultiComponentFactory mcf, XMultiServiceFactory xMSF,
			XNameContainer cont, XToolkit xToolkit, XWindowPeer peer,
			XPropertySet dlgProps, XDialog xDialog
	) throws com.sun.star.uno.Exception {

		this.xMSF = xMSF;
		this.cont = cont;
		this.xDialog = xDialog;
		this.xcc = Lo.qi(XControlContainer.class, xDialog);

		var gp = GlobalProperties.get();
		zeilenDaten = new ArrayList<>();
		for (var eintrag : gp.getPortEintraege()) {
			zeilenDaten.add(new ZeilenDaten(
					String.valueOf(eintrag.port()), eintrag.sheetConfig(), eintrag.aktiv()));
		}

		komboBoxItems = ladeComboBoxItems();

		erstelleStatischeControls();
		aktualisiereZeilenArea();
	}

	// ---- Statische Controls (einmalig, nie in dynamischeControlNamen) ----

	private void erstelleStatischeControls() throws com.sun.star.uno.Exception {
		fuegeCheckBoxEin("cbAktiv",
				I18n.get("webserver.konfig.cb.aktiv"),
				5, 5, 260, 12, GlobalProperties.get().isWebserverAktiv());
		cbAktiv = leseCheckBox("cbAktiv");

		fuegeFixedTextEin("lblKopfPort",
				I18n.get("webserver.konfig.tabelle.kopf.port"),
				PORT_X, 20, PORT_W, 10);
		fuegeFixedTextEin("lblKopfTyp",
				I18n.get("webserver.konfig.tabelle.kopf.typ"),
				TYP_X, 20, TYP_W, 10);
		fuegeFixedTextEin("lblKopfAktiv",
				I18n.get("webserver.konfig.tabelle.kopf.aktiv"),
				AKTIV_X, 20, AKTIV_W + DEL_W, 10);
	}

	// ---- Dynamischer Bereich ----

	private void aktualisiereZeilenArea() throws com.sun.star.uno.Exception {
		bereinigeDynamischeControls();
		erstelleZeilenControls();
		erstelleFooterControls();
	}

	private void bereinigeDynamischeControls() {
		for (String name : dynamischeControlNamen) {
			entferneControl(name);
		}
		dynamischeControlNamen.clear();
	}

	private void erstelleZeilenControls() throws com.sun.star.uno.Exception {
		for (int i = 0; i < zeilenDaten.size(); i++) {
			int y = ZEILE_Y_START + i * ZEILE_ABSTAND;
			var zd = zeilenDaten.get(i);
			fuegeEditEinDyn("portRow_" + i, zd.port(), PORT_X, y, PORT_W, ZEILE_H);
			fuegeComboBoxEinDyn("typRow_" + i, komboBoxItems, TYP_X, y, TYP_W, ZEILE_H, zd.typ());
			fuegeCheckBoxEinDyn("aktivRow_" + i, "", AKTIV_X, y, AKTIV_W, ZEILE_H, zd.aktiv());
			fuegeButtonEinDyn("delRow_" + i,
					I18n.get("webserver.konfig.btn.zeile.loeschen"),
					DEL_X, y, DEL_W, ZEILE_H, (short) PushButtonType.STANDARD_value);
			setzeTooltip("delRow_" + i, I18n.get("webserver.konfig.btn.zeile.loeschen.tooltip"));
		}
		for (int i = 0; i < zeilenDaten.size(); i++) {
			final int idx = i;
			registriereActionListener("delRow_" + idx, () -> loescheZeile(idx));
		}
	}

	private void erstelleFooterControls() throws com.sun.star.uno.Exception {
		int addY = ZEILE_Y_START + zeilenDaten.size() * ZEILE_ABSTAND + 4;
		fuegeButtonEinDyn("btnHinzufuegen",
				I18n.get("webserver.konfig.btn.hinzufuegen"),
				5, addY, 65, ZEILE_H, (short) PushButtonType.STANDARD_value);
		fuegeButtonEinDyn("btnOk",
				I18n.get("dialog.ok"),
				OK_X, FOOTER_Y, OK_W, ZEILE_H, (short) PushButtonType.STANDARD_value);
		fuegeButtonEinDyn("btnAbbrechen",
				I18n.get("dialog.abbrechen"),
				ABBRECHEN_X, FOOTER_Y, ABBRECHEN_W, ZEILE_H, (short) PushButtonType.CANCEL_value);

		if (zeilenDaten.size() >= MAX_ZEILEN) {
			XControl addBtn = xcc.getControl("btnHinzufuegen");
			if (addBtn != null) {
				Lo.qi(XPropertySet.class, addBtn.getModel()).setPropertyValue("Enabled", Boolean.FALSE);
			}
		}

		registriereActionListener("btnHinzufuegen", this::fuegeZeileHinzu);
		registriereActionListener("btnOk", this::beimOkKlick);
	}

	// ---- Aktionen ----

	private void fuegeZeileHinzu() {
		try {
			leseZeilenDatenAusControls();
			zeilenDaten.add(new ZeilenDaten(
					String.valueOf(berechneNaechstenFreienPort()), DEFAULT_TYP, true));
			aktualisiereZeilenArea();
			XControl neuesPortFeld = xcc.getControl("portRow_" + (zeilenDaten.size() - 1));
			if (neuesPortFeld != null) {
				Lo.qi(XWindow.class, neuesPortFeld).setFocus();
			}
		} catch (com.sun.star.uno.Exception e) {
			logger.error("Fehler beim Hinzufügen einer Zeile: {}", e.getMessage(), e);
		}
	}

	private void loescheZeile(int idx) {
		try {
			leseZeilenDatenAusControls();
			zeilenDaten.remove(idx);
			aktualisiereZeilenArea();
		} catch (com.sun.star.uno.Exception e) {
			logger.error("Fehler beim Löschen einer Zeile: {}", e.getMessage(), e);
		}
	}

	private void beimOkKlick() {
		leseZeilenDatenAusControls();
		try {
			var eintraege = validiereUndKonvertiere();
			boolean aktiv = cbAktiv != null && cbAktiv.getState() == 1;
			GlobalProperties.get().speichernWebserver(aktiv, eintraege);
			logger.info("Webserver-Konfiguration gespeichert: aktiv={}, ports={}", aktiv, eintraege.size());
			xDialog.endExecute();
		} catch (UngueltigeEingabeException e) {
			MessageBox.from(xContext, MessageBoxTypeEnum.ERROR_OK)
					.caption(I18n.get("webserver.konfig.fehler.titel"))
					.message(e.getMessage())
					.show();
		}
	}

	// ---- Zeilendaten lesen / validieren ----

	private void leseZeilenDatenAusControls() {
		zeilenDaten.clear();
		for (int i = 0; ; i++) {
			XControl portCtrl = xcc.getControl("portRow_" + i);
			if (portCtrl == null) {
				break;
			}
			String port = Lo.qi(XTextComponent.class, portCtrl).getText().trim();
			String typ = Lo.qi(XTextComponent.class, xcc.getControl("typRow_" + i)).getText().trim();
			boolean ak = Lo.qi(XCheckBox.class, xcc.getControl("aktivRow_" + i)).getState() == 1;
			zeilenDaten.add(new ZeilenDaten(port, typ, ak));
		}
	}

	private List<PortEintragRoh> validiereUndKonvertiere() throws UngueltigeEingabeException {
		List<PortEintragRoh> ergebnis = new ArrayList<>();
		Set<Integer> bekannte = new HashSet<>();
		for (int i = 0; i < zeilenDaten.size(); i++) {
			var zd = zeilenDaten.get(i);
			int nr = i + 1;
			if (zd.port().isEmpty()) {
				throw new UngueltigeEingabeException(I18n.get("webserver.konfig.fehler.port.leer", nr));
			}
			int port;
			try {
				port = Integer.parseInt(zd.port());
			} catch (NumberFormatException e) {
				throw new UngueltigeEingabeException(
						I18n.get("webserver.konfig.fehler.port.ungueltig", nr, zd.port()));
			}
			if (port < 1 || port > 65535) {
				throw new UngueltigeEingabeException(
						I18n.get("webserver.konfig.fehler.port.ungueltig", nr, zd.port()));
			}
			if (!bekannte.add(port)) {
				throw new UngueltigeEingabeException(
						I18n.get("webserver.konfig.fehler.port.duplikat", port));
			}
			if (zd.typ().isEmpty()) {
				throw new UngueltigeEingabeException(I18n.get("webserver.konfig.fehler.typ.leer", nr));
			}
			ergebnis.add(new PortEintragRoh(port, zd.typ(), zd.aktiv()));
		}
		return ergebnis;
	}

	// ---- Hilfsmethoden: ComboBox-Items + Port-Berechnung ----

	private String[] ladeComboBoxItems() {
		var items = new LinkedHashSet<String>();
		XSpreadsheetDocument doc = DocumentHelper.getCurrentSpreadsheetDocument(xContext);
		if (doc != null) {
			items.addAll(Arrays.asList(doc.getSheets().getElementNames()));
		}
		items.addAll(Arrays.asList(SHEET_TYPEN));
		return items.toArray(String[]::new);
	}

	private int berechneNaechstenFreienPort() {
		Set<Integer> belegt = new HashSet<>();
		for (var zd : zeilenDaten) {
			if (zd.port().matches("\\d+")) {
				belegt.add(Integer.parseInt(zd.port()));
			}
		}
		int kandidat = DEFAULT_PORT;
		while (belegt.contains(kandidat)) {
			kandidat++;
		}
		return kandidat;
	}

	// ---- Hilfsmethoden: Control-Erstellung (statisch, kein Tracking) ----

	private void fuegeCheckBoxEin(String name, String label, int x, int y, int w, int h, boolean checked)
			throws com.sun.star.uno.Exception {
		var model = xMSF.createInstance("com.sun.star.awt.UnoControlCheckBoxModel");
		var props = Lo.qi(XPropertySet.class, model);
		props.setPropertyValue("Label",     label);
		props.setPropertyValue("PositionX", x);
		props.setPropertyValue("PositionY", y);
		props.setPropertyValue("Width",     w);
		props.setPropertyValue("Height",    h);
		props.setPropertyValue("State",     (short) (checked ? 1 : 0));
		cont.insertByName(name, model);
	}

	private void fuegeFixedTextEin(String name, String label, int x, int y, int w, int h)
			throws com.sun.star.uno.Exception {
		var model = xMSF.createInstance("com.sun.star.awt.UnoControlFixedTextModel");
		var props = Lo.qi(XPropertySet.class, model);
		props.setPropertyValue("Label",     label);
		props.setPropertyValue("PositionX", x);
		props.setPropertyValue("PositionY", y);
		props.setPropertyValue("Width",     w);
		props.setPropertyValue("Height",    h);
		cont.insertByName(name, model);
	}

	// ---- Hilfsmethoden: dynamische Controls (mit Tracking) ----

	private void fuegeEditEinDyn(String name, String text, int x, int y, int w, int h)
			throws com.sun.star.uno.Exception {
		var model = xMSF.createInstance("com.sun.star.awt.UnoControlEditModel");
		var props = Lo.qi(XPropertySet.class, model);
		props.setPropertyValue("PositionX", x);
		props.setPropertyValue("PositionY", y);
		props.setPropertyValue("Width",     w);
		props.setPropertyValue("Height",    h);
		props.setPropertyValue("Text",      text != null ? text : "");
		props.setPropertyValue("MultiLine", Boolean.FALSE);
		cont.insertByName(name, model);
		dynamischeControlNamen.add(name);
	}

	private void fuegeComboBoxEinDyn(String name, String[] items, int x, int y, int w, int h, String selected)
			throws com.sun.star.uno.Exception {
		var model = xMSF.createInstance("com.sun.star.awt.UnoControlComboBoxModel");
		var props = Lo.qi(XPropertySet.class, model);
		props.setPropertyValue("PositionX",      x);
		props.setPropertyValue("PositionY",      y);
		props.setPropertyValue("Width",          w);
		props.setPropertyValue("Height",         h);
		props.setPropertyValue("StringItemList", items);
		props.setPropertyValue("Text",           selected != null ? selected : "");
		props.setPropertyValue("Dropdown",       Boolean.TRUE);
		cont.insertByName(name, model);
		dynamischeControlNamen.add(name);
	}

	private void fuegeCheckBoxEinDyn(String name, String label, int x, int y, int w, int h, boolean checked)
			throws com.sun.star.uno.Exception {
		var model = xMSF.createInstance("com.sun.star.awt.UnoControlCheckBoxModel");
		var props = Lo.qi(XPropertySet.class, model);
		props.setPropertyValue("Label",     label);
		props.setPropertyValue("PositionX", x);
		props.setPropertyValue("PositionY", y);
		props.setPropertyValue("Width",     w);
		props.setPropertyValue("Height",    h);
		props.setPropertyValue("State",     (short) (checked ? 1 : 0));
		cont.insertByName(name, model);
		dynamischeControlNamen.add(name);
	}

	private void fuegeButtonEinDyn(String name, String label, int x, int y, int w, int h, short pushButtonType)
			throws com.sun.star.uno.Exception {
		var model = xMSF.createInstance("com.sun.star.awt.UnoControlButtonModel");
		var props = Lo.qi(XPropertySet.class, model);
		props.setPropertyValue("Label",          label);
		props.setPropertyValue("PositionX",      x);
		props.setPropertyValue("PositionY",      y);
		props.setPropertyValue("Width",          w);
		props.setPropertyValue("Height",         h);
		props.setPropertyValue("PushButtonType", pushButtonType);
		cont.insertByName(name, model);
		dynamischeControlNamen.add(name);
	}

	private void setzeTooltip(String name, String text) {
		XControl ctrl = xcc.getControl(name);
		if (ctrl != null) {
			try {
				Lo.qi(XPropertySet.class, ctrl.getModel()).setPropertyValue("HelpText", text);
			} catch (com.sun.star.uno.Exception e) {
				logger.warn("Tooltip konnte nicht gesetzt werden für '{}': {}", name, e.getMessage());
			}
		}
	}

	private void registriereActionListener(String ctlName, Runnable aktion) {
		XControl ctrl = xcc.getControl(ctlName);
		if (ctrl == null) {
			return;
		}
		Lo.qi(XButton.class, ctrl).addActionListener(new com.sun.star.awt.XActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				aktion.run();
			}

			@Override
			public void disposing(EventObject e) {
				// kein Aufräumen nötig
			}
		});
	}

	private void entferneControl(String name) {
		try {
			cont.removeByName(name);
		} catch (NoSuchElementException | WrappedTargetException e) {
			// Control existiert nicht oder kann nicht entfernt werden – ignorieren
		}
	}

	private XCheckBox leseCheckBox(String name) {
		XControl ctrl = xcc.getControl(name);
		return ctrl != null ? Lo.qi(XCheckBox.class, ctrl) : null;
	}
}
