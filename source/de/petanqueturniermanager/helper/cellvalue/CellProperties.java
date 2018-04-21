/**
* Erstellung : 15.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.cellvalue;

import java.util.HashMap;

import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.TableBorder2;

@SuppressWarnings("serial")
public class CellProperties extends HashMap<String, Object> {

	public static final String WIDTH = "Width";
	public static final String HORI_JUSTIFY = "HoriJustify";
	public static final String VERT_JUSTIFY = "VertJustify";
	public static final String CHAR_COLOR = "CharColor";
	public static final String CHAR_WEIGHT = "CharWeight";
	public static final String CHAR_HEIGHT = "CharHeight";
	// public static final String TABLE_BORDER = "TableBorder";
	public static final String TABLE_BORDER2 = "TableBorder2";
	public static final String HEIGHT = "Height";
	// SHRINK_TO_FIT = Boolean, Text in der Zelle wird an der Zelle Große angepasst
	public static final String SHRINK_TO_FIT = "ShrinkToFit";
	public static final String CELL_BACK_COLOR = "CellBackColor";

	public static CellProperties from() {
		return new CellProperties();
	}

	public CellProperties setBorder(TableBorder2 tableBorder2) {
		this.put(TABLE_BORDER2, tableBorder2);
		return this;
	}

	public CellProperties addCellProperty(String key, Object val) {
		this.put(key, val);
		return this;
	}

	// list of common properties
	/**
	 * @param fontWeight = com.sun.star.awt.FontWeight.*
	 * @return
	 */

	public CellProperties setCharWeight(float fontWeight) {
		this.put(CHAR_WEIGHT, fontWeight);
		return this;
	}

	/**
	 * property "CharColor"
	 */

	public CellProperties setCharColor(Integer charColor) {
		this.put(CHAR_COLOR, charColor);
		return this;
	}

	/**
	 * @param hexCharColor = ein hex wert ohne vorzeichen
	 * @return
	 */

	public CellProperties setCharColor(String hexCharColor) {
		this.put(CHAR_COLOR, Integer.valueOf(hexCharColor, 16));
		return this;
	}

	public CellProperties setHeight(int height) {
		this.put(HEIGHT, height);
		return this;
	}

	public CellProperties setCharHeight(int height) {
		this.put(CHAR_HEIGHT, height);
		return this;
	}

	/**
	 * com.sun.star.table.CellHoriJustify.class
	 *
	 * @param cellHoriJustify CellHoriJustify
	 */

	public CellProperties setHoriJustify(CellHoriJustify cellHoriJustify) {
		this.put(HORI_JUSTIFY, cellHoriJustify);
		return this;
	}

	/**
	 * Text in der Zelle wird an der Zelle Große angepasst
	 *
	 * @param boolean
	 */

	public CellProperties setShrinkToFit(boolean shrink) {
		this.put(SHRINK_TO_FIT, shrink);
		return this;
	}

	/**
	 * @param vertjustify Type = CellVertJustify2
	 * @return
	 */

	public CellProperties setVertJustify(int vertjustify) {
		this.put(VERT_JUSTIFY, vertjustify);
		return this;
	}

	public CellProperties setCellBackColor(Integer color) {
		this.put(CELL_BACK_COLOR, color);
		return this;
	}

	/**
	 * @param hexCharColor = ein hex wert ohne vorzeichen
	 * @return
	 */

	public CellProperties setCellBackColor(String hexCharColor) {
		this.put(CELL_BACK_COLOR, Integer.valueOf(hexCharColor, 16));
		return this;
	}

	public CellProperties removeCellBackColor() {
		this.remove(CELL_BACK_COLOR);
		return this;
	}

	public CellProperties setWidth(int width) {
		this.put(WIDTH, width);
		return this;
	}

}
