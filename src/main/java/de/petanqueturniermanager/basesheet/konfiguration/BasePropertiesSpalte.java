/*
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
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.StringTools;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellstyle.MeldungenHintergrundFarbeGeradeStyle;
import de.petanqueturniermanager.helper.cellstyle.MeldungenHintergrundFarbeUnGeradeStyle;
import de.petanqueturniermanager.basesheet.meldeliste.TeilnehmerListeSortModus;
import de.petanqueturniermanager.helper.sheet.EditierbaresZelleFormatHelper;
import de.petanqueturniermanager.helper.upload.UploadKonfiguration;
import de.petanqueturniermanager.helper.upload.UploadProtokoll;
import de.petanqueturniermanager.konfigdialog.AuswahlConfigProperty;
import de.petanqueturniermanager.konfigdialog.ConfigProperty;
import de.petanqueturniermanager.konfigdialog.ConfigPropertyType;
import de.petanqueturniermanager.konfigdialog.HeaderFooterConfigProperty;
import de.petanqueturniermanager.helper.i18n.I18n;

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

	public static final String KONFIG_PROP_TURNIERLOGO_URL = "Turnierlogo Url";
	public static final String KONFIG_PROP_EDITIERBARE_FELDER_HERVORHEBEN = EditierbaresZelleFormatHelper.PROPERTY_KEY;

	public static final String KONFIG_PROP_ANZ_TEILNEHMER_IN_SPALTE = "Teilnehmerliste Anzahl je Spalte";
	private static final int DEFAULT_ANZ_TEILNEHMER_IN_SPALTE = 40;

	public static final String KONFIG_PROP_CHECKIN_LISTE_SORT_MODUS = "Checkin-Liste Sortierung";

	public static final String KONFIG_PROP_TEILNEHMER_LISTE_SORT_MODUS = "Teilnehmerliste Sortierung";

	// Upload / Export (für alle Systeme mit IUploadKonfigurierbar)
	public static final String KONFIG_PROP_UPLOAD_PROTOKOLL      = "Upload Protokoll";
	public static final String KONFIG_PROP_UPLOAD_HOST           = "Upload Host";
	public static final String KONFIG_PROP_UPLOAD_PORT           = "Upload Port";
	public static final String KONFIG_PROP_UPLOAD_BENUTZER       = "Upload Benutzer";
	public static final String KONFIG_PROP_UPLOAD_VERZEICHNIS    = "Upload Verzeichnis";
	public static final String KONFIG_PROP_MELDELISTE_EXPORTIEREN = "Meldeliste exportieren";

	// Tab-Farben (Document Properties Schlüssel)
	public static final String KONFIG_PROP_TAB_COLOR_MELDELISTE      = "Tab-Farbe Meldeliste";
	public static final String KONFIG_PROP_TAB_COLOR_TEILNEHMER      = "Tab-Farbe Teilnehmer";
	public static final String KONFIG_PROP_TAB_COLOR_SPIELRUNDE      = "Tab-Farbe Spielrunde";
	public static final String KONFIG_PROP_TAB_COLOR_RANGLISTE       = "Tab-Farbe Rangliste";
	public static final String KONFIG_PROP_TAB_COLOR_DIREKTVERGLEICH = "Tab-Farbe Direktvergleich";

	// Strong-Ref: BasePropertiesSpalte ist ein operationaler Helper, der nicht über die ISheet-Lebensdauer hinaus existiert.
	protected final ISheet iSheet;
	private final DocumentPropertiesHelper docPropHelper;

	protected static void ADDBaseProp(List<ConfigProperty<?>> KONFIG_PROPERTIES) {
		ADDBaseProp(KONFIG_PROPERTIES, true);
	}

	protected static void ADDBaseProp(List<ConfigProperty<?>> KONFIG_PROPERTIES, boolean mitRangliste) {

		KONFIG_PROPERTIES.add(HeaderFooterConfigProperty.from(KONFIG_PROP_FUSSZEILE_LINKS)
				.setDescription("config.desc.footer.links"));
		KONFIG_PROPERTIES.add(HeaderFooterConfigProperty.from(KONFIG_PROP_FUSSZEILE_MITTE)
				.setDescription("config.desc.footer.mitte"));

		// Hinweis: KONFIG_PROP_TURNIERLOGO_URL wird nicht mehr als Sidebar/Konfig-Sheet-Property
		// gepflegt. Der Wert wird nun direkt im Dialog „Turnier Startseite" als
		// Document-Custom-Property gesetzt und bei Bedarf über getTurnierlogoUrl() gelesen.

		KONFIG_PROPERTIES.add(ConfigProperty.<Boolean>from(ConfigPropertyType.BOOLEAN, KONFIG_PROP_EDITIERBARE_FELDER_HERVORHEBEN)
				.setDefaultVal(true)
				.setDescription("config.desc.editierbare.felder.hervorheben")
				.mitNachSpeichernAktion(ws -> {
					var calc = Lo.qi(com.sun.star.sheet.XCalculatable.class, ws.getWorkingSpreadsheetDocument());
					if (calc != null) {
						calc.calculateAll();
					}
				}));

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_MELDELISTE_COLOR_BACK_GERADE)
				.setDefaultVal(DEFAULT_GERADE_BACK_COLOR)
				.setDescription("config.desc.meldeliste.gerade"));
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_MELDELISTE_COLOR_BACK_UNGERADE)
				.setDefaultVal(DEFAULT_UNGERADE_BACK_COLOR)
				.setDescription("config.desc.meldeliste.ungerade"));
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_MELDELISTE_COLOR_BACK_HEADER)
				.setDefaultVal(DEFAULT_HEADER_BACK_COLOR)
				.setDescription("config.desc.meldeliste.header"));

		if (mitRangliste) {
			KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_RANGLISTE_COLOR_BACK_GERADE)
					.setDefaultVal(DEFAULT_GERADE_BACK_COLOR).setDescription("config.desc.rangliste.gerade"));
			KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_RANGLISTE_COLOR_BACK_UNGERADE)
					.setDefaultVal(DEFAULT_UNGERADE_BACK_COLOR)
					.setDescription("config.desc.rangliste.ungerade"));
			KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_RANGLISTE_COLOR_BACK_HEADER)
					.setDefaultVal(DEFAULT_HEADER_BACK_COLOR)
					.setDescription("config.desc.rangliste.header"));
		}

		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.INTEGER, KONFIG_PROP_ANZ_TEILNEHMER_IN_SPALTE)
				.setDefaultVal(DEFAULT_ANZ_TEILNEHMER_IN_SPALTE)
				.setDescription("config.desc.teilnehmer.anzahl.spalte"));

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

	/**
	 * Fügt die Auswahl-Property für die Checkin-Listen-Sortierung hinzu
	 * (Default {@link TeilnehmerListeSortModus#NAME}).
	 * <p>
	 * Bewusst NICHT Teil von {@link #ADDBaseProp(List)}, da das Liga-System keine Checkin-Liste besitzt.
	 * Jedes Nicht-Liga-System ruft diese Methode in seinem statischen Initialisierer auf.
	 * <p>
	 * Verwendet dieselbe Modi-Enum und dieselben Option-Labels wie die Teilnehmerliste
	 * ({@link TeilnehmerListeSortModus}), bleibt aber eine eigenständige Property.
	 *
	 * @param KONFIG_PROPERTIES Property-Liste des jeweiligen Systems
	 */
	protected static void addCheckinSortProp(List<ConfigProperty<?>> KONFIG_PROPERTIES) {
		KONFIG_PROPERTIES.add(((AuswahlConfigProperty) AuswahlConfigProperty.from(KONFIG_PROP_CHECKIN_LISTE_SORT_MODUS)
				.setDefaultVal(TeilnehmerListeSortModus.NAME.getKey())
				.setDescription("config.desc.checkin.sort.modus"))
				.addAuswahl(TeilnehmerListeSortModus.NUMMER.getKey(), I18n.get("config.teilnehmer.sort.nummer"))
				.addAuswahl(TeilnehmerListeSortModus.NAME.getKey(), I18n.get("config.teilnehmer.sort.name"))
				.addAuswahl(TeilnehmerListeSortModus.TEAMNAME.getKey(), I18n.get("config.teilnehmer.sort.teamname")));
	}

	/**
	 * Fügt die Auswahl-Property für die Teilnehmerlisten-Sortierung hinzu
	 * (Default {@link TeilnehmerListeSortModus#NAME}).
	 * <p>
	 * Bewusst NICHT Teil von {@link #ADDBaseProp(List)}, da das Liga-System keine Teilnehmerliste besitzt.
	 * Jedes System mit Teilnehmerliste ruft diese Methode in seinem statischen Initialisierer auf.
	 *
	 * @param KONFIG_PROPERTIES Property-Liste des jeweiligen Systems
	 */
	protected static void addTeilnehmerListeSortProp(List<ConfigProperty<?>> KONFIG_PROPERTIES) {
		KONFIG_PROPERTIES.add(((AuswahlConfigProperty) AuswahlConfigProperty
				.from(KONFIG_PROP_TEILNEHMER_LISTE_SORT_MODUS)
				.setDefaultVal(TeilnehmerListeSortModus.NAME.getKey())
				.setDescription("config.desc.teilnehmer.sort.modus"))
				.addAuswahl(TeilnehmerListeSortModus.NUMMER.getKey(), I18n.get("config.teilnehmer.sort.nummer"))
				.addAuswahl(TeilnehmerListeSortModus.NAME.getKey(), I18n.get("config.teilnehmer.sort.name"))
				.addAuswahl(TeilnehmerListeSortModus.TEAMNAME.getKey(), I18n.get("config.teilnehmer.sort.teamname")));
	}

	/**
	 * Fügt die gemeinsamen Upload-Properties hinzu, markiert mit
	 * {@link de.petanqueturniermanager.konfigdialog.ConfigProperty#exportKonfig()}.
	 * Aufzurufen von jedem System, das {@code IUploadKonfigurierbar} implementiert.
	 */
	protected static void ADDUploadProp(List<ConfigProperty<?>> KONFIG_PROPERTIES) {
		KONFIG_PROPERTIES.add(((AuswahlConfigProperty) AuswahlConfigProperty.from(KONFIG_PROP_UPLOAD_PROTOKOLL)
				.setDefaultVal("SFTP").setDescription("config.desc.upload.protokoll").exportKonfig())
				.addAuswahl("SFTP", "SFTP")
				.addAuswahl("FTP", "FTP"));
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.STRING, KONFIG_PROP_UPLOAD_HOST)
				.setDefaultVal("").setDescription("config.desc.upload.host").exportKonfig());
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.STRING, KONFIG_PROP_UPLOAD_PORT)
				.setDefaultVal("").setDescription("config.desc.upload.port").exportKonfig());
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.STRING, KONFIG_PROP_UPLOAD_BENUTZER)
				.setDefaultVal("").setDescription("config.desc.upload.benutzer").exportKonfig());
		KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.STRING, KONFIG_PROP_UPLOAD_VERZEICHNIS)
				.setDefaultVal("").setDescription("config.desc.upload.verzeichnis").exportKonfig());
		KONFIG_PROPERTIES.add(ConfigProperty.<Boolean>from(ConfigPropertyType.BOOLEAN, KONFIG_PROP_MELDELISTE_EXPORTIEREN)
				.setDefaultVal(false).setDescription("config.desc.meldeliste.exportieren").exportKonfig());
	}

	protected BasePropertiesSpalte(ISheet sheet) {
		this.iSheet = checkNotNull(sheet);
		docPropHelper = newDocumentPropertiesHelper(sheet.getWorkingSpreadsheet());
	}

	@VisibleForTesting
	DocumentPropertiesHelper newDocumentPropertiesHelper(WorkingSpreadsheet wkspreadSheet) {
		return new DocumentPropertiesHelper(wkspreadSheet);
	}

	protected abstract List<ConfigProperty<?>> getKonfigProperties();

	protected WorkingSpreadsheet getWorkingSpreadsheet() {
		return iSheet.getWorkingSpreadsheet();
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
		// value aus Document properties lesen
		Object defaultVal = getDefaultProp(key);
		String defaultStr = (defaultVal == null) ? "" : defaultVal.toString();
		// Fehlende Property einmalig mit dem Konfig-Default in die UserDefinedProperties
		// schreiben — sonst bleibt der Default nur Code-seitig sichtbar, und externe
		// Leser (z.B. MeldelisteZielFactory) bekämen weiter leeres Ergebnis.
		// initStringPropertyIfAbsent ist no-op, sobald die Property gesetzt wurde,
		// es entstehen also keine wiederholten Doc-Mutationen.
		if (defaultVal != null) {
			docPropHelper.initStringPropertyIfAbsent(key, defaultStr);
		}
		return docPropHelper.getStringProperty(key, defaultStr);
	}

	public Boolean readBooleanProperty(String key) {
		return StringTools.stringToBoolean(readStringProperty(key));
	}

	protected int getUploadPortOderStandard() {
		return UploadKonfiguration.portOderStandard(
				readStringProperty(KONFIG_PROP_UPLOAD_PORT),
				UploadProtokoll.vonString(readStringProperty(KONFIG_PROP_UPLOAD_PROTOKOLL)));
	}

	@Override
	public boolean isMeldelisteExportieren() {
		return readBooleanProperty(KONFIG_PROP_MELDELISTE_EXPORTIEREN);
	}

	/**
	 * Liest eine Enum-Property robust: leerer/unbekannter Wert → defaultValue.
	 */
	protected <E extends Enum<E>> E readEnumProperty(String key, Class<E> enumClass, E defaultValue) {
		String val = readStringProperty(key);
		if (val == null || val.isEmpty()) {
			return defaultValue;
		}
		try {
			return Enum.valueOf(enumClass, val);
		} catch (IllegalArgumentException e) {
			return defaultValue;
		}
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
	public String getTurnierlogoUrl() {
		var val = readStringProperty(KONFIG_PROP_TURNIERLOGO_URL);
		if (val.isEmpty()) {
			// Migration: alten Liga-spezifischen Schlüssel einmalig in neuen Schlüssel übertragen
			val = readStringProperty("Liga-Logo Url");
			if (!val.isEmpty()) {
				setStringProperty(KONFIG_PROP_TURNIERLOGO_URL, val);
			}
		}
		return val;
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

	@Override
	public TeilnehmerListeSortModus getCheckinListeSortModus() {
		return readEnumProperty(KONFIG_PROP_CHECKIN_LISTE_SORT_MODUS, TeilnehmerListeSortModus.class,
				TeilnehmerListeSortModus.NAME);
	}

	@Override
	public TeilnehmerListeSortModus getTeilnehmerListeSortModus() {
		return readEnumProperty(KONFIG_PROP_TEILNEHMER_LISTE_SORT_MODUS, TeilnehmerListeSortModus.class,
				TeilnehmerListeSortModus.NAME);
	}

	@Override
	public boolean isEditierbareFelder() {
		var val = readBooleanProperty(KONFIG_PROP_EDITIERBARE_FELDER_HERVORHEBEN);
		return val != null ? val : true;
	}

}
