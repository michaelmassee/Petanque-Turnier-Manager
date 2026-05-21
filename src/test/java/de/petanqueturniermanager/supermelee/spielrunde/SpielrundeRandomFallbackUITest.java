package de.petanqueturniermanager.supermelee.spielrunde;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleePropertiesSpalte;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_New;
import de.petanqueturniermanager.supermelee.meldeliste.TestSuperMeleeMeldeListeErstellen;

/**
 * Prüft, dass bei einer Spielerkonstellation ohne valide Constraint-Lösung
 * (alle Spieler mit gleicher Setzposition → niemand darf mit niemandem im Team
 * sein) der Auslosungs-Algorithmus scheitert, die Lockerung der Spieltag-Historie
 * keinen Effekt hat (setzPos ist kein Historien-Constraint) und schließlich der
 * Random-Fallback eine Spielrunde ohne Constraint-Prüfung schreibt.
 */
public class SpielrundeRandomFallbackUITest extends BaseCalcUITest {

	private TestSuperMeleeMeldeListeErstellen testMeldeListeErstellen;
	private MeldeListeSheet_New meldeListeSheetNew;

	@BeforeEach
	public void setUp() throws GenerateException {
		testMeldeListeErstellen = new TestSuperMeleeMeldeListeErstellen(wkingSpreadsheet, doc);
		testMeldeListeErstellen.initMitAlleDieSpielen(6);
		meldeListeSheetNew = testMeldeListeErstellen.getMeldeListeSheetNew();
	}

	@Test
	public void zufallsSpielplanWirdErzeugtWennKonstellationUnloesbar() throws GenerateException {
		alleSpielerAufSelbeSetzPosition(6, 1);

		SpielrundeSheet_Naechste spielrundeSheetNaechste = new SpielrundeSheet_Naechste(wkingSpreadsheet);
		spielrundeSheetNaechste.setForceOk(true);
		spielrundeSheetNaechste.run();

		String sheetName = SpielrundeSheetKonstanten.sheetName(1, 1);
		XSpreadsheet spielrundeSheet = sheetHlp.findByName(sheetName);
		assertThat(spielrundeSheet)
				.as("Sheet '%s' muss trotz Algorithmen-Versagen vom Random-Fallback geschrieben werden", sheetName)
				.isNotNull();

		assertThat(docPropHelper.getIntProperty(SuperMeleePropertiesSpalte.KONFIG_PROP_NAME_SPIELRUNDE, -1))
				.as("Aktive Spielrunde muss auf 1 stehen (Random-Fallback ersetzt das Sheet nicht und löscht es nicht)")
				.isEqualTo(1);

		RangePosition spielerNrRange = RangePosition.from(
				SpielrundeSheetKonstanten.ERSTE_SPIELERNR_SPALTE, SpielrundeSheetKonstanten.ERSTE_DATEN_ZEILE,
				SpielrundeSheetKonstanten.LETZTE_SPALTE, SpielrundeSheetKonstanten.ERSTE_DATEN_ZEILE);
		RangeData data = RangeHelper.from(spielrundeSheet, wkingSpreadsheet.getWorkingSpreadsheetDocument(), spielerNrRange)
				.getDataFromRange();

		assertThat(data).as("Eine Bahn (2 Tripletten) bei 6 Spielern").hasSize(1);
		data.get(0).forEach(cell -> assertThat(cell.getStringVal())
				.as("Random-Fallback muss alle 6 Slots der einen Bahn füllen").isNotBlank());
	}

	private void alleSpielerAufSelbeSetzPosition(int anzSpieler, int setzPos) throws GenerateException {
		int ersteDatenZeile = meldeListeSheetNew.getMeldungenSpalte().getErsteDatenZiele();
		int spielerNameErsteSpalte = meldeListeSheetNew.getMeldungenSpalte().getErsteMeldungNameSpalte();
		// Layout: Vorname(+0) | Nachname(+1) | setzPos(+2) | alle_spielen(+3)
		int setzPosSpalte = spielerNameErsteSpalte + 2;
		for (int i = 0; i < anzSpieler; i++) {
			sheetHlp.setValInCell(meldeListeSheetNew.getXSpreadSheet(),
					Position.from(setzPosSpalte, ersteDatenZeile + i), setzPos);
		}
	}
}
