/*
 * Erstellung 17.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.comp.turnierevent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.XCallback;
import com.sun.star.awt.XRequestCallback;
import com.sun.star.lang.DisposedException;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.comp.PetanqueTurnierMngrSingleton;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.perflog.PerfLog;

/**
 * Broadcastet TurnierEvents an registrierte {@link ITurnierEventListener}.
 * <p>
 * Versand-Modell:
 * <ul>
 *   <li>Während ein {@link SheetRunner} läuft, werden eingehende Events
 *       <em>koalesziert</em>: nur der letzte Snapshot wird gespeichert,
 *       der eigentliche Broadcast erfolgt erst beim {@link #flushPending()}
 *       aus dem {@code SheetRunner}-{@code finally}-Block (nach
 *       {@code unlockControllers}).</li>
 *   <li>Außerhalb eines {@code SheetRunner}-Laufs wird der Broadcast über
 *       {@code com.sun.star.awt.AsyncCallback} auf den LO-Main-Thread
 *       gepostet, damit Listener (Sidebar, Toolbar) ihre UI-Mutationen
 *       nicht auf einem Fremd-Thread ausführen.</li>
 *   <li>Falls der AsyncCallback-Service oder der UNO-Context noch nicht
 *       verfügbar sind (z.B. in Headless-Tests), wird synchron auf dem
 *       Caller-Thread iteriert.</li>
 * </ul>
 */
public class TurnierEventHandler {

	private static final Logger logger = LogManager.getLogger(TurnierEventHandler.class);

	private final List<ITurnierEventListener> listeners;
	private final AtomicReference<ITurnierEvent> pending = new AtomicReference<>();
	private final AtomicInteger coalescedSeitFlush = new AtomicInteger();
	private volatile XRequestCallback dispatcher;

	public TurnierEventHandler() {
		listeners = Collections.synchronizedList(new ArrayList<>());
	}

	public void trigger(TurnierEventType type, ITurnierEvent eventObj) {
		switch (type) {
		case PropertiesChanged:
			onPropertiesChanged(eventObj);
			break;
		default:
			break;
		}
	}

	private void onPropertiesChanged(ITurnierEvent eventObj) {
		if (SheetRunner.isRunning()) {
			pending.set(eventObj);
			coalescedSeitFlush.incrementAndGet();
			return;
		}
		dispatch(eventObj, "trigger");
	}

	/**
	 * Feuert einen ggf. während eines {@link SheetRunner}-Laufs koaleszierten
	 * Event. Wird aus {@code SheetRunner.run()} {@code finally}-Block aufgerufen,
	 * nachdem der ControllerLock freigegeben wurde.
	 */
	public void flushPending() {
		ITurnierEvent ev = pending.getAndSet(null);
		int coalesced = coalescedSeitFlush.getAndSet(0);
		if (ev != null) {
			PerfLog.log(logger, "[WORKER-TIMING] TurnierEventHandler.flushPending koalesziert={}",
					coalesced);
			dispatch(ev, "flushPending");
		}
	}

	private void dispatch(ITurnierEvent eventObj, String quelle) {
		XRequestCallback d = getDispatcher();
		if (d == null) {
			broadcastSync(eventObj, quelle, true);
			return;
		}
		try {
			d.addCallback((XCallback) data -> broadcastSync(eventObj, quelle, false), null);
		} catch (RuntimeException e) {
			logger.debug("AsyncCallback-Post fehlgeschlagen – synchroner Fallback", e);
			broadcastSync(eventObj, quelle, true);
		}
	}

	private void broadcastSync(ITurnierEvent eventObj, String quelle, boolean synchronerPfad) {
		long startNs = System.nanoTime();
		int anzahl;
		String eventDoc = eventObj == null ? "null"
				: de.petanqueturniermanager.comp.ProtocolHandler.beschreibeDokument(
						eventObj.getWorkingSpreadsheetDocument());
		synchronized (listeners) {
			anzahl = listeners.size();
			logger.info("[FOKUS-TRACE] TurnierEvent broadcast START quelle={} pfad={} eventDoc={} listeners={} thread={}",
					quelle, synchronerPfad ? "sync" : "main", eventDoc, anzahl,
					Thread.currentThread().getName());
			for (ITurnierEventListener listner : listeners) {
				try {
					listner.onPropertiesChanged(eventObj);
				} catch (RuntimeException e) {
					logger.warn("Listener-Aufruf fehlgeschlagen", e);
				}
			}
		}
		long dauerMs = (System.nanoTime() - startNs) / 1_000_000L;
		logger.info("[FOKUS-TRACE] TurnierEvent broadcast ENDE quelle={} dauerMs={} listener={}",
				quelle, dauerMs, anzahl);
		PerfLog.log(logger, "[WORKER-TIMING] TurnierEventHandler broadcast PropertiesChanged quelle={} pfad={} : {} ms, listener={}, thread={}",
				quelle, synchronerPfad ? "sync" : "main", dauerMs, anzahl,
				Thread.currentThread().getName());
	}

	private XRequestCallback getDispatcher() {
		XRequestCallback d = dispatcher;
		if (d != null) {
			return d;
		}
		XComponentContext ctx = PetanqueTurnierMngrSingleton.getContext();
		if (ctx == null) {
			return null;
		}
		synchronized (this) {
			if (dispatcher != null) {
				return dispatcher;
			}
			try {
				Object asyncCallback = ctx.getServiceManager()
						.createInstanceWithContext("com.sun.star.awt.AsyncCallback", ctx);
				dispatcher = Lo.qi(XRequestCallback.class, asyncCallback);
			} catch (com.sun.star.uno.Exception e) {
				logger.warn("AsyncCallback-Service nicht verfügbar – Fallback synchron", e);
			} catch (DisposedException e) {
				// LO-Bridge tot (z.B. nach Office-Shutdown zwischen UI-Test-Klassen).
				// Synchroner Fallback. sharedContext bleibt stale; das ist Aufruferproblem.
				logger.debug("LO-Bridge disposed – Fallback synchron");
			}
			return dispatcher;
		}
	}

	public void addTurnierEventListener(ITurnierEventListener listner) {
		listeners.add(listner);
	}

	public void removeTurnierEventListener(ITurnierEventListener listner) {
		listeners.remove(listner);
	}

	public void disposing() {
		listeners.clear();
		pending.set(null);
		coalescedSeitFlush.set(0);
		dispatcher = null;
	}
}
