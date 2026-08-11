/*
 * Erstellung 03.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

import com.sun.star.sheet.ConditionOperator;

import de.petanqueturniermanager.basesheet.SheetTabFarben;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.cellstyle.EditierbareZelleHintergrundFarbeGeradeStyle;
import de.petanqueturniermanager.helper.cellstyle.EditierbareZelleHintergrundFarbeUnGeradeStyle;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.ConditionalFormatHelper;
import de.petanqueturniermanager.helper.sheet.EditierbaresZelleFormatHelper;

/**
 * Gemeinsame Konstanten für alle Meldelisten-Sheets.
 */
public interface MeldeListeKonstanten {

	int SHEET_COLOR = SheetTabFarben.MELDELISTE;

	/** Gibt den lokalisierten Tabellennamen zurück. */
	static String sheetName() {
		return SheetNamen.meldeliste();
	}

	int CELL_MARGIN = 120;

	int ERSTE_DATEN_ZEILE = 2; // Zeile 3
	int SPIELER_NR_SPALTE = 0; // Spalte A=0
	int ERSTE_HEADER_ZEILE = ERSTE_DATEN_ZEILE - 2; // Zeile 1
	int ZWEITE_HEADER_ZEILE = ERSTE_DATEN_ZEILE - 1; // Zeile 2

	/**
	 * Formation "Nur Teamname": Teamname ist die einzige Team-Identität - doppelte Teamnamen
	 * live wie bei der Team-Nr-Spalte mit rotem Zellhintergrund markieren. Muss NACH
	 * EditierbaresZelleFormatHelper.anwenden() UND mit .clear() auf die (an dieser Stelle
	 * homogen mit dessen Zebra-Regeln belegte) Teamname-Spalte neu aufgebaut werden: anwenden()
	 * liest/schreibt "ConditionalFormat" für den gesamten editierbaren Bereich als Ganzes und
	 * würde eine zuvor nur auf die Teamname-Spalte gesetzte Regel sonst überschreiben.
	 */
	static void markiereDoppelteTeamnamenBeiNurTeamname(ISheet sheet, boolean teamnameAnzeigen,
			boolean nurTeamname, int letzteDatenZeile) throws GenerateException {
		if (!teamnameAnzeigen || !nurTeamname) {
			return;
		}
		RangePosition teamnameRange = RangePosition.from(1, ERSTE_DATEN_ZEILE, 1, letzteDatenZeile);
		String kondDoppeltTeamname = "AND(NOT(ISBLANK(" + ConditionalFormatHelper.FORMULA_CURRENT_CELL
				+ "));COUNTIF(" + Position.from(1, 0).getSpalteAddressWith$() + ";"
				+ ConditionalFormatHelper.FORMULA_CURRENT_CELL + ")>1)";
		var geradeStyle = new EditierbareZelleHintergrundFarbeGeradeStyle(
				EditierbaresZelleFormatHelper.EDITIERBAR_GERADE_FARBE);
		var ungeradeStyle = new EditierbareZelleHintergrundFarbeUnGeradeStyle(
				EditierbaresZelleFormatHelper.EDITIERBAR_UNGERADE_FARBE);
		ConditionalFormatHelper.from(sheet, teamnameRange).clear()
				.formula1(kondDoppeltTeamname).operator(ConditionOperator.FORMULA).styleIsFehler()
				.applyAndDoReset()
				.formulaIsEvenRowAndBoolProp(EditierbaresZelleFormatHelper.PROPERTY_KEY).style(geradeStyle)
				.applyAndDoReset()
				.formulaIsOddRowAndBoolProp(EditierbaresZelleFormatHelper.PROPERTY_KEY).style(ungeradeStyle)
				.applyAndDoReset();
	}

}
