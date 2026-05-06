/**
 * Erstellung : 05.04.2018 / Michael Massee
 **/

package de.petanqueturniermanager.helper.sheet;

import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.ref.WeakReference;

public class WeakRefHelper<T> {

	private final WeakReference<T> wkRef;

	public WeakRefHelper(T referent) {
		checkNotNull(referent);
		this.wkRef = new WeakReference<>(referent);
	}

	public WeakRefHelper(WeakReference<T> wkRef) {
		checkNotNull(wkRef);
		this.wkRef = wkRef;
	}

	public final boolean isPresent() {
		return this.wkRef.get() != null;
	}

	public final T get() {
		// Einmaliger Read — kein TOCTOU-Race zwischen Check und Auflösung.
		T referent = this.wkRef.get();
		if (referent == null) {
			throw new NullPointerException("Weakref " + wkRef + " ist Nicht mehr verfügbar");
		}
		return referent;
	}
}
