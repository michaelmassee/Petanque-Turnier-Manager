package de.petanqueturniermanager.schweizer.spielrunde;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.meldeliste.TeamAnzeige;
import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetTestDaten;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetUpdate;
import de.petanqueturniermanager.schweizer.rangliste.SchweizerRanglisteSheet;

/**
 * Regression: Im Anzeigemodus {@link TeamAnzeige#NAME} muss eine Umbenennung eines
 * Teams in der Meldeliste im bereits erzeugten Spielplan sofort sichtbar werden (Team-Name
 * wird per SVERWEIS-Formel statt statischem Text geschrieben, siehe
 * {@link SchweizerAbstractSpielrundeSheet#teamNamenFormelnSchreiben}).
 */
public class SchweizerSpielrundeNamenLiveUpdateUITest extends BaseCalcUITest {

	private static final String ALTER_NAME = "Team 1";
	private static final String NEUER_NAME = "Team 1 NEU";

	@Test
	public void teamNameInSpielrundeAktualisiertSichNachUmbenennungInMeldeliste() throws GenerateException {
		// Meldeliste mit Testdaten anlegen (Teamname + Vereinsname aktiv)
		new SchweizerMeldeListeSheetTestDaten(wkingSpreadsheet).doRun();

		// Erst danach auf NAME-Anzeigemodus umschalten und Runde 1 erzeugen
		SchweizerSpielrundeSheetNaechste spielrundeNaechste = new SchweizerSpielrundeSheetNaechste(wkingSpreadsheet);
		spielrundeNaechste.getKonfigurationSheet().setSpielplanTeamAnzeige(TeamAnzeige.NAME);
		spielrundeNaechste.getKonfigurationSheet().setSpielrundeSpielbahn(SpielrundeSpielbahn.R);
		spielrundeNaechste.doRun();

		XSpreadsheet spielrundeSheet = spielrundeNaechste.getXSpreadSheet();
		int letzteZeile = spielrundeNaechste.getMeldeListe().getAktiveMeldungen().size() / 2 - 1
				+ SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE;

		Position gefundenePos = null;
		for (int zeile = SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE; zeile <= letzteZeile; zeile++) {
			for (int spalte : new int[] { SchweizerAbstractSpielrundeSheet.TEAM_A_SPALTE,
					SchweizerAbstractSpielrundeSheet.TEAM_B_SPALTE }) {
				Position pos = Position.from(spalte, zeile);
				if (ALTER_NAME.equals(spielrundeNaechste.getSheetHelper().getTextFromCell(spielrundeSheet, pos))) {
					gefundenePos = pos;
				}
			}
		}
		assertThat(gefundenePos).as("Team '%s' im frisch erzeugten Spielplan gefunden", ALTER_NAME).isNotNull();

		// Zeile mit dem Namen in der Meldeliste suchen (Nr-zu-Name-Zuordnung ist wegen
		// alphabetischer Sortierung in updateMeldungenNr() nicht vorhersagbar) und umbenennen
		XSpreadsheet meldeListeSheet = spielrundeNaechste.getMeldeListe().getXSpreadSheet();
		int teamnameSpalte = spielrundeNaechste.getMeldeListe().getTeamnameSpalte();
		int meldeZeile = -1;
		int meldeListeErsteDatenZeile = spielrundeNaechste.getMeldeListe().getErsteDatenZiele();
		for (int zeile = meldeListeErsteDatenZeile; zeile < meldeListeErsteDatenZeile
				+ SchweizerMeldeListeSheetTestDaten.ANZ_TEAMS_DEFAULT; zeile++) {
			if (ALTER_NAME
					.equals(spielrundeNaechste.getSheetHelper().getTextFromCell(meldeListeSheet, Position.from(teamnameSpalte, zeile)))) {
				meldeZeile = zeile;
				break;
			}
		}
		assertThat(meldeZeile).as("Team '%s' in der Meldeliste gefunden", ALTER_NAME).isNotEqualTo(-1);

		Position teamNamePos = Position.from(teamnameSpalte, meldeZeile);
		spielrundeNaechste.getSheetHelper()
				.setStringValueInCell(StringCellValue.from(meldeListeSheet, teamNamePos, NEUER_NAME));
		spielrundeNaechste.getxCalculatable().calculateAll();

		String aktualisierterText = spielrundeNaechste.getSheetHelper().getTextFromCell(spielrundeSheet,
				gefundenePos);
		assertThat(aktualisierterText).isEqualTo(NEUER_NAME);
	}

	@Test
	public void zusammengesetzteSpielernamenSindAlsEigeneSpielrundenAnzeigeVerfuegbar() throws GenerateException {
		new SchweizerMeldeListeSheetTestDaten(wkingSpreadsheet).doRun();

		SchweizerSpielrundeSheetNaechste spielrundeNaechste = new SchweizerSpielrundeSheetNaechste(wkingSpreadsheet);
		spielrundeNaechste.getKonfigurationSheet().setSpielplanTeamAnzeige(TeamAnzeige.SPIELERNAMEN);
		spielrundeNaechste.doRun();

		XSpreadsheet spielrundeSheet = spielrundeNaechste.getXSpreadSheet();
		boolean hatZusammengesetztenNamen = false;
		for (int zeile = SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE;
				zeile < SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE + 8; zeile++) {
			for (int spalte : new int[] { SchweizerAbstractSpielrundeSheet.TEAM_A_SPALTE,
					SchweizerAbstractSpielrundeSheet.TEAM_B_SPALTE }) {
				String wert = spielrundeNaechste.getSheetHelper()
						.getTextFromCell(spielrundeSheet, Position.from(spalte, zeile));
				hatZusammengesetztenNamen |= wert != null && wert.contains(" / ");
			}
		}
		assertThat(hatZusammengesetztenNamen)
				.as("Spielernamen-Anzeige muss die Namen aller Teammitglieder zusammensetzen")
				.isTrue();
	}

	@Test
	public void zusammengesetzteSpielernamenBleibenAbZweiterRundeAuslosbarUndInDerRanglisteWertbar()
			throws GenerateException {
		int anzTeams = SchweizerMeldeListeSheetTestDaten.ANZ_TEAMS_DEFAULT;
		SchweizerTurnierTestDaten testDaten = new SchweizerTurnierTestDaten(wkingSpreadsheet, anzTeams,
				TeamAnzeige.SPIELERNAMEN);
		testDaten.generate(1, false);

		// Eine Korrektur nach Runde 1 muss in deren Formelanzeige erscheinen und darf die
		// technische Teamzuordnung für Runde 2 nicht zerstören.
		XSpreadsheet meldeListe = testDaten.naechsteSpielrunde.getMeldeListe().getXSpreadSheet();
		int meldeZeile = testDaten.naechsteSpielrunde.getMeldeListe().getErsteDatenZiele();
		int vornameSpalte = testDaten.naechsteSpielrunde.getMeldeListe().getVornameSpalte(0);
		testDaten.naechsteSpielrunde.getSheetHelper().setStringValueInCell(
				StringCellValue.from(meldeListe, Position.from(vornameSpalte, meldeZeile), "Korrigiert"));
		testDaten.naechsteSpielrunde.getxCalculatable().calculateAll();

		XSpreadsheet ersteRunde = sheetHlp.findByName("1. " + SchweizerAbstractSpielrundeSheet.SHEET_NAMEN);
		assertThat(hatAnzeigenamen(ersteRunde, "Korrigiert"))
				.as("Die Namenskorrektur muss in Runde 1 durch die Anzeigeformel sichtbar sein")
				.isTrue();
		assertThat(sheetHlp.getIntFromCell(ersteRunde, Position.from(
				SchweizerAbstractSpielrundeSheet.TECHNISCHE_TEAM_A_NR_SPALTE,
				SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE)))
				.as("Runde 1 muss die technische Teamnummer getrennt von der Namensanzeige speichern")
				.isGreaterThan(0);
		pruefeTechnischeSpaltenAusgeblendet(ersteRunde);

		testDaten.naechsteSpielrunde.doRun();

		XSpreadsheet zweiteRunde = sheetHlp.findByName("2. " + SchweizerAbstractSpielrundeSheet.SHEET_NAMEN);
		assertThat(zweiteRunde).as("Die zweite Runde muss erzeugt werden").isNotNull();
		assertThat(sheetHlp.getTextFromCell(zweiteRunde,
				Position.from(SchweizerAbstractSpielrundeSheet.TEAM_A_SPALTE,
						SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE)))
				.as("Die zweite Runde muss eine Paarung mit zusammengesetzten Spielernamen enthalten")
				.contains(" / ");
		assertThat(sheetHlp.getIntFromCell(zweiteRunde, Position.from(
				SchweizerAbstractSpielrundeSheet.TECHNISCHE_TEAM_A_NR_SPALTE,
				SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE)))
				.as("Die zweite Runde muss für die nächste Auswertung eine technische Teamnummer speichern")
				.isGreaterThan(0);
		pruefeTechnischeSpaltenAusgeblendet(zweiteRunde);
		RangeData ergebnisse = new RangeData();
		for (int i = 0; i < anzTeams / 2; i++) {
			ergebnisse.addNewRow(13, 5);
		}
		RangeHelper.from(zweiteRunde, wkingSpreadsheet.getWorkingSpreadsheetDocument(),
				ergebnisse.getRangePosition(Position.from(SchweizerAbstractSpielrundeSheet.ERG_TEAM_A_SPALTE,
						SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE)))
				.setDataInRange(ergebnisse);
		assertThat(siegeInNeuerRangliste(anzTeams)).as("Die Rangliste muss die Siege aus beiden Runden zählen")
				.isEqualTo(anzTeams);
	}

	@Test
	public void alteSpielrundeOhneTechnischeSpaltenWirdUeberDieMeldelisteAusgewertet() throws GenerateException {
		int anzTeams = SchweizerMeldeListeSheetTestDaten.ANZ_TEAMS_DEFAULT;
		SchweizerTurnierTestDaten testDaten = new SchweizerTurnierTestDaten(wkingSpreadsheet, anzTeams,
				TeamAnzeige.SPIELERNAMEN);
		testDaten.generate(1, false);

		// Zustand einer vor Einführung der technischen Spalten erzeugten Datei nachstellen
		XSpreadsheet ersteRunde = sheetHlp.findByName("1. " + SchweizerAbstractSpielrundeSheet.SHEET_NAMEN);
		technischeSpaltenLoeschen(ersteRunde, anzTeams);

		assertThat(siegeInNeuerRangliste(anzTeams))
				.as("Ohne technische Spalten müssen die Spielernamen über die Meldeliste aufgelöst werden")
				.isEqualTo(anzTeams / 2);
	}

	@Test
	public void mehrdeutigeAnzeigeInAlterSpielrundeBrichtAbStattFreilosZuWerten() throws GenerateException {
		int anzTeams = SchweizerMeldeListeSheetTestDaten.ANZ_TEAMS_DEFAULT;
		SchweizerTurnierTestDaten testDaten = new SchweizerTurnierTestDaten(wkingSpreadsheet, anzTeams,
				TeamAnzeige.SPIELERNAMEN);
		testDaten.generate(1, false);

		XSpreadsheet ersteRunde = sheetHlp.findByName("1. " + SchweizerAbstractSpielrundeSheet.SHEET_NAMEN);
		int teamNrA = sheetHlp.getIntFromCell(ersteRunde, Position.from(
				SchweizerAbstractSpielrundeSheet.TECHNISCHE_TEAM_A_NR_SPALTE,
				SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE));
		int teamNrB = sheetHlp.getIntFromCell(ersteRunde, Position.from(
				SchweizerAbstractSpielrundeSheet.TECHNISCHE_TEAM_B_NR_SPALTE,
				SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE));
		technischeSpaltenLoeschen(ersteRunde, anzTeams);

		// Team A erhält die Spielernamen von Team B → beide Paarungszellen zeigen dieselbe Kennung
		SchweizerMeldeListeSheetUpdate meldeListe = testDaten.naechsteSpielrunde.getMeldeListe();
		namenKopieren(meldeListe, teamNrB, teamNrA);
		testDaten.naechsteSpielrunde.getxCalculatable().calculateAll();

		Position teamAPos = Position.from(SchweizerAbstractSpielrundeSheet.TEAM_A_SPALTE,
				SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE);
		Position teamBPos = Position.from(SchweizerAbstractSpielrundeSheet.TEAM_B_SPALTE,
				SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE);
		String kennung = sheetHlp.getTextFromCell(ersteRunde, teamAPos);
		assertThat(sheetHlp.getTextFromCell(ersteRunde, teamBPos))
				.as("Vorbedingung: beide Teams der Paarung haben dieselbe sichtbare Kennung")
				.isEqualTo(kennung);

		assertThatThrownBy(() -> new SchweizerRanglisteSheet(wkingSpreadsheet).doRun())
				.as("Eine mehrdeutige Kennung darf weder als Datenende noch als Freilos gewertet werden")
				.isInstanceOf(GenerateException.class)
				.hasMessage(I18n.get("schweizer.spielrunde.fehler.team.mehrdeutig", kennung));
	}

	@Test
	public void spielernamenAnzeigeMitTeamnameSpalteLiestTeamnameNichtAlsVorname() throws GenerateException {
		int anzTeams = SchweizerMeldeListeSheetTestDaten.ANZ_TEAMS_DEFAULT;
		SchweizerTurnierTestDaten testDaten = new SchweizerTurnierTestDaten(wkingSpreadsheet, anzTeams,
				TeamAnzeige.SPIELERNAMEN);
		testDaten.generate(1, false);

		SchweizerMeldeListeSheetUpdate meldeListe = testDaten.naechsteSpielrunde.getMeldeListe();
		assertThat(meldeListe.getTeamnameSpalte()).as("Vorbedingung: Testdaten mit Teamname-Spalte")
				.isPositive();
		XSpreadsheet ersteRunde = sheetHlp.findByName("1. " + SchweizerAbstractSpielrundeSheet.SHEET_NAMEN);
		int teamNrA = sheetHlp.getIntFromCell(ersteRunde, Position.from(
				SchweizerAbstractSpielrundeSheet.TECHNISCHE_TEAM_A_NR_SPALTE,
				SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE));

		String erwartet = erwarteteSpielernamen(meldeListe, meldeZeileVonTeam(meldeListe, teamNrA),
				testDaten.naechsteSpielrunde.getKonfigurationSheet().getMeldeListeFormation().getAnzSpieler());
		assertThat(sheetHlp.getTextFromCell(ersteRunde, Position.from(SchweizerAbstractSpielrundeSheet.TEAM_A_SPALTE,
				SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE)))
				.as("Spielernamen-Anzeige muss bei Spieler 1 beginnen, nicht beim Teamnamen")
				.isEqualTo(erwartet);
	}

	/** Baut die erwartete Anzeige unabhängig vom Formatierer direkt aus den Meldelisten-Zellen. */
	private String erwarteteSpielernamen(SchweizerMeldeListeSheetUpdate meldeListe, int zeile, int anzSpieler)
			throws GenerateException {
		XSpreadsheet sheet = meldeListe.getXSpreadSheet();
		List<String> spieler = new ArrayList<>();
		for (int idx = 0; idx < anzSpieler; idx++) {
			String vorname = sheetHlp.getTextFromCell(sheet, Position.from(meldeListe.getVornameSpalte(idx), zeile));
			String nachname = sheetHlp.getTextFromCell(sheet, Position.from(meldeListe.getNachnameSpalte(idx), zeile));
			String verein = sheetHlp.getTextFromCell(sheet,
					Position.from(meldeListe.getVereinsnameSpalte(idx), zeile));
			spieler.add(vorname + " " + nachname + " (" + verein + ")");
		}
		return String.join(" / ", spieler);
	}

	private void pruefeTechnischeSpaltenAusgeblendet(XSpreadsheet spielrunde) {
		assertThat(istSpalteSichtbar(spielrunde, SchweizerAbstractSpielrundeSheet.TECHNISCHE_TEAM_A_NR_SPALTE))
				.as("Technische Teamnummer A muss ausgeblendet sein").isFalse();
		assertThat(istSpalteSichtbar(spielrunde, SchweizerAbstractSpielrundeSheet.TECHNISCHE_TEAM_B_NR_SPALTE))
				.as("Technische Teamnummer B muss ausgeblendet sein").isFalse();
	}

	/** Stellt den Zustand einer vor Einführung der technischen Spalten erzeugten Datei nach. */
	private void technischeSpaltenLoeschen(XSpreadsheet spielrunde, int anzTeams) throws GenerateException {
		RangeHelper.from(spielrunde, wkingSpreadsheet.getWorkingSpreadsheetDocument(), RangePosition.from(
				SchweizerAbstractSpielrundeSheet.TECHNISCHE_TEAM_A_NR_SPALTE,
				SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE,
				SchweizerAbstractSpielrundeSheet.TECHNISCHE_TEAM_B_NR_SPALTE,
				SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE + anzTeams)).clearRange();
	}

	/** Kopiert Teamname- und Spielerspalten der Meldelistenzeile von {@code quellNr} nach {@code zielNr}. */
	private void namenKopieren(SchweizerMeldeListeSheetUpdate meldeListe, int quellNr, int zielNr)
			throws GenerateException {
		XSpreadsheet sheet = meldeListe.getXSpreadSheet();
		var xDoc = wkingSpreadsheet.getWorkingSpreadsheetDocument();
		int ersteNamenSpalte = meldeListe.getTeamNrSpalte() + 1;
		int letzteNamenSpalte = meldeListe.getSetzPositionSpalte() - 1;

		int quellZeile = meldeZeileVonTeam(meldeListe, quellNr);
		int zielZeile = meldeZeileVonTeam(meldeListe, zielNr);

		RangeData namen = RangeHelper.from(sheet, xDoc,
				RangePosition.from(ersteNamenSpalte, quellZeile, letzteNamenSpalte, quellZeile)).getDataFromRange();
		RangeHelper.from(sheet, xDoc, namen.getRangePosition(Position.from(ersteNamenSpalte, zielZeile)))
				.setDataInRange(namen);
	}

	private int meldeZeileVonTeam(SchweizerMeldeListeSheetUpdate meldeListe, int teamNr) throws GenerateException {
		int ersteZeile = meldeListe.getErsteDatenZiele();
		RangeData nummern = RangeHelper.from(meldeListe.getXSpreadSheet(), wkingSpreadsheet.getWorkingSpreadsheetDocument(),
				RangePosition.from(meldeListe.getTeamNrSpalte(), ersteZeile, meldeListe.getTeamNrSpalte(),
						meldeListe.getLetzteMitDatenZeileInSpielerNrSpalte()))
				.getDataFromRange();
		for (int idx = 0; idx < nummern.size(); idx++) {
			if (nummern.get(idx).get(0).getIntVal(0) == teamNr) {
				return ersteZeile + idx;
			}
		}
		throw new IllegalStateException("Team " + teamNr + " nicht in der Meldeliste gefunden");
	}

	private int siegeInNeuerRangliste(int anzTeams) throws GenerateException {
		new SchweizerRanglisteSheet(wkingSpreadsheet).doRun();

		XSpreadsheet rangliste = sheetHlp.findByName(SheetNamen.rangliste());
		assertThat(rangliste).as("Die Rangliste muss erzeugt werden").isNotNull();
		var daten = RangeHelper.from(rangliste, wkingSpreadsheet.getWorkingSpreadsheetDocument(), RangePosition.from(
				SchweizerRanglisteSheet.SIEGE_SPALTE, SchweizerRanglisteSheet.ERSTE_DATEN_ZEILE,
				SchweizerRanglisteSheet.SIEGE_SPALTE,
				SchweizerRanglisteSheet.ERSTE_DATEN_ZEILE + anzTeams - 1)).getDataFromRange();
		int siege = 0;
		for (var zeile : daten) {
			siege += zeile.get(0).getIntVal(0);
		}
		return siege;
	}

	private boolean hatAnzeigenamen(XSpreadsheet spielrunde, String teil) throws GenerateException {
		for (int zeile = SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE;
				zeile < SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE
						+ SchweizerMeldeListeSheetTestDaten.ANZ_TEAMS_DEFAULT / 2; zeile++) {
			for (int spalte : new int[] { SchweizerAbstractSpielrundeSheet.TEAM_A_SPALTE,
					SchweizerAbstractSpielrundeSheet.TEAM_B_SPALTE }) {
				String wert = sheetHlp.getTextFromCell(spielrunde, Position.from(spalte, zeile));
				if (wert != null && wert.contains(teil)) {
					return true;
				}
			}
		}
		return false;
	}

}
