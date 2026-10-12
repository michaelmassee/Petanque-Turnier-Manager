/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.poule.vorrunde;

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
import de.petanqueturniermanager.poule.PouleTurnierTestDaten;
import de.petanqueturniermanager.poule.rangliste.PouleVorrundenRanglisteSheet;

/**
 * Die Nr-Zellen der ersten Vorrunden-Runde tragen die UUID des Teams und zeigen die aktuelle Nummer. Nach
 * „Teamnummer ändern“ führt die neu aufgebaute Vorrunden-Rangliste das Team unter der neuen Nummer mit denselben
 * Ergebnissen; Sieger-/Verlierer-Zeilen folgen der Anzeige.
 */
public class PouleTeamnummerAendernUITest extends BaseCalcUITest {

	private static final int NEUE_NR = 42;
	private static final int MAX_RANGLISTE_ZEILEN = 80;

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
		new PouleTurnierTestDaten(wkingSpreadsheet).generate();
		XSpreadsheet vorrunde = new PouleVorrundeSheet(wkingSpreadsheet).getXSpreadSheet();
		Position nrA = Position.from(AbstractPouleVorrundeSheet.SPALTE_TEAM_A_NR,
				AbstractPouleVorrundeSheet.ERSTE_DATEN_ZEILE);
		String uuidA = TeamUuidFormel.findeUuid(sheetHlp.getFormulaFromCell(vorrunde, nrA)).orElseThrow();
		int alteNr = MeldelisteUuidIndex.lese(wkingSpreadsheet).nr(uuidA).orElseThrow();
		List<String> ranglisteVorher = ranglisteJeNr().get(alteNr);

		// Das Property schreibt sonst erst SheetRunner.run(); der Test ruft doRun() direkt.
		docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM, TurnierSystem.POULE.getId());
		new TeamnummerAendernSheet(wkingSpreadsheet).mitEingabe(alteNr, NEUE_NR).doRun();
		recalcAll();

		assertThat(sheetHlp.getTextFromCell(vorrunde, nrA)).as("Nr-Zelle zeigt die neue Nummer")
				.isEqualTo(String.valueOf(NEUE_NR));

		// Ranglisten führen Nummern als Werte und zeigen die neue Nummer erst nach dem Aktualisieren.
		new PouleVorrundenRanglisteSheet(wkingSpreadsheet).doRun();
		Map<Integer, List<String>> ranglisteNachher = ranglisteJeNr();
		assertThat(ranglisteNachher).as("Die alte Nummer gibt es nicht mehr").doesNotContainKey(alteNr);
		assertThat(ranglisteNachher.get(NEUE_NR)).as("Platz, Gruppe, Name und Ergebnisse unter der neuen Nummer")
				.isEqualTo(ranglisteVorher);
	}

	/** Ranglistenzeile ohne Nr (Platz bis Turnier) je Teamnummer. */
	private Map<Integer, List<String>> ranglisteJeNr() throws GenerateException {
		XSpreadsheet rangliste = new PouleVorrundenRanglisteSheet(wkingSpreadsheet).getXSpreadSheet();
		RangePosition bereich = RangePosition.from(PouleVorrundenRanglisteSheet.SPALTE_PLATZ,
				PouleVorrundenRanglisteSheet.HEADER_ZEILEN, PouleVorrundenRanglisteSheet.SPALTE_TURNIER,
				PouleVorrundenRanglisteSheet.HEADER_ZEILEN + MAX_RANGLISTE_ZEILEN);
		RangeData daten = RangeHelper.from(rangliste, wkingSpreadsheet.getWorkingSpreadsheetDocument(), bereich)
				.getDataFromRange();
		Map<Integer, List<String>> jeNr = new HashMap<>();
		for (RowData zeile : daten) {
			int nr = zeile.size() > PouleVorrundenRanglisteSheet.SPALTE_NR
					? zeile.get(PouleVorrundenRanglisteSheet.SPALTE_NR).getIntVal(0)
					: 0;
			if (nr > 0) {
				List<String> rest = new ArrayList<>();
				for (int i = 0; i < zeile.size(); i++) {
					if (i != PouleVorrundenRanglisteSheet.SPALTE_NR) {
						rest.add(zeile.get(i).getStringVal());
					}
				}
				jeNr.put(nr, rest);
			}
		}
		return jeNr;
	}
}
