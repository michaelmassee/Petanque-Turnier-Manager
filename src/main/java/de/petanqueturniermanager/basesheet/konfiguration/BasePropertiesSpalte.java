/**
 * Erstellung 10.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.konfiguration;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import com.google.common.annotations.VisibleForTesting;
import com.sun.star.table.TableBorder2;

import de.petanqueturniermanager.basesheet.SheetTabFarben;
import de.petanqueturniermanager.comp.GlobalProperties;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.StringTools;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellstyle.MeldungenHintergrundFarbeGeradeStyle;
import de.petanqueturniermanager.helper.cellstyle.MeldungenHintergrundFarbeUnGeradeStyle;
import de.petanqueturniermanager.helper.sheet.WeakRefHelper;
import de.petanqueturniermanager.konfigdialog.ConfigProperty;
import de.petanqueturniermanager.konfigdialog.ConfigPropertyType;
import de.petanqueturniermanager.konfigdialog.HeaderFooterConfigProperty;

/**
 * @author Michael Massee
 *
 */
public abstract class BasePropertiesSpalte implements IPropertiesSpalte {

	public static final int PROP_CELL_MARGIN = 150;
	public static final int HEADER_HEIGHT = 800;
	public static final TableBorder2 HEADER_BORDER = BorderFactory.from().allThin().boldLn().forBottom().toBorder();
	public static final String HEADER_BACK_COLOR = "#dedbd3";
	public static final Integer DEFAULT_GERADE_BACK_COLOR = Integer.valueOf("e1e9f7", 16);
	public static final Integer DEFAULT_UNGERADE_BACK_COLOR = Integer.valueOf("c0d6f7", 16);
	public static final Integer DEFAULT_HEADER_BACK_COLOR = Integer.valueOf("e6ebf4", 16);

	public static final String KONFIG_PROP_NAME_TURNIERSYSTEM = "Turniersystem";
	public static final String KONFIG_PROP_ERSTELLT_MIT_VERSION = "Erstellt mit Version";
	public static final String KONFIG_PROP_NAME_TURNIER_MODUS = "TurnierModus";

	public static final String KONFIG_PROP_MELDELISTE_COLOR_BACK_GERADE = "Meldeliste Hintergrund Gerade";
	public static final String KONFIG_PROP_MELDELISTE_COLOR_BACK_UNGERADE = "Meldeliste Hintergrund Ungerade";
	public static final String KONFIG_PROP_MELDELISTE_COLOR_BACK_HEADER = "Meldeliste Header";

	public static final String KONFIG_PROP_RANGLISTE_COLOR_BACK_GERADE = "Rangliste Hintergrund Gerade";
	public static final String KONFIG_PROP_RANGLISTE_COLOR_BACK_UNGERADE = "Rangliste Hintergrund Ungerade";
	public static final String KONFIG_PROP_RANGLISTE_COLOR_BACK_HEADER = "Rangliste Header";

	public static final String KONFIG_PROP_FUSSZEILE_LINKS = "Fußzeile links";
	public static final String KONFIG_PROP_FUSSZEILE_MITTE = "Fußzeile mitte";

	public static final String KONFIG_PROP_ANZ_TEILNEHMER_IN_SPALTE = "Teilnehmerliste Anzahl je Spalte";
	private static final int DEFAULT_ANZ_TEILNEHMER_IN_SPALTE = 40;

	// Tab-Farben (Document Properties Schlüssel)
	public static final String KONFIG_PROP_TAB_COLOR_MELDELISTE      = "Tab-Farbe Meldeliste";
	public static final String KONFIG_PROP_TAB_COLOR_TEILNEHMER      = "Tab-Farbe Teilnehmer";
	public static final String KONFIG_PROP_TAB_COLOR_SPIELRUNDE      = "Tab-Farbe Spielrunde";
	public static final String KONFIG_PROP_TAB_COLOR_RANGLISTE       = "Tab-Farbe Rangliste";
	public static final String KONFIG_PROP_TAB_COLOR_DIREKTVERGLEICH = "Tab-Farbe Direktvergleich";

	protected final WeakRefHelper<ISheet> sheetWkRef;
	private final DocumentPropertiesHelper docPropHelper;

	protected static void ADDBaseProp(List<ConfigProperty<?>> KONFIG_PROPERTIES) {
		ADDBaseProp(KONFIG_PROPERTIES, true);
	}

	protected static void ADDBaseProp(List<ConfigProperty<?>> KONFIG_PROPERTIES, boolean mitRangliste) {

		KONFIG_PROPERTIES.add(HeaderFooterConfigProperty.from(KONFIG_PROP_FUSSZEILE_LINKS)
				.setDescription("config.desc.footer.links").inSideBar());
		KONFIG_PROPERTIES.add(HeaderFooterConfigProperty.from(KONFIG_PROP_FUSSZEILE_MITTE)
				.setDescription("config.desc.footer.mitte").inSideBar());

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_MELDELISTE_COLOR_BACK_GERADE)
				.setDefaultVal(DEFAULT_GERADE_BACK_COLOR)
				.setDescription("config.desc.meldeliste.gerade").inSideBar());
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_MELDELISTE_COLOR_BACK_UNGERADE)
				.setDefaultVal(DEFAULT_UNGERADE_BACK_COLOR)
				.setDescription("config.desc.meldeliste.ungerade").inSideBar());
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_MELDELISTE_COLOR_BACK_HEADER)
				.setDefaultVal(DEFAULT_HEADER_BACK_COLOR)
				.setDescription("config.desc.meldeliste.header").inSideBar());

		if (mitRangliste) {
			KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_RANGLISTE_COLOR_BACK_GERADE)
					.setDefaultVal(DEFAULT_GERADE_BACK_COLOR).setDescription("config.desc.rangliste.gerade")
					.inSideBar());
			KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_RANGLISTE_COLOR_BACK_UNGERADE)
					.setDefaultVal(DEFAULT_UNGERADE_BACK_COLOR)
					.setDescription("config.desc.rangliste.ungerade").inSideBar());
			KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_RANGLISTE_COLOR_BACK_HEADER)
					.setDefaultVal(DEFAULT_HEADER_BACK_COLOR)
					.setDescription("config.desc.rangliste.header").inSideBar());
		}

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.INTEGER, KONFIG_PROP_ANZ_TEILNEHMER_IN_SPALTE)
				.setDefaultVal(DEFAULT_ANZ_TEILNEHMER_IN_SPALTE)
				.setDescription("config.desc.teilnehmer.anzahl.spalte")
				.inSideBar());

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_TAB_COLOR_MELDELISTE)
				.setDefaultVal(SheetTabFarben.MELDELISTE)
				.setDescription("config.desc.tab.farbe.meldeliste").tabFarbe());
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_TAB_COLOR_TEILNEHMER)
				.setDefaultVal(SheetTabFarben.TEILNEHMER)
				.setDescription("config.desc.tab.farbe.teilnehmer").tabFarbe());
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_TAB_COLOR_SPIELRUNDE)
				.setDefaultVal(SheetTabFarben.SPIELRUNDE)
				.setDescription("config.desc.tab.farbe.spielrunde").tabFarbe());
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_TAB_COLOR_RANGLISTE)
				.setDefaultVal(SheetTabFarben.RANGLISTE)
				.setDescription("config.desc.tab.farbe.rangliste").tabFarbe());
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_TAB_COLOR_DIREKTVERGLEICH)
				.setDefaultVal(SheetTabFarben.DIREKTVERGLEICH)
				.setDescription("config.desc.tab.farbe.direktvergleich").tabFarbe());
	}

	protected BasePropertiesSpalte(ISheet sheet) {
		sheetWkRef = new WeakRefHelper<>(sheet);
		checkNotNull(sheet);
		docPropHelper = newDocumentPropertiesHelper(sheetWkRef.get().getWorkingSpreadsheet());
	}

	@VisibleForTesting
	DocumentPropertiesHelper newDocumentPropertiesHelper(WorkingSpreadsheet wkspreadSheet) {
		return new DocumentPropertiesHelper(wkspreadSheet);
	}

	protected abstract List<ConfigProperty<?>> getKonfigProperties();

	protected WorkingSpreadsheet getWorkingSpreadsheet() {
		return sheetWkRef.get().getWorkingSpreadsheet();
	}

	/**
	 * Liest einen Integer-Property-Wert. Für Tab-Farb-Properties gilt die Fallback-Kette:
	 * Document Properties → PetanqueTurnierManager.properties → SheetTabFarben-Konstante.
	 *
	 * @param key Property-Schlüssel
	 * @return gespeicherter Wert oder Fallback-Default
	 */
	public int readIntProperty(String key) {
		int hardcodedDefault = getHardcodedDefault(key);
		int effectiveDefault = istTabFarbProp(key)
				? GlobalProperties.get().getTabFarbe(key, hardcodedDefault)
				: hardcodedDefault;
		return docPropHelper.getIntProperty(key, effectiveDefault);
	}

	private int getHardcodedDefault(String key) {
		Object defaultVal = getDefaultProp(key);
		return defaultVal == null ? 0 : (Integer) defaultVal;
	}

	private boolean istTabFarbProp(String key) {
		return getKonfigProperties().stream()
				.anyMatch(p -> p.getKey().equals(key) && p.isTabFarbe());
	}

	/**
	 *
	 * @param name @
	 */
	public void writeIntProperty(String key, Integer val) {
		docPropHelper.setIntProperty(key, val);
	}

	/**
	 * lese von der Zelle im Sheet zu diesen Property, das Property "CellBackColor"
	 *
	 * @param key = property
	 * @return Integer, -1 wenn keine Farbe, null when not found
	 */
	public Integer readCellBackColorProperty(String key) {
		return readIntProperty(key);
	}

	/**
	 * @param name
	 * @return defaultVal aus ConfigProperty, -1 wenn fehler
	 */
	public String readStringProperty(String key) {
		String val = null;

		// value aus Document properties lesen
		Object defaultVal = getDefaultProp(key);
		val = docPropHelper.getStringProperty(key, ((defaultVal == null) ? "" : defaultVal.toString()));
		return val;
	}

	public Boolean readBooleanProperty(String key) {
		return StringTools.stringToBoolean(readStringProperty(key));
	}

	public void setStringProperty(String key, String val) {
		docPropHelper.setStringProperty(key, val);
	}

	private Object getDefaultProp(String key) {
		for (ConfigProperty<?> konfigProp : getKonfigProperties()) {
			if (konfigProp.getKey().equals(key)) {
				return konfigProp.getDefaultVal();
			}
		}
		return null;
	}

	@Override
	public final Integer getMeldeListeHintergrundFarbeGerade() {
		return readCellBackColorProperty(KONFIG_PROP_MELDELISTE_COLOR_BACK_GERADE);
	}

	@Override
	public final Integer getMeldeListeHintergrundFarbeUnGerade() {
		return readCellBackColorProperty(KONFIG_PROP_MELDELISTE_COLOR_BACK_UNGERADE);
	}

	@Override
	public final MeldungenHintergrundFarbeGeradeStyle getMeldeListeHintergrundFarbeGeradeStyle() {
		return new MeldungenHintergrundFarbeGeradeStyle(getMeldeListeHintergrundFarbeGerade());
	}

	@Override
	public final MeldungenHintergrundFarbeUnGeradeStyle getMeldeListeHintergrundFarbeUnGeradeStyle() {
		return new MeldungenHintergrundFarbeUnGeradeStyle(getMeldeListeHintergrundFarbeUnGerade());
	}

	@Override
	public Integer getMeldeListeHeaderFarbe() {
		return readCellBackColorProperty(KONFIG_PROP_MELDELISTE_COLOR_BACK_HEADER);
	}

	@Override
	public final String getFusszeileLinks() {
		return readStringProperty(KONFIG_PROP_FUSSZEILE_LINKS);
	}

	@Override
	public final String getFusszeileMitte() {
		return readStringProperty(KONFIG_PROP_FUSSZEILE_MITTE);
	}

	@Override
	public Integer getRanglisteHeaderFarbe() {
		return readCellBackColorProperty(KONFIG_PROP_RANGLISTE_COLOR_BACK_HEADER);
	}

	@Override
	public Integer getRanglisteHintergrundFarbeGerade() {
		return readCellBackColorProperty(KONFIG_PROP_RANGLISTE_COLOR_BACK_GERADE);
	}

	@Override
	public Integer getRanglisteHintergrundFarbeUnGerade() {
		return readCellBackColorProperty(KONFIG_PROP_RANGLISTE_COLOR_BACK_UNGERADE);
	}

	@Override
	public Integer getMaxAnzTeilnehmerInSpalte() {
		return readIntProperty(KONFIG_PROP_ANZ_TEILNEHMER_IN_SPALTE);
	}

	@Override
	public int getMeldelisteTabFarbe() {
		return readIntProperty(KONFIG_PROP_TAB_COLOR_MELDELISTE);
	}

	@Override
	public int getTeilnehmerTabFarbe() {
		return readIntProperty(KONFIG_PROP_TAB_COLOR_TEILNEHMER);
	}

	@Override
	public int getSpielrundeTabFarbe() {
		return readIntProperty(KONFIG_PROP_TAB_COLOR_SPIELRUNDE);
	}

	@Override
	public int getRanglisteTabFarbe() {
		return readIntProperty(KONFIG_PROP_TAB_COLOR_RANGLISTE);
	}

	@Override
	public int getDirektvergleichTabFarbe() {
		return readIntProperty(KONFIG_PROP_TAB_COLOR_DIREKTVERGLEICH);
	}

}