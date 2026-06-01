package de.petanqueturniermanager.triptete.spielplan;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.algorithmen.triptete.TripTeteBegegnungErgebnis;
import de.petanqueturniermanager.algorithmen.triptete.TripTetePartie;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.model.SpielErgebnis;
import de.petanqueturniermanager.model.Team;

/**
 * Liest den TripTête-Spielplan zeilenweise und erzeugt daraus
 * {@link TripTeteBegegnungErgebnis}-Objekte für den Ranglisten-Algorithmus.
 * Zeilen ohne eingetragene Ergebnisse (Spielpunkte-Zellen leer) werden übersprungen.
 * Freilos-Zeilen (Team B = 0) werden übersprungen.
 */
public final class TripTeteSpielPlanLeser {

	private static final int MAX_SCAN_ZEILEN = 500;

	private final ISheet spielplanSheet;
	private final SheetHelper sheetHelper;

	private TripTeteSpielPlanLeser(ISheet spielplanSheet, SheetHelper sheetHelper) {
		this.spielplanSheet = spielplanSheet;
		this.sheetHelper = sheetHelper;
	}

	public static TripTeteSpielPlanLeser from(ISheet spielplanSheet) throws GenerateException {
		return new TripTeteSpielPlanLeser(spielplanSheet, spielplanSheet.getSheetHelper());
	}

	public List<TripTeteBegegnungErgebnis> leseBegegnungen() throws GenerateException {
		List<TripTeteBegegnungErgebnis> ergebnisse = new ArrayList<>();
		XSpreadsheet sheet = spielplanSheet.getXSpreadSheet();
		int letzteZeile = TripTeteSpielPlanSheet.ERSTE_DATEN_ZEILE + MAX_SCAN_ZEILEN;

		for (int zeile = TripTeteSpielPlanSheet.ERSTE_DATEN_ZEILE; zeile <= letzteZeile; zeile++) {
			int teamANr = liesInt(sheet, TripTeteSpielPlanSheet.TEAM_A_NR_SPALTE, zeile);
			if (teamANr < 0) {
				break; // Ende des Spielplans
			}
			int teamBNr = liesInt(sheet, TripTeteSpielPlanSheet.TEAM_B_NR_SPALTE, zeile);
			if (teamBNr <= 0) {
				continue; // Freilos-Zeile überspringen
			}

			int triA = liesInt(sheet, TripTeteSpielPlanSheet.TRI_A_SPALTE, zeile);
			if (triA < 0) {
				continue; // Ergebnis noch nicht eingetragen
			}
			int triB = liesInt(sheet, TripTeteSpielPlanSheet.TRI_B_SPALTE, zeile);
			int douA = liesInt(sheet, TripTeteSpielPlanSheet.DOU_A_SPALTE, zeile);
			int douB = liesInt(sheet, TripTeteSpielPlanSheet.DOU_B_SPALTE, zeile);
			int teteA = liesInt(sheet, TripTeteSpielPlanSheet.TETE_A_SPALTE, zeile);
			int teteB = liesInt(sheet, TripTeteSpielPlanSheet.TETE_B_SPALTE, zeile);

			if (triB < 0 || douA < 0 || douB < 0 || teteA < 0 || teteB < 0) {
				continue; // unvollständig eingetragen
			}

			var begegnung = new TripTeteBegegnungErgebnis(Team.from(teamANr), Team.from(teamBNr))
					.setPartieErgebnis(TripTetePartie.TRIPLETTE, new SpielErgebnis(triA, triB))
					.setPartieErgebnis(TripTetePartie.DOUBLETTE, new SpielErgebnis(douA, douB))
					.setPartieErgebnis(TripTetePartie.TETE, new SpielErgebnis(teteA, teteB));
			ergebnisse.add(begegnung);
		}
		return ergebnisse;
	}

	/**
	 * Liest den Spielplan rundenweise und liefert für jede Runde die Liste der Begegnungen.
	 * Freilos-Paarungen und nicht eingetragene Ergebnisse ergeben {@code Optional.empty()}.
	 *
	 * @param anzPaarungenProRunde Anzahl Begegnungen pro Runde (inkl. Freilos-Paarungen)
	 * @param anzRunden            Gesamtzahl der Runden
	 * @return Liste der Runden, jede Runde als Liste optionaler Begegnungsergebnisse
	 */
	public List<List<Optional<TripTeteBegegnungErgebnis>>> leseAlleRunden(int anzPaarungenProRunde, int anzRunden)
			throws GenerateException {
		List<List<Optional<TripTeteBegegnungErgebnis>>> runden = new ArrayList<>();
		XSpreadsheet sheet = spielplanSheet.getXSpreadSheet();

		for (int r = 0; r < anzRunden; r++) {
			List<Optional<TripTeteBegegnungErgebnis>> runde = new ArrayList<>();
			runden.add(runde);
			int startZeile = TripTeteSpielPlanSheet.ERSTE_DATEN_ZEILE + r * anzPaarungenProRunde;

			for (int p = 0; p < anzPaarungenProRunde; p++) {
				runde.add(leseBegegnung(sheet, startZeile + p));
			}
		}
		return runden;
	}

	private Optional<TripTeteBegegnungErgebnis> leseBegegnung(XSpreadsheet sheet, int zeile) {
		int teamANr = liesInt(sheet, TripTeteSpielPlanSheet.TEAM_A_NR_SPALTE, zeile);
		if (teamANr < 0) {
			return Optional.empty();
		}
		int teamBNr = liesInt(sheet, TripTeteSpielPlanSheet.TEAM_B_NR_SPALTE, zeile);
		if (teamBNr <= 0) {
			return Optional.empty(); // Freilos
		}
		int triA = liesInt(sheet, TripTeteSpielPlanSheet.TRI_A_SPALTE, zeile);
		if (triA < 0) {
			return Optional.empty(); // Ergebnis noch nicht eingetragen
		}
		int triB = liesInt(sheet, TripTeteSpielPlanSheet.TRI_B_SPALTE, zeile);
		int douA = liesInt(sheet, TripTeteSpielPlanSheet.DOU_A_SPALTE, zeile);
		int douB = liesInt(sheet, TripTeteSpielPlanSheet.DOU_B_SPALTE, zeile);
		int teteA = liesInt(sheet, TripTeteSpielPlanSheet.TETE_A_SPALTE, zeile);
		int teteB = liesInt(sheet, TripTeteSpielPlanSheet.TETE_B_SPALTE, zeile);
		if (triB < 0 || douA < 0 || douB < 0 || teteA < 0 || teteB < 0) {
			return Optional.empty();
		}
		return Optional.of(
				new TripTeteBegegnungErgebnis(Team.from(teamANr), Team.from(teamBNr))
						.setPartieErgebnis(TripTetePartie.TRIPLETTE, new SpielErgebnis(triA, triB))
						.setPartieErgebnis(TripTetePartie.DOUBLETTE, new SpielErgebnis(douA, douB))
						.setPartieErgebnis(TripTetePartie.TETE, new SpielErgebnis(teteA, teteB)));
	}

	private int liesInt(XSpreadsheet sheet, int spalte, int zeile) {
		return sheetHelper.getIntFromCell(sheet, Position.from(spalte, zeile));
	}
}
