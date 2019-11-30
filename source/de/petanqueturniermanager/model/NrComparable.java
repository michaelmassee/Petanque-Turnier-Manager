package de.petanqueturniermanager.model;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Objects;

/*
* NrComparable.java
*
* Erstellung     : 07.09.2017 / Michael Massee
*
*/

public abstract class NrComparable<T extends TurnierDaten> implements Comparable<T> {
	final int nr;

	public NrComparable(int nr) {
		checkArgument(nr > 0, "Nr <1");
		this.nr = nr;
	}

	public int getNr() {
		return nr;
	}

	@Override
	public int compareTo(T turnierDaten) {
		if (turnierDaten.getNr() < getNr()) {
			return 1;
		}
		if (turnierDaten.getNr() > getNr()) {
			return -1;
		}
		return 0;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof TurnierDaten)) {
			return false;
		}
		return getNr() == ((TurnierDaten) obj).getNr();
	}

	@Override
	public int hashCode() {
		return Objects.hash(nr);
	}
}
