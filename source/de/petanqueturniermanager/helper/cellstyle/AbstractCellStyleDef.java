/**
* Erstellung : 21.05.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.cellstyle;

import static com.google.common.base.Preconditions.*;

import de.petanqueturniermanager.helper.cellvalue.CellProperties;

public abstract class AbstractCellStyleDef {

	private final String name;
	private final CellProperties cellProperties;

	public AbstractCellStyleDef(String name, CellProperties cellProperties) {
		checkNotNull(name);
		checkNotNull(cellProperties);
		this.name = name;
		this.cellProperties = cellProperties;
	}

	public String getName() {
		return this.name;
	}

	public CellProperties getCellProperties() {
		return this.cellProperties;
	}

}
