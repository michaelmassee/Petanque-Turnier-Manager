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
		for (ITurnierEventListener listner : listeners) {
			listner.onPropertiesChanged(eventObj);
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
	}
}
