/**
 * Erstellung 10.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.konfiguration;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import com.google.common.annotations.VisibleForTesting;
import com.sun.star.table.TableBorder2;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.StringTools;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.sheet.WeakRefHelper;
import de.petanqueturniermanager.konfigdialog.ConfigProperty;
import de.petanqueturniermanager.konfigdialog.ConfigPropertyType;
import de.petanqueturniermanager.konfigdialog.HeaderFooterConfigProperty;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.SpielTagNr;

/**
 * @author Michael Massee
 *
 */
abstract public class BasePropertiesSpalte implements IPropertiesSpalte {

	public static final int PROP_CELL_MARGIN = 150;
	public static final int HEADER_HEIGHT = 800;
	public static final TableBorder2 HEADER_BORDER = BorderFactory.from().allThin().boldLn().forBottom().toBorder();
	public static final String HEADER_BACK_COLOR = "#dedbd3";
	public static final Integer DEFAULT_GERADE_BACK_COLOR = Integer.valueOf("e1e9f7", 16);
	public static final Integer DEFAULT_UNGERADE__BACK_COLOR = Integer.valueOf("c0d6f7", 16);
	public static final Integer DEFAULT_HEADER__BACK_COLOR = Integer.valueOf("e6ebf4", 16);

	public static final String KONFIG_PROP_NAME_SPIELTAG = "Spieltag";
	public static final String KONFIG_PROP_NAME_SPIELRUNDE = "Spielrunde";

	public static final String KONFIG_PROP_NAME_TURNIERSYSTEM = "Turniersystem";
	private static final String KONFIG_PROP_MELDELISTE_COLOR_BACK_GERADE = "Meldeliste Hintergrund Gerade";
	private static final String KONFIG_PROP_MELDELISTE_COLOR_BACK_UNGERADE = "Meldeliste Hintergrund Ungerade";
	private static final String KONFIG_PROP_MELDELISTE_COLOR_BACK_HEADER = "Meldeliste Header";

	private static final String KONFIG_PROP_RANGLISTE_COLOR_BACK_GERADE = "Rangliste Hintergrund Gerade";
	private static final String KONFIG_PROP_RANGLISTE_COLOR_BACK_UNGERADE = "Rangliste Hintergrund Ungerade";
	private static final String KONFIG_PROP_RANGLISTE_COLOR_BACK_HEADER = "Rangliste Header";

	private static final String KONFIG_PROP_FUSSZEILE_LINKS = "Fußzeile links";
	private static final String KONFIG_PROP_FUSSZEILE_MITTE = "Fußzeile mitte";

	private static final String KONFIG_PROP_ZEIGE_ARBEITS_SPALTEN = "Zeige Arbeitsspalten"; // diesen Daten werden nur intern gebraucht, default = false

	protected final WeakRefHelper<ISheet> sheetWkRef;
	private final DocumentPropertiesHelper docPropHelper;

	protected static void ADDBaseProp(List<ConfigProperty<?>> KONFIG_PROPERTIES) {
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.INTEGER, KONFIG_PROP_NAME_SPIELTAG).setDefaultVal(1).setDescription("Aktuelle Spieltag").inSideBarInfoPanel());
		KONFIG_PROPERTIES
				.add(ConfigProperty.from(ConfigPropertyType.INTEGER, KONFIG_PROP_NAME_SPIELRUNDE).setDefaultVal(1).setDescription("Aktuelle Spielrunde").inSideBarInfoPanel());

		KONFIG_PROPERTIES.add(HeaderFooterConfigProperty.from(KONFIG_PROP_FUSSZEILE_LINKS).setDescription("Fußzeile Links").inSideBar());
		KONFIG_PROPERTIES.add(HeaderFooterConfigProperty.from(KONFIG_PROP_FUSSZEILE_MITTE).setDescription("Fußzeile Mitte").inSideBar());

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_MELDELISTE_COLOR_BACK_GERADE).setDefaultVal(DEFAULT_GERADE_BACK_COLOR)
				.setDescription("Meldeliste Hintergrundfarbe für gerade Zeilen").inSideBar());
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_MELDELISTE_COLOR_BACK_UNGERADE).setDefaultVal(DEFAULT_UNGERADE__BACK_COLOR)
				.setDescription("Meldeliste Hintergrundfarbe für ungerade Zeilen").inSideBar());
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_MELDELISTE_COLOR_BACK_HEADER).setDefaultVal(DEFAULT_HEADER__BACK_COLOR)
				.setDescription("Meldeliste Hintergrundfarbe für die Tabelle-Kopfzeilen").inSideBar());

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_RANGLISTE_COLOR_BACK_GERADE).setDefaultVal(DEFAULT_GERADE_BACK_COLOR)
				.setDescription("Rangliste Hintergrundfarbe für gerade Zeilen").inSideBar());
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_RANGLISTE_COLOR_BACK_UNGERADE).setDefaultVal(DEFAULT_UNGERADE__BACK_COLOR)
				.setDescription("Rangliste Hintergrundfarbe für ungerade Zeilen").inSideBar());
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_RANGLISTE_COLOR_BACK_HEADER).setDefaultVal(DEFAULT_HEADER__BACK_COLOR)
				.setDescription("Rangliste Hintergrundfarbe für die Tabelle-Kopfzeilen").inSideBar());

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.BOOLEAN, KONFIG_PROP_ZEIGE_ARBEITS_SPALTEN).setDefaultVal(false)
				.setDescription("Zeige Arbeitsdaten (N/J),Nur fuer fortgeschrittene Benutzer empfohlen. Default = N").inSideBar());
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

	abstract protected List<ConfigProperty<?>> getKonfigProperties();

	protected WorkingSpreadsheet getWorkingSpreadsheet() {
		return sheetWkRef.get().getWorkingSpreadsheet();
	}

	/**
	 *
	 * @param name
	 * @return defaultVal aus ConfigProperty, -1 wenn fehler
	 * @throws GenerateException
	 */
	public int readIntProperty(String key) throws GenerateException {
		Integer val = null;
		Object defaultVal = getDefaultProp(key);
		val = docPropHelper.getIntProperty(key, (defaultVal == null) ? 0 : (Integer) defaultVal);
		return val;
	}

	/**
	 *
	 * @param name
	 * @throws GenerateException
	 */
	public void writeIntProperty(String key, Integer val) throws GenerateException {
		docPropHelper.setIntProperty(key, val);
	}

	/**
	 * lese von der Zelle im Sheet zu diesen Property, das Property "CellBackColor"
	 *
	 * @param key = property
	 * @return Integer, -1 wenn keine Farbe, null when not found
	 * @throws GenerateException
	 */
	public Integer readCellBackColorProperty(String key) throws GenerateException {
		return readIntProperty(key);
	}

	/**
	 * @param name
	 * @return defaultVal aus ConfigProperty, -1 wenn fehler
	 * @throws GenerateException
	 */
	public String readStringProperty(String key) throws GenerateException {
		String val = null;

		// value aus Document properties lesen
		Object defaultVal = getDefaultProp(key);
		val = docPropHelper.getStringProperty(key, ((defaultVal == null) ? "" : defaultVal.toString()));
		return val;
	}

	public Boolean readBooleanProperty(String key) throws GenerateException {
		return StringTools.stringToBoolean(readStringProperty(key));
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
	public final Integer getMeldeListeHintergrundFarbeGerade() throws GenerateException {
		return readCellBackColorProperty(KONFIG_PROP_MELDELISTE_COLOR_BACK_GERADE);
	}

	@Override
	public final Integer getMeldeListeHintergrundFarbeUnGerade() throws GenerateException {
		return readCellBackColorProperty(KONFIG_PROP_MELDELISTE_COLOR_BACK_UNGERADE);
	}

	@Override
	public Integer getMeldeListeHeaderFarbe() throws GenerateException {
		return readCellBackColorProperty(KONFIG_PROP_MELDELISTE_COLOR_BACK_HEADER);
	}

	@Override
	public final String getFusszeileLinks() throws GenerateException {
		return readStringProperty(KONFIG_PROP_FUSSZEILE_LINKS);
	}

	@Override
	public final String getFusszeileMitte() throws GenerateException {
		return readStringProperty(KONFIG_PROP_FUSSZEILE_MITTE);
	}

	@Override
	public final boolean zeigeArbeitsSpalten() throws GenerateException {
		return readBooleanProperty(KONFIG_PROP_ZEIGE_ARBEITS_SPALTEN);
	}

	@Override
	public Integer getRanglisteHeaderFarbe() throws GenerateException {
		return readCellBackColorProperty(KONFIG_PROP_RANGLISTE_COLOR_BACK_HEADER);
	}

	@Override
	public Integer getRanglisteHintergrundFarbeGerade() throws GenerateException {
		return readCellBackColorProperty(KONFIG_PROP_RANGLISTE_COLOR_BACK_GERADE);
	}

	@Override
	public Integer getRanglisteHintergrundFarbeUnGerade() throws GenerateException {
		return readCellBackColorProperty(KONFIG_PROP_RANGLISTE_COLOR_BACK_UNGERADE);
	}

	@Override
	public final void setAktiveSpielRunde(SpielRundeNr spielrunde) throws GenerateException {
		writeIntProperty(KONFIG_PROP_NAME_SPIELRUNDE, spielrunde.getNr());
	}

	@Override
	public final void setAktiveSpieltag(SpielTagNr spieltag) throws GenerateException {
		writeIntProperty(KONFIG_PROP_NAME_SPIELTAG, spieltag.getNr());
	}

	@Override
	public SpielTagNr getAktiveSpieltag() throws GenerateException {
		return SpielTagNr.from(readIntProperty(KONFIG_PROP_NAME_SPIELTAG));
	}

	@Override
	public SpielRundeNr getAktiveSpielRunde() throws GenerateException {
		return SpielRundeNr.from(readIntProperty(KONFIG_PROP_NAME_SPIELRUNDE));
	}
}