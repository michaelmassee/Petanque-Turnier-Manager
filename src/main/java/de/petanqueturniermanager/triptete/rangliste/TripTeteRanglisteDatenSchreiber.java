package de.petanqueturniermanager.triptete.rangliste;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.algorithmen.triptete.TripTeteBegegnungErgebnis;
import de.petanqueturniermanager.algorithmen.triptete.TripTeteRangliste;
import de.petanqueturniermanager.algorithmen.triptete.TripTeteTeamErgebnis;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.triptete.meldeliste.TripTeteMeldeListeSheetUpdate;
import de.petanqueturniermanager.triptete.spielplan.TripTeteSpielPlanLeser;
import de.petanqueturniermanager.triptete.spielplan.TripTeteSpielPlanSheet;

/**
 * Schreibt die Daten-Zeilen der Trip-Tête-Rangliste direkt als Werte (ohne Formeln):
 * Team-Nr, Name, Rang, Begegnungssiege (Punkte), Partiensiege (Siege), Kugeln+ (spPunkte), Kugel-Δ.
 * <p>
 * Liest die Spielplan-Daten über {@link TripTeteSpielPlanLeser}, berechnet die Rangliste
 * via {@link TripTeteRangliste}-Algorithmus und schreibt alles als Block per RangeHelper.
 */
final class TripTeteRanglisteDatenSchreiber {

	private final ISheet ranglisteSheet;
	private final TripTeteMeldeListeSheetUpdate meldeListe;
	private final WorkingSpreadsheet workingSpreadsheet;

	private TripTeteRanglisteDatenSchreiber(ISheet ranglisteSheet, TripTeteMeldeListeSheetUpdate meldeListe,
			WorkingSpreadsheet workingSpreadsheet) {
		this.ranglisteSheet = ranglisteSheet;
		this.meldeListe = meldeListe;
		this.workingSpreadsheet = workingSpreadsheet;
	}

	static TripTeteRanglisteDatenSchreiber from(ISheet ranglisteSheet, TripTeteMeldeListeSheetUpdate meldeListe,
			WorkingSpreadsheet workingSpreadsheet) {
		return new TripTeteRanglisteDatenSchreiber(ranglisteSheet, meldeListe, workingSpreadsheet);
	}

	void schreibeDaten() throws GenerateException {
		TeamMeldungen meldungen = meldeListe.getAlleMeldungen();
		if (meldungen.teams().isEmpty()) {
			return;
		}

		var spielPlanSheet = new TripTeteSpielPlanSheet(workingSpreadsheet);
		List<TripTeteBegegnungErgebnis> begegnungen = TripTeteSpielPlanLeser.from(spielPlanSheet)
				.leseBegegnungen();

		var rangliste = new TripTeteRangliste();
		meldungen.teams().forEach(rangliste::addTeam);
		begegnungen.forEach(rangliste::addBegegnung);

		List<TripTeteTeamErgebnis> sortiert = rangliste.getRangliste();
		Map<Integer, String> namen = namenMap();

		RangeData rangeData = new RangeData();
		int rang = 1;
		for (int i = 0; i < sortiert.size(); i++) {
			if (i > 0 && sortiert.get(i).compareTo(sortiert.get(i - 1)) != 0) {
				rang = i + 1;
			}
			TripTeteTeamErgebnis ergebnis = sortiert.get(i);
			int teamNr = ergebnis.getTeam().getNr();
			RowData row = rangeData.addNewRow();
			row.newInt(teamNr);
			row.newString(namen.getOrDefault(teamNr, ""));
			row.newInt(rang);
			row.newInt(ergebnis.getBegegnungenGewonnen());
			row.newInt(ergebnis.getPartienGewonnen());
			row.newInt(ergebnis.getKugelnPlus());
			row.newInt(ergebnis.getKugelDiff());
		}

		Position startPos = Position.from(TripTeteRanglisteSheet.TEAM_NR_SPALTE,
				TripTeteRanglisteSheet.ERSTE_DATEN_ZEILE);
		RangeHelper.from(ranglisteSheet, rangeData.getRangePosition(startPos)).setDataInRange(rangeData);
	}

	private Map<Integer, String> namenMap() throws GenerateException {
		var ms = meldeListe.getMeldungenSpalte();
		XSpreadsheet sheet = meldeListe.getXSpreadSheet();
		SheetHelper sh = meldeListe.getSheetHelper();
		int nrSpalte = ms.getSpielerNrSpalte();
		int letzte = ms.getLetzteMitDatenZeileInSpielerNrSpalte();

		Map<Integer, String> map = new HashMap<>();
		for (int z = ms.getErsteDatenZiele(); z <= letzte; z++) {
			int nr = sh.getIntFromCell(sheet, Position.from(nrSpalte, z));
			if (nr > 0) {
				map.put(nr, ms.leseSpielerNameZeile(sheet, z));
			}
		}
		return map;
	}
}
