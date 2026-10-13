/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.supermelee.endrangliste;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.random.RandomSource;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.supermelee.spielrunde.SpielrundeSheetKonstanten;
import de.petanqueturniermanager.supermelee.spieltagrangliste.SupermeleeTurnierTestDaten;

/**
 * Die Spielernummern der Supermelee-Spielrunden tragen die UUID des Spielers und zeigen die aktuelle Nummer. Nach
 * „Teamnummer ändern“ werden die Spieltag-Ranglisten neu aufgebaut; die Endrangliste führt den Spieler unter der
 * neuen Nummer mit denselben Ergebnissen aller Spieltage.
 */
public class SupermeleeTeamnummerAendernUITest extends BaseCalcUITest {

	private static final Logger logger = LogManager.getLogger(SupermeleeTeamnummerAendernUITest.class);

	/** Das Beispielturnier hat 100 Spieler; die Nummer ist frei, es wird nicht getauscht. */
	private static final int NEUE_NR = 142;
	private static final int MAX_ENDRANGLISTE_ZEILEN = 120;

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
	public void endranglisteFuehrtErgebnisseUnterDerNeuenNummer() throws GenerateException {
		new SupermeleeTurnierTestDaten(wkingSpreadsheet).generate();
		XSpreadsheet ersteRunde = sheetHlp.findByName(SheetNamen.supermeleeSpielrunde(1, 1));
		Position ersterSpieler = Position.from(SpielrundeSheetKonstanten.ERSTE_SPIELERNR_SPALTE,
				SpielrundeSheetKonstanten.ERSTE_DATEN_ZEILE);
		String uuid = TeamUuidFormel.findeUuid(sheetHlp.getFormulaFromCell(ersteRunde, ersterSpieler)).orElseThrow();
		int alteNr = MeldelisteUuidIndex.lese(wkingSpreadsheet).nr(uuid).orElseThrow();
		new EndranglisteSheet(wkingSpreadsheet).doRun();
		List<String> endranglisteVorher = endranglisteJeNr().get(alteNr);
		String nameVorher = endranglisteVorher.get(0);
		pruefeUuidName(new EndranglisteSheet(wkingSpreadsheet).getXSpreadSheet(), uuid, alteNr, nameVorher);
		pruefeUuidName(sheetHlp.findByName(SheetNamen.spieltagRangliste(1)), uuid, alteNr, nameVorher);

		// Das Property schreibt sonst erst SheetRunner.run(); der Test ruft doRun() direkt.
		docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM,
				TurnierSystem.SUPERMELEE.getId());
		long start = System.nanoTime();
		new TeamnummerAendernSheet(wkingSpreadsheet).mitEingabe(alteNr, NEUE_NR).doRun();
		logger.info("Teamnummer ändern inkl. Neuaufbau der Spieltag-Ranglisten: {} ms",
				(System.nanoTime() - start) / 1_000_000);
		recalcAll();

		assertThat(sheetHlp.getTextFromCell(ersteRunde, ersterSpieler)).as("Spielrunde zeigt die neue Nummer")
				.isEqualTo(String.valueOf(NEUE_NR));

		new EndranglisteSheet(wkingSpreadsheet).doRun();
		Map<Integer, List<String>> endranglisteNachher = endranglisteJeNr();
		assertThat(endranglisteNachher).as("Die alte Nummer gibt es nicht mehr").doesNotContainKey(alteNr);
		assertThat(endranglisteNachher.get(NEUE_NR)).as("Name, Platz und Ergebnisse unter der neuen Nummer")
				.isEqualTo(endranglisteVorher);
		pruefeUuidName(new EndranglisteSheet(wkingSpreadsheet).getXSpreadSheet(), uuid, NEUE_NR, nameVorher);
		pruefeUuidName(sheetHlp.findByName(SheetNamen.spieltagRangliste(1)), uuid, NEUE_NR, nameVorher);
	}

	private void pruefeUuidName(XSpreadsheet sheet, String uuid, int nr, String name) {
		for (int zeile = EndranglisteSheet.ERSTE_DATEN_ZEILE; zeile < MAX_ENDRANGLISTE_ZEILEN; zeile++) {
			Position nrPos = Position.from(0, zeile);
			if (TeamUuidFormel.findeUuid(sheetHlp.getFormulaFromCell(sheet, nrPos)).filter(uuid::equals).isPresent()) {
				assertThat(sheetHlp.getIntFromCell(sheet, nrPos)).isEqualTo(nr);
				Position namePos = Position.from(1, zeile);
				assertThat(sheetHlp.getFormulaFromCell(sheet, namePos)).containsIgnoringCase("PTMUUIDTEAMANZEIGE")
						.contains(uuid).doesNotContain("MATCH(", "VLOOKUP");
				assertThat(sheetHlp.getTextFromCell(sheet, namePos)).isEqualTo(name);
				return;
			}
		}
		throw new AssertionError("Rangliste enthält die UUID nicht: " + uuid);
	}

	/** Endranglistenzeile ohne Nr (Name bis letzte Spalte) je Spielernummer. */
	private Map<Integer, List<String>> endranglisteJeNr() throws GenerateException {
		EndranglisteSheet endrangliste = new EndranglisteSheet(wkingSpreadsheet);
		RangePosition bereich = RangePosition.from(EndranglisteSheet.SPIELER_NR_SPALTE,
				EndranglisteSheet.ERSTE_DATEN_ZEILE, endrangliste.getLetzteSpalte(),
				EndranglisteSheet.ERSTE_DATEN_ZEILE + MAX_ENDRANGLISTE_ZEILEN);
		RangeData daten = RangeHelper.from(endrangliste.getXSpreadSheet(),
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
