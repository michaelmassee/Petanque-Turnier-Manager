/*
 * Erstellung 29.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.liga.spielplan;

import de.petanqueturniermanager.helper.random.RandomSource;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import de.petanqueturniermanager.algorithmen.liga.JederGegenJeden;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.NewTestDatenValidator;
import de.petanqueturniermanager.liga.meldeliste.LigaMeldeListeSheetTestDaten;
import de.petanqueturniermanager.liga.rangliste.LigaRanglisteSheet;
import de.petanqueturniermanager.model.SpielErgebnis;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;

/**
 * @author Michael Massee
 *
 */
public class LigaSpielPlanSheetTestDaten extends LigaSpielPlanSheet {

	public static final String TEST_GRUPPE = "Test Gruppe";
	private final boolean mitFreispiel;

	/**
	 * @param workingSpreadsheet
	 */
	public LigaSpielPlanSheetTestDaten(WorkingSpreadsheet workingSpreadsheet, boolean mitFreispiel) {
		super(workingSpreadsheet);
		this.mitFreispiel = mitFreispiel;
	}

	@Override
	protected void doRun() throws GenerateException {

		if (!NewTestDatenValidator.from(getWorkingSpreadsheet(), getSheetHelper(), TurnierSystem.LIGA)
				.prefix(getLogPrefix()).validate()) {
			return;
		}

		// clean up first
		getSheetHelper().removeAllSheetsExclude(new String[] {});
		getKonfigurationSheet().setGruppenname(TEST_GRUPPE);
		// Meldeliste
		new LigaMeldeListeSheetTestDaten(getWorkingSpreadsheet(), !mitFreispiel)
				.erstelleUndFuelleTestDaten(TEST_GRUPPE);
		generate();

		// Rangliste
		new LigaRanglisteSheet(getWorkingSpreadsheet()).upDateSheet();
	}

	/**
	 * testdaten generieren
	 *
	 * @throws GenerateException
	 */
	public void generate() throws GenerateException {
		TeamMeldungen alleMeldungen = getMeldeListe().getAlleMeldungen();
		super.generate(alleMeldungen);

		JederGegenJeden jederGegenJeden = new JederGegenJeden(alleMeldungen);
		// anzahl runden x 2, weil hin und rückrunde
		int anzRunden = jederGegenJeden.anzRunden() * 2;
		int anzPaarungen = jederGegenJeden.anzPaarungen();
		testTermineEintragen(anzRunden, anzPaarungen);
		super.spielErgebnisseEinlesen(getTestDaten(anzRunden, anzPaarungen, 5));
	}

	private void testTermineEintragen(int anzRunden, int anzPaarungen) throws GenerateException {
		LocalDate startDatum = LocalDate.of(2026, 6, 20);
		LocalTime startZeit = LocalTime.of(10, 0);
		for (int runde = 0; runde < anzRunden; runde++) {
			double datum = calcDatum(startDatum.plusWeeks(runde));
			double uhrzeit = calcUhrzeit(startZeit.plusMinutes((long) runde * 30));
			for (int paarung = 0; paarung < anzPaarungen; paarung++) {
				int zeile = ERSTE_SPIELTAG_DATEN_ZEILE + (runde * anzPaarungen) + paarung;
				if (istFreispielZeile(zeile)) {
					continue;
				}
				getSheetHelper().setNumberValueInCell(
						NumberCellValue.from(getXSpreadSheet(), DATUM_SPALTE, zeile, datum));
				getSheetHelper().setNumberValueInCell(
						NumberCellValue.from(getXSpreadSheet(), UHRZEIT_SPALTE, zeile, uhrzeit));
			}
		}
	}

	private boolean istFreispielZeile(int zeile) throws GenerateException {
		try {
			int teamA = (int) getXSpreadSheet().getCellByPosition(TEAM_A_NR_SPALTE, zeile).getValue();
			int teamB = (int) getXSpreadSheet().getCellByPosition(TEAM_B_NR_SPALTE, zeile).getValue();
			return istFreispiel(teamA, teamB);
		} catch (Exception e) {
			throw new GenerateException(e.getMessage());
		}
	}

	private static double calcDatum(LocalDate datum) {
		return datum.toEpochDay() - LocalDate.of(1899, 12, 30).toEpochDay();
	}

	private static double calcUhrzeit(LocalTime zeit) {
		return zeit.toSecondOfDay() / 86400.0;
	}

	private List<List<List<SpielErgebnis>>> getTestDaten(int anzRunden, int anzPaarungen, int anzSpielInBegnung) {

		// ein begegnung sind:
		// Runde1 = 1x Tete + 2 x Doublette
		// Runde2 = 1 x Doublette + 1 x Triplette

		ArrayList<List<List<SpielErgebnis>>> result = new ArrayList<>();

		for (int runde = 0; runde < anzRunden; runde++) {
			ArrayList<List<SpielErgebnis>> paarungen = new ArrayList<>();
			for (int i = 0; i < anzPaarungen; i++) {
				ArrayList<SpielErgebnis> ergebnisse = new ArrayList<>();
				boolean freispiel = mitFreispiel && i == 0;
				for (int sp = 0; sp < anzSpielInBegnung; sp++) {
					int welchenTeamHatGewonnen = RandomSource.nextInt(0, 2); // 0,1
					int verliererPunkte = RandomSource.nextInt(0, 13); // 0 - 12
					if (freispiel) {
						continue;
					}
					SpielErgebnis ergebnis;
					if (welchenTeamHatGewonnen == 0) {
						ergebnis = new SpielErgebnis(13, verliererPunkte);
					} else {
						ergebnis = new SpielErgebnis(verliererPunkte, 13);
					}
					ergebnisse.add(ergebnis);
				}
				paarungen.add(ergebnisse);
			}
			result.add(paarungen);
		}

		return result;
	}

}
