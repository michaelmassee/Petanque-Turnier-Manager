package de.petanqueturniermanager.basesheet.spielrunde;

import java.util.ArrayList;
import java.util.Collections;

import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.CellVertJustify2;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.CellProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;

/**
 * Erstellung 31.03.2024 / Michael Massee
 */

public class SpielrundeHelper {

	private final ISheet sheet;

	public SpielrundeHelper(ISheet sheet) {
		this.sheet = sheet;

	}

	/**
	 * enweder einfach ein laufende nummer, oder jenachdem was in der konfig steht die Spielbahnnummer<br>
	 * property getSpielrundeSpielbahn<br>
	 * X = nur ein laufende paarungen nummer<br>
	 * L = Spielbahn -> leere Spalte<br>
	 * N = Spielbahn -> durchnumeriert<br>
	 * R = Spielbahn -> random<br>
	 *
	 * @throws GenerateException
	 */
	public void datenErsteSpalte(SpielrundeSpielbahn spielrundeSpielbahnFlagAusKonfig, int erstZeile, int letzteZeile,
			int nrSpalte, int headerZeile, Integer headerColor) throws GenerateException {

		sheet.processBoxinfo("Erste Spalte Daten einfügen");

		Position posLetztDatenZelle = Position.from(nrSpalte, letzteZeile);
		Position posErsteDatenZelle = Position.from(nrSpalte, erstZeile);
		Position posErsteHeaderZelle = Position.from(nrSpalte, headerZeile);

		// header
		// -------------------------
		// spalte paarungen Nr oder Spielbahn-Nummer
		// -------------------------
		ColumnProperties columnProperties = ColumnProperties.from().setVertJustify(CellVertJustify2.CENTER)
				.setHoriJustify(CellHoriJustify.CENTER);
		if (spielrundeSpielbahnFlagAusKonfig == SpielrundeSpielbahn.X) {
			columnProperties.setWidth(500); // Paarungen cntr
			sheet.getSheetHelper().setColumnProperties(sheet, nrSpalte, columnProperties);
		} else {
			// Spielbahn Spalte header
			columnProperties.setWidth(900); // Paarungen cntr
			StringCellValue headerValue = StringCellValue.from(sheet, posErsteHeaderZelle).setRotateAngle(27000)
					.setVertJustify(CellVertJustify2.CENTER).setBorder(BorderFactory.from().allThin().toBorder())
					.setCellBackColor(headerColor).setCharHeight(14).setColumnProperties(columnProperties)
					.setEndPosMergeZeilePlus(1).setValue("Bahn").setComment("Spielbahn");
			sheet.getSheetHelper().setStringValueInCell(headerValue);

			RangePosition nbrRange = RangePosition.from(posErsteHeaderZelle, posLetztDatenZelle);
			sheet.getSheetHelper().setPropertiesInRange(sheet, nbrRange, CellProperties.from().setCharHeight(16));
		}

		// Daten
		if (spielrundeSpielbahnFlagAusKonfig == SpielrundeSpielbahn.X
				|| spielrundeSpielbahnFlagAusKonfig == SpielrundeSpielbahn.N) {
			StringCellValue formulaCellValue = StringCellValue.from(sheet, posErsteDatenZelle);
			formulaCellValue.setValue("=ROW()-" + erstZeile).setFillAutoDown(letzteZeile);
			sheet.getSheetHelper().setFormulaInCell(formulaCellValue);
		} else if (spielrundeSpielbahnFlagAusKonfig == SpielrundeSpielbahn.R) {
			// R = Spielbahn -> random x 
			// anzahl paarungen ?
			int anzPaarungen = letzteZeile - erstZeile + 1;

			ArrayList<Integer> bahnnummern = new ArrayList<>();
			// fill
			for (int i = 1; i <= anzPaarungen; i++) {
				bahnnummern.add(i);
			}
			// mishen
			Collections.shuffle(bahnnummern);
			StringCellValue stringCellValue = StringCellValue.from(sheet, posErsteDatenZelle);
			for (Integer bahnnr : bahnnummern) {
				if (bahnnr > 0) { // es kann sein das wir lücken haben, = teampaarungen ohne bahnnummer
					stringCellValue.setValue(bahnnr);
					sheet.getSheetHelper().setStringValueInCell(stringCellValue);
				}
				stringCellValue.zeilePlusEins();
			}
		}
	}

}
