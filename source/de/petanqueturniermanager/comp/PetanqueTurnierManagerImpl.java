/**
* Erstellung : 26.03.2018 / Michael Massee
**/
package de.petanqueturniermanager.comp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.lang.XServiceInfo;
import com.sun.star.lang.XSingleComponentFactory;
import com.sun.star.lib.uno.helper.Factory;
import com.sun.star.lib.uno.helper.WeakBase;
import com.sun.star.registry.XRegistryKey;
import com.sun.star.task.XJobExecutor;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.konfiguration.KonfigurationSheet;
import de.petanqueturniermanager.supermelee.SupermeleeTeamPaarungenSheet;
import de.petanqueturniermanager.supermelee.endrangliste.EndranglisteSheet;
import de.petanqueturniermanager.supermelee.meldeliste.AnmeldungenSheet;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_New;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_Update;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeTestDatenGenerator;
import de.petanqueturniermanager.supermelee.spielrunde.AktuelleSpielrundeSheet;
import de.petanqueturniermanager.supermelee.spielrunde.NaechsteSpielrundeSheet;
import de.petanqueturniermanager.supermelee.spielrunde.SpielrundenTestDaten;
import de.petanqueturniermanager.supermelee.spieltagrangliste.SpieltagRanglisteSheet;
import de.petanqueturniermanager.supermelee.spieltagrangliste.SpieltagRanglisteSheetSortOnly;

// Jobs
// https://wiki.openoffice.org/wiki/Documentation/DevGuide/WritingUNO/Jobs/Jobs
// https://wiki.openoffice.org/wiki/Documentation/DevGuide/WritingUNO/Jobs/Configuration

public final class PetanqueTurnierManagerImpl extends WeakBase implements XServiceInfo, XJobExecutor {
	private static final Logger logger = LogManager.getLogger(PetanqueTurnierManagerImpl.class);

	private final XComponentContext m_xContext;
	private static final String m_implementationName = PetanqueTurnierManagerImpl.class.getName();
	private static final String[] m_serviceNames = { "de.petanqueturniermanager.Turnier" };

	public PetanqueTurnierManagerImpl(XComponentContext context) {
		this.m_xContext = context;
	};

	public static XSingleComponentFactory __getComponentFactory(String sImplementationName) {
		XSingleComponentFactory xFactory = null;

		if (sImplementationName.equals(m_implementationName)) {
			xFactory = Factory.createComponentFactory(PetanqueTurnierManagerImpl.class, m_serviceNames);
		}
		return xFactory;
	}

	public static boolean __writeRegistryServiceInfo(XRegistryKey xRegistryKey) {
		return Factory.writeRegistryServiceInfo(m_implementationName, m_serviceNames, xRegistryKey);
	}

	// com.sun.star.lang.XServiceInfo:
	@Override
	public String getImplementationName() {
		return m_implementationName;
	}

	// com.sun.star.lang.XServiceInfo:
	@Override
	public boolean supportsService(String sService) {
		int len = m_serviceNames.length;

		for (int i = 0; i < len; i++) {
			if (sService.equals(m_serviceNames[i]))
				return true;
		}
		return false;
	}

	@Override
	public String[] getSupportedServiceNames() {
		return m_serviceNames;
	}

	// com.sun.star.task.XJobExecutor:
	@Override
	public void trigger(String action) {
		try {
			logger.info("Trigger " + action);

			switch (action) {
			// ------------------------------
			case "neue_meldeliste":
				new MeldeListeSheet_New(this.m_xContext).start();
				break;
			case "meldeliste":
				new MeldeListeSheet_Update(this.m_xContext).start();
				break;
			case "anmeldungen":
				new AnmeldungenSheet(this.m_xContext).start();
				break;
			case "naechste_spieltag":
				new MeldeListeSheet_New(this.m_xContext).start();
				break;
			case "meldeliste_testdaten":
				new MeldeListeTestDatenGenerator(this.m_xContext).start();
				break;
			case "supermelee_teampaarungen":
				new SupermeleeTeamPaarungenSheet(this.m_xContext).start();
				break;
			// ------------------------------
			case "aktuelle_spielrunde":
				new AktuelleSpielrundeSheet(this.m_xContext).start();
				break;
			case "naechste_spielrunde":
				new NaechsteSpielrundeSheet(this.m_xContext).start();
				break;
			case "spielrunden_testdaten":
				new SpielrundenTestDaten(this.m_xContext).start();
				break;
			// ------------------------------
			case "spieltag_rangliste":
				new SpieltagRanglisteSheet(this.m_xContext).start();
				break;
			// ------------------------------
			case "spieltag_rangliste_sort":
				new SpieltagRanglisteSheetSortOnly(this.m_xContext).start();
				break;
			// ------------------------------
			case "mittelhessenrunde_endrangliste":
				new EndranglisteSheet(this.m_xContext).start();
				break;
			case "konfiguration":
				new KonfigurationSheet(this.m_xContext).start();
				break;
			default:
				logger.error("Unknown action: " + action);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
}
