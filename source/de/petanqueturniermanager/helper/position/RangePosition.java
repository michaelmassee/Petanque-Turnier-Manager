/**
* Erstellung : 27.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.position;

import static com.google.common.base.Preconditions.*;

public class RangePosition {

	private final AbstractPosition<?> start;
	private final AbstractPosition<?> ende;

	public RangePosition(int startSpalte, int startZeile, int endeSpalte, int endeZeile) {
		this(new Position(startSpalte, startZeile), new Position(endeSpalte, endeZeile));
	}

	public RangePosition(AbstractPosition<?> start, AbstractPosition<?> ende) {
		checkNotNull(start);
		checkNotNull(ende);
		checkArgument(start.getColumn() <= ende.getColumn(), "spalte (column) start %s > ende %s", start.getColumn(),
				ende.getColumn());
		checkArgument(start.getRow() <= ende.getRow(), "zeile (row) start %s > ende %s", start.getRow(), ende.getRow());

		this.start = start;
		this.ende = ende;
	}

	public static RangePosition from(int startSpalte, int startZeile, int endeSpalte, int endeZeile) {
		return new RangePosition(startSpalte, startZeile, endeSpalte, endeZeile);
	}

	public static RangePosition from(AbstractPosition<?> start, AbstractPosition<?> ende) {
		return new RangePosition(start, ende);
	}

	public AbstractPosition<?> getStart() {
		return this.start;
	}

	public int getStartZeile() {
		return this.start.getZeile();
	}

	public int getStartSpalte() {
		return this.start.getSpalte();
	}

	public AbstractPosition<?> getEnde() {
		return this.ende;
	}

	public int getEndeZeile() {
		return this.ende.getZeile();
	}

	public int getEndeSpalte() {
		return this.ende.getSpalte();
	}

}
