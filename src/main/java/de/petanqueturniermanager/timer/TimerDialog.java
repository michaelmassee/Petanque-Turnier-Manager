package de.petanqueturniermanager.timer;

import java.awt.Color;

import javax.swing.JColorChooser;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.PushButtonType;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XNameContainer;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.konfigdialog.AbstractUnoDialog;

/**
 * Modaler UNO-Dialog zum Konfigurieren und Starten des Rundenzeit-Timers.
 * <p>
 * Felder: Dauer (MM:SS), Bezeichnung (optional), Webserver-Port, Hintergrundfarbe.
 * Buttons: [−1 min] [+1 min] [Starten] [Abbrechen].
 * Letzte Einstellungen werden aus {@link TimerEinstellungen} vorbelegt
 * und nach dem Starten dauerhaft gespeichert.
 */
public class TimerDialog extends AbstractUnoDialog {

    private static final Logger logger = LogManager.getLogger(TimerDialog.class);

    private static final int DIALOG_BREITE = 250;
    private static final int DIALOG_HOEHE  = 150;

    private static final int LBL_X     = 5;
    private static final int LBL_W     = 80;
    private static final int FIELD_X   = 90;
    private static final int FIELD_W   = 150;
    private static final int CTRL_H    = 12;
    private static final int ZEILE1_Y  = 10;
    private static final int ZEILE2_Y  = 30;
    private static final int ZEILE3_Y  = 50;
    private static final int ZEILE4_Y  = 70;
    private static final int DAUER_FIELD_W  = 55;
    private static final int BTN_PM_W       = 45;
    private static final int BTN_PM_GAP     = 5;
    private static final int BTN_MINUS_X    = FIELD_X + DAUER_FIELD_W + BTN_PM_GAP;
    private static final int BTN_PLUS_X     = BTN_MINUS_X + BTN_PM_W + BTN_PM_GAP;
    private static final int BTN_PM_Y       = ZEILE1_Y - 1;
    private static final int BTN_H          = 14;
    private static final int COLOR_VORSCHAU_W = 70;
    private static final int BTN_FARBE_X   = FIELD_X + COLOR_VORSCHAU_W + BTN_PM_GAP;
    private static final int BTN_FARBE_W   = 45;
    private static final int FOOTER_Y      = 125;
    private static final int BTN_START_X   = 90;
    private static final int BTN_ABBRUCH_X = 165;
    private static final int BTN_ACTION_W  = 70;

    private static final String CTRL_DAUER       = "editDauer";
    private static final String CTRL_BEZEICHNUNG = "editBezeichnung";
    private static final String CTRL_PORT        = "editPort";
    private static final String CTRL_FARBE_VORSCHAU = "lblFarbeVorschau";
    private static final String CTRL_BTN_FARBE   = "btnFarbe";

    private XControlContainer xcc;
    private XMultiServiceFactory xMSF;
    private XNameContainer cont;

    private int hintergrundFarbeInt;
    private XPropertySet vorschauProps;

    public TimerDialog(XComponentContext xContext) {
        super(xContext);
    }

    /**
     * Öffnet den Dialog.
     *
     * @throws com.sun.star.uno.Exception bei UNO-Fehlern
     */
    public void zeigen() throws com.sun.star.uno.Exception {
        erstelleUndAusfuehren();
    }

    @Override
    protected String getTitel() {
        return I18n.get("timer.dialog.titel");
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
            XPropertySet dlgProps, XDialog xDialog
    ) throws com.sun.star.uno.Exception {
        this.xMSF = xMSF;
        this.cont = cont;
        this.xcc  = Lo.qi(XControlContainer.class, xDialog);
        this.hintergrundFarbeInt = TimerEinstellungen.letzteHintergrundFarbe();

        // ── Zeile 1: Dauer ──────────────────────────────────────────────────────
        fuegeLabel("lblDauer", I18n.get("timer.dialog.dauer.label"), LBL_X, ZEILE1_Y, LBL_W, CTRL_H);
        fuegeEdit(CTRL_DAUER, TimerEinstellungen.letzteDauer(), FIELD_X, ZEILE1_Y, DAUER_FIELD_W, CTRL_H);

        // +/- Buttons direkt neben dem Dauer-Feld
        fuegeButton("btnMinus", "-1 min", BTN_MINUS_X, BTN_PM_Y, BTN_PM_W, BTN_H, (short) 0);
        fuegeButton("btnPlus",  "+1 min", BTN_PLUS_X,  BTN_PM_Y, BTN_PM_W, BTN_H, (short) 0);

        registriereButtonAktion("btnMinus", () -> passeZeitAn(-60));
        registriereButtonAktion("btnPlus",  () -> passeZeitAn(+60));

        // ── Zeile 2: Bezeichnung ────────────────────────────────────────────────
        fuegeLabel("lblBezeichnung", I18n.get("timer.dialog.bezeichnung.label"), LBL_X, ZEILE2_Y, LBL_W, CTRL_H);
        fuegeEdit(CTRL_BEZEICHNUNG, TimerEinstellungen.letzteBezeichnung(), FIELD_X, ZEILE2_Y, FIELD_W, CTRL_H);

        // ── Zeile 3: Port ───────────────────────────────────────────────────────
        fuegeLabel("lblPort", I18n.get("timer.dialog.port.label"), LBL_X, ZEILE3_Y, LBL_W, CTRL_H);
        fuegeEdit(CTRL_PORT, String.valueOf(TimerEinstellungen.letzterPort()), FIELD_X, ZEILE3_Y, 40, CTRL_H);

        // ── Zeile 4: Hintergrundfarbe ───────────────────────────────────────────
        fuegeLabel("lblFarbe", I18n.get("timer.dialog.hintergrundfarbe.label"), LBL_X, ZEILE4_Y, LBL_W, CTRL_H);
        vorschauProps = fuegeColorVorschau(CTRL_FARBE_VORSCHAU, hintergrundFarbeInt,
                FIELD_X, ZEILE4_Y, COLOR_VORSCHAU_W, CTRL_H);
        fuegeButton(CTRL_BTN_FARBE, "...", BTN_FARBE_X, ZEILE4_Y - 1, BTN_FARBE_W, BTN_H, (short) 0);

        registriereButtonAktion(CTRL_BTN_FARBE, this::oeffneFarbwahl);

        // ── Footer: Starten / Abbrechen ─────────────────────────────────────────
        fuegeButton("btnStarten",   I18n.get("timer.dialog.start.btn"),
                BTN_START_X, FOOTER_Y, BTN_ACTION_W, BTN_H, (short) PushButtonType.OK_value);
        fuegeButton("btnAbbrechen", I18n.get("dialog.abbrechen"),
                BTN_ABBRUCH_X, FOOTER_Y, BTN_ACTION_W, BTN_H, (short) PushButtonType.CANCEL_value);
    }

    @Override
    protected void beiOkGeklickt() throws Exception {
        var dauerText = leseFeld(CTRL_DAUER);
        var bezeichnung = leseFeld(CTRL_BEZEICHNUNG);
        var portText = leseFeld(CTRL_PORT);

        long dauerSekunden;
        try {
            dauerSekunden = TimerManager.parseDauer(dauerText);
        } catch (IllegalArgumentException e) {
            zeigeValidierungsFehler(I18n.get("timer.fehler.dauer.ungueltig"));
            throw e;
        }

        int port;
        try {
            port = Integer.parseInt(portText.trim());
            if (port < 1024 || port > 65535) {
                throw new NumberFormatException("Port außerhalb des Bereichs");
            }
        } catch (NumberFormatException e) {
            zeigeValidierungsFehler(I18n.get("timer.fehler.port.ungueltig"));
            throw new IllegalArgumentException(I18n.get("timer.fehler.port.ungueltig"), e);
        }

        TimerEinstellungen.speichern(dauerText.trim(), port, bezeichnung, hintergrundFarbeInt);
        TimerManager.get().starten(dauerSekunden, bezeichnung, port, hintergrundFarbeInt);
        logger.info("Timer gestartet via Dialog: {} s, Port {}, Farbe #{}", dauerSekunden, port,
                String.format("%06x", hintergrundFarbeInt & 0xFFFFFF));
    }

    // ── Dauer-Anpassung per ±-Button ──────────────────────────────────────────

    private void passeZeitAn(long deltaSekunden) {
        var dauerText = leseFeld(CTRL_DAUER);
        long aktuelle;
        try {
            aktuelle = TimerManager.parseDauer(dauerText);
        } catch (IllegalArgumentException e) {
            aktuelle = 0;
        }
        long neueZeit = Math.max(60, aktuelle + deltaSekunden);
        setzeFeld(CTRL_DAUER, TimerManager.formatiere(neueZeit));
    }

    // ── Farbwahl per JColorChooser ────────────────────────────────────────────

    private void oeffneFarbwahl() {
        try {
            var aktuell = new Color(hintergrundFarbeInt);
            var frame = ProcessBox.from().moveInsideTopWindow().toFront().getFrame();
            var neu = JColorChooser.showDialog(frame, I18n.get("timer.dialog.hintergrundfarbe.label"), aktuell);
            if (neu != null) {
                hintergrundFarbeInt = neu.getRGB() & 0xFFFFFF;
                vorschauProps.setPropertyValue("BackgroundColor", hintergrundFarbeInt);
            }
        } catch (Exception e) {
            logger.error("Fehler bei Farbwahl", e);
        }
    }

    // ── UNO-Hilfsmethoden ─────────────────────────────────────────────────────

    private void fuegeLabel(String name, String text, int x, int y, int w, int h)
            throws com.sun.star.uno.Exception {
        var model = xMSF.createInstance("com.sun.star.awt.UnoControlFixedTextModel");
        var props = Lo.qi(XPropertySet.class, model);
        props.setPropertyValue("Label",     text);
        props.setPropertyValue("PositionX", x);
        props.setPropertyValue("PositionY", y);
        props.setPropertyValue("Width",     w);
        props.setPropertyValue("Height",    h);
        cont.insertByName(name, model);
    }

    /**
     * Erstellt ein gefärbtes Vorschau-Label (Hintergrundfarbe + Rahmen).
     *
     * @return XPropertySet des Label-Modells (für spätere Farbaktualisierung)
     */
    private XPropertySet fuegeColorVorschau(String name, int farbe, int x, int y, int w, int h)
            throws com.sun.star.uno.Exception {
        var model = xMSF.createInstance("com.sun.star.awt.UnoControlFixedTextModel");
        var props = Lo.qi(XPropertySet.class, model);
        props.setPropertyValue("Label",           "");
        props.setPropertyValue("PositionX",       x);
        props.setPropertyValue("PositionY",       y);
        props.setPropertyValue("Width",           w);
        props.setPropertyValue("Height",          h);
        props.setPropertyValue("BackgroundColor", farbe);
        props.setPropertyValue("Border",          (short) 2);
        props.setPropertyValue("BorderColor",     0x000000);
        cont.insertByName(name, model);
        return props;
    }

    private void fuegeEdit(String name, String text, int x, int y, int w, int h)
            throws com.sun.star.uno.Exception {
        var model = xMSF.createInstance("com.sun.star.awt.UnoControlEditModel");
        var props = Lo.qi(XPropertySet.class, model);
        props.setPropertyValue("Text",      text != null ? text : "");
        props.setPropertyValue("PositionX", x);
        props.setPropertyValue("PositionY", y);
        props.setPropertyValue("Width",     w);
        props.setPropertyValue("Height",    h);
        props.setPropertyValue("MultiLine", Boolean.FALSE);
        cont.insertByName(name, model);
    }

    private void fuegeButton(String name, String label, int x, int y, int w, int h, short typ)
            throws com.sun.star.uno.Exception {
        var model = xMSF.createInstance("com.sun.star.awt.UnoControlButtonModel");
        var props = Lo.qi(XPropertySet.class, model);
        props.setPropertyValue("Label",          label);
        props.setPropertyValue("PositionX",      x);
        props.setPropertyValue("PositionY",      y);
        props.setPropertyValue("Width",          w);
        props.setPropertyValue("Height",         h);
        props.setPropertyValue("PushButtonType", typ);
        cont.insertByName(name, model);
    }

    private void registriereButtonAktion(String name, Runnable aktion) {
        XControl ctrl = xcc.getControl(name);
        if (ctrl == null) return;
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

    private String leseFeld(String name) {
        XControl ctrl = xcc.getControl(name);
        if (ctrl == null) return "";
        var tc = Lo.qi(XTextComponent.class, ctrl);
        return tc != null ? tc.getText().trim() : "";
    }

    private void setzeFeld(String name, String text) {
        XControl ctrl = xcc.getControl(name);
        if (ctrl == null) return;
        var tc = Lo.qi(XTextComponent.class, ctrl);
        if (tc != null) {
            tc.setText(text);
        }
    }

    private void zeigeValidierungsFehler(String meldung) {
        MessageBox.from(xContext, MessageBoxTypeEnum.ERROR_OK)
                .caption(I18n.get("timer.dialog.titel"))
                .message(meldung)
                .show();
    }
}
