/**
* Erstellung : 21.05.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.cellstyle;

import de.petanqueturniermanager.helper.ColorHelper;

public class RanglisteHintergrundFarbeGeradeStyleRed extends AbstractVordergrundHintergrundFarbeStyle {

	public RanglisteHintergrundFarbeGeradeStyleRed(Integer geradeColor) {
		super(CellStyleDefName.RanglisteHintergrundFarbeGeradeGreen, geradeColor, ColorHelper.CHAR_COLOR_RED);
	}

}
