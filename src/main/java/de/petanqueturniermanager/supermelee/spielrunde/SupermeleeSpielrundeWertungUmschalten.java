/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.supermelee.spielrunde;

import de.petanqueturniermanager.basesheet.konfiguration.BaseKonfigurationSheet;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.basesheet.spielrunde.AbstractSpielrundeWertungUmschalten;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeKonfigurationSheet;
import de.petanqueturniermanager.supermelee.spieltagrangliste.SpieltagRanglisteSheet;

/**
 * Supermêlée: aktive Spielrunde werten bzw. nicht werten.
 * <p>
 * Baut die Spieltag-Rangliste des betroffenen Spieltags neu auf. Die Endrangliste liest deren
 * Summen und zieht über die Sheet-Synchronisation nach.
 */
public class SupermeleeSpielrundeWertungUmschalten extends AbstractSpielrundeWertungUmschalten {

	private final SuperMeleeKonfigurationSheet konfigurationSheet;

	public SupermeleeSpielrundeWertungUmschalten(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.SUPERMELEE);
		konfigurationSheet = new SuperMeleeKonfigurationSheet(workingSpreadsheet);
	}

	@Override
	protected BaseKonfigurationSheet getKonfigurationSheet() {
		return konfigurationSheet;
	}

	@Override
	protected String getRundenSchluesselPraefix() {
		return SheetMetadataHelper.SCHLUESSEL_SUPERMELEE_SPIELRUNDE_PREFIX;
	}

	@Override
	protected boolean istRundenSchluessel(String schluessel) {
		// __PTM_SUPERMELEE_SPIELRUNDE_PLAN_ teilt das Präfix, ist aber kein Spielrunden-Blatt
		return !schluessel.startsWith(SheetMetadataHelper.SCHLUESSEL_SUPERMELEE_SPIELRUNDE_PLAN_PREFIX);
	}

	@Override
	protected void ranglisteNeuAufbauen(String rundenSchluessel) throws GenerateException {
		SpielTagNr spieltag = spieltagAusSchluessel(rundenSchluessel);
		wennVorhanden(SheetMetadataHelper.schluesselSpieltagRangliste(spieltag.getNr()),
				() -> new SpieltagRanglisteSheet(getWorkingSpreadsheet()).generate(spieltag));
	}

	/**
	 * {@code __PTM_SUPERMELEE_SPIELRUNDE_<spieltag>_<runde>__} → Spieltag
	 */
	static SpielTagNr spieltagAusSchluessel(String rundenSchluessel) {
		String kern = rundenSchluessel.substring(SheetMetadataHelper.SCHLUESSEL_SUPERMELEE_SPIELRUNDE_PREFIX.length());
		return SpielTagNr.from(Integer.parseInt(kern.substring(0, kern.indexOf('_'))));
	}
}
