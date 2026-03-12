/**
 * Erstellung : 01.03.2024 / Michael Massee
 **/

package de.petanqueturniermanager.schweizer.meldeliste;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.uno.Exception;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.NewTestDatenValidator;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerSheet;
import de.petanqueturniermanager.schweizer.konfiguration.SpielplanTeamAnzeige;

public class SchweizerMeldeListeSheetNew extends AbstractSchweizerMeldeListeSheet {
	private static final Logger logger = LogManager.getLogger(SchweizerMeldeListeSheetNew.class);

	public SchweizerMeldeListeSheetNew(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
	}

	@Override
	protected boolean isUpdateKonfigurationSheetBeforeDoRun() {
		return false;
	}

	@Override
	protected void doRun() throws GenerateException {
		if (!NewTestDatenValidator.from(getWorkingSpreadsheet(), getSheetHelper(), SchweizerSheet.TURNIERSYSTEM)
				.prefix(getLogPrefix()).validate()) {
			return;
		}

		// Dialog zuerst – bei Abbruch keine Änderungen am Dokument
		Optional<SchweizerTurnierParameterDialog.TurnierParameter> param;
		try {
			param = SchweizerTurnierParameterDialog.from(getWorkingSpreadsheet()).show(Formation.DOUBLETTE, false, false,
				SpielplanTeamAnzeige.NR, getKonfigurationSheet().getRankingModus());
		} catch (Exception e) {
			logger.error("{} Fehler beim Anzeigen des Parameterdialogs: {}", e.getMessage(), e);
			throw new GenerateException("Fehler beim Anzeigen des Parameterdialogs: " + e.getMessage());
		}

		if (param.isEmpty()) {
			return; // Benutzer hat abgebrochen – keine Dokument-Änderungen
		}

		// Erst nach Bestätigung: TurnierSystem + Page Styles setzen
		getKonfigurationSheet().update();

		getSheetHelper().removeAllSheetsExclude();
		getKonfigurationSheet().setRankingModus(param.get().rankingModus);
		createMeldelisteWithParams(param.get().formation, param.get().teamnameAnzeigen, param.get().vereinsnameAnzeigen,
				param.get().spielplanTeamAnzeige);
	}

	/**
	 * Erstellt die Meldeliste mit den angegebenen Parametern ohne Dialog.
	 * Wird auch von TestDaten-Klassen aufgerufen.
	 */
	public void createMeldelisteWithParams(Formation formation, boolean teamnameAnzeigen, boolean vereinsnameAnzeigen)
			throws GenerateException {
		createMeldelisteWithParams(formation, teamnameAnzeigen, vereinsnameAnzeigen, SpielplanTeamAnzeige.NR);
	}

	public void createMeldelisteWithParams(Formation formation, boolean teamnameAnzeigen, boolean vereinsnameAnzeigen,
			SpielplanTeamAnzeige spielplanTeamAnzeige) throws GenerateException {
		if (NewSheet.from(this, SHEETNAME).pos(DefaultSheetPos.MELDELISTE).hideGrid().tabColor(SHEET_COLOR)
				.setDocVersionWhenNew().create().isDidCreate()) {
			getKonfigurationSheet().setMeldeListeFormation(formation);
			getKonfigurationSheet().setMeldeListeTeamnameAnzeigen(teamnameAnzeigen);
			getKonfigurationSheet().setMeldeListeVereinsnameAnzeigen(vereinsnameAnzeigen);
			getKonfigurationSheet().setSpielplanTeamAnzeige(spielplanTeamAnzeige);
			upDateSheet();
		}
	}


}
