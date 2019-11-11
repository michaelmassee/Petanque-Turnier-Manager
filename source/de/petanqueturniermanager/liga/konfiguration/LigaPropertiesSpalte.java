/**
 * Erstellung 10.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.liga.konfiguration;

import java.util.ArrayList;
import java.util.List;

import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.konfigdialog.ConfigProperty;
import de.petanqueturniermanager.supermelee.meldeliste.SpielSystem;

/**
 * @author Michael Massee
 *
 */
public class LigaPropertiesSpalte extends BasePropertiesSpalte implements ILigaPropertiesSpalte {

	private static final List<ConfigProperty<?>> KONFIG_PROPERTIES = new ArrayList<>();

	static {
		ADDSpielsystemProp(SpielSystem.LIGA, KONFIG_PROPERTIES);
		ADDBaseProp(KONFIG_PROPERTIES);
	}

	/**
	 * @param propertiesSpalte
	 * @param erstePropertiesZeile
	 * @param sheet
	 */
	protected LigaPropertiesSpalte(int propertiesSpalte, int erstePropertiesZeile, ISheet sheet) {
		super(propertiesSpalte, erstePropertiesZeile, sheet);
	}

	@Override
	protected List<ConfigProperty<?>> getKonfigProperties() {
		return KONFIG_PROPERTIES;
	}

}
