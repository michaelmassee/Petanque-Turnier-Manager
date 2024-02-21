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

	private static final String KONFIG_PROP_SPIELPLAN_COLOR_BACK_GERADE = "Spielplan Hintergrund Gerade";
	private static final String KONFIG_PROP_SPIELPLAN_COLOR_BACK_UNGERADE = "Spielplan Hintergrund Ungerade";
	private static final String KONFIG_PROP_SPIELPLAN_COLOR_BACK_HEADER = "Spielplan Header";

	private static final String KONFIG_PROP_KOPF_ZEILE_LINKS = "Kopfzeile Links";
	private static final String KONFIG_PROP_KOPF_ZEILE_MITTE = "Kopfzeile Mitte";
	private static final String KONFIG_PROP_KOPF_ZEILE_RECHTS = "Kopfzeile Rechts";

	// html export
	public static final String KONFIG_PROP_NAME_GRUPPE = "Gruppenname";
	public static final String KONFIG_PROP_LOGO_URL = "Liga-Logo Url"; // (png)";
	public static final String KONFIG_PROP_PDF_IMG_URL = "Pdf-Img Url"; // (png)";
	public static final String KONFIG_PROP_DOWNLOAD_URL = "Download Url"; // fuer der Download von Spielpläne";

	static {
		ADDBaseProp(KONFIG_PROPERTIES);
	}

	static {
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_SPIELPLAN_COLOR_BACK_GERADE)
				.setDefaultVal(DEFAULT_GERADE_BACK_COLOR).setDescription("Spielplan Hintergrundfarbe für gerade Zeilen")
				.inSideBar());
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_SPIELPLAN_COLOR_BACK_UNGERADE)
				.setDefaultVal(DEFAULT_UNGERADE_BACK_COLOR)
				.setDescription("Spielplan Hintergrundfarbe für ungerade Zeilen").inSideBar());
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_SPIELPLAN_COLOR_BACK_HEADER)
				.setDefaultVal(DEFAULT_HEADER_BACK_COLOR).setDescription("Spielplan Header-Hintergrundfarbe")
				.inSideBar());

		KONFIG_PROPERTIES.add(HeaderFooterConfigProperty.from(KONFIG_PROP_KOPF_ZEILE_LINKS)
				.setDescription(KONFIG_PROP_KOPF_ZEILE_LINKS).inSideBar());
		KONFIG_PROPERTIES.add(HeaderFooterConfigProperty.from(KONFIG_PROP_KOPF_ZEILE_MITTE)
				.setDescription(KONFIG_PROP_KOPF_ZEILE_MITTE).inSideBar());
		KONFIG_PROPERTIES.add(HeaderFooterConfigProperty.from(KONFIG_PROP_KOPF_ZEILE_RECHTS)
				.setDescription(KONFIG_PROP_KOPF_ZEILE_RECHTS).inSideBar());

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.STRING, KONFIG_PROP_NAME_GRUPPE).setDefaultVal("")
				.setDescription("Name der Gruppe in dieses Dokument.").inSideBar());

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.STRING, KONFIG_PROP_LOGO_URL).setDefaultVal("")
				.setDescription("Url zur Liga Logo Datei"));

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.STRING, KONFIG_PROP_PDF_IMG_URL)
				.setDefaultVal("http://bc-linden.de/images/bclinden/pdf-download.png")
				.setDescription("Url zur Pdf Download-Image"));

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
	public String getPdfImageUr() {
		return readStringProperty(KONFIG_PROP_PDF_IMG_URL);
	}

}
