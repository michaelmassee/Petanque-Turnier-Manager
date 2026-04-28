/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.formulex.meldeliste;

import java.util.Optional;

import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.XActionListener;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XCheckBox;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XControlModel;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XRadioButton;
import com.sun.star.awt.XSpinField;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindow;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XNameContainer;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;

/**
 * Modaler Dialog zur Abfrage der Turnier-Parameter vor dem Anlegen einer neuen
 * Formule X Meldeliste.
 * <p>
 * Abgefragt werden:
 * <ul>
 *   <li>Formation: Tête / Doublette / Triplette (Radio-Buttons)</li>
 *   <li>Teamname anzeigen: Ja / Nein (Checkbox)</li>
 *   <li>Vereinsname anzeigen: Ja / Nein (Checkbox)</li>
 *   <li>Anzahl Runden (Spinner, Minimum 1)</li>
 * </ul>
 */
class FormuleXTurnierParameterDialog {

    record TurnierParameter(
            Formation formation,
            boolean teamnameAnzeigen,
            boolean vereinsnameAnzeigen,
            int anzahlRunden) {
    }

    private final WorkingSpreadsheet workingSpreadsheet;
    private volatile boolean okGedrueckt = false;

    private FormuleXTurnierParameterDialog(WorkingSpreadsheet workingSpreadsheet) {
        this.workingSpreadsheet = workingSpreadsheet;
    }

    static FormuleXTurnierParameterDialog from(WorkingSpreadsheet workingSpreadsheet) {
        return new FormuleXTurnierParameterDialog(workingSpreadsheet);
    }

    Optional<TurnierParameter> anzeigen(Formation standardFormation, boolean standardTeamnameAnzeigen,
            boolean standardVereinsnameAnzeigen, int standardAnzahlRunden) throws com.sun.star.uno.Exception {

        ProcessBox.from().hide();

        XComponentContext context = workingSpreadsheet.getxContext();
        XMultiComponentFactory xMCF = context.getServiceManager();

        Object dialogModel = xMCF.createInstanceWithContext("com.sun.star.awt.UnoControlDialogModel", context);
        XPropertySet dlgProps = Lo.qi(XPropertySet.class, dialogModel);
        dlgProps.setPropertyValue("PositionX", Integer.valueOf(50));
        dlgProps.setPropertyValue("PositionY", Integer.valueOf(50));
        dlgProps.setPropertyValue("Width", Integer.valueOf(160));
        dlgProps.setPropertyValue("Height", Integer.valueOf(165));
        dlgProps.setPropertyValue("Title", I18n.get("dialog.formulex.turnier.parameter.titel"));
        dlgProps.setPropertyValue("Moveable", Boolean.TRUE);

        Object dialog = xMCF.createInstanceWithContext("com.sun.star.awt.UnoControlDialog", context);
        XControl xControl = Lo.qi(XControl.class, dialog);
        xControl.setModel(Lo.qi(XControlModel.class, dialogModel));

        XMultiServiceFactory xMSF = Lo.qi(XMultiServiceFactory.class, dialogModel);
        XNameContainer cont = Lo.qi(XNameContainer.class, dialogModel);
        XControlContainer xcc = Lo.qi(XControlContainer.class, dialog);

        fuegeLabel(xMSF, cont, "lblFormation", I18n.get("dialog.poule.label.formation"), 8, 8, 80, 10);
        fuegeRadioButton(xMSF, cont, "radioTete",
                Formation.TETE.getBezeichnung(), 8, 21, 140, 10, standardFormation == Formation.TETE);
        fuegeRadioButton(xMSF, cont, "radioDoublette",
                Formation.DOUBLETTE.getBezeichnung(), 8, 33, 140, 10, standardFormation == Formation.DOUBLETTE);
        fuegeRadioButton(xMSF, cont, "radioTriplette",
                Formation.TRIPLETTE.getBezeichnung(), 8, 45, 140, 10, standardFormation == Formation.TRIPLETTE);

        fuegeTrennlinie(xMSF, cont, "sep1", 5, 59, 150, 2);

        fuegeCheckBox(xMSF, cont, "cbTeamname", I18n.get("dialog.poule.label.teamname"),
                8, 65, 140, 10, standardTeamnameAnzeigen);
        fuegeCheckBox(xMSF, cont, "cbVereinsname", I18n.get("dialog.poule.label.vereinsname"),
                8, 79, 140, 10, standardVereinsnameAnzeigen);

        fuegeTrennlinie(xMSF, cont, "sep2", 5, 93, 150, 2);

        fuegeLabel(xMSF, cont, "lblAnzahlRunden", I18n.get("dialog.formulex.label.anzahl.runden"), 8, 99, 100, 10);
        fuegeSpinner(xMSF, cont, "spinnerRunden", 110, 97, 40, 12, standardAnzahlRunden, 1, 20);

        fuegeTrennlinie(xMSF, cont, "sep3", 5, 115, 150, 2);

        fuegeButton(xMSF, cont, "btnOk", I18n.get("dialog.button.ok"), 22, 143, 50, 14);
        fuegeButton(xMSF, cont, "btnCancel", I18n.get("dialog.button.abbrechen"), 88, 143, 60, 14);

        XDialog xDialog = Lo.qi(XDialog.class, dialog);
        okGedrueckt = false;
        haengeButtonListener(xcc, "btnOk", new XActionListener() {
            @Override
            public void disposing(EventObject e) {
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                okGedrueckt = true;
                xDialog.endExecute();
            }
        });
        haengeButtonListener(xcc, "btnCancel", new XActionListener() {
            @Override
            public void disposing(EventObject e) {
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                xDialog.endExecute();
            }
        });

        Object toolkit = xMCF.createInstanceWithContext("com.sun.star.awt.Toolkit", context);
        XToolkit xToolkit = Lo.qi(XToolkit.class, toolkit);
        XWindow xWindow = Lo.qi(XWindow.class, xControl);
        xWindow.setVisible(false);
        xControl.createPeer(xToolkit, null);

        xDialog.execute();

        Optional<TurnierParameter> ergebnis = Optional.empty();
        if (okGedrueckt) {
            Formation formation = leseFormation(xcc);
            boolean teamnameAnzeigen = leseCheckBoxZustand(xcc, "cbTeamname");
            boolean vereinsnameAnzeigen = leseCheckBoxZustand(xcc, "cbVereinsname");
            int anzahlRunden = leseSpinnerWert(xcc, "spinnerRunden", standardAnzahlRunden);
            ergebnis = Optional.of(new TurnierParameter(formation, teamnameAnzeigen, vereinsnameAnzeigen, anzahlRunden));
        }

        Lo.qi(XComponent.class, dialog).dispose();
        ProcessBox.from().visible();

        return ergebnis;
    }

    // ---------------------------------------------------------------
    // Hilfsmethoden – Zustand auslesen
    // ---------------------------------------------------------------

    private Formation leseFormation(XControlContainer xcc) {
        if (istRadioGewaehlt(xcc, "radioTete")) {
            return Formation.TETE;
        }
        if (istRadioGewaehlt(xcc, "radioDoublette")) {
            return Formation.DOUBLETTE;
        }
        return Formation.TRIPLETTE;
    }

    private boolean istRadioGewaehlt(XControlContainer xcc, String name) {
        XControl ctrl = xcc.getControl(name);
        if (ctrl == null) {
            return false;
        }
        XRadioButton radio = Lo.qi(XRadioButton.class, ctrl);
        return radio != null && radio.getState();
    }

    private boolean leseCheckBoxZustand(XControlContainer xcc, String name) {
        XControl ctrl = xcc.getControl(name);
        if (ctrl == null) {
            return false;
        }
        XCheckBox cb = Lo.qi(XCheckBox.class, ctrl);
        return cb != null && cb.getState() == 1;
    }

    private int leseSpinnerWert(XControlContainer xcc, String name, int fallback) {
        XControl ctrl = xcc.getControl(name);
        if (ctrl == null) {
            return fallback;
        }
        XSpinField sf = Lo.qi(XSpinField.class, ctrl);
        if (sf == null) {
            return fallback;
        }
        try {
            XPropertySet props = Lo.qi(XPropertySet.class, ctrl.getModel());
            Object val = props.getPropertyValue("Value");
            return val instanceof Number n ? n.intValue() : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    private void haengeButtonListener(XControlContainer xcc, String name, XActionListener listener) {
        XControl ctrl = xcc.getControl(name);
        if (ctrl != null) {
            XButton btn = Lo.qi(XButton.class, ctrl);
            if (btn != null) {
                btn.addActionListener(listener);
            }
        }
    }

    // ---------------------------------------------------------------
    // Hilfsmethoden – Controls zum Dialog-Modell hinzufügen
    // ---------------------------------------------------------------

    private void fuegeLabel(XMultiServiceFactory xMSF, XNameContainer cont,
            String name, String text, int x, int y, int w, int h) throws com.sun.star.uno.Exception {
        Object model = xMSF.createInstance("com.sun.star.awt.UnoControlFixedTextModel");
        XPropertySet props = Lo.qi(XPropertySet.class, model);
        props.setPropertyValue("Label", text);
        props.setPropertyValue("PositionX", Integer.valueOf(x));
        props.setPropertyValue("PositionY", Integer.valueOf(y));
        props.setPropertyValue("Width", Integer.valueOf(w));
        props.setPropertyValue("Height", Integer.valueOf(h));
        cont.insertByName(name, model);
    }

    private void fuegeRadioButton(XMultiServiceFactory xMSF, XNameContainer cont,
            String name, String label, int x, int y, int w, int h, boolean gewaehlt)
            throws com.sun.star.uno.Exception {
        Object model = xMSF.createInstance("com.sun.star.awt.UnoControlRadioButtonModel");
        XPropertySet props = Lo.qi(XPropertySet.class, model);
        props.setPropertyValue("Label", label);
        props.setPropertyValue("PositionX", Integer.valueOf(x));
        props.setPropertyValue("PositionY", Integer.valueOf(y));
        props.setPropertyValue("Width", Integer.valueOf(w));
        props.setPropertyValue("Height", Integer.valueOf(h));
        props.setPropertyValue("State", (short) (gewaehlt ? 1 : 0));
        cont.insertByName(name, model);
    }

    private void fuegeCheckBox(XMultiServiceFactory xMSF, XNameContainer cont,
            String name, String label, int x, int y, int w, int h, boolean angehaekt)
            throws com.sun.star.uno.Exception {
        Object model = xMSF.createInstance("com.sun.star.awt.UnoControlCheckBoxModel");
        XPropertySet props = Lo.qi(XPropertySet.class, model);
        props.setPropertyValue("Label", label);
        props.setPropertyValue("PositionX", Integer.valueOf(x));
        props.setPropertyValue("PositionY", Integer.valueOf(y));
        props.setPropertyValue("Width", Integer.valueOf(w));
        props.setPropertyValue("Height", Integer.valueOf(h));
        props.setPropertyValue("State", (short) (angehaekt ? 1 : 0));
        cont.insertByName(name, model);
    }

    private void fuegeSpinner(XMultiServiceFactory xMSF, XNameContainer cont,
            String name, int x, int y, int w, int h, int wert, int min, int max)
            throws com.sun.star.uno.Exception {
        Object model = xMSF.createInstance("com.sun.star.awt.UnoControlNumericFieldModel");
        XPropertySet props = Lo.qi(XPropertySet.class, model);
        props.setPropertyValue("PositionX", Integer.valueOf(x));
        props.setPropertyValue("PositionY", Integer.valueOf(y));
        props.setPropertyValue("Width", Integer.valueOf(w));
        props.setPropertyValue("Height", Integer.valueOf(h));
        props.setPropertyValue("Value", (double) wert);
        props.setPropertyValue("ValueMin", (double) min);
        props.setPropertyValue("ValueMax", (double) max);
        props.setPropertyValue("Increment", 1.0);
        props.setPropertyValue("DecimalAccuracy", (short) 0);
        props.setPropertyValue("Spin", Boolean.TRUE);
        cont.insertByName(name, model);
    }

    private void fuegeButton(XMultiServiceFactory xMSF, XNameContainer cont,
            String name, String label, int x, int y, int w, int h) throws com.sun.star.uno.Exception {
        Object model = xMSF.createInstance("com.sun.star.awt.UnoControlButtonModel");
        XPropertySet props = Lo.qi(XPropertySet.class, model);
        props.setPropertyValue("Label", label);
        props.setPropertyValue("PositionX", Integer.valueOf(x));
        props.setPropertyValue("PositionY", Integer.valueOf(y));
        props.setPropertyValue("Width", Integer.valueOf(w));
        props.setPropertyValue("Height", Integer.valueOf(h));
        cont.insertByName(name, model);
    }

    private void fuegeTrennlinie(XMultiServiceFactory xMSF, XNameContainer cont,
            String name, int x, int y, int w, int h) throws com.sun.star.uno.Exception {
        Object model = xMSF.createInstance("com.sun.star.awt.UnoControlFixedLineModel");
        XPropertySet props = Lo.qi(XPropertySet.class, model);
        props.setPropertyValue("PositionX", Integer.valueOf(x));
        props.setPropertyValue("PositionY", Integer.valueOf(y));
        props.setPropertyValue("Width", Integer.valueOf(w));
        props.setPropertyValue("Height", Integer.valueOf(h));
        cont.insertByName(name, model);
    }
}
