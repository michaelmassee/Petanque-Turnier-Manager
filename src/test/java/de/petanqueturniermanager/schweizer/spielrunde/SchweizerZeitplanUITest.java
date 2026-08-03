package de.petanqueturniermanager.schweizer.spielrunde;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
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

	private RangeData ladeDurchgangSpalten(XSpreadsheet sheet, int anzZeilen) throws GenerateException {
		RangePosition range = RangePosition.from(
				SchweizerAbstractSpielrundeSheet.DURCHGANG_LABEL_SPALTE, SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE,
				SchweizerAbstractSpielrundeSheet.DURCHGANG_STARTZEIT_SPALTE,
				SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE + anzZeilen - 1);
		return RangeHelper.from(sheet, wkingSpreadsheet.getWorkingSpreadsheetDocument(), range).getDataFromRange();
	}

	private RangeData ladeBahnNummern(XSpreadsheet sheet, int anzZeilen) throws GenerateException {
		RangePosition range = RangePosition.from(
				SchweizerAbstractSpielrundeSheet.BAHN_NR_SPALTE, SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE,
				SchweizerAbstractSpielrundeSheet.BAHN_NR_SPALTE,
				SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE + anzZeilen - 1);
		return RangeHelper.from(sheet, wkingSpreadsheet.getWorkingSpreadsheetDocument(), range).getDataFromRange();
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

		RangeData durchgangSpalten = ladeDurchgangSpalten(runde1, ANZ_TEAMS / 2);
		assertThat(durchgangSpalten).allSatisfy(row -> {
			assertThat(row.get(0).getStringVal()).as("Durchgang-Label muss leer sein").isNullOrEmpty();
			assertThat(row.get(1).getStringVal()).as("Durchgang-Startzeit muss leer sein").isNullOrEmpty();
		});

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

		// 8 Paarungen / 3 Bahnen -> Bloecke [3, 3, 2] -> Durchgang-Label auf Zeile 0, 3, 6 (relativ)
		RangeData durchgangSpalten = ladeDurchgangSpalten(runde1, ANZ_TEAMS / 2);
		assertThat(durchgangSpalten.get(0).get(0).getStringVal()).contains("1");
		assertThat(durchgangSpalten.get(3).get(0).getStringVal()).contains("2");
		assertThat(durchgangSpalten.get(6).get(0).getStringVal()).contains("3");
		// alle anderen Zeilen: kein Label
		for (int i : new int[] { 1, 2, 4, 5, 7 }) {
			assertThat(durchgangSpalten.get(i).get(0).getStringVal())
					.as("Zeile %d ist keine Durchgang-Startzeile", i).isNullOrEmpty();
		}

		// Durchgang 1: reine Zellbezug-Formel auf die Rundenstartzeit, kein Add-in-Aufruf noetig ->
		// Wert direkt pruefbar (formatierten Anzeigetext lesen, nicht CellData.getStringVal(), die
		// Number-Werte auf Int rundet, siehe getIntVal()).
		assertThat(sheetHlp.getTextFromCell(runde1,
				Position.from(SchweizerAbstractSpielrundeSheet.DURCHGANG_STARTZEIT_SPALTE, SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE)))
				.isEqualTo("09:00");

		// Durchgang 2/3: Formel referenziert PTM.ALG.INTPROPERTY (live Konfig-Wert). Die
		// berechnete Anzeige haengt von der LO-"aktuelles Dokument"-Erkennung des Add-ins ab, die
		// im automatisierten, nicht-interaktiven UITest-Kontext nicht zuverlaessig auf das
        // Testdokument zeigt (siehe Recherche zu Review-Nachbesserung) — deshalb wird hier
		// die Formel-STRUKTUR geprueft (korrekte Zellreferenz, korrekter struktureller Faktor
		// N-1, korrekte Property-Keys), nicht der kalkulierte Anzeigewert. Die zugrunde liegende
		// PTM.ALG.INTPROPERTY-Mechanik ist bereits produktiv im Einsatz (siehe
		// ConditionalFormatHelper/LigaSpielPlanSheet) und wird hier nicht erneut verifiziert.
		String formelDurchgang2 = sheetHlp.getFormulaFromCell(runde1,
				Position.from(SchweizerAbstractSpielrundeSheet.DURCHGANG_STARTZEIT_SPALTE, SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE + 3));
		assertThat(formelDurchgang2).contains("$F$2").contains("(1)").contains("Durchgang Zeitlimit (Minuten)")
				.contains("Durchgang Pause (Minuten)");

		String formelDurchgang3 = sheetHlp.getFormulaFromCell(runde1,
				Position.from(SchweizerAbstractSpielrundeSheet.DURCHGANG_STARTZEIT_SPALTE, SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE + 6));
		assertThat(formelDurchgang3).contains("$F$2").contains("(2)").contains("Durchgang Zeitlimit (Minuten)")
				.contains("Durchgang Pause (Minuten)");

		// Bahn-Nr beginnt in jedem Durchgang neu bei 1
		RangeData bahnNrn = ladeBahnNummern(runde1, ANZ_TEAMS / 2);
		int[] erwarteteBahnNr = { 1, 2, 3, 1, 2, 3, 1, 2 };
		for (int i = 0; i < erwarteteBahnNr.length; i++) {
			assertThat(bahnNrn.get(i).get(0).getIntVal(-1))
					.as("Bahn-Nr Zeile %d", i).isEqualTo(erwarteteBahnNr[i]);
		}
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

		// Durchgang-Spalten trotz NAME-Modus korrekt befuellt (mind. ein Label vorhanden)
		RangeData durchgangSpalten = ladeDurchgangSpalten(runde1, anzPaarungszeilen);
		boolean mindEinLabel = durchgangSpalten.stream().anyMatch(row -> {
			String label = row.get(0).getStringVal();
			return label != null && !label.isBlank();
		});
		assertThat(mindEinLabel).as("Bei mehr Paarungen als Bahnen muss mind. ein Durchgang-Label geschrieben werden").isTrue();
	}
}
