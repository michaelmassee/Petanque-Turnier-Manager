/**
 * Erstellung 08.05.2019 / Michael Massee
 */
package de.petanqueturniermanager.konfigdialog.properties;

import java.util.function.Predicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.konfigdialog.ConfigProperty;
import de.petanqueturniermanager.konfigdialog.ConfigPropertyType;

/**
 * wird von Protokollhandler verwendet <br>
 * TODO ist Abgeschaltet
 * 
 * @author Michael Massee
 */
public class ExtendedPropertiesDialog extends BasePropertiesDialog {

	public static final Predicate<ConfigProperty<?>> SPIELTAG_FILTER = konfigprop -> konfigprop
			.getType() == ConfigPropertyType.INTEGER
			&& (konfigprop.getKey().equals(BasePropertiesSpalte.KONFIG_PROP_NAME_SPIELTAG));

	public static final Predicate<ConfigProperty<?>> SPIELRUNDE_FILTER = konfigprop -> konfigprop
			.getType() == ConfigPropertyType.INTEGER
			&& (konfigprop.getKey().equals(BasePropertiesSpalte.KONFIG_PROP_NAME_SPIELRUNDE));

	static final Logger logger = LogManager.getLogger(ExtendedPropertiesDialog.class);

	public ExtendedPropertiesDialog(WorkingSpreadsheet currentSpreadsheet) {
		super(currentSpreadsheet);
	}

	@Override
	protected Predicate<ConfigProperty<?>> getKonfigFieldFilter() {
		return SPIELTAG_FILTER.or(SPIELRUNDE_FILTER);
	}

	@Override
	protected String getTitle() {
		return "Extended";
	}

}
