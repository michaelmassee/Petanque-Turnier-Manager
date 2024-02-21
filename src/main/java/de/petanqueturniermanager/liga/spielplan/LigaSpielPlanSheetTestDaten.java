/**
 * Erstellung 29.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.liga.spielplan;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import de.petanqueturniermanager.algorithmen.JederGegenJeden;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.NewTestDatenValidator;
import de.petanqueturniermanager.liga.meldeliste.LigaMeldeListeSheetTestDaten;
import de.petanqueturniermanager.liga.rangliste.LigaRanglisteSheet;
import de.petanqueturniermanager.model.SpielErgebnis;
import de.petanqueturniermanager.model.TeamMeldungen;

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

		if (!NewTestDatenValidator.from(getWorkingSpreadsheet(), getSheetHelper()).prefix(getLogPrefix()).validate()) {
			return;
		}

		// clean up first
		getSheetHelper().removeAllSheetsExclude(new String[] {});
		getKonfigurationSheet().setGruppenname(TEST_GRUPPE);
		// Meldeliste
		new LigaMeldeListeSheetTestDaten(getWorkingSpreadsheet(), !mitFreispiel).testNamenEinfuegen();
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
		super.spielErgebnisseEinlesen(getTestDaten(jederGegenJeden.anzRunden() * 2, jederGegenJeden.anzPaarungen(), 5));
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
				for (int sp = 0; sp < anzSpielInBegnung; sp++) {
					int welchenTeamHatGewonnen = ThreadLocalRandom.current().nextInt(0, 2); // 0,1
					int verliererPunkte = ThreadLocalRandom.current().nextInt(0, 13); // 0 - 12
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
