/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.konfigdialog.properties;

import java.util.function.Predicate;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.konfigdialog.ConfigProperty;
import de.petanqueturniermanager.konfigdialog.ZeitplanConfigProperty;

/**
 * @author Michael Massee
 */
public class ZeitplanDialog extends BasePropertiesDialog {

	public ZeitplanDialog(WorkingSpreadsheet currentSpreadsheet) {
		super(currentSpreadsheet);
	}

	@Override
	protected Predicate<ConfigProperty<?>> getKonfigFieldFilter() {
		return konfigprop -> konfigprop instanceof ZeitplanConfigProperty;
	}

	@Override
	protected String getTitle() {
		return I18n.get("dialog.title.zeitplan");
	}
}
