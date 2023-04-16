/**
 * Erstellung : 26.03.2018 / Michael Massee
 **/

package de.petanqueturniermanager.helper.position;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.MoreObjects;

public abstract class AbstractPosition<T> {
	private int zeile; // 0 = 1. Zeile  
	private int spalte; // 0 = 1. Spalte

	// Nur Package
	AbstractPosition() {
	}

	/**
	 *
	 * @param spalte (column) max 1024
	 * @param zeile (row)
	 */

	protected AbstractPosition(int spalte, int zeile) {
		checkArgument(spalte > -1, "spalte (column) ungueltige wert %s. <0 ", spalte);
		checkArgument(zeile > -1, "zeile (row) ungueltige wert %s. <0 ", zeile);
		this.setZeile(zeile);
		this.setSpalte(spalte);
	}

	/**
	 * @return row
	 */
	public int getZeile() {
		return this.zeile;
	}

	/**
	 * @return zeile
	 */
	public int getRow() {
		return getZeile();
	}

	/**
	 * @return Column
	 */
	public int getSpalte() {
		return this.spalte;
	}

	/**
	 * @return spalte
	 */
	public int getColumn() {
		return getSpalte();
	}

	@SuppressWarnings("unchecked")
	public T zeilePlusEins() {
		this.setZeile(this.getZeile() + 1);
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T zeilePlus(int anz) {
		this.setZeile(this.getZeile() + anz);
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T zeile(int val) {
		this.setZeile(val);
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T spaltePlusEins() {
		this.setSpalte(this.getSpalte() + 1);
		return (T) this;
	}

	/**
	 * @param anz, anzahl spalten nach rechts verschieben, minus wert nach links
	 * @return
	 */

	@SuppressWarnings("unchecked")
	public T spaltePlus(int anz) {
		this.setSpalte(this.getSpalte() + anz);
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T spalte(int val) {
		this.setSpalte(val);
		return (T) this;
	}

	public void setZeile(int zeile) {
		checkArgument(zeile > -1, "zeile (row) ungueltige wert %d. <0 ", zeile);
		this.zeile = zeile;
	}

	public void setSpalte(int spalte) {
		checkArgument(spalte > -1, "spalte (column) ungueltige wert %d. <0 ", spalte);
		this.spalte = spalte;
	}

	public String getSpalteAddressWith$() {
		String aStr = "$";
		aStr += getSpalteString();
		aStr += ":$";
		aStr += getSpalteString();
		return aStr;

	}

	public String getSpalteAddress() {
		String aStr = "";
		aStr += getSpalteString();
		aStr += ":";
		aStr += getSpalteString();
		return aStr;
	}

	public String getAddress() {
		String aStr = "";
		aStr += getSpalteString();
		aStr += (getRow() + 1);
		return aStr;
	}

	public String getAddressWith$() {
		String aStr = "$";
		aStr += getSpalteString();
		aStr += "$";
		aStr += (getRow() + 1);
		return aStr;
	}

	/**
	 * alternativ apache.poi, ist aber eine komplette MS office lib!<br>
	 * https://poi.apache.org/apidocs/dev/org/apache/poi/ss/util/CellReference.html#convertNumToColString-int- <br>
	 * 
	 * @return
	 */

	public String getSpalteString() {
		return spalteNrToString(getColumn());
	}

	/**
	 * 1. Spalte =0
	 * 
	 * @param name "AAA"
	 * @return spaltenummer
	 */
	public static int spalteStringToNumber(String name) {
		checkNotNull(name);
		int number = 0;
		for (int i = 0; i < name.length(); i++) {
			number = number * 26 + (name.charAt(i) - ('A' - 1));
		}
		return number - 1;
	}

	/**
	 * 1. Spalte =0
	 * 
	 * @param number -1 >
	 * @return spalte String "A"
	 */

	public static String spalteNrToString(int number) {
		checkArgument(number > -1);
		number++;
		StringBuilder sb = new StringBuilder();
		while (number-- > 0) {
			sb.append((char) ('A' + (number % 26)));
			number /= 26;
		}
		return sb.reverse().toString();
	}

	/**
	 * equal is true when spalte und zeile ==
	 */

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}

		if (!(obj instanceof AbstractPosition<?>)) {
			return false;
		}

		return ((AbstractPosition<?>) obj).getSpalte() == this.getSpalte()
				&& ((AbstractPosition<?>) obj).getZeile() == this.getZeile();
	}

	@Override
	public String toString() {
		// @formatter:off
		return MoreObjects.toStringHelper(this)
				.add("Zeile (row)", this.getZeile())
				.add("Spalte (column)", this.getSpalte())
				.add("Adress", this.getAddress())
				.toString();
		// @formatter:on
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}
}
