package de.petanqueturniermanager.sidebar.layout;

import com.sun.star.awt.PosSize;
import com.sun.star.awt.Rectangle;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XWindow;

import de.petanqueturniermanager.helper.Lo;

/**
 * Layout-Element, das die volle verbleibende Höhe des übergeordneten Containers verwendet.
 * <p>
 * Im Gegensatz zu {@link ControlLayout} (statische Höhe) passt sich dieses Layout
 * bei jedem {@code layout()}-Aufruf an die tatsächlich verfügbare Höhe an.
 * Geeignet als letztes Element in einem {@link VerticalLayout}, um den verbleibenden
 * Platz vollständig zu füllen (z.B. eine scrollbare ListBox in der Sidebar).
 *
 * @author Michael Massee
 */
public class FuellendeControlLayout implements Layout {

    private final XWindow control;
    private final int minHoehe;

    public FuellendeControlLayout(XControl control, int minHoehe) {
        this.control = Lo.qi(XWindow.class, control);
        this.minHoehe = minHoehe;
    }

    @Override
    public int layout(Rectangle rect) {
        int hoehe = Math.max(minHoehe, rect.Height - rect.Y);
        control.setPosSize(rect.X, rect.Y, rect.Width, hoehe, PosSize.POSSIZE);
        return hoehe;
    }

    @Override
    public void addLayout(Layout layout, int weight) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getHeight() {
        return minHoehe;
    }
}
