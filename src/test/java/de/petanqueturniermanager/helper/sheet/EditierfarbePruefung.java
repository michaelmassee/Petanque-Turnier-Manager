/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper.sheet;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.XPropertySet;
import com.sun.star.sheet.ConditionOperator;
import com.sun.star.sheet.XSheetCondition;
import com.sun.star.sheet.XSheetConditionalEntries;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.table.XCellRange;

import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;

/**
 * Prüf-Hilfen für UITests rund um Zebra-Zeilenfarbe und Editierfarbe.
 * <p>
 * Regel: Zebra wird direkt als Zellhintergrund geschrieben, nie als bedingte Formatierung;
 * editierbare Zellen tragen die Editierfarbe ({@link EditierbaresZelleFormatHelper}) als bedingte
 * Formatierung, die durch keine unbedingte Regel verdeckt werden darf.
 */
public final class EditierfarbePruefung {

	private EditierfarbePruefung() {
		// Hilfsklasse – kein Instantiieren
	}

	/**
	 * Prüft eine Meldeliste: Nr-Spalte (0) mit direktem Zebra ohne Zebra-CF, alle Spalten
	 * 1..{@code letzteEditierbareSpalte} in den ersten beiden Datenzeilen als editierbar
	 * hervorgehoben.
	 */
	public static void pruefeMeldeliste(XSpreadsheet sheet, int ersteDatenZeile, int letzteEditierbareSpalte,
			int geradeFarbe, int ungeradeFarbe) throws Exception {
		pruefeZebraDirekt(sheet, 0, ersteDatenZeile, geradeFarbe, ungeradeFarbe);
		for (int spalte = 1; spalte <= letzteEditierbareSpalte; spalte++) {
			for (int zeile = ersteDatenZeile; zeile <= ersteDatenZeile + 1; zeile++) {
				assertEditierbarHervorgehoben(sheet, Position.from(spalte, zeile));
			}
		}
	}

	/**
	 * Die Zelle trägt die Editierfarbe als bedingte Formatierung und keine reine Zebra-Regel, die
	 * sie verdecken würde.
	 */
	public static void assertEditierbarHervorgehoben(XSpreadsheet sheet, Position pos) throws Exception {
		List<String> formeln = cfFormeln(sheet, pos);
		assertThat(formeln).as("Zelle %s muss als editierbar hervorgehoben sein", pos.getAddress())
				.anySatisfy(formel -> assertThat(formel).contains(EditierbaresZelleFormatHelper.PROPERTY_KEY));
		assertKeineZebraCf(formeln, pos);
	}

	/**
	 * Die ersten beiden Datenzeilen der Spalte haben die Zebrafarben direkt als Zellhintergrund
	 * (keine Zebra-CF).
	 */
	public static void pruefeZebraDirekt(XSpreadsheet sheet, int spalte, int ersteDatenZeile, int geradeFarbe,
			int ungeradeFarbe) throws Exception {
		Position erste = Position.from(spalte, ersteDatenZeile);
		Position zweite = Position.from(spalte, ersteDatenZeile + 1);
		assertKeineZebraCf(cfFormeln(sheet, erste), erste);
		assertKeineZebraCf(cfFormeln(sheet, zweite), zweite);
		assertThat(List.of(hintergrund(sheet, erste), hintergrund(sheet, zweite)))
				.as("Zebra muss in Spalte %d direkt als Zellhintergrund gesetzt sein", spalte)
				.containsExactlyInAnyOrder(geradeFarbe, ungeradeFarbe);
	}

	/** Liest alle Formula1-Ausdrücke der bedingten Formatierung einer Zelle. */
	public static List<String> cfFormeln(XSpreadsheet sheet, Position pos) throws Exception {
		XPropertySet xPropSet = Lo.qi(XPropertySet.class, sheet.getCellByPosition(pos.getSpalte(), pos.getZeile()));
		XSheetConditionalEntries xEntries = Lo.qi(XSheetConditionalEntries.class,
				xPropSet.getPropertyValue("ConditionalFormat"));
		List<String> formeln = new ArrayList<>();
		for (int i = 0; i < xEntries.getCount(); i++) {
			formeln.add(Lo.qi(XSheetCondition.class, xEntries.getByIndex(i)).getFormula1());
		}
		return formeln;
	}

	/**
	 * Schreibt zwei Meldungen (Nr, Teamname, Vor- und Nachname Spieler 1) in eine Meldeliste mit
	 * Teamname-Spalte. Ohne Meldungen würde „Aktualisieren“ die leeren Zeilen komplett leeren
	 * (inkl. bedingter Formatierung) und ein Altdokument-Test nichts aussagen.
	 */
	public static void schreibeZweiMeldungen(XSpreadsheet sheet, XSpreadsheetDocument doc, int ersteDatenZeile)
			throws Exception {
		RangeData data = new RangeData();
		for (int nr = 1; nr <= 2; nr++) {
			RowData zeile = data.addNewRow();
			zeile.newInt(nr);
			zeile.newString("Team " + nr);
			zeile.newString("Vorname " + nr);
			zeile.newString("Nachname " + nr);
		}
		RangeHelper.from(sheet, doc, data.getRangePosition(Position.from(0, ersteDatenZeile))).setDataInRange(data);
	}

	/**
	 * Simuliert ein Altdokument: setzt per Roh-UNO (am Laufzeit-Guard vorbei) eine reine
	 * Zebra-Regel auf eine Datenspalte.
	 */
	public static void setzeAlteZebraCf(XSpreadsheet sheet, int spalte, int ersteZeile, int letzteZeile)
			throws Exception {
		XCellRange range = sheet.getCellRangeByPosition(spalte, ersteZeile, spalte, letzteZeile);
		XPropertySet xPropSet = Lo.qi(XPropertySet.class, range);
		XSheetConditionalEntries xEntries = Lo.qi(XSheetConditionalEntries.class,
				xPropSet.getPropertyValue("ConditionalFormat"));
		xEntries.clear();
		xEntries.addNew(new PropertyValue[] { propertyValue("Operator", ConditionOperator.FORMULA),
				propertyValue("Formula1", ConditionalFormatHelper.FORMULA_ISEVEN_ROW),
				propertyValue("StyleName", "Default") });
		xPropSet.setPropertyValue("ConditionalFormat", xEntries);
		assertThat(cfFormeln(sheet, Position.from(spalte, ersteZeile)))
				.as("Vorbedingung: alte Zebra-CF muss in Spalte %d gesetzt sein", spalte)
				.anyMatch(ConditionalFormatHelper::istReineZebraFormel);
	}

	private static void assertKeineZebraCf(List<String> formeln, Position pos) {
		assertThat(formeln).as("Zelle %s darf keine Zebra-Zeilenfarbe als bedingte Formatierung tragen",
				pos.getAddress()).noneMatch(ConditionalFormatHelper::istReineZebraFormel);
	}

	private static int hintergrund(XSpreadsheet sheet, Position pos) throws Exception {
		XPropertySet xPropSet = Lo.qi(XPropertySet.class, sheet.getCellByPosition(pos.getSpalte(), pos.getZeile()));
		return (Integer) xPropSet.getPropertyValue("CellBackColor");
	}

	private static PropertyValue propertyValue(String name, Object wert) {
		PropertyValue propertyValue = new PropertyValue();
		propertyValue.Name = name;
		propertyValue.Value = wert;
		return propertyValue;
	}
}
