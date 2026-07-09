/*
 * Erstellung : 24.03.2018 / Michael Massee
 **/

package de.petanqueturniermanager.helper;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;
import java.util.Optional;
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
import com.sun.star.frame.XModel;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XInterface;
import com.sun.star.uno.Any;
import com.sun.star.uno.Type;
import com.sun.star.util.XModifiable;

import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.comp.PetanqueTurnierMngrSingleton;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.comp.turnierevent.OnProperiesChangedEvent;
import de.petanqueturniermanager.comp.turnierevent.TurnierEventType;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;

/**
 * http://www.openoffice.org/api/docs/common/ref/com/sun/star/document/XDocumentProperties.html <br>
 * https://forum.openoffice.org/en/forum/viewtopic.php?t=33455 <br>
 * <br>
 * Setzt und liest Docoment properties<br>
 * <br>
 * <b>Sichtbarkeit in Datei → Eigenschaften → Benutzerdefiniert ist zeitpunktabhängig:</b>
 * {@link #insertStringPropertyIfNotExist} legt neue Properties mit Attribut {@code 0} an (nicht
 * {@code PropertyAttribute.REMOVABLE}). LO filtert den Custom-Properties-Dialog auf genau dieses Flag
 * (siehe LO-Quelle {@code sfx2/source/dialog/dinfdlg.cxx}, {@code SfxDocumentPropertiesPage}:
 * „non-removable user-defined property? => ignore it"). Frisch in der laufenden Session gesetzte
 * Properties sind daher zunächst unsichtbar. Das ODF-Format ({@code meta.xml}) kennt dieses Attribut
 * aber gar nicht – es speichert nur Name/Typ/Wert. Beim nächsten Laden des Dokuments (Neu laden oder
 * Schließen + Öffnen) legt {@code SfxDocumentMetaData} beim Einlesen von {@code meta:user-defined}
 * <b>jede</b> Property unabhängig von ihrer Herkunft mit {@code PropertyAttribute.REMOVABLE} neu an
 * (LO-Quelle {@code sfx2/source/doc/SfxDocumentMetaData.cxx}, Methode die {@code meta:user-defined}
 * parst). Ab diesem Reload sind also auch die vom Plugin geschriebenen Properties sichtbar. Ein
 * "gemischter" Zustand (manche sichtbar, manche nicht) entsteht dadurch schlicht danach, welche
 * Properties schon vor dem letzten Laden/Reload in der Datei standen und welche erst in der aktuellen
 * Session neu hinzukamen. Funktional ist das unabhängig davon immer korrekt – Prüfung im Zweifel über
 * {@link #getStringProperty} statt über den LO-Eigenschaften-Dialog.
 */

public class DocumentPropertiesHelper {
	private static final Logger logger = LogManager.getLogger(DocumentPropertiesHelper.class);

	// Cache für Dokument-Properties im Speicher, um wiederholte UNO-Aufrufe zu vermeiden.
	// Schlüssel: UNO-OID (via UnoRuntime.generateOid), stabile Identität unabhängig von
	// Java-Proxy-Instanzen. ConcurrentHashMap statt Hashtable für bessere Nebenläufigkeit.
	private static final ConcurrentHashMap<String, ConcurrentHashMap<String, String>> PROPLISTE =
	        new ConcurrentHashMap<>();

	private final XSpreadsheetDocument xSpreadsheetDocument;
	private final ConcurrentHashMap<String, String> currentPropListe;
	private boolean firstLoad = false;

	public DocumentPropertiesHelper(WorkingSpreadsheet currentSpreadsheet) {
		this(checkNotNull(currentSpreadsheet).getWorkingSpreadsheetDocument());
	}

	public DocumentPropertiesHelper(XSpreadsheetDocument xSpreadsheetDocument) {
		this.xSpreadsheetDocument = checkNotNull(xSpreadsheetDocument);
		var oid = dokumentOid(xSpreadsheetDocument);
		var cachedListe = PROPLISTE.get(oid);
		if (cachedListe != null) {
			currentPropListe = cachedListe;
		} else {
			// einmal laden
			var neueListe = new ConcurrentHashMap<String, String>();
			XMultiPropertySet xMultiPropertySet = getXMultiPropertySet();
			XPropertySet xPropertySet = getXPropertySet();
			Property[] properties = xMultiPropertySet.getPropertySetInfo().getProperties();
			for (Property userProp : properties) {
				try {
					Object propVal = xPropertySet.getPropertyValue(userProp.Name);
					neueListe.put(userProp.Name, propVal.toString());
				} catch (UnknownPropertyException | WrappedTargetException e) {
					// Property nicht lesbar – überspringen
				}
			}
			var vorhandene = PROPLISTE.putIfAbsent(oid, neueListe);
			currentPropListe = vorhandene != null ? vorhandene : neueListe;
			firstLoad = vorhandene == null;
		}
	}

	private static String dokumentOid(XSpreadsheetDocument doc) {
		return UnoRuntime.generateOid(Lo.qi(XInterface.class, doc));
	}

	public boolean isEmpty() {
		return currentPropListe.isEmpty();
	}

	/**
	 * Document close – entfernt den Cache-Eintrag für das geschlossene Dokument.
	 */
	public static void removeDocument(Object source) {
		try {
			if (source != null) {
				XModel xModel = Lo.qi(XModel.class, source);
				XSpreadsheetDocument xSpreadsheetDocument = Lo.qi(XSpreadsheetDocument.class, xModel);
				// null dann wenn kein XSpreadsheetDocument
				if (xSpreadsheetDocument != null) {
					var oid = dokumentOid(xSpreadsheetDocument);
					var removed = PROPLISTE.remove(oid);
					if (removed == null) {
						logger.warn("removeDocument: Kein Cache-Eintrag für OID={}, Class={}",
								oid, source.getClass().getName());
					}
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
			if (!Objects.equals(oldVal, val)) {
				setStringPropertyInDocument(propName, val);
				currentPropListe.put(propName, val);
				if (logger.isInfoEnabled()) {
					logger.trace("[FOKUS-TRACE] setStringProperty: name='{}' old='{}' new='{}' doc={}",
							propName, oldVal, val,
							de.petanqueturniermanager.comp.ProtocolHandler.beschreibeDokument(xSpreadsheetDocument));
				}
				PetanqueTurnierMngrSingleton.triggerTurnierEventListener(TurnierEventType.PropertiesChanged,
						new OnProperiesChangedEvent(xSpreadsheetDocument).addChanged(propName, oldVal, val));
			}
		}
	}

	/**
	 * Wie {@link #setStringProperty(String, String)}, aber ohne {@link TurnierEventType#PropertiesChanged}-Event
	 * zu feuern. Für interne Infrastruktur-Properties (Hash, Timestamps, Recovery-Flags), an denen kein
	 * UI-Listener interessiert ist. Wichtig insbesondere, wenn der Aufruf von einem Hintergrund-Thread
	 * (z.B. {@code PTM-SheetSyncDebouncer}) erfolgt: ohne Event-Trigger werden Sidebar-Rebuilds
	 * (VCL-Operationen auf falschem Thread) vermieden.
	 */
	public void setStringPropertyOhneEvent(String propName, String val) {
		if (val != null) {
			String oldVal = currentPropListe.get(propName);
			if (!Objects.equals(oldVal, val)) {
				setStringPropertyInDocument(propName, val);
				currentPropListe.put(propName, val);
			}
		}
	}

	/**
	 * Wie {@link #setBooleanProperty(String, Boolean)}, aber ohne TurnierEvent. Siehe
	 * {@link #setStringPropertyOhneEvent(String, String)}.
	 */
	public void setBooleanPropertyOhneEvent(String propName, Boolean newVal) {
		setStringPropertyOhneEvent(propName, StringTools.booleanToString(newVal));
	}

	/**
	 * Führt {@code aktion} aus und stellt anschließend das Modified-Flag des Dokuments wieder her,
	 * falls es vor der Aktion {@code false} war. Reine Infrastruktur-Buchführung (z.B. SheetSync-
	 * Signaturen) schreibt UserDefined-Properties; das setzt in LibreOffice das Modified-Flag, auch
	 * ohne inhaltliche Änderung. Damit ein bloßer Lese-/Verify-Vorgang das Anwender-Dokument nicht
	 * als „geändert" markiert (sonst „Speichern?"-Abfrage beim Schließen), wird das Flag hier nur dann
	 * zurückgesetzt, wenn das Dokument zuvor unverändert war – echte User-Änderungen bleiben erhalten.
	 */
	public void ohneModifiedFlag(Runnable aktion) {
		checkNotNull(aktion);
		XModifiable xModifiable = Lo.qi(XModifiable.class, xSpreadsheetDocument);
		boolean warVorherModified = xModifiable != null && xModifiable.isModified();
		aktion.run();
		if (xModifiable != null && !warVorherModified) {
			try {
				xModifiable.setModified(false);
			} catch (PropertyVetoException e) {
				logger.warn("Modified-Flag zurücksetzen abgelehnt", e);
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
	 * @param defaultVal
	 * @return
	 */
	public boolean getBooleanProperty(String propName, Boolean defaultVal) {
		String stringProperty = getStringProperty(propName, StringTools.booleanToString(defaultVal));
		return StringTools.stringToBoolean(stringProperty);
	}

	/**
	 * @return
	 */
	public void setBooleanProperty(String propName, Boolean newVal) {
		setStringProperty(propName, StringTools.booleanToString(newVal));
	}

	public void initBooleanPropertyIfAbsent(String propName, boolean defaultVal) {
		boolean absent = currentPropListe.keySet().stream().noneMatch(key -> key.equalsIgnoreCase(propName));
		if (absent) {
			setBooleanProperty(propName, defaultVal);
		}
	}

	/**
	 * Schreibt {@code defaultVal} in die UserDefinedProperties des Dokuments,
	 * sofern dort noch keine Property unter {@code propName} existiert (case-insensitiv).
	 * Wird genutzt, um Bestandsdokumente nachträglich auf einen vollständigen
	 * Property-Satz zu migrieren — vermeidet, dass „fehlende" Properties dauerhaft
	 * nur durch Code-Defaults maskiert bleiben und zwischen Lese-Pfaden
	 * (Konfig-Sheet vs. extern via {@link #getStringProperty}) divergieren.
	 */
	public void initStringPropertyIfAbsent(String propName, String defaultVal) {
		if (defaultVal == null) {
			return;
		}
		boolean absent = currentPropListe.keySet().stream().noneMatch(key -> key.equalsIgnoreCase(propName));
		if (absent) {
			setStringProperty(propName, defaultVal);
		}
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

	/**
	 * @return true wenn properties das erste mal geladen wurde
	 */
	public boolean isFirstLoad() {
		return firstLoad;
	}
}
