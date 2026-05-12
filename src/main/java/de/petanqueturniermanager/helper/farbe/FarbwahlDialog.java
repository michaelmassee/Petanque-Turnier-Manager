package de.petanqueturniermanager.helper.farbe;

import java.util.OptionalInt;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.XPropertyAccess;
import com.sun.star.ui.dialogs.XExecutableDialog;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.helper.Lo;

/**
 * Zentraler Helper für die Farbwahl. Verwendet den nativen LO-Color-Picker
 * ({@code com.sun.star.cui.ColorPicker}) — denselben Dialog, den LO intern überall
 * benutzt (Zellfarben, Zeichenfarben usw.).
 *
 * <p>Damit entfallen alle Modal-/Z-Order-/Event-Loop-Probleme, die ein Swing-
 * {@code JColorChooser} innerhalb eines UNO-Modal-Dialogs verursacht hätte:
 * <ul>
 *   <li>echte Modality gegenüber dem Parent-UNO-Dialog (LO's weld-Modality),</li>
 *   <li>korrekter z-Order — kein „setzt sich hinter den Dialog",</li>
 *   <li>läuft im UNO-Event-Loop — LO bleibt responsiv,</li>
 *   <li>Optik konsistent mit dem Rest von LibreOffice.</li>
 * </ul>
 *
 * <p>Aufrufer-seitig wird nichts deaktiviert/synchronisiert — Modality und Re-Entry-Schutz
 * sind in LO's nativem Modal-Stack erledigt.
 *
 * <p><b>Bekannter LO-Bug (ab 26.2, tdf#172054):</b> Der Service ignoriert die per
 * {@code setPropertyValues("Color", …)} gesetzte Initialfarbe — der Dialog öffnet immer
 * mit Default (Schwarz). Regression durch Drop+Reintroduce in den Commits 5ba0ccb (Jun 2025)
 * und 9d6ade1 (Nov 2025), siehe
 * {@code vcl/source/components/ColorPicker.cxx::execute()} — dort fehlt ein
 * {@code aColorDialog.SetColor(m_aColor)} vor {@code Execute()}. Patch für upstream liegt
 * im Projekt unter {@code upstream/libreoffice/tdf172054-colorpicker-seed-initial-color.patch}.
 * Bugzilla: https://bugs.documentfoundation.org/show_bug.cgi?id=172054. Sobald der Fix in
 * einer freigegebenen LO-Version landet, kann dieser Hinweis entfernt werden — am Helper
 * selbst ist nichts zu ändern.
 */
public final class FarbwahlDialog {

    private static final Logger logger = LogManager.getLogger(FarbwahlDialog.class);
    private static final String SERVICE = "com.sun.star.cui.ColorPicker";
    private static final String PROP_COLOR = "Color";

    private FarbwahlDialog() {}

    /**
     * Standard-Aufruf von einem UNO-Dialog/Sidebar-Panel aus: erwartet den
     * {@link XWindowPeer} des Aufruferfensters als Parent. Der Peer ist genau das,
     * was LO intern erwartet — der UNO-Control-Wrapper alleine reicht nicht (siehe
     * {@code Application::GetFrameWeld(XWindow)}, das per {@code dynamic_cast} nur
     * {@code VCLXWindow} erkennt, was wiederum der Peer ist, nicht der Control-Wrapper).
     */
    public static OptionalInt waehle(XComponentContext xContext, XWindowPeer parentPeer,
            int aktuelleFarbeRgb) {
        XWindow parent = parentPeer != null ? Lo.qi(XWindow.class, parentPeer) : null;
        return waehle(xContext, parent, aktuelleFarbeRgb);
    }

    /**
     * Öffnet den nativen UNO-Color-Picker und blockiert, bis der Anwender OK oder Abbrechen klickt.
     * Während der Picker offen ist, ist das Parent-Fenster automatisch nicht klickbar.
     *
     * @param xContext         UNO-Kontext (Pflicht)
     * @param parent           Parent-Window (z. B. {@link XWindow} oder
     *                         {@link com.sun.star.awt.XWindowPeer} des aufrufenden
     *                         Dialogs); darf {@code null} sein für einen freistehenden Picker
     *                         (z. B. von der Sidebar aus aufgerufen).
     * @param aktuelleFarbeRgb aktuell ausgewählte Farbe als 0xRRGGBB
     * @return ausgewählte Farbe als 0xRRGGBB, oder {@code empty} bei Abbruch / Fehler
     */
    public static OptionalInt waehle(XComponentContext xContext, XWindow parent, int aktuelleFarbeRgb) {
        if (xContext == null) {
            return OptionalInt.empty();
        }
        try {
            var mcf = xContext.getServiceManager();
            Object[] args = parent != null ? new Object[] { parent } : new Object[0];
            Object pickerInst = mcf.createInstanceWithArgumentsAndContext(SERVICE, args, xContext);

            var pickerProps = Lo.qi(XPropertyAccess.class, pickerInst);
            var pvFarbe = new PropertyValue();
            pvFarbe.Name = PROP_COLOR;
            pvFarbe.Value = Integer.valueOf(aktuelleFarbeRgb & 0xFFFFFF);
            pickerProps.setPropertyValues(new PropertyValue[] { pvFarbe });

            var picker = Lo.qi(XExecutableDialog.class, pickerInst);
            if (picker.execute() != 1) {
                return OptionalInt.empty();
            }

            for (var pv : pickerProps.getPropertyValues()) {
                if (PROP_COLOR.equals(pv.Name) && pv.Value instanceof Integer i) {
                    return OptionalInt.of(i.intValue() & 0xFFFFFF);
                }
            }
            return OptionalInt.empty();
        } catch (com.sun.star.uno.Exception e) {
            logger.error("Fehler bei UNO-ColorPicker", e);
            return OptionalInt.empty();
        }
    }
}
