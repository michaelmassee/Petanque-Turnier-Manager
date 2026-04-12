/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.kaskade.meldeliste;

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
 * Kaskaden-KO-Meldeliste.
 * <p>
 * Abgefragt werden:
 * <ul>
 *   <li>Formation: Tête / Doublette / Triplette (Radio-Buttons)</li>
 *   <li>Teamname anzeigen: Ja / Nein (Checkbox)</li>
 *   <li>Vereinsname anzeigen: Ja / Nein (Checkbox)</li>
 *   <li>Anzahl Kaskaden: 2 (A/B/C/D) oder 3 (A/B/C/D/E/F/G/H) (Radio-Buttons)</li>
 * </ul>
 */
class KaskadeTurnierParameterDialog {

    record TurnierParameter(
            Formation formation,
            boolean teamnameAnzeigen,
            boolean vereinsnameAnzeigen,
            int anzahlKaskaden) {
    }

    private final WorkingSpreadsheet workingSpreadsheet;
    private volatile boolean okGedrueckt = false;

    private KaskadeTurnierParameterDialog(WorkingSpreadsheet workingSpreadsheet) {
        this.workingSpreadsheet = workingSpreadsheet;
    }

    static KaskadeTurnierParameterDialog from(WorkingSpreadsheet workingSpreadsheet) {
        return new KaskadeTurnierParameterDialog(workingSpreadsheet);
    }

    Optional<TurnierParameter> anzeigen(Formation standardFormation, boolean standardTeamnameAnzeigen,
            boolean standardVereinsnameAnzeigen, int standardAnzahlKaskaden) throws com.sun.star.uno.Exception {

        ProcessBox.from().hide();

        XComponentContext context = workingSpreadsheet.getxContext();
        XMultiComponentFactory xMCF = context.getServiceManager();

        Object dialogModel = xMCF.createInstanceWithContext("com.sun.star.awt.UnoControlDialogModel", context);
        XPropertySet dlgProps = Lo.qi(XPropertySet.class, dialogModel);
        dlgProps.setPropertyValue("PositionX", Integer.valueOf(50));
        dlgProps.setPropertyValue("PositionY", Integer.valueOf(50));
        dlgProps.setPropertyValue("Width", Integer.valueOf(160));
        dlgProps.setPropertyValue("Height", Integer.valueOf(175));
        dlgProps.setPropertyValue("Title", I18n.get("dialog.kaskade.turnier.parameter.titel"));
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

        fuegeLabel(xMSF, cont, "lblKaskaden", I18n.get("dialog.kaskade.label.anzahl.kaskaden"), 8, 99, 140, 10);
        fuegeRadioButton(xMSF, cont, "radioKaskaden2",
                I18n.get("dialog.kaskade.label.kaskaden.2"), 8, 113, 140, 10, standardAnzahlKaskaden == 2);
        fuegeRadioButton(xMSF, cont, "radioKaskaden3",
                I18n.get("dialog.kaskade.label.kaskaden.3"), 8, 125, 140, 10, standardAnzahlKaskaden == 3);

        fuegeTrennlinie(xMSF, cont, "sep3", 5, 139, 150, 2);

        fuegeButton(xMSF, cont, "btnOk", I18n.get("dialog.button.ok"), 22, 153, 50, 14);
        fuegeButton(xMSF, cont, "btnCancel", I18n.get("dialog.button.abbrechen"), 88, 153, 60, 14);

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
            int anzahlKaskaden = leseAnzahlKaskaden(xcc);
            ergebnis = Optional.of(new TurnierParameter(formation, teamnameAnzeigen, vereinsnameAnzeigen, anzahlKaskaden));
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

    private int leseAnzahlKaskaden(XControlContainer xcc) {
        return istRadioGewaehlt(xcc, "radioKaskaden3") ? 3 : 2;
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
