/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.triptete;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.algorithmen.triptete.TripTeteBegegnungErgebnis;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.MeldelisteUuidIndex;
import de.petanqueturniermanager.basesheet.meldeliste.TeamUuidFormel;
import de.petanqueturniermanager.basesheet.meldeliste.TeamnummerAendernSheet;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.random.RandomSource;
import de.petanqueturniermanager.triptete.spielplan.TripTeteSpielPlanLeser;
import de.petanqueturniermanager.triptete.spielplan.TripTeteSpielPlanSheet;
import de.petanqueturniermanager.triptete.spielplan.TripTeteSpielPlanSheetTestDaten;

/**
 * Die ausgeblendeten Nr-Spalten des Trip-Tête-Spielplans tragen die UUID des Teams und zeigen die aktuelle Nummer.
 * Nach „Teamnummer ändern“ ordnet der Spielplan-Leser die Begegnung weiter demselben Team zu, die Namen bleiben.
 */
public class TripTeteTeamnummerAendernUITest extends BaseCalcUITest {

	private static final int NEUE_NR = 42;

	@BeforeEach
	@Override
	public void beforeTest() {
		super.beforeTest();
		RandomSource.setSeed(42L);
	}

	@AfterEach
	public void resetRandom() {
		RandomSource.reset();
	}

	@Test
	public void begegnungBleibtNachNummernwechselBeimTeam() throws GenerateException {
		var spielplan = new TripTeteSpielPlanSheetTestDaten(wkingSpreadsheet);
		spielplan.generate();
		XSpreadsheet sheet = spielplan.getXSpreadSheet();
		int zeile = TripTeteSpielPlanSheet.ERSTE_DATEN_ZEILE;
		Position nrA = Position.from(TripTeteSpielPlanSheet.TEAM_A_NR_SPALTE, zeile);
		Position nameA = Position.from(TripTeteSpielPlanSheet.NAME_A_SPALTE, zeile);
		String uuidA = TeamUuidFormel.findeUuid(sheetHlp.getFormulaFromCell(sheet, nrA)).orElseThrow();
		int alteNr = MeldelisteUuidIndex.lese(wkingSpreadsheet).nr(uuidA).orElseThrow();
		String nameVorher = sheetHlp.getTextFromCell(sheet, nameA);
		assertThat(sheetHlp.getFormulaFromCell(sheet, nameA)).containsIgnoringCase("PTMUUIDTEAMANZEIGE")
				.contains(uuidA).doesNotContain("MATCH(", "VLOOKUP", "IFNA");
		TripTeteBegegnungErgebnis vorher = begegnungVon(spielplan, alteNr);

		// Das Property schreibt sonst erst SheetRunner.run(); der Test ruft doRun() direkt.
		docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM,
				TurnierSystem.TRIPTETE.getId());
		new TeamnummerAendernSheet(wkingSpreadsheet).mitEingabe(alteNr, NEUE_NR).doRun();
		recalcAll();

		assertThat(sheetHlp.getTextFromCell(sheet, nrA)).as("Nr-Spalte zeigt die neue Nummer")
				.isEqualTo(String.valueOf(NEUE_NR));
		assertThat(sheetHlp.getTextFromCell(sheet, nameA)).as("Der Name bleibt").isEqualTo(nameVorher);
		TripTeteBegegnungErgebnis nachher = begegnungVon(spielplan, NEUE_NR);
		assertThat(nachher.begegnungPunkteA()).as("Dieselbe Begegnung, jetzt unter der neuen Nummer")
				.isEqualTo(vorher.begegnungPunkteA());
		assertThat(nachher.getTeamB().getNr()).isEqualTo(vorher.getTeamB().getNr());
	}

	@Test
	public void fehlendesUuidTeamIstKeinFreispiel() throws Exception {
		var spielplan = new TripTeteSpielPlanSheetTestDaten(wkingSpreadsheet);
		spielplan.generate();
		XSpreadsheet sheet = spielplan.getXSpreadSheet();
		int zeile = TripTeteSpielPlanSheet.ERSTE_DATEN_ZEILE;
		Position nameA = Position.from(TripTeteSpielPlanSheet.NAME_A_SPALTE, zeile);
		String uuid = TeamUuidFormel.findeUuid(sheetHlp.getFormulaFromCell(sheet, nameA)).orElseThrow();
		XSpreadsheet meldeliste = spielplan.getMeldeListe().getXSpreadSheet();
		boolean ersetzt = false;
		for (int row = 0; row < 20; row++) {
			var cell = meldeliste.getCellByPosition(0, row);
			String formel = cell.getFormula();
			if (formel.contains(uuid)) {
				// Eine neue Meldung mit gleicher Nummer darf die alte Teamidentität nicht übernehmen.
				cell.setFormula(formel.replace(uuid, "ffffffff-0000-4000-8000-000000000001"));
				ersetzt = true;
				break;
			}
		}
		assertThat(ersetzt).isTrue();
		recalcAll();
		assertThat(sheet.getCellByPosition(TripTeteSpielPlanSheet.NAME_A_SPALTE, zeile).getError()).isNotZero();
		assertThat(sheetHlp.getTextFromCell(sheet, nameA))
				.isNotEqualTo(de.petanqueturniermanager.helper.i18n.I18n.get("spielplan.freispiel.name"));
	}

	private static TripTeteBegegnungErgebnis begegnungVon(TripTeteSpielPlanSheet spielplan, int teamNr)
			throws GenerateException {
		return TripTeteSpielPlanLeser.from(spielplan).leseBegegnungen().stream()
				.filter(b -> b.getTeamA().getNr() == teamNr).findFirst()
				.orElseThrow(() -> new AssertionError("Keine Begegnung mit Team A = " + teamNr));
	}
}
