package de.petanqueturniermanager.timer;

import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.FontDescriptor;
import com.sun.star.awt.InvalidateStyle;
import com.sun.star.awt.MouseEvent;
import com.sun.star.awt.Point;
import com.sun.star.awt.Rectangle;
import com.sun.star.awt.XGraphics;
import com.sun.star.awt.XRequestCallback;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.beans.PropertyValue;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.frame.FeatureStateEvent;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.XEventListener;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.lib.uno.helper.WeakBase;
import com.sun.star.uno.XComponentContext;

/**
 * UNO-Statusleisten-Controller für den Timer.
 * <p>
 * Zeigt die verbleibende Zeit ({@code MM:SS}) in der Calc-Statusleiste an.
 * Schriftfarbe wechselt je nach {@link TimerZustand}:
 * <ul>
 *   <li>LAEUFT   → Grün   (0x00CC44)</li>
 *   <li>PAUSIERT → Gelb   (0xEECC00)</li>
 *   <li>BEENDET  → Rot    (0xFF3333)</li>
 *   <li>INAKTIV  → Grau   (0x888888)</li>
 * </ul>
 * Registriert sich als {@link TimerListener} beim {@link TimerManager}
 * und fordert bei jedem Tick eine Neuzeichnung der Statusleiste an.
 */
public class TimerStatusbarController extends WeakBase
        implements com.sun.star.frame.XStatusbarController, XServiceInfo, TimerListener {

    private static final Logger logger = LogManager.getLogger(TimerStatusbarController.class);

    private static final String IMPLEMENTATION_NAME = TimerStatusbarController.class.getName();
    private static final String[] SERVICE_NAMES     =
            { "com.sun.star.frame.StatusbarController" };

    private static final int FARBE_LAEUFT   = 0x00CC44;
    private static final int FARBE_PAUSIERT = 0xEECC00;
    private static final int FARBE_BEENDET  = 0xFF3333;
    private static final int FARBE_INAKTIV  = 0x888888;
    private static final float SCHRIFT_GROESSE = 9.0f;

    private final CopyOnWriteArrayList<XEventListener> disposeListeners = new CopyOnWriteArrayList<>();

    private final XComponentContext xContext;
    private XWindowPeer parentWindowPeer;
    private XRequestCallback asyncCallback;
    private final AtomicBoolean repaintPending = new AtomicBoolean(false);
    private volatile TimerState aktuellerZustand = TimerState.inaktiv();

    public TimerStatusbarController(XComponentContext xContext) {
        this.xContext = xContext;
    }

    // ── XInitialization ───────────────────────────────────────────────────────

    @Override
    public void initialize(Object[] args) throws com.sun.star.uno.Exception {
        // args[0] = Frame, args[1] = XStatusbarItem, args[2] = XWindow (parentWindow)
        for (Object arg : args) {
            var fenster = UnoRuntime.queryInterface(XWindow.class, arg);
            if (fenster != null) {
                parentWindowPeer = UnoRuntime.queryInterface(XWindowPeer.class, fenster);
                break;
            }
        }
        try {
            asyncCallback = UnoRuntime.queryInterface(
                    XRequestCallback.class,
                    xContext.getServiceManager().createInstanceWithContext(
                            "com.sun.star.awt.AsyncCallback", xContext));
        } catch (Exception e) {
            logger.debug("AsyncCallback konnte nicht erstellt werden – Statusbar läuft unsynchronisiert", e);
        }
        if (asyncCallback == null) {
            logger.debug("AsyncCallback nicht verfügbar – Statusbar läuft unsynchronisiert");
        }
        try {
            TimerManager.get().addListener(this);
        } catch (IllegalStateException e) {
            logger.warn("TimerManager noch nicht initialisiert beim Statusbar-Controller-Init", e);
        }
    }

    // ── TimerListener ─────────────────────────────────────────────────────────

    @Override
    public void onChange(TimerState state) {
        aktuellerZustand = state;
        if (parentWindowPeer == null) return;
        var cb = asyncCallback;
        if (cb != null && repaintPending.compareAndSet(false, true)) {
            cb.addCallback(aData -> {
                repaintPending.set(false);
                var p = parentWindowPeer;
                if (p == null) return;
                try {
                    p.invalidate((short) InvalidateStyle.NOERASE);
                } catch (Exception e) {
                    logger.debug("invalidate() fehlgeschlagen (vermutlich disposed)", e);
                }
            }, null);
        }
        // kein Fallback – ohne AsyncCallback kein invalidate() vom falschen Thread
    }

    // ── XStatusbarController – Zeichnung ─────────────────────────────────────

    @Override
    public void paint(XGraphics xGraphics, Rectangle rechteck, int nState) {
        if (xGraphics == null) {
            return;
        }
        var state = aktuellerZustand;
        xGraphics.push();
        try {
            xGraphics.setFillColor(0x1C1C1C);
            xGraphics.drawRect(rechteck.X, rechteck.Y, rechteck.Width, rechteck.Height);

            xGraphics.setTextColor(farbeVonZustand(state.zustand()));
            xGraphics.setTextFillColor(0x1C1C1C);

            var schrift = new FontDescriptor();
            schrift.Name = "Courier New";
            schrift.Height = (short) Math.round(SCHRIFT_GROESSE);
            schrift.Weight = com.sun.star.awt.FontWeight.BOLD;
            xGraphics.selectFont(schrift);

            int textX = rechteck.X + 4;
            int textY = rechteck.Y + rechteck.Height - 3;
            xGraphics.drawText(textX, textY, state.anzeige());
        } finally {
            xGraphics.pop();
        }
    }

    private static int farbeVonZustand(TimerZustand zustand) {
        return switch (zustand) {
            case LAEUFT   -> FARBE_LAEUFT;
            case PAUSIERT -> FARBE_PAUSIERT;
            case BEENDET  -> FARBE_BEENDET;
            case INAKTIV  -> FARBE_INAKTIV;
        };
    }

    // ── XStatusbarController – Interaktion (nicht verwendet) ──────────────────

    @Override
    public boolean mouseButtonDown(MouseEvent e) {
        return false;
    }

    @Override
    public boolean mouseMove(MouseEvent e) {
        return false;
    }

    @Override
    public boolean mouseButtonUp(MouseEvent e) {
        return false;
    }

    @Override
    public void command(Point aPos, int nCommand, boolean bMouseEvent, Object aData) {
        // keine Aktion
    }

    @Override
    public void click(Point aPos) {
        // keine Aktion
    }

    @Override
    public void doubleClick(Point aPos) {
        // keine Aktion
    }

    // ── XStatusListener ───────────────────────────────────────────────────────

    @Override
    public void statusChanged(FeatureStateEvent aEvent) {
        // keine Aktion – Zustand kommt vom TimerListener
    }

    // ── XUpdatable ────────────────────────────────────────────────────────────

    @Override
    public void update() {
        try {
            aktuellerZustand = TimerManager.get().getAktuellerZustand();
        } catch (IllegalStateException e) {
            aktuellerZustand = TimerState.inaktiv();
        }
        if (parentWindowPeer != null) {
            parentWindowPeer.invalidate((short) InvalidateStyle.NOERASE);
        }
    }

    // ── XComponent ────────────────────────────────────────────────────────────

    @Override
    public void dispose() {
        try {
            TimerManager.get().removeListener(this);
        } catch (IllegalStateException e) {
            // TimerManager bereits disposed – ignorieren
        }
        var event = new EventObject(this);
        disposeListeners.forEach(l -> l.disposing(event));
        disposeListeners.clear();
        asyncCallback = null;
        parentWindowPeer = null;
        repaintPending.set(false);
    }

    @Override
    public void addEventListener(XEventListener xListener) {
        if (xListener != null) {
            disposeListeners.add(xListener);
        }
    }

    @Override
    public void removeEventListener(XEventListener xListener) {
        disposeListeners.remove(xListener);
    }

    // ── XEventListener (von XStatusListener geerbt) ───────────────────────────

    @Override
    public void disposing(EventObject aEvent) {
        dispose();
    }

    // ── XServiceInfo ──────────────────────────────────────────────────────────

    @Override
    public String getImplementationName() {
        return IMPLEMENTATION_NAME;
    }

    @Override
    public boolean supportsService(String serviceName) {
        return Arrays.asList(SERVICE_NAMES).contains(serviceName);
    }

    @Override
    public String[] getSupportedServiceNames() {
        return SERVICE_NAMES;
    }

}
