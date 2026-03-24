/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.maastrichter.meldeliste;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeKonstanten;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.NewTestDatenValidator;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.maastrichter.konfiguration.MaastrichterKonfigurationSheet;
import de.petanqueturniermanager.schweizer.konfiguration.SpielplanTeamAnzeige;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetUpdate;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Erstellt eine neue Maastrichter-Meldeliste samt Konfiguration.
 * Verwendet das gleiche Format wie die Schweizer Meldeliste.
 */
public class MaastrichterMeldeListeSheetNew extends SheetRunner implements ISheet, MeldeListeKonstanten {

	private static final Logger logger = LogManager.getLogger(MaastrichterMeldeListeSheetNew.class);

	private static final String METADATA_SCHLUESSEL = SheetMetadataHelper.SCHLUESSEL_MAASTRICHTER_MELDELISTE;

	private final MaastrichterKonfigurationSheet konfigurationSheet;

	public MaastrichterMeldeListeSheetNew(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.MAASTRICHTER, "Maastrichter-Meldeliste");
		konfigurationSheet = new MaastrichterKonfigurationSheet(workingSpreadsheet);
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return SheetMetadataHelper.findeSheetUndHeile(
				getWorkingSpreadsheet().getWorkingSpreadsheetDocument(), METADATA_SCHLUESSEL, SheetNamen.LEGACY_MELDELISTE);
	}

	@Override
	public TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}

	@Override
	protected MaastrichterKonfigurationSheet getKonfigurationSheet() {
		return konfigurationSheet;
	}

	@Override
	protected boolean isUpdateKonfigurationSheetBeforeDoRun() {
		return false;
	}

	@Override
	protected void doRun() throws GenerateException {
		if (!NewTestDatenValidator.from(getWorkingSpreadsheet(), getSheetHelper(), TurnierSystem.MAASTRICHTER)
				.prefix(getLogPrefix()).validate()) {
			return;
		}

		Optional<MaastrichterTurnierParameterDialog.TurnierParameter> param;
		try {
			param = MaastrichterTurnierParameterDialog.from(getWorkingSpreadsheet()).show(
					Formation.DOUBLETTE, false, false, SpielplanTeamAnzeige.NR,
					konfigurationSheet.getRankingModus(),
					konfigurationSheet.getAnzVorrunden(),
					konfigurationSheet.getSpielbaumTeamAnzeige(),
					konfigurationSheet.getSpielbaumSpielbahn(),
					konfigurationSheet.isSpielbaumSpielUmPlatz3(),
					konfigurationSheet.getGruppenGroesse(),
					konfigurationSheet.getMinRestGroesse());
		} catch (Exception e) {
			logger.error("{} Fehler beim Anzeigen des Parameterdialogs: {}", e.getMessage(), e);
			throw new GenerateException("Fehler beim Anzeigen des Parameterdialogs: " + e.getMessage());
		}

		if (param.isEmpty()) {
			return;
		}

		// TurnierSystem + Page Styles setzen
		konfigurationSheet.update();

		getSheetHelper().removeAllSheetsExclude();

		konfigurationSheet.setRankingModus(param.get().rankingModus);
		konfigurationSheet.setAnzVorrunden(param.get().anzVorrunden);
		konfigurationSheet.setAktiveSpielRunde(SpielRundeNr.from(1));
		konfigurationSheet.setSpielbaumTeamAnzeige(param.get().spielbaumTeamAnzeige);
		konfigurationSheet.setSpielbaumSpielbahn(param.get().spielbaumSpielbahn);
		konfigurationSheet.setSpielbaumSpielUmPlatz3(param.get().spielUmPlatz3);
		konfigurationSheet.setGruppenGroesse(param.get().gruppenGroesse);
		konfigurationSheet.setMinRestGroesse(param.get().minRestGroesse);

		erstelleMeldeliste(param.get().formation, param.get().teamnameAnzeigen, param.get().vereinsnameAnzeigen,
				param.get().spielplanTeamAnzeige);
	}

	/**
	 * Erstellt die Meldeliste mit den angegebenen Parametern.
	 * Verwendet intern die Schweizer-Delegate-Logik, da das Format identisch ist.
	 */
	public void erstelleMeldeliste(Formation formation, boolean teamnameAnzeigen, boolean vereinsnameAnzeigen,
			SpielplanTeamAnzeige spielplanTeamAnzeige) throws GenerateException {
		if (NewSheet.from(this, SheetNamen.meldeliste()).pos(DefaultSheetPos.MELDELISTE).hideGrid().tabColor(SHEET_COLOR)
				.setDocVersionWhenNew().create().isDidCreate()) {
			konfigurationSheet.setMeldeListeFormation(formation);
			konfigurationSheet.setMeldeListeTeamnameAnzeigen(teamnameAnzeigen);
			konfigurationSheet.setMeldeListeVereinsnameAnzeigen(vereinsnameAnzeigen);
			konfigurationSheet.setSpielplanTeamAnzeige(spielplanTeamAnzeige);
			SheetMetadataHelper.schreibeSheetMetadaten(
					getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
					getXSpreadSheet(), METADATA_SCHLUESSEL);
			// Layout via Schweizer-MeldeListeSheetUpdate aufbauen (Format identisch)
			new SchweizerMeldeListeSheetUpdate(getWorkingSpreadsheet()).upDateSheet();
		}
	}

}
