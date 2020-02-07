/**
 * Erstellung 08.05.2019 / Michael Massee
 */
package de.petanqueturniermanager.konfigdialog.properties;

import java.util.function.Predicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.konfigdialog.ConfigProperty;
import de.petanqueturniermanager.sidebar.config.headerfooter.HeaderFooterSidebarContent;

/**
 * @author Michael Massee
 */
public class KopfFusszeilenDialog extends BasePropertiesDialog {

	static final Logger logger = LogManager.getLogger(KopfFusszeilenDialog.class);

	public KopfFusszeilenDialog(WorkingSpreadsheet currentSpreadsheet) {
		super(currentSpreadsheet);
	}

	@Override
	protected Predicate<ConfigProperty<?>> getKonfigFieldFilter() {
		return HeaderFooterSidebarContent.HEADERFOOTER_FILTER;
	}

	@Override
	protected String getTitle() {
		return "Kopf/Fusszeilen";
	}
}
