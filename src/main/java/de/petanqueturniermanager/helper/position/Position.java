/**
 * Erstellung : 26.03.2018 / Michael Massee
 **/

package de.petanqueturniermanager.helper.position;

public class Position extends AbstractPosition<Position> {
	/**
	 *
	 * @param spalte (column) erste Spalte = 0
	 * @param zeile (row) erste Zeile = 0
	 */

	private Position(int spalte, int zeile) {
		super(spalte, zeile);
	}

	/**
	 *
	 * @param spalte (column) erste Spalte = 0
	 * @param zeile (row) erste Zeile = 0
	 */
	public static Position from(int spalte, int zeile) {
		return new Position(spalte, zeile);
	}

	public static Position from(String spalte, int zeile) {
		return new Position(spalteStringToNumber(spalte), zeile);
	}

	public static Position from(AbstractPosition<?> pos) {
		if (pos != null) {
			return Position.from(pos.getSpalte(), pos.getZeile());
		}
		return null;
	}

}
