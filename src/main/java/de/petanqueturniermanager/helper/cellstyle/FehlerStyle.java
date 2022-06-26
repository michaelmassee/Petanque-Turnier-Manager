/**
* Erstellung : 21.05.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.cellstyle;

import de.petanqueturniermanager.helper.ColorHelper;
import de.petanqueturniermanager.helper.cellvalue.properties.CellProperties;

public class FehlerStyle extends AbstractCellStyleDef {

	public FehlerStyle() {
		super(CellStyleDefName.IstFehler, buildCellProperties());
	}

	private static CellProperties buildCellProperties() {
		return CellProperties.from().setCellBackColor(ColorHelper.CHAR_COLOR_RED).setCellbackgroundTransparent(false);
	}
}
