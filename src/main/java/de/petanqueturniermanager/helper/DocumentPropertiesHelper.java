/**
 * Erstellung : 24.03.2018 / Michael Massee
 **/

package de.petanqueturniermanager.helper;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Hashtable;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
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
import com.sun.star.frame.XModel;
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
 * http://www.openoffice.org/api/docs/common/ref/com/sun/star/document/XDocumentProperties.html <br>
 * https://forum.openoffice.org/en/forum/viewtopic.php?t=33455 <br>
 * <br>
 * Setzt und liest Docoment properties<br>
 *
 */

public class DocumentPropertiesHelper {
	private static final Logger logger = LogManager.getLogger(DocumentPropertiesHelper.class);

	// TODO ist das noch notwendig ?
	// Wegen core dumps die ich nicht nachvolziehen kann, eigene Properties liste in speicher.
	// Hashtable is synchronized
	// key search ignore case
	private static final Hashtable<Integer, Hashtable<String, String>> PROPLISTE = new Hashtable<>();

	private final XSpreadsheetDocument xSpreadsheetDocument;
	private final Hashtable<String, String> currentPropListe;
	private boolean firstLoad = false;

	public DocumentPropertiesHelper(WorkingSpreadsheet currentSpreadsheet) {
		this(checkNotNull(currentSpreadsheet).getWorkingSpreadsheetDocument());
	}

	public DocumentPropertiesHelper(XSpreadsheetDocument xSpreadsheetDocument) {
		this.xSpreadsheetDocument = checkNotNull(xSpreadsheetDocument);
		Integer xSpreadsheetDocumentHash = xSpreadsheetDocument.hashCode();
		if (PROPLISTE.containsKey((xSpreadsheetDocumentHash))) {
			currentPropListe = PROPLISTE.get(xSpreadsheetDocumentHash);
		} else {
			// einmal laden
			currentPropListe = new Hashtable<>();
			// properties aus dokument laden
			XMultiPropertySet xMultiPropertySet = getXMultiPropertySet();
			XPropertySet xPropertySet = getXPropertySet();
			Property[] properties = xMultiPropertySet.getPropertySetInfo().getProperties();
			for (Property userProp : properties) {
				try {
					Object propVal = xPropertySet.getPropertyValue(userProp.Name);
					currentPropListe.put(userProp.Name, propVal.toString());
				} catch (UnknownPropertyException | WrappedTargetException e) {
				}
			}
			PROPLISTE.put(xSpreadsheetDocumentHash, currentPropListe);
			firstLoad = true;
		}
	}

	public boolean isEmpty() {
		return currentPropListe.isEmpty();
	}

	/**
	 * Document close
	 */
	public static synchronized void removeDocument(Object source) {
		try {
			if (source != null) {
				XModel xModel = Lo.qi(XModel.class, source);
				XSpreadsheetDocument xSpreadsheetDocument = Lo.qi(XSpreadsheetDocument.class, xModel);
				// null dann wenn kein XSpreadsheetDocument
				if (xSpreadsheetDocument != null) {
					PROPLISTE.remove(xSpreadsheetDocument.hashCode());
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	/**
	 * Property in interne Cache, und Document speichern
	 *
	 * @param propName
	 * @param val
	 */
	public void setStringProperty(String propName, String val) {
		if (val != null) {
			String oldVal = currentPropListe.get(propName);
			if (!StringUtils.equals(oldVal, val)) {
				setStringPropertyInDocument(propName, val);
				currentPropListe.put(propName, val);
				PetanqueTurnierMngrSingleton.triggerTurnierEventListener(TurnierEventType.PropertiesChanged,
						new OnProperiesChangedEvent(xSpreadsheetDocument).addChanged(propName, oldVal, val));
			}
		}
	}

	/**
	 * fügt ein neues Property zum Dokument hinzu, wenn nicht vorhanden<br>
	 * speichert der neue wert
	 * 
	 */
	private void setStringPropertyInDocument(String propName, String val) {
		boolean didExist = insertStringPropertyIfNotExist(propName, val); // zuerst neu einfuegen wenn nicht vorhanden
		if (didExist) {
			// wenn bereits vorhanden dann wert updaten!
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
	 * https://api.libreoffice.org/docs/idl/ref/namespacecom_1_1sun_1_1star_1_1beans_1_1PropertyAttribute.html#a04101331ecfe0e8dc89c54c7e557c07f
	 *
	 * @param name
	 * @param val
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
	 * 
	 * @param propName case insenitive
	 * @param defaultVal
	 * @return
	 */
	public String getStringProperty(String propName, String defaultVal) {
		Optional<String> korrektPropName = currentPropListe.keySet().stream()
				.filter(key -> key.equalsIgnoreCase(propName)).findFirst();
		if (korrektPropName.isPresent()) {
			return currentPropListe.get(korrektPropName.get());
		}
		return defaultVal;
	}

	/**
	 * @param propName = name vom property
	 * @return default when not found
	 */
	private String getStringPropertyFromDocument(String propName, boolean ignoreNotFound, String defaultVal) {
		XPropertySet propSet = getXPropertySet();
		Object propVal = null;
		try {
			propVal = propSet.getPropertyValue(propName);
		} catch (UnknownPropertyException | WrappedTargetException e) {
			if (!ignoreNotFound) {
				logger.error(e.getMessage(), e);
			}
		}

		if (propVal instanceof String strval) {
			return strval;
		}
		return defaultVal;
	}

	/**
	 * @param propName = name vom property
	 * @return -1 when not found
	 */
	public int getIntProperty(String propName, int defaultVal) {
		String stringProperty = getStringProperty(propName, "" + defaultVal);
		if (stringProperty != null) {
			return NumberUtils.toInt(stringProperty, -1);
		}
		return -1;
	}

	/**
	 * @param propName
	 * @param val int val wird als String gespeichert
	 */
	public void setIntProperty(String propName, int val) {
		setStringProperty(propName, "" + val);
	}

	/**
	 * @param key
	 * @param defaultVal
	 * @return
	 */
	public boolean getBooleanProperty(String propName, Boolean defaultVal) {
		String stringProperty = getStringProperty(propName, StringTools.booleanToString(defaultVal));
		return StringTools.stringToBoolean(stringProperty);
	}

	/**
	 * @param key
	 * @param defaultVal
	 * @return
	 */
	public void setBooleanProperty(String propName, Boolean newVal) {
		setStringProperty(propName, StringTools.booleanToString(newVal));
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

	/**
	 * @return true wenn properties das erste mal geladen wurde
	 */
	public boolean isFirstLoad() {
		return firstLoad;
	}
}
