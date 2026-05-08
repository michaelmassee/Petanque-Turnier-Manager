package de.petanqueturniermanager.spielerdb.ui;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.ItemEvent;
import com.sun.star.awt.PushButtonType;
import com.sun.star.awt.XActionListener;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XCheckBox;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XItemListener;
import com.sun.star.awt.XListBox;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XTextListener;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XNameContainer;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lang.XMultiServiceFactory;

import de.petanqueturniermanager.helper.Lo;

/**
 * Kapselt das Erzeugen und Aktualisieren von UNO-AWT-Controls für die
 * Spieler-DB-Dialoge. Bewusst kein generischer Layout-Container — die
 * Dialoge sind klein genug für absolute Positionierung.
 */
final class UnoControlsHelper {

    private static final Logger logger = LogManager.getLogger(UnoControlsHelper.class);

    private final XMultiServiceFactory xMSF;
    private final XNameContainer cont;
    private final XControlContainer xcc;

    UnoControlsHelper(XMultiServiceFactory xMSF, XNameContainer cont, XControlContainer xcc) {
        this.xMSF = xMSF;
        this.cont = cont;
        this.xcc = xcc;
    }

    /** Statisches Label. */
    void fixedText(String name, String label, int x, int y, int w, int h) throws com.sun.star.uno.Exception {
        var model = xMSF.createInstance("com.sun.star.awt.UnoControlFixedTextModel");
        var props = Lo.qi(XPropertySet.class, model);
        props.setPropertyValue("Label", label);
        props.setPropertyValue("PositionX", x);
        props.setPropertyValue("PositionY", y);
        props.setPropertyValue("Width", w);
        props.setPropertyValue("Height", h);
        cont.insertByName(name, model);
    }

    /** Einzeiliges Editfeld. */
    void edit(String name, String text, int x, int y, int w, int h) throws com.sun.star.uno.Exception {
        var model = xMSF.createInstance("com.sun.star.awt.UnoControlEditModel");
        var props = Lo.qi(XPropertySet.class, model);
        props.setPropertyValue("PositionX", x);
        props.setPropertyValue("PositionY", y);
        props.setPropertyValue("Width", w);
        props.setPropertyValue("Height", h);
        props.setPropertyValue("Text", text == null ? "" : text);
        props.setPropertyValue("MultiLine", Boolean.FALSE);
        cont.insertByName(name, model);
    }

    /** Edit-fähige ComboBox (Autocomplete-Style). */
    void comboBox(String name, String[] items, String selected, int x, int y, int w, int h)
            throws com.sun.star.uno.Exception {
        var model = xMSF.createInstance("com.sun.star.awt.UnoControlComboBoxModel");
        var props = Lo.qi(XPropertySet.class, model);
        props.setPropertyValue("PositionX", x);
        props.setPropertyValue("PositionY", y);
        props.setPropertyValue("Width", w);
        props.setPropertyValue("Height", h);
        props.setPropertyValue("StringItemList", items);
        props.setPropertyValue("Text", selected == null ? "" : selected);
        props.setPropertyValue("Dropdown", Boolean.TRUE);
        props.setPropertyValue("LineCount", (short) 12);
        props.setPropertyValue("Autocomplete", Boolean.TRUE);
        cont.insertByName(name, model);
    }

    /** ListBox (single-select, ohne Mehrfachauswahl). */
    void listBox(String name, int x, int y, int w, int h) throws com.sun.star.uno.Exception {
        var model = xMSF.createInstance("com.sun.star.awt.UnoControlListBoxModel");
        var props = Lo.qi(XPropertySet.class, model);
        props.setPropertyValue("PositionX", x);
        props.setPropertyValue("PositionY", y);
        props.setPropertyValue("Width", w);
        props.setPropertyValue("Height", h);
        props.setPropertyValue("Dropdown", Boolean.FALSE);
        props.setPropertyValue("MultiSelection", Boolean.FALSE);
        cont.insertByName(name, model);
    }

    /** Multi-Select-ListBox (Mehrfachauswahl per Strg/Shift). */
    void multiSelectListBox(String name, String[] items, int x, int y, int w, int h)
            throws com.sun.star.uno.Exception {
        var model = xMSF.createInstance("com.sun.star.awt.UnoControlListBoxModel");
        var props = Lo.qi(XPropertySet.class, model);
        props.setPropertyValue("PositionX", x);
        props.setPropertyValue("PositionY", y);
        props.setPropertyValue("Width", w);
        props.setPropertyValue("Height", h);
        props.setPropertyValue("Dropdown", Boolean.FALSE);
        props.setPropertyValue("MultiSelection", Boolean.TRUE);
        props.setPropertyValue("StringItemList", items);
        cont.insertByName(name, model);
    }

    /** Setzt die ausgewählten Indizes einer Multi-Select-ListBox. */
    void setzeAusgewaehlteIndizes(String name, short[] indizes) {
        XControl ctrl = xcc.getControl(name);
        if (ctrl == null) {
            return;
        }
        try {
            Lo.qi(XPropertySet.class, ctrl.getModel())
                    .setPropertyValue("SelectedItems", indizes);
        } catch (com.sun.star.uno.Exception e) {
            logger.warn("SelectedItems konnte nicht gesetzt werden für '{}': {}", name, e.getMessage());
        }
    }

    /** Liefert die aktuell ausgewählten Indizes einer Multi-Select-ListBox. */
    short[] ausgewaehlteIndizes(String name) {
        XListBox lb = listBox(name);
        if (lb == null) {
            return new short[0];
        }
        return lb.getSelectedItemsPos();
    }

    /** Dropdown-ListBox (single-select, nicht-editierbar) für Filter-Auswahlen. */
    void dropdownListBox(String name, String[] items, int x, int y, int w, int h)
            throws com.sun.star.uno.Exception {
        var model = xMSF.createInstance("com.sun.star.awt.UnoControlListBoxModel");
        var props = Lo.qi(XPropertySet.class, model);
        props.setPropertyValue("PositionX", x);
        props.setPropertyValue("PositionY", y);
        props.setPropertyValue("Width", w);
        props.setPropertyValue("Height", h);
        props.setPropertyValue("Dropdown", Boolean.TRUE);
        props.setPropertyValue("MultiSelection", Boolean.FALSE);
        props.setPropertyValue("StringItemList", items);
        if (items.length > 0) {
            props.setPropertyValue("SelectedItems", new short[] { 0 });
        }
        cont.insertByName(name, model);
    }

    /** Checkbox mit Initial-Zustand. */
    void checkBox(String name, String label, int x, int y, int w, int h, boolean angekreuzt)
            throws com.sun.star.uno.Exception {
        var model = xMSF.createInstance("com.sun.star.awt.UnoControlCheckBoxModel");
        var props = Lo.qi(XPropertySet.class, model);
        props.setPropertyValue("Label", label);
        props.setPropertyValue("PositionX", x);
        props.setPropertyValue("PositionY", y);
        props.setPropertyValue("Width", w);
        props.setPropertyValue("Height", h);
        props.setPropertyValue("State", (short) (angekreuzt ? 1 : 0));
        cont.insertByName(name, model);
    }

    /** {@code true} wenn die Checkbox angekreuzt ist (State == 1). */
    boolean istAngekreuzt(String name) {
        XControl ctrl = xcc.getControl(name);
        if (ctrl == null) {
            return false;
        }
        XCheckBox cb = Lo.qi(XCheckBox.class, ctrl);
        return cb != null && cb.getState() == 1;
    }

    /** Reagiert auf jeden Wechsel des Häkchen-Zustands. */
    void registriereCheckBoxListener(String name, Runnable aktion) {
        XControl ctrl = xcc.getControl(name);
        if (ctrl == null) {
            return;
        }
        XCheckBox cb = Lo.qi(XCheckBox.class, ctrl);
        if (cb == null) {
            return;
        }
        cb.addItemListener(new XItemListener() {
            @Override public void itemStateChanged(ItemEvent e) { aktion.run(); }
            @Override public void disposing(EventObject e) { /* kein Aufräumen nötig */ }
        });
    }

    void button(String name, String label, int x, int y, int w, int h) throws com.sun.star.uno.Exception {
        button(name, label, x, y, w, h, (short) PushButtonType.STANDARD_value);
    }

    void button(String name, String label, int x, int y, int w, int h, short pushButtonType)
            throws com.sun.star.uno.Exception {
        var model = xMSF.createInstance("com.sun.star.awt.UnoControlButtonModel");
        var props = Lo.qi(XPropertySet.class, model);
        props.setPropertyValue("Label", label);
        props.setPropertyValue("PositionX", x);
        props.setPropertyValue("PositionY", y);
        props.setPropertyValue("Width", w);
        props.setPropertyValue("Height", h);
        props.setPropertyValue("PushButtonType", pushButtonType);
        cont.insertByName(name, model);
    }

    /** Liest Text aus einem Edit-/ComboBox-Control. */
    String leseText(String name) {
        XControl ctrl = xcc.getControl(name);
        if (ctrl == null) {
            return "";
        }
        XTextComponent tc = Lo.qi(XTextComponent.class, ctrl);
        return tc == null ? "" : tc.getText();
    }

    /** Setzt Text in ein Edit-/ComboBox-Control. */
    void setzeText(String name, @Nullable String text) {
        XControl ctrl = xcc.getControl(name);
        if (ctrl == null) {
            return;
        }
        XTextComponent tc = Lo.qi(XTextComponent.class, ctrl);
        if (tc != null) {
            tc.setText(text == null ? "" : text);
        }
    }

    /** Aktiviert/deaktiviert ein Control. */
    void enabled(String name, boolean aktiv) {
        XControl ctrl = xcc.getControl(name);
        if (ctrl == null) {
            return;
        }
        try {
            Lo.qi(XPropertySet.class, ctrl.getModel()).setPropertyValue("Enabled", aktiv);
        } catch (com.sun.star.uno.Exception e) {
            logger.warn("Enabled konnte nicht gesetzt werden für '{}': {}", name, e.getMessage());
        }
    }

    void label(String name, String label) {
        XControl ctrl = xcc.getControl(name);
        if (ctrl == null) {
            return;
        }
        try {
            Lo.qi(XPropertySet.class, ctrl.getModel()).setPropertyValue("Label", label);
        } catch (com.sun.star.uno.Exception e) {
            logger.warn("Label konnte nicht gesetzt werden für '{}': {}", name, e.getMessage());
        }
    }

    /**
     * Setzt die Schriftfarbe eines Controls. {@code rgb < 0} (z.&nbsp;B. {@code -1})
     * setzt die UNO-Property auf „automatisch", was den Default-Schwarz wiederherstellt.
     */
    void labelTextColor(String name, int rgb) {
        XControl ctrl = xcc.getControl(name);
        if (ctrl == null) {
            return;
        }
        try {
            Lo.qi(XPropertySet.class, ctrl.getModel()).setPropertyValue("TextColor", rgb);
        } catch (com.sun.star.uno.Exception e) {
            logger.warn("TextColor konnte nicht gesetzt werden für '{}': {}", name, e.getMessage());
        }
    }

    void registriereActionListener(String name, Runnable aktion) {
        XControl ctrl = xcc.getControl(name);
        if (ctrl == null) {
            return;
        }
        XButton btn = Lo.qi(XButton.class, ctrl);
        if (btn == null) {
            return;
        }
        btn.addActionListener(new XActionListener() {
            @Override public void actionPerformed(ActionEvent e) { aktion.run(); }
            @Override public void disposing(EventObject e) { /* kein Aufräumen nötig */ }
        });
    }

    /** Live-Filter-Hook. */
    void registriereTextListener(String name, Runnable aktion) {
        XControl ctrl = xcc.getControl(name);
        if (ctrl == null) {
            return;
        }
        XTextComponent tc = Lo.qi(XTextComponent.class, ctrl);
        if (tc == null) {
            return;
        }
        tc.addTextListener(new XTextListener() {
            @Override public void textChanged(com.sun.star.awt.TextEvent e) { aktion.run(); }
            @Override public void disposing(EventObject e) { /* kein Aufräumen nötig */ }
        });
    }

    /** ListBox-Selektion: Single-Click und Doppelklick. */
    void registriereListBoxAuswahl(String name, Runnable aufAuswahl, Runnable aufDoppelklick) {
        XControl ctrl = xcc.getControl(name);
        if (ctrl == null) {
            return;
        }
        XListBox lb = Lo.qi(XListBox.class, ctrl);
        if (lb == null) {
            return;
        }
        lb.addItemListener(new XItemListener() {
            @Override public void itemStateChanged(ItemEvent e) { aufAuswahl.run(); }
            @Override public void disposing(EventObject e) { /* kein Aufräumen nötig */ }
        });
        lb.addActionListener(new XActionListener() {
            @Override public void actionPerformed(ActionEvent e) { aufDoppelklick.run(); }
            @Override public void disposing(EventObject e) { /* kein Aufräumen nötig */ }
        });
    }

    @Nullable XListBox listBox(String name) {
        XControl ctrl = xcc.getControl(name);
        return ctrl == null ? null : Lo.qi(XListBox.class, ctrl);
    }

    /** Setzt den Items einer ListBox neu (alte werden entfernt). */
    void setzeListItems(String name, String[] items) {
        XListBox lb = listBox(name);
        if (lb == null) {
            return;
        }
        short anz = lb.getItemCount();
        if (anz > 0) {
            lb.removeItems((short) 0, anz);
        }
        if (items.length > 0) {
            lb.addItems(items, (short) 0);
        }
    }

    /** Selektierten Index oder -1. */
    short ausgewaehlterIndex(String name) {
        XListBox lb = listBox(name);
        if (lb == null) {
            return -1;
        }
        return lb.getSelectedItemPos();
    }

    void entferneControl(String name) {
        try {
            cont.removeByName(name);
        } catch (NoSuchElementException | WrappedTargetException e) {
            // Control existiert nicht oder kann nicht entfernt werden – ignorieren
        }
    }
}
