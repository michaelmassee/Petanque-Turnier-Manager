/**
* Erstellung : 21.05.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.cellstyle;

import de.petanqueturniermanager.helper.ColorHelper;

public class RanglisteHintergrundFarbeUnGeradeGreenStyle extends AbstractVordergrundHintergrundFarbeStyle {

	public RanglisteHintergrundFarbeUnGeradeGreenStyle(Integer unGeradeColor) {
		super(CellStyleDefName.RanglisteHintergrundFarbeUnGeradeGreen, ColorHelper.CHAR_COLOR_GREEN, unGeradeColor);
	}

}
