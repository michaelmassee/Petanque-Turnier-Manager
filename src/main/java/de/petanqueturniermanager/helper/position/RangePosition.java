/**
* Erstellung : 27.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.position;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.MoreObjects;

public class RangePosition {

	private final AbstractPosition<?> start;
	private final AbstractPosition<?> ende;

	private RangePosition(AbstractPosition<?> start, AbstractPosition<?> ende) {
		checkNotNull(start);
		checkNotNull(ende);
		checkArgument(start.getColumn() <= ende.getColumn(), "spalte (column) start %s > ende %s", start.getColumn(), ende.getColumn());
		checkArgument(start.getRow() <= ende.getRow(), "zeile (row) start %s > ende %s", start.getRow(), ende.getRow());

		this.start = start;
		this.ende = ende;
	}

	public static RangePosition from(AbstractPosition<?> start, AbstractPosition<?> ende) {
		return new RangePosition(start, ende);
	}

	public static RangePosition from(int startSpalte, int startZeile, int endeSpalte, int endeZeile) {
		return from(Position.from(startSpalte, startZeile), Position.from(endeSpalte, endeZeile));
	}

	public static RangePosition from(int startSpalte, int startZeile, AbstractPosition<?> ende) {
		return from(startSpalte, startZeile, ende.getSpalte(), ende.getZeile());
	}

	public static RangePosition from(AbstractPosition<?> start, int endeSpalte, int endeZeile) {
		return from(start, Position.from(endeSpalte, endeZeile));
	}

	public AbstractPosition<?> getStart() {
		return start;
	}

	public int getStartZeile() {
		return start.getZeile();
	}

	public int getStartSpalte() {
		return start.getSpalte();
	}

	public AbstractPosition<?> getEnde() {
		return ende;
	}

	public int getEndeZeile() {
		return ende.getZeile();
	}

	public int getEndeSpalte() {
		return ende.getSpalte();
	}

	@Override
	public String toString() {
		// @formatter:off
		return MoreObjects.toStringHelper(this)
				.add("\r\nStart", start)
				.add("\r\nEnd", ende)
				.toString();
		// @formatter:on
	}

	public int getAnzahlZeilen() {
		return (ende.getZeile() - start.getZeile()) + 1;
	}

	public int getAnzahlSpalten() {
		return (ende.getSpalte() - start.getSpalte()) + 1;
	}

	/**
	 * Der komplete Range um eine Spalte nach rechts verschieben
	 */
	public RangePosition spaltePlusEins() {
		start.spaltePlusEins();
		ende.spaltePlusEins();
		return this;
	}

}
