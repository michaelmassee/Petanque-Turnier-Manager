/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.XCallback;
import com.sun.star.awt.XRequestCallback;
import com.sun.star.uno.XComponentContext;

/**
 * Postet {@link Runnable}s via {@code com.sun.star.awt.AsyncCallback} auf den LO-Main-Thread.
 * <p>
 * Einsatz: UI-Operationen, die nicht <em>re-entrant</em> im aktuellen LO-Aufruf laufen dürfen.
 * Konkreter Anlass: Toolbar-Aufbau ({@code showElement}/{@code requestElement}) darf nicht
 * synchron innerhalb eines laufenden LO-{@code FillToolbar()} angestoßen werden – das erzeugt
 * unter Windows beim Calc-Start schwarze Icon-Flächen. Das Posten stellt sicher, dass die
 * Aktion erst <em>nach</em> dem aktuellen Main-Thread-Aufruf ausgeführt wird. Reihenfolge ist FIFO.
 * <p>
 * Fallback: Ist der {@code AsyncCallback}-Service nicht verfügbar, wird {@code r} synchron
 * ausgeführt, damit die Funktion nicht stillschweigend verloren geht.
 */
public final class LoMainThread {

    private static final Logger logger = LogManager.getLogger(LoMainThread.class);

    private LoMainThread() {
    }

    public static void post(XComponentContext xContext, Runnable r) {
        try {
            var serviceManager = xContext.getServiceManager();
            if (serviceManager == null) {
                logger.debug("ServiceManager nicht verfügbar – führe direkt aus");
                r.run();
                return;
            }
            var asyncCallback = serviceManager.createInstanceWithContext("com.sun.star.awt.AsyncCallback", xContext);
            var dispatcher = Lo.qi(XRequestCallback.class, asyncCallback);
            if (dispatcher == null) {
                logger.debug("AsyncCallback nicht verfügbar – führe direkt aus");
                r.run();
                return;
            }
            dispatcher.addCallback((XCallback) data -> {
                try {
                    r.run();
                } catch (RuntimeException e) {
                    logger.warn("Main-Thread-Aktion fehlgeschlagen", e);
                }
            }, null);
        } catch (Exception e) {
            logger.warn("AsyncCallback-Post fehlgeschlagen – führe direkt aus", e);
            r.run();
        }
    }
}
