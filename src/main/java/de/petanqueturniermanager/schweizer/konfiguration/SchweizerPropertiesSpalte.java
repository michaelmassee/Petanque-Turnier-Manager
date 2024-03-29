/**
 * Erstellung 12.04.2020 / Michael Massee
 */
package de.petanqueturniermanager.schweizer.konfiguration;

import java.util.ArrayList;
import java.util.List;

import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.konfigdialog.ConfigProperty;
import de.petanqueturniermanager.konfigdialog.ConfigPropertyType;
import de.petanqueturniermanager.konfigdialog.HeaderFooterConfigProperty;
import de.petanqueturniermanager.supermelee.SpielRundeNr;

/**
 * @author Michael Massee
 *
 */
public class SchweizerPropertiesSpalte extends BasePropertiesSpalte implements ISchweizerPropertiesSpalte {

	public static final List<ConfigProperty<?>> KONFIG_PROPERTIES = new ArrayList<>();

	private static final String KONFIG_PROP_KOPF_ZEILE_LINKS = "Kopfzeile Links";
	private static final String KONFIG_PROP_KOPF_ZEILE_MITTE = "Kopfzeile Mitte";
	private static final String KONFIG_PROP_KOPF_ZEILE_RECHTS = "Kopfzeile Rechts";
	public static final String KONFIG_PROP_NAME_SPIELRUNDE = "Spielrunde";

	static {
		ADDBaseProp(KONFIG_PROPERTIES);
	}

	static {

		KONFIG_PROPERTIES.add(HeaderFooterConfigProperty.from(KONFIG_PROP_KOPF_ZEILE_LINKS)
				.setDescription(KONFIG_PROP_KOPF_ZEILE_LINKS).inSideBar());
		KONFIG_PROPERTIES.add(HeaderFooterConfigProperty.from(KONFIG_PROP_KOPF_ZEILE_MITTE)
				.setDescription(KONFIG_PROP_KOPF_ZEILE_MITTE).inSideBar());
		KONFIG_PROPERTIES.add(HeaderFooterConfigProperty.from(KONFIG_PROP_KOPF_ZEILE_RECHTS)
				.setDescription(KONFIG_PROP_KOPF_ZEILE_RECHTS).inSideBar());

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.INTEGER, KONFIG_PROP_NAME_SPIELRUNDE)
				.setDefaultVal(1).setDescription("Aktuelle Spielrunde").inSideBarInfoPanel());
	}

	/**
	 * @param propertiesSpalte
	 * @param erstePropertiesZeile
	 * @param sheet
	 */
	SchweizerPropertiesSpalte(ISheet sheet) {
		super(sheet);
	}

	@Override
	protected List<ConfigProperty<?>> getKonfigProperties() {
		return KONFIG_PROPERTIES;
	}

	@Override
	public String getKopfZeileLinks() throws GenerateException {
		return readStringProperty(KONFIG_PROP_KOPF_ZEILE_LINKS);
	}

	@Override
	public String getKopfZeileMitte() throws GenerateException {
		return readStringProperty(KONFIG_PROP_KOPF_ZEILE_MITTE);
	}

	@Override
	public String getKopfZeileRechts() throws GenerateException {
		return readStringProperty(KONFIG_PROP_KOPF_ZEILE_RECHTS);
	}

	@Override
	public final void setAktiveSpielRunde(SpielRundeNr spielrunde) throws GenerateException {
		writeIntProperty(KONFIG_PROP_NAME_SPIELRUNDE, spielrunde.getNr());
	}

}
