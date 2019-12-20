/**
 * Erstellung 12.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.konfiguration;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.liga.konfiguration.LigaKonfigurationSheetStarter;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeKonfigurationSheetStarter;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * @author Michael Massee
 *
 */
public class KonfigurationStarter implements IKonfigurationKonstanten {

	// Vorhandene konfiguration Dokument finden und starten
	public static void start(WorkingSpreadsheet currentSpreadsheet) {
		DocumentPropertiesHelper docPropHelper = new DocumentPropertiesHelper(currentSpreadsheet);
		int turniersystemId = docPropHelper.getIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM);
		TurnierSystem turnierSystem = TurnierSystem.findById(turniersystemId);
		if (turnierSystem != null) {
			// leider oldschool fest verdrathen weil reflection nicht funktioniert :-(
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
}
