/**
* Erstellung : 21.05.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.cellstyle;

import de.petanqueturniermanager.helper.ColorHelper;

public class RanglisteHintergrundFarbeUnGeradeCharOrangeStyle extends AbstractVordergrundHintergrundFarbeStyle {

	public RanglisteHintergrundFarbeUnGeradeCharOrangeStyle(Integer unGeradeColor) {
		super(CellStyleDefName.RanglisteHintergrundFarbeUnGeradeOrange, ColorHelper.CHAR_COLOR_ORANGE, unGeradeColor);
	}

}
