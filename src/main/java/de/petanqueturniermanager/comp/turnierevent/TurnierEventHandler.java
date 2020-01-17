/**
 * Erstellung 17.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.comp.turnierevent;

import java.util.ArrayList;

/**
 * @author Michael Massee
 *
 */
public class TurnierEventHandler {

	private final ArrayList<ITurnierEventListener> listeners;

	public TurnierEventHandler() {
		listeners = new ArrayList<>();
	}

	public void trigger(TurnierEventType type, ITurnierEvent eventObj) {
		switch (type) {
		case NewCreated:
			onNewCreated(eventObj);
		case ConfigChanged:
			onConfigChanged(eventObj);
			break;
		default:
			break;
		}
	}

	public void addTurnierEventListener(ITurnierEventListener listner) {
		listeners.add(listner);
	}

	public void removeTurnierEventListener(ITurnierEventListener listner) {
		listeners.remove(listner);
	}

	private void onNewCreated(ITurnierEvent eventObj) {
		for (ITurnierEventListener listner : listeners) {
			listner.onNewCreated(eventObj);
		}
	}

	/**
	 * @param eventObj
	 */
	private void onConfigChanged(ITurnierEvent eventObj) {
		for (ITurnierEventListener listner : listeners) {
			listner.onConfigChanged(eventObj);
		}
	}

	public void disposing() {
		listeners.clear();
	}
}
