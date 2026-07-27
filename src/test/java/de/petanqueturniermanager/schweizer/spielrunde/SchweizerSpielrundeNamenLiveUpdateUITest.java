package de.petanqueturniermanager.schweizer.spielrunde;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.schweizer.konfiguration.SpielplanTeamAnzeige;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetTestDaten;

/**
 * Regression: Im Anzeigemodus {@link SpielplanTeamAnzeige#NAME} muss eine Umbenennung eines
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
		spielrundeNaechste.getKonfigurationSheet().setSpielplanTeamAnzeige(SpielplanTeamAnzeige.NAME);
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

}
