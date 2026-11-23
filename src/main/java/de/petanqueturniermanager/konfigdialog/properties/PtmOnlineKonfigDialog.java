package de.petanqueturniermanager.konfigdialog.properties;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.konfigdialog.ConfigProperty;

/** Einstellungen für die Live-Ansicht und die Ergebnismeldung im verbundenen Online-Turnier. */
public class PtmOnlineKonfigDialog extends BasePropertiesDialog {

	private static final List<String> EIGENSCHAFTEN = List.of(
			BasePropertiesSpalte.KONFIG_PROP_PTMONLINE_LIVE_ANSICHT,
			BasePropertiesSpalte.KONFIG_PROP_PTMONLINE_ERGEBNISMELDUNG,
			BasePropertiesSpalte.KONFIG_PROP_PTMONLINE_ERGEBNISSE_PRUEFEN);

	public PtmOnlineKonfigDialog(WorkingSpreadsheet currentSpreadsheet) {
		super(currentSpreadsheet);
	}

	static boolean istPtmOnlineEinstellung(ConfigProperty<?> property) {
		return EIGENSCHAFTEN.contains(property.getKey());
	}

	@Override
	protected Predicate<ConfigProperty<?>> getKonfigFieldFilter() {
		return PtmOnlineKonfigDialog::istPtmOnlineEinstellung;
	}

	@Override
	protected Comparator<ConfigProperty<?>> getKonfigFieldComparator() {
		return Comparator.comparingInt(property -> EIGENSCHAFTEN.indexOf(property.getKey()));
	}

	@Override
	protected int getBreite() {
		return 300;
	}

	@Override
	protected String getTitle() {
		return I18n.get("dialog.title.ptmonline.konfiguration");
	}
}
