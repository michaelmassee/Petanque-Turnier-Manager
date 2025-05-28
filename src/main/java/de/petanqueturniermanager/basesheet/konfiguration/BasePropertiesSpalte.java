/**
 * Erstellung 10.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.konfiguration;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import com.google.common.annotations.VisibleForTesting;
import com.sun.star.table.TableBorder2;

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

	public static final String KONFIG_PROP_MELDELISTE_COLOR_BACK_GERADE = "Meldeliste Hintergrund Gerade";
	public static final String KONFIG_PROP_MELDELISTE_COLOR_BACK_UNGERADE = "Meldeliste Hintergrund Ungerade";
	public static final String KONFIG_PROP_MELDELISTE_COLOR_BACK_HEADER = "Meldeliste Header";

	public static final String KONFIG_PROP_RANGLISTE_COLOR_BACK_GERADE = "Rangliste Hintergrund Gerade";
	public static final String KONFIG_PROP_RANGLISTE_COLOR_BACK_UNGERADE = "Rangliste Hintergrund Ungerade";
	public static final String KONFIG_PROP_RANGLISTE_COLOR_BACK_HEADER = "Rangliste Header";

	public static final String KONFIG_PROP_FUSSZEILE_LINKS = "Fußzeile links";
	public static final String KONFIG_PROP_FUSSZEILE_MITTE = "Fußzeile mitte";

	public static final String KONFIG_PROP_ZEIGE_ARBEITS_SPALTEN = "Zeige Arbeitsspalten"; // diesen Daten werden nur intern gebraucht, default = false

	protected final WeakRefHelper<ISheet> sheetWkRef;
	private final DocumentPropertiesHelper docPropHelper;

	protected static void ADDBaseProp(List<ConfigProperty<?>> KONFIG_PROPERTIES) {

		KONFIG_PROPERTIES.add(HeaderFooterConfigProperty.from(KONFIG_PROP_FUSSZEILE_LINKS)
				.setDescription("Fußzeile Links").inSideBar());
		KONFIG_PROPERTIES.add(HeaderFooterConfigProperty.from(KONFIG_PROP_FUSSZEILE_MITTE)
				.setDescription("Fußzeile Mitte").inSideBar());

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_MELDELISTE_COLOR_BACK_GERADE)
				.setDefaultVal(DEFAULT_GERADE_BACK_COLOR)
				.setDescription("Meldeliste Hintergrundfarbe für gerade Zeilen").inSideBar());
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_MELDELISTE_COLOR_BACK_UNGERADE)
				.setDefaultVal(DEFAULT_UNGERADE_BACK_COLOR)
				.setDescription("Meldeliste Hintergrundfarbe für ungerade Zeilen").inSideBar());
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_MELDELISTE_COLOR_BACK_HEADER)
				.setDefaultVal(DEFAULT_HEADER_BACK_COLOR)
				.setDescription("Meldeliste Hintergrundfarbe für die Tabelle-Kopfzeilen").inSideBar());

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_RANGLISTE_COLOR_BACK_GERADE)
				.setDefaultVal(DEFAULT_GERADE_BACK_COLOR).setDescription("Rangliste Hintergrundfarbe für gerade Zeilen")
				.inSideBar());
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_RANGLISTE_COLOR_BACK_UNGERADE)
				.setDefaultVal(DEFAULT_UNGERADE_BACK_COLOR)
				.setDescription("Rangliste Hintergrundfarbe für ungerade Zeilen").inSideBar());
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_RANGLISTE_COLOR_BACK_HEADER)
				.setDefaultVal(DEFAULT_HEADER_BACK_COLOR)
				.setDescription("Rangliste Hintergrundfarbe für die Tabelle-Kopfzeilen").inSideBar());

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.BOOLEAN, KONFIG_PROP_ZEIGE_ARBEITS_SPALTEN)
				.setDefaultVal(false)
				.setDescription("Zeige Arbeitsdaten (N/J),Nur fuer fortgeschrittene Benutzer empfohlen. Default = N")
				.inSideBar());
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
	 *
	 * @param name
	 * @return defaultVal aus ConfigProperty, -1 wenn fehler @
	 */
	public int readIntProperty(String key) {
		Integer val = null;
		Object defaultVal = getDefaultProp(key);
		val = docPropHelper.getIntProperty(key, (defaultVal == null) ? 0 : (Integer) defaultVal);
		return val;
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
	public final boolean zeigeArbeitsSpalten() {
		return readBooleanProperty(KONFIG_PROP_ZEIGE_ARBEITS_SPALTEN);
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

}