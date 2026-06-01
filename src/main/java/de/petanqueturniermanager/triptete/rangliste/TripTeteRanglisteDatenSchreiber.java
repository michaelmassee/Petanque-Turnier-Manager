package de.petanqueturniermanager.triptete.rangliste;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import de.petanqueturniermanager.algorithmen.triptete.TripTeteBegegnungErgebnis;
import de.petanqueturniermanager.algorithmen.triptete.TripTetePaarungen;
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
import de.petanqueturniermanager.model.TeamPaarung;
import de.petanqueturniermanager.triptete.meldeliste.TripTeteMeldeListeSheetUpdate;
import de.petanqueturniermanager.triptete.spielplan.TripTeteSpielPlanLeser;
import de.petanqueturniermanager.triptete.spielplan.TripTeteSpielPlanSheet;

/**
 * Schreibt die Daten-Zeilen der Trip-Tête-Rangliste direkt als Werte (ohne Formeln).
 * <p>
 * Pro Team werden geschrieben: Team-Nr, Name, Rang, je Runde 6 Werte
 * (BegGew+, BegVer-, PartGew+, PartVer-, SpPnkt+, SpPnkt-),
 * anschließend 8 Summen-Spalten und 1 Begegnungs-Spalte.
 */
final class TripTeteRanglisteDatenSchreiber {

	private record RundeErgebnis(int begGew, int begVer, int partGew, int partVer,
			int spPnktPlus, int spPnktMinus) {

		static final RundeErgebnis LEER = new RundeErgebnis(0, 0, 0, 0, 0, 0);

		static RundeErgebnis vonBegegnung(TripTeteBegegnungErgebnis b, boolean istTeamA) {
			int gew = istTeamA ? (b.siegA() ? 1 : 0) : (b.siegB() ? 1 : 0);
			int ver = istTeamA ? (b.siegB() ? 1 : 0) : (b.siegA() ? 1 : 0);
			int partGew = istTeamA ? b.begegnungPunkteA() : b.begegnungPunkteB();
			int partVer = istTeamA ? b.begegnungPunkteB() : b.begegnungPunkteA();
			int spPlus = istTeamA ? b.spielpunkteFuerA() : b.spielpunkteGegenA();
			int spMinus = istTeamA ? b.spielpunkteGegenA() : b.spielpunkteFuerA();
			return new RundeErgebnis(gew, ver, partGew, partVer, spPlus, spMinus);
		}
	}

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

		List<List<TeamPaarung>> spielPlanStruktur = TripTetePaarungen.jederGegenJeden(meldungen);
		int anzRunden = spielPlanStruktur.size();
		int anzPaarungenProRunde = spielPlanStruktur.isEmpty() ? 0
				: spielPlanStruktur.get(0).size();

		var spielPlanSheet = new TripTeteSpielPlanSheet(workingSpreadsheet);
		List<List<Optional<TripTeteBegegnungErgebnis>>> alleRunden = TripTeteSpielPlanLeser
				.from(spielPlanSheet).leseAlleRunden(anzPaarungenProRunde, anzRunden);

		var rangliste = new TripTeteRangliste();
		meldungen.teams().forEach(rangliste::addTeam);
		alleRunden.stream().flatMap(Collection::stream)
				.filter(Optional::isPresent).map(Optional::get)
				.forEach(rangliste::addBegegnung);

		Map<Integer, Map<Integer, RundeErgebnis>> rundenLookup = erstelleRundenLookup(alleRunden);
		Map<Integer, String> namen = namenMap();

		RangeData rangeData = new RangeData();
		List<TripTeteTeamErgebnis> sortiert = rangliste.getRangliste();
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

			schreibeRundenDaten(row, teamNr, anzRunden, rundenLookup);
			schreibeSummen(row, ergebnis);
		}

		Position startPos = Position.from(TripTeteRanglisteSheet.TEAM_NR_SPALTE,
				TripTeteRanglisteSheet.ERSTE_DATEN_ZEILE);
		RangeHelper.from(ranglisteSheet, rangeData.getRangePosition(startPos)).setDataInRange(rangeData);
	}

	private void schreibeRundenDaten(RowData row, int teamNr, int anzRunden,
			Map<Integer, Map<Integer, RundeErgebnis>> rundenLookup) {
		Map<Integer, RundeErgebnis> teamRunden = rundenLookup.getOrDefault(teamNr, Map.of());
		for (int r = 0; r < anzRunden; r++) {
			RundeErgebnis re = teamRunden.getOrDefault(r, RundeErgebnis.LEER);
			row.newInt(re.begGew());
			row.newInt(re.begVer());
			row.newInt(re.partGew());
			row.newInt(re.partVer());
			row.newInt(re.spPnktPlus());
			row.newInt(re.spPnktMinus());
		}
	}

	private void schreibeSummen(RowData row, TripTeteTeamErgebnis ergebnis) {
		// Punkte: Begegnungssiege/-verluste
		row.newInt(ergebnis.getBegegnungenGewonnen());
		row.newInt(ergebnis.getBegegnungenVerloren());
		// Spiele: Partiensiege/-verluste + Differenz
		row.newInt(ergebnis.getPartienGewonnen());
		row.newInt(ergebnis.getPartienVerloren());
		row.newInt(ergebnis.getPartienGewonnen() - ergebnis.getPartienVerloren());
		// Spielpunkte: Σ+ / Σ- / Δ
		row.newInt(ergebnis.getSpielPunktePlus());
		row.newInt(ergebnis.getSpielPunkteMinus());
		row.newInt(ergebnis.getSpielPunkteDiff());
		// Begegnungen gesamt
		row.newInt(ergebnis.getBegegnungenGespielt());
	}

	private Map<Integer, Map<Integer, RundeErgebnis>> erstelleRundenLookup(
			List<List<Optional<TripTeteBegegnungErgebnis>>> alleRunden) {
		Map<Integer, Map<Integer, RundeErgebnis>> lookup = new HashMap<>();
		for (int r = 0; r < alleRunden.size(); r++) {
			for (Optional<TripTeteBegegnungErgebnis> optB : alleRunden.get(r)) {
				if (optB.isEmpty()) {
					continue;
				}
				TripTeteBegegnungErgebnis b = optB.get();
				int nrA = b.getTeamA().getNr();
				int nrB = b.getTeamB().getNr();
				lookup.computeIfAbsent(nrA, x -> new HashMap<>()).put(r, RundeErgebnis.vonBegegnung(b, true));
				lookup.computeIfAbsent(nrB, x -> new HashMap<>()).put(r, RundeErgebnis.vonBegegnung(b, false));
			}
		}
		return lookup;
	}

	private Map<Integer, String> namenMap() throws GenerateException {
		return meldeListe.leseTeamNamenMap();
	}
}
