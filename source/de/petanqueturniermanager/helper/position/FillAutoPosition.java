/**
* Erstellung : 09.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.position;

import com.sun.star.sheet.FillDirection;

public class FillAutoPosition extends AbstractPosition<FillAutoPosition> {

	private FillDirection fillDirection = FillDirection.TO_BOTTOM; // default fill direction fuer fillAuto

	public FillAutoPosition() {
	}

	public FillAutoPosition(int spalte, int zeile) {
		super(spalte, zeile);
	}

	public FillDirection getFillDirection() {
		return this.fillDirection;
	}

	public FillAutoPosition setFillDirection(FillDirection fillDirection) {
		this.fillDirection = fillDirection;
		return this;
	}

	/**
	 *
	 * @param spalte (column)
	 * @param zeile (row)
	 */
	public static FillAutoPosition from(int spalte, int zeile) {
		return new FillAutoPosition(spalte, zeile);
	}

	public static FillAutoPosition from(AbstractPosition pos) {
		if (pos != null) {
			return FillAutoPosition.from(pos.getSpalte(), pos.getZeile());
		}
		return null;
	}

}
