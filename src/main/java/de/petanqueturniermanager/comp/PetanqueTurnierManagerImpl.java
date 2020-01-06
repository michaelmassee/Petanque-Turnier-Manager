/**
* Erstellung : 26.03.2018 / Michael Massee
**/
package de.petanqueturniermanager.comp;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kohsuke.github.GHRelease;

import com.sun.star.lang.XServiceInfo;
import com.sun.star.lang.XSingleComponentFactory;
import com.sun.star.lib.uno.helper.Factory;
import com.sun.star.lib.uno.helper.WeakBase;
import com.sun.star.registry.XRegistryKey;
import com.sun.star.task.XJobExecutor;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.konfiguration.KonfigurationStarter;
import de.petanqueturniermanager.comp.newrelease.DownloadExtension;
import de.petanqueturniermanager.comp.newrelease.NewReleaseChecker;
import de.petanqueturniermanager.forme.korunde.CadrageSheet;
import de.petanqueturniermanager.forme.korunde.KoGruppeABSheet;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.liga.meldeliste.LigaMeldeListeSheet_New;
import de.petanqueturniermanager.liga.meldeliste.LigaMeldeListeSheet_TestDaten;
import de.petanqueturniermanager.liga.meldeliste.LigaMeldeListeSheet_Update;
import de.petanqueturniermanager.liga.rangliste.LigaRanglisteSheet;
import de.petanqueturniermanager.liga.spielplan.LigaSpielPlanSheet;
import de.petanqueturniermanager.liga.spielplan.LigaSpielPlanSheet_TestDaten;
import de.petanqueturniermanager.supermelee.SupermeleeTeamPaarungenSheet;
import de.petanqueturniermanager.supermelee.endrangliste.EndranglisteSheet;
import de.petanqueturniermanager.supermelee.endrangliste.EndranglisteSheet_Sort;
import de.petanqueturniermanager.supermelee.meldeliste.AnmeldungenSheet;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_NeuerSpieltag;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_New;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_TestDaten;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_Update;
import de.petanqueturniermanager.supermelee.meldeliste.TielnehmerSheet;
import de.petanqueturniermanager.supermelee.spielrunde.SpielrundeSheet_Naechste;
import de.petanqueturniermanager.supermelee.spielrunde.SpielrundeSheet_TestDaten;
import de.petanqueturniermanager.supermelee.spielrunde.SpielrundeSheet_Update;
import de.petanqueturniermanager.supermelee.spielrunde.SpielrundeSheet_Validator;
import de.petanqueturniermanager.supermelee.spieltagrangliste.SpieltagRanglisteSheet;
import de.petanqueturniermanager.supermelee.spieltagrangliste.SpieltagRanglisteSheet_SortOnly;
import de.petanqueturniermanager.supermelee.spieltagrangliste.SpieltagRanglisteSheet_TestDaten;

// Jobs
// https://wiki.openoffice.org/wiki/Documentation/DevGuide/WritingUNO/Jobs/Jobs
// https://wiki.openoffice.org/wiki/Documentation/DevGuide/WritingUNO/Jobs/Configuration

// Options Page, eigene Tab !
// https://wiki.openoffice.org/wiki/Documentation/DevGuide/Extensions/Options_Dialog

public final class PetanqueTurnierManagerImpl extends WeakBase implements XServiceInfo, XJobExecutor {
	private static final Logger logger = LogManager.getLogger(PetanqueTurnierManagerImpl.class);
	public static final File BASE_INTERNAL_DIR = new File(System.getProperty("user.home"), "/.petanqueturniermanager/");
	private static final String m_implementationName = PetanqueTurnierManagerImpl.class.getName();
	private static final String[] m_serviceNames = { "de.petanqueturniermanager.Turnier" };
	private static boolean didInformAboutNewRelease = false; // static weil immer ein neuen instance

	private final XComponentContext xContext;

	// !! für jeden aufruf vom menue wird ein neuen Instance erstelt
	public PetanqueTurnierManagerImpl(XComponentContext context) {
		xContext = context;
		TopWindowListener.addThisListenerOnce(new WorkingSpreadsheet(xContext));
		checkForUpdate();

		try {
			ProcessBox.init(context);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

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
			boolean didHandle = false;
			WorkingSpreadsheet currentSpreadsheet = new WorkingSpreadsheet(xContext);

			if (!didHandle) {
				ProcessBox.from().visible().clearWennNotRunning().info("Start " + action);
				didHandle = handleSuperMelee(action, currentSpreadsheet);

				if (!didHandle) {
					didHandle = handleFormee(action, currentSpreadsheet);
				}

				if (!didHandle) {
					didHandle = handleLiga(action, currentSpreadsheet);
				}

			}

			if (!didHandle) {
				switch (action) {

				// ------------------------------
				case "turnierkonfiguration":
					// Konfiguration vorhanden ? dann starten
					KonfigurationStarter.start(currentSpreadsheet);
					didHandle = true;
					break;
				case "downloadExtension":
					new DownloadExtension(currentSpreadsheet).start();
					didHandle = true;
					break;
				case "abbruch":
					didHandle = true;
					SheetRunner.cancelRunner();
					break;
				}
			}

			if (!didHandle) {
				ProcessBox.from().fehler("ungueltige Aktion " + action);
				logger.error("Unknown action: " + action);
			}
		} catch (Exception e) {
			ProcessBox.from().fehler(e.getMessage());
			logger.error(e.getMessage(), e);
		}
	}

	private void checkForUpdate() {
		NewReleaseChecker newReleaseChecker = new NewReleaseChecker();
		if (!didInformAboutNewRelease) {
			boolean isnewRelease = newReleaseChecker.checkForNewRelease(xContext);
			if (isnewRelease) {
				didInformAboutNewRelease = true;
				GHRelease readLatestRelease = newReleaseChecker.readLatestRelease();
				MessageBox.from(xContext, MessageBoxTypeEnum.INFO_OK).caption("Neue Version verfügbar")
						.message("Ein neue Version (" + readLatestRelease.getName() + ") von Pétanque-Turnier-Manager ist verfügbar.").show();
			}
		}
		newReleaseChecker.runUpdateOnceThread(); // update release info
	}

	private boolean handleSuperMelee(String action, WorkingSpreadsheet workingSpreadsheet) {
		boolean didHandle = true;

		switch (action) {
		// ------------------------------
		case "neue_meldeliste":
			new MeldeListeSheet_New(workingSpreadsheet).start();
			break;
		case "update_meldeliste":
			new MeldeListeSheet_Update(workingSpreadsheet).start();
			break;
		case "anmeldungen":
			new AnmeldungenSheet(workingSpreadsheet).start();
			break;
		case "teilnehmer":
			new TielnehmerSheet(workingSpreadsheet).start();
			break;
		case "naechste_spieltag":
			new MeldeListeSheet_NeuerSpieltag(workingSpreadsheet).start();
			break;
		case "meldeliste_testdaten":
			new MeldeListeSheet_TestDaten(workingSpreadsheet).start();
			break;
		case "supermelee_teampaarungen":
			new SupermeleeTeamPaarungenSheet(workingSpreadsheet).start();
			break;
		// ------------------------------
		case "aktuelle_spielrunde":
			new SpielrundeSheet_Update(workingSpreadsheet).start();
			break;
		case "naechste_spielrunde":
			new SpielrundeSheet_Naechste(workingSpreadsheet).start();
			break;
		case "spielrunden_testdaten":
			new SpielrundeSheet_TestDaten(workingSpreadsheet).start();

			break;
		// ------------------------------
		case "spieltag_rangliste":
			new SpieltagRanglisteSheet(workingSpreadsheet).start();
			break;
		case "spieltag_rangliste_sort":
			new SpieltagRanglisteSheet_SortOnly(workingSpreadsheet).start();
			break;
		case "SpieltagRanglisteSheet_TestDaten":
			new SpieltagRanglisteSheet_TestDaten(workingSpreadsheet).start();
			break;
		// ------------------------------
		case "supermelee_endrangliste":
			new EndranglisteSheet(workingSpreadsheet).start();
			break;
		case "supermelee_endrangliste_sort":
			new EndranglisteSheet_Sort(workingSpreadsheet).start();
			break;
		case "supermelee_validate":
			new SpielrundeSheet_Validator(workingSpreadsheet).start();
			break;
		default:
			didHandle = false;
		}
		return didHandle;
	}

	private boolean handleFormee(String action, WorkingSpreadsheet workingSpreadsheet) {
		boolean didHandle = true;

		switch (action) {
		// ------------------------------
		case "cadrage":
			new CadrageSheet(workingSpreadsheet).start();
			break;
		// ------------------------------
		case "koRundeAB":
			new KoGruppeABSheet(workingSpreadsheet).start();
			break;
		default:
			didHandle = false;
		}
		return didHandle;
	}

	private boolean handleLiga(String action, WorkingSpreadsheet workingSpreadsheet) {
		boolean didHandle = true;
		if (!action.toLowerCase().startsWith("liga")) {
			return false;
		}

		switch (action) {
		// ------------------------------
		case "liga_neue_meldeliste":
			new LigaMeldeListeSheet_New(workingSpreadsheet).start();
			break;
		// ------------------------------
		case "liga_update_meldeliste":
			new LigaMeldeListeSheet_Update(workingSpreadsheet).start();
			break;
		// ------------------------------
		case "liga_testdaten_meldeliste":
			new LigaMeldeListeSheet_TestDaten(workingSpreadsheet, true).start();
			break;
		case "liga_spielplan":
			new LigaSpielPlanSheet(workingSpreadsheet).start();
			break;
		case "liga_rangliste":
			new LigaRanglisteSheet(workingSpreadsheet).start();
			break;
		case "liga_spielplan_testdaten":
			new LigaSpielPlanSheet_TestDaten(workingSpreadsheet, false).start();
			break;
		case "liga_spielplan_testdaten_mit_freispiel":
			new LigaSpielPlanSheet_TestDaten(workingSpreadsheet, true).start();
			break;
		default:
			didHandle = false;
		}
		return didHandle;
	}

}
