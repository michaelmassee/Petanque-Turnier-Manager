package de.petanqueturniermanager.liga.spielplan;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.random.RandomSource;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.liga.rangliste.LigaRanglisteSheet;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;

/**
 * UITest für die Liga-Beispielturniere in zwei Konstellationen:
 * <ul>
 *   <li>6 Teams Hin- und Rückrunde (Standardvariante, gerade Teamanzahl).</li>
 *   <li>7 Teams mit Freispiel-Pfad (ungerade Teamanzahl) – deckt den
 *       Freilos-Code in Spielplan und Rangliste ab.</li>
 * </ul>
 *
 * <p>Reproduzierbarkeit über {@link RandomSource#setSeed(long)}; bei
 * Algorithmen-Änderungen Referenz-JSONs neu erfassen (writeToJson temporär
 * aktivieren und Datei nach src/test/resources/.../liga/spielplan/ kopieren).
 */
public class LigaTurnierTestDatenUITest extends BaseCalcUITest {

	private static final long SEED_FUER_TESTS = 42L;
	private static final int MELDELISTE_ERSTE_DATEN_ZEILE = 2;
	private static final String[] TEAMNAMEN_6 = {
			"Boule Biebertal",
			"Boule-Freunde Fernwald",
			"Boulefreunde Marburg",
			"Boulodromedare Fulda 2",
			"DFG Wettenberg 1",
			"PC Petterweil"
	};
	private static final String[] TEAMNAMEN_7 = {
			"BC-Linden 1",
			"Boule Biebertal",
			"Boule-Freunde Fernwald",
			"Boulefreunde Marburg",
			"Boulodromedare Fulda 2",
			"DFG Wettenberg 1",
			"PC Petterweil"
	};

	@BeforeEach
	@Override
	public void beforeTest() {
		super.beforeTest();
		RandomSource.setSeed(SEED_FUER_TESTS);
	}

	@AfterEach
	public void resetRandom() {
		RandomSource.reset();
	}

	@Test
	public void testLigaTurnier6Teams() throws GenerateException {
		final int anzTeams = 6;
		new LigaTurnierTestDaten(wkingSpreadsheet).erzeugeBeispielturnier();

		validiereGrundstruktur(TEAMNAMEN_6[0]);
		validiereMeldelistePerJson(anzTeams, "liga-meldeliste.json");
		validiereSpielplanPerJson("liga-spielplan.json");
		validiereTermineProTeilnehmer(TEAMNAMEN_6, 10);
		validiereRanglistePerJson(anzTeams, "liga-rangliste.json");
	}

	@Test
	public void testLigaTurnierMitFreispiel() throws GenerateException {
		final int anzTeams = 7;
		new LigaMitFreispielTurnierTestDaten(wkingSpreadsheet).erzeugeBeispielturnier();

		validiereGrundstruktur(TEAMNAMEN_7[0]);
		validiereMeldelistePerJson(anzTeams, "liga-freispiel-meldeliste.json");
		validiereSpielplanPerJson("liga-freispiel-spielplan.json");
		validiereTermineProTeilnehmer(TEAMNAMEN_7, 14);
		validiereRanglistePerJson(anzTeams, "liga-freispiel-rangliste.json");
	}

	/**
	 * Korrektheit der PTM-Metadaten (6 Teams): Meldeliste, Spielplan, Termine und Rangliste müssen je
	 * exakt ihren Identitäts-Schlüssel tragen – kein weiteres Blatt einen unerwarteten.
	 * (Liga erzeugt kein Direktvergleich-Blatt im Beispielturnier.)
	 */
	@Test
	public void jedesBlattTraegtKorrektenSchluessel() throws GenerateException {
		new LigaTurnierTestDaten(wkingSpreadsheet).erzeugeBeispielturnier();

		Map<String, String> erwartung = new LinkedHashMap<>();
		erwartung.put(SheetNamen.meldeliste(), SheetMetadataHelper.SCHLUESSEL_LIGA_MELDELISTE);
		erwartung.put(SheetNamen.spielplan(), SheetMetadataHelper.SCHLUESSEL_LIGA_SPIELPLAN);
		for (int teamNr = 1; teamNr <= TEAMNAMEN_6.length; teamNr++) {
			erwartung.put(TEAMNAMEN_6[teamNr - 1],
					SheetMetadataHelper.schluesselLigaTermineProTeilnehmer(teamNr));
		}
		erwartung.put(SheetNamen.rangliste(), SheetMetadataHelper.SCHLUESSEL_LIGA_RANGLISTE);

		pruefeJedesBlattTraegtKorrektenSchluessel(erwartung);
	}

	private void validiereGrundstruktur(String ersterTerminSheetName) {
		assertThat(sheetHlp.findByName(SheetNamen.meldeliste()))
				.as("Meldeliste-Sheet muss existieren").isNotNull();
		assertThat(sheetHlp.findByName(SheetNamen.spielplan()))
				.as("Spielplan-Sheet muss existieren").isNotNull();
		assertThat(sheetHlp.findByName(ersterTerminSheetName))
				.as("Termine-pro-Teilnehmer-Sheet für Team 1 muss existieren").isNotNull();
		assertThat(sheetHlp.findByName(SheetNamen.rangliste()))
				.as("Rangliste-Sheet muss existieren").isNotNull();
	}

	private void validiereMeldelistePerJson(int anzTeams, String referenzDatei) throws GenerateException {
		XSpreadsheet meldeliste = sheetHlp.findByName(SheetNamen.meldeliste());
		RangePosition meldelisteRange = RangePosition.from(
				0, MELDELISTE_ERSTE_DATEN_ZEILE,
				2, MELDELISTE_ERSTE_DATEN_ZEILE + anzTeams - 1);

		// writeToJson(referenzDatei, meldelisteRange, meldeliste, wkingSpreadsheet.getWorkingSpreadsheetDocument());

		RangeData rangeData = rangeDateFromRangePosition(meldelisteRange, meldeliste,
				wkingSpreadsheet.getWorkingSpreadsheetDocument());

		InputStream jsonFile = LigaTurnierTestDatenUITest.class.getResourceAsStream(referenzDatei);
		validateWithJson(rangeData, jsonFile);
	}

	private void validiereTermineProTeilnehmer(String[] teamNamen, int erwarteteTermineProTeam) {
		for (int teamIndex = 0; teamIndex < teamNamen.length; teamIndex++) {
			int teamNr = teamIndex + 1;
			XSpreadsheet termine = sheetHlp.findByName(teamNamen[teamIndex]);
			assertThat(termine)
					.as("Termine-Sheet für Team %d muss den Teilnehmernamen tragen", teamNr)
					.isNotNull();
			RangeData rangeData = rangeDateFromRangePosition(
					RangePosition.from(0, 0, 5, 1 + erwarteteTermineProTeam + 5),
					termine, wkingSpreadsheet.getWorkingSpreadsheetDocument());

			assertThat(rangeData.get(0).get(0).getStringVal()).isEqualTo("Spiel");
			assertThat(rangeData.get(0).get(1).getStringVal()).isEqualTo("Datum");
			assertThat(rangeData.get(0).get(5).getStringVal()).isEqualTo("Gegner");

			int anzahlTermine = 0;
			for (int zeile = 1; zeile < rangeData.size(); zeile++) {
				String spielNr = rangeData.get(zeile).get(0).getStringVal();
				if (spielNr == null || spielNr.isBlank()) {
					break;
				}
				anzahlTermine++;
			}

			assertThat(anzahlTermine)
					.as("Team %d muss die erwartete Anzahl Termine haben", teamNr)
					.isEqualTo(erwarteteTermineProTeam);
		}
	}

	private void validiereSpielplanPerJson(String referenzDatei) throws GenerateException {
		XSpreadsheet spielplan = sheetHlp.findByName(SheetNamen.spielplan());
		// 6 Teams: 10 Spieltage; 7 Teams mit Freispiel: 14 Spieltage. Großzügiger Bereich.
		RangePosition spielplanRange = RangePosition.from(0, 0, 12, 110);

		// writeToJson(referenzDatei, spielplanRange, spielplan, wkingSpreadsheet.getWorkingSpreadsheetDocument());

		RangeData rangeData = rangeDateFromRangePosition(spielplanRange, spielplan,
				wkingSpreadsheet.getWorkingSpreadsheetDocument());

		InputStream jsonFile = LigaTurnierTestDatenUITest.class.getResourceAsStream(referenzDatei);
		validateWithJson(rangeData, jsonFile);
	}

	private void validiereRanglistePerJson(int anzTeams, String referenzDatei) throws GenerateException {
		XSpreadsheet rangliste = sheetHlp.findByName(SheetNamen.rangliste());
		// Rangliste hat 2 Header-Zeilen plus anzTeams Datenzeilen; +1 Puffer am Ende.
		RangePosition ranglisteRange = RangePosition.from(0, 0, 6, 2 + anzTeams);

		// writeToJson(referenzDatei, ranglisteRange, rangliste, wkingSpreadsheet.getWorkingSpreadsheetDocument());

		RangeData rangeData = rangeDateFromRangePosition(ranglisteRange, rangliste,
				wkingSpreadsheet.getWorkingSpreadsheetDocument());

		InputStream jsonFile = LigaTurnierTestDatenUITest.class.getResourceAsStream(referenzDatei);
		validateWithJson(rangeData, jsonFile);
	}

	/**
	 * Regression im Kiosk-Modus: nach voller 6-Team-Turniergenerierung muss ein
	 * erneutes {@link LigaRanglisteSheet#run()} unter aktivem TurnierModus +
	 * Liga-Blattschutz sauber durchlaufen.
	 */
	@Test
	public void kioskModus_ranglisteUpdateNach6TeamTurnier() throws GenerateException {
		new LigaTurnierTestDaten(wkingSpreadsheet).erzeugeBeispielturnier();
		mitKioskModus(TurnierSystem.LIGA, () -> new LigaRanglisteSheet(wkingSpreadsheet).run());

		assertThat(sheetHlp.findByName(SheetNamen.rangliste()))
				.as("Liga-Rangliste muss nach Kiosk-Update weiterhin existieren")
				.isNotNull();
	}
}
