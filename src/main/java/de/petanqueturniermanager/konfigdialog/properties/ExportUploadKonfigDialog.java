package de.petanqueturniermanager.konfigdialog.properties;

import java.util.function.Predicate;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.konfigdialog.ConfigProperty;

/**
 * Dialog für Export- und Upload-Einstellungen (Ordner, FTP, SFTP).
 */
public class ExportUploadKonfigDialog extends BasePropertiesDialog {

	public ExportUploadKonfigDialog(WorkingSpreadsheet currentSpreadsheet) {
		super(currentSpreadsheet);
	}

	@Override
	protected Predicate<ConfigProperty<?>> getKonfigFieldFilter() {
		return ConfigProperty::isExportKonfig;
	}

	@Override
	protected String getTitle() {
		return I18n.get("dialog.title.export.upload.konfiguration");
	}
}
