/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper.sheet;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.star.beans.XPropertySet;
import com.sun.star.sheet.XSheetCondition;
import com.sun.star.sheet.XSheetConditionalEntries;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.XCell;

import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.position.Position;

/**
 * Prüf-Hilfe für UITests: wertet die Fehlerregel (erste bedingte Formatierung) einer Zelle in LibreOffice
 * tatsächlich aus. Ein reiner Textvergleich der Formel genügt nicht – eine syntaktisch vorhandene Regel kann
 * zur Laufzeit einen Fehler liefern (z.B. Err:504) und greift dann nie.
 * <p>
 * Ausgewertet wird wie in LibreOffice: Die relativen Zeilenbezüge ({@code $B4}) gelten ab der Quellposition der
 * Bedingung und werden auf die zu prüfende Zeile umgerechnet, absolute ({@code $B$4}) bleiben. Eine falsche
 * Quellposition (Zeilenversatz) fällt dadurch auf. Spaltenbezüge der Regel müssen absolut sein;
 * {@link ConditionalFormatHelper#FORMULA_CURRENT_CELL} wird durch die Adresse der geprüften Zelle ersetzt, weil
 * die Hilfszelle in einer anderen Spalte liegt.
 */
public final class FehlerregelAuswertung {

	/** Weit rechts liegende, im Test ungenutzte Spalte für die Hilfszelle. */
	private static final int HILFS_SPALTE = 200;

	/** Bezug mit absoluter Spalte und relativer Zeile, z.B. {@code $B4} – nicht {@code $B$4}. */
	private static final Pattern RELATIVE_ZEILE = Pattern.compile("(\\$[A-Z]{1,3})(\\d+)");

	private FehlerregelAuswertung() {
		// Hilfsklasse – kein Instantiieren
	}

	/**
	 * @return {@code true} wenn die Fehlerregel der Zelle für diese Zeile WAHR ergibt; schlägt fehl, wenn die
	 *         Regel nicht fehlerfrei auswertbar ist
	 */
	public static boolean greift(XSpreadsheet sheet, int spalte, int zeile) throws Exception {
		XPropertySet properties = Lo.qi(XPropertySet.class, sheet.getCellByPosition(spalte, zeile));
		XSheetConditionalEntries entries = Lo.qi(XSheetConditionalEntries.class,
				properties.getPropertyValue("ConditionalFormat"));
		assertThat(entries.getCount()).as("Zelle %d/%d hat keine bedingte Formatierung", spalte, zeile).isPositive();
		XSheetCondition fehlerregel = Lo.qi(XSheetCondition.class, entries.getByIndex(0));
		String formel = fuerZeile(fehlerregel.getFormula1(), zeile - fehlerregel.getSourcePosition().Row)
				.replace(ConditionalFormatHelper.FORMULA_CURRENT_CELL, Position.from(spalte, zeile).getAddressWith$());

		XCell hilfszelle = sheet.getCellByPosition(HILFS_SPALTE, zeile);
		try {
			hilfszelle.setFormula("=IF(" + formel + ";1;0)");
			assertThat(hilfszelle.getError()).as("Fehlerregel in Zelle %d/%d ist nicht auswertbar: %s", spalte, zeile,
					formel).isZero();
			return hilfszelle.getValue() == 1;
		} finally {
			hilfszelle.setFormula("");
		}
	}

	/** Verschiebt alle relativen Zeilenbezüge der Formel um {@code versatz} Zeilen. */
	static String fuerZeile(String formel, int versatz) {
		Matcher matcher = RELATIVE_ZEILE.matcher(formel);
		StringBuilder ergebnis = new StringBuilder();
		while (matcher.find()) {
			int zeileEinsBasiert = Integer.parseInt(matcher.group(2));
			matcher.appendReplacement(ergebnis, Matcher.quoteReplacement(matcher.group(1) + (zeileEinsBasiert + versatz)));
		}
		matcher.appendTail(ergebnis);
		return ergebnis.toString();
	}
}
