/**
 * Erstellung 08.05.2019 / Michael Massee
 */
package de.petanqueturniermanager.konfigdialog.properties;

import java.util.function.Predicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.konfigdialog.ConfigProperty;
import de.petanqueturniermanager.sidebar.config.color.ColorSidebarContent;

/**
 * @author Michael Massee
 */
public class FarbenDialog extends BasePropertiesDialog {

	static final Logger logger = LogManager.getLogger(FarbenDialog.class);

	public FarbenDialog(WorkingSpreadsheet currentSpreadsheet) {
		super(currentSpreadsheet);
	}

	@Override
	protected Predicate<ConfigProperty<?>> getKonfigFieldFilter() {
		return ColorSidebarContent.COLOR_FILTER;
	}

	@Override
	protected String getTitle() {
		return "Farben";
	}

}
