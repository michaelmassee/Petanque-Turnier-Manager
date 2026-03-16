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
import de.petanqueturniermanager.sidebar.config.headerfooter.HeaderFooterSidebarContent;

/**
 * @author Michael Massee
 */
public class TurnierDialog extends BasePropertiesDialog {

	static final Logger logger = LogManager.getLogger(TurnierDialog.class);

	public TurnierDialog(WorkingSpreadsheet currentSpreadsheet) {
		super(currentSpreadsheet);
	}

	@Override
	protected Predicate<ConfigProperty<?>> getKonfigFieldFilter() {
		// alles nicht color oder header - footer
		return ColorSidebarContent.COLOR_FILTER.negate().and(HeaderFooterSidebarContent.HEADERFOOTER_FILTER.negate());
	}

	@Override
	protected String getTitle() {
		return "Turnier Konfiguration";
	}

}
