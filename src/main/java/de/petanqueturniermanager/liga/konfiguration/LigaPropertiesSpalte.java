/**
 * Erstellung 10.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.liga.konfiguration;

import java.util.ArrayList;
import java.util.List;

import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.konfigdialog.ConfigProperty;
import de.petanqueturniermanager.konfigdialog.ConfigPropertyType;
import de.petanqueturniermanager.konfigdialog.HeaderFooterConfigProperty;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.SpielTagNr;

/**
 * @author Michael Massee
 *
 */
public class LigaPropertiesSpalte extends BasePropertiesSpalte implements ILigaPropertiesSpalte {

	public static final List<ConfigProperty<?>> KONFIG_PROPERTIES = new ArrayList<>();

	private static final String KONFIG_PROP_SPIELPLAN_COLOR_BACK_GERADE = "Spielplan Hintergrund Gerade";
	private static final String KONFIG_PROP_SPIELPLAN_COLOR_BACK_UNGERADE = "Spielplan Hintergrund Ungerade";
	private static final String KONFIG_PROP_SPIELPLAN_COLOR_BACK_HEADER = "Spielplan Header";

	private static final String KONFIG_PROP_KOPF_ZEILE_LINKS = "Kopfzeile Links";
	private static final String KONFIG_PROP_KOPF_ZEILE_MITTE = "Kopfzeile Mitte";
	private static final String KONFIG_PROP_KOPF_ZEILE_RECHTS = "Kopfzeile Rechts";

	// html export
	private static final String KONFIG_PROP_NAME_GRUPPE = "Gruppennamen";
	private static final String KONFIG_PROP_LOGO_URL = "Liga-Logo Url"; // (png)";
	private static final String KONFIG_PROP_DOWNLOAD_URL = "Download Url"; // fuer der Download von Spielpläne";

	static {
		ADDBaseProp(KONFIG_PROPERTIES);
	}

	static {
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_SPIELPLAN_COLOR_BACK_GERADE)
				.setDefaultVal(DEFAULT_GERADE_BACK_COLOR).setDescription("Spielplan Hintergrundfarbe für gerade Zeilen")
				.inSideBar());
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_SPIELPLAN_COLOR_BACK_UNGERADE)
				.setDefaultVal(DEFAULT_UNGERADE__BACK_COLOR)
				.setDescription("Spielplan Hintergrundfarbe für ungerade Zeilen").inSideBar());
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_SPIELPLAN_COLOR_BACK_HEADER)
				.setDefaultVal(DEFAULT_HEADER__BACK_COLOR).setDescription("Spielplan Header-Hintergrundfarbe")
				.inSideBar());

		KONFIG_PROPERTIES.add(HeaderFooterConfigProperty.from(KONFIG_PROP_KOPF_ZEILE_LINKS)
				.setDescription("Kopfzeile Links").inSideBar());
		KONFIG_PROPERTIES.add(HeaderFooterConfigProperty.from(KONFIG_PROP_KOPF_ZEILE_MITTE)
				.setDescription("Kopfzeile Mitte").inSideBar());
		KONFIG_PROPERTIES.add(HeaderFooterConfigProperty.from(KONFIG_PROP_KOPF_ZEILE_RECHTS)
				.setDescription("Kopfzeile Rechts").inSideBar());

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.STRING, KONFIG_PROP_NAME_GRUPPE).setDefaultVal("")
				.setDescription("Name der Gruppe in dieses Dokument.").inSideBar());

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.STRING, KONFIG_PROP_LOGO_URL).setDefaultVal("")
				.setDescription("Url zur Liga Logo Datei").inSideBar());

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.STRING, KONFIG_PROP_DOWNLOAD_URL).setDefaultVal("")
				.setDescription("Bases Url fuer den Download von Spielpläne, Ranglisten, etc..").inSideBar());
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
	public SpielTagNr getAktiveSpieltag() throws GenerateException {
		return SpielTagNr.from(1);
	}

	@Override
	public SpielRundeNr getAktiveSpielRunde() throws GenerateException {
		return SpielRundeNr.from(1);
	}

	@Override
	public Integer getSpielPlanHintergrundFarbeGerade() throws GenerateException {
		return readCellBackColorProperty(KONFIG_PROP_SPIELPLAN_COLOR_BACK_GERADE);
	}

	@Override
	public Integer getSpielPlanHintergrundFarbeUnGerade() throws GenerateException {
		return readCellBackColorProperty(KONFIG_PROP_SPIELPLAN_COLOR_BACK_UNGERADE);
	}

	@Override
	public Integer getSpielPlanHeaderFarbe() throws GenerateException {
		return readCellBackColorProperty(KONFIG_PROP_SPIELPLAN_COLOR_BACK_HEADER);
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
	public String getGruppennamen() throws GenerateException {
		return readStringProperty(KONFIG_PROP_NAME_GRUPPE);
	}

	@Override
	public String getBaseDownloadUrl() throws GenerateException {
		return readStringProperty(KONFIG_PROP_DOWNLOAD_URL);
	}

	@Override
	public String getLigaLogoUr() throws GenerateException {
		return readStringProperty(KONFIG_PROP_LOGO_URL);
	}

}
