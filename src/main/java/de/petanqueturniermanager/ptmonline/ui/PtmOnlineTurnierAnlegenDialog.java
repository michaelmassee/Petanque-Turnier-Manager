/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.ui;

import java.time.LocalDate;
import java.time.LocalTime;

import org.jspecify.annotations.Nullable;

import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.PushButtonType;
import com.sun.star.awt.XActionListener;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XDateField;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XTimeField;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XNameContainer;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.uno.XComponentContext;
import com.sun.star.util.Date;
import com.sun.star.util.Time;

import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.konfigdialog.AbstractUnoDialog;

/**
 * Fragt Name, Datum und Ort fuer ein neu online anzulegendes Turnier ab. Das lokale Turnierdokument
 * kennt diese Angaben nicht als eigenes Konzept (keine Name/Datum/Ort-Property in der Konfiguration),
 * daher werden sie hier einmalig erfasst statt geraten.
 */
public final class PtmOnlineTurnierAnlegenDialog extends AbstractUnoDialog {

    private static final int DIALOG_BREITE = 220;
    private static final int DIALOG_HOEHE = 126;
    private static final LocalTime STANDARD_STARTZEIT = LocalTime.of(9, 0);
    private static final int LABEL_X = 8;
    private static final int LABEL_W = 55;
    private static final int FELD_X = 66;
    private static final int FELD_W = 146;
    private static final int ZEILE_H = 16;

    @Nullable private final XWindowPeer parentPeer;

    @Nullable private XControlContainer xcc;
    @Nullable private XDialog xDialog;
    @Nullable private Werte ergebnis;

    public PtmOnlineTurnierAnlegenDialog(XComponentContext xContext, @Nullable XWindowPeer parentPeer) {
        super(xContext);
        this.parentPeer = parentPeer;
    }

    @Override
    protected XWindowPeer holeParentPeer() {
        return parentPeer;
    }

    /** Liefert die eingegebenen Werte, oder {@code null} bei Abbruch. */
    public @Nullable Werte zeigen() throws com.sun.star.uno.Exception {
        erstelleUndAusfuehren();
        return ergebnis;
    }

    @Override
    protected String getTitel() {
        return I18n.get("ptmonline.turnier.dialog.titel");
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

        int y = 8;
        label(xMSF, cont, "lblName", I18n.get("ptmonline.turnier.dialog.label.name"), LABEL_X, y, LABEL_W, 10);
        textFeld(xMSF, cont, "txtName", "", FELD_X, y - 2, FELD_W, 12);

        y += ZEILE_H;
        label(xMSF, cont, "lblDatum", I18n.get("ptmonline.turnier.dialog.label.datum"), LABEL_X, y, LABEL_W, 10);
        datumFeld(xMSF, cont, "txtDatum", LocalDate.now(), FELD_X, y - 2, FELD_W, 12);

        y += ZEILE_H;
        label(xMSF, cont, "lblStartzeit", I18n.get("ptmonline.turnier.dialog.label.startzeit"), LABEL_X, y, LABEL_W, 10);
        zeitFeld(xMSF, cont, "txtStartzeit", STANDARD_STARTZEIT, FELD_X, y - 2, FELD_W, 12);

        y += ZEILE_H;
        label(xMSF, cont, "lblOrt", I18n.get("ptmonline.turnier.dialog.label.ort"), LABEL_X, y, LABEL_W, 10);
        textFeld(xMSF, cont, "txtOrt", "", FELD_X, y - 2, FELD_W, 12);

        y += ZEILE_H + 8;
        button(xMSF, cont, "btnOk", I18n.get("dialog.ok"), FELD_X + FELD_W - 135, y, 55, ZEILE_H,
                (short) PushButtonType.STANDARD_value);
        button(xMSF, cont, "btnAbbrechen", I18n.get("dialog.abbrechen"), FELD_X + FELD_W - 75, y, 75, ZEILE_H,
                (short) PushButtonType.CANCEL_value);

        registriereKlick(xcc, "btnOk", this::beimOkGeklickt);
    }

    /** Vom Nutzer eingegebene, validierte Turnier-Eckdaten. */
    public record Werte(String name, String datumIso, String startzeitIso, String ort) {
    }

    private void beimOkGeklickt() {
        if (xDialog == null || xcc == null) {
            return;
        }
        String name = text(xcc, "txtName");
        LocalDate datum = datum(xcc, "txtDatum");
        LocalTime startzeit = zeit(xcc, "txtStartzeit");
        String ort = text(xcc, "txtOrt");

        if (name.length() < 2) {
            zeigeFehler(I18n.get("ptmonline.turnier.dialog.fehler.name_leer"));
            return;
        }
        if (ort.isBlank()) {
            zeigeFehler(I18n.get("ptmonline.turnier.dialog.fehler.ort_leer"));
            return;
        }

        ergebnis = new Werte(name, datum.toString(), startzeit.toString(), ort);
        xDialog.endExecute();
    }

    private void zeigeFehler(String meldung) {
        MessageBox.from(xContext, MessageBoxTypeEnum.ERROR_OK)
                .caption(I18n.get("ptmonline.fehler.titel"))
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

    private static LocalDate datum(XControlContainer xcc, String name) {
        var ctrl = xcc.getControl(name);
        if (ctrl == null) {
            return LocalDate.now();
        }
        var df = Lo.qi(XDateField.class, ctrl);
        if (df == null) {
            return LocalDate.now();
        }
        Date d = df.getDate();
        return LocalDate.of(d.Year, d.Month, d.Day);
    }

    private static LocalTime zeit(XControlContainer xcc, String name) {
        var ctrl = xcc.getControl(name);
        if (ctrl == null) {
            return STANDARD_STARTZEIT;
        }
        var tf = Lo.qi(XTimeField.class, ctrl);
        if (tf == null) {
            return STANDARD_STARTZEIT;
        }
        Time t = tf.getTime();
        return LocalTime.of(t.Hours, t.Minutes);
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
            String name, String text, int x, int y, int w, int h) throws com.sun.star.uno.Exception {
        var model = xMSF.createInstance("com.sun.star.awt.UnoControlEditModel");
        var props = Lo.qi(XPropertySet.class, model);
        props.setPropertyValue("PositionX", x);
        props.setPropertyValue("PositionY", y);
        props.setPropertyValue("Width", w);
        props.setPropertyValue("Height", h);
        props.setPropertyValue("Text", text);
        props.setPropertyValue("MultiLine", Boolean.FALSE);
        cont.insertByName(name, model);
    }

    private static void datumFeld(XMultiServiceFactory xMSF, XNameContainer cont,
            String name, LocalDate wert, int x, int y, int w, int h) throws com.sun.star.uno.Exception {
        var model = xMSF.createInstance("com.sun.star.awt.UnoControlDateFieldModel");
        var props = Lo.qi(XPropertySet.class, model);
        props.setPropertyValue("PositionX", x);
        props.setPropertyValue("PositionY", y);
        props.setPropertyValue("Width", w);
        props.setPropertyValue("Height", h);
        props.setPropertyValue("Dropdown", Boolean.TRUE);
        props.setPropertyValue("Date",
                new Date((short) wert.getDayOfMonth(), (short) wert.getMonthValue(), (short) wert.getYear()));
        cont.insertByName(name, model);
    }

    private static void zeitFeld(XMultiServiceFactory xMSF, XNameContainer cont,
            String name, LocalTime wert, int x, int y, int w, int h) throws com.sun.star.uno.Exception {
        var model = xMSF.createInstance("com.sun.star.awt.UnoControlTimeFieldModel");
        var props = Lo.qi(XPropertySet.class, model);
        props.setPropertyValue("PositionX", x);
        props.setPropertyValue("PositionY", y);
        props.setPropertyValue("Width", w);
        props.setPropertyValue("Height", h);
        props.setPropertyValue("Time",
                new Time(0, (short) 0, (short) wert.getMinute(), (short) wert.getHour(), false));
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
