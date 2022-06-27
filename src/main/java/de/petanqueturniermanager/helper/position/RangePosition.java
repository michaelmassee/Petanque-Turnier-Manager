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
		checkArgument(start.getColumn() <= ende.getColumn(), "spalte (column) start %s > ende %s", start.getColumn(),
				ende.getColumn());
		checkArgument(start.getRow() <= ende.getRow(), "zeile (row) start %s > ende %s", start.getRow(), ende.getRow());

		this.start = start;
		this.ende = ende;
	}

	/**
	 * @param rangePos
	 * @return
	 */
	public static RangePosition from(RangePosition rangePos) {
		checkNotNull(rangePos);
		return from(rangePos.getStart(), rangePos.getEnde());
	}

	public static RangePosition from(AbstractPosition<?> start, AbstractPosition<?> ende) {
		checkNotNull(start);
		checkNotNull(ende);
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

	/**
	 * Adress of Range BT5:BT10
	 * 
	 * @return
	 */
	public String getAddress() {
		if (start != null && ende != null) {
			return start.getAddress() + ":" + ende.getAddress();
		}
		return "";
	}

	public String getAddressWith$() {
		if (start != null && ende != null) {
			return start.getAddressWith$() + ":" + ende.getAddressWith$();
		}
		return "";
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

	/**
	 * Der komplete Range um x Spalten nach rechts oder links (negativ) verschieben
	 */
	public RangePosition spaltePlus(int anz) {
		start.spaltePlus(anz);
		ende.spaltePlus(anz);
		return this;
	}

	/**
	 * Verschiebe Range nach Spalte
	 */
	public RangePosition spalte(int spalte) {
		start.spalte(spalte);
		ende.spalte(spalte);
		return this;
	}

	/**
	 * Der komplete Range um eine Zeile nach unten verschieben
	 */
	public RangePosition zeilePlusEins() {
		start.zeilePlusEins();
		ende.zeilePlusEins();
		return this;
	}

	/**
	 * Der komplete Range um x Spalten nach unten oder oben (negativ) verschieben
	 */
	public RangePosition zeilePlus(int anz) {
		start.zeilePlus(anz);
		ende.zeilePlus(anz);
		return this;

	}

}
