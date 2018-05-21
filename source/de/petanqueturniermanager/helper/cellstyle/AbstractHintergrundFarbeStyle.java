/**
* Erstellung : 21.05.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.cellstyle;

import de.petanqueturniermanager.helper.cellvalue.CellProperties;

public abstract class AbstractHintergrundFarbeStyle extends AbstractCellStyleDef {

	public AbstractHintergrundFarbeStyle(String name, Integer color) {
		super(name, buildCellProperties(color));
	}

	static CellProperties buildCellProperties(Integer geradeColor) {
		return CellProperties.from().setCellBackColor(geradeColor).setCellbackgroundTransparent(false);
	}
}
