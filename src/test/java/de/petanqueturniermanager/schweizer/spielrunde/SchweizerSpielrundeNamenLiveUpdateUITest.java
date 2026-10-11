package de.petanqueturniermanager.schweizer.spielrunde;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.MeldelisteUuidIndex;
import de.petanqueturniermanager.basesheet.meldeliste.TeamAnzeige;
import de.petanqueturniermanager.basesheet.meldeliste.TeamAnzeigeHelper;
import de.petanqueturniermanager.basesheet.meldeliste.TeamUuidFormel;
import de.petanqueturniermanager.basesheet.meldeliste.TeamnummerAendernSheet;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerKonfigurationSheet;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetTestDaten;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetUpdate;
import de.petanqueturniermanager.schweizer.rangliste.SchweizerRanglisteSheet;

/**
 * Teamzellen der Spielrunde sind Formeln mit der UUID des Teams: Umbenennungen in der Meldeliste werden sofort
 * sichtbar, und eine geänderte Teamnummer ändert nur die Anzeige, nicht die Zuordnung von Paarungen und Ergebnissen.
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
		assertThat(teamNrInRunde(ersteRunde, SchweizerAbstractSpielrundeSheet.TEAM_A_SPALTE,
				SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE))
				.as("Runde 1 muss die UUID des Teams in der Anzeigeformel speichern")
				.isGreaterThan(0);

		testDaten.naechsteSpielrunde.doRun();

		XSpreadsheet zweiteRunde = sheetHlp.findByName("2. " + SchweizerAbstractSpielrundeSheet.SHEET_NAMEN);
		assertThat(zweiteRunde).as("Die zweite Runde muss erzeugt werden").isNotNull();
		assertThat(sheetHlp.getTextFromCell(zweiteRunde,
				Position.from(SchweizerAbstractSpielrundeSheet.TEAM_A_SPALTE,
						SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE)))
				.as("Die zweite Runde muss eine Paarung mit zusammengesetzten Spielernamen enthalten")
				.contains(" / ");
		assertThat(teamNrInRunde(zweiteRunde, SchweizerAbstractSpielrundeSheet.TEAM_A_SPALTE,
				SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE))
				.as("Die zweite Runde muss für die nächste Auswertung die UUID des Teams speichern")
				.isGreaterThan(0);
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
	public void teamnummerAendernTauschtNurDieAnzeigeNichtDieZuordnung() throws GenerateException {
		int anzTeams = SchweizerMeldeListeSheetTestDaten.ANZ_TEAMS_DEFAULT;
		SchweizerTurnierTestDaten testDaten = new SchweizerTurnierTestDaten(wkingSpreadsheet, anzTeams,
				TeamAnzeige.NR);
		testDaten.generate(1, false);

		XSpreadsheet ersteRunde = sheetHlp.findByName("1. " + SchweizerAbstractSpielrundeSheet.SHEET_NAMEN);
		int zeile = SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE;
		Position teamAPos = Position.from(SchweizerAbstractSpielrundeSheet.TEAM_A_SPALTE, zeile);
		Position teamBPos = Position.from(SchweizerAbstractSpielrundeSheet.TEAM_B_SPALTE, zeile);
		String uuidSieger = uuidInRunde(ersteRunde, teamAPos);
		String uuidVerlierer = uuidInRunde(ersteRunde, teamBPos);
		ergebnisSetzen(ersteRunde, zeile, 13, 5);

		MeldelisteUuidIndex vorher = MeldelisteUuidIndex.lese(wkingSpreadsheet);
		int nrSieger = vorher.nr(uuidSieger).orElseThrow();
		int nrVerlierer = vorher.nr(uuidVerlierer).orElseThrow();
		// Das Property schreibt sonst erst SheetRunner.run(); der Test ruft doRun() direkt.
		new DocumentPropertiesHelper(wkingSpreadsheet).setIntProperty(
				BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM, TurnierSystem.SCHWEIZER.getId());
		new TeamnummerAendernSheet(wkingSpreadsheet).mitEingabe(nrSieger, nrVerlierer).doRun();
		recalcAll();

		assertThat(sheetHlp.getTextFromCell(ersteRunde, teamAPos))
				.as("Die Runde muss die neue Nummer des Siegers anzeigen").isEqualTo(String.valueOf(nrVerlierer));
		assertThat(sheetHlp.getTextFromCell(ersteRunde, teamBPos))
				.as("Die Runde muss die neue Nummer des Verlierers anzeigen").isEqualTo(String.valueOf(nrSieger));

		new SchweizerRanglisteSheet(wkingSpreadsheet).doRun();
		assertThat(siegeVonTeam(anzTeams, nrVerlierer))
				.as("Der Sieg gehört dem Team (UUID), nicht der alten Nummer").isEqualTo(1);
		assertThat(siegeVonTeam(anzTeams, nrSieger))
				.as("Der Verlierer trägt jetzt die alte Siegernummer und hat keinen Sieg").isZero();
	}

	@Test
	public void alteSpielrundeMitTechnischenSpaltenWirdUeberDieseAusgewertet() throws GenerateException {
		int anzTeams = SchweizerMeldeListeSheetTestDaten.ANZ_TEAMS_DEFAULT;
		SchweizerTurnierTestDaten testDaten = new SchweizerTurnierTestDaten(wkingSpreadsheet, anzTeams,
				TeamAnzeige.SPIELERNAMEN);
		testDaten.generate(1, false);

		XSpreadsheet ersteRunde = sheetHlp.findByName("1. " + SchweizerAbstractSpielrundeSheet.SHEET_NAMEN);
		alteSpielrundeNachstellen(ersteRunde, anzTeams, testDaten.naechsteSpielrunde.getKonfigurationSheet(), true);

		assertThat(siegeInNeuerRangliste(anzTeams))
				.as("Runden vor der UUID-Umstellung werden über die technischen Teamnummern ausgewertet")
				.isEqualTo(anzTeams / 2);
	}

	@Test
	public void alteSpielrundeOhneTechnischeSpaltenWirdUeberDieMeldelisteAusgewertet() throws GenerateException {
		int anzTeams = SchweizerMeldeListeSheetTestDaten.ANZ_TEAMS_DEFAULT;
		SchweizerTurnierTestDaten testDaten = new SchweizerTurnierTestDaten(wkingSpreadsheet, anzTeams,
				TeamAnzeige.SPIELERNAMEN);
		testDaten.generate(1, false);

		// Zustand einer vor Einführung der technischen Spalten erzeugten Datei nachstellen
		XSpreadsheet ersteRunde = sheetHlp.findByName("1. " + SchweizerAbstractSpielrundeSheet.SHEET_NAMEN);
		alteSpielrundeNachstellen(ersteRunde, anzTeams, testDaten.naechsteSpielrunde.getKonfigurationSheet(), false);

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
		int teamNrA = teamNrInRunde(ersteRunde, SchweizerAbstractSpielrundeSheet.TEAM_A_SPALTE,
				SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE);
		int teamNrB = teamNrInRunde(ersteRunde, SchweizerAbstractSpielrundeSheet.TEAM_B_SPALTE,
				SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE);
		alteSpielrundeNachstellen(ersteRunde, anzTeams, testDaten.naechsteSpielrunde.getKonfigurationSheet(), false);

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
		int teamNrA = teamNrInRunde(ersteRunde, SchweizerAbstractSpielrundeSheet.TEAM_A_SPALTE,
				SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE);

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

	private String uuidInRunde(XSpreadsheet spielrunde, Position teamPos) {
		return TeamUuidFormel.findeUuid(sheetHlp.getFormulaFromCell(spielrunde, teamPos))
				.orElseThrow(() -> new IllegalStateException("Keine UUID in " + teamPos.getAddress()));
	}

	/** Aktuelle Teamnummer der Teamzelle, über ihre UUID und die Meldeliste. */
	private int teamNrInRunde(XSpreadsheet spielrunde, int spalte, int zeile) {
		String uuid = uuidInRunde(spielrunde, Position.from(spalte, zeile));
		return MeldelisteUuidIndex.lese(wkingSpreadsheet).nr(uuid).orElseThrow();
	}

	private void ergebnisSetzen(XSpreadsheet spielrunde, int zeile, int ergA, int ergB) throws GenerateException {
		RangeData ergebnis = new RangeData();
		ergebnis.addNewRow(ergA, ergB);
		RangeHelper.from(spielrunde, wkingSpreadsheet.getWorkingSpreadsheetDocument(),
				ergebnis.getRangePosition(Position.from(SchweizerAbstractSpielrundeSheet.ERG_TEAM_A_SPALTE, zeile)))
				.setDataInRange(ergebnis);
	}

	/**
	 * Stellt den Zustand einer vor der UUID-Umstellung erzeugten Datei nach: Die Anzeigeformeln enthalten die
	 * Teamnummer als Literal oder – mit technischen Spalten – als Bezug auf die ausgeblendete Nummer daneben.
	 */
	private void alteSpielrundeNachstellen(XSpreadsheet spielrunde, int anzTeams, SchweizerKonfigurationSheet konfig,
			boolean mitTechnischenSpalten) throws GenerateException {
		int ersteZeile = SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE;
		int anzPaarungen = anzTeams / 2;
		RangeData nummern = new RangeData();
		String[][] formeln = new String[anzPaarungen][2];
		for (int idx = 0; idx < anzPaarungen; idx++) {
			int zeile = ersteZeile + idx;
			int nrA = teamNrInRunde(spielrunde, SchweizerAbstractSpielrundeSheet.TEAM_A_SPALTE, zeile);
			int nrB = teamNrInRunde(spielrunde, SchweizerAbstractSpielrundeSheet.TEAM_B_SPALTE, zeile);
			nummern.addNewRow(nrA, nrB);
			formeln[idx][0] = alteAnzeigeFormel(mitTechnischenSpalten
					? Position.from(SchweizerAbstractSpielrundeSheet.TECHNISCHE_TEAM_A_NR_SPALTE, zeile).getAddress()
					: String.valueOf(nrA), konfig);
			formeln[idx][1] = alteAnzeigeFormel(mitTechnischenSpalten
					? Position.from(SchweizerAbstractSpielrundeSheet.TECHNISCHE_TEAM_B_NR_SPALTE, zeile).getAddress()
					: String.valueOf(nrB), konfig);
		}
		if (mitTechnischenSpalten) {
			RangeHelper.from(spielrunde, wkingSpreadsheet.getWorkingSpreadsheetDocument(), nummern.getRangePosition(
					Position.from(SchweizerAbstractSpielrundeSheet.TECHNISCHE_TEAM_A_NR_SPALTE, ersteZeile)))
					.setDataInRange(nummern);
		}
		sheetHlp.setFormulaArrayInRange(spielrunde, RangePosition.from(SchweizerAbstractSpielrundeSheet.TEAM_A_SPALTE,
				ersteZeile, SchweizerAbstractSpielrundeSheet.TEAM_B_SPALTE, ersteZeile + anzPaarungen - 1), formeln);
		recalcAll();
	}

	private static String alteAnzeigeFormel(String teamNrAusdruck, SchweizerKonfigurationSheet konfig) {
		return TeamAnzeigeHelper.formel(teamNrAusdruck, konfig.getSpielplanTeamAnzeige(),
				konfig.isMeldeListeTeamnameAnzeigen(), konfig.getMeldeListeFormation(),
				konfig.isMeldeListeVereinsnameAnzeigen());
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

	private int siegeVonTeam(int anzTeams, int teamNr) throws GenerateException {
		XSpreadsheet rangliste = sheetHlp.findByName(SheetNamen.rangliste());
		var daten = RangeHelper.from(rangliste, wkingSpreadsheet.getWorkingSpreadsheetDocument(), RangePosition.from(
				SchweizerRanglisteSheet.TEAM_NR_SPALTE, SchweizerRanglisteSheet.ERSTE_DATEN_ZEILE,
				SchweizerRanglisteSheet.SIEGE_SPALTE,
				SchweizerRanglisteSheet.ERSTE_DATEN_ZEILE + anzTeams - 1)).getDataFromRange();
		int spalteSiege = SchweizerRanglisteSheet.SIEGE_SPALTE - SchweizerRanglisteSheet.TEAM_NR_SPALTE;
		return daten.stream().filter(zeile -> zeile.get(0).getIntVal(0) == teamNr).findFirst()
				.map(zeile -> zeile.get(spalteSiege).getIntVal(0))
				.orElseThrow(() -> new IllegalStateException("Team " + teamNr + " nicht in der Rangliste"));
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
