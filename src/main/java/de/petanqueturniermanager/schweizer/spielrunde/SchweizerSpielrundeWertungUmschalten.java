/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.schweizer.spielrunde;

import de.petanqueturniermanager.basesheet.konfiguration.BaseKonfigurationSheet;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.basesheet.spielrunde.AbstractSpielrundeWertungUmschalten;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerKonfigurationSheet;
import de.petanqueturniermanager.schweizer.rangliste.SchweizerRanglisteSheet;

/**
 * Schweizer System: aktive Spielrunde werten bzw. nicht werten.
 */
public class SchweizerSpielrundeWertungUmschalten extends AbstractSpielrundeWertungUmschalten {

	private final SchweizerKonfigurationSheet konfigurationSheet;

	public SchweizerSpielrundeWertungUmschalten(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.SCHWEIZER);
		konfigurationSheet = new SchweizerKonfigurationSheet(workingSpreadsheet);
	}

	@Override
	protected BaseKonfigurationSheet getKonfigurationSheet() {
		return konfigurationSheet;
	}

	@Override
	protected String getRundenSchluesselPraefix() {
		return SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_SPIELRUNDE_PREFIX;
	}

	@Override
	protected int letzteTabellenSpalte() {
		return konfigurationSheet.isZeitplanAktiv() ? SchweizerAbstractSpielrundeSheet.ZEIT_SPALTE
				: SchweizerAbstractSpielrundeSheet.ERG_TEAM_B_SPALTE;
	}

	@Override
	protected void ranglisteNeuAufbauen(String rundenSchluessel) throws GenerateException {
		wennVorhanden(SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_RANGLISTE,
				() -> new SchweizerRanglisteSheet(getWorkingSpreadsheet()).doRun());
	}
}
