/**
 * Erstellung 10.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.liga.konfiguration;

import java.util.ArrayList;
import java.util.List;

import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.konfigdialog.ConfigProperty;
import de.petanqueturniermanager.konfigdialog.ConfigPropertyType;
import de.petanqueturniermanager.konfigdialog.HeaderFooterConfigProperty;

/**
 * @author Michael Massee
 *
 */
public class LigaPropertiesSpalte extends BasePropertiesSpalte implements ILigaPropertiesSpalte {

	public static final List<ConfigProperty<?>> KONFIG_PROPERTIES = new ArrayList<>();

	public static final String KONFIG_PROP_FREISPIEL_PUNKTE_PLUS  = "Freispiel Punkte +";
	public static final String KONFIG_PROP_FREISPIEL_PUNKTE_MINUS = "Freispiel Punkte -";

	private static final String KONFIG_PROP_SPIELPLAN_COLOR_BACK_GERADE = "Spielplan Hintergrund Gerade";
	private static final String KONFIG_PROP_SPIELPLAN_COLOR_BACK_UNGERADE = "Spielplan Hintergrund Ungerade";
	private static final String KONFIG_PROP_SPIELPLAN_COLOR_BACK_HEADER = "Spielplan Header";

	private static final String KONFIG_PROP_KOPF_ZEILE_LINKS = "Kopfzeile Links";
	private static final String KONFIG_PROP_KOPF_ZEILE_MITTE = "Kopfzeile Mitte";
	private static final String KONFIG_PROP_KOPF_ZEILE_RECHTS = "Kopfzeile Rechts";

	// html export
	public static final String KONFIG_PROP_NAME_GRUPPE = "Gruppenname";
	public static final String KONFIG_PROP_LOGO_URL = "Liga-Logo Url"; // (png)";
	public static final String KONFIG_PROP_DOWNLOAD_URL = "Download Url"; // fuer den Download von Spielpläne";

	static {
		ADDBaseProp(KONFIG_PROPERTIES);
	}

	static {
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_SPIELPLAN_COLOR_BACK_GERADE)
				.setDefaultVal(DEFAULT_GERADE_BACK_COLOR).setDescription("config.desc.liga.spielplan.gerade")
				.inSideBar());
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_SPIELPLAN_COLOR_BACK_UNGERADE)
				.setDefaultVal(DEFAULT_UNGERADE_BACK_COLOR)
				.setDescription("config.desc.liga.spielplan.ungerade").inSideBar());
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_SPIELPLAN_COLOR_BACK_HEADER)
				.setDefaultVal(DEFAULT_HEADER_BACK_COLOR).setDescription("config.desc.liga.spielplan.header")
				.inSideBar());

		KONFIG_PROPERTIES.add(HeaderFooterConfigProperty.from(KONFIG_PROP_KOPF_ZEILE_LINKS)
				.setDescription("config.desc.header.links").inSideBar());
		KONFIG_PROPERTIES.add(HeaderFooterConfigProperty.from(KONFIG_PROP_KOPF_ZEILE_MITTE)
				.setDescription("config.desc.header.mitte").inSideBar());
		KONFIG_PROPERTIES.add(HeaderFooterConfigProperty.from(KONFIG_PROP_KOPF_ZEILE_RECHTS)
				.setDescription("config.desc.header.rechts").inSideBar());

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.STRING, KONFIG_PROP_NAME_GRUPPE).setDefaultVal("")
				.setDescription("config.desc.liga.gruppenname").inSideBar());

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.STRING, KONFIG_PROP_LOGO_URL).setDefaultVal("")
				.setDescription("config.desc.liga.logo.url"));

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.STRING, KONFIG_PROP_DOWNLOAD_URL).setDefaultVal("")
				.setDescription("config.desc.liga.download.url")
				.inSideBar());

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.INTEGER, KONFIG_PROP_FREISPIEL_PUNKTE_PLUS)
				.setDefaultVal(13).setDescription("config.desc.freispiel.punkte.plus").inSideBar());
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.INTEGER, KONFIG_PROP_FREISPIEL_PUNKTE_MINUS)
				.setDefaultVal(7).setDescription("config.desc.freispiel.punkte.minus").inSideBar());
	}

	/**
	 * @param propertiesSpalte
	 * @param erstePropertiesZeile
	 * @param sheet
	 */
	LigaPropertiesSpalte(ISheet sheet) {
		super(sheet);
	}

	@Override
	protected List<ConfigProperty<?>> getKonfigProperties() {
		return KONFIG_PROPERTIES;
	}

	@Override
	public Integer getSpielPlanHintergrundFarbeGerade() {
		return readCellBackColorProperty(KONFIG_PROP_SPIELPLAN_COLOR_BACK_GERADE);
	}

	@Override
	public Integer getSpielPlanHintergrundFarbeUnGerade() {
		return readCellBackColorProperty(KONFIG_PROP_SPIELPLAN_COLOR_BACK_UNGERADE);
	}

	@Override
	public Integer getSpielPlanHeaderFarbe() {
		return readCellBackColorProperty(KONFIG_PROP_SPIELPLAN_COLOR_BACK_HEADER);
	}

	@Override
	public String getKopfZeileLinks() {
		return readStringProperty(KONFIG_PROP_KOPF_ZEILE_LINKS);
	}

	@Override
	public String getKopfZeileMitte() {
		return readStringProperty(KONFIG_PROP_KOPF_ZEILE_MITTE);
	}

	public void setKopfZeileMitte(String text) {
		setStringProperty(KONFIG_PROP_KOPF_ZEILE_MITTE, text);
	}

	@Override
	public String getKopfZeileRechts() {
		return readStringProperty(KONFIG_PROP_KOPF_ZEILE_RECHTS);
	}

	@Override
	public String getGruppenname() {
		return readStringProperty(KONFIG_PROP_NAME_GRUPPE);
	}

	@Override
	public void setGruppenname(String name) {
		setStringProperty(KONFIG_PROP_NAME_GRUPPE, name);
	}

	@Override
	public String getBaseDownloadUrl() {
		return readStringProperty(KONFIG_PROP_DOWNLOAD_URL);
	}

	@Override
	public String getLigaLogoUr() {
		return readStringProperty(KONFIG_PROP_LOGO_URL);
	}

	@Override
	public Integer getFreispielPunktePlus() {
		return readIntProperty(KONFIG_PROP_FREISPIEL_PUNKTE_PLUS);
	}

	@Override
	public Integer getFreispielPunkteMinus() {
		return readIntProperty(KONFIG_PROP_FREISPIEL_PUNKTE_MINUS);
	}

}
