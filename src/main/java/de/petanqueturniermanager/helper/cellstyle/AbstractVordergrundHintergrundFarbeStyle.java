/**
* Erstellung : 21.05.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.cellstyle;

import static com.google.common.base.Preconditions.checkNotNull;

import de.petanqueturniermanager.helper.cellvalue.properties.CellProperties;

public abstract class AbstractVordergrundHintergrundFarbeStyle extends AbstractCellStyleDef {

	/**
	 * @param name
	 * @param charColor, Character Color
	 * @param backgrColor, Background Color
	 */
	public AbstractVordergrundHintergrundFarbeStyle(CellStyleDefName name, Integer charColor, Integer backgrColor) {
		super(checkNotNull(name, "name=null"), buildCellProperties(checkNotNull(charColor, "charColor=null"),
				checkNotNull(backgrColor, "backgrColor=null")));
	}

	private static CellProperties buildCellProperties(Integer charColor, Integer backgrColor) {
		return CellProperties.from().setCharColor(charColor).setCellBackColor(backgrColor)
				.setCellbackgroundTransparent(false);
	}
}
