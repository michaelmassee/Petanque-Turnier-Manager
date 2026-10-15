/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.addins;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.sun.star.awt.XTopWindow;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XModel;
import com.sun.star.sheet.XCalculatable;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.comp.OfficeDocumentHelper;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.SheetHelper;

/**
 * {@code PTM.ALG.UUIDZEILE}, {@code PTM.ALG.UUIDTEAMNR} und {@code PTM.ALG.UUID.TEAMANZEIGE} lösen eine UUID nur im
 * übergebenen Bereich ihres eigenen Dokuments auf. Zwei offene Dokumente führen dieselbe Nr für verschiedene Teams
 * und dieselbe UUID unter verschiedener Nr und Zeile; der Fokus liegt jeweils auf dem anderen Dokument.
 * <p>
 * Anders als {@link GlobalImplMehrDokumenteUITest} wird hier über Zellformeln geprüft: Die Funktionen lesen keine
 * Properties, sondern nur den übergebenen Bereich, der Wert der Zelle ist daher verlässlich.
 */
public class UuidFunktionenMehrDokumenteUITest extends BaseCalcUITest {

	private static final String UUID_A = "1a2b3c4d-0000-4000-8000-00000000000a";
	private static final String UUID_B = "1a2b3c4d-0000-4000-8000-00000000000b";
	private static final String NR_BEREICH = "$A$1:$A$20";
	private static final String TEAM_BEREICH = "$A$1:$C$20";
	private static final int GLEICHE_NR = 7;

	private static final Position ZEILE_ZELLE = Position.from(5, 0);
	private static final Position NR_ZELLE = Position.from(6, 0);
	private static final Position ANZEIGE_ZELLE = Position.from(7, 0);

	private XSpreadsheetDocument zweitesDokument;

	@AfterEach
	public void schliesseZweitesDokument() {
		if (zweitesDokument != null) {
			OfficeDocumentHelper.closeDoc(zweitesDokument);
			zweitesDokument = null;
		}
	}

	@Test
	public void uuidWirdNurImEigenenDokumentAufgeloest() {
		// Dokument A: Team A in Zeile 3 mit Nr 7, Team B in Zeile 5 mit Nr 9.
		XSpreadsheet sheetA = sheetHlp.getSheetByIdx(0);
		schreibeTeam(sheetHlp, sheetA, 2, UUID_A, GLEICHE_NR, "Anna", "Albatros");
		schreibeTeam(sheetHlp, sheetA, 4, UUID_B, 9, "Bert", "Boule");
		schreibeAbfragen(sheetHlp, sheetA);

		// Dokument B: Nr 7 gehört Team B, Team A steht in Zeile 6 mit Nr 12 und anderem Namen.
		zweitesDokument = OfficeDocumentHelper.from(loader).createSichtbaresCalc();
		assertThat(zweitesDokument).as("zweites Dokument konnte nicht erstellt werden").isNotNull();
		SheetHelper sheetHlpB = new SheetHelper(starter.getxComponentContext(), zweitesDokument);
		XSpreadsheet sheetB = sheetHlpB.getSheetByIdx(0);
		schreibeTeam(sheetHlpB, sheetB, 2, UUID_B, GLEICHE_NR, "Bert", "Boule");
		schreibeTeam(sheetHlpB, sheetB, 5, UUID_A, 12, "Clara", "Calc");
		schreibeAbfragen(sheetHlpB, sheetB);

		fokussiere(zweitesDokument);
		neuBerechnen(doc);
		pruefe(sheetHlp, sheetA, 3, GLEICHE_NR, "Anna Albatros");

		fokussiere(doc);
		neuBerechnen(zweitesDokument);
		pruefe(sheetHlpB, sheetB, 6, 12, "Clara Calc");

		// Eine Änderung in B wirkt nicht auf A: kein dokumentübergreifend zwischengespeichertes Ergebnis.
		schreibeTeam(sheetHlpB, sheetB, 5, UUID_A, 15, "Clara", "Calc");
		fokussiere(zweitesDokument);
		neuBerechnen(doc);
		pruefe(sheetHlp, sheetA, 3, GLEICHE_NR, "Anna Albatros");
		neuBerechnen(zweitesDokument);
		pruefe(sheetHlpB, sheetB, 6, 15, "Clara Calc");
	}

	private static void schreibeTeam(SheetHelper helper, XSpreadsheet sheet, int zeile, String uuid, int nr,
			String vorname, String nachname) {
		helper.setFormulaInCell(sheet, Position.from(0, zeile), GlobalImpl.FORMAT_PTM_TNR_UUID(nr, uuid));
		helper.setStringValueInCell(StringCellValue.from(sheet, Position.from(1, zeile), vorname));
		helper.setStringValueInCell(StringCellValue.from(sheet, Position.from(2, zeile), nachname));
	}

	/** Alle drei Abfragen suchen Team A. */
	private static void schreibeAbfragen(SheetHelper helper, XSpreadsheet sheet) {
		String uuid = "\"" + UUID_A + "\"";
		helper.setFormulaInCell(sheet, ZEILE_ZELLE, GlobalImpl.FORMAT_PTM_UUID_ZEILE(uuid, NR_BEREICH));
		helper.setFormulaInCell(sheet, NR_ZELLE, GlobalImpl.FORMAT_PTM_UUID_TEAMNR(uuid, NR_BEREICH));
		helper.setFormulaInCell(sheet, ANZEIGE_ZELLE,
				GlobalImpl.FORMAT_PTM_UUID_TEAM_ANZEIGE(false, 1, false, uuid, TEAM_BEREICH, false));
	}

	private static void pruefe(SheetHelper helper, XSpreadsheet sheet, int zeile, int nr, String anzeige) {
		assertThat(helper.getIntFromCell(sheet, ZEILE_ZELLE)).as("UUIDZEILE").isEqualTo(zeile);
		assertThat(helper.getIntFromCell(sheet, NR_ZELLE)).as("UUIDTEAMNR").isEqualTo(nr);
		assertThat(helper.getTextFromCell(sheet, ANZEIGE_ZELLE)).as("UUID.TEAMANZEIGE").isEqualTo(anzeige);
	}

	private static void neuBerechnen(XSpreadsheetDocument dokument) {
		Lo.qi(XCalculatable.class, dokument).calculateAll();
	}

	/** Gleiche Fokus-Sequenz wie {@code TurnierSystemNeueDateiAuswahlDialog.fokussiereDokument()}. */
	private static void fokussiere(XSpreadsheetDocument dokument) {
		XFrame frame = Lo.qi(XModel.class, dokument).getCurrentController().getFrame();
		frame.activate();
		XTopWindow topWindow = Lo.qi(XTopWindow.class, frame.getContainerWindow());
		if (topWindow != null) {
			topWindow.toFront();
		}
	}
}
