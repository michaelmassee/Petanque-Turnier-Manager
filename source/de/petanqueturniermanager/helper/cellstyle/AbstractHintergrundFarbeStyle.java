/**
* Erstellung : 21.05.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.cellstyle;

import static com.google.common.base.Preconditions.checkNotNull;

import de.petanqueturniermanager.helper.cellvalue.properties.CellProperties;

public abstract class AbstractHintergrundFarbeStyle extends AbstractCellStyleDef {

	public AbstractHintergrundFarbeStyle(String name, Integer color) {
		super(checkNotNull(name, "name=null"), buildCellProperties(checkNotNull(color, "color=null")));
	}

	static CellProperties buildCellProperties(Integer geradeColor) {
		return CellProperties.from().setCellBackColor(geradeColor).setCellbackgroundTransparent(false);
	}
}
