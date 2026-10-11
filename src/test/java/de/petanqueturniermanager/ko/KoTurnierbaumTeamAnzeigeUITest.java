/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ko;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.meldeliste.TeamAnzeige;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.ErwarteteSpielernamen;
import de.petanqueturniermanager.ko.konfiguration.KoKonfigurationSheet;
import de.petanqueturniermanager.ko.konfiguration.KoPropertiesSpalte;
import de.petanqueturniermanager.ko.meldeliste.KoMeldeListeSheetTestDaten;

/**
 * Team-Anzeige im K.-o.-Spielbaum: zusammengesetzte Spielernamen (auch bei aktiver Teamname-Spalte)
 * und der Fallback für ältere Dateien, in denen „Teamname" gespeichert ist, die Meldeliste aber
 * keine Teamname-Spalte führt.
 */
class KoTurnierbaumTeamAnzeigeUITest extends BaseCalcUITest {

	private static final int ANZ_TEAMS = 8;
	/** Wie {@code KoListeDelegate.ERSTE_DATEN_ZEILE} (package-private). */
	private static final int MELDELISTE_ERSTE_DATEN_ZEILE = 3;

	@Test
	void spielernamenWerdenImSpielbaumZusammengesetzt() throws GenerateException {
		var konfig = new KoKonfigurationSheet(wkingSpreadsheet);
		konfig.setMeldeListeTeamnameAnzeigen(false);
		konfig.setSpielbaumTeamAnzeige(TeamAnzeige.SPIELERNAMEN);

		String eintrag = ersterTeamEintragImSpielbaum();

		assertThat(eintrag).as("Spielernamen aller Teammitglieder").contains(" / ");
	}

	@Test
	void gespeicherterTeamnameOhneTeamnameSpalteZeigtWeiterhinSpielernamen() throws GenerateException {
		var konfig = new KoKonfigurationSheet(wkingSpreadsheet);
		konfig.setMeldeListeTeamnameAnzeigen(false);
		// Zustand einer älteren Datei: NAME gespeichert, ohne dass der Setter die Teamname-Spalte aktiviert.
		docPropHelper.setStringProperty(KoPropertiesSpalte.KONFIG_PROP_SPIELBAUM_TEAM_ANZEIGE, TeamAnzeige.NAME.name());

		String eintrag = ersterTeamEintragImSpielbaum();

		assertThat(eintrag).as("Fallback auf Spielernamen statt leerer Teamname-Spalte").contains(" / ");
	}

	@Test
	void spielernamenMitTeamnameSpalteBeginnenBeiSpieler1() throws GenerateException {
		var konfig = new KoKonfigurationSheet(wkingSpreadsheet);
		konfig.setMeldeListeTeamnameAnzeigen(true);
		konfig.setMeldeListeVereinsnameAnzeigen(true);
		konfig.setSpielbaumTeamAnzeige(TeamAnzeige.SPIELERNAMEN);

		String eintrag = ersterTeamEintragImSpielbaum();

		Map<Integer, String> erwartet = ErwarteteSpielernamen.ausMeldeliste(
				sheetHlp.findByName(SheetNamen.meldeliste()), doc, MELDELISTE_ERSTE_DATEN_ZEILE, ANZ_TEAMS,
				konfig.getMeldeListeFormation().getAnzSpieler(), true);
		assertThat(erwartet.values())
				.as("Spielernamen-Anzeige muss bei Spieler 1 beginnen, nicht beim Teamnamen")
				.contains(eintrag);
	}

	private String ersterTeamEintragImSpielbaum() throws GenerateException {
		new KoMeldeListeSheetTestDaten(wkingSpreadsheet, ANZ_TEAMS).erstelleMeldelisteWithTestdaten();
		var turnierbaum = new KoTurnierbaumSheet(wkingSpreadsheet);
		turnierbaum.erstelleTurnierbaumOhneDialog();
		recalcAll();

		String praefix = SheetNamen.koTurnierbaumEinzel();
		String sheetName = Arrays.stream(doc.getSheets().getElementNames()).filter(n -> n.startsWith(praefix))
				.findFirst().orElseThrow();
		XSpreadsheet sheet = sheetHlp.findByName(sheetName);
		return sheetHlp.getTextFromCell(sheet,
				Position.from(turnierbaum.teamSpalte(1), turnierbaum.teamAZeile(1, 0)));
	}
}
