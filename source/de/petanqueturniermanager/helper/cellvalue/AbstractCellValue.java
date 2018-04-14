/**
* Erstellung : 27.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.cellvalue;

import static com.google.common.base.Preconditions.*;

import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.MoreObjects;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;

import de.petanqueturniermanager.helper.position.FillAutoPosition;
import de.petanqueturniermanager.helper.position.Position;

@SuppressWarnings("rawtypes")
abstract public class AbstractCellValue<T extends AbstractCellValue, V> {

	// VertJustifyMethod

	public static final String HORI_JUSTIFY = "HoriJustify";
	public static final String VERT_JUSTIFY = "VertJustify";
	public static final String CHAR_COLOR = "CharColor";
	public static final String CHAR_WEIGHT = "CharWeight";
	public static final String CHAR_HEIGHT = "CharHeight";
	public static final String TABLE_BORDER = "TableBorder";
	public static final String TABLE_BORDER2 = "TableBorder2";
	public static final String HEIGHT = "Height";
	// SHRINK_TO_FIT = Boolean, Text in der Zelle wird an der Zelle Große angepasst
	public static final String SHRINK_TO_FIT = "ShrinkToFit";
	public static final String CELL_BACK_COLOR = "CellBackColor";

	private V value;
	private XSpreadsheet sheet;
	private Position pos;
	private String comment;
	private int setColumnWidth;
	private CellHoriJustify spalteHoriJustify;
	private HashMap<String, Object> cellProperties = new HashMap<>();
	private Position endPosMerge; // wenn vorhanden dann merge die zellen von pos bis endPosMerge
	private FillAutoPosition fillAuto; // wenn vorhanden dann autoFill bis diese Position

	public AbstractCellValue() {
	}

	public AbstractCellValue(XSpreadsheet sheet, Position pos) {
		checkNotNull(sheet);
		checkNotNull(pos);
		this.setSheet(sheet);
		this.setPos(pos);
	}

	public String getComment() {
		return this.comment;
	}

	public boolean hasComment() {
		return StringUtils.isNotBlank(this.comment);
	}

	@SuppressWarnings("unchecked")
	public T setComment(String comment) {
		this.comment = comment;
		return (T) this;
	}

	public XSpreadsheet getSheet() {
		return this.sheet;
	}

	@SuppressWarnings("unchecked")
	public T setSheet(XSpreadsheet sheet) {
		checkNotNull(sheet);
		this.sheet = sheet;
		return (T) this;
	}

	public Position getPos() {
		checkNotNull(this.pos);
		return this.pos;
	}

	@SuppressWarnings("unchecked")
	public T setPos(Position pos) {
		checkNotNull(pos);
		this.pos = Position.from(pos);
		return (T) this;
	}

	public Position getEndPosMerge() {
		return this.endPosMerge;
	}

	/**
	 * wenn vorhanden dan werden die zellen von pos bis endpos zusammengefasst (merge)
	 *
	 * @param endPosMerge darf null sein
	 * @return this
	 */
	@SuppressWarnings("unchecked")
	public T setEndPosMerge(Position endPosMerge) {
		this.endPosMerge = Position.from(endPosMerge);
		return (T) this;
	}

	/**
	 * wenn vorhanden dann werden die zellen von pos bis endpos zusammengefasst (merge)<br>
	 * aktuelle position plus anzahl spalten
	 *
	 * @return this
	 */
	@SuppressWarnings("unchecked")
	public T setEndPosMergeSpaltePlus(int anzSpalten) {
		this.setEndPosMerge(Position.from(getPos()).spaltePlus(anzSpalten));
		return (T) this;
	}

	/**
	 * @param abstractCellValue
	 */
	@SuppressWarnings("unchecked")
	protected T copyAttr(T abstractCellValue) {
		checkNotNull(abstractCellValue);
		copyCommonAttr(abstractCellValue);
		this.value = (V) abstractCellValue.getValue();
		return (T) this;
	}

	protected AbstractCellValue copyCommonAttr(AbstractCellValue<?, ?> abstractCellValue) {
		this.setSheet(abstractCellValue.getSheet());
		this.setPos(abstractCellValue.getPos());
		this.setEndPosMerge(abstractCellValue.getEndPosMerge());
		this.comment = abstractCellValue.getComment();
		this.setColumnWidth = abstractCellValue.getSetColumnWidth();
		this.spalteHoriJustify = abstractCellValue.getSpalteHoriJustify();
		this.cellProperties.putAll(abstractCellValue.getCellProperties());
		return this;
	}

	public V getValue() {
		return this.value;
	}

	@SuppressWarnings("unchecked")
	public T setValue(V value) {
		this.value = value;
		return (T) this;
	}

	@Override
	public String toString() {
		// @formatter:off
		return MoreObjects.toStringHelper(this).
				add("Value", getValue()).
				add("Comment", getComment()).
				add("\nProperties", getCellProperties()).
				add("\nPosition", (this.pos!=null)?this.pos.toString():"null").
				add("\nEndPosMerge", (this.endPosMerge!=null)?this.endPosMerge.toString():"null").
				toString();
		// @formatter:on
	}

	public int getSetColumnWidth() {
		return this.setColumnWidth;
	}

	@SuppressWarnings("unchecked")
	public T setSetColumnWidth(int setColumnWidth) {
		this.setColumnWidth = setColumnWidth;
		return (T) this;
	}

	public CellHoriJustify getSpalteHoriJustify() {
		return this.spalteHoriJustify;
	}

	@SuppressWarnings("unchecked")
	public T setSpalteHoriJustify(CellHoriJustify spalteHoriJustify) {
		this.spalteHoriJustify = spalteHoriJustify;
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T spalte(int val) {
		getPos().spalte(val);
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T spaltePlusEins() {
		getPos().spaltePlusEins();
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T spaltePlus(int anz) {
		getPos().spaltePlus(anz);
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T zeile(int val) {
		getPos().zeile(val);
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T zeilePlusEins() {
		getPos().zeilePlusEins();
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T zeilePlus(int anz) {
		getPos().zeilePlus(anz);
		return (T) this;
	}

	public HashMap<String, Object> getCellProperties() {
		return this.cellProperties;
	}

	@SuppressWarnings("unchecked")
	public T setCellProperties(HashMap<String, Object> cellProperties) {
		this.cellProperties = cellProperties;
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T addCellProperty(String key, Object val) {
		this.cellProperties.put(key, val);
		return (T) this;
	}

	// list of common properties
	/**
	 * @param fontWeight = com.sun.star.awt.FontWeight.*
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public T setCharWeight(float fontWeight) {
		this.cellProperties.put(CHAR_WEIGHT, fontWeight);
		return (T) this;
	}

	/**
	 * property "CharColor"
	 */
	@SuppressWarnings("unchecked")
	public T setCharColor(Integer charColor) {
		this.cellProperties.put(CHAR_COLOR, charColor);
		return (T) this;
	}

	/**
	 * @param hexCharColor = ein hex wert ohne vorzeichen
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public T setCharColor(String hexCharColor) {
		this.cellProperties.put(CHAR_COLOR, Integer.valueOf(hexCharColor, 16));
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T setHeight(int height) {
		this.cellProperties.put(HEIGHT, height);
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T setCharHeight(int height) {
		this.cellProperties.put(CHAR_HEIGHT, height);
		return (T) this;
	}

	/**
	 * com.sun.star.table.CellHoriJustify.class
	 *
	 * @param cellHoriJustify CellHoriJustify
	 */
	@SuppressWarnings("unchecked")
	public T setHoriJustify(CellHoriJustify cellHoriJustify) {
		this.cellProperties.put(HORI_JUSTIFY, cellHoriJustify);
		return (T) this;
	}

	/**
	 * Text in der Zelle wird an der Zelle Große angepasst
	 *
	 * @param boolean
	 */
	@SuppressWarnings("unchecked")
	public T setShrinkToFit(boolean shrink) {
		this.cellProperties.put(SHRINK_TO_FIT, shrink);
		return (T) this;
	}

	/**
	 * @param vertjustify Type = CellVertJustify2
	 * @return
	 */

	@SuppressWarnings("unchecked")
	public T setVertJustify(int vertjustify) {
		this.cellProperties.put(VERT_JUSTIFY, vertjustify);
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T setCellBackColor(Integer color) {
		this.cellProperties.put(CELL_BACK_COLOR, color);
		return (T) this;
	}

	/**
	 * @param hexCharColor = ein hex wert ohne vorzeichen
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public T setCellBackColor(String hexCharColor) {
		this.cellProperties.put(CELL_BACK_COLOR, Integer.valueOf(hexCharColor, 16));
		return (T) this;
	}

	public FillAutoPosition getFillAuto() {
		return this.fillAuto;
	}

	@SuppressWarnings("unchecked")
	public T setFillAuto(FillAutoPosition fillAuto) {
		this.fillAuto = fillAuto;
		return (T) this;
	}

}
