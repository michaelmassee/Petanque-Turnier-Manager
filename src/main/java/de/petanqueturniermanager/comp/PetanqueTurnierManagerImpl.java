/**
 * Erstellung : 26.03.2018 / Michael Massee
 **/
package de.petanqueturniermanager.comp;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.lang.XSingleComponentFactory;
import com.sun.star.lib.uno.helper.Factory;
import com.sun.star.lib.uno.helper.WeakBase;
import com.sun.star.registry.XRegistryKey;
import com.sun.star.task.XJobExecutor;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.comp.newrelease.DownloadExtension;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.forme.korunde.CadrageSheet;
import de.petanqueturniermanager.forme.korunde.KoGruppeABSheet;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.jedergegenjeden.meldeliste.JGJMeldeListeSheet_New;
import de.petanqueturniermanager.jedergegenjeden.meldeliste.JGJMeldeListeSheet_Update;
import de.petanqueturniermanager.jedergegenjeden.rangliste.JGJRanglisteDirektvergleichSheet;
import de.petanqueturniermanager.jedergegenjeden.rangliste.JGJRanglisteSheet;
import de.petanqueturniermanager.jedergegenjeden.rangliste.JGJRanglisteSheetSortOnly;
import de.petanqueturniermanager.jedergegenjeden.spielplan.JGJSpielPlanSheet;
import de.petanqueturniermanager.konfigdialog.properties.FarbenDialog;
import de.petanqueturniermanager.konfigdialog.properties.KopfFusszeilenDialog;
import de.petanqueturniermanager.konfigdialog.properties.TurnierDialog;
import de.petanqueturniermanager.liga.meldeliste.LigaMeldeListeSheet_Export;
import de.petanqueturniermanager.liga.meldeliste.LigaMeldeListeSheet_New;
import de.petanqueturniermanager.liga.meldeliste.LigaMeldeListeSheet_TestDaten;
import de.petanqueturniermanager.liga.meldeliste.LigaMeldeListeSheet_Update;
import de.petanqueturniermanager.liga.rangliste.LigaRanglisteDirektvergleichSheet;
import de.petanqueturniermanager.liga.rangliste.LigaRanglisteSheet;
import de.petanqueturniermanager.liga.rangliste.LigaRanglisteSheetSortOnly;
import de.petanqueturniermanager.liga.spielplan.LigaSpielPlanSheet;
import de.petanqueturniermanager.liga.spielplan.LigaSpielPlanSheet_TestDaten;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeTeteSheet_New;
import de.petanqueturniermanager.supermelee.SupermeleeTeamPaarungenSheet;
import de.petanqueturniermanager.supermelee.endrangliste.EndranglisteSheet;
import de.petanqueturniermanager.supermelee.endrangliste.EndranglisteSheet_Sort;
import de.petanqueturniermanager.supermelee.meldeliste.AnmeldungenSheet;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_NeuerSpieltag;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_New;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_TestDaten;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_Update;
import de.petanqueturniermanager.supermelee.meldeliste.TielnehmerSheet;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;
import de.petanqueturniermanager.supermelee.spielrunde.SpielrundeSheet_Naechste;
import de.petanqueturniermanager.supermelee.spielrunde.SpielrundeSheet_TestDaten;
import de.petanqueturniermanager.supermelee.spielrunde.SpielrundeSheet_Update;
import de.petanqueturniermanager.supermelee.spielrunde.SpielrundeSheet_Validator;
import de.petanqueturniermanager.supermelee.spieltagrangliste.SpieltagRanglisteSheet;
import de.petanqueturniermanager.supermelee.spieltagrangliste.SpieltagRanglisteSheet_SortOnly;
import de.petanqueturniermanager.supermelee.spieltagrangliste.SpieltagRanglisteSheet_TestDaten;
import de.petanqueturniermanager.supermelee.spieltagrangliste.SpieltagRangliste_Validator;

// Jobs
// https://wiki.openoffice.org/wiki/Documentation/DevGuide/WritingUNO/Jobs/Jobs
// https://wiki.openoffice.org/wiki/Documentation/DevGuide/WritingUNO/Jobs/Configuration

// Options Page, eigene Tab !
// https://wiki.openoffice.org/wiki/Documentation/DevGuide/Extensions/Options_Dialog

public final class PetanqueTurnierManagerImpl extends WeakBase implements XJobExecutor {
	private static final Logger logger = LogManager.getLogger(PetanqueTurnierManagerImpl.class);
	public static final File BASE_INTERNAL_DIR = new File(System.getProperty("user.home"), "/.petanqueturniermanager/");
	private static final String IMPLEMENTATION_NAME = PetanqueTurnierManagerImpl.class.getName();
	private static final String[] SERVICE_NAMES = { "de.petanqueturniermanager.MenuJobExecute" };

	private final XComponentContext xContext;

	// !! für jeden aufruf vom menue wird ein neuen Instance erstelt
	public PetanqueTurnierManagerImpl(final XComponentContext context) {
		xContext = context;
		PetanqueTurnierMngrSingleton.init(context);
		//		new NewReleaseChecker().udateNewReleaseInfo(context);
	}

	// -----------------------------------------------------------------------------------------------
	/**
	 * kommt zuerst
	 *
	 * @param xRegistryKey
	 * @return
	 */
	public static boolean __writeRegistryServiceInfo(XRegistryKey xRegistryKey) {
		return Factory.writeRegistryServiceInfo(IMPLEMENTATION_NAME, SERVICE_NAMES, xRegistryKey);
	}

	/**
	 * Gives a factory for creating the service.<br>
	 * This method is called by the <code>JavaLoader</code><br>
	 *
	 * @return Returns a <code>XSingleServiceFactory</code> for creating the component.<br>
	 * @see com.sun.star.comp.loader.JavaLoader<br>
	 * @param sImplementationName The implementation name of the component.<br>
	 */

	public static XSingleComponentFactory __getComponentFactory(String sImplementationName) {
		logger.debug("__getComponentFactory " + sImplementationName);

		XSingleComponentFactory xFactory = null;

		if (sImplementationName.equals(IMPLEMENTATION_NAME)) {
			xFactory = Factory.createComponentFactory(PetanqueTurnierManagerImpl.class, SERVICE_NAMES);
		}
		return xFactory;
	}

	// -----------------------------------------------------------------------------------------------
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

				if (!didHandle) {
					didHandle = handleSchweizer(action, currentSpreadsheet);
				}

				if (!didHandle) {
					didHandle = handleJerGegenJeden(action, currentSpreadsheet);
				}

				if (!didHandle) {
					didHandle = handleKonfiguration(action, currentSpreadsheet);
				}
			}

			if (!didHandle) {
				switch (action) {
				// ------------------------------
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

	private boolean handleKonfiguration(String action, WorkingSpreadsheet workingSpreadsheet) throws GenerateException {
		boolean didHandle = true;
		if (!action.toLowerCase().startsWith("konfiguration")) {
			return false;
		}

		TurnierSystem turnierSystemAusDocument = new DocumentPropertiesHelper(workingSpreadsheet)
				.getTurnierSystemAusDocument();

		if (turnierSystemAusDocument == TurnierSystem.KEIN) {
			MessageBox.from(workingSpreadsheet.getxContext(), MessageBoxTypeEnum.ERROR_OK).caption("Konfiguration")
					.message("Kein Turnier vorhanden").show();
			return true;
		}
		try {
			switch (action) {
			// ------------------------------
			case "konfiguration_turnier":
				new TurnierDialog(workingSpreadsheet).createDialog();
				break;

			case "konfiguration_kopffusszeilen":
				// Modal Dialog
				new KopfFusszeilenDialog(workingSpreadsheet).createDialog();
				break;
			case "konfiguration_farben":
				// Modal Dialog
				new FarbenDialog(workingSpreadsheet).createDialog();
				break;
			case "konfiguration_update_erstellt_mit_version":
				// schreibe die aktuelle Plugin Version im Turnier Dokument
				DocumentHelper.setDocErstelltMitVersion(workingSpreadsheet);
				break;
			default:
				didHandle = false;
			}
		} catch (com.sun.star.uno.Exception e) {
			logger.error(e.getMessage(), e);
		}
		return didHandle;
	}

	private boolean handleSuperMelee(String action, WorkingSpreadsheet workingSpreadsheet) throws GenerateException {
		boolean didHandle = true;

		switch (action) {
		// ------------------------------
		case "neue_meldeliste":
			new MeldeListeSheet_New(workingSpreadsheet).start();
			break;
		case "update_meldeliste":
			new MeldeListeSheet_Update(workingSpreadsheet).testTurnierVorhanden().start();
			break;
		case "anmeldungen":
			new AnmeldungenSheet(workingSpreadsheet).testTurnierVorhanden().backUpDocument().start();
			break;
		case "teilnehmer":
			new TielnehmerSheet(workingSpreadsheet).testTurnierVorhanden().start();
			break;
		case "naechste_spieltag":
			new MeldeListeSheet_NeuerSpieltag(workingSpreadsheet).testTurnierVorhanden().backUpDocument().start();
			break;
		case "meldeliste_testdaten":
			new MeldeListeSheet_TestDaten(workingSpreadsheet).start();
			break;
		case "supermelee_teampaarungen":
			new SupermeleeTeamPaarungenSheet(workingSpreadsheet).testTurnierVorhanden().start();
			break;
		// ------------------------------
		case "aktuelle_spielrunde":
			new SpielrundeSheet_Update(workingSpreadsheet).testTurnierVorhanden().backUpDocument()
					.backupDocumentAfterRun().start();
			break;
		case "naechste_spielrunde":
			new SpielrundeSheet_Naechste(workingSpreadsheet).testTurnierVorhanden().backUpDocument()
					.backupDocumentAfterRun().start();
			break;
		case "spielrunden_testdaten":
			new SpielrundeSheet_TestDaten(workingSpreadsheet).start();
			break;
		// ------------------------------
		case "spieltag_rangliste":
			new SpieltagRanglisteSheet(workingSpreadsheet).testTurnierVorhanden().backUpDocument().start();
			break;
		case "spieltag_rangliste_sort":
			new SpieltagRanglisteSheet_SortOnly(workingSpreadsheet).testTurnierVorhanden().start();
			break;
		case "SpieltagRanglisteSheet_TestDaten":
			new SpieltagRanglisteSheet_TestDaten(workingSpreadsheet).start();
			break;
		// ------------------------------
		case "supermelee_endrangliste":
			new EndranglisteSheet(workingSpreadsheet).testTurnierVorhanden().backUpDocument().start();
			break;
		case "supermelee_endrangliste_sort":
			new EndranglisteSheet_Sort(workingSpreadsheet).testTurnierVorhanden().start();
			break;
		case "supermelee_validate":
			new SpielrundeSheet_Validator(workingSpreadsheet).testTurnierVorhanden().start();
			break;
		case "supermelee_spieltagrangliste_validate":
			new SpieltagRangliste_Validator(workingSpreadsheet).testTurnierVorhanden().start();
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

	private boolean handleJerGegenJeden(String action, WorkingSpreadsheet workingSpreadsheet) throws GenerateException {
		boolean didHandle = true;
		if (!action.toLowerCase().startsWith("jgj")) {
			return false;
		}

		switch (action) {
		// ------------------------------
		case "jgj_neue_meldeliste":
			new JGJMeldeListeSheet_New(workingSpreadsheet).start();
			break;
		// ------------------------------
		case "jgj_update_meldeliste":
			new JGJMeldeListeSheet_Update(workingSpreadsheet).testTurnierVorhanden().backUpDocument().start();
			break;
		// ------------------------------
		case "jgj_spielplan":
			new JGJSpielPlanSheet(workingSpreadsheet).testTurnierVorhanden().backUpDocument().backupDocumentAfterRun()
					.start();
			break;
		case "jgj_rangliste":
			new JGJRanglisteSheet(workingSpreadsheet).testTurnierVorhanden().backUpDocument().start();
			break;
		case "jgj_rangliste_sortieren":
			new JGJRanglisteSheetSortOnly(workingSpreadsheet).testTurnierVorhanden().start();
			break;
		case "jgj_direktvergleich":
			new JGJRanglisteDirektvergleichSheet(workingSpreadsheet).testTurnierVorhanden().start();
			break;

		default:
			didHandle = false;
		}
		return didHandle;
	}

	private boolean handleLiga(String action, WorkingSpreadsheet workingSpreadsheet) throws GenerateException {
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
			new LigaMeldeListeSheet_Update(workingSpreadsheet).testTurnierVorhanden().backUpDocument().start();
			break;
		// ------------------------------
		case "liga_testdaten_meldeliste":
			new LigaMeldeListeSheet_TestDaten(workingSpreadsheet, true).start();
			break;
		case "liga_spielplan":
			new LigaSpielPlanSheet(workingSpreadsheet).testTurnierVorhanden().backUpDocument().backupDocumentAfterRun()
					.start();
			break;
		case "liga_rangliste":
			new LigaRanglisteSheet(workingSpreadsheet).testTurnierVorhanden().backUpDocument().start();
			break;
		case "liga_rangliste_sortieren":
			new LigaRanglisteSheetSortOnly(workingSpreadsheet).testTurnierVorhanden().start();
			break;
		case "liga_direktvergleich":
			new LigaRanglisteDirektvergleichSheet(workingSpreadsheet).testTurnierVorhanden().start();
			break;
		case "liga_spielplan_testdaten":
			new LigaSpielPlanSheet_TestDaten(workingSpreadsheet, false).start();
			break;
		case "liga_spielplan_testdaten_mit_freispiel":
			new LigaSpielPlanSheet_TestDaten(workingSpreadsheet, true).start();
			break;
		case "liga_export":
			new LigaMeldeListeSheet_Export(workingSpreadsheet).testTurnierVorhanden().start();
			break;
		default:
			didHandle = false;
		}
		return didHandle;
	}

	private boolean handleSchweizer(String action, WorkingSpreadsheet workingSpreadsheet) {
		boolean didHandle = true;
		if (!action.toLowerCase().startsWith("schweizer")) {
			return false;
		}
		switch (action) {
		// ------------------------------
		case "schweizer_neue_meldeliste_tete":
			new SchweizerMeldeListeTeteSheet_New(workingSpreadsheet).start();
			break;
		default:
			didHandle = false;
		}
		return didHandle;
	}

}
