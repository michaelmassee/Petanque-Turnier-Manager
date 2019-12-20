package de.petanqueturniermanager.model;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Objects;

/*
* NrComparable.java
*
* Erstellung     : 07.09.2017 / Michael Massee
*
*/

public abstract class NrComparable implements Comparable<NrComparable> {
	final int nr;

	public NrComparable(int nr) {
		checkArgument(nr > 0, "Nr <1");
		this.nr = nr;
	}

	public final int getNr() {
		return nr;
	}

	@Override
	public final int compareTo(NrComparable nrComparable) {

		if (nrComparable == null) {
			return 1;
		}

		if (nrComparable.getNr() < getNr()) {
			return 1;
		}
		if (nrComparable.getNr() > getNr()) {
			return -1;
		}
		return 0;
	}

	@Override
	public final boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}

		if (obj == this) {
			return true;
		}
		if (!(obj instanceof NrComparable)) {
			return false;
		}
		return getNr() == ((NrComparable) obj).getNr();
	}

	@Override
	public final int hashCode() {
		return Objects.hash(nr);
	}
}
