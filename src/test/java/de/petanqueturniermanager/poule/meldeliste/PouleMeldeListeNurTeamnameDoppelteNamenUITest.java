/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.poule.meldeliste;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sun.star.beans.XPropertySet;
import com.sun.star.sheet.XSheetCondition;
import com.sun.star.sheet.XSheetConditionalEntries;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.XCell;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.position.Position;

/**
 * Regressionstest analog {@code SchweizerMeldeListeNurTeamnameDoppelteNamenUITest}: bei
 * Formation.NUR_TEAMNAME muss die Teamname-Spalte - genau wie die Team-Nr-Spalte - über eine
 * live nachgeführte bedingte Formatierung (roter Zellhintergrund) markiert werden.
 */
class PouleMeldeListeNurTeamnameDoppelteNamenUITest extends BaseCalcUITest {

	@Test
	void teamnameSpalteHatLiveDuplikatPruefungWieTeamNrSpalte() throws Exception {
		PouleMeldeListeSheetNew meldeListeNew = new PouleMeldeListeSheetNew(wkingSpreadsheet);
		meldeListeNew.createMeldelisteWithParams(Formation.NUR_TEAMNAME, false, false);

		int ersteDatenZeile = PouleListeDelegate.ERSTE_DATEN_ZEILE;
		int teamnameSpalte = meldeListeNew.getTeamnameSpalte();
		XSpreadsheet xSheet = meldeListeNew.getXSpreadSheet();

		String formel = ladeErsteConditionalFormatFormel(xSheet, Position.from(teamnameSpalte, ersteDatenZeile));
		assertThat(formel)
				.as("Teamname-Spalte muss bei Formation NUR_TEAMNAME eine COUNTIF-Duplikat-Prüfung als "
						+ "bedingte Formatierung haben (live, wie die Team-Nr-Spalte)")
				.containsIgnoringCase("COUNTIF");
	}

	private String ladeErsteConditionalFormatFormel(XSpreadsheet sheet, Position pos) throws GenerateException {
		try {
			XCell xCell = sheet.getCellByPosition(pos.getSpalte(), pos.getZeile());
			XPropertySet xPropSet = Lo.qi(XPropertySet.class, xCell);
			XSheetConditionalEntries xEntries = Lo.qi(XSheetConditionalEntries.class,
					xPropSet.getPropertyValue("ConditionalFormat"));
			assertThat(xEntries.getCount()).as("Zelle %s muss eine bedingte Formatierung haben", pos.getAddress())
					.isGreaterThan(0);
			XSheetCondition xCondition = Lo.qi(XSheetCondition.class, xEntries.getByIndex(0));
			return xCondition.getFormula1();
		} catch (Exception e) {
			throw new GenerateException(e.getMessage());
		}
	}
}
