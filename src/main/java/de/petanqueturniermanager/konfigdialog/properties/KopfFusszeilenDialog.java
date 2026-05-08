/*
 * Erstellung 08.05.2019 / Michael Massee
 */
package de.petanqueturniermanager.konfigdialog.properties;

import java.util.Comparator;
import java.util.function.Predicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.konfigdialog.ConfigProperty;
import de.petanqueturniermanager.konfigdialog.HeaderFooterConfigProperty;

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
		return konfigprop -> konfigprop instanceof HeaderFooterConfigProperty;
	}

	/**
	 * Reihenfolge im Dialog: Kopfzeilen oben, Fußzeilen unten — unabhängig davon, in welcher
	 * Reihenfolge die einzelnen Turniersystem-Konfigurationen die Properties registrieren.
	 * Innerhalb einer Gruppe bleibt die Original-Reihenfolge erhalten (stabile Sortierung).
	 */
	@Override
	protected Comparator<ConfigProperty<?>> getKonfigFieldComparator() {
		return Comparator.comparingInt(konfigprop -> {
			String key = konfigprop.getKey();
			if (key == null) {
				return 1;
			}
			String lower = key.toLowerCase();
			if (lower.startsWith("kopfzeile") || lower.startsWith("kopf zeile") || lower.startsWith("kopf-zeile")) {
				return 0;
			}
			return 1;
		});
	}

	@Override
	protected String getTitle() {
		return I18n.get("dialog.title.kopf.fusszeilen");
	}
}
