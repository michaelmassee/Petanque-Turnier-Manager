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
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.SpielTagNr;

/**
 * @author Michael Massee
 *
 */
public class LigaPropertiesSpalte extends BasePropertiesSpalte implements ILigaPropertiesSpalte {

	private static final List<ConfigProperty<?>> KONFIG_PROPERTIES = new ArrayList<>();

	private static final String KONFIG_PROP_SPIELPLAN_COLOR_BACK_GERADE = "Spielplan Hintergrund Gerade";
	private static final String KONFIG_PROP_SPIELPLAN_COLOR_BACK_UNGERADE = "Spielplan Hintergrund Ungerade";
	private static final String KONFIG_PROP_SPIELPLAN_COLOR_BACK_HEADER = "Spielplan Header";

	private static final String KONFIG_PROP_KOPF_ZEILE_LINKS = "Kopfzeile Links";
	private static final String KONFIG_PROP_KOPF_ZEILE_MITTE = "Kopfzeile Mitte";
	private static final String KONFIG_PROP_KOPF_ZEILE_RECHTS = "Kopfzeile Rechts";

	static {
		ADDBaseProp(KONFIG_PROPERTIES);
	}

	static {
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_SPIELPLAN_COLOR_BACK_GERADE).setDefaultVal(DEFAULT_GERADE_BACK_COLOR)
				.setDescription("Spielplan Hintergrundfarbe für gerade Zeilen"));
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_SPIELPLAN_COLOR_BACK_UNGERADE).setDefaultVal(DEFAULT_UNGERADE__BACK_COLOR)
				.setDescription("Spielplan Hintergrundfarbe für ungerade Zeilen"));
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_SPIELPLAN_COLOR_BACK_HEADER).setDefaultVal(DEFAULT_HEADER__BACK_COLOR)
				.setDescription("Spielplan Header-Hintergrundfarbe"));

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.STRING, KONFIG_PROP_KOPF_ZEILE_LINKS).setDefaultVal("").setDescription("Kopfzeile Links"));
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.STRING, KONFIG_PROP_KOPF_ZEILE_MITTE).setDefaultVal("").setDescription("Kopfzeile Mitte"));
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.STRING, KONFIG_PROP_KOPF_ZEILE_RECHTS).setDefaultVal("").setDescription("Kopfzeile Rechts"));
	}

	/**
	 * @param propertiesSpalte
	 * @param erstePropertiesZeile
	 * @param sheet
	 */
	LigaPropertiesSpalte(int propertiesSpalte, int erstePropertiesZeile, ISheet sheet) {
		super(propertiesSpalte, erstePropertiesZeile, sheet);
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
	public void setAktiveSpieltag(SpielTagNr spieltag) throws GenerateException {
		// nichts
	}

	@Override
	public SpielRundeNr getAktiveSpielRunde() throws GenerateException {
		return SpielRundeNr.from(1);
	}

	@Override
	public void setAktiveSpielRunde(SpielRundeNr neueSpielrunde) throws GenerateException {
		// Nichts
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

}
