/**
* Erstellung : 15.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.cellvalue;

import static com.google.common.base.Preconditions.checkNotNull;

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
	public static final String ROTATEANGLE = "RotateAngle";
	public static final String IS_CELLBACKGROUND_TRANSPARENT = "IsCellBackgroundTransparent";

	CellProperties() {
	}

	public static CellProperties from() {
		return new CellProperties();
	}

	public static CellProperties from(String key, Object value) {
		checkNotNull(key);
		checkNotNull(value);
		return CellProperties.from().put(key, value);
	}

	@Override
	public CellProperties put(String key, Object value) {
		super.put(key, value);
		return this;
	}

	public CellProperties setBorder(TableBorder2 tableBorder2) {
		return put(TABLE_BORDER2, tableBorder2);
	}

	// list of common properties
	/**
	 * @param fontWeight = com.sun.star.awt.FontWeight.*
	 * @return
	 */

	public CellProperties setCharWeight(float fontWeight) {
		return put(CHAR_WEIGHT, fontWeight);
	}

	/**
	 * property "CharColor"
	 */

	public CellProperties setCharColor(Integer charColor) {
		super.put(CHAR_COLOR, charColor);
		return this;
	}

	/**
	 * @param hexCharColor = ein hex wert ohne vorzeichen
	 * @return
	 */

	public CellProperties setCharColor(String hexCharColor) {
		return put(CHAR_COLOR, Integer.valueOf(hexCharColor, 16));
	}

	public CellProperties setHeight(int height) {
		return put(HEIGHT, height);
	}

	public CellProperties setCharHeight(int height) {
		return put(CHAR_HEIGHT, height);
	}

	/**
	 * com.sun.star.table.CellHoriJustify.class
	 *
	 * @param cellHoriJustify CellHoriJustify
	 */

	public CellProperties setHoriJustify(CellHoriJustify cellHoriJustify) {
		return put(HORI_JUSTIFY, cellHoriJustify);
	}

	/**
	 * Text in der Zelle wird an der Zelle Große angepasst
	 *
	 * @param boolean
	 */

	public CellProperties setShrinkToFit(boolean shrink) {
		return put(SHRINK_TO_FIT, shrink);
	}

	/**
	 * @param vertjustify Type = CellVertJustify2
	 * @return
	 */

	public CellProperties setVertJustify(int vertjustify) {
		return put(VERT_JUSTIFY, vertjustify);
	}

	public CellProperties setCellBackColor(Integer color) {
		return put(CELL_BACK_COLOR, color);
	}

	/**
	 * @param hexCharColor = ein hex wert ohne vorzeichen
	 * @return
	 */

	public CellProperties setCellBackColor(String hexCharColor) {
		return put(CELL_BACK_COLOR, Integer.valueOf(hexCharColor, 16));
	}

	public CellProperties removeCellBackColor() {
		this.remove(CELL_BACK_COLOR);
		return this;
	}

	public CellProperties setWidth(int width) {
		return put(WIDTH, width);
	}

	/**
	 * ist in inspector als Typ Long ? aber muss als int, sonnst iligalargument exception
	 *
	 * @param angle
	 * @return
	 */

	public CellProperties setRotateAngle(int angle) {
		return put(ROTATEANGLE, new Integer(angle));
	}

	public CellProperties setCellbackgroundTransparent(boolean isTransparent) {
		return put(IS_CELLBACKGROUND_TRANSPARENT, isTransparent);
	}
}
