/**
* Erstellung : 21.05.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.cellstyle;

import de.petanqueturniermanager.helper.ColorHelper;

public class RanglisteHintergrundFarbeUnGeradeCharRedStyle extends AbstractVordergrundHintergrundFarbeStyle {

	public RanglisteHintergrundFarbeUnGeradeCharRedStyle(Integer unGeradeColor) {
		super(CellStyleDefName.RanglisteHintergrundFarbeUnGeradeRed, ColorHelper.CHAR_COLOR_RED, unGeradeColor);
	}

}
