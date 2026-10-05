/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper.sheet;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.beans.XPropertySet;
import com.sun.star.sheet.ConditionOperator;
import com.sun.star.sheet.TableValidationVisibility;
import com.sun.star.sheet.ValidationAlertStyle;
import com.sun.star.sheet.ValidationType;
import com.sun.star.sheet.XSheetCondition;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.XCellRange;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.position.RangePosition;

/**
 * Native Calc-Datengültigkeit für manuelle Eingabezellen (Übersicht und Regeln:
 * {@code turniersysteme/DATENGUELTIGKEIT.md}).
 * <p>
 * Leere Zellen sind immer erlaubt. Programmatische Schreibvorgänge umgehen die Prüfung.
 */
public final class DatengueltigkeitHelper {

	private static final Logger logger = LogManager.getLogger(DatengueltigkeitHelper.class);

	/** Höchste Punktzahl eines Spiels (Spielziel 13), sofern das System kein eigenes Spielziel kennt. */
	public static final int MAX_SPIELPUNKTE = 13;

	/** Aktuelle Zelle in einer Gültigkeitsformel, unabhängig von der Position im Bereich. */
	private static final String AKTUELLE_ZELLE = "INDIRECT(ADDRESS(ROW();COLUMN()))";

	/** Leer, Text (Bahnbezeichnung) oder positive Ganzzahl. */
	static final String BAHN_FORMEL = "OR(ISBLANK(" + AKTUELLE_ZELLE + ");ISTEXT(" + AKTUELLE_ZELLE
			+ ");AND(ISNUMBER(" + AKTUELLE_ZELLE + ");" + AKTUELLE_ZELLE + "=INT(" + AKTUELLE_ZELLE + ");"
			+ AKTUELLE_ZELLE + ">0))";

	private static final Meldungen ZAHL_MELDUNGEN = new Meldungen("datengueltigkeit.zahl.eingabehilfe.titel",
			"datengueltigkeit.zahl.eingabehilfe", "datengueltigkeit.zahl.fehler.titel",
			"datengueltigkeit.zahl.fehler", "datengueltigkeit.spalte.zahl");

	private static final Meldungen BAHN_MELDUNGEN = new Meldungen("datengueltigkeit.bahn.eingabehilfe.titel",
			"datengueltigkeit.bahn.eingabehilfe", "datengueltigkeit.bahn.fehler.titel",
			"datengueltigkeit.bahn.fehler", "datengueltigkeit.spalte.bahn");

	/**
	 * I18n-Schlüssel der Texte einer Gültigkeitsregel.
	 *
	 * @param spaltenKey Bezeichnung der Spalte für die Fehlermeldung, falls die Regel nicht gesetzt werden kann
	 */
	public record Meldungen(String eingabeTitelKey, String eingabeKey, String fehlerTitelKey, String fehlerKey,
			String spaltenKey) {
	}

	private record Regel(ValidationType typ, ConditionOperator operator, String formel1, String formel2,
			ValidationAlertStyle fehlerStil, boolean auswahlliste, Meldungen meldungen) {
	}

	private DatengueltigkeitHelper() {
	}

	public static void setzeGanzzahlBereich(ISheet sheet, RangePosition range, int minimum, int maximum)
			throws GenerateException {
		setze(sheet, range, new Regel(ValidationType.WHOLE, ConditionOperator.BETWEEN, String.valueOf(minimum),
				String.valueOf(maximum), ValidationAlertStyle.STOP, false, ZAHL_MELDUNGEN));
	}

	public static void setzeSpielpunkte(ISheet sheet, RangePosition range) throws GenerateException {
		setzeGanzzahlBereich(sheet, range, 0, MAX_SPIELPUNKTE);
	}

	public static void setzeNichtNegativeGanzzahl(ISheet sheet, RangePosition range) throws GenerateException {
		setze(sheet, range, new Regel(ValidationType.WHOLE, ConditionOperator.GREATER_EQUAL, "0", "",
				ValidationAlertStyle.STOP, false, ZAHL_MELDUNGEN));
	}

	/**
	 * Bewusst lockere Prüfung für manuell eingetragene Spielbahnen: erlaubt sind positive Ganzzahlen
	 * und beliebiger Text (Bahnbezeichnungen wie „A3"). Andere Werte (0, negativ, Kommazahl) lösen nur
	 * eine bestätigbare Warnung aus. Doppelte Bahnen werden nicht abgelehnt – das würde das Tauschen
	 * zweier Bahnen blockieren –, sondern nur über die bedingte Formatierung rot markiert.
	 */
	public static void setzeBahnDatengueltigkeit(ISheet sheet, RangePosition range) throws GenerateException {
		setze(sheet, range, new Regel(ValidationType.CUSTOM, ConditionOperator.FORMULA, BAHN_FORMEL, "",
				ValidationAlertStyle.WARNING, false, BAHN_MELDUNGEN));
	}

	/** Auswahlliste aus Zahlen, z.B. die Aktiv-Werte 1 und 2. */
	public static void setzeZahlenListe(ISheet sheet, RangePosition range, List<Integer> werte, Meldungen meldungen)
			throws GenerateException {
		setzeListe(sheet, range, werte.stream().map(String::valueOf).collect(Collectors.joining(";")), meldungen);
	}

	/** Auswahlliste aus Texten, z.B. die Check-in-Markierung. */
	public static void setzeTextListe(ISheet sheet, RangePosition range, List<String> werte, Meldungen meldungen)
			throws GenerateException {
		setzeListe(sheet, range, werte.stream().map(wert -> "\"" + wert.replace("\"", "\"\"") + "\"")
				.collect(Collectors.joining(";")), meldungen);
	}

	private static void setzeListe(ISheet sheet, RangePosition range, String eintraege, Meldungen meldungen)
			throws GenerateException {
		// Eine Listen-Gültigkeit erwartet eine Calc-Formel, die eine Liste/Matrix liefert. "1;2" bzw.
		// "X" allein wird als unvollständige Formel gelesen (Err:509); die geschweiften Klammern
		// machen daraus eine gültige einspaltige Array-Konstante.
		setze(sheet, range, new Regel(ValidationType.LIST, ConditionOperator.EQUAL, "{" + eintraege + "}", "",
				ValidationAlertStyle.STOP, true, meldungen));
	}

	private static void setze(ISheet sheet, RangePosition range, Regel regel) throws GenerateException {
		XPropertySet rangeProperties = rangeProperties(sheet, range);
		try {
			XPropertySet validation = Lo.qi(XPropertySet.class, rangeProperties.getPropertyValue("Validation"));
			validation.setPropertyValue("Type", regel.typ());
			validation.setPropertyValue("IgnoreBlankCells", Boolean.TRUE);
			if (regel.auswahlliste()) {
				validation.setPropertyValue("ShowList", Short.valueOf(TableValidationVisibility.UNSORTED));
			}
			setzeMeldungen(validation, regel.fehlerStil(), regel.meldungen());
			XSheetCondition condition = Lo.qi(XSheetCondition.class, validation);
			condition.setOperator(regel.operator());
			condition.setFormula1(regel.formel1());
			condition.setFormula2(regel.formel2());
			rangeProperties.setPropertyValue("Validation", validation);
		} catch (com.sun.star.uno.Exception | RuntimeException e) {
			String spalte = I18n.get(regel.meldungen().spaltenKey());
			logger.error("Datengültigkeit der {} konnte nicht gesetzt werden", spalte, e);
			throw new GenerateException(I18n.get("error.datengueltigkeit", spalte, e.getMessage()));
		}
	}

	private static void setzeMeldungen(XPropertySet validation, ValidationAlertStyle fehlerStil, Meldungen meldungen)
			throws com.sun.star.uno.Exception {
		validation.setPropertyValue("ShowInputMessage", Boolean.TRUE);
		validation.setPropertyValue("InputTitle", I18n.get(meldungen.eingabeTitelKey()));
		validation.setPropertyValue("InputMessage", I18n.get(meldungen.eingabeKey()));
		validation.setPropertyValue("ShowErrorMessage", Boolean.TRUE);
		validation.setPropertyValue("ErrorAlertStyle", fehlerStil);
		validation.setPropertyValue("ErrorTitle", I18n.get(meldungen.fehlerTitelKey()));
		validation.setPropertyValue("ErrorMessage", I18n.get(meldungen.fehlerKey()));
	}

	private static XPropertySet rangeProperties(ISheet sheet, RangePosition range) throws GenerateException {
		XSpreadsheet xSheet = sheet.getXSpreadSheet();
		try {
			XCellRange xRange = xSheet.getCellRangeByPosition(range.getStartSpalte(), range.getStartZeile(),
					range.getEndeSpalte(), range.getEndeZeile());
			return Lo.qi(XPropertySet.class, xRange);
		} catch (com.sun.star.lang.IndexOutOfBoundsException e) {
			logger.error("Ungültiger Bereich für Datengültigkeit: {}", range, e);
			throw new GenerateException(I18n.get("error.datengueltigkeit", range.getAddress(), e.getMessage()));
		}
	}
}
