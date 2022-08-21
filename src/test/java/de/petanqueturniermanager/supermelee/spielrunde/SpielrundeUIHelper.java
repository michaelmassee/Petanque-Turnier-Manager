package de.petanqueturniermanager.supermelee.spielrunde;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;

/**
 * Erstellung 21.08.2022 / Michael Massee
 */

public class SpielrundeUIHelper {

	private final BaseCalcUITest baseCalcUITest;

	public SpielrundeUIHelper(BaseCalcUITest baseCalcUITest) {
		this.baseCalcUITest = baseCalcUITest;
	}

	public void saveSpielPaarungenToJson(int spielRndNr, SheetHelper sheetHlp, WorkingSpreadsheet wkingSpreadsheet,
			int anzPaarungen, String jsonFileName) throws GenerateException {

		assertThat(jsonFileName).isNotNull();
		XSpreadsheet spielrunde = sheetHlp
				.findByName("1." + spielRndNr + ". " + AbstractSpielrundeSheet.PREFIX_SHEET_NAMEN);
		assertThat(spielrunde).isNotNull();

		RangePosition rangeSplrNr = RangePosition.from(SpielrundeSheet_Naechste.ERSTE_SPIELERNR_SPALTE,
				SpielrundeSheet_Naechste.ERSTE_DATEN_ZEILE, SpielrundeSheet_Naechste.ERSTE_SPIELERNR_SPALTE + 5, // Team A + B
				SpielrundeSheet_Naechste.ERSTE_DATEN_ZEILE + (anzPaarungen - 1)); // 10 paarungen
		RangeHelper rngHlpr = RangeHelper.from(spielrunde, wkingSpreadsheet.getWorkingSpreadsheetDocument(),
				rangeSplrNr);
		rngHlpr.getDataFromRange();
		baseCalcUITest.writeToJson(jsonFileName, rngHlpr.getDataFromRange());
	}

	public void restoreSpielPaarungenFromJson(int spielRndNr, SheetHelper sheetHlp, WorkingSpreadsheet wkingSpreadsheet,
			InputStream jsonFile) throws GenerateException {

		assertThat(jsonFile).isNotNull();

		XSpreadsheet spielrunde = sheetHlp
				.findByName("1." + spielRndNr + ". " + AbstractSpielrundeSheet.PREFIX_SHEET_NAMEN);
		assertThat(spielrunde).isNotNull();

		RangePosition rangeSplrNr = RangePosition.from(SpielrundeSheet_Naechste.ERSTE_SPIELERNR_SPALTE,
				SpielrundeSheet_Naechste.ERSTE_DATEN_ZEILE, SpielrundeSheet_Naechste.ERSTE_SPIELERNR_SPALTE + 5, // Team A + B
				SpielrundeSheet_Naechste.ERSTE_DATEN_ZEILE);
		RangeHelper rngHlpr = RangeHelper.from(spielrunde, wkingSpreadsheet.getWorkingSpreadsheetDocument(),
				rangeSplrNr);

		// feste spielrunden aus json dateien laden
		Gson gson = new GsonBuilder().create();
		RangeData rundeSpielerNrData = gson.fromJson(new BufferedReader(new InputStreamReader(jsonFile)),
				RangeData.class);
		rngHlpr.setDataInRange(rundeSpielerNrData, true);
	}

}
