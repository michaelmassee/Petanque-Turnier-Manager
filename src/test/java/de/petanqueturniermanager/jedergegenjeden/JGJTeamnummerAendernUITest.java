/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.jedergegenjeden;

import static org.assertj.core.api.Assertions.assertThat;

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
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.random.RandomSource;
import de.petanqueturniermanager.jedergegenjeden.meldeliste.JGJMeldeListeSheet_Update;
import de.petanqueturniermanager.jedergegenjeden.rangliste.JGJRanglisteRechner;
import de.petanqueturniermanager.jedergegenjeden.spielplan.JGJSpielPlanSheet;
import de.petanqueturniermanager.jedergegenjeden.spielplan.JGJTurnierTestDaten;

/**
 * Die ausgeblendeten Nr-Spalten des JGJ-Spielplans tragen die UUID des Teams und zeigen die aktuelle Nummer. Nach
 * „Teamnummer ändern“ rechnet die Rangliste dem Team unter der neuen Nummer dieselben Ergebnisse zu.
 */
public class JGJTeamnummerAendernUITest extends BaseCalcUITest {

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
	public void ranglisteRechnetErgebnisseDerNeuenNummerZu() throws GenerateException {
		new JGJTurnierTestDaten(wkingSpreadsheet).generate();
		XSpreadsheet spielplan = sheetHlp.findByName(SheetNamen.spielplan());
		Position nrA = Position.from(JGJSpielPlanSheet.TEAM_A_NR_SPALTE, JGJSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE);
		String uuidA = TeamUuidFormel.findeUuid(sheetHlp.getFormulaFromCell(spielplan, nrA)).orElseThrow();
		int alteNr = MeldelisteUuidIndex.lese(wkingSpreadsheet).nr(uuidA).orElseThrow();
		int[] statsVorher = stats().get(alteNr);

		// Das Property schreibt sonst erst SheetRunner.run(); der Test ruft doRun() direkt.
		docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM, TurnierSystem.JGJ.getId());
		new TeamnummerAendernSheet(wkingSpreadsheet).mitEingabe(alteNr, NEUE_NR).doRun();
		recalcAll();

		assertThat(sheetHlp.getTextFromCell(spielplan, nrA)).as("Nr-Spalte zeigt die neue Nummer")
				.isEqualTo(String.valueOf(NEUE_NR));
		Map<Integer, int[]> statsNachher = stats();
		assertThat(statsNachher).as("Die alte Nummer gibt es nicht mehr").doesNotContainKey(alteNr);
		assertThat(statsNachher.get(NEUE_NR)).as("Dieselben Ergebnisse unter der neuen Nummer")
				.containsExactly(statsVorher);
	}

	private Map<Integer, int[]> stats() throws GenerateException {
		return new JGJRanglisteRechner(wkingSpreadsheet)
				.leseSpielplanStats(new JGJMeldeListeSheet_Update(wkingSpreadsheet).getAktiveMeldungen());
	}
}
