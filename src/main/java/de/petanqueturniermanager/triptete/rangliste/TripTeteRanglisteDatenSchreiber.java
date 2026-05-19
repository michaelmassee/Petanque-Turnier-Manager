package de.petanqueturniermanager.triptete.rangliste;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.triptete.meldeliste.TripTeteMeldeListeSheetUpdate;
import de.petanqueturniermanager.triptete.spielplan.TripTeteSpielPlanSheet;

/**
 * Schreibt die Daten-Zeilen der Trip-Tête-Rangliste pro Team:
 * Team-Nr, Name (per VLOOKUP), Rang (RANG-Formel), Begegnungs-Siege (SUMIF
 * Spielplan), Σ Partien (SUMIF Spielplan), Buchholz (Summe der Begegnungs-
 * Siege der Gegner).
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
									TripTeteSpielPlanSheet.BEG_PUNKT_B)));

			// Spalte 4: Σ Partien-Siege aus Spielplan
			ranglisteSheet.getSheetHelper().setFormulaInCell(
					StringCellValue.from(sheet, Position.from(TripTeteRanglisteSheet.PAR_SIEGE_SPALTE, zeile))
							.setValue(sumIfFormel(teamNr,
									TripTeteSpielPlanSheet.PARTIE_SIEGE_A,
									TripTeteSpielPlanSheet.PARTIE_SIEGE_B)));

			// Spalte 5: Buchholz – Summe der Begegnungs-Siege der gespielten Gegner.
			// Vereinfachte Variante: Σ über alle Begegnungen, in denen das Team
			// vorkam: für jeden gespielten Gegner dessen aktuelle Begegnungs-Siege.
			// Implementiert als SUMPRODUCT.
			ranglisteSheet.getSheetHelper().setFormulaInCell(
					StringCellValue.from(sheet, Position.from(TripTeteRanglisteSheet.BUCHHOLZ_SPALTE, zeile))
							.setValue(buchholzFormel(teamNr, erstePunkteZeile, letztePunkteZeile)));
			zeile++;
		}
	}

	/**
	 * SUMIF auf eine Spalte (TeamA oder TeamB) im Spielplan, Wertspalte daneben.
	 * Konkret: für Begegnungs-Siege wird in der TeamA-Spalte des Spielplans nach
	 * teamNr gesucht und die BEG_PUNKT_A-Spalte aufsummiert, plus analoge Suche
	 * in TeamB-Spalte.
	 */
	private static String sumIfFormel(int teamNr, int spaltenA, int spaltenB) {
		String spielplan = "$'" + SheetNamen.spielplan() + "'.";
		String teamASpalte = spielplan + spalteAlsRange(TripTeteSpielPlanSheet.TEAM_A_NR_SPALTE);
		String teamBSpalte = spielplan + spalteAlsRange(TripTeteSpielPlanSheet.TEAM_B_NR_SPALTE);
		String wertSpalteA = spielplan + spalteAlsRange(spaltenA);
		String wertSpalteB = spielplan + spalteAlsRange(spaltenB);
		return "SUMIF(" + teamASpalte + ";" + teamNr + ";" + wertSpalteA + ")"
				+ "+SUMIF(" + teamBSpalte + ";" + teamNr + ";" + wertSpalteB + ")";
	}

	/**
	 * Buchholz: Σ der Begegnungs-Siege aller Gegner, die in Begegnungen mit
	 * diesem Team vorkamen. Per SUMPRODUCT über die TeamA-/TeamB-Spalten des
	 * Spielplans und VLOOKUP auf die Rangliste.
	 *
	 * <p>Vereinfachung: wir nutzen SUMPRODUCT mit Indikator-Funktion über die
	 * Spielplan-Reihen.
	 */
	private static String buchholzFormel(int teamNr, int erstePunkteZeile, int letztePunkteZeile) {
		String spielplan = "$'" + SheetNamen.spielplan() + "'.";
		String teamA = spielplan + spalteAlsRange(TripTeteSpielPlanSheet.TEAM_A_NR_SPALTE);
		String teamB = spielplan + spalteAlsRange(TripTeteSpielPlanSheet.TEAM_B_NR_SPALTE);
		// für jede Spielplan-Zeile:
		//   wenn teamA == teamNr → Gegner = teamB, VLOOKUP dessen Begegnungs-Siege
		//   wenn teamB == teamNr → Gegner = teamA, VLOOKUP
		// Summiert über alle Zeilen.
		String rangNrRange = adresse(TripTeteRanglisteSheet.TEAM_NR_SPALTE, erstePunkteZeile)
				+ ":" + adresse(TripTeteRanglisteSheet.BUCHHOLZ_SPALTE, letztePunkteZeile);
		int vlookupSpaltenIndex = TripTeteRanglisteSheet.BEG_SIEGE_SPALTE - TripTeteRanglisteSheet.TEAM_NR_SPALTE + 1;
		return "SUMPRODUCT((" + teamA + "=" + teamNr + ")*IFERROR(VLOOKUP(" + teamB + ";" + rangNrRange + ";"
				+ vlookupSpaltenIndex + ";0);0))"
				+ "+SUMPRODUCT((" + teamB + "=" + teamNr + ")*IFERROR(VLOOKUP(" + teamA + ";" + rangNrRange + ";"
				+ vlookupSpaltenIndex + ";0);0))";
	}

	private static String adresse(int spalte, int zeile) {
		return "$" + spalteBuchstabe(spalte) + "$" + (zeile + 1);
	}

	private static String spalteAlsRange(int spalte) {
		String buchstabe = spalteBuchstabe(spalte);
		return "$" + buchstabe + ":$" + buchstabe;
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
