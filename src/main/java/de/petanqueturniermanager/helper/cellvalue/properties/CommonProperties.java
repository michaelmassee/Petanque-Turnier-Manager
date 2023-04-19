/**
 * Erstellung 16.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.helper.cellvalue.properties;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;

import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.CellVertJustify2;
import com.sun.star.table.TableBorder2;

import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.sheet.numberformat.UserNumberFormat;

/**
 * @author Michael Massee
 *
 */
public abstract class CommonProperties<T> extends HashMap<String, Object> implements ICommonProperties {

	private UserNumberFormat userNumberFormat = null;

	CommonProperties() {
	}

	@Override
	@SuppressWarnings("unchecked")
	public final T put(String key, Object value) {
		super.put(key, value);
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public final T remove(String key) {
		super.remove(key);
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T numberFormat(UserNumberFormat userNumberFormat) {
		this.userNumberFormat = userNumberFormat;
		return (T) this;
	}

	public T setAllThinBorder() {
		TableBorder2 tableBorder2 = BorderFactory.from().allThin().toBorder();
		return put(TABLE_BORDER2, tableBorder2);
	}

	public T setBorder(TableBorder2 tableBorder2) {
		return put(TABLE_BORDER2, tableBorder2);
	}

	/**
	 * @param fontWeight = com.sun.star.awt.FontWeight.*
	 * @return
	 */

	public T setCharWeight(float fontWeight) {
		return put(CHAR_WEIGHT, fontWeight);
	}

	/**
	 * property "CharColor"
	 */

	public T setCharColor(Integer charColor) {
		return put(CHAR_COLOR, charColor);
	}

	/**
	 * @param hexCharColor = ein hex wert
	 * @return
	 */

	public T setCharColor(String hexCharColor) {
		return put(CHAR_COLOR, Integer.valueOf(StringUtils.strip(StringUtils.strip(hexCharColor), "#"), 16));
	}

	public T setCharHeight(int height) {
		return put(CHAR_HEIGHT, height);
	}

	/**
	 * horizontal und vertikal center
	 *
	 * @return
	 */
	public T centerJustify() {
		centerHoriJustify();
		return centerVertJustify();
	}

	public T centerHoriJustify() {
		return put(HORI_JUSTIFY, CellHoriJustify.CENTER);
	}

	public T centerVertJustify() {
		return put(VERT_JUSTIFY, CellVertJustify2.CENTER);
	}

	/**
	 * com.sun.star.table.CellHoriJustify.class
	 *
	 * @param cellHoriJustify CellHoriJustify
	 */

	public T setHoriJustify(CellHoriJustify cellHoriJustify) {
		return put(HORI_JUSTIFY, cellHoriJustify);
	}

	/**
	 * Text in der Zelle wird an der Zelle Große angepasst
	 *
	 * @param boolean
	 */

	public T setShrinkToFit(boolean shrink) {
		return put(SHRINK_TO_FIT, shrink);
	}

	public T setShrinkToFit() {
		return put(SHRINK_TO_FIT, true);
	}

	/**
	 * @param vertjustify Type = CellVertJustify2
	 * @return
	 */

	public T setVertJustify(int vertjustify) {
		return put(VERT_JUSTIFY, vertjustify);
	}

	public T setCellBackColor(Integer color) {
		checkNotNull(color, "color=null");
		return put(CELL_BACK_COLOR, color);
	}

	/**
	 * @param hexCharColor = ein hex wert
	 * @return
	 */
	public T setCellBackColor(String hexCharColor) {
		return put(CELL_BACK_COLOR, Integer.valueOf(StringUtils.strip(StringUtils.strip(hexCharColor), "#"), 16));
	}

	public T removeCellBackColor() {
		return remove(CELL_BACK_COLOR);
	}

	public T removeCharColor() {
		return remove(CHAR_COLOR);
	}

	/**
	 * ist in inspector als Typ Long ? aber muss als int, sonnst iligalargument exception
	 *
	 * @param angle
	 * @return
	 */
	public T setRotateAngle(int angle) {
		return put(ROTATEANGLE, Integer.valueOf(angle));
	}

	public T setCellbackgroundTransparent(boolean isTransparent) {
		return put(IS_CELLBACKGROUND_TRANSPARENT, isTransparent);
	}

	public T topMargin(int margin) {
		return put(TOP_MARGIN, Integer.valueOf(margin));
	}

	public T bottomMargin(int margin) {
		return put(BOTTOM_MARGIN, Integer.valueOf(margin));
	}

	public T leftMargin(int margin) {
		return put(LEFT_MARGIN, Integer.valueOf(margin));
	}

	public T rightMargin(int margin) {
		return put(RIGHT_MARGIN, Integer.valueOf(margin));
	}

	public T margin(int margin) {
		topMargin(margin);
		bottomMargin(margin);
		leftMargin(margin);
		return rightMargin(margin);
	}

	/**
	 * @return the userNumberFormat
	 */
	public final UserNumberFormat getUserNumberFormat() {
		return userNumberFormat;
	}
}
