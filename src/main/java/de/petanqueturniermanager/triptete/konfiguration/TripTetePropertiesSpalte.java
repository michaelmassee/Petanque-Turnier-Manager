package de.petanqueturniermanager.triptete.konfiguration;

import java.util.ArrayList;
import java.util.List;

import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.konfigdialog.AuswahlConfigProperty;
import de.petanqueturniermanager.konfigdialog.ConfigProperty;
import de.petanqueturniermanager.konfigdialog.ConfigPropertyType;
import de.petanqueturniermanager.konfigdialog.HeaderFooterConfigProperty;

/**
 * Properties-Spalte für die Trip-Tête-Konfiguration.
 */
public class TripTetePropertiesSpalte extends BasePropertiesSpalte implements ITripTetePropertiesSpalte {

	public static final List<ConfigProperty<?>> KONFIG_PROPERTIES = new ArrayList<>();

	public static final String KONFIG_PROP_SPIELZIEL = "Punktegrenze Partie";

	private static final String KONFIG_PROP_MELDELISTE_TEAMNAME    = "Meldeliste Teamname";
	private static final String KONFIG_PROP_MELDELISTE_VEREINSNAME = "Meldeliste Vereinsname";

	private static final String KONFIG_PROP_SPIELPLAN_COLOR_BACK_GERADE = "Spielplan Hintergrund Gerade";
	private static final String KONFIG_PROP_SPIELPLAN_COLOR_BACK_UNGERADE = "Spielplan Hintergrund Ungerade";
	private static final String KONFIG_PROP_SPIELPLAN_COLOR_BACK_HEADER = "Spielplan Header";

	private static final String KONFIG_PROP_KOPF_ZEILE_LINKS = "Kopfzeile Links";
	private static final String KONFIG_PROP_KOPF_ZEILE_MITTE = "Kopfzeile Mitte";
	private static final String KONFIG_PROP_KOPF_ZEILE_RECHTS = "Kopfzeile Rechts";

	static {
		ADDBaseProp(KONFIG_PROPERTIES);

		KONFIG_PROPERTIES.add(((AuswahlConfigProperty) AuswahlConfigProperty.from(KONFIG_PROP_MELDELISTE_TEAMNAME)
				.setDefaultVal("N").setDescription("config.desc.meldeliste.teamname"))
				.addAuswahl("J", "Ja").addAuswahl("N", "Nein"));

		KONFIG_PROPERTIES.add(((AuswahlConfigProperty) AuswahlConfigProperty.from(KONFIG_PROP_MELDELISTE_VEREINSNAME)
				.setDefaultVal("N").setDescription("config.desc.schweizer.vereinsname"))
				.addAuswahl("J", "Ja").addAuswahl("N", "Nein"));

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_SPIELPLAN_COLOR_BACK_GERADE)
				.setDefaultVal(DEFAULT_GERADE_BACK_COLOR).setDescription("config.desc.triptete.spielplan.gerade"));
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_SPIELPLAN_COLOR_BACK_UNGERADE)
				.setDefaultVal(DEFAULT_UNGERADE_BACK_COLOR)
				.setDescription("config.desc.triptete.spielplan.ungerade"));
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_SPIELPLAN_COLOR_BACK_HEADER)
				.setDefaultVal(DEFAULT_HEADER_BACK_COLOR).setDescription("config.desc.triptete.spielplan.header"));

		KONFIG_PROPERTIES.add(HeaderFooterConfigProperty.from(KONFIG_PROP_KOPF_ZEILE_LINKS)
				.setDescription("config.desc.header.links"));
		KONFIG_PROPERTIES.add(HeaderFooterConfigProperty.from(KONFIG_PROP_KOPF_ZEILE_MITTE)
				.setDescription("config.desc.header.mitte"));
		KONFIG_PROPERTIES.add(HeaderFooterConfigProperty.from(KONFIG_PROP_KOPF_ZEILE_RECHTS)
				.setDescription("config.desc.header.rechts"));

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.INTEGER, KONFIG_PROP_SPIELZIEL)
				.setDefaultVal(13).setDescription("config.desc.triptete.spielziel"));
	}

	TripTetePropertiesSpalte(ISheet sheet) {
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
	public Integer getSpielZiel() {
		return readIntProperty(KONFIG_PROP_SPIELZIEL);
	}

	@Override
	public boolean isMeldeListeTeamnameAnzeigen() {
		return "J".equalsIgnoreCase(readStringProperty(KONFIG_PROP_MELDELISTE_TEAMNAME));
	}

	@Override
	public boolean isMeldeListeVereinsnameAnzeigen() {
		return "J".equalsIgnoreCase(readStringProperty(KONFIG_PROP_MELDELISTE_VEREINSNAME));
	}
}
