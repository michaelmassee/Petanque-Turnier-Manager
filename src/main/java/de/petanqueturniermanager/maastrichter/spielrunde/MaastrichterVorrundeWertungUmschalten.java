/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.maastrichter.spielrunde;

import java.util.Arrays;
import java.util.Optional;

import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.basesheet.konfiguration.BaseKonfigurationSheet;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.basesheet.spielrunde.AbstractSpielrundeWertungUmschalten;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.maastrichter.konfiguration.MaastrichterKonfigurationSheet;
import de.petanqueturniermanager.maastrichter.rangliste.MaastrichterVorrundenRanglisteSheet;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerAbstractSpielrundeSheet;

/**
 * Maastrichter: aktive Vorrunde werten bzw. nicht werten.
 * <p>
 * Nur solange noch keine Finalrunde erzeugt wurde – danach hat die Vorrunden-Rangliste die
 * Finalgruppen bereits festgelegt.
 */
public class MaastrichterVorrundeWertungUmschalten extends AbstractSpielrundeWertungUmschalten {

	private final MaastrichterKonfigurationSheet konfigurationSheet;

	public MaastrichterVorrundeWertungUmschalten(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.MAASTRICHTER);
		konfigurationSheet = new MaastrichterKonfigurationSheet(workingSpreadsheet);
	}

	@Override
	protected BaseKonfigurationSheet getKonfigurationSheet() {
		return konfigurationSheet;
	}

	@Override
	protected String getRundenSchluesselPraefix() {
		return SheetMetadataHelper.SCHLUESSEL_MAASTRICHTER_VORRUNDE_PREFIX;
	}

	@Override
	protected Optional<String> sperrGrund() {
		XSpreadsheetDocument xDoc = getWorkingSpreadsheet().getWorkingSpreadsheetDocument();
		boolean finalrundeVorhanden = Arrays
				.stream(SheetMetadataHelper.getSchluesselMitPrefix(xDoc,
						SheetMetadataHelper.SCHLUESSEL_MAASTRICHTER_FINALRUNDE_PREFIX))
				.anyMatch(schluessel -> SheetMetadataHelper.findeSheet(xDoc, schluessel).isPresent());
		return finalrundeVorhanden
				? Optional.of(I18n.get("maastrichter.spielrunde.wertung.finalrunde.vorhanden"))
				: Optional.empty();
	}

	@Override
	protected int letzteTabellenSpalte() {
		return konfigurationSheet.isZeitplanAktiv() ? SchweizerAbstractSpielrundeSheet.ZEIT_SPALTE
				: SchweizerAbstractSpielrundeSheet.ERG_TEAM_B_SPALTE;
	}

	@Override
	protected void ranglisteNeuAufbauen(String rundenSchluessel) throws GenerateException {
		wennVorhanden(SheetMetadataHelper.SCHLUESSEL_MAASTRICHTER_VORRUNDEN_RANGLISTE,
				() -> new MaastrichterVorrundenRanglisteSheet(getWorkingSpreadsheet()).doRun());
	}
}
