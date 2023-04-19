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
		return !this.wkRef.refersTo(null) && this.wkRef.get() != null;
	}

	public final T get() {
		if (!this.wkRef.refersTo(null)) {
			return this.wkRef.get();
		}
		// darf nicht passieren
		throw new NullPointerException("Weakref " + wkRef + " ist Nicht mehr verfügbar");
	}
}
