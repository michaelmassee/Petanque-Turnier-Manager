/**
 * Erstellung 12.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.konfiguration;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.jedergegenjeden.konfiguration.JGJPropertiesSpalte;
import de.petanqueturniermanager.konfigdialog.ConfigProperty;
import de.petanqueturniermanager.ko.konfiguration.KoPropertiesSpalte;
import de.petanqueturniermanager.liga.konfiguration.LigaPropertiesSpalte;
import de.petanqueturniermanager.maastrichter.konfiguration.MaastrichterPropertiesSpalte;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerPropertiesSpalte;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleePropertiesSpalte;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * @author Michael Massee
 *
 */
public class KonfigurationSingleton {
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
				case MAASTRICHTER:
					return MaastrichterPropertiesSpalte.KONFIG_PROPERTIES;
				case SUPERMELEE:
					return SuperMeleePropertiesSpalte.KONFIG_PROPERTIES;
				case SCHWEIZER:
					return SchweizerPropertiesSpalte.KONFIG_PROPERTIES;
				case JGJ:
					return JGJPropertiesSpalte.KONFIG_PROPERTIES;
				case KO:
					return KoPropertiesSpalte.KONFIG_PROPERTIES;
				default:
					logger.error("TurnierSystem ungültig " + turnierSystem.getBezeichnung());
					break;
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}

	/**
	 * @param currentSpreadsheet
	 * @return
	 */
	private static TurnierSystem getTurnierSystem(WorkingSpreadsheet currentSpreadsheet) {
		DocumentPropertiesHelper docPropHelper = new DocumentPropertiesHelper(currentSpreadsheet);
		int turniersystemId = docPropHelper.getIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM,
				TurnierSystem.KEIN.getId());
		TurnierSystem turnierSystem = TurnierSystem.findById(turniersystemId);
		return turnierSystem;
	}
}
