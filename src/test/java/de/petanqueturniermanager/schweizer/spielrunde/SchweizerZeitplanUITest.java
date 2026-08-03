package de.petanqueturniermanager.schweizer.spielrunde;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerPropertiesSpalte;
import de.petanqueturniermanager.schweizer.konfiguration.SpielplanTeamAnzeige;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetTestDaten;
import de.petanqueturniermanager.schweizer.rangliste.SchweizerRanglisteSheet;

/**
 * UI-Tests für die optionale Rundenzeitplanung (Turnier-Startzeit, Rundenpause,
 * Durchgang-Aufteilung) im Schweizer System, siehe Plan
 * "Schweizer/Maastrichter Spielrunde: Rundenzeitplanung mit optionaler Durchgang-Aufteilung".
 */
public class SchweizerZeitplanUITest extends BaseCalcUITest {

	private static final int ANZ_TEAMS = 16; // Triplette -> 8 Paarungen pro Runde
	private static final int BAHNEN = 3; // 8 Paarungen / 3 Bahnen -> Bloecke [3, 3, 2]

	private SchweizerTurnierTestDaten testDaten;

	private RangeData ladeZeitSpalte(XSpreadsheet sheet, int anzZeilen) throws GenerateException {
		RangePosition range = RangePosition.from(
				SchweizerAbstractSpielrundeSheet.ZEIT_SPALTE, SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE,
				SchweizerAbstractSpielrundeSheet.ZEIT_SPALTE,
				SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE + anzZeilen - 1);
		return RangeHelper.from(sheet, wkingSpreadsheet.getWorkingSpreadsheetDocument(), range).getDataFromRange();
	}

	/** Liest {@code TableBorder2} der Zeile ab {@code BAHN_NR_SPALTE} (fuer die Durchgang-Trennlinien-Pruefung). */
	private com.sun.star.table.TableBorder2 ladeZeilenOberrand(XSpreadsheet sheet, int zeile) throws GenerateException {
		try {
			RangePosition zeilenRange = RangePosition.from(SchweizerAbstractSpielrundeSheet.BAHN_NR_SPALTE, zeile,
					SchweizerAbstractSpielrundeSheet.FEHLER_SPALTE, zeile);
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
				SchweizerAbstractSpielrundeSheet.BAHN_NR_SPALTE, SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE,
				SchweizerAbstractSpielrundeSheet.BAHN_NR_SPALTE,
				SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE + anzZeilen - 1);
		return RangeHelper.from(sheet, wkingSpreadsheet.getWorkingSpreadsheetDocument(), range).getDataFromRange();
	}

	/** Traegt fuer die ersten {@code anzPaarungen} Datenzeilen ein eindeutiges 13:5-Ergebnis ein. */
	private void ergebnisseEintragen(XSpreadsheet sheet, int anzPaarungen) {
		for (int i = 0; i < anzPaarungen; i++) {
			int zeile = SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE + i;
			sheetHlp.setNumberValueInCell(
					NumberCellValue.from(sheet, Position.from(SchweizerAbstractSpielrundeSheet.ERG_TEAM_A_SPALTE, zeile)).setValue(13));
			sheetHlp.setNumberValueInCell(
					NumberCellValue.from(sheet, Position.from(SchweizerAbstractSpielrundeSheet.ERG_TEAM_B_SPALTE, zeile)).setValue(5));
		}
	}

	/**
	 * Szenario 1 (Plan-Verifikation Punkt 1): Feature deaktiviert (Default) — das Sheet-Layout
	 * muss exakt dem bisherigen Verhalten entsprechen. Keine Durchgang-Spalten, keine
	 * Rundenstartzeit-Zelle.
	 */
	@Test
	public void featureAus_KeineDurchgangSpaltenUndKeineRundenstartzeit() throws GenerateException {
		testDaten = new SchweizerTurnierTestDaten(wkingSpreadsheet, ANZ_TEAMS, SpielplanTeamAnzeige.NR);
		assertThat(testDaten.naechsteSpielrunde.getKonfigurationSheet().isZeitplanAktiv())
				.as("Default muss Zeitplanung deaktiviert sein").isFalse();

		testDaten.generate(1, false);

		XSpreadsheet runde1 = sheetHlp.findByName("1. " + SchweizerAbstractSpielrundeSheet.SHEET_NAMEN);
		assertThat(runde1).isNotNull();

		RangeData zeitSpalte = ladeZeitSpalte(runde1, ANZ_TEAMS / 2);
		assertThat(zeitSpalte).allSatisfy(row ->
				assertThat(row.get(0).getStringVal()).as("ZEIT_SPALTE muss leer sein").isNullOrEmpty());

		String startzeitLabel = sheetHlp.getTextFromCell(runde1,
				Position.from(SchweizerAbstractSpielrundeSheet.FEHLER_SPALTE, SchweizerAbstractSpielrundeSheet.ERSTE_HEADER_ZEILE));
		assertThat(startzeitLabel).as("Kein 'Start'-Label ohne aktives Feature").isNullOrEmpty();
	}

	/**
	 * Szenario 2 (Plan-Verifikation Punkt 2): Feature aktiv, mehr Paarungen als Bahnen —
	 * erwartete Durchgang-Anzahl/-Groessen, Label-Inhalte, pro-Durchgang neu beginnende Bahn-Nr,
	 * Rundenstartzeit korrekt aus der Turnier-Startzeit abgeleitet.
	 */
	@Test
	public void featureAn_MehrPaarungenAlsBahnen_ErzeugtErwarteteDurchgaenge() throws GenerateException {
		// Meldeliste + Runde direkt erzeugen (nicht ueber SchweizerTurnierTestDaten.generate(),
		// das intern SpielrundeSpielbahn.R erzwingt — fuer die Bahn-Nr-Restart-Pruefung wird
		// hier bewusst Modus N (durchnummerieren) verwendet).
		new SchweizerMeldeListeSheetTestDaten(wkingSpreadsheet, ANZ_TEAMS).doRun();
		SchweizerSpielrundeSheetNaechste spielrundeNaechste = new SchweizerSpielrundeSheetNaechste(wkingSpreadsheet);
		var konfig = spielrundeNaechste.getKonfigurationSheet();
		konfig.setSpielplanTeamAnzeige(SpielplanTeamAnzeige.NR);
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
				Position.from(SchweizerAbstractSpielrundeSheet.FEHLER_SPALTE, SchweizerAbstractSpielrundeSheet.ZWEITE_HEADER_ZEILE));
		assertThat(rundenStartzeit).as("Rundenstartzeit Runde 1 muss der Turnier-Startzeit entsprechen").isEqualTo("09:00");

		// 8 Paarungen / 3 Bahnen -> Bloecke [3, 3, 2] (relative Zeilen 0-2 | 3-5 | 6-7).
		// ZEIT_SPALTE: Start in der ersten, Ende in der letzten Zeile jedes Blocks.
		RangeData zeitSpalte = ladeZeitSpalte(runde1, ANZ_TEAMS / 2);
		for (int i : new int[] { 0, 2, 3, 5, 6, 7 }) {
			assertThat(zeitSpalte.get(i).get(0).getStringVal())
					.as("Zeile %d ist Start oder Ende eines Durchgangs", i).isNotBlank();
		}
		// mittlere Zeilen der Bloecke 1 und 2: weder Start noch Ende
		for (int i : new int[] { 1, 4 }) {
			assertThat(zeitSpalte.get(i).get(0).getStringVal())
					.as("Zeile %d ist weder Start noch Ende eines Durchgangs", i).isNullOrEmpty();
		}

		// Optische Trennung statt Text-Label: doppelte Linie am oberen Rand der Durchgang-2/3-Startzeilen,
		// keine Trennlinie vor dem ersten Durchgang (Zeile 0, direkt unter dem Header).
		assertThat(ladeZeilenOberrand(runde1, SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE).TopLine.LineStyle)
				.as("Keine Trennlinie vor dem ersten Durchgang").isNotEqualTo(com.sun.star.table.BorderLineStyle.DOUBLE);
		for (int relZeile : new int[] { 3, 6 }) {
			com.sun.star.table.TableBorder2 oberrand = ladeZeilenOberrand(runde1,
					SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE + relZeile);
			assertThat(oberrand.IsTopLineValid).as("Trennlinie Durchgang-Beginn Zeile %d muss gesetzt sein", relZeile).isTrue();
			assertThat(oberrand.TopLine.LineStyle)
					.as("Trennlinie Durchgang-Beginn Zeile %d muss doppelt sein", relZeile)
					.isEqualTo(com.sun.star.table.BorderLineStyle.DOUBLE_THIN);
		}

		// Start Durchgang 1: reine Zellbezug-Formel auf die Rundenstartzeit, kein Add-in-Aufruf noetig ->
		// Wert direkt pruefbar (formatierten Anzeigetext lesen, nicht CellData.getStringVal(), die
		// Number-Werte auf Int rundet, siehe getIntVal()).
		assertThat(sheetHlp.getTextFromCell(runde1,
				Position.from(SchweizerAbstractSpielrundeSheet.ZEIT_SPALTE, SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE)))
				.isEqualTo("09:00");

		// Alle Folgezellen (Ende/Start ab Durchgang 1-Ende) referenzieren PTM.ALG.INTPROPERTY (live
		// Konfig-Wert). Die berechnete Anzeige haengt von der LO-"aktuelles Dokument"-Erkennung des
		// Add-ins ab, die im automatisierten, nicht-interaktiven UITest-Kontext nicht zuverlaessig auf
		// das Testdokument zeigt (siehe Recherche zu Review-Nachbesserung) — deshalb wird hier die
		// Formel-STRUKTUR geprueft (korrekte Zellreferenz auf die jeweils vorherige Zeit-Zelle,
		// korrekte Property-Keys), nicht der kalkulierte Anzeigewert.
		Position startDurchgang1 = Position.from(SchweizerAbstractSpielrundeSheet.ZEIT_SPALTE, SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE);
		Position endeDurchgang1 = Position.from(SchweizerAbstractSpielrundeSheet.ZEIT_SPALTE, SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE + 2);
		Position startDurchgang2 = Position.from(SchweizerAbstractSpielrundeSheet.ZEIT_SPALTE, SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE + 3);
		Position endeDurchgang2 = Position.from(SchweizerAbstractSpielrundeSheet.ZEIT_SPALTE, SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE + 5);
		Position startDurchgang3 = Position.from(SchweizerAbstractSpielrundeSheet.ZEIT_SPALTE, SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE + 6);
		Position endeDurchgang3 = Position.from(SchweizerAbstractSpielrundeSheet.ZEIT_SPALTE, SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE + 7);

		// Ende = Start desselben Durchgangs + Zeitlimit
		assertThat(sheetHlp.getFormulaFromCell(runde1, endeDurchgang1))
				.as("Ende Durchgang 1 muss auf dessen eigenen Start verweisen")
				.contains(startDurchgang1.getAddress()).contains("Durchgang Zeitlimit (Minuten)");
		assertThat(sheetHlp.getFormulaFromCell(runde1, endeDurchgang2))
				.as("Ende Durchgang 2 muss auf dessen eigenen Start verweisen")
				.contains(startDurchgang2.getAddress()).contains("Durchgang Zeitlimit (Minuten)");
		assertThat(sheetHlp.getFormulaFromCell(runde1, endeDurchgang3))
				.as("Ende Durchgang 3 muss auf dessen eigenen Start verweisen")
				.contains(startDurchgang3.getAddress()).contains("Durchgang Zeitlimit (Minuten)");

		// Start eines Folge-Durchgangs = Ende des vorherigen Durchgangs + Durchgang-Pause (Fix:
		// direkte Verkettung an die tatsaechliche Vorgaenger-Endzeit statt rekonstruiertem Faktor N-1).
		assertThat(sheetHlp.getFormulaFromCell(runde1, startDurchgang2))
				.as("Start Durchgang 2 muss auf das Ende von Durchgang 1 verweisen")
				.contains(endeDurchgang1.getAddress()).contains("Durchgang Pause (Minuten)");
		assertThat(sheetHlp.getFormulaFromCell(runde1, startDurchgang3))
				.as("Start Durchgang 3 muss auf das Ende von Durchgang 2 verweisen")
				.contains(endeDurchgang2.getAddress()).contains("Durchgang Pause (Minuten)");

		// alle anderen Zeilen: keine Zeit-Formel
		for (int i : new int[] { 1, 4 }) {
			assertThat(sheetHlp.getTextFromCell(runde1,
					Position.from(SchweizerAbstractSpielrundeSheet.ZEIT_SPALTE, SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE + i)))
					.as("Zeile %d ist weder Start noch Ende eines Durchgangs", i).isNullOrEmpty();
		}

		// Bahn-Nr beginnt in jedem Durchgang neu bei 1
		RangeData bahnNrn = ladeBahnNummern(runde1, ANZ_TEAMS / 2);
		int[] erwarteteBahnNr = { 1, 2, 3, 1, 2, 3, 1, 2 };
		for (int i = 0; i < erwarteteBahnNr.length; i++) {
			assertThat(bahnNrn.get(i).get(0).getIntVal(-1))
					.as("Bahn-Nr Zeile %d", i).isEqualTo(erwarteteBahnNr[i]);
		}

		// Regressionstest: Duplikat-Pruefung (bedingte Formatierung, rot bei doppelter Bahn-Nr)
		// darf nur INNERHALB eines Durchgangs pruefen, nicht ueber die gesamte Spalte — sonst
		// waeren die legitim wiederkehrenden Nummern 1/2/3 der Folge-Durchgaenge faelschlich rot.
		String cfFormelDurchgang1 = ladeErsteConditionalFormatFormel(runde1,
				Position.from(SchweizerAbstractSpielrundeSheet.BAHN_NR_SPALTE, SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE));
		assertThat(cfFormelDurchgang1)
				.as("Duplikat-Pruefung Durchgang 1 muss auf die Bloecke skaliert sein (Zeilen 3-5), nicht auf die ganze Spalte")
				.contains("$A$3:$A$5");

		String cfFormelDurchgang2 = ladeErsteConditionalFormatFormel(runde1,
				Position.from(SchweizerAbstractSpielrundeSheet.BAHN_NR_SPALTE, SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE + 3));
		assertThat(cfFormelDurchgang2)
				.as("Duplikat-Pruefung Durchgang 2 muss auf dessen eigenen Block skaliert sein (Zeilen 6-8)")
				.contains("$A$6:$A$8");
	}

	/** Liest die Formula1 der ersten bedingten Formatierung einer Zelle (fuer COUNTIF-Duplikat-Pruefungen). */
	private String ladeErsteConditionalFormatFormel(XSpreadsheet sheet, Position pos) throws GenerateException {
		try {
			com.sun.star.table.XCell xCell = sheet.getCellByPosition(pos.getSpalte(), pos.getZeile());
			com.sun.star.beans.XPropertySet xPropSet = de.petanqueturniermanager.helper.Lo.qi(com.sun.star.beans.XPropertySet.class, xCell);
			com.sun.star.sheet.XSheetConditionalEntries xEntries = de.petanqueturniermanager.helper.Lo.qi(
					com.sun.star.sheet.XSheetConditionalEntries.class, xPropSet.getPropertyValue("ConditionalFormat"));
			assertThat(xEntries.getCount()).as("Zelle %s muss eine bedingte Formatierung haben", pos.getAddress()).isGreaterThan(0);
			com.sun.star.sheet.XSheetCondition xCondition = de.petanqueturniermanager.helper.Lo.qi(
					com.sun.star.sheet.XSheetCondition.class, xEntries.getByIndex(0));
			return xCondition.getFormula1();
		} catch (Exception e) {
			throw new GenerateException(e.getMessage());
		}
	}

	/**
	 * Regressionstest: einzeiliger letzter Durchgang-Block (Start- und Endzeile identisch). Die
	 * Endzeit-Formel muss die zuvor geschriebene Startzeit-Formel in derselben Zelle ueberschreiben
	 * ("Nix mischen" — die Zelle enthaelt am Ende ausschliesslich die numerische Ende-Formel, kein
	 * kombinierter Text), damit die Zelle als Verkettungs-Anker fuer Folge-Durchgang/-Runde nutzbar
	 * bleibt (siehe {@link SchweizerAbstractSpielrundeSheet#ermittleLetzteZeitZeile}).
	 */
	@Test
	public void featureAn_EinzeiligerLetzterDurchgang_EndzeitUeberschreibtStartzeitInDerselbenZelle() throws GenerateException {
		int anzTeams = 14; // 7 Paarungen, 3 Bahnen -> Bloecke [3, 3, 1]
		new SchweizerMeldeListeSheetTestDaten(wkingSpreadsheet, anzTeams).doRun();
		SchweizerSpielrundeSheetNaechste spielrundeNaechste = new SchweizerSpielrundeSheetNaechste(wkingSpreadsheet);
		var konfig = spielrundeNaechste.getKonfigurationSheet();
		konfig.setSpielplanTeamAnzeige(SpielplanTeamAnzeige.NR);
		konfig.setSpielrundeSpielbahn(SpielrundeSpielbahn.N);
		konfig.setZeitplanAktiv(true);
		konfig.setZeitplanAnzahlBahnen(BAHNEN);
		konfig.setZeitplanTurnierStartzeit("09:00");
		spielrundeNaechste.doRun();

		XSpreadsheet runde1 = spielrundeNaechste.getXSpreadSheet();
		assertThat(runde1).isNotNull();

		// Bugfix-Regression: Zeitlimit/Pause wurden hier NIE explizit gesetzt (nur setZeitplanAktiv
		// und setZeitplanAnzahlBahnen) — readIntProperty() muss deren Konfig-Default trotzdem in die
		// UserDefinedProperties des Dokuments persistieren, sonst liefert das live referenzierte
		// PTM.ALG.INTPROPERTY(...) in den Formeln 0 statt 15/5/10 (sichtbarer Bug: Endzeit == Startzeit).
		String sentinel = "SENTINEL_NICHT_PERSISTIERT";
		DocumentPropertiesHelper docProps = new DocumentPropertiesHelper(wkingSpreadsheet);
		assertThat(docProps.getStringProperty(SchweizerPropertiesSpalte.KONFIG_PROP_ZEITPLAN_ZEITLIMIT_MINUTEN, sentinel))
				.as("Zeitlimit-Default muss ohne expliziten Setter-Aufruf im Dokument persistiert sein").isNotEqualTo(sentinel);
		assertThat(docProps.getStringProperty(SchweizerPropertiesSpalte.KONFIG_PROP_ZEITPLAN_DURCHGANG_PAUSE_MINUTEN, sentinel))
				.as("Durchgang-Pause-Default muss ohne expliziten Setter-Aufruf im Dokument persistiert sein").isNotEqualTo(sentinel);
		assertThat(docProps.getStringProperty(SchweizerPropertiesSpalte.KONFIG_PROP_ZEITPLAN_RUNDEN_PAUSE_MINUTEN, sentinel))
				.as("Runden-Pause-Default muss ohne expliziten Setter-Aufruf im Dokument persistiert sein").isNotEqualTo(sentinel);

		// letzter Block [6] (relative Zeile 6, 0-indiziert): Start- und Endzeile identisch.
		Position letzteZeile = Position.from(SchweizerAbstractSpielrundeSheet.ZEIT_SPALTE,
				SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE + 6);
		Position endeVorherigerBlock = Position.from(SchweizerAbstractSpielrundeSheet.ZEIT_SPALTE,
				SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE + 5);
		String formel = sheetHlp.getFormulaFromCell(runde1, letzteZeile);
		assertThat(formel).as("Einzeiliger Block: Zelle muss die Ende-Formel enthalten (verweist auf sich selbst + Zeitlimit)")
				.contains(letzteZeile.getAddress()).contains("Durchgang Zeitlimit (Minuten)");
		assertThat(formel).as("darf NICHT die urspruengliche Start-Formel (Verweis auf Ende des Vorgaenger-Blocks) sein")
				.doesNotContain(endeVorherigerBlock.getAddress());
	}

	/**
	 * Szenario 3 (Plan-Verifikation Punkt 3, kritischer Regressionstest zu Review-Finding 1):
	 * Runde mit aktiver Durchgang-Aufteilung wird ueber alle Durchgaenge hinweg mit Ergebnissen
	 * gespielt. leseRundeEin()/gespieltenRundenEinlesen() darf NICHT beim ersten Durchgang
	 * abbrechen — die komplette Rangliste (Siegesumme) muss alle 8 Paarungen widerspiegeln.
	 */
	@Test
	public void featureAn_NaechsteRundeListAlleErgebnisseTrotzDurchgangAufteilungKorrektEin() throws GenerateException {
		testDaten = new SchweizerTurnierTestDaten(wkingSpreadsheet, ANZ_TEAMS, SpielplanTeamAnzeige.NR);
		var konfig = testDaten.naechsteSpielrunde.getKonfigurationSheet();
		konfig.setZeitplanAktiv(true);
		konfig.setZeitplanAnzahlBahnen(BAHNEN);

		// generate() füllt selbst alle Paarungen mit Zufallsergebnissen (auch über mehrere
		// Durchgänge hinweg) und baut danach die Rangliste auf.
		testDaten.generate(1, true);

		XSpreadsheet rangliste = sheetHlp.findByName(
				de.petanqueturniermanager.helper.i18n.SheetNamen.rangliste());
		assertThat(rangliste).isNotNull();

		RangePosition ranglisteRange = RangePosition.from(
				SchweizerRanglisteSheet.TEAM_NR_SPALTE, SchweizerRanglisteSheet.ERSTE_DATEN_ZEILE,
				SchweizerRanglisteSheet.SIEGE_SPALTE, SchweizerRanglisteSheet.ERSTE_DATEN_ZEILE + ANZ_TEAMS - 1);
		RangeData ranglisteDaten = RangeHelper
				.from(rangliste, wkingSpreadsheet.getWorkingSpreadsheetDocument(), ranglisteRange).getDataFromRange();

		int siegeSumme = ranglisteDaten.stream()
				.mapToInt(row -> row.get(SchweizerRanglisteSheet.SIEGE_SPALTE - SchweizerRanglisteSheet.TEAM_NR_SPALTE)
						.getIntVal(0))
				.sum();

		// 8 Paarungen, jede mit eindeutigem Sieger (loserPts 0-12 < 13) -> genau 8 Siege gesamt.
		// Waere leseRundeEin() beim ersten Durchgang abgebrochen, kaemen nur die ersten 3
		// Paarungen (Bloecke [3,3,2]) in die Statistik -> Siegesumme waere 3 statt 8.
		assertThat(siegeSumme)
				.as("Alle 8 Paarungen (ueber alle Durchgaenge hinweg) muessen in die Rangliste einfliessen")
				.isEqualTo(ANZ_TEAMS / 2);
	}

	/**
	 * Szenario 4 (Plan-Verifikation Punkt 4): NAME-Modus und Freilos bleiben bei aktivem
	 * Feature intakt (Formeln/Formatierung der neuen Spalten stoeren TEAM_A/B-SVERWEIS-Formeln
	 * und Freilos-Vorbelegung nicht).
	 */
	@Test
	public void featureAn_NameModusUndFreilosBleibenIntakt() throws GenerateException {
		int anzTeamsUngerade = ANZ_TEAMS + 1; // erzwingt genau ein Freilos
		testDaten = new SchweizerTurnierTestDaten(wkingSpreadsheet, anzTeamsUngerade, SpielplanTeamAnzeige.NAME);
		var konfig = testDaten.naechsteSpielrunde.getKonfigurationSheet();
		konfig.setZeitplanAktiv(true);
		konfig.setZeitplanAnzahlBahnen(BAHNEN);

		testDaten.generate(1, false);
		testDaten.naechsteSpielrunde.getxCalculatable().calculateAll();

		XSpreadsheet runde1 = sheetHlp.findByName("1. " + SchweizerAbstractSpielrundeSheet.SHEET_NAMEN);
		assertThat(runde1).isNotNull();

		int freispielPlus = konfig.getFreispielPunktePlus();
		int freispielMinus = konfig.getFreispielPunkteMinus();

		int anzPaarungszeilen = (anzTeamsUngerade + 1) / 2; // inkl. Freilos-Zeile
		RangePosition datenRange = RangePosition.from(
				SchweizerAbstractSpielrundeSheet.TEAM_A_SPALTE, SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE,
				SchweizerAbstractSpielrundeSheet.ERG_TEAM_B_SPALTE,
				SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE + anzPaarungszeilen - 1);
		RangeData daten = RangeHelper.from(runde1, wkingSpreadsheet.getWorkingSpreadsheetDocument(), datenRange)
				.getDataFromRange();

		boolean freilosGefunden = false;
		for (var row : daten) {
			String teamA = row.get(0).getStringVal();
			assertThat(teamA).as("Team-A-Name (SVERWEIS-Formel) darf nicht leer sein").isNotBlank();
			String teamB = row.get(1).getStringVal();
			if (teamB == null || teamB.isBlank()) {
				freilosGefunden = true;
				assertThat(row.get(2).getIntVal(-1)).as("Freilos ERG A = Freispiel+").isEqualTo(freispielPlus);
				assertThat(row.get(3).getIntVal(-1)).as("Freilos ERG B = Freispiel-").isEqualTo(freispielMinus);
			}
		}
		assertThat(freilosGefunden).as("Bei ungerader Teamanzahl muss genau ein Freilos existieren").isTrue();

		// ZEIT_SPALTE trotz NAME-Modus korrekt befuellt (mind. ein Wert vorhanden)
		RangeData zeitSpalte = ladeZeitSpalte(runde1, anzPaarungszeilen);
		boolean mindEineZeit = zeitSpalte.stream().anyMatch(row -> {
			String wert = row.get(0).getStringVal();
			return wert != null && !wert.isBlank();
		});
		assertThat(mindEineZeit).as("Bei mehr Paarungen als Bahnen muss mind. eine Zeit-Zelle geschrieben werden").isTrue();
	}

	/**
	 * Regressionstest (Review-Nachbesserung): Wird die Vorrunde umbenannt, bevor die Folgerunde
	 * erzeugt wird, muss die Rundenstartzeit-Formel der Folgerunde trotzdem auf die (umbenannte)
	 * Vorrunde zeigen — kein {@code #REF!}. Der Sheet-Name im Formel-Bezug muss ueber die
	 * Metadaten-Suche ({@link de.petanqueturniermanager.helper.sheet.SheetMetadataHelper#findeSheetUndHeile})
	 * aufgeloest werden, nicht ueber den lokalisierten Default-Namen.
	 */
	@Test
	public void featureAn_VorrundeUmbenannt_RundenstartzeitFolgerundeZeigtAufUmbenannteVorrunde() throws GenerateException {
		new SchweizerMeldeListeSheetTestDaten(wkingSpreadsheet, ANZ_TEAMS).doRun();
		SchweizerSpielrundeSheetNaechste spielrundeNaechste = new SchweizerSpielrundeSheetNaechste(wkingSpreadsheet);
		var konfig = spielrundeNaechste.getKonfigurationSheet();
		konfig.setSpielplanTeamAnzeige(SpielplanTeamAnzeige.NR);
		konfig.setZeitplanAktiv(true);
		konfig.setZeitplanAnzahlBahnen(BAHNEN);
		konfig.setZeitplanTurnierStartzeit("09:00");
		konfig.setZeitplanRundenPauseMinuten(10);

		spielrundeNaechste.doRun(); // Runde 1

		XSpreadsheet runde1 = sheetHlp.findByName("1. " + SchweizerAbstractSpielrundeSheet.SHEET_NAMEN);
		assertThat(runde1).isNotNull();
		ergebnisseEintragen(runde1, ANZ_TEAMS / 2); // Vorbedingung fuer naechsteSpielrundeEinfuegen()

		String umbenannterName = "Custom Runde 1";
		assertThat(sheetHlp.reNameSheet(runde1, umbenannterName)).as("Umbenennung muss gelingen").isTrue();

		spielrundeNaechste.doRun(); // Runde 2 — muss trotz Umbenennung die richtige Vorrunde finden

		XSpreadsheet runde2 = sheetHlp.findByName("2. " + SchweizerAbstractSpielrundeSheet.SHEET_NAMEN);
		assertThat(runde2).isNotNull();
		spielrundeNaechste.getxCalculatable().calculateAll();

		String formelRunde2 = sheetHlp.getFormulaFromCell(runde2,
				Position.from(SchweizerAbstractSpielrundeSheet.FEHLER_SPALTE, SchweizerAbstractSpielrundeSheet.ZWEITE_HEADER_ZEILE));
		assertThat(formelRunde2)
				.as("Formel muss auf den tatsaechlichen (umbenannten) Sheet-Namen zeigen, nicht auf den Default-Namen")
				.contains("'" + umbenannterName + "'")
				.doesNotContain("'1. " + SchweizerAbstractSpielrundeSheet.SHEET_NAMEN + "'");

		String angezeigterWert = sheetHlp.getTextFromCell(runde2,
				Position.from(SchweizerAbstractSpielrundeSheet.FEHLER_SPALTE, SchweizerAbstractSpielrundeSheet.ZWEITE_HEADER_ZEILE));
		assertThat(angezeigterWert).as("Kein #REF! nach Umbenennung der Vorrunde").doesNotContain("REF").doesNotContain("Fehler");
	}
}
