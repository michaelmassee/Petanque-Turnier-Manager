package de.petanqueturniermanager.triptete.rangliste;

import java.util.List;
import java.util.Map;

import de.petanqueturniermanager.algorithmen.triptete.TripTeteBegegnungErgebnis;
import de.petanqueturniermanager.algorithmen.triptete.TripTeteRangliste;
import de.petanqueturniermanager.algorithmen.triptete.TripTeteTeamErgebnis;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.triptete.meldeliste.TripTeteMeldeListeSheetUpdate;
import de.petanqueturniermanager.triptete.spielplan.TripTeteSpielPlanLeser;
import de.petanqueturniermanager.triptete.spielplan.TripTeteSpielPlanSheet;

/**
 * Schreibt die Daten-Zeilen der Trip-Tête-Rangliste direkt als Werte (ohne Formeln):
 * Team-Nr, Name, Rang, BegGew+, BegVer-, PartGew+, PartVer-, PartΔ,
 * SpPnkt+, SpPnkt-, SpPnktΔ, Begegnungen.
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

	/**
	 * Ermittelt die Ranglistenzeilen aller gelisteten Teams: aktive Teams und Teams mit verbuchter Begegnung (auch nach
	 * einem Ausstieg). Die Anzahl der Zeilen kann daher größer sein als die Anzahl aktiver Meldungen.
	 */
	RangeData erstelleZeilen() throws GenerateException {
		RangeData rangeData = new RangeData();
		TeamMeldungen meldungen = meldeListe.getAlleMeldungen();
		if (meldungen.teams().isEmpty()) {
			return rangeData;
		}

		var spielPlanSheet = new TripTeteSpielPlanSheet(workingSpreadsheet);
		List<TripTeteBegegnungErgebnis> begegnungen = TripTeteSpielPlanLeser.from(spielPlanSheet)
				.leseBegegnungen();

		var rangliste = new TripTeteRangliste();
		meldungen.teams().forEach(rangliste::addTeam);
		begegnungen.forEach(rangliste::addBegegnung);

		List<TripTeteTeamErgebnis> sortiert = rangliste.getRangliste();
		Map<Integer, String> namen = meldeListe.leseTeamNamenMap();

		int rang = 1;
		for (int i = 0; i < sortiert.size(); i++) {
			if (i > 0 && sortiert.get(i).compareTo(sortiert.get(i - 1)) != 0) {
				rang = i + 1;
			}
			schreibeZeile(rangeData.addNewRow(), sortiert.get(i), rang, namen);
		}
		return rangeData;
	}

	void schreibe(RangeData zeilen) throws GenerateException {
		if (zeilen.isEmpty()) {
			return;
		}
		Position startPos = Position.from(TripTeteRanglisteSheet.TEAM_NR_SPALTE,
				TripTeteRanglisteSheet.ERSTE_DATEN_ZEILE);
		RangeHelper.from(ranglisteSheet, zeilen.getRangePosition(startPos)).setDataInRange(zeilen);
	}

	private static void schreibeZeile(RowData row, TripTeteTeamErgebnis ergebnis, int rang,
			Map<Integer, String> namen) {
		int teamNr = ergebnis.getTeam().getNr();
		row.newInt(teamNr);
		row.newString(namen.getOrDefault(teamNr, ""));
		row.newInt(rang);
		row.newInt(ergebnis.getBegegnungenGewonnen());
		row.newInt(ergebnis.getBegegnungenVerloren());
		row.newInt(ergebnis.getPartienGewonnen());
		row.newInt(ergebnis.getPartienVerloren());
		row.newInt(ergebnis.getPartienGewonnen() - ergebnis.getPartienVerloren());
		row.newInt(ergebnis.getSpielPunktePlus());
		row.newInt(ergebnis.getSpielPunkteMinus());
		row.newInt(ergebnis.getSpielPunkteDiff());
		row.newInt(ergebnis.getBegegnungenGespielt());
	}
}
