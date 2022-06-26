/**
* Erstellung : 21.05.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.cellstyle;

import static com.google.common.base.Preconditions.checkNotNull;

import de.petanqueturniermanager.helper.cellvalue.properties.CellProperties;

public abstract class AbstractCellStyleDef {

	private final CellStyleDefName name;
	private final CellProperties cellProperties;

	public AbstractCellStyleDef(CellStyleDefName name, CellProperties cellProperties) {
		this.name = checkNotNull(name, "name == null");
		this.cellProperties = checkNotNull(cellProperties, "cellProperties == null");
	}

	public String getName() {
		return name.name();
	}

	public CellProperties getCellProperties() {
		return cellProperties;
	}

}
