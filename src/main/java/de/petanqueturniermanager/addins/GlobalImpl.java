package de.petanqueturniermanager.addins;

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.lang.XSingleComponentFactory;
import com.sun.star.lib.uno.helper.Factory;
import com.sun.star.registry.XRegistryKey;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.addin.XGlobal;
import de.petanqueturniermanager.algorithmen.Direktvergleich;
import de.petanqueturniermanager.comp.DocumentHelper;
import de.petanqueturniermanager.comp.PetanqueTurnierMngrSingleton;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

public final class GlobalImpl extends AbstractAddInImpl implements XGlobal {
	static final Logger logger = LogManager.getLogger(GlobalImpl.class);

	private final XComponentContext xContext;
	private static final String implName = GlobalImpl.class.getName();
	private static final String SERVICE_NAME = "de.petanqueturniermanager.addin.GlobalAddIn";
	private static final String[] serviceNames = { SERVICE_NAME };

	private static AtomicBoolean isDirty;

	// =de.petanqueturniermanager.addin.GlobalAddIn.ptmspielrunde()
	public static final String PTM_INT_PROPERTY = SERVICE_NAME + ".ptmintproperty";
	public static final String PTM_STRING_PROPERTY = SERVICE_NAME + ".ptmstringproperty";
	public static final String PTM_DIREKTVERGLEICH = SERVICE_NAME + ".ptmdirektvergleich";

	public static final String FORMAT_PTM_INT_PROPERTY(String propName) {
		return PTM_INT_PROPERTY + "(\"" + propName + "\")";
	}

	public static final String FORMAT_PTM_STRING_PROPERTY(String propName) {
		return PTM_STRING_PROPERTY + "(\"" + propName + "\")";
	}

	// wird nur einmal aufgerufen für alle sheets
	public GlobalImpl(XComponentContext xContext) {
		this.xContext = xContext;
		GlobalImpl.isDirty = new AtomicBoolean(false);
		PetanqueTurnierMngrSingleton.init(xContext);
		// PetanqueTurnierMngrSingleton.addGlobalEventListener(this);
	}

	public static final XSingleComponentFactory __getComponentFactory(String name) {
		XSingleComponentFactory xFactory = null;
		if (name.equals(implName)) {
			xFactory = Factory.createComponentFactory(GlobalImpl.class, serviceNames);
		}
		return xFactory;
	}

	public static final boolean __writeRegistryServiceInfo(XRegistryKey regKey) {
		return Factory.writeRegistryServiceInfo(implName, serviceNames, regKey);
	}

	// ------------------- XGlobal function(s) -----------------

	/**
	 * bei jeden call das aktive Dokument ermitteln
	 * 
	 * @return
	 */

	private DocumentPropertiesHelper getDocumentPropertiesHelper() {
		XSpreadsheetDocument doc = DocumentHelper.getCurrentSpreadsheetDocument(xContext);
		// XSpreadsheetDocument doc = spreadsheet;
		if (doc != null) {
			DocumentPropertiesHelper hlpr = new DocumentPropertiesHelper(doc);
			if (hlpr.isEmpty() && hlpr.isFirstLoad()) {
				// ist dann der fall wenn das Turnier dokument als erstes neu aus dem Menue geladen wird,
				// oder das Dokument hat keine properties aber PTM Funktionen.
				logger.debug("properties isFirstLoad and isEmpty=true");
				GlobalImpl.isDirty.set(true);
				return null;
			}
			return hlpr;
		}
		// das hat nicht funktioniert
		GlobalImpl.isDirty.set(true);
		logger.debug("XSpreadsheetDocument = null");
		return null;
	}

	@Override
	public int ptmintproperty(String arg0) {
		DocumentPropertiesHelper hlpr = getDocumentPropertiesHelper();
		if (hlpr != null) {
			TurnierSystem turnierSystemAusDocument = hlpr.getTurnierSystemAusDocument();

			if (!StringUtils.isAllBlank(arg0) && turnierSystemAusDocument != TurnierSystem.KEIN) {
				Integer propVal = hlpr.getIntProperty(arg0, 0);
				logger.debug("return:" + arg0 + "=" + propVal);
				return propVal;
			}
		}
		return 0;
	}

	@Override
	public String ptmstringproperty(String arg0) {
		DocumentPropertiesHelper hlpr = getDocumentPropertiesHelper();
		if (hlpr != null) {
			TurnierSystem turnierSystemAusDocument = hlpr.getTurnierSystemAusDocument();

			if (!StringUtils.isAllBlank(arg0) && turnierSystemAusDocument != TurnierSystem.KEIN) {
				String propVal = hlpr.getStringProperty(arg0, "Property '" + arg0 + "' fehlt.");
				logger.debug("return:" + arg0 + "=" + propVal);
				return propVal;
			}
		}
		return "fehler";
	}

	@Override
	public String ptmturniersystem() {
		DocumentPropertiesHelper hlpr = getDocumentPropertiesHelper();
		if (hlpr != null) {
			TurnierSystem turnierSystemAusDocument = hlpr.getTurnierSystemAusDocument();
			return turnierSystemAusDocument.getBezeichnung();
		}
		return TurnierSystem.KEIN.getBezeichnung();
	}

	static boolean getAndSetDirty(boolean newval) {
		return GlobalImpl.isDirty.getAndSet(newval);
	}

	@Override
	public int ptmdirektvergleich(int teamA, int teamB, int[][] paarungen, int[][] siege, int[][] spielpunkte) {
		Direktvergleich dvrgl = new Direktvergleich(teamA, teamB, paarungen, siege, spielpunkte);
		return dvrgl.calc().getCode();
	}

	@Override
	String getImplName() {
		return implName;
	}

	@Override
	String[] getServiceNames() {
		return serviceNames;
	}

}
