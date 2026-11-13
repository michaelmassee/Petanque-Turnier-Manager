/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.sidebar.layout;

import com.sun.star.awt.PosSize;
import com.sun.star.awt.Rectangle;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XWindow;

import de.petanqueturniermanager.helper.Lo;

/**
 * Wie {@link ControlLayout} (statische Höhe des Controls), lässt sich aber ausblenden: Ausgeblendet belegt die Zeile
 * keine Höhe und das Control ist unsichtbar. Nach {@link #setSichtbar} muss der Aufrufer das Layout neu anfordern,
 * wenn sich die Sichtbarkeit geändert hat.
 *
 * @author Michael Massee
 */
public class AusblendbaresControlLayout implements Layout {

    private final XWindow control;
    private final int hoehe;
    private volatile boolean sichtbar;

    public AusblendbaresControlLayout(XControl control, boolean sichtbar) {
        this.control = Lo.qi(XWindow.class, control);
        this.hoehe = this.control.getPosSize().Height;
        this.sichtbar = sichtbar;
    }

    /**
     * Merkt die gewünschte Sichtbarkeit vor; wirksam beim nächsten {@link #layout}.
     *
     * @return ob sich die Sichtbarkeit geändert hat (dann Layout neu anfordern)
     */
    public boolean setSichtbar(boolean neu) {
        boolean geaendert = sichtbar != neu;
        sichtbar = neu;
        return geaendert;
    }

    @Override
    public int layout(Rectangle rect) {
        control.setVisible(sichtbar);
        if (!sichtbar) {
            return 0;
        }
        control.setPosSize(rect.X, rect.Y, rect.Width, hoehe, PosSize.POSSIZE);
        return hoehe;
    }

    /**
     * Diese Operation ist nicht erlaubt.
     */
    @Override
    public void addLayout(Layout layout, int weight) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getHeight() {
        return sichtbar ? hoehe : 0;
    }
}
