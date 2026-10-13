/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.liga.rangliste;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.MeldelisteUuidIndex;
import de.petanqueturniermanager.basesheet.meldeliste.TeamUuidFormel;
import de.petanqueturniermanager.basesheet.meldeliste.TeamnummerAendernSheet;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.random.RandomSource;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.liga.spielplan.LigaSpielPlanSheet;
import de.petanqueturniermanager.liga.spielplan.LigaTurnierTestDaten;

/**
 * Die ausgeblendeten Nr-Spalten des Liga-Spielplans tragen die UUID des Teams und zeigen die aktuelle Nummer. Nach
 * „Teamnummer ändern“ führt die neu aufgebaute Rangliste das Team unter der neuen Nummer mit denselben Ergebnissen.
 */
public class LigaTeamnummerAendernUITest extends BaseCalcUITest {

	private static final int NEUE_NR = 42;
	private static final int MAX_RANGLISTE_ZEILEN = 50;

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
	public void ranglisteFuehrtErgebnisseUnterDerNeuenNummer() throws GenerateException {
		new LigaTurnierTestDaten(wkingSpreadsheet).erzeugeBeispielturnier();
		XSpreadsheet spielplan = new LigaSpielPlanSheet(wkingSpreadsheet).getXSpreadSheet();
		Position nrA = Position.from(LigaSpielPlanSheet.TEAM_A_NR_SPALTE,
				LigaSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE);
		String uuidA = TeamUuidFormel.findeUuid(sheetHlp.getFormulaFromCell(spielplan, nrA)).orElseThrow();
		int alteNr = MeldelisteUuidIndex.lese(wkingSpreadsheet).nr(uuidA).orElseThrow();
		Position nameA = Position.from(LigaSpielPlanSheet.NAME_A_SPALTE,
				LigaSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE);
		String nameVorher = sheetHlp.getTextFromCell(spielplan, nameA);
		assertThat(sheetHlp.getFormulaFromCell(spielplan, nameA)).containsIgnoringCase("PTMUUIDTEAMANZEIGE")
				.contains(uuidA).doesNotContain("MATCH(", "VLOOKUP");
		List<String> ranglisteVorher = ranglisteJeNr().get(alteNr);

		// Das Property schreibt sonst erst SheetRunner.run(); der Test ruft doRun() direkt.
		docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM, TurnierSystem.LIGA.getId());
		new TeamnummerAendernSheet(wkingSpreadsheet).mitEingabe(alteNr, NEUE_NR).doRun();
		recalcAll();

		assertThat(sheetHlp.getTextFromCell(spielplan, nrA)).as("Nr-Spalte zeigt die neue Nummer")
				.isEqualTo(String.valueOf(NEUE_NR));
		assertThat(sheetHlp.getTextFromCell(spielplan, nameA)).isEqualTo(nameVorher);

		// Ranglisten führen Nummern als Werte und zeigen die neue Nummer erst nach dem Aktualisieren.
		new LigaRanglisteSheet(wkingSpreadsheet).upDateSheet();
		Map<Integer, List<String>> ranglisteNachher = ranglisteJeNr();
		assertThat(ranglisteNachher).as("Die alte Nummer gibt es nicht mehr").doesNotContainKey(alteNr);
		assertThat(ranglisteNachher.get(NEUE_NR)).as("Name, Platz und Ergebnisse unter der neuen Nummer")
				.isEqualTo(ranglisteVorher);
	}

	/** Ranglistenzeile ohne Nr (Name bis Begegnungen) je Teamnummer. */
	private Map<Integer, List<String>> ranglisteJeNr() throws GenerateException {
		LigaRanglisteSheet rangliste = new LigaRanglisteSheet(wkingSpreadsheet);
		RangePosition bereich = RangePosition.from(LigaRanglisteSheet.TEAM_NR_SPALTE,
				LigaRanglisteSheet.ERSTE_DATEN_ZEILE, rangliste.getManuellSortSpalte() - 1,
				LigaRanglisteSheet.ERSTE_DATEN_ZEILE + MAX_RANGLISTE_ZEILEN);
		RangeData daten = RangeHelper.from(rangliste.getXSpreadSheet(),
				wkingSpreadsheet.getWorkingSpreadsheetDocument(), bereich).getDataFromRange();
		Map<Integer, List<String>> jeNr = new HashMap<>();
		for (RowData zeile : daten) {
			int nr = zeile.isEmpty() ? 0 : zeile.get(0).getIntVal(0);
			if (nr > 0) {
				List<String> rest = new ArrayList<>();
				for (int i = 1; i < zeile.size(); i++) {
					rest.add(zeile.get(i).getStringVal());
				}
				jeNr.put(nr, rest);
			}
		}
		return jeNr;
	}
}
