/*
 * Erstellung 17.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.comp.turnierevent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Michael Massee
 *
 */
public class TurnierEventHandler {

	private static final Logger logger = LogManager.getLogger(TurnierEventHandler.class);

	private final List<ITurnierEventListener> listeners;

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

	/**
	 * @param eventObj
	 */
	private void onPropertiesChanged(ITurnierEvent eventObj) {
		long startNs = System.nanoTime();
		int anzahl;
		synchronized (listeners) {
			anzahl = listeners.size();
			for (ITurnierEventListener listner : listeners) {
				listner.onPropertiesChanged(eventObj);
			}
		}
		long dauerMs = (System.nanoTime() - startNs) / 1_000_000L;
		logger.info("[WORKER-TIMING] TurnierEventHandler.trigger PropertiesChanged: {} ms, listener={}, thread={}",
				dauerMs, anzahl, Thread.currentThread().getName());
	}

	public void addTurnierEventListener(ITurnierEventListener listner) {
		listeners.add(listner);
	}

	public void removeTurnierEventListener(ITurnierEventListener listner) {
		listeners.remove(listner);
	}

	public void disposing() {
		listeners.clear();
	}
}
