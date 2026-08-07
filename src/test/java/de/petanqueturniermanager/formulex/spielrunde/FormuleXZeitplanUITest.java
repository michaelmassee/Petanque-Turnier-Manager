/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.formulex.spielrunde;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.formulex.meldeliste.FormuleXMeldeListeSheetTestDaten;
import de.petanqueturniermanager.formulex.rangliste.FormuleXRanglisteSheet;
import de.petanqueturniermanager.formulex.rangliste.FormuleXRanglisteSheetUpdate;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;

/**
 * UI-Tests für die optionale Rundenzeitplanung (Turnier-Startzeit, Rundenpause,
 * Durchgang-Aufteilung) im Formule X System — übertragen aus
 * {@code SchweizerZeitplanUITest} (identisches Feature, siehe
 * Plan "Durchgang-Aufteilung auf Formule_X übertragen").
 */
public class FormuleXZeitplanUITest extends BaseCalcUITest {

	private static final int ANZ_TEAMS = 16; // Triplette -> 8 Paarungen pro Runde
	private static final int BAHNEN = 3; // 8 Paarungen / 3 Bahnen -> Bloecke [3, 3, 2]

	private RangeData ladeZeitSpalte(XSpreadsheet sheet, int anzZeilen) throws GenerateException {
		RangePosition range = RangePosition.from(
				FormuleXAbstractSpielrundeSheet.ZEIT_SPALTE, FormuleXAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE,
				FormuleXAbstractSpielrundeSheet.ZEIT_SPALTE,
				FormuleXAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE + anzZeilen - 1);
		return RangeHelper.from(sheet, wkingSpreadsheet.getWorkingSpreadsheetDocument(), range).getDataFromRange();
	}

	/** Liest {@code TableBorder2} der Zeile ab {@code BAHN_NR_SPALTE} (fuer die Durchgang-Trennlinien-Pruefung). */
	private com.sun.star.table.TableBorder2 ladeZeilenOberrand(XSpreadsheet sheet, int zeile) throws GenerateException {
		try {
			RangePosition zeilenRange = RangePosition.from(FormuleXAbstractSpielrundeSheet.BAHN_NR_SPALTE, zeile,
					FormuleXAbstractSpielrundeSheet.ZEIT_SPALTE, zeile);
			com.sun.star.table.XCellRange xRange = sheet.getCellRangeByPosition(zeilenRange.getStartSpalte(),
					zeilenRange.getStartZeile(), zeilenRange.getEndeSpalte(), zeilenRange.getEndeZeile());
			com.sun.star.beans.XPropertySet xPropSet = de.petanqueturniermanager.helper.Lo.qi(com.sun.star.beans.XPropertySet.class, xRange);
			return (com.sun.star.table.TableBorder2) xPropSet.getPropertyValue("TableBorder2");
		} catch (Exception e) {
			throw new GenerateException(e.getMessage());
		}
	}

	private RangeData ladeBahnNummern(XSpreadsheet sheet, int anzZeilen) throws GenerateException {
		RangePosition range = RangePosition.from(
				FormuleXAbstractSpielrundeSheet.BAHN_NR_SPALTE, FormuleXAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE,
				FormuleXAbstractSpielrundeSheet.BAHN_NR_SPALTE,
				FormuleXAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE + anzZeilen - 1);
		return RangeHelper.from(sheet, wkingSpreadsheet.getWorkingSpreadsheetDocument(), range).getDataFromRange();
	}

	/** Traegt fuer die ersten {@code anzPaarungen} Datenzeilen ein eindeutiges 13:5-Ergebnis ein. */
	private void ergebnisseEintragen(XSpreadsheet sheet, int anzPaarungen) {
		for (int i = 0; i < anzPaarungen; i++) {
			int zeile = FormuleXAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE + i;
			sheetHlp.setNumberValueInCell(
					NumberCellValue.from(sheet, Position.from(FormuleXAbstractSpielrundeSheet.ERG_TEAM_A_SPALTE, zeile)).setValue(13));
			sheetHlp.setNumberValueInCell(
					NumberCellValue.from(sheet, Position.from(FormuleXAbstractSpielrundeSheet.ERG_TEAM_B_SPALTE, zeile)).setValue(5));
		}
	}

	/**
	 * Feature deaktiviert (Default) — das Sheet-Layout muss exakt dem bisherigen Verhalten
	 * entsprechen. Keine Durchgang-Spalten, keine Rundenstartzeit-Zelle.
	 */
	@Test
	public void featureAus_KeineDurchgangSpaltenUndKeineRundenstartzeit() throws Exception {
		new FormuleXMeldeListeSheetTestDaten(wkingSpreadsheet, ANZ_TEAMS).erstelleMeldelisteWithTestdaten();
		FormuleXSpielrundeSheetNaechste spielrundeNaechste = new FormuleXSpielrundeSheetNaechste(wkingSpreadsheet);
		assertThat(spielrundeNaechste.getKonfigurationSheet().isZeitplanAktiv())
				.as("Default muss Zeitplanung deaktiviert sein").isFalse();

		spielrundeNaechste.doRun();

		XSpreadsheet runde1 = spielrundeNaechste.getXSpreadSheet();
		assertThat(runde1).isNotNull();

		RangeData zeitSpalte = ladeZeitSpalte(runde1, ANZ_TEAMS / 2);
		assertThat(zeitSpalte).allSatisfy(row ->
				assertThat(row.get(0).getStringVal()).as("ZEIT_SPALTE muss leer sein").isNullOrEmpty());
	}

	/**
	 * Feature aktiv, mehr Paarungen als Bahnen — erwartete Durchgang-Anzahl/-Groessen,
	 * pro-Durchgang neu beginnende Bahn-Nr, Rundenstartzeit korrekt aus der Turnier-Startzeit
	 * abgeleitet, Trennlinien zwischen den Durchgaengen.
	 */
	@Test
	public void featureAn_MehrPaarungenAlsBahnen_ErzeugtErwarteteDurchgaenge() throws GenerateException {
		new FormuleXMeldeListeSheetTestDaten(wkingSpreadsheet, ANZ_TEAMS).erstelleMeldelisteWithTestdaten();
		FormuleXSpielrundeSheetNaechste spielrundeNaechste = new FormuleXSpielrundeSheetNaechste(wkingSpreadsheet);
		var konfig = spielrundeNaechste.getKonfigurationSheet();
		konfig.setSpielrundeSpielbahn(SpielrundeSpielbahn.N);
		konfig.setZeitplanAktiv(true);
		konfig.setZeitplanAnzahlBahnen(BAHNEN);
		konfig.setZeitplanZeitlimitMinuten(15);
		konfig.setZeitplanDurchgangPauseMinuten(5);
		konfig.setZeitplanTurnierStartzeit("09:00");
		spielrundeNaechste.doRun();

		XSpreadsheet runde1 = spielrundeNaechste.getXSpreadSheet();
		assertThat(runde1).isNotNull();
		spielrundeNaechste.getxCalculatable().calculateAll();

		// Rundenstartzeit (Runde 1) = Turnier-Startzeit
		String rundenStartzeit = sheetHlp.getTextFromCell(runde1,
				Position.from(FormuleXAbstractSpielrundeSheet.ZEIT_SPALTE, FormuleXAbstractSpielrundeSheet.ZWEITE_HEADER_ZEILE));
		assertThat(rundenStartzeit).as("Rundenstartzeit Runde 1 muss der Turnier-Startzeit entsprechen").isEqualTo("09:00");

		// 8 Paarungen / 3 Bahnen -> Bloecke [3, 3, 2] (relative Zeilen 0-2 | 3-5 | 6-7).
		RangeData zeitSpalte = ladeZeitSpalte(runde1, ANZ_TEAMS / 2);
		for (int i : new int[] { 0, 2, 3, 5, 6, 7 }) {
			assertThat(zeitSpalte.get(i).get(0).getStringVal())
					.as("Zeile %d ist Start oder Ende eines Durchgangs", i).isNotBlank();
		}
		for (int i : new int[] { 1, 4 }) {
			assertThat(zeitSpalte.get(i).get(0).getStringVal())
					.as("Zeile %d ist weder Start noch Ende eines Durchgangs", i).isNullOrEmpty();
		}

		// Optische Trennung: doppelte Linie am oberen Rand der Durchgang-2/3-Startzeilen,
		// keine Trennlinie vor dem ersten Durchgang.
		assertThat(ladeZeilenOberrand(runde1, FormuleXAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE).TopLine.LineStyle)
				.as("Keine Trennlinie vor dem ersten Durchgang").isNotEqualTo(com.sun.star.table.BorderLineStyle.DOUBLE);
		for (int relZeile : new int[] { 3, 6 }) {
			com.sun.star.table.TableBorder2 oberrand = ladeZeilenOberrand(runde1,
					FormuleXAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE + relZeile);
			assertThat(oberrand.IsTopLineValid).as("Trennlinie Durchgang-Beginn Zeile %d muss gesetzt sein", relZeile).isTrue();
			assertThat(oberrand.TopLine.LineStyle)
					.as("Trennlinie Durchgang-Beginn Zeile %d muss doppelt sein", relZeile)
					.isEqualTo(com.sun.star.table.BorderLineStyle.DOUBLE_THIN);
		}

		// Bahn-Nr beginnt in jedem Durchgang neu bei 1
		RangeData bahnNrn = ladeBahnNummern(runde1, ANZ_TEAMS / 2);
		int[] erwarteteBahnNr = { 1, 2, 3, 1, 2, 3, 1, 2 };
		for (int i = 0; i < erwarteteBahnNr.length; i++) {
			assertThat(bahnNrn.get(i).get(0).getIntVal(-1))
					.as("Bahn-Nr Zeile %d", i).isEqualTo(erwarteteBahnNr[i]);
		}

		// Start Durchgang 1: reine Zellbezug-Formel auf die Rundenstartzeit -> Wert direkt pruefbar.
		assertThat(sheetHlp.getTextFromCell(runde1,
				Position.from(FormuleXAbstractSpielrundeSheet.ZEIT_SPALTE, FormuleXAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE)))
				.isEqualTo("09:00");

		Position startDurchgang1 = Position.from(FormuleXAbstractSpielrundeSheet.ZEIT_SPALTE, FormuleXAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE);
		Position endeDurchgang1 = Position.from(FormuleXAbstractSpielrundeSheet.ZEIT_SPALTE, FormuleXAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE + 2);
		Position startDurchgang2 = Position.from(FormuleXAbstractSpielrundeSheet.ZEIT_SPALTE, FormuleXAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE + 3);
		String endeDurchgang1Formel = sheetHlp.getFormulaFromCell(runde1, endeDurchgang1);
		assertThat(endeDurchgang1Formel)
				.as("Ende Durchgang 1 muss auf dessen eigenen Start verweisen")
				.contains(startDurchgang1.getAddress()).contains("Durchgang Zeitlimit (Minuten)")
				.contains("TEXT(").endsWith(";\"HH:MM\")");
		assertThat(sheetHlp.getFormulaFromCell(runde1, startDurchgang2))
				.as("Start Durchgang 2 muss auf das Ende von Durchgang 1 verweisen")
				.contains("TIMEVALUE(" + endeDurchgang1.getAddress() + ")").contains("Durchgang Pause (Minuten)");
	}

	/**
	 * Regressionstest (Portierung aus Schweizer): einzeiliger letzter Durchgang-Block (Start- und
	 * Endzeile identisch). Die Ende-Formel referenziert die Endzeit-Zelle des VORHERIGEN Blocks +
	 * Zeitlimit — NICHT sich selbst (Calc-Zirkelbezug).
	 */
	@Test
	public void featureAn_EinzeiligerLetzterDurchgang_EndzeitUeberschreibtStartzeitInDerselbenZelle() throws GenerateException {
		int anzTeams = 14; // 7 Paarungen, 3 Bahnen -> Bloecke [3, 3, 1]
		new FormuleXMeldeListeSheetTestDaten(wkingSpreadsheet, anzTeams).erstelleMeldelisteWithTestdaten();
		FormuleXSpielrundeSheetNaechste spielrundeNaechste = new FormuleXSpielrundeSheetNaechste(wkingSpreadsheet);
		var konfig = spielrundeNaechste.getKonfigurationSheet();
		konfig.setSpielrundeSpielbahn(SpielrundeSpielbahn.N);
		konfig.setZeitplanAktiv(true);
		konfig.setZeitplanAnzahlBahnen(BAHNEN);
		konfig.setZeitplanTurnierStartzeit("09:00");
		spielrundeNaechste.doRun();

		XSpreadsheet runde1 = spielrundeNaechste.getXSpreadSheet();
		assertThat(runde1).isNotNull();

		Position letzteZeile = Position.from(FormuleXAbstractSpielrundeSheet.ZEIT_SPALTE,
				FormuleXAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE + 6);
		Position endeVorherigerBlock = Position.from(FormuleXAbstractSpielrundeSheet.ZEIT_SPALTE,
				FormuleXAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE + 5);
		String formel = sheetHlp.getFormulaFromCell(runde1, letzteZeile);
		assertThat(formel).as("Einzeiliger Block: Ende-Formel muss auf die Endzeit-Zelle des Vorgaenger-Blocks + Zeitlimit verweisen")
				.contains(endeVorherigerBlock.getAddress()).contains("Durchgang Zeitlimit (Minuten)");
		assertThat(formel).as("darf NICHT sich selbst referenzieren (Calc-Zirkelbezug)")
				.doesNotContain(letzteZeile.getAddress());
	}

	/**
	 * Regressionstest: {@code gespieltenRundenEinlesen()} darf nicht beim ersten Durchgang
	 * abbrechen — die komplette Rangliste (Siegesumme) muss alle Paarungen widerspiegeln.
	 */
	@Test
	public void featureAn_RanglisteListAlleErgebnisseTrotzDurchgangAufteilungKorrektEin() throws GenerateException {
		new FormuleXMeldeListeSheetTestDaten(wkingSpreadsheet, ANZ_TEAMS).erstelleMeldelisteWithTestdaten();
		FormuleXSpielrundeSheetNaechste spielrundeNaechste = new FormuleXSpielrundeSheetNaechste(wkingSpreadsheet);
		var konfig = spielrundeNaechste.getKonfigurationSheet();
		konfig.setSpielrundeSpielbahn(SpielrundeSpielbahn.N);
		konfig.setZeitplanAktiv(true);
		konfig.setZeitplanAnzahlBahnen(BAHNEN);
		spielrundeNaechste.doRun();

		XSpreadsheet runde1 = spielrundeNaechste.getXSpreadSheet();
		assertThat(runde1).isNotNull();
		ergebnisseEintragen(runde1, ANZ_TEAMS / 2);

		new FormuleXRanglisteSheetUpdate(wkingSpreadsheet).doRun();

		XSpreadsheet rangliste = sheetHlp.findByName(SheetNamen.formulexRangliste());
		assertThat(rangliste).isNotNull();

		RangePosition ranglisteRange = RangePosition.from(
				FormuleXRanglisteSheet.TEAM_NR_SPALTE, FormuleXRanglisteSheet.ERSTE_DATEN_ZEILE,
				FormuleXRanglisteSheet.SIEGE_SPALTE, FormuleXRanglisteSheet.ERSTE_DATEN_ZEILE + ANZ_TEAMS - 1);
		RangeData ranglisteDaten = RangeHelper
				.from(rangliste, wkingSpreadsheet.getWorkingSpreadsheetDocument(), ranglisteRange).getDataFromRange();

		int siegeSumme = ranglisteDaten.stream()
				.mapToInt(row -> row.get(FormuleXRanglisteSheet.SIEGE_SPALTE - FormuleXRanglisteSheet.TEAM_NR_SPALTE)
						.getIntVal(0))
				.sum();

		// 8 Paarungen, jede mit eindeutigem Sieger -> genau 8 Siege gesamt. Waere leseRundeEin() beim
		// ersten Durchgang abgebrochen, kaemen nur die ersten 3 Paarungen (Bloecke [3,3,2]) in die
		// Statistik -> Siegesumme waere 3 statt 8.
		assertThat(siegeSumme)
				.as("Alle 8 Paarungen (ueber alle Durchgaenge hinweg) muessen in die Rangliste einfliessen")
				.isEqualTo(ANZ_TEAMS / 2);
	}
}
