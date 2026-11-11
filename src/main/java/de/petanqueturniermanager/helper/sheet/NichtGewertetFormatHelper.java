/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper.sheet;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.beans.PropertyState;
import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.XPropertySet;
import com.sun.star.sheet.ConditionOperator;
import com.sun.star.sheet.XCellRangeAddressable;
import com.sun.star.sheet.XConditionalFormat;
import com.sun.star.sheet.XConditionalFormats;
import com.sun.star.sheet.XSheetCellRanges;
import com.sun.star.sheet.XSheetCondition2;
import com.sun.star.sheet.XSheetConditionalEntries;
import com.sun.star.sheet.XSheetConditionalEntry;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.table.CellAddress;
import com.sun.star.table.CellRangeAddress;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.cellstyle.CellStyleHelper;
import de.petanqueturniermanager.helper.cellstyle.NichtGewertetStyle;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzManager;

/**
 * Bedingte Formatierung „Spielrunde wird nicht gewertet“ über die ganze Tabelle eines
 * Spielrunden-Blatts (grau, durchgestrichen, {@link NichtGewertetStyle}).
 * <p>
 * Die Regel muss sich gegen die vorhandenen Fehler- und Editier-Regeln durchsetzen. Dabei gelten
 * folgende LO-Eigenheiten (LO-Quelle {@code sc/}):
 * <ul>
 * <li>Die Range-Property {@code ConditionalFormat} ({@code cellsuno.cxx}) entfernt beim Setzen
 * <em>alle</em> bedingten Formatierungen des Bereichs und ersetzt sie – ein Overlay über die ganze
 * Tabelle würde die Fehlerregeln zerstören.</li>
 * <li>Liegen mehrere Formate auf einer Zelle, gewinnt das mit dem kleinsten Schlüssel
 * ({@code ScDocument::GetCondResult}) – ein neues Zusatzformat hätte die niedrigste Priorität.</li>
 * <li>Die neue API {@code XConditionalFormat.createEntry} ignoriert die Position (hängt immer an),
 * {@code removeByIndex} wirft wegen verdrehter Bereichsprüfung, {@code removeByID} lässt verwaiste
 * Indizes in den Zellattributen zurück.</li>
 * </ul>
 * Deshalb wird je vorhandenem Formatbereich (ermittelt über die Blatt-Property
 * {@code ConditionalFormats}) die Regel über die alte API <em>vorangestellt</em>: Einträge lesen,
 * leeren, eigene Regel zuerst, bisherige Regeln dahinter. Zellen ohne jede bedingte Formatierung
 * erhalten die Regel allein. Beim Entfernen werden die bisherigen Regeln ohne die eigene neu
 * geschrieben.
 */
public final class NichtGewertetFormatHelper {

	private static final Logger logger = LogManager.getLogger(NichtGewertetFormatHelper.class);

	private static final String PROP_CONDITIONAL_FORMAT = "ConditionalFormat";
	private static final String PROP_CONDITIONAL_FORMATS = "ConditionalFormats";
	private static final String PROP_RANGE = "Range";

	/** Bedingung der Regel: immer wahr – die Regel existiert nur, solange die Runde nicht gewertet ist. */
	static final String FORMEL_IMMER_WAHR = "1";

	private NichtGewertetFormatHelper() {
		// Utility-Klasse – keine Instanzen
	}

	/**
	 * Legt die Regel „nicht gewertet“ über den Tabellenbereich und stellt sie dabei allen vorhandenen
	 * Regeln voran. Idempotent.
	 */
	public static void anwenden(XSpreadsheetDocument xDoc, XSpreadsheet xSheet, RangePosition tabelle)
			throws GenerateException {
		checkNotNull(xDoc);
		checkNotNull(xSheet);
		checkNotNull(tabelle);
		CellStyleHelper.from(xDoc, new NichtGewertetStyle()).apply();
		BlattschutzManager.get().ensureUnprotectedInScope();

		List<RangePosition> formatBereiche = formatBereicheInTabelle(xSheet, tabelle);
		for (RangePosition bereich : formatBereiche) {
			voranstellenVorhandenenRegeln(xDoc, xSheet, bereich);
		}
		for (RangePosition bereich : freieBereiche(tabelle, formatBereiche)) {
			voranstellen(xDoc, xSheet, bereich, leseEintraege(xDoc, xSheet, bereich));
		}
	}

	/**
	 * Entfernt die Regel „nicht gewertet“ aus dem Tabellenbereich; alle anderen Regeln bleiben in
	 * ihrer Reihenfolge erhalten.
	 */
	public static void entfernen(XSpreadsheetDocument xDoc, XSpreadsheet xSheet, RangePosition tabelle)
			throws GenerateException {
		checkNotNull(xDoc);
		checkNotNull(xSheet);
		checkNotNull(tabelle);
		BlattschutzManager.get().ensureUnprotectedInScope();
		for (RangePosition bereich : formatBereicheInTabelle(xSheet, tabelle)) {
			ohneNichtGewertetNeuSchreiben(xDoc, xSheet, bereich);
		}
	}

	/**
	 * Prüft, ob die erste wirksame Regel der Zelle die Regel „nicht gewertet“ ist.
	 */
	public static boolean istVorangestellt(XSpreadsheetDocument xDoc, XSpreadsheet xSheet, int spalte, int zeile)
			throws GenerateException {
		XSheetConditionalEntries eintraege = leseEintraege(xDoc, xSheet, RangePosition.from(spalte, zeile, spalte, zeile));
		return eintraege.getCount() > 0 && istNichtGewertetEintrag(eintrag(eintraege, 0));
	}

	/**
	 * Zerlegt die Zellen des Tabellenbereichs, die von keinem der {@code belegt}-Bereiche abgedeckt
	 * sind, in spaltenweise zusammenhängende Abschnitte.
	 */
	static List<RangePosition> freieBereiche(RangePosition tabelle, List<RangePosition> belegt) {
		List<RangePosition> frei = new ArrayList<>();
		for (int spalte = tabelle.getStartSpalte(); spalte <= tabelle.getEndeSpalte(); spalte++) {
			Integer abschnittStart = null;
			for (int zeile = tabelle.getStartZeile(); zeile <= tabelle.getEndeZeile(); zeile++) {
				boolean istFrei = !istAbgedeckt(belegt, spalte, zeile);
				if (istFrei && abschnittStart == null) {
					abschnittStart = zeile;
				} else if (!istFrei && abschnittStart != null) {
					frei.add(RangePosition.from(spalte, abschnittStart, spalte, zeile - 1));
					abschnittStart = null;
				}
			}
			if (abschnittStart != null) {
				frei.add(RangePosition.from(spalte, abschnittStart, spalte, tabelle.getEndeZeile()));
			}
		}
		return frei;
	}

	/**
	 * Schnittmenge zweier Bereiche, leer wenn sie sich nicht überlappen.
	 */
	static Optional<RangePosition> schnitt(RangePosition a, RangePosition b) {
		int startSpalte = Math.max(a.getStartSpalte(), b.getStartSpalte());
		int endeSpalte = Math.min(a.getEndeSpalte(), b.getEndeSpalte());
		int startZeile = Math.max(a.getStartZeile(), b.getStartZeile());
		int endeZeile = Math.min(a.getEndeZeile(), b.getEndeZeile());
		if (startSpalte > endeSpalte || startZeile > endeZeile) {
			return Optional.empty();
		}
		return Optional.of(RangePosition.from(startSpalte, startZeile, endeSpalte, endeZeile));
	}

	private static boolean istAbgedeckt(List<RangePosition> bereiche, int spalte, int zeile) {
		return bereiche.stream()
				.anyMatch(bereich -> spalte >= bereich.getStartSpalte() && spalte <= bereich.getEndeSpalte()
						&& zeile >= bereich.getStartZeile() && zeile <= bereich.getEndeZeile());
	}

	/**
	 * Alle Rechtecke vorhandener bedingter Formatierungen des Blatts, beschnitten auf den
	 * Tabellenbereich.
	 */
	private static List<RangePosition> formatBereicheInTabelle(XSpreadsheet xSheet, RangePosition tabelle)
			throws GenerateException {
		List<RangePosition> bereiche = new ArrayList<>();
		try {
			XPropertySet sheetProps = Lo.qi(XPropertySet.class, xSheet);
			XConditionalFormats formate = Lo.qi(XConditionalFormats.class,
					sheetProps.getPropertyValue(PROP_CONDITIONAL_FORMATS));
			for (XConditionalFormat format : formate.getConditionalFormats()) {
				XPropertySet formatProps = Lo.qi(XPropertySet.class, format);
				XSheetCellRanges ranges = Lo.qi(XSheetCellRanges.class, formatProps.getPropertyValue(PROP_RANGE));
				for (CellRangeAddress adresse : ranges.getRangeAddresses()) {
					schnitt(tabelle, RangePosition.from(adresse.StartColumn, adresse.StartRow, adresse.EndColumn,
							adresse.EndRow)).ifPresent(bereiche::add);
				}
			}
		} catch (com.sun.star.uno.Exception e) {
			logger.error(e.getMessage(), e);
			throw new GenerateException("Bedingte Formatierungen konnten nicht gelesen werden: " + e.getMessage());
		}
		return bereiche;
	}

	private static void voranstellenVorhandenenRegeln(XSpreadsheetDocument xDoc, XSpreadsheet xSheet,
			RangePosition bereich) throws GenerateException {
		XSheetConditionalEntries eintraege = leseEintraege(xDoc, xSheet, bereich);
		if (eintraege.getCount() == 0) {
			// Bereich mit unterschiedlich belegten Zellen: die alte API liefert dann keine Einträge, ein
			// Schreiben würde alle Regeln des Bereichs löschen. Lieber ohne Kennzeichnung lassen.
			logger.warn("Bedingte Formatierung im Bereich {} nicht einheitlich – Regel 'nicht gewertet' "
					+ "wird dort nicht vorangestellt", bereich);
			return;
		}
		voranstellen(xDoc, xSheet, bereich, eintraege);
	}

	private static void voranstellen(XSpreadsheetDocument xDoc, XSpreadsheet xSheet, RangePosition bereich,
			XSheetConditionalEntries eintraege) throws GenerateException {
		List<PropertyValue[]> bisher = bisherigeBedingungen(eintraege);
		if (bisher.size() < eintraege.getCount()) {
			// Regel bereits vorhanden (z.B. überlappende Formatbereiche)
			return;
		}
		eintraege.clear();
		eintraege.addNew(nichtGewertetBedingung(xSheet, bereich));
		bisher.forEach(eintraege::addNew);
		schreibeEintraege(xDoc, xSheet, bereich, eintraege);
	}

	private static void ohneNichtGewertetNeuSchreiben(XSpreadsheetDocument xDoc, XSpreadsheet xSheet,
			RangePosition bereich) throws GenerateException {
		XSheetConditionalEntries eintraege = leseEintraege(xDoc, xSheet, bereich);
		List<PropertyValue[]> bisher = bisherigeBedingungen(eintraege);
		if (bisher.size() == eintraege.getCount()) {
			return;
		}
		eintraege.clear();
		bisher.forEach(eintraege::addNew);
		schreibeEintraege(xDoc, xSheet, bereich, eintraege);
	}

	/** Alle Bedingungen außer der Regel „nicht gewertet“, in ihrer Reihenfolge. */
	private static List<PropertyValue[]> bisherigeBedingungen(XSheetConditionalEntries eintraege)
			throws GenerateException {
		List<PropertyValue[]> bedingungen = new ArrayList<>();
		for (int i = 0; i < eintraege.getCount(); i++) {
			Object eintrag = eintrag(eintraege, i);
			if (!istNichtGewertetEintrag(eintrag)) {
				bedingungen.add(alsBedingung(eintrag));
			}
		}
		return bedingungen;
	}

	private static boolean istNichtGewertetEintrag(Object eintrag) {
		XSheetConditionalEntry entry = Lo.qi(XSheetConditionalEntry.class, eintrag);
		return entry != null && new NichtGewertetStyle().getName().equals(entry.getStyleName());
	}

	private static PropertyValue[] alsBedingung(Object eintrag) {
		XSheetCondition2 bedingung = Lo.qi(XSheetCondition2.class, eintrag);
		XSheetConditionalEntry entry = Lo.qi(XSheetConditionalEntry.class, eintrag);
		return new PropertyValue[] {
				property("Operator", bedingung.getConditionOperator()),
				property("Formula1", bedingung.getFormula1()),
				property("Formula2", bedingung.getFormula2()),
				property("SourcePosition", bedingung.getSourcePosition()),
				property("StyleName", entry.getStyleName()) };
	}

	private static PropertyValue[] nichtGewertetBedingung(XSpreadsheet xSheet, RangePosition bereich) {
		short sheetIndex = Lo.qi(XCellRangeAddressable.class, xSheet).getRangeAddress().Sheet;
		return new PropertyValue[] {
				property("Operator", ConditionOperator.FORMULA),
				property("Formula1", FORMEL_IMMER_WAHR),
				property("SourcePosition",
						new CellAddress(sheetIndex, bereich.getStartSpalte(), bereich.getStartZeile())),
				property("StyleName", new NichtGewertetStyle().getName()) };
	}

	private static PropertyValue property(String name, Object wert) {
		return new PropertyValue(name, 0, wert, PropertyState.DIRECT_VALUE);
	}

	private static Object eintrag(XSheetConditionalEntries eintraege, int index) throws GenerateException {
		try {
			return eintraege.getByIndex(index);
		} catch (com.sun.star.uno.Exception e) {
			logger.error(e.getMessage(), e);
			throw new GenerateException("Bedingte Formatierung konnte nicht gelesen werden: " + e.getMessage());
		}
	}

	private static XSheetConditionalEntries leseEintraege(XSpreadsheetDocument xDoc, XSpreadsheet xSheet,
			RangePosition bereich) throws GenerateException {
		try {
			XPropertySet props = RangeHelper.from(xSheet, xDoc, bereich).getPropertySet();
			return Lo.qi(XSheetConditionalEntries.class, props.getPropertyValue(PROP_CONDITIONAL_FORMAT));
		} catch (com.sun.star.uno.Exception e) {
			logger.error(e.getMessage(), e);
			throw new GenerateException("Bedingte Formatierung konnte nicht gelesen werden: " + e.getMessage());
		}
	}

	private static void schreibeEintraege(XSpreadsheetDocument xDoc, XSpreadsheet xSheet, RangePosition bereich,
			XSheetConditionalEntries eintraege) throws GenerateException {
		try {
			XPropertySet props = RangeHelper.from(xSheet, xDoc, bereich).getPropertySet();
			props.setPropertyValue(PROP_CONDITIONAL_FORMAT, eintraege);
		} catch (com.sun.star.uno.Exception e) {
			logger.error(e.getMessage(), e);
			throw new GenerateException("Bedingte Formatierung konnte nicht geschrieben werden: " + e.getMessage());
		}
	}
}
