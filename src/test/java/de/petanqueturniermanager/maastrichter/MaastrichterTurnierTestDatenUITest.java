package de.petanqueturniermanager.maastrichter;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.star.beans.XPropertySet;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.BorderLineStyle;
import com.sun.star.table.TableBorder2;
import com.sun.star.text.XText;
import com.sun.star.uno.UnoRuntime;

import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;
import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.random.RandomSource;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.maastrichter.korunde.KoGruppeABSheet;
import de.petanqueturniermanager.maastrichter.finalrunde.MaastrichterFinalrundeSheet;
import de.petanqueturniermanager.maastrichter.rangliste.MaastrichterVorrundenRanglisteSheetUpdate;
import de.petanqueturniermanager.schweizer.rangliste.SchweizerRanglisteSheet;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerAbstractSpielrundeSheet;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;

/**
 * UITest für die Maastrichter-Beispielturniere:
 * <ul>
 *   <li>12 Teams, 3 Vorrunden, Finalrunde A – Standardvariante.</li>
 *   <li>57 Teams, 4 Vorrunden, Finalgruppen A/B/C/D (Gruppe D mit Cadrage) – validiert
 *       den Mehrgruppenfall inklusive Cadrage-Pfad.</li>
 *   <li>Forme-Phase {@link KoGruppeABSheet} oberhalb der 12-Team-Vorrunde.</li>
 * </ul>
 *
 * <p>Reproduzierbarkeit über {@link RandomSource#setSeed(long)}: Spielergebnisse,
 * Bahnen-Auslosung (SpielrundeSpielbahn.R) und KO-Gruppen-Reihenfolge werden gegen
 * JSON-Referenzdateien verglichen. Bei Algorithmen-Änderungen müssen die Referenzen
 * neu erfasst werden – hierzu temporär {@code writeToJson(...)} aktivieren.
 */
public class MaastrichterTurnierTestDatenUITest extends BaseCalcUITest {

	private static final long SEED_FUER_TESTS = 42L;
	private static final int MELDELISTE_ERSTE_DATEN_ZEILE = 3;
	private static final int MELDELISTE_NACHNAME_SPALTE = 2;
	/** CellBackColor-Wert für „keine Farbe" (transparent) – so setzt der Bracket-Aufbau Leerflächen zurück. */
	private static final int TRANSPARENT = -1;

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
	public void testMaastrichterTurnier12Teams() throws GenerateException {
		final int anzTeams = 12;
		final int anzVorrunden = 3;
		new MaastrichterTurnierTestDaten(wkingSpreadsheet).generate();

		assertThat(sheetHlp.findByName(SheetNamen.meldeliste()))
				.as("Meldeliste-Sheet muss existieren").isNotNull();
		assertThat(sheetHlp.findByName(SheetNamen.maastrichterVorrundenRangliste()))
				.as("Vorrunden-Rangliste-Sheet muss existieren").isNotNull();
		assertThat(sheetHlp.findByName(SheetNamen.teilnehmer()))
				.as("Teilnehmer-Sheet muss existieren").isNotNull();
		// Bei 12 Teams nutzt die Maastrichter-Finalrunde das KO-Bracket (Sheet-Name "A-Finale").
		assertThat(sheetHlp.findByName(SheetNamen.koFinaleGruppe("A")))
				.as("A-Finale-Sheet muss existieren").isNotNull();

		validiereMeldelistePerJson(anzTeams, "maastrichter-meldeliste.json");
		for (int runde = 1; runde <= anzVorrunden; runde++) {
			assertThat(sheetHlp.findByName(SheetNamen.maastrichterVorrunde(runde)))
					.as("Maastrichter Vorrunde %s muss den Spielrunden-Sheetnamen verwenden", runde)
					.isNotNull();
			validiereVorrundenErgebnissePerJson(anzTeams, runde, "maastrichter-vorrunde-" + runde + ".json");
		}
		validiereVorrundenRanglistePerJson(anzTeams, "maastrichter-vorrundenrangliste.json");
		validiereTeilnehmerPerJson(anzTeams, "maastrichter-teilnehmer.json");
		validiereFinaleGruppePerJson("A", "maastrichter-finale-a.json");
	}

	@Test
	public void maastrichterVorrundeHatDoppelteLinieRechtsVonSpalteB() throws Exception {
		new MaastrichterTurnierTestDaten(wkingSpreadsheet).generate();

		XSpreadsheet spielrundeSheet = sheetHlp.findByName(SheetNamen.maastrichterVorrunde(1));
		assertThat(spielrundeSheet).as("Maastrichter Vorrunde 1 muss vorhanden sein").isNotNull();

		assertDoppelteRechteLinie(spielrundeSheet, SchweizerAbstractSpielrundeSheet.ZWEITE_HEADER_ZEILE);
		assertDoppelteRechteLinie(spielrundeSheet, SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE);
	}

	@Test
	public void testMaastrichterTurnier57TeamsVierGruppen() throws GenerateException {
		final int anzTeams = 57;
		final int anzVorrunden = 4;
		new Maastrichter57TeamsTurnierTestDaten(wkingSpreadsheet).generate();

		assertThat(sheetHlp.findByName(SheetNamen.meldeliste())).isNotNull();
		assertThat(sheetHlp.findByName(SheetNamen.maastrichterVorrundenRangliste())).isNotNull();
		assertThat(sheetHlp.findByName(SheetNamen.teilnehmer())).isNotNull();
		for (String gruppe : new String[]{"A", "B", "C", "D"}) {
			assertThat(sheetHlp.findByName(SheetNamen.koFinaleGruppe(gruppe)))
					.as("Finalgruppe '%s' muss existieren", gruppe).isNotNull();
		}

		validiereMeldelistePerJson(anzTeams, "maastrichter-57-meldeliste.json");
		for (int runde = 1; runde <= anzVorrunden; runde++) {
			validiereVorrundenErgebnissePerJson(anzTeams, runde, "maastrichter-57-vorrunde-" + runde + ".json");
		}
		validiereVorrundenRanglistePerJson(anzTeams, "maastrichter-57-vorrundenrangliste.json");
		validiereTeilnehmerPerJson(anzTeams, "maastrichter-57-teilnehmer.json");
		validiereFinaleGruppePerJson("A", "maastrichter-57-finale-a.json");
		validiereFinalrundeLeerflaechenSindTransparent("A");
		validiereFinaleGruppePerJson("B", "maastrichter-57-finale-b.json");
		validiereFinalrundenBleibenNachTabWechselUnveraendert("A", "B");
		validiereFinaleGruppePerJson("C", "maastrichter-57-finale-c.json");
		validiereFinaleGruppePerJson("D", "maastrichter-57-finale-d.json");
		// Zuletzt, weil der bestätigte Neuaufbau die Bahn-Auslosung (RandomSource) neu würfelt
		// und damit die JSON-Referenzvergleiche oben nicht mehr reproduzierbar wären.
		validiereFinalrundeNeuAufbauNachBestaetigung("A");
	}

	@Test
	public void maastrichterZweiFinalgruppenNummeriertErsteKoRundeGruppenuebergreifend() throws GenerateException {
		new MaastrichterTurnierTestDaten(wkingSpreadsheet, 25, 3, 16).generate();

		var konfig = new de.petanqueturniermanager.maastrichter.konfiguration.MaastrichterKonfigurationSheet(
				wkingSpreadsheet);
		konfig.setSpielbaumSpielbahn(SpielrundeSpielbahn.N);
		new MaastrichterFinalrundeSheet(wkingSpreadsheet).doRun();

		XSpreadsheet gruppeA = sheetHlp.findByName(SheetNamen.koFinaleGruppe("A"));
		assertThat(gruppeA).as("A-Finale muss bei 25 Teams und Gruppengroesse 16 existieren").isNotNull();
		assertThat(sheetHlp.getIntFromCell(gruppeA, Position.from(0, 3)))
				.as("Erste KO-Runde beginnt in Gruppe A mit Bahn 1")
				.isEqualTo(1);

		XSpreadsheet gruppeB = sheetHlp.findByName(SheetNamen.koFinaleGruppe("B"));
		assertThat(gruppeB).as("B-Finale muss bei 25 Teams und Gruppengroesse 16 existieren").isNotNull();
		assertThat(sheetHlp.getTextFromCell(gruppeB, Position.from(0, 2)))
				.as("Cadrage muss eine Bahn-Spalte haben")
				.isEqualTo("Bn");
		assertThat(sheetHlp.getTextFromCell(gruppeB, Position.from(4, 2)))
				.as("Wenn Cadrage die erste Runde ist, hat Runde 1 keine Bahn-Spalte")
				.isEqualTo("Nr");
		assertThat(sheetHlp.getIntFromCell(gruppeB, Position.from(0, 5)))
				.as("Cadrage zaehlt als erste KO-Runde und wird nach Gruppe A weiter nummeriert")
				.isEqualTo(9);
	}

	@Test
	public void maastrichterRanglisteUpdateEntferntZebraUnterhalbDerTabelle() throws GenerateException {
		new MaastrichterTurnierTestDaten(wkingSpreadsheet).generate();

		var xDoc = wkingSpreadsheet.getWorkingSpreadsheetDocument();
		XSpreadsheet rangliste = SheetMetadataHelper.findeSheetUndHeile(xDoc,
				SheetMetadataHelper.SCHLUESSEL_MAASTRICHTER_VORRUNDE_PREFIX, null);
		assertThat(rangliste).as("Maastrichter Vorrunden-Rangliste muss existieren").isNotNull();

		int ersteAlteZeile = SchweizerRanglisteSheet.ERSTE_DATEN_ZEILE + 10;
		int zweiteAlteZeile = ersteAlteZeile + 1;
		assertThat(cellBackColor(rangliste, SchweizerRanglisteSheet.TEAM_NR_SPALTE, ersteAlteZeile))
				.as("Vorbedingung: alte Zeile 11 hat Zebra-Farbe")
				.isNotEqualTo(TRANSPARENT);
		assertThat(cellBackColor(rangliste, SchweizerRanglisteSheet.TEAM_NR_SPALTE, zweiteAlteZeile))
				.as("Vorbedingung: alte Zeile 12 hat Zebra-Farbe")
				.isNotEqualTo(TRANSPARENT);

		XSpreadsheet meldeliste = SheetMetadataHelper.findeSheetUndHeile(xDoc,
				SheetMetadataHelper.SCHLUESSEL_MAASTRICHTER_MELDELISTE, null);
		assertThat(meldeliste).as("Maastrichter Meldeliste muss existieren").isNotNull();
		RangeHelper.from(meldeliste, xDoc, RangePosition.from(0,
				MELDELISTE_ERSTE_DATEN_ZEILE + 10, 8, MELDELISTE_ERSTE_DATEN_ZEILE + 11))
				.clearRange();

		new MaastrichterVorrundenRanglisteSheetUpdate(wkingSpreadsheet).doRun();

		assertThat(cellBackColor(rangliste, SchweizerRanglisteSheet.TEAM_NR_SPALTE, ersteAlteZeile))
				.as("Erste Zeile unterhalb der Tabelle darf keine Zebra-Farbe behalten")
				.isEqualTo(TRANSPARENT);
		assertThat(cellBackColor(rangliste, SchweizerRanglisteSheet.TEAM_NR_SPALTE, zweiteAlteZeile))
				.as("Zweite Zeile unterhalb der Tabelle darf keine Zebra-Farbe behalten")
				.isEqualTo(TRANSPARENT);
	}

	/**
	 * Forme-Phase: nach der vollständigen Maastrichter-Vorrunde wird zusätzlich die
	 * KO-Gruppe AB (Forme/KoGruppeABSheet) erzeugt.
	 */
	@Test
	public void testKoGruppeAbAlsFormeNachVorrunde() throws GenerateException {
		new MaastrichterTurnierTestDaten(wkingSpreadsheet).generate();

		new KoGruppeABSheet(wkingSpreadsheet).run();

		XSpreadsheet koRunde = sheetHlp.findByName(SheetNamen.koRunde());
		assertThat(koRunde).as("KoRunde-Sheet (Forme) muss nach KoGruppeABSheet.run() existieren").isNotNull();
	}

	/**
	 * Regressionstest: Ein Freilos-Team (ungerade Teamanzahl) muss in der
	 * Vorrunden-Rangliste die konfigurierten Freispiel-Punkte (Default 13:7) verbucht
	 * bekommen – nicht 0:0. Maastrichter erbt die Rangliste-Berechnung direkt von
	 * {@link SchweizerRanglisteSheet}, daher gilt der gleiche Fix wie im Schweizer System.
	 */
	@Test
	public void testFreilosBekommtFreispielPunkte() throws GenerateException {
		final int anzTeamsUngerade = 13;
		final int anzVorrunden = 1;
		new MaastrichterTurnierTestDaten(wkingSpreadsheet, anzTeamsUngerade, anzVorrunden, 16).generate();

		var konfig = new de.petanqueturniermanager.maastrichter.konfiguration.MaastrichterKonfigurationSheet(
				wkingSpreadsheet);
		int freispielPlus = konfig.getFreispielPunktePlus();
		int freispielMinus = konfig.getFreispielPunkteMinus();

		XSpreadsheet vorrunde1 = sheetHlp.findByName(SheetNamen.maastrichterVorrunde(1));
		assertThat(vorrunde1).as("Maastrichter Vorrunde 1 muss existieren").isNotNull();
		int freilosTeamNr = ermittleFreilosTeamNr(vorrunde1, anzTeamsUngerade);
		assertThat(freilosTeamNr).as("Bei ungerader Teamanzahl muss genau ein Freilos existieren").isGreaterThan(0);

		XSpreadsheet rangliste = sheetHlp.findByName(SheetNamen.maastrichterVorrundenRangliste());
		assertThat(rangliste).as("Vorrunden-Rangliste-Sheet muss vorhanden sein").isNotNull();
		RangePosition ranglisteRange = RangePosition.from(
				SchweizerRanglisteSheet.TEAM_NR_SPALTE, SchweizerRanglisteSheet.ERSTE_DATEN_ZEILE,
				SchweizerRanglisteSheet.PUNKTE_DIFF_SPALTE,
				SchweizerRanglisteSheet.ERSTE_DATEN_ZEILE + anzTeamsUngerade - 1);
		RangeData data = RangeHelper
				.from(rangliste, wkingSpreadsheet.getWorkingSpreadsheetDocument(), ranglisteRange)
				.getDataFromRange();

		RowData freilosZeile = data.stream()
				.filter(row -> row.get(SchweizerRanglisteSheet.TEAM_NR_SPALTE).getIntVal(-1) == freilosTeamNr)
				.findFirst()
				.orElseThrow();

		assertThat(freilosZeile.get(SchweizerRanglisteSheet.SIEGE_SPALTE).getIntVal(-1))
				.as("Freilos-Team muss als Sieg gezählt werden").isEqualTo(1);
		assertThat(freilosZeile.get(SchweizerRanglisteSheet.PUNKTE_PLUS_SPALTE).getIntVal(-1))
				.as("Freilos-Team muss die konfigurierten Freispiel-Punkte+ (%d) verbucht bekommen", freispielPlus)
				.isEqualTo(freispielPlus);
		assertThat(freilosZeile.get(SchweizerRanglisteSheet.PUNKTE_MINUS_SPALTE).getIntVal(-1))
				.as("Freilos-Team muss die konfigurierten Freispiel-Punkte- (%d) verbucht bekommen", freispielMinus)
				.isEqualTo(freispielMinus);
		assertThat(freilosZeile.get(SchweizerRanglisteSheet.PUNKTE_DIFF_SPALTE).getIntVal(Integer.MIN_VALUE))
				.as("Punkte-Differenz des Freilos-Teams muss Freispiel+ - Freispiel- sein")
				.isEqualTo(freispielPlus - freispielMinus);
	}

	/**
	 * Sucht in der Spielrunde die Zeile ohne Gegner-Team (Freilos) und liefert die
	 * TeamNr von Team A dieser Zeile, oder -1 falls keine gefunden wurde.
	 */
	private int ermittleFreilosTeamNr(XSpreadsheet rundeSheet, int anzTeams) throws GenerateException {
		RangePosition leseRange = RangePosition.from(
				SchweizerAbstractSpielrundeSheet.TEAM_A_SPALTE, SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE,
				SchweizerAbstractSpielrundeSheet.TEAM_B_SPALTE,
				SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE + anzTeams);
		RangeData data = RangeHelper
				.from(rundeSheet, wkingSpreadsheet.getWorkingSpreadsheetDocument(), leseRange)
				.getDataFromRange();

		for (RowData row : data) {
			int nrA = row.get(0).getIntVal(-1);
			if (nrA <= 0) break;
			int nrB = row.get(1).getIntVal(-1);
			if (nrB <= 0) return nrA;
		}
		return -1;
	}

	/**
	 * Korrektheit der PTM-Metadaten (12 Teams, 3 Vorrunden, A-Finale): Meldeliste, alle drei
	 * Vorrunden, Vorrunden-Rangliste, Teilnehmer und das A-Finale-Bracket müssen je exakt ihren
	 * Identitäts-Schlüssel tragen – kein weiteres Blatt einen unerwarteten.
	 */
	@Test
	public void jedesBlattTraegtKorrektenSchluessel12Teams() throws GenerateException {
		new MaastrichterTurnierTestDaten(wkingSpreadsheet).generate();

		Map<String, String> erwartung = maastrichterBasisErwartung(3);
		erwartung.put(SheetNamen.koFinaleGruppe("A"), SheetMetadataHelper.schluesselMaastrichterFinalrunde("A"));

		pruefeJedesBlattTraegtKorrektenSchluessel(erwartung);
	}

	/**
	 * Korrektheit der PTM-Metadaten (57 Teams, 4 Vorrunden, Finalgruppen A–D): zusätzlich zu den
	 * vier Vorrunden müssen alle vier Finalgruppen-Brackets ihren Identitäts-Schlüssel tragen.
	 */
	@Test
	public void jedesBlattTraegtKorrektenSchluessel57TeamsVierGruppen() throws GenerateException {
		new Maastrichter57TeamsTurnierTestDaten(wkingSpreadsheet).generate();

		Map<String, String> erwartung = maastrichterBasisErwartung(4);
		for (String gruppe : new String[]{"A", "B", "C", "D"}) {
			erwartung.put(SheetNamen.koFinaleGruppe(gruppe),
					SheetMetadataHelper.schluesselMaastrichterFinalrunde(gruppe));
		}

		pruefeJedesBlattTraegtKorrektenSchluessel(erwartung);
	}

	private Map<String, String> maastrichterBasisErwartung(int anzVorrunden) {
		Map<String, String> erwartung = new LinkedHashMap<>();
		erwartung.put(SheetNamen.meldeliste(), SheetMetadataHelper.SCHLUESSEL_MAASTRICHTER_MELDELISTE);
		for (int runde = 1; runde <= anzVorrunden; runde++) {
			erwartung.put(SheetNamen.maastrichterVorrunde(runde),
					SheetMetadataHelper.schluesselMaastrichterVorrunde(runde));
		}
		erwartung.put(SheetNamen.maastrichterVorrundenRangliste(),
				SheetMetadataHelper.SCHLUESSEL_MAASTRICHTER_VORRUNDE_PREFIX);
		erwartung.put(SheetNamen.teilnehmer(), SheetMetadataHelper.SCHLUESSEL_TEILNEHMER);
		return erwartung;
	}

	private void validiereMeldelistePerJson(int anzTeams, String referenzDatei) throws GenerateException {
		XSpreadsheet meldeliste = sheetHlp.findByName(SheetNamen.meldeliste());
		RangePosition meldelisteRange = RangePosition.from(
				0, MELDELISTE_ERSTE_DATEN_ZEILE,
				MELDELISTE_NACHNAME_SPALTE, MELDELISTE_ERSTE_DATEN_ZEILE + anzTeams - 1);

		// writeToJson(referenzDatei, meldelisteRange, meldeliste, wkingSpreadsheet.getWorkingSpreadsheetDocument());

		RangeData rangeData = rangeDateFromRangePosition(meldelisteRange, meldeliste,
				wkingSpreadsheet.getWorkingSpreadsheetDocument());

		InputStream jsonFile = MaastrichterTurnierTestDatenUITest.class.getResourceAsStream(referenzDatei);
		validateWithJson(rangeData, jsonFile);
	}

	private void validiereVorrundenErgebnissePerJson(int anzTeams, int rundeNr, String referenzDatei)
			throws GenerateException {
		String sheetName = SheetNamen.maastrichterVorrunde(rundeNr);
		XSpreadsheet sheet = sheetHlp.findByName(sheetName);
		assertThat(sheet).as("Vorrunden-Sheet '%s' muss existieren", sheetName).isNotNull();

		// Schweizer-Format: anzTeams/2 Paarungen pro Runde, Spalten 0..6.
		RangePosition vorrundenRange = RangePosition.from(
				0, SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE,
				6, SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE + (anzTeams / 2) - 1);

		// writeToJson(referenzDatei, vorrundenRange, sheet, wkingSpreadsheet.getWorkingSpreadsheetDocument());

		RangeData rangeData = rangeDateFromRangePosition(vorrundenRange, sheet,
				wkingSpreadsheet.getWorkingSpreadsheetDocument());

		InputStream jsonFile = MaastrichterTurnierTestDatenUITest.class.getResourceAsStream(referenzDatei);
		validateWithJson(rangeData, jsonFile);
	}

	private void validiereVorrundenRanglistePerJson(int anzTeams, String referenzDatei) throws GenerateException {
		XSpreadsheet rangliste = sheetHlp.findByName(SheetNamen.maastrichterVorrundenRangliste());
		assertThat(rangliste).isNotNull();

		// Rangliste-Bereich: Spalten 0..3 (Platz, Nr, Team, Punkte).
		RangePosition ranglisteRange = RangePosition.from(
				0, SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE,
				3, SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE + anzTeams - 1);

		// writeToJson(referenzDatei, ranglisteRange, rangliste, wkingSpreadsheet.getWorkingSpreadsheetDocument());

		RangeData rangeData = rangeDateFromRangePosition(ranglisteRange, rangliste,
				wkingSpreadsheet.getWorkingSpreadsheetDocument());

		InputStream jsonFile = MaastrichterTurnierTestDatenUITest.class.getResourceAsStream(referenzDatei);
		validateWithJson(rangeData, jsonFile);
	}

	private void validiereTeilnehmerPerJson(int anzTeams, String referenzDatei) throws GenerateException {
		XSpreadsheet sheet = sheetHlp.findByName(SheetNamen.teilnehmer());
		assertThat(sheet).isNotNull();

		// Teilnehmerliste (Doublette → 2 Spieler pro Team).
		RangePosition teilnehmerRange = RangePosition.from(0, 1, 3, anzTeams * 2);

		// writeToJson(referenzDatei, teilnehmerRange, sheet, wkingSpreadsheet.getWorkingSpreadsheetDocument());

		RangeData rangeData = rangeDateFromRangePosition(teilnehmerRange, sheet,
				wkingSpreadsheet.getWorkingSpreadsheetDocument());

		InputStream jsonFile = MaastrichterTurnierTestDatenUITest.class.getResourceAsStream(referenzDatei);
		validateWithJson(rangeData, jsonFile);
	}

	private void validiereFinaleGruppePerJson(String gruppenBuchstabe, String referenzDatei) throws GenerateException {
		XSpreadsheet sheet = sheetHlp.findByName(SheetNamen.koFinaleGruppe(gruppenBuchstabe));
		assertThat(sheet).as("Finalgruppe-Sheet '%s' muss existieren", gruppenBuchstabe).isNotNull();

		// Großzügiger Bereich – analog KO-Turnierbaum, deckt 4–16-Team-Gruppen inkl. Cadrage ab.
		RangePosition finaleRange = RangePosition.from(0, 0, 23, 32);

		// writeToJson(referenzDatei, finaleRange, sheet, wkingSpreadsheet.getWorkingSpreadsheetDocument());

		RangeData rangeData = rangeDateFromRangePosition(finaleRange, sheet,
				wkingSpreadsheet.getWorkingSpreadsheetDocument());

		InputStream jsonFile = MaastrichterTurnierTestDatenUITest.class.getResourceAsStream(referenzDatei);
		validateWithJson(rangeData, jsonFile);
	}

	/**
	 * Leere Bracket-Flächen werden bewusst transparent ({@code CellBackColor == -1}) zurückgesetzt
	 * (siehe {@code KoTurnierbaumSheet#setzeBracketLeerflaecheZurueck}), damit der HTML-Web-Export
	 * sie nicht als {@code background-color:#FFFFFF} rendert. Deckendes Weiß wäre also ein Fehler.
	 */
	private void validiereFinalrundeLeerflaechenSindTransparent(String gruppenBuchstabe) {
		XSpreadsheet sheet = sheetHlp.findByName(SheetNamen.koFinaleGruppe(gruppenBuchstabe));
		assertThat(sheet).as("Finalgruppe-Sheet '%s' muss existieren", gruppenBuchstabe).isNotNull();

		assertThat(cellBackColor(sheet, 0, 5)).as("1/8-Finale Leerflaeche A6").isEqualTo(TRANSPARENT);
		assertThat(cellBackColor(sheet, 2, 5)).as("1/8-Finale Verbinder-Leerflaeche C6").isEqualTo(TRANSPARENT);
		assertThat(cellBackColor(sheet, 3, 3)).as("1/4-Finale Leerflaeche D4").isEqualTo(TRANSPARENT);
		assertThat(cellBackColor(sheet, 5, 3)).as("1/4-Finale Verbinder-Leerflaeche F4").isEqualTo(TRANSPARENT);
	}

	private void validiereFinalrundenBleibenNachTabWechselUnveraendert(String... gruppenBuchstaben) {
		for (String gruppenBuchstabe : gruppenBuchstaben) {
			XSpreadsheet sheet = sheetHlp.findByName(SheetNamen.koFinaleGruppe(gruppenBuchstabe));
			assertThat(sheet).as("Finalgruppe-Sheet '%s' muss existieren", gruppenBuchstabe).isNotNull();
			sheetHlp.setActiveSheet(sheet);
			wartenAufRunnerFertig(30_000);
			validiereFinalrundeLeerflaechenSindTransparent(gruppenBuchstabe);
		}
	}

	/**
	 * Neues Verhalten (ersetzt den früheren Signatur-Skip): Sind bereits KO-Finalrunden
	 * vorhanden, fragt {@link MaastrichterFinalrundeSheet} per WARN_YES_NO nach, ob alle
	 * bestehenden Finalrunden gelöscht und neu erstellt werden sollen. Im Headless-Test
	 * liefert die MessageBox automatisch YES ({@code MessageBox.setDialogeUeberspringen}),
	 * d.h. die vorhandenen Finale-Sheets werden gelöscht und neu aufgebaut. Ein zuvor in
	 * eine Zelle geschriebener Marker darf danach nicht mehr vorhanden sein.
	 */
	private void validiereFinalrundeNeuAufbauNachBestaetigung(String gruppenBuchstabe)
			throws GenerateException {
		XSpreadsheet sheet = sheetHlp.findByName(SheetNamen.koFinaleGruppe(gruppenBuchstabe));
		assertThat(sheet).as("Finalgruppe-Sheet '%s' muss existieren", gruppenBuchstabe).isNotNull();

		setCellText(sheet, 30, 30, "PTM_REFRESH_MARKER");
		new MaastrichterFinalrundeSheet(wkingSpreadsheet).doRun();

		XSpreadsheet sheetNachUpdate = sheetHlp.findByName(SheetNamen.koFinaleGruppe(gruppenBuchstabe));
		assertThat(sheetNachUpdate)
				.as("KO-Finalrunde muss nach bestätigtem Neuaufbau weiterhin existieren")
				.isNotNull();
		assertThat(cellText(sheetNachUpdate, 30, 30))
				.as("Bestätigter Neuaufbau muss das alte Finale-Sheet löschen und neu erstellen")
				.isEmpty();
		validiereFinalrundeLeerflaechenSindTransparent(gruppenBuchstabe);
	}

	private void wartenAufRunnerFertig(long timeoutMs) {
		long deadline = System.currentTimeMillis() + timeoutMs;
		try {
			Thread.sleep(50);
			while (SheetRunner.isRunning() && System.currentTimeMillis() < deadline) {
				Thread.sleep(50);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new AssertionError("Warten auf SheetRunner wurde unterbrochen", e);
		}
		assertThat(SheetRunner.isRunning())
				.as("SheetRunner muss innerhalb von %d ms fertig werden", timeoutMs)
				.isFalse();
	}

	private int cellBackColor(XSpreadsheet sheet, int spalte, int zeile) {
		try {
			XPropertySet props = UnoRuntime.queryInterface(XPropertySet.class, sheet.getCellByPosition(spalte, zeile));
			return (Integer) props.getPropertyValue("CellBackColor");
		} catch (Exception e) {
			throw new AssertionError("CellBackColor konnte nicht gelesen werden", e);
		}
	}

	private void assertDoppelteRechteLinie(XSpreadsheet sheet, int zeile) throws Exception {
		XPropertySet props = UnoRuntime.queryInterface(XPropertySet.class,
				sheet.getCellByPosition(SchweizerAbstractSpielrundeSheet.TEAM_B_SPALTE, zeile));
		TableBorder2 border = (TableBorder2) props.getPropertyValue("TableBorder2");

		assertThat(border.IsRightLineValid)
				.as("rechte Linie von Spalte B in Zeile %d muss gesetzt sein", zeile)
				.isTrue();
		assertThat(border.RightLine.LineStyle)
				.as("rechte Linie von Spalte B in Zeile %d muss doppelt sein", zeile)
				.isEqualTo(BorderLineStyle.DOUBLE_THIN);
	}

	private void setCellText(XSpreadsheet sheet, int spalte, int zeile, String text) {
		try {
			XText xText = UnoRuntime.queryInterface(XText.class, sheet.getCellByPosition(spalte, zeile));
			xText.setString(text);
		} catch (Exception e) {
			throw new AssertionError("Zelltext konnte nicht geschrieben werden", e);
		}
	}

	private String cellText(XSpreadsheet sheet, int spalte, int zeile) {
		try {
			XText xText = UnoRuntime.queryInterface(XText.class, sheet.getCellByPosition(spalte, zeile));
			return xText.getString();
		} catch (Exception e) {
			throw new AssertionError("Zelltext konnte nicht gelesen werden", e);
		}
	}

	/**
	 * Regression im Kiosk-Modus: nach Vollaufbau (12 Teams, 3 Vorrunden, A-Finale) muss ein
	 * erneutes {@link MaastrichterVorrundenRanglisteSheetUpdate#doRun()} unter aktivem
	 * TurnierModus + Maastrichter-Blattschutz sauber durchlaufen.
	 */
	@Test
	public void kioskModus_vorrundenRanglisteUpdateUnterSchutz() throws GenerateException {
		new MaastrichterTurnierTestDaten(wkingSpreadsheet).generate();
		mitKioskModus(TurnierSystem.MAASTRICHTER, () ->
				new MaastrichterVorrundenRanglisteSheetUpdate(wkingSpreadsheet).doRun());

		assertThat(sheetHlp.findByName(SheetNamen.maastrichterVorrundenRangliste()))
				.as("Vorrunden-Rangliste muss nach Kiosk-Update weiterhin existieren")
				.isNotNull();
	}
}
