package de.petanqueturniermanager.jedergegenjeden;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.jedergegenjeden.konfiguration.JGJKonfigurationSheet;
import de.petanqueturniermanager.jedergegenjeden.meldeliste.JGJMeldeListeSheet_New;
import de.petanqueturniermanager.jedergegenjeden.meldeliste.JGJMeldeListeSheet_Update;
import de.petanqueturniermanager.jedergegenjeden.spielplan.JGJSpielPlanSheet;
import de.petanqueturniermanager.schweizer.konfiguration.SpielplanTeamAnzeige;

/**
 * UITest für die Setzpositions-Logik der JGJ-Gruppenaufteilung.
 * <p>
 * Erzeugt eine Meldeliste mit 9 Teams und Gruppengröße 3, setzt für drei Teams dieselbe Setzposition
 * und überprüft am generierten Spielplan, dass diese Teams in unterschiedlichen Gruppen gelandet sind
 * (also nie in einer Paarung zusammenkommen).
 */
public class JGJSetzPositionSpielPlanUITest extends BaseCalcUITest {

	private static final Logger logger = LogManager.getLogger(JGJSetzPositionSpielPlanUITest.class);

	private static final int ANZAHL_TEAMS = 9;
	private static final int GRUPPEN_GROESSE = 3;
	private static final int SETZ_POSITION = 1;
	private static final int[] TEAMS_MIT_SETZ_POS = { 1, 2, 3 };

	// Spaltenindizes für Formation TETE ohne Teamname/Vereinsname
	// (vgl. JGJMeldeListeDelegate#getSetzPositionSpalte / #getAktivSpalte)
	private static final int SETZ_POSITION_SPALTE = 3;
	private static final int AKTIV_SPALTE = 4;
	private static final int AKTIV_WERT_NIMMT_TEIL = 1;

	@Test
	public void testGleicheSetzPositionLandetInVerschiedenenGruppen() throws GenerateException {
		logger.info("testGleicheSetzPositionLandetInVerschiedenenGruppen");

		meldeListeMitSetzPositionenAnlegen();

		JGJSpielPlanSheet spielPlan = new JGJSpielPlanSheet(wkingSpreadsheet);
		spielPlan.run();

		Map<Integer, Set<Integer>> mitspielerProTeam = mitspielerAusSpielplanLesen(spielPlan);

		// Teams mit gleicher Setzposition dürfen nie miteinander gepaart sein,
		// d. h. sie tauchen niemals in der Mitspieler-Liste des jeweils anderen auf.
		for (int teamA : TEAMS_MIT_SETZ_POS) {
			for (int teamB : TEAMS_MIT_SETZ_POS) {
				if (teamA == teamB) {
					continue;
				}
				assertThat(mitspielerProTeam.getOrDefault(teamA, Set.of()))
						.as("Team %d hat Setzpos %d und darf nicht gegen Team %d (selbe Setzpos) spielen",
								teamA, SETZ_POSITION, teamB)
						.doesNotContain(teamB);
			}
		}

		// Sanity-Check: Es muss überhaupt Paarungen geben, damit die Constraint-Prüfung aussagekräftig ist.
		assertThat(mitspielerProTeam).as("Mindestens ein Team hat Mitspieler im Spielplan").isNotEmpty();
	}

	// ─── Setup ───────────────────────────────────────────────────────────

	private void meldeListeMitSetzPositionenAnlegen() throws GenerateException {
		JGJMeldeListeSheet_New meldeListeNew = new JGJMeldeListeSheet_New(wkingSpreadsheet);
		meldeListeNew.createMeldelisteWithParams(Formation.TETE, false, false, SpielplanTeamAnzeige.NR,
				GRUPPEN_GROESSE);

		JGJKonfigurationSheet konfig = new JGJKonfigurationSheet(wkingSpreadsheet);
		assertThat(konfig.getGruppengroesse()).isEqualTo(GRUPPEN_GROESSE);

		int ersteDatenZeile = meldeListeNew.getMeldungenSpalte().getErsteDatenZiele();
		int nrSpalte = meldeListeNew.getMeldungenSpalte().getSpielerNrSpalte();
		XSpreadsheet sheet = meldeListeNew.getXSpreadSheet();

		// Nr + Vorname pro Team eintragen
		RangeData stammdaten = new RangeData();
		for (int teamNr = 1; teamNr <= ANZAHL_TEAMS; teamNr++) {
			RowData zeile = stammdaten.addNewRow();
			zeile.newInt(teamNr);
			zeile.newString("Spieler" + teamNr);
		}
		Position startStamm = Position.from(nrSpalte, ersteDatenZeile);
		RangeHelper.from(sheet, doc, stammdaten.getRangePosition(startStamm)).setDataInRange(stammdaten);

		// Setzposition + Aktiv-Flag pro Zeile schreiben (alles in einem Block)
		RangeData kontrollSpalten = new RangeData();
		Set<Integer> setzPosTeams = new HashSet<>();
		for (int t : TEAMS_MIT_SETZ_POS) {
			setzPosTeams.add(t);
		}
		for (int teamNr = 1; teamNr <= ANZAHL_TEAMS; teamNr++) {
			RowData zeile = kontrollSpalten.addNewRow();
			zeile.newInt(setzPosTeams.contains(teamNr) ? SETZ_POSITION : 0);
			zeile.newInt(AKTIV_WERT_NIMMT_TEIL);
		}
		Position startKontroll = Position.from(SETZ_POSITION_SPALTE, ersteDatenZeile);
		RangeHelper.from(sheet, doc, kontrollSpalten.getRangePosition(startKontroll))
				.setDataInRange(kontrollSpalten);

		assertThat(SETZ_POSITION_SPALTE).as("Erwartete Setzpos-Spalte für TETE").isEqualTo(3);
		assertThat(AKTIV_SPALTE).as("Erwartete Aktiv-Spalte für TETE").isEqualTo(4);

		JGJMeldeListeSheet_Update meldeListeUpdate = new JGJMeldeListeSheet_Update(wkingSpreadsheet);
		meldeListeUpdate.run();
	}

	// ─── Auswertung Spielplan ────────────────────────────────────────────

	/**
	 * Liest aus dem Spielplan alle Paarungen (TEAM_A_NR/TEAM_B_NR) und liefert pro Team
	 * die Menge aller Mitspieler-Nummern.
	 */
	private Map<Integer, Set<Integer>> mitspielerAusSpielplanLesen(JGJSpielPlanSheet spielPlan)
			throws GenerateException {
		// Großzügig 200 Zeilen lesen (deckt 9 Teams in 3 Gruppen mit Gruppen-Headern ab).
		int letzteZeile = JGJSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE + 200;
		RangeData paarungen = RangeHelper.from(spielPlan.getXSpreadSheet(),
				wkingSpreadsheet.getWorkingSpreadsheetDocument(),
				RangePosition.from(JGJSpielPlanSheet.TEAM_A_NR_SPALTE,
						JGJSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE,
						JGJSpielPlanSheet.TEAM_B_NR_SPALTE, letzteZeile))
				.getDataFromRange();

		Map<Integer, Set<Integer>> mitspieler = new HashMap<>();
		List<Integer> alleTeamNrs = new ArrayList<>();
		for (int i = 0; i < paarungen.size(); i++) {
			RowData zeile = paarungen.get(i);
			int nrA = zeile.get(0).getIntVal(0);
			int nrB = zeile.get(1).getIntVal(0);
			if (nrA <= 0 || nrB <= 0) {
				continue;
			}
			alleTeamNrs.add(nrA);
			alleTeamNrs.add(nrB);
			mitspieler.computeIfAbsent(nrA, key -> new HashSet<>()).add(nrB);
			mitspieler.computeIfAbsent(nrB, key -> new HashSet<>()).add(nrA);
		}

		// Zur Sicherheit: alle 9 Teams müssen mindestens einmal im Spielplan auftauchen.
		Set<Integer> gefunden = new HashSet<>(alleTeamNrs);
		for (int teamNr = 1; teamNr <= ANZAHL_TEAMS; teamNr++) {
			assertThat(gefunden)
					.as("Team %d muss im Spielplan vorkommen", teamNr)
					.contains(teamNr);
		}
		return mitspieler;
	}
}
