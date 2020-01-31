/**
 * Erstellung 12.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.konfiguration;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.konfigdialog.ConfigProperty;
import de.petanqueturniermanager.liga.konfiguration.LigaKonfigurationSheetStarter;
import de.petanqueturniermanager.liga.konfiguration.LigaPropertiesSpalte;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeKonfigurationSheetStarter;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleePropertiesSpalte;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * @author Michael Massee
 *
 */
public class KonfigurationSingleton implements IKonfigurationKonstanten {
	static final Logger logger = LogManager.getLogger(KonfigurationSingleton.class);

	// List<ConfigProperty<?>> KONFIG_PROPERTIES
	public static List<ConfigProperty<?>> getKonfigProperties(WorkingSpreadsheet currentSpreadsheet) {
		try {

			TurnierSystem turnierSystem = getTurnierSystem(currentSpreadsheet);
			if (turnierSystem != null) {
				// leider oldschool fest verdrathen, weil reflection nicht funktioniert :-(
				switch (turnierSystem) {
				case LIGA:
					return LigaPropertiesSpalte.KONFIG_PROPERTIES;
				case SCHWEIZER_KO:
					break;
				case SUPERMELEE:
					return SuperMeleePropertiesSpalte.KONFIG_PROPERTIES;
				default:
					break;
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}

	// Vorhandene konfiguration Dokument finden und starten
	public static void start(WorkingSpreadsheet currentSpreadsheet) {
		TurnierSystem turnierSystem = getTurnierSystem(currentSpreadsheet);
		if (turnierSystem != null) {
			// leider oldschool fest verdrathen, weil reflection nicht funktioniert :-(
			switch (turnierSystem) {
			case LIGA:
				LigaKonfigurationSheetStarter.start(currentSpreadsheet);
				break;
			case SCHWEIZER_KO:
				break;
			case SUPERMELEE:
				SuperMeleeKonfigurationSheetStarter.start(currentSpreadsheet);
				break;
			default:
				ProcessBox.from().fehler("TurnierSystem unbekannt");
				break;
			}
		} else {
			ProcessBox.from().fehler("Noch kein Turnier vorhanden.");
		}
	}

	/**
	 * @param currentSpreadsheet
	 * @return
	 */
	private static TurnierSystem getTurnierSystem(WorkingSpreadsheet currentSpreadsheet) {
		DocumentPropertiesHelper docPropHelper = new DocumentPropertiesHelper(currentSpreadsheet);
		int turniersystemId = docPropHelper.getIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM, TurnierSystem.KEIN.getId());
		TurnierSystem turnierSystem = TurnierSystem.findById(turniersystemId);
		return turnierSystem;
	}
}
