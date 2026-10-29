/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.schweizer.spielrunde;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetTestDaten;

/**
 * Regression: Ein nur einseitig eingetragenes Ergebnis (13 : leer) galt als vollständig, die nächste Runde
 * wurde ausgelost und das Spiel als 13:0 gewertet.
 */
class SchweizerEinseitigesErgebnisUITest extends BaseCalcUITest {

	private static final int ERSTE_ZEILE = SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE;

	@Test
	void einseitigesErgebnisVerhindertNaechsteRunde() throws Exception {
		XSpreadsheet ersteRunde = ersteRundeAnlegen();
		ergebnisseEintragen(ersteRunde, false);

		new SchweizerSpielrundeSheetNaechste(wkingSpreadsheet).doRun();

		assertThat(SheetMetadataHelper.findeSheet(doc, SheetMetadataHelper.schluesselSchweizerSpielrunde(2)))
				.as("ein Ergebnis fehlt – keine 2. Spielrunde").isEmpty();
	}

	@Test
	void vollstaendigeErgebnisseMitNullPunktenErlaubenNaechsteRunde() throws Exception {
		XSpreadsheet ersteRunde = ersteRundeAnlegen();
		ergebnisseEintragen(ersteRunde, true);

		new SchweizerSpielrundeSheetNaechste(wkingSpreadsheet).doRun();

		assertThat(SheetMetadataHelper.findeSheet(doc, SheetMetadataHelper.schluesselSchweizerSpielrunde(2)))
				.as("13:0 ist ein vollständiges Ergebnis").isPresent();
	}

	private XSpreadsheet ersteRundeAnlegen() throws Exception {
		new SchweizerMeldeListeSheetTestDaten(wkingSpreadsheet).doRun();
		SchweizerSpielrundeSheetNaechste spielrunde = new SchweizerSpielrundeSheetNaechste(wkingSpreadsheet);
		spielrunde.getKonfigurationSheet().setSpielrundeSpielbahn(SpielrundeSpielbahn.R);
		spielrunde.doRun();
		return spielrunde.getXSpreadSheet();
	}

	/**
	 * Trägt in jede Paarung 13:7 ein, in die erste 13 : 0 bzw. 13 : leer. Freilos-Zeilen bleiben unberührt.
	 */
	private void ergebnisseEintragen(XSpreadsheet sheet, boolean ersteVollstaendig) throws Exception {
		RangeData paarungen = RangeHelper.from(sheet, doc, RangePosition.from(SchweizerAbstractSpielrundeSheet.TEAM_A_SPALTE,
				ERSTE_ZEILE, SchweizerAbstractSpielrundeSheet.TEAM_B_SPALTE, ERSTE_ZEILE + 99)).getDataFromRange();
		RangeData ergebnisse = new RangeData();
		for (RowData paarung : paarungen) {
			if (StringUtils.isBlank(paarung.get(0).getStringVal())) {
				break;
			}
			RowData ergebnis = ergebnisse.addNewRow();
			if (StringUtils.isBlank(paarung.get(1).getStringVal())) {
				ergebnis.newEmpty();
				ergebnis.newEmpty();
			} else if (ergebnisse.size() == 1) {
				ergebnis.newInt(13);
				if (ersteVollstaendig) {
					ergebnis.newInt(0);
				} else {
					ergebnis.newEmpty();
				}
			} else {
				ergebnis.newInt(13);
				ergebnis.newInt(7);
			}
		}
		RangeHelper.from(sheet, doc, ergebnisse.getRangePosition(
				Position.from(SchweizerAbstractSpielrundeSheet.ERG_TEAM_A_SPALTE, ERSTE_ZEILE))).setDataInRange(ergebnisse);
	}
}
