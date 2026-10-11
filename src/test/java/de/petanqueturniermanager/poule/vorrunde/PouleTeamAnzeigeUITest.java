/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.poule.vorrunde;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.text.XText;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.meldeliste.TeamAnzeige;
import de.petanqueturniermanager.basesheet.meldeliste.TeamAnzeigeHelper;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.ErwarteteSpielernamen;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.poule.PouleTurnierTestDaten;
import de.petanqueturniermanager.poule.konfiguration.PouleKonfigurationSheet;
import de.petanqueturniermanager.poule.rangliste.PouleVorrundenRanglisteSheet;

/** Sichtbarkeit der Namensspalten in Poule-Vorrunde und -Rangliste je Team-Anzeige. */
class PouleTeamAnzeigeUITest extends BaseCalcUITest {

	private static final int ANZ_TEAMS = 8;
	/** Wie {@code PouleListeDelegate.ERSTE_DATEN_ZEILE} (package-private). */
	private static final int MELDELISTE_ERSTE_DATEN_ZEILE = 3;

	@Test
	void vorrundeZeigtStandardmaessigNamenWieBisher() throws GenerateException {
		new PouleTurnierTestDaten(wkingSpreadsheet, ANZ_TEAMS).generate();

		XSpreadsheet vorrunde = sheet(SheetMetadataHelper.SCHLUESSEL_POULE_VORRUNDE);
		assertThat(istSpalteSichtbar(vorrunde, AbstractPouleVorrundeSheet.SPALTE_TEAM_A_NAME))
				.as("Bestehende Poule-Turniere ohne gespeicherte Spielplan-Anzeige zeigen weiterhin Namen")
				.isTrue();
	}

	@Test
	void ranglisteBlendetNamensspalteImNummernModusAus() throws GenerateException {
		new PouleTurnierTestDaten(wkingSpreadsheet, ANZ_TEAMS).generate();
		var konfig = new PouleKonfigurationSheet(wkingSpreadsheet);

		konfig.setRanglisteTeamAnzeige(TeamAnzeige.NR);
		new PouleVorrundenRanglisteSheet(wkingSpreadsheet).run();
		XSpreadsheet rangliste = sheet(SheetMetadataHelper.SCHLUESSEL_POULE_VORRUNDEN_RANGLISTE);
		assertThat(istSpalteSichtbar(rangliste, PouleVorrundenRanglisteSheet.SPALTE_NAME)).isFalse();

		konfig.setRanglisteTeamAnzeige(TeamAnzeige.SPIELERNAMEN);
		new PouleVorrundenRanglisteSheet(wkingSpreadsheet).run();
		rangliste = sheet(SheetMetadataHelper.SCHLUESSEL_POULE_VORRUNDEN_RANGLISTE);
		assertThat(istSpalteSichtbar(rangliste, PouleVorrundenRanglisteSheet.SPALTE_NAME)).isTrue();
	}

	/**
	 * Regressionstest: Die Kopfzeile der Team-Namensspalten zeigte den rohen i18n-Key
	 * ("column.header.teamname") statt des übersetzten Textes.
	 */
	@Test
	void vorrundeKopfzeileZeigtUebersetzteTexteStattI18nKeys() throws Exception {
		new PouleTurnierTestDaten(wkingSpreadsheet, ANZ_TEAMS).generate();
		var konfig = new PouleKonfigurationSheet(wkingSpreadsheet);
		String erwarteterTeamHeader = I18n.get(TeamAnzeigeHelper.headerI18nKey(konfig.getSpielplanTeamAnzeige()));

		XSpreadsheet vorrunde = sheet(SheetMetadataHelper.SCHLUESSEL_POULE_VORRUNDE);
		assertThat(zellText(vorrunde, AbstractPouleVorrundeSheet.SPALTE_TEAM_A_NAME, 1)).isEqualTo(erwarteterTeamHeader)
				.doesNotStartWith("column.header");
		assertThat(zellText(vorrunde, AbstractPouleVorrundeSheet.SPALTE_TEAM_B_NAME, 1)).isEqualTo(erwarteterTeamHeader);
		assertThat(zellText(vorrunde, AbstractPouleVorrundeSheet.SPALTE_ERG_A, 1))
				.isEqualTo(I18n.get("poule.vorrunde.header.ergebnis.a"));
		assertThat(zellText(vorrunde, AbstractPouleVorrundeSheet.SPALTE_ERG_A + 1, 1))
				.isEqualTo(I18n.get("poule.vorrunde.header.ergebnis.b"));
	}

	/**
	 * Regressionstest: Mit aktiver Teamname-Spalte las die Spielernamen-Anzeige den Teamnamen als
	 * Vornamen von Spieler 1 und verschob alle Spielerspalten.
	 */
	@Test
	void spielernamenMitTeamnameSpalteBeginnenBeiSpieler1() throws GenerateException {
		new PouleTurnierTestDaten(wkingSpreadsheet, ANZ_TEAMS).generate();
		var konfig = new PouleKonfigurationSheet(wkingSpreadsheet);
		assertThat(konfig.isMeldeListeTeamnameAnzeigen()).as("Vorbedingung: Testdaten mit Teamname-Spalte")
				.isTrue();
		konfig.setSpielplanTeamAnzeige(TeamAnzeige.SPIELERNAMEN);
		konfig.setRanglisteTeamAnzeige(TeamAnzeige.SPIELERNAMEN);
		new PouleVorrundenRanglisteSheet(wkingSpreadsheet).run();
		new PouleVorrundeSheet(wkingSpreadsheet).doRun();

		Map<Integer, String> erwartet = ErwarteteSpielernamen.ausMeldeliste(
				sheetHlp.findByName(SheetNamen.pouleMeldeliste()), doc, MELDELISTE_ERSTE_DATEN_ZEILE, ANZ_TEAMS,
				konfig.getMeldeListeFormation().getAnzSpieler(), konfig.isMeldeListeVereinsnameAnzeigen());

		pruefeAnzeige("Vorrunde", sheet(SheetMetadataHelper.SCHLUESSEL_POULE_VORRUNDE),
				AbstractPouleVorrundeSheet.SPALTE_TEAM_A_NR, AbstractPouleVorrundeSheet.SPALTE_TEAM_A_NAME,
				AbstractPouleVorrundeSheet.ERSTE_DATEN_ZEILE, erwartet);
		pruefeAnzeige("Rangliste", sheet(SheetMetadataHelper.SCHLUESSEL_POULE_VORRUNDEN_RANGLISTE),
				PouleVorrundenRanglisteSheet.SPALTE_NR, PouleVorrundenRanglisteSheet.SPALTE_NAME,
				PouleVorrundenRanglisteSheet.HEADER_ZEILEN, erwartet);
	}

	private void pruefeAnzeige(String blatt, XSpreadsheet sheet, int nrSpalte, int nameSpalte, int zeile,
			Map<Integer, String> erwartet) {
		int nr = sheetHlp.getIntFromCell(sheet, Position.from(nrSpalte, zeile));
		assertThat(erwartet).as("%s: Teamnummer", blatt).containsKey(nr);
		assertThat(sheetHlp.getTextFromCell(sheet, Position.from(nameSpalte, zeile)))
				.as("%s: Spielernamen-Anzeige muss bei Spieler 1 beginnen, nicht beim Teamnamen", blatt)
				.isEqualTo(erwartet.get(nr));
	}

	private static String zellText(XSpreadsheet sheet, int spalte, int zeile) throws Exception {
		return Lo.qi(XText.class, sheet.getCellByPosition(spalte, zeile)).getString();
	}

	private XSpreadsheet sheet(String schluessel) {
		XSpreadsheet sheet = SheetMetadataHelper.findeSheetUndHeile(wkingSpreadsheet.getWorkingSpreadsheetDocument(),
				schluessel, null);
		assertThat(sheet).as("Sheet %s", schluessel).isNotNull();
		return sheet;
	}
}
