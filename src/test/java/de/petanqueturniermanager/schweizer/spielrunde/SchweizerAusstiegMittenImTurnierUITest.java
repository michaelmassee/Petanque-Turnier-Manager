package de.petanqueturniermanager.schweizer.spielrunde;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.algorithmen.schweizer.SchweizerAuswertung;
import de.petanqueturniermanager.algorithmen.schweizer.SchweizerTeamErgebnis;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetUpdate;
import de.petanqueturniermanager.schweizer.rangliste.SchweizerRanglisteSheet;

/**
 * Regression: ein Team steigt NACH bereits gespielter Runde 1 aus (Aktiv-Spalte = 2).
 * Erwartet (aktuelles, konsistentes Verhalten, Pétanque-Konvention Schweizer System): Runde 2 paart
 * das Team nicht mehr, Runde-1-Ergebnisse bleiben unverändert stehen. Die Rangliste bleibt dagegen
 * mit dem bisherigen Ergebnis des ausgestiegenen Teams stehen ({@code getAktiveUndAusgesetztMeldungen()})
 * – ein vorzeitiger Ausstieg darf weder das eigene, bereits erspielte Ergebnis noch (über die
 * Buchholz-Wertung) das der Gegner verfälschen.
 */
public class SchweizerAusstiegMittenImTurnierUITest extends BaseCalcUITest {

	private static final int ANZ_TEAMS = 16;
	private static final int AUSGESTIEGENES_TEAM_NR = 1;

	private SchweizerTurnierTestDaten testDaten;

	@BeforeEach
	public void setup() {
		testDaten = new SchweizerTurnierTestDaten(wkingSpreadsheet);
	}

	@Test
	public void ausstiegNachRunde1_wirdInRunde2NichtMehrGepaart() throws GenerateException {
		// Runde 1 spielen, noch keine Rangliste
		testDaten.generate(1, false);

		String runde1SheetName = SheetNamen.spielrunde(1);
		List<List<Integer>> runde1VorAusstieg = leseSpielpaarungWerte(runde1SheetName);

		setzeAktivWert(AUSGESTIEGENES_TEAM_NR, SchweizerMeldeListeSheetUpdate.AKTIV_WERT_AUSGESTIEGEN);

		// Runde 2 erzeugen und mit Ergebnissen füllen
		testDaten.naechsteSpielrunde.doRun();
		String runde2SheetName = SheetNamen.spielrunde(2);
		XSpreadsheet runde2Sheet = sheetHlp.findByName(runde2SheetName);
		assertThat(runde2Sheet).as(runde2SheetName + " muss vorhanden sein").isNotNull();
		testDaten.ergebnisseEinfuegen(runde2Sheet);

		// Runde 1: Ergebnisse unverändert (Ausstieg wirkt nicht rückwirkend)
		List<List<Integer>> runde1NachAusstieg = leseSpielpaarungWerte(runde1SheetName);
		assertThat(runde1NachAusstieg).as("Runde 1 darf durch den Ausstieg nicht verändert werden")
				.isEqualTo(runde1VorAusstieg);

		// Runde 2: das ausgestiegene Team taucht in keiner Paarung mehr auf
		List<List<Integer>> runde2Paarungen = leseSpielpaarungWerte(runde2SheetName);
		for (var row : runde2Paarungen) {
			int nrA = row.get(0);
			int nrB = row.get(1);
			assertThat(nrA).as("Ausgestiegenes Team darf nicht mehr als Team A gepaart werden")
					.isNotEqualTo(AUSGESTIEGENES_TEAM_NR);
			assertThat(nrB).as("Ausgestiegenes Team darf nicht mehr als Team B gepaart werden")
					.isNotEqualTo(AUSGESTIEGENES_TEAM_NR);
		}

		// Rangliste erzeugen: darf nicht crashen. SchweizerRanglisteSheet baut aus
		// getAktiveUndAusgesetztMeldungen() auf – das ausgestiegene Team bleibt mit seinem
		// bisherigen (Runde-1-)Ergebnis in der Rangliste stehen, wird aber (siehe oben) nicht mehr
		// neu gepaart.
		SchweizerRanglisteSheet ranglisteSheet = new SchweizerRanglisteSheet(wkingSpreadsheet);
		ranglisteSheet.doRun();

		XSpreadsheet rangliste = sheetHlp.findByName(SheetNamen.rangliste());
		assertThat(rangliste).as("Rangliste-Sheet").isNotNull();

		RangePosition ranglisteRange = RangePosition.from(
				SchweizerRanglisteSheet.TEAM_NR_SPALTE, SchweizerRanglisteSheet.ERSTE_DATEN_ZEILE,
				SchweizerRanglisteSheet.SIEGE_SPALTE, SchweizerRanglisteSheet.ERSTE_DATEN_ZEILE + ANZ_TEAMS - 1);
		RangeData ranglisteData = RangeHelper
				.from(rangliste, wkingSpreadsheet.getWorkingSpreadsheetDocument(), ranglisteRange)
				.getDataFromRange();
		assertThat(ranglisteData)
				.as("Rangliste muss weiterhin alle " + ANZ_TEAMS + " Teams listen (inkl. ausgestiegenem)")
				.hasSize(ANZ_TEAMS);

		boolean ausgestiegenesTeamInRangliste = ranglisteData.stream()
				.anyMatch(row -> row.get(SchweizerRanglisteSheet.TEAM_NR_SPALTE).getIntVal(-1)
						== AUSGESTIEGENES_TEAM_NR);
		assertThat(ausgestiegenesTeamInRangliste)
				.as("Ausgestiegenes Team bleibt mit seinem bisherigen Ergebnis in der Rangliste")
				.isTrue();

		// Regression: jede echte Paarungszeile (Runde 1 UND Runde 2, beide sind zu diesem Zeitpunkt
		// bereits gespielt) vergibt genau einen Sieg (echter Gewinn oder Freilos). Die Siege-Summe
		// über die GESAMTE Rangliste muss dieser Zeilenzahl entsprechen - würde der Gegner des
		// ausgestiegenen Teams seinen bereits erspielten Sieg verlieren (weil dessen Zeile beim
		// Rangliste-Aufbau komplett übersprungen wird), fehlte hier ein Sieg.
		int erwarteteSiege = zaehleGueltigeZeilen(runde1VorAusstieg) + zaehleGueltigeZeilen(runde2Paarungen);
		int siegeSumme = ranglisteData.stream()
				.mapToInt(row -> row.get(SchweizerRanglisteSheet.SIEGE_SPALTE).getIntVal(0))
				.sum();
		assertThat(siegeSumme)
				.as("Kein Sieg darf durch den Ausstieg des Gegners aus der Rangliste-Summe verschwinden")
				.isEqualTo(erwarteteSiege);

		// Alle übrigen 15 aktiven Teams müssen in Runde 2 gepaart worden sein (ggf. inkl. 1 Freilos)
		var gepaarteNrn = new java.util.HashSet<Integer>();
		for (var row : runde2Paarungen) {
			int nrA = row.get(0);
			if (nrA > 0) {
				gepaarteNrn.add(nrA);
			}
			int nrB = row.get(1);
			if (nrB > 0) {
				gepaarteNrn.add(nrB);
			}
		}
		assertThat(gepaarteNrn).as("Alle 15 weiterhin aktiven Teams müssen in Runde 2 gepaart sein")
				.hasSize(ANZ_TEAMS - 1)
				.doesNotContain(AUSGESTIEGENES_TEAM_NR);
	}

	/**
	 * Regression: Steigt der in Runde 1 besiegte Gegner aus, behält der Sieger seinen Sieg auch in der Auslosung
	 * (nicht nur in der Rangliste). Mit 15 aktiven Teams bilden die 8 Runde-1-Sieger eine gerade Sieggruppe; der
	 * Sieger muss daher in Runde 2 gegen einen anderen Runde-1-Sieger spielen und darf kein Freilos bekommen.
	 */
	@Test
	public void ausstiegDesBesiegten_siegerBehaeltSiegInAuslosung() throws GenerateException {
		testDaten.generate(1, false);
		List<List<Integer>> runde1 = leseSpielpaarungWerte(SheetNamen.spielrunde(1));
		List<Integer> begegnung = ersteBegegnungMitGegner(runde1);
		int sieger = siegerVon(begegnung);
		int besiegter = besiegterVon(begegnung);
		Set<Integer> rundeEinsSieger = runde1.stream().filter(this::istBegegnung).map(this::siegerVon)
				.collect(Collectors.toSet());

		setzeAktivWert(besiegter, SchweizerMeldeListeSheetUpdate.AKTIV_WERT_AUSGESTIEGEN);

		SchweizerAuswertung auswertung = testDaten.naechsteSpielrunde
				.gespieltenRundenEinlesen(neueMeldeliste().getAktiveMeldungen(), 1, 1);
		SchweizerTeamErgebnis siegerErgebnis = ergebnisVon(auswertung, sieger);
		assertThat(siegerErgebnis.siege()).as("Sieg gegen das ausgestiegene Team zählt in der Auslosung").isEqualTo(1);
		assertThat(siegerErgebnis.punktedifferenz()).as("Punktedifferenz gegen das ausgestiegene Team")
				.isEqualTo(Math.abs(begegnung.get(2) - begegnung.get(3)));
		assertThat(siegerErgebnis.gegnerNrn()).as("Gegnerhistorie (Buchholz)").contains(besiegter);
		assertThat(auswertung.ergebnisse()).as("Ausgestiegenes Team bleibt für Buchholz/Feinbuchholz erhalten")
				.extracting(SchweizerTeamErgebnis::teamNr).contains(besiegter);

		testDaten.naechsteSpielrunde.doRun();
		List<List<Integer>> runde2 = leseSpielpaarungWerte(SheetNamen.spielrunde(2));
		List<Integer> runde2Zeile = runde2.stream()
				.filter(zeile -> zeile.get(0) == sieger || zeile.get(1) == sieger).findFirst().orElseThrow();
		int gegnerRunde2 = runde2Zeile.get(0) == sieger ? runde2Zeile.get(1) : runde2Zeile.get(0);
		assertThat(gegnerRunde2).as("Sieger darf kein Freilos bekommen").isPositive();
		assertThat(rundeEinsSieger).as("Gegner in Runde 2 muss ebenfalls Runde 1 gewonnen haben")
				.contains(gegnerRunde2);
	}

	/**
	 * Ein ausgestiegenes Team kann wieder einsteigen: Seine Begegnungen zählen wieder vollständig, und der
	 * Wiederholungsschutz gegen frühere Gegner greift wieder.
	 */
	@Test
	public void wiedereinstieg_historieUndWiederholungsschutzGreifenWieder() throws GenerateException {
		testDaten.generate(1, false);
		List<Integer> begegnung = ersteBegegnungMitGegner(leseSpielpaarungWerte(SheetNamen.spielrunde(1)));
		int sieger = siegerVon(begegnung);
		int besiegter = besiegterVon(begegnung);

		setzeAktivWert(besiegter, SchweizerMeldeListeSheetUpdate.AKTIV_WERT_AUSGESTIEGEN);
		setzeAktivWert(besiegter, SchweizerMeldeListeSheetUpdate.AKTIV_WERT_NIMMT_TEIL);

		TeamMeldungen aktive = neueMeldeliste().getAktiveMeldungen();
		SchweizerAuswertung auswertung = testDaten.naechsteSpielrunde.gespieltenRundenEinlesen(aktive, 1, 1);
		assertThat(ergebnisVon(auswertung, sieger).siege()).isEqualTo(1);
		assertThat(ergebnisVon(auswertung, besiegter).gegnerNrn()).contains(sieger);
		assertThat(aktive.getTeam(sieger).hatAlsGegner(aktive.getTeam(besiegter)))
				.as("Wiederholungsschutz nach Wiedereinstieg").isTrue();
	}

	private SchweizerMeldeListeSheetUpdate neueMeldeliste() {
		return new SchweizerMeldeListeSheetUpdate(wkingSpreadsheet);
	}

	private void setzeAktivWert(int teamNr, int aktivWert) throws GenerateException {
		SchweizerMeldeListeSheetUpdate meldeliste = neueMeldeliste();
		int zeile = meldeliste.getSpielerZeileNr(teamNr);
		assertThat(zeile).as("Team %d muss in der Meldeliste gefunden werden", teamNr).isGreaterThan(0);
		sheetHlp.setNumberValueInCell(NumberCellValue
				.from(meldeliste.getXSpreadSheet(), Position.from(meldeliste.getAktivSpalte(), zeile))
				.setValue(aktivWert));
	}

	private static SchweizerTeamErgebnis ergebnisVon(SchweizerAuswertung auswertung, int teamNr) {
		return auswertung.ergebnisse().stream().filter(erg -> erg.teamNr() == teamNr).findFirst().orElseThrow();
	}

	private boolean istBegegnung(List<Integer> zeile) {
		return zeile.get(0) > 0 && zeile.get(1) > 0;
	}

	private List<Integer> ersteBegegnungMitGegner(List<List<Integer>> paarungen) {
		return paarungen.stream().filter(this::istBegegnung).findFirst().orElseThrow();
	}

	private int siegerVon(List<Integer> begegnung) {
		return begegnung.get(2) > begegnung.get(3) ? begegnung.get(0) : begegnung.get(1);
	}

	private int besiegterVon(List<Integer> begegnung) {
		return begegnung.get(2) > begegnung.get(3) ? begegnung.get(1) : begegnung.get(0);
	}

	/**
	 * Zählt die echten Paarungszeilen (bis zur ersten Zeile ohne gültige Team-A-Nr).
	 */
	private int zaehleGueltigeZeilen(List<List<Integer>> paarungen) {
		int anzahl = 0;
		for (var row : paarungen) {
			if (row.get(0) <= 0) {
				break;
			}
			anzahl++;
		}
		return anzahl;
	}

	/**
	 * Liest Team-A-/Team-B-/Ergebnis-Werte als {@code List<List<Integer>>} statt {@code RangeData} –
	 * {@link de.petanqueturniermanager.helper.sheet.rangedata.CellData} hat kein {@code equals()},
	 * ein direkter {@code RangeData}-Vergleich wäre daher immer Objekt-Identität statt Werte-Vergleich.
	 */
	private List<List<Integer>> leseSpielpaarungWerte(String sheetName) throws GenerateException {
		XSpreadsheet sheet = sheetHlp.findByName(sheetName);
		assertThat(sheet).as(sheetName + " muss vorhanden sein").isNotNull();
		RangePosition paarungenRange = RangePosition.from(
				SchweizerAbstractSpielrundeSheet.TEAM_A_SPALTE, SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE,
				SchweizerAbstractSpielrundeSheet.ERG_TEAM_B_SPALTE, SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE + 100);
		RangeData data = RangeHelper.from(sheet, wkingSpreadsheet.getWorkingSpreadsheetDocument(), paarungenRange)
				.getDataFromRange();

		List<List<Integer>> werte = new ArrayList<>();
		for (var row : data) {
			List<Integer> zeile = new ArrayList<>();
			for (var cell : row) {
				zeile.add(cell.getIntVal(0));
			}
			werte.add(zeile);
		}
		return werte;
	}
}
