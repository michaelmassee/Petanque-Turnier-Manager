/**
* Erstellung : 21.05.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.cellstyle;

import de.petanqueturniermanager.helper.ColorHelper;

public class RanglisteHintergrundFarbeGeradeCharRedStyle extends AbstractVordergrundHintergrundFarbeStyle {

	public RanglisteHintergrundFarbeGeradeCharRedStyle(Integer geradeColor) {
		super(CellStyleDefName.RanglisteHintergrundFarbeGeradeRed, ColorHelper.CHAR_COLOR_RED, geradeColor);
	}

}
