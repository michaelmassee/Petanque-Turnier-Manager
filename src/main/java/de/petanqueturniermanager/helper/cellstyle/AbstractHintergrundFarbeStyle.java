/**
* Erstellung : 21.05.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.cellstyle;

import static com.google.common.base.Preconditions.checkNotNull;

import de.petanqueturniermanager.helper.cellvalue.properties.CellProperties;

public abstract class AbstractHintergrundFarbeStyle extends AbstractCellStyleDef {

	public AbstractHintergrundFarbeStyle(CellStyleDefName name, Integer color) {
		super(checkNotNull(name, "name=null"), buildCellProperties(checkNotNull(color, "color=null")));
	}

	private static CellProperties buildCellProperties(Integer backgrColor) {
		return CellProperties.from().setCellBackColor(backgrColor).setCellbackgroundTransparent(false);
	}
}
