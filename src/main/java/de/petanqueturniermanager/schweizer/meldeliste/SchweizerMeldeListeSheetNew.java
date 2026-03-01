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

public class SchweizerMeldeListeSheetNew extends AbstractSchweizerMeldeListeSheet {
	private static final Logger logger = LogManager.getLogger(SchweizerMeldeListeSheetNew.class);

	public SchweizerMeldeListeSheetNew(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
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
			param = SchweizerTurnierParameterDialog.from(getWorkingSpreadsheet()).show(Formation.DOUBLETTE, false, false);
		} catch (Exception e) {
			logger.error("{} Fehler beim Anzeigen des Parameterdialogs: {}", e.getMessage(), e);
			throw new GenerateException("Fehler beim Anzeigen des Parameterdialogs: " + e.getMessage());
		}

		if (param.isEmpty()) {
			return; // Benutzer hat abgebrochen – kein Eingriff ins Dokument
		}

		getSheetHelper().removeAllSheetsExclude();

		if (NewSheet.from(this, SHEETNAME).pos(DefaultSheetPos.MELDELISTE).hideGrid().tabColor(SHEET_COLOR)
				.setDocVersionWhenNew().create().isDidCreate()) {
			getKonfigurationSheet().setMeldeListeFormation(param.get().formation);
			getKonfigurationSheet().setMeldeListeTeamnameAnzeigen(param.get().teamnameAnzeigen);
			getKonfigurationSheet().setMeldeListeVereinsnameAnzeigen(param.get().vereinsnameAnzeigen);
			upDateSheet();
		}
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

}
