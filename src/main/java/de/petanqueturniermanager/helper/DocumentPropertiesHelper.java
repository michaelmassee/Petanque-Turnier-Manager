/**
 * Erstellung : 24.03.2018 / Michael Massee
 **/

package de.petanqueturniermanager.helper;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.beans.IllegalTypeException;
import com.sun.star.beans.Property;
import com.sun.star.beans.PropertyExistException;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XMultiPropertySet;
import com.sun.star.beans.XPropertyContainer;
import com.sun.star.beans.XPropertySet;
import com.sun.star.document.XDocumentProperties;
import com.sun.star.document.XDocumentPropertiesSupplier;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.uno.Any;
import com.sun.star.uno.Type;

import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.comp.PetanqueTurnierMngrSingleton;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.comp.turnierevent.OnProperiesChangedEvent;
import de.petanqueturniermanager.comp.turnierevent.TurnierEventType;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Setzt und liest Document-Properties (benutzerdefinierte Metadaten).<br>
 * <br>
 * Kein globaler Cache – jede Instanz liest frisch vom Dokument.<br>
 * Schlüssel werden lowercase normalisiert für O(1) case-insensitiven Zugriff.
 */
public class DocumentPropertiesHelper {
	private static final Logger logger = LogManager.getLogger(DocumentPropertiesHelper.class);

	private final XSpreadsheetDocument xSpreadsheetDocument;
	private final ConcurrentHashMap<String, String> currentPropListe;

	public DocumentPropertiesHelper(WorkingSpreadsheet currentSpreadsheet) {
		this(checkNotNull(currentSpreadsheet).getWorkingSpreadsheetDocument());
	}

	public DocumentPropertiesHelper(XSpreadsheetDocument xSpreadsheetDocument) {
		this.xSpreadsheetDocument = checkNotNull(xSpreadsheetDocument);
		currentPropListe = new ConcurrentHashMap<>();
		// Kein globaler Cache – jede Instanz liest frisch vom Dokument.
		// Lifecycle wird implizit durch Instanz-GC gehandhabt.
		ladeDokumentProperties();
	}

	private void ladeDokumentProperties() {
		XMultiPropertySet xMultiPropertySet = getXMultiPropertySet();
		XPropertySet xPropertySet = getXPropertySet();
		for (Property userProp : xMultiPropertySet.getPropertySetInfo().getProperties()) {
			try {
				Object propVal = xPropertySet.getPropertyValue(userProp.Name);
				currentPropListe.put(
						userProp.Name.toLowerCase(Locale.ROOT),
						propVal != null ? propVal.toString() : "");
			} catch (UnknownPropertyException | WrappedTargetException e) {
				logger.debug("Property '{}' nicht lesbar: {}", userProp.Name, e.getMessage());
			}
		}
		if (currentPropListe.isEmpty()) {
			logger.debug("Dokument hat keine benutzerdefinierten Properties");
		}
	}

	public boolean isEmpty() {
		return currentPropListe.isEmpty();
	}

	/**
	 * Prüft ob das PTM-Pflichtfeld (Turniersystem) vorhanden ist.
	 * Robustere Alternative zu {@link #isEmpty()} – ein Dokument kann legitim
	 * leer sein, während ein PTM-Dokument immer dieses Pflichtfeld besitzt.
	 */
	public boolean hasRequiredProperties() {
		return currentPropListe.containsKey(
				BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM.toLowerCase(Locale.ROOT));
	}

	/**
	 * Property in interne Map und Dokument speichern
	 */
	public void setStringProperty(String propName, String val) {
		if (val != null) {
			String oldVal = currentPropListe.get(propName.toLowerCase(Locale.ROOT));
			if (!Objects.equals(oldVal, val)) {
				setStringPropertyInDocument(propName, val);
				currentPropListe.put(propName.toLowerCase(Locale.ROOT), val);
				PetanqueTurnierMngrSingleton.triggerTurnierEventListener(TurnierEventType.PropertiesChanged,
						new OnProperiesChangedEvent(xSpreadsheetDocument).addChanged(propName, oldVal, val));
			}
		}
	}

	/**
	 * fügt ein neues Property zum Dokument hinzu, wenn nicht vorhanden<br>
	 * speichert den neuen Wert
	 */
	private void setStringPropertyInDocument(String propName, String val) {
		boolean didExist = insertStringPropertyIfNotExist(propName, val);
		if (didExist) {
			XPropertySet propSet = getXPropertySet();
			try {
				propSet.setPropertyValue(propName, val);
			} catch (IllegalArgumentException | UnknownPropertyException | PropertyVetoException
					| WrappedTargetException e) {
				logger.error(e);
			}
		}
	}

	/**
	 * @return true wenn bereits vorhanden
	 */
	private boolean insertStringPropertyIfNotExist(String name, String val) {
		checkNotNull(name);
		checkNotNull(val);

		boolean didExist = false;
		XPropertyContainer xpc = getXPropertyContainer();

		try {
			xpc.addProperty(name, (short) 0, new Any(Type.STRING, val));
		} catch (PropertyExistException e) {
			didExist = true;
		} catch (IllegalTypeException | IllegalArgumentException e) {
			logger.error(e.getMessage(), e);
		}
		return didExist;
	}

	private XPropertyContainer getXPropertyContainer() {
		XDocumentPropertiesSupplier xps = Lo.qi(XDocumentPropertiesSupplier.class, xSpreadsheetDocument);
		XDocumentProperties xp = xps.getDocumentProperties();
		return xp.getUserDefinedProperties();
	}

	private XMultiPropertySet getXMultiPropertySet() {
		return Lo.qi(XMultiPropertySet.class, getXPropertyContainer());
	}

	private XPropertySet getXPropertySet() {
		return Lo.qi(XPropertySet.class, getXPropertyContainer());
	}

	/**
	 * @param propName   case insensitive
	 * @param defaultVal Rückgabewert wenn Property nicht vorhanden
	 */
	public String getStringProperty(String propName, String defaultVal) {
		return currentPropListe.getOrDefault(propName.toLowerCase(Locale.ROOT), defaultVal);
	}

	/**
	 * @param propName   Name des Property
	 * @param defaultVal Rückgabewert wenn Property nicht vorhanden oder nicht parsbar
	 * @return gespeicherter int-Wert oder defaultVal
	 */
	public int getIntProperty(String propName, int defaultVal) {
		String stringProperty = getStringProperty(propName, "" + defaultVal);
		if (stringProperty != null) {
			return NumberUtils.toInt(stringProperty, -1);
		}
		return -1;
	}

	/**
	 * @param propName Name des Property
	 * @param val      int-Wert wird als String gespeichert
	 */
	public void setIntProperty(String propName, int val) {
		setStringProperty(propName, "" + val);
	}

	public boolean getBooleanProperty(String propName, Boolean defaultVal) {
		String stringProperty = getStringProperty(propName, StringTools.booleanToString(defaultVal));
		return StringTools.stringToBoolean(stringProperty);
	}

	public void setBooleanProperty(String propName, Boolean newVal) {
		setStringProperty(propName, StringTools.booleanToString(newVal));
	}

	public boolean getTurnierModusAusDocument() {
		return getBooleanProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIER_MODUS, false);
	}

	public TurnierSystem getTurnierSystemAusDocument() {
		TurnierSystem turnierSystemAusDocument = TurnierSystem.KEIN;
		int spielsystem = getIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM,
				TurnierSystem.KEIN.getId());
		if (spielsystem > -1) {
			turnierSystemAusDocument = TurnierSystem.findById(spielsystem);
		}
		return turnierSystemAusDocument;
	}
}
