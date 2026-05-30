package de.petanqueturniermanager.triptete.rangliste;

import java.util.List;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.algorithmen.triptete.TripTetePaarungen;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.model.TeamPaarung;
import de.petanqueturniermanager.triptete.meldeliste.TripTeteMeldeListeSheetUpdate;
import de.petanqueturniermanager.triptete.spielplan.TripTeteSpielPlanSheet;

/**
 * Schreibt die Daten-Zeilen der Trip-Tête-Rangliste pro Team:
 * Team-Nr, Name (per VLOOKUP), Rang (RANK-Formel), Begegnungs-Siege (SUMIF
 * Spielplan), Σ Partien-Siege (SUMIF Spielplan).
 */
final class TripTeteRanglisteDatenSchreiber {

	private final ISheet ranglisteSheet;
	private final TripTeteMeldeListeSheetUpdate meldeListe;

	private TripTeteRanglisteDatenSchreiber(ISheet ranglisteSheet, TripTeteMeldeListeSheetUpdate meldeListe) {
		this.ranglisteSheet = ranglisteSheet;
		this.meldeListe = meldeListe;
	}

	static TripTeteRanglisteDatenSchreiber from(ISheet ranglisteSheet, TripTeteMeldeListeSheetUpdate meldeListe) {
		return new TripTeteRanglisteDatenSchreiber(ranglisteSheet, meldeListe);
	}

	void schreibeDaten() throws GenerateException {
		TeamMeldungen meldungen = meldeListe.getAlleMeldungen();
		XSpreadsheet sheet = ranglisteSheet.getXSpreadSheet();

		int zeile = TripTeteRanglisteSheet.ERSTE_DATEN_ZEILE;
		int erstePunkteZeile = zeile;
		int letztePunkteZeile = zeile + meldungen.size() - 1;
		String punkteRange = adresse(TripTeteRanglisteSheet.BEG_SIEGE_SPALTE, erstePunkteZeile)
				+ ":" + adresse(TripTeteRanglisteSheet.BEG_SIEGE_SPALTE, letztePunkteZeile);

		// Spielplan-Bereich strikt begrenzen: SUMPRODUCT/VLOOKUP auf $X:$X-Vollspalten
		// rechnet pro Formel-Zelle über alle 1 048 576 Zeilen — bei N Teams werden
		// daraus N × 1 Mio VLOOKUPs, und LO friert beim Recalc ein.
		List<List<TeamPaarung>> spielPlan = TripTetePaarungen.jederGegenJeden(meldungen);
		int anzSpielzeilen = spielPlan.isEmpty() ? 0 : spielPlan.size() * spielPlan.get(0).size();
		int letzteSpielplanZeile = TripTeteSpielPlanSheet.ERSTE_DATEN_ZEILE + Math.max(anzSpielzeilen, 1) - 1;

		for (Team team : meldungen.teams()) {
			int teamNr = team.getNr();

			// Spalte 0: Team-Nr (Wert)
			ranglisteSheet.getSheetHelper().setNumberValueInCell(
					NumberCellValue.from(sheet, TripTeteRanglisteSheet.TEAM_NR_SPALTE, zeile, teamNr));

			// Spalte 1: Name via Sverweis auf Meldeliste
			String nameAdrInRangliste = adresse(TripTeteRanglisteSheet.TEAM_NR_SPALTE, zeile);
			String namenFormel = meldeListe.formulaSverweisSpielernamen(nameAdrInRangliste);
			ranglisteSheet.getSheetHelper().setFormulaInCell(
					StringCellValue.from(sheet, Position.from(TripTeteRanglisteSheet.NAME_SPALTE, zeile))
							.setValue(namenFormel));

			// Spalte 2: Rang per RANK
			String rangFormel = "RANK(" + adresse(TripTeteRanglisteSheet.BEG_SIEGE_SPALTE, zeile)
					+ ";" + punkteRange + ";0)";
			ranglisteSheet.getSheetHelper().setFormulaInCell(
					StringCellValue.from(sheet, Position.from(TripTeteRanglisteSheet.RANG_SPALTE, zeile))
							.setValue(rangFormel));

			// Spalte 3: Σ Begegnungs-Punkte aus Spielplan
			ranglisteSheet.getSheetHelper().setFormulaInCell(
					StringCellValue.from(sheet, Position.from(TripTeteRanglisteSheet.BEG_SIEGE_SPALTE, zeile))
							.setValue(sumIfFormel(teamNr,
									TripTeteSpielPlanSheet.BEG_PUNKT_A,
									TripTeteSpielPlanSheet.BEG_PUNKT_B,
									letzteSpielplanZeile)));

			// Spalte 4: Σ Partien-Siege aus Spielplan
			ranglisteSheet.getSheetHelper().setFormulaInCell(
					StringCellValue.from(sheet, Position.from(TripTeteRanglisteSheet.PAR_SIEGE_SPALTE, zeile))
							.setValue(sumIfFormel(teamNr,
									TripTeteSpielPlanSheet.PARTIE_SIEGE_A,
									TripTeteSpielPlanSheet.PARTIE_SIEGE_B,
									letzteSpielplanZeile)));

			zeile++;
		}
	}

	/**
	 * SUMIF auf eine Spalte (TeamA oder TeamB) im Spielplan, Wertspalte daneben.
	 * Konkret: für Begegnungs-Siege wird in der TeamA-Spalte des Spielplans nach
	 * teamNr gesucht und die BEG_PUNKT_A-Spalte aufsummiert, plus analoge Suche
	 * in TeamB-Spalte.
	 */
	private static String sumIfFormel(int teamNr, int spaltenA, int spaltenB, int letzteSpielplanZeile) {
		String spielplan = "$'" + SheetNamen.spielplan() + "'.";
		String teamASpalte = spielplan + spielplanRange(TripTeteSpielPlanSheet.TEAM_A_NR_SPALTE, letzteSpielplanZeile);
		String teamBSpalte = spielplan + spielplanRange(TripTeteSpielPlanSheet.TEAM_B_NR_SPALTE, letzteSpielplanZeile);
		String wertSpalteA = spielplan + spielplanRange(spaltenA, letzteSpielplanZeile);
		String wertSpalteB = spielplan + spielplanRange(spaltenB, letzteSpielplanZeile);
		return "SUMIF(" + teamASpalte + ";" + teamNr + ";" + wertSpalteA + ")"
				+ "+SUMIF(" + teamBSpalte + ";" + teamNr + ";" + wertSpalteB + ")";
	}

	private static String adresse(int spalte, int zeile) {
		return "$" + spalteBuchstabe(spalte) + "$" + (zeile + 1);
	}

	private static String spielplanRange(int spalte, int letzteSpielplanZeile) {
		String buchstabe = spalteBuchstabe(spalte);
		int ersteZeileEinsBasiert = TripTeteSpielPlanSheet.ERSTE_DATEN_ZEILE + 1;
		int letzteZeileEinsBasiert = letzteSpielplanZeile + 1;
		return "$" + buchstabe + "$" + ersteZeileEinsBasiert + ":$" + buchstabe + "$" + letzteZeileEinsBasiert;
	}

	private static String spalteBuchstabe(int spalte) {
		StringBuilder sb = new StringBuilder();
		int s = spalte;
		while (s >= 0) {
			sb.insert(0, (char) ('A' + (s % 26)));
			s = s / 26 - 1;
		}
		return sb.toString();
	}
}
