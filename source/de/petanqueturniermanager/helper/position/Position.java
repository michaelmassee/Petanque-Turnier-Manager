/**
* Erstellung : 26.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.position;

public class Position extends AbstractPosition<Position> {
	/**
	 *
	 * @param spalte (column)
	 * @param zeile (row)
	 */

	private Position(int spalte, int zeile) {
		super(spalte, zeile);
	}

	/**
	 *
	 * @param spalte (column)
	 * @param zeile (row)
	 */
	public static Position from(int spalte, int zeile) {
		return new Position(spalte, zeile);
	}

	public static Position from(AbstractPosition<?> pos) {
		if (pos != null) {
			return Position.from(pos.getSpalte(), pos.getZeile());
		}
		return null;
	}

}
