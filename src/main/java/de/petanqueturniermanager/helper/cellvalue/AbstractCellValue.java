/**
* Erstellung : 27.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.cellvalue;

import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.MoreObjects;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.CellVertJustify2;
import com.sun.star.table.TableBorder2;

import de.petanqueturniermanager.helper.cellvalue.properties.CellProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.RowProperties;
import de.petanqueturniermanager.helper.position.FillAutoPosition;
import de.petanqueturniermanager.helper.position.Position;

@SuppressWarnings("rawtypes")
abstract public class AbstractCellValue<T extends ICellValue, V> {

	public static final int ROTATEANGLE_PLUS_90 = 27000;

	private V value;
	private Position pos;
	private String comment;
	private CellProperties cellProperties = CellProperties.from();
	private ColumnProperties columnProperties = ColumnProperties.from();
	private RowProperties rowProperties = RowProperties.from();
	private Position endPosMerge; // wenn vorhanden dann merge die zellen von pos bis endPosMerge
	private FillAutoPosition fillAuto; // wenn vorhanden dann autoFill bis diese Position
	private boolean ueberschreiben = true; // vorhanden inhalt in der zelle überschreiben ? Default = true

	protected AbstractCellValue() {
	}

	protected AbstractCellValue(Position pos) {
		checkNotNull(pos);
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

	public Position getPos() {
		checkNotNull(this.pos);
		return this.pos;
	}

	/**
	 * Copy Position values in new Position Object
	 * 
	 * @param pos
	 * @return this
	 */

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
	 * wenn vorhanden dann werden die zellen von pos bis endpos zusammengefasst (merge)<br>
	 * zum löschen = null
	 *
	 * @param endPosMerge darf null sein
	 * @return this
	 */
	@SuppressWarnings("unchecked")
	public T setEndPosMerge(Position endPosMerge) {
		this.endPosMerge = Position.from(endPosMerge); // wenn null dann return null
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
	 * @param letzteSpalte
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public T setEndPosMergeSpalte(int letzteSpalte) {
		this.setEndPosMerge(Position.from(getPos()).spalte(letzteSpalte));
		return (T) this;
	}

	/**
	 * wenn vorhanden dann werden die zellen von pos bis endpos zusammengefasst (merge)<br>
	 * aktuelle position plus anzahl spalten
	 *
	 * @return this
	 */
	@SuppressWarnings("unchecked")
	public T setEndPosMergeZeilePlus(int anzZeilen) {
		this.setEndPosMerge(Position.from(getPos()).zeilePlus(anzZeilen));
		return (T) this;
	}

	/**
	 * @param letzteZeile
	 * @return this
	 */
	@SuppressWarnings("unchecked")
	public T setEndPosMergeZeile(int letzteZeile) {
		this.setEndPosMerge(Position.from(getPos()).zeile(letzteZeile));
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

	protected AbstractCellValue copyCommonAttr(ICellValue<?> abstractCellValue) {
		this.setPos(abstractCellValue.getPos());
		this.setEndPosMerge(abstractCellValue.getEndPosMerge());
		this.comment = abstractCellValue.getComment();
		this.cellProperties.putAll(abstractCellValue.getCellProperties());
		this.columnProperties.putAll(abstractCellValue.getColumnProperties());
		this.rowProperties.putAll(abstractCellValue.getRowProperties());
		this.ueberschreiben = abstractCellValue.isUeberschreiben();
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

	@SuppressWarnings("unchecked")
	public T spalte(int val) {
		getPos().spalte(val);
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T spaltePlusEins() {
		spaltePlus(1);
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T spaltePlus(int anz) {
		getPos().spaltePlus(anz);
		if (this.endPosMerge != null) {
			this.endPosMerge.spaltePlus(anz);
		}
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T zeile(int val) {
		getPos().zeile(val);
		return (T) this;
	}

	/**
	 * pos Zeile + 1, <br>
	 * UND ! ggf endposmerge zeile +1
	 *
	 * @return AbstractCellValue
	 */
	@SuppressWarnings("unchecked")
	public T zeilePlusEins() {
		zeilePlus(1);
		return (T) this;
	}

	/**
	 * pos Zeile + x, <br>
	 * UND ! ggf endposmerge zeile + x
	 *
	 * @return AbstractCellValue
	 */
	@SuppressWarnings("unchecked")
	public T zeilePlus(int anz) {
		getPos().zeilePlus(anz);
		if (this.endPosMerge != null) {
			this.endPosMerge.zeilePlus(anz);
		}
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
		this.cellProperties.setCharWeight(fontWeight);
		return (T) this;
	}

	/**
	 * property "CharColor"
	 */
	@SuppressWarnings("unchecked")
	public T setCharColor(Integer charColor) {
		this.cellProperties.setCharColor(charColor);
		return (T) this;
	}

	/**
	 * @param hexCharColor = ein hex wert
	 */
	@SuppressWarnings("unchecked")
	public T setCharColor(String hexCharColor) {
		this.cellProperties.setCharColor(Integer.valueOf(StringUtils.strip(StringUtils.strip(hexCharColor), "#"), 16));
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T setCharHeight(int height) {
		this.cellProperties.setCharHeight(height);
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T centerJustify() {
		this.centerHoriJustify();
		this.centerVertJustify();
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T centerHoriJustify() {
		this.cellProperties.setHoriJustify(CellHoriJustify.CENTER);
		return (T) this;
	}

	/**
	 * com.sun.star.table.CellHoriJustify.class
	 *
	 * @param cellHoriJustify CellHoriJustify
	 */
	@SuppressWarnings("unchecked")
	public T setHoriJustify(CellHoriJustify cellHoriJustify) {
		this.cellProperties.setHoriJustify(cellHoriJustify);
		return (T) this;
	}

	/**
	 * Text in der Zelle wird an der Zelle Große angepasst
	 *
	 * @param boolean
	 */
	@SuppressWarnings("unchecked")
	public T setShrinkToFit(boolean shrink) {
		this.cellProperties.setShrinkToFit(shrink);
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T centerVertJustify() {
		this.cellProperties.setVertJustify(CellVertJustify2.CENTER);
		return (T) this;
	}

	/**
	 * @param vertjustify Type = CellVertJustify2
	 * @return
	 */

	@SuppressWarnings("unchecked")
	public T setVertJustify(int vertjustify) {
		this.cellProperties.setVertJustify(vertjustify);
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T removeCellBackColor() {
		this.cellProperties.removeCellBackColor();
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T setCellBackColor(Integer color) {
		this.cellProperties.setCellBackColor(color);
		return (T) this;
	}

	/**
	 * @param hexCharColor = ein hex wert
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public T setCellBackColor(String hexCharColor) {
		checkNotNull(hexCharColor);
		if (StringUtils.isNotBlank(hexCharColor)) {
			this.cellProperties
					.setCellBackColor(Integer.valueOf(StringUtils.strip(StringUtils.strip(hexCharColor), "#"), 16));
		}
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

	@SuppressWarnings("unchecked")
	public T setFillAutoDown(int zeile) {
		this.fillAuto = FillAutoPosition.from(getPos()).zeile(zeile);
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T setFillAutoDownZeilePlus(int anzZeile) {
		this.fillAuto = FillAutoPosition.from(getPos()).zeilePlus(anzZeile);
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T setAllThinBorder() {
		this.cellProperties.setAllThinBorder();
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T setBorder(TableBorder2 tableBorder2) {
		this.cellProperties.setBorder(tableBorder2);
		return (T) this;
	}

	/**
	 * rotiere 90 Grad in Uhrzeiger
	 * 
	 * @return
	 */
	public T setRotate90() {
		return setRotateAngle(ROTATEANGLE_PLUS_90);
	}

	/**
	 * ist in inspector als Typ Long ? aber muss als int, sonnst iligalargument exception<br>
	 * 27000 = 90 Grad
	 *
	 * @param angle
	 * @return
	 */

	@SuppressWarnings("unchecked")
	public T setRotateAngle(int angle) {
		this.cellProperties.setRotateAngle(angle);
		return (T) this;
	}

	// public CellProperties getColumnProperties() {
	// return this.columnProperties;
	// }
	//
	// @SuppressWarnings("unchecked")
	// public T setColumnProperties(CellProperties columnProperties) {
	// this.columnProperties = checkNotNull(columnProperties);
	// return (T) this;
	// }

	@SuppressWarnings("unchecked")
	public T addColumnProperty(String key, Object val) {
		checkNotNull(key);
		checkNotNull(val);
		this.columnProperties.put(key, val);
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T addColumnProperties(ColumnProperties columnProperties) {
		checkNotNull(columnProperties);
		this.columnProperties.putAll(columnProperties);
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T addRowProperty(String key, Object val) {
		this.rowProperties.put(key, val);
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T addRowProperties(RowProperties rowProperties) {
		this.rowProperties.putAll(rowProperties);
		return (T) this;
	}

	public boolean isUeberschreiben() {
		return ueberschreiben;
	}

	@SuppressWarnings("unchecked")
	public T nichtUeberschreiben() {
		this.ueberschreiben = false;
		return (T) this;
	}

	/**
	 * @return the columnProperties
	 */
	public final ColumnProperties getColumnProperties() {
		return columnProperties;
	}

	/**
	 * @param columnProperties
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public final T setColumnProperties(ColumnProperties columnProperties) {
		this.columnProperties = columnProperties;
		return (T) this;
	}

	/**
	 * @return the rowProperties
	 */
	public final RowProperties getRowProperties() {
		return rowProperties;
	}

	@SuppressWarnings("unchecked")
	public final T setRowProperties(RowProperties rowProperties) {
		this.rowProperties = rowProperties;
		return (T) this;
	}

	/**
	 * @return the cellProperties
	 */
	public final CellProperties getCellProperties() {
		return cellProperties;
	}

	@SuppressWarnings("unchecked")
	public final T setCellProperties(CellProperties cellProperties) {
		this.cellProperties = cellProperties;
		return (T) this;
	}

}
