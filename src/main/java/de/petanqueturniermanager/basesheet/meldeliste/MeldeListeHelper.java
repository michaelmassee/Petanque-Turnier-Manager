/*
 * Erstellung 10.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

import com.google.common.annotations.VisibleForTesting;
import com.sun.star.awt.FontWeight;
import com.sun.star.sheet.ConditionOperator;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.CellVertJustify2;

import de.petanqueturniermanager.addins.GlobalImpl;
import de.petanqueturniermanager.exception.DoppelteStartnummerException;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ColorHelper;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.cellstyle.MeldungenHintergrundFarbeGeradeStyle;
import de.petanqueturniermanager.helper.cellstyle.MeldungenHintergrundFarbeUnGeradeStyle;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.CellProperties;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxResult;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.ConditionalFormatHelper;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.SortHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.model.IMeldungen;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;

/**
 * @author Michael Massee
 *
 */
public class MeldeListeHelper<MLD_LIST_TYPE, MLDTYPE> implements MeldeListeKonstanten {

	private final IMeldeliste<MLD_LIST_TYPE, MLDTYPE> meldeListe;
	private final String metadatenSchluessel;

	public MeldeListeHelper(IMeldeliste<MLD_LIST_TYPE, MLDTYPE> newMeldeListe, String metadatenSchluessel) {
		meldeListe = checkNotNull(newMeldeListe);
		checkArgument(StringUtils.isNotBlank(metadatenSchluessel));
		this.metadatenSchluessel = metadatenSchluessel;
	}

	public void insertFormulaFuerDoppelteSpielerNrGeradeUngradeFarbe(int letzteDatenZeile, ISheet sheet,
			MeldungenHintergrundFarbeGeradeStyle meldungenHintergrundFarbeGeradeStyle,
			MeldungenHintergrundFarbeUnGeradeStyle meldungenHintergrundFarbeUnGeradeStyle) throws GenerateException {

		RangePosition nrSetPosRange = RangePosition.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE, SPIELER_NR_SPALTE,
				letzteDatenZeile);
		String conditionfindDoppeltNr = "COUNTIF(" + Position.from(SPIELER_NR_SPALTE, 0).getSpalteAddressWith$() + ";"
				+ ConditionalFormatHelper.FORMULA_CURRENT_CELL + ")>1";
		ConditionalFormatHelper.from(sheet, nrSetPosRange).clear()
				.formulaIsText().styleIsFehler().applyAndDoReset()
				.formula1(conditionfindDoppeltNr).operator(ConditionOperator.FORMULA).styleIsFehler().applyAndDoReset()
				// nr muss >0 und <999 sein
				.formula1("0").formula2("" + MeldungenSpalte.MAX_ANZ_MELDUNGEN)
				.operator(ConditionOperator.NOT_BETWEEN).styleIsFehler().applyAndDoReset();
		SheetHelper.faerbeZeilenAbwechselnd(sheet, nrSetPosRange,
				meldungenHintergrundFarbeGeradeStyle.getFarbe(),
				meldungenHintergrundFarbeUnGeradeStyle.getFarbe());

	}

	public void insertFormulaFuerDoppelteNamenGeradeUngradeFarbe(int erstNameSpalte, int letzteNamespalte,
			int letzteDatenZeile, ISheet sheet,
			MeldungenHintergrundFarbeGeradeStyle meldungenHintergrundFarbeGeradeStyle,
			MeldungenHintergrundFarbeUnGeradeStyle meldungenHintergrundFarbeUnGeradeStyle) throws GenerateException {
		// -----------------------------------------------
		// Spieler Namen prüfen auf doppelte namen
		// -----------------------------------------------
		RangePosition nameSetPosRange = RangePosition.from(erstNameSpalte, ERSTE_DATEN_ZEILE, letzteNamespalte,
				letzteDatenZeile);
		String conditionfindDoppeltNamen;
		if (erstNameSpalte == letzteNamespalte) {
			conditionfindDoppeltNamen = "COUNTIF(" + Position.from(erstNameSpalte, 0).getSpalteAddressWith$() + ";"
					+ ConditionalFormatHelper.FORMULA_CURRENT_CELL + ")>1";
		} else {
			// Mehrere Namens-Spalten (z.B. Vorname+Nachname): Kombination per COUNTIFS prüfen.
			// Spalten absolut, Zeile relativ ($B3 / $C3) damit die Formel pro Zeile mitläuft.
			final int ankerZeile = ERSTE_DATEN_ZEILE + 1; // 1-basiert für Zell-Adresse
			final int endZeile = MeldungenSpalte.MAX_ANZ_MELDUNGEN + 1;
			StringBuilder sb = new StringBuilder("COUNTIFS(");
			for (int sp = erstNameSpalte; sp <= letzteNamespalte; sp++) {
				if (sp > erstNameSpalte) {
					sb.append(';');
				}
				String spalteAbs = Position.from(sp, 0).getSpalteAddressWith$();
				sb.append(spalteAbs).append(ankerZeile).append(":").append(spalteAbs).append(endZeile).append(';')
						.append(spalteAbs).append(ankerZeile);
			}
			sb.append(")>1");
			conditionfindDoppeltNamen = sb.toString();
		}
		ConditionalFormatHelper.from(sheet, nameSetPosRange).clear()
				.formula1(conditionfindDoppeltNamen).operator(ConditionOperator.FORMULA).styleIsFehler()
				.applyAndDoReset();
		SheetHelper.faerbeZeilenAbwechselnd(sheet, nameSetPosRange,
				meldungenHintergrundFarbeGeradeStyle.getFarbe(),
				meldungenHintergrundFarbeUnGeradeStyle.getFarbe());
		// -----------------------------------------------
	}

	public void insertFormulaSetzpositionGeradeUngradeFarbe(int letzteDatenZeile, ISheet sheet,
			MeldungenHintergrundFarbeGeradeStyle geradeStyle,
			MeldungenHintergrundFarbeUnGeradeStyle ungeradeStyle) throws GenerateException {
		int setzposSpalte = setzPositionSpalte();
		var setzposRangePos = RangePosition.from(setzposSpalte, ERSTE_DATEN_ZEILE, setzposSpalte, letzteDatenZeile);
		ConditionalFormatHelper.from(sheet, setzposRangePos).clear()
				.formula1(setzpositionUngueltigFormel()).operator(ConditionOperator.FORMULA)
				.styleIsFehler().applyAndDoReset();
		SheetHelper.faerbeZeilenAbwechselnd(sheet, setzposRangePos,
				geradeStyle.getFarbe(), ungeradeStyle.getFarbe());
	}

	public void insertFormulaSpieltageSpaltenGeradeUngradeFarbe(int letzteDatenZeile, int letzteSpieltagSpalte,
			ISheet sheet, MeldungenHintergrundFarbeGeradeStyle geradeStyle,
			MeldungenHintergrundFarbeUnGeradeStyle ungeradeStyle) throws GenerateException {
		var spieltageRangePos = RangePosition.from(ersteSpieltagSpalte(), ERSTE_DATEN_ZEILE,
				letzteSpieltagSpalte, letzteDatenZeile);
		ConditionalFormatHelper.from(sheet, spieltageRangePos).clear()
				.formula1("0").formula2("2").operator(ConditionOperator.NOT_BETWEEN).styleIsFehler().applyAndDoReset()
				.formulaIsText().styleIsFehler().applyAndDoReset();
		SheetHelper.faerbeZeilenAbwechselnd(sheet, spieltageRangePos,
				geradeStyle.getFarbe(), ungeradeStyle.getFarbe());
	}

	/**
	 * Löscht Werte in der Aktiv-Spalte, die weder leer noch in {@code gueltigeWerte} enthalten sind –
	 * Gegenstück zur Fehlerfarben-Markierung in {@link #formatiereAktivSpalteFehlerfarbe}, damit ein
	 * Refresh/Vollaufbau ungültige Werte nicht dauerhaft stehen lässt. Zusätzlich wird ein
	 * Aktiv-Wert ohne zugehörige Meldung (Namens-/Teamname-Spalte {@code zeilenKennungSpalte} leer)
	 * gelöscht, sonst bleibt ein "verwaister" Aktiv-Wert stehen, wenn eine Meldung entfernt wird,
	 * ohne die Aktiv-Zelle selbst zu leeren.
	 */
	public void bereinigeUngueltigeAktivWerte(int aktivSpalte, int zeilenKennungSpalte, int ersteDatenZeile,
			int letzteDatenZeile, List<Integer> gueltigeWerte) throws GenerateException {
		XSpreadsheet xSheet = getXSpreadSheet();
		for (int zeile = ersteDatenZeile; zeile <= letzteDatenZeile; zeile++) {
			Position pos = Position.from(aktivSpalte, zeile);
			if (istZelleWirklichLeer(xSheet, pos)) {
				continue;
			}
			String name = meldeListe.getSheetHelper().getTextFromCell(xSheet, Position.from(zeilenKennungSpalte, zeile));
			int wert = meldeListe.getSheetHelper().getIntFromCell(xSheet, pos);
			if (StringUtils.isBlank(name) || !gueltigeWerte.contains(wert)) {
				meldeListe.getSheetHelper().clearValInCell(xSheet, pos);
			}
		}
	}

	/**
	 * Prüft, ob eine Zelle wirklich leer ist (kein Inhalt, auch kein reines Leerzeichen) – im
	 * Unterschied zu {@link de.petanqueturniermanager.helper.sheet.SheetHelper#getTextFromCell}, das
	 * den Inhalt trimmt und eine Zelle mit nur Leerzeichen fälschlich wie eine leere Zelle aussehen
	 * lässt. Ein Leerzeichen ist selbst kein gültiger Aktiv-/SP-Wert und muss beim Refresh bereinigt
	 * statt übersprungen werden.
	 */
	private boolean istZelleWirklichLeer(XSpreadsheet xSheet, Position pos) throws GenerateException {
		var xText = meldeListe.getSheetHelper().getXTextFromCell(xSheet, pos);
		return xText == null || xText.getString() == null || xText.getString().isEmpty();
	}

	/**
	 * Bedingte Formatierung der Aktiv-Spalte: Fehlerfarbe wenn die Zelle weder leer noch in
	 * {@code gueltigeWerte} enthalten ist. Für Systeme, deren Zeilenfarbe nicht über bedingte
	 * Formatierung, sondern direkt gesetzt wird (z.B. Poule).
	 */
	public void formatiereAktivSpalteFehlerfarbe(ISheet sheet, RangePosition aktivRange, List<Integer> gueltigeWerte)
			throws GenerateException {
		ConditionalFormatHelper.from(sheet, aktivRange).clear()
				.formula1(aktivUngueltigFormel(gueltigeWerte)).operator(ConditionOperator.FORMULA)
				.styleIsFehler().applyAndDoReset();
	}

	/**
	 * Wie {@link #formatiereAktivSpalteFehlerfarbe(ISheet, RangePosition, List)}, hängt aber
	 * zusätzlich die Zeilenfarbe (gerade/ungerade) als bedingte Formatierung mit niedrigerer
	 * Priorität an.
	 */
	public void formatiereAktivSpalteFehlerfarbe(ISheet sheet, RangePosition aktivRange, List<Integer> gueltigeWerte,
			MeldungenHintergrundFarbeGeradeStyle farbeGerade, MeldungenHintergrundFarbeUnGeradeStyle farbeUngerade)
			throws GenerateException {
		ConditionalFormatHelper.from(sheet, aktivRange).clear()
				.formula1(aktivUngueltigFormel(gueltigeWerte)).operator(ConditionOperator.FORMULA)
				.styleIsFehler().applyAndDoReset()
				.formulaIsEvenRow().style(farbeGerade).applyAndDoReset()
				.formulaIsOddRow().style(farbeUngerade).applyAndDoReset();
	}

	private static String aktivUngueltigFormel(List<Integer> gueltigeWerte) {
		StringBuilder kondAktivUngueltig = new StringBuilder("AND(NOT(ISBLANK(")
				.append(ConditionalFormatHelper.FORMULA_CURRENT_CELL).append("))");
		for (Integer wert : gueltigeWerte) {
			kondAktivUngueltig.append(';').append(ConditionalFormatHelper.FORMULA_CURRENT_CELL).append("<>").append(wert);
		}
		return kondAktivUngueltig.append(')').toString();
	}

	/**
	 * Löscht Werte in der Setzpositions-Spalte, die weder leer noch eine gültige nicht-negative
	 * Ganzzahl sind – Gegenstück zur Fehlerfarben-Markierung in
	 * {@link #formatiereSetzpositionSpalteFehlerfarbe}, damit ein Refresh/Vollaufbau ungültige Werte
	 * (Text, Dezimalzahlen, negative Zahlen) nicht dauerhaft stehen lässt. 0 bleibt gültig (= "kein
	 * Setzstatus"). Zusätzlich wird eine SP ohne zugehörige Meldung (Namens-/Teamname-Spalte
	 * {@code zeilenKennungSpalte} leer) gelöscht, sonst bleibt ein "verwaister" SP-Wert stehen,
	 * wenn eine Meldung entfernt wird, ohne die SP-Zelle selbst zu leeren.
	 */
	public void bereinigeUngueltigeSetzpositionWerte(int setzposSpalte, int zeilenKennungSpalte, int ersteDatenZeile,
			int letzteDatenZeile) throws GenerateException {
		XSpreadsheet xSheet = getXSpreadSheet();
		for (int zeile = ersteDatenZeile; zeile <= letzteDatenZeile; zeile++) {
			Position pos = Position.from(setzposSpalte, zeile);
			if (istZelleWirklichLeer(xSheet, pos)) {
				continue;
			}
			String name = meldeListe.getSheetHelper().getTextFromCell(xSheet, Position.from(zeilenKennungSpalte, zeile));
			if (StringUtils.isBlank(name) || meldeListe.getSheetHelper().getIntFromCell(xSheet, pos) < 0) {
				meldeListe.getSheetHelper().clearValInCell(xSheet, pos);
			}
		}
	}

	/**
	 * Bedingte Formatierung der Setzpositions-Spalte: Fehlerfarbe wenn die Zelle weder leer noch eine
	 * gültige nicht-negative Ganzzahl ist (0 = "kein Setzstatus", erlaubt und beabsichtigt). Für
	 * Systeme, deren Zeilenfarbe nicht über bedingte Formatierung, sondern direkt gesetzt wird (z.B.
	 * Schweizer/Poule/JGJ).
	 */
	public void formatiereSetzpositionSpalteFehlerfarbe(ISheet sheet, RangePosition spRange)
			throws GenerateException {
		ConditionalFormatHelper.from(sheet, spRange).clear()
				.formula1(setzpositionUngueltigFormel()).operator(ConditionOperator.FORMULA)
				.styleIsFehler().applyAndDoReset();
	}

	/**
	 * Wie {@link #formatiereSetzpositionSpalteFehlerfarbe(ISheet, RangePosition)}, hängt aber
	 * zusätzlich die Zeilenfarbe (gerade/ungerade) als bedingte Formatierung mit niedrigerer
	 * Priorität an.
	 */
	public void formatiereSetzpositionSpalteFehlerfarbe(ISheet sheet, RangePosition spRange,
			MeldungenHintergrundFarbeGeradeStyle farbeGerade, MeldungenHintergrundFarbeUnGeradeStyle farbeUngerade)
			throws GenerateException {
		ConditionalFormatHelper.from(sheet, spRange).clear()
				.formula1(setzpositionUngueltigFormel()).operator(ConditionOperator.FORMULA)
				.styleIsFehler().applyAndDoReset()
				.formulaIsEvenRow().style(farbeGerade).applyAndDoReset()
				.formulaIsOddRow().style(farbeUngerade).applyAndDoReset();
	}

	private static String setzpositionUngueltigFormel() {
		String zelle = ConditionalFormatHelper.FORMULA_CURRENT_CELL;
		// IF-Guard statt OR(NOT(ISNUMBER(..));..): OR() propagiert Fehler aus INT()/<>-Vergleich
		// bei Textwerten (kein Short-Circuit in Calc), wodurch die Formel #VALUE! statt TRUE
		// liefert und die Fehlerfarbe für Text ausbleibt.
		return "AND(NOT(ISBLANK(" + zelle + "));IF(ISNUMBER(" + zelle + ");OR(" + zelle + "<0;" + zelle + "<>INT("
				+ zelle + "));TRUE))";
	}

	/**
	 *
	 * @param spalteNr 0 = erste spalte
	 * @param isAscending
	 * @throws GenerateException
	 */

	public void doSort(int spalteNr, boolean isAscending) throws GenerateException {
		int letzteSpielZeile = meldeListe.getMeldungenSpalte().letzteZeileMitSpielerName();
		if (letzteSpielZeile > meldeListe.getErsteDatenZiele()) { // daten vorhanden
			RangePosition rangeToSort = RangePosition.from(SPIELER_NR_SPALTE, meldeListe.getErsteDatenZiele(),
					meldeListe.letzteSpielTagSpalte(), letzteSpielZeile);
			SortHelper.from(meldeListe, rangeToSort).spalteToSort(spalteNr).aufSteigendSortieren(isAscending).doSort();
		}
	}

	/**
	 * prüft auf doppelte spieler nr oder namen
	 *
	 * @return
	 * @throws GenerateException wenn doppelt daten
	 */
	public void testDoppelteMeldungen() throws GenerateException {
		meldeListe.processBoxinfo("processbox.meldeliste.pruefe.doppelte");
		XSpreadsheet xSheet = getXSpreadSheet();

		int letzteSpielZeile = meldeListe.getMeldungenSpalte().letzteZeileMitSpielerName();
		if (letzteSpielZeile < meldeListe.getErsteDatenZiele()) { // daten vorhanden ?
			return; // keine Daten
		}

		doSort(SPIELER_NR_SPALTE, false); // hoechste nummer oben, ohne nummer nach unten

		// Fehler-Markierung eines vorherigen Laufs zurücksetzen: die Prüfung bricht beim ersten
		// gefundenen Duplikat ab (throw) und markiert daher immer nur die eine Fundstelle. Ohne
		// Reset bliebe eine bereits korrigierte Zeile aus einem früheren Lauf dauerhaft farbig,
		// weil kein Code-Pfad eine nicht mehr doppelte Zeile aktiv zurück auf Schwarz setzt.
		zuruecksetzenFehlerFarbe(letzteSpielZeile);

		// doppelte spieler Nummer entfernen !?!?!
		HashSet<Integer> spielrNrInSheet = new HashSet<>();
		HashSet<String> spielrNamenInSheet = new HashSet<>();

		int spielrNr;
		String spielerName;
		NumberCellValue errCelVal = NumberCellValue
				.from(xSheet, Position.from(SPIELER_NR_SPALTE, meldeListe.getErsteDatenZiele()))
				.setCharColor(ColorHelper.CHAR_COLOR_ORANGE);

		StringCellValue errStrCelVal = StringCellValue
				.from(xSheet, Position.from(meldeListe.getMeldungenSpalte().getNamenOderTeamnameSpalte(),
						meldeListe.getErsteDatenZiele()))
				.setCharColor(ColorHelper.CHAR_COLOR_ORANGE);

		int letzteNamensSpalte = meldeListe.getMeldungenSpalte().getLetzteMeldungNameSpalte();

		for (int spielerZeilecntr = meldeListe.getErsteDatenZiele(); spielerZeilecntr <= letzteSpielZeile; spielerZeilecntr++) {
			// -------------------
			// Spieler nr testen
			// -------------------
			spielrNr = meldeListe.getSheetHelper().getIntFromCell(xSheet,
					Position.from(SPIELER_NR_SPALTE, spielerZeilecntr));
			if (spielrNr > 0) {
				if (spielrNrInSheet.contains(spielrNr)) {
					// Error color
					meldeListe.getSheetHelper()
							.setNumberValueInCell(errCelVal.setValue((double) spielrNr).zeile(spielerZeilecntr));
					throw new DoppelteStartnummerException(spielrNr, I18n.get("error.meldeliste.spieler.nr", spielrNr));
				}
				spielrNrInSheet.add(spielrNr);
			} else {
				// nr ist ungültig einfach löschen
				meldeListe.getSheetHelper().clearValInCell(xSheet, Position.from(SPIELER_NR_SPALTE, spielerZeilecntr));
			}

			// -------------------
			// spieler namen testen
			// -------------------
			// Bei 2 Namens-Spalten (Supermelee Vorname+Nachname) Kombination als Dup-Schlüssel.
			spielerName = meldeListe.getMeldungenSpalte().leseSpielerNameZeile(xSheet, spielerZeilecntr);

			if (StringUtils.isNotEmpty(spielerName)) {
				if (spielrNamenInSheet.contains(cleanUpSpielerName(spielerName))) {
					// Error color in alle Namens-Spalten
					for (int sp = meldeListe.getMeldungenSpalte().getNamenOderTeamnameSpalte();
							sp <= letzteNamensSpalte; sp++) {
						String zellValue = meldeListe.getSheetHelper().getTextFromCell(xSheet,
								Position.from(sp, spielerZeilecntr));
						meldeListe.getSheetHelper().setStringValueInCell(
								errStrCelVal.spalte(sp).setValue(zellValue).zeile(spielerZeilecntr));
					}
					// Erwartbare Eingabe-Validierung (Anwender muss den Namen selbst bereinigen),
					// kein Plugin-Fehler – daher nur als Warnung protokollieren (GenerateException(msg, true)).
					throw new GenerateException(I18n.get("error.meldeliste.spieler.name", spielerName, spielerZeilecntr), true);
				}
				spielrNamenInSheet.add(cleanUpSpielerName(spielerName));
			}
		}
	}

	/**
	 * Setzt die Schriftfarbe der Nr- und Namens-/Teamname-Spalte im Datenbereich blockweise auf
	 * Schwarz zurück - Gegenstück zur Fehler-Markierung in {@link #testDoppelteMeldungen()}, die nur
	 * beim jeweils ersten gefundenen Duplikat greift und daher nie von sich aus wieder zurückgesetzt
	 * wird.
	 */
	private void zuruecksetzenFehlerFarbe(int letzteZeile) throws GenerateException {
		XSpreadsheet xSheet = getXSpreadSheet();
		int ersteZeile = meldeListe.getErsteDatenZiele();
		CellProperties schwarz = CellProperties.from().setCharColor(ColorHelper.CHAR_COLOR_BLACK);

		RangePosition nrRange = RangePosition.from(SPIELER_NR_SPALTE, ersteZeile, SPIELER_NR_SPALTE, letzteZeile);
		meldeListe.getSheetHelper().setPropertiesInRange(xSheet, nrRange, schwarz);

		int ersteNamenSpalte = meldeListe.getMeldungenSpalte().getNamenOderTeamnameSpalte();
		int letzteNamenSpalte = meldeListe.getMeldungenSpalte().getLetzteMeldungNameSpalte();
		if (letzteNamenSpalte >= ersteNamenSpalte) {
			RangePosition namenRange = RangePosition.from(ersteNamenSpalte, ersteZeile, letzteNamenSpalte, letzteZeile);
			meldeListe.getSheetHelper().setPropertiesInRange(xSheet, namenRange, schwarz);
		}
	}

	/**
	 * wie {@link #testDoppelteMeldungen()}, fragt aber bei doppelter Startnummer den
	 * Anwender, ob nur die betroffenen Meldungen automatisch neu nummeriert werden sollen.
	 * Doppelte Spieler-Namen werden weiterhin sofort als Fehler gemeldet.
	 *
	 * @throws GenerateException wenn doppelte Namen gefunden wurden, oder der Anwender die
	 *                           automatische Neu-Nummerierung ablehnt
	 */
	public void pruefeUndKorrigiereDoppelteStartnummern() throws GenerateException {
		while (true) {
			try {
				testDoppelteMeldungen();
				return;
			} catch (DoppelteStartnummerException doppelteStartnummerExc) {
				if (!sollAutomatischNeuNummeriertWerden(doppelteStartnummerExc.getStartnummer())) {
					throw doppelteStartnummerExc;
				}
				korrigiereDoppelteStartnummer(doppelteStartnummerExc.getStartnummer());
			}
		}
	}

	/**
	 * fragt den Anwender, ob die Meldungen mit der übergebenen doppelten Startnummer
	 * automatisch neu nummeriert werden sollen.
	 *
	 * @param doppelteNr die doppelt vergebene Startnummer
	 * @return {@code true} wenn der Anwender die automatische Neu-Nummerierung bestätigt hat
	 */
	@VisibleForTesting
	boolean sollAutomatischNeuNummeriertWerden(int doppelteNr) {
		MessageBoxResult antwort = MessageBox.from(meldeListe.getxContext(), MessageBoxTypeEnum.WARN_YES_NO)
				.caption(I18n.get("msg.caption.doppelte.startnummer"))
				.message(I18n.get("msg.text.doppelte.startnummer.neu.nummerieren", doppelteNr))
				.show();
		return antwort == MessageBoxResult.YES;
	}

	/**
	 * nummeriert nur die Meldungen mit der übergebenen (doppelten) Startnummer neu. Die
	 * erste gefundene Meldung behält ihre Nummer, alle weiteren erhalten neue, bisher
	 * unbenutzte Startnummern.
	 *
	 * @param doppelteNr die doppelt vergebene Startnummer
	 * @throws GenerateException
	 */
	@VisibleForTesting
	void korrigiereDoppelteStartnummer(int doppelteNr) throws GenerateException {
		int letzteSpielZeile = meldeListe.getMeldungenSpalte().letzteZeileMitSpielerName();
		if (letzteSpielZeile < meldeListe.getErsteDatenZiele()) { // daten vorhanden ?
			return; // keine Daten
		}
		XSpreadsheet xSheet = getXSpreadSheet();

		// Alle bereits vergebenen Nummern sammeln, damit die Neuvergabe zuerst freie Lücken
		// nutzt (z.B. durch gelöschte Meldungen entstanden), statt immer nur hochzuzählen.
		HashSet<Integer> belegteNummern = new HashSet<>();
		for (int zeile = meldeListe.getErsteDatenZiele(); zeile <= letzteSpielZeile; zeile++) {
			int spielrNr = meldeListe.getSheetHelper().getIntFromCell(xSheet, Position.from(SPIELER_NR_SPALTE, zeile));
			if (spielrNr > 0) {
				belegteNummern.add(spielrNr);
			}
		}

		NumberCellValue neueNrCelVal = NumberCellValue
				.from(xSheet, Position.from(SPIELER_NR_SPALTE, meldeListe.getErsteDatenZiele()))
				.setCharColor(ColorHelper.CHAR_COLOR_BLACK);

		int naechsteFreieNr = 1;
		boolean ersteFundstelleBehalten = true;
		for (int zeile = meldeListe.getErsteDatenZiele(); zeile <= letzteSpielZeile; zeile++) {
			int spielrNr = meldeListe.getSheetHelper().getIntFromCell(xSheet, Position.from(SPIELER_NR_SPALTE, zeile));
			if (spielrNr != doppelteNr) {
				continue;
			}
			if (ersteFundstelleBehalten) {
				ersteFundstelleBehalten = false;
				continue; // erste Fundstelle behält ihre Nummer
			}
			while (belegteNummern.contains(naechsteFreieNr)) {
				naechsteFreieNr++;
			}
			belegteNummern.add(naechsteFreieNr);
			meldeListe.getSheetHelper()
					.setNumberValueInCell(neueNrCelVal.setValue((double) naechsteFreieNr).zeile(zeile));
		}
	}

	/**
	 * für ein vergleich ,.: und leerzeichen entfernen
	 *
	 * @param name
	 * @return
	 */
	@VisibleForTesting
	String cleanUpSpielerName(String name) {
		return name.replaceAll("[^a-zA-Z0-9öäüÄÖÜß]+", "").toLowerCase(Locale.ROOT);
	}

	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return NewSheet.from(meldeListe, SheetNamen.meldeliste(), metadatenSchluessel).setDocVersionWhenNew().useIfExist().hideGrid()
				.pos(DefaultSheetPos.MELDELISTE).tabColor(SHEET_COLOR).create().getSheet();
	}

	public int getSpielerNameSpalte() {
		return meldeListe.getSpielerNameErsteSpalte();
	}

	/**
	 * alle zeilen mit nummer ohne namen entfernen
	 *
	 * @throws GenerateException
	 */

	public void zeileOhneSpielerNamenEntfernen() throws GenerateException {
		meldeListe.processBoxinfo("processbox.meldeliste.zeilen.ohne.namen.entfernen");

		doSort(meldeListe.getMeldungenSpalte().getNamenOderTeamnameSpalte(), true); // alle zeilen ohne namen nach unten sortieren, egal ob daten oder nicht
		int letzteNrZeile = meldeListe.naechsteFreieDatenZeileInSpielerNrSpalte();
		if (letzteNrZeile < meldeListe.getErsteDatenZiele()) { // daten vorhanden ?
			return; // keine Daten
		}
		XSpreadsheet xSheet = getXSpreadSheet();

		// Vorname/Nachname/Teamname trimmen, bevor auf leere Zeilen geprüft wird – sonst
		// zählen Zellen mit nur Leerzeichen fälschlich als "hat einen Namen".
		meldeListe.getMeldungenSpalte().trimNamenUndTeamnameSpalten(letzteNrZeile - 1);
		// erneut sortieren: eine Zeile mit nur Leerzeichen war beim ersten Sortieren (vor dem
		// Trimmen) noch "nicht leer" und kann daher oberhalb einer echten Meldung gelandet sein
		doSort(meldeListe.getMeldungenSpalte().getNamenOderTeamnameSpalte(), true);

		int letzteZeileMitSpielerName = meldeListe.letzteZeileMitSpielerName(); // erst ab zeilen ohne namen anfangen

		if (letzteZeileMitSpielerName > 0) {
			for (int spielerNrZeilecntr = letzteZeileMitSpielerName; spielerNrZeilecntr < letzteNrZeile; spielerNrZeilecntr++) {
				// alle Namens-Spalten kombiniert prüfen (z.B. Supermelee Vorname+Nachname),
				// sonst wird eine Zeile mit leerem Vorname aber gefülltem Nachname fälschlich entfernt
				String spielerNamen = meldeListe.getMeldungenSpalte().leseSpielerNameZeile(xSheet, spielerNrZeilecntr);
				if (StringUtils.isBlank(spielerNamen)) { // null oder leer oder leerzeichen
					// Ganze Zeile leeren (Nr, Setzposition, Aktiv, Spieltag-Status), nicht nur die
					// Nr-Zelle – sonst bleiben verwaiste Werte (z.B. Aktiv-Flag) in der Leerzeile stehen.
					RangeHelper.from(meldeListe, RangePosition.from(SPIELER_NR_SPALTE, spielerNrZeilecntr,
							meldeListe.letzteSpielTagSpalte(), spielerNrZeilecntr)).clearRange();
				}
			}
		}
	}

	/**
	 * Erzeugt eine VLOOKUP-Formel, die den Anzeigenamen eines Teams anhand der Teamnummer
	 * aus der Meldeliste liest.<br>
	 * Bei aktivierter Teamname-Spalte wird der Teamname zurückgegeben (Spalte B). Andernfalls
	 * werden alle Spielernamen ("Vorname Nachname") konkateniert und mit " / " getrennt – damit
	 * z.B. eine Doublette ohne Teamname-Spalte beide Spieler vollständig anzeigt.<br>
	 * Der Sheet-Name wird korrekt quotiert, damit lokalisierte Namen mit Leerzeichen funktionieren.
	 *
	 * @param nrAdresse           Zell-Adresse oder Literal, das die Teamnummer enthält
	 * @param teamnameAnzeigen    {@code true} wenn die Meldeliste eine Teamname-Spalte hat
	 * @param formation           Formation der Meldeliste (Anzahl Spieler pro Team)
	 * @param vereinsnameAnzeigen {@code true} wenn die Meldeliste eine Vereinsname-Spalte pro Spieler hat
	 * @return Formel-String (ODF/englisch)
	 */
	public static String teamNameFormel(String nrAdresse, boolean teamnameAnzeigen,
			Formation formation, boolean vereinsnameAnzeigen) {
		String range = "$'" + SheetNamen.meldeliste() + "'.$A$1:$Z$999";
		String nrRange = "$'" + SheetNamen.meldeliste() + "'.$A$1:$A$999";
		String zeile = "INDEX(" + range + ";MATCH(" + nrAdresse + ";" + nrRange + ";0);0)";
		return GlobalImpl.FORMAT_PTM_TEAM_ANZEIGE(teamnameAnzeigen, formation.getAnzSpieler(),
				vereinsnameAnzeigen, zeile);
	}

	public String formulaSverweisSpielernamen(String spielrNrAdresse) {
		int ersteName = meldeListe.getMeldungenSpalte().getNamenOderTeamnameSpalte();
		int letzteName = meldeListe.getMeldungenSpalte().getLetzteMeldungNameSpalte();
		String ersteZelleAddress = Position.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE).getAddressWith$();
		String letzteZelleAddress = Position.from(letzteName, 999).getAddressWith$();
		String range = "$'" + SheetNamen.meldeliste() + "'." + ersteZelleAddress + ":" + letzteZelleAddress;

		if (ersteName == letzteName) {
			return "VLOOKUP(" + spielrNrAdresse + ";" + range + ";2;0)";
		}
		// 2 Namens-Spalten (Supermelee): "Nachname, Vorname" zusammensetzen.
		int vornameIdx = ersteName - SPIELER_NR_SPALTE + 1;
		int nachnameIdx = letzteName - SPIELER_NR_SPALTE + 1;
		return "VLOOKUP(" + spielrNrAdresse + ";" + range + ";" + nachnameIdx + ";0)"
				+ " & \", \" & "
				+ "VLOOKUP(" + spielrNrAdresse + ";" + range + ";" + vornameIdx + ";0)";
	}

	/**
	 *
	 * @param spieltag
	 * @param spielrundeGespielt list mit Flags. null für alle
	 * @return
	 * @throws GenerateException
	 */

	public IMeldungen<MLD_LIST_TYPE, MLDTYPE> getMeldungen(final SpielTagNr spieltag,
			final List<SpielrundeGespielt> spielrundeGespielt, IMeldungen<MLD_LIST_TYPE, MLDTYPE> meldungen)
			throws GenerateException {
		checkNotNull(spieltag, "spieltag == null");
		checkNotNull(meldungen, "meldungen == null");

		int letzteZeile = meldeListe.getMeldungenSpalte().getLetzteMitDatenZeileInSpielerNrSpalte();

		if (letzteZeile >= ERSTE_DATEN_ZEILE) {
			// daten vorhanden
			int spieltagSpalte = spieltagSpalte(spieltag);

			// Use getDataArray to get an Array off all Spieler bis SpieltagSpalte
			RangePosition rangebisSpieltagSpalte = RangePosition.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE,
					spieltagSpalte, letzteZeile);
			RangeData meldungenDaten = RangeHelper.from(meldeListe, rangebisSpieltagSpalte).getDataFromRange();

			// 0 = nr
			// 1-3 = namen je nach Modus, Tete,Doubl,Tripl
			// 4 = setzpos
			// letzte ! spalte = aktuelle Spieltag status
			for (RowData meldungZeile : meldungenDaten) {
				int isAktivStatus = meldungZeile.getLast().getIntVal(-1);
				SpielrundeGespielt status = SpielrundeGespielt.findById(isAktivStatus);
				if (spielrundeGespielt == null || spielrundeGespielt.contains(status)) {
					meldungen.addNewWennNichtVorhanden(meldungZeile);
				}
			}
		}
		return meldungen;
	}

	public int setzPositionSpalte() {
		return meldeListe.getMeldungenSpalte().getLetzteMeldungNameSpalte() + 1;
	}

	public int ersteSpieltagSpalte() {
		if (setzPositionSpalte() > -1) {
			return setzPositionSpalte() + 1;
		}
		return SPIELER_NR_SPALTE + meldeListe.getMeldungenSpalte().getAnzahlSpielerNamenSpalten();
	}

	public int spieltagSpalte(SpielTagNr spieltag) {
		return ersteSpieltagSpalte() + spieltag.getNr() - 1;
	}

	public void updateMeldungenNr(TeilnehmerListeSortModus sortModus) throws GenerateException {

		meldeListe.processBoxinfo("processbox.meldeliste.nummern.aktualisieren");

		int letzteSpielZeile = meldeListe.getMeldungenSpalte().letzteZeileMitSpielerName();
		if (letzteSpielZeile < meldeListe.getErsteDatenZiele()) { // daten vorhanden ?
			return; // keine Daten
		}
		XSpreadsheet xSheet = getXSpreadSheet();
		doSort(SPIELER_NR_SPALTE, false); // hoechste nummer oben, ohne nummer nach unten

		// Zeile erste Meldung ohne Nummer, weil ohne nummer nach unten sortiert
		int ersteZeileOhneNummer = meldeListe.naechsteFreieDatenZeileInSpielerNrSpalte(); // letzte Zeile ohne Spieler Nr

		// Bereits vergebene Nummern sammeln, damit freie Lücken (z.B. durch gelöschte
		// Meldungen) zuerst wiederverwendet werden, statt immer nur hochzuzählen.
		HashSet<Integer> belegteNummern = new HashSet<>();
		for (int zeile = meldeListe.getErsteDatenZiele(); zeile < ersteZeileOhneNummer; zeile++) {
			int nr = meldeListe.getSheetHelper().getIntFromCell(xSheet, Position.from(SPIELER_NR_SPALTE, zeile));
			if (nr > 0) {
				belegteNummern.add(nr);
			}
		}

		// lücken füllen
		int naechsteFreieNr = 1;
		NumberCellValue celVal = NumberCellValue.from(xSheet, Position.from(SPIELER_NR_SPALTE, meldeListe.getErsteDatenZiele()));
		for (int spielerZeilecntr = ersteZeileOhneNummer; spielerZeilecntr <= letzteSpielZeile; spielerZeilecntr++) {
			int spielrNr = meldeListe.getSheetHelper().getIntFromCell(xSheet,
					Position.from(SPIELER_NR_SPALTE, spielerZeilecntr));
			if (spielrNr == -1) {
				while (belegteNummern.contains(naechsteFreieNr)) {
					naechsteFreieNr++;
				}
				belegteNummern.add(naechsteFreieNr);
				meldeListe.getSheetHelper()
						.setNumberValueInCell(celVal.setValue((double) naechsteFreieNr).zeile(spielerZeilecntr));
			}
		}
		// Meldeliste nach konfiguriertem Modus sortieren; der Sortierbereich nimmt alle
		// Daten-/Statusspalten mit.
		doSort(sortSpalteFuer(sortModus), true);
	}

	/**
	 * Liefert die Spalte, nach der die Meldeliste gemäß dem übergebenen Sortier-Modus final
	 * sortiert werden soll.
	 * <p>
	 * {@code NAME} sortiert nach dem Nachnamen von Spieler 1 (wie bei Checkin-/Teilnehmerliste),
	 * nicht nach der ersten Namensspalte (= Vorname). {@code TEAMNAME} fällt auf {@code NAME}
	 * zurück, wenn das System keine Teamname-Spalte führt.
	 */
	private int sortSpalteFuer(TeilnehmerListeSortModus sortModus) {
		return switch (sortModus) {
			case NUMMER -> SPIELER_NR_SPALTE;
			case NAME -> nachnameSpieler1Spalte();
			case TEAMNAME -> {
				int teamnameSpalte = meldeListe.getMeldungenSpalte().getTeamnameSpalte();
				yield teamnameSpalte >= 0 ? teamnameSpalte : nachnameSpieler1Spalte();
			}
		};
	}

	/**
	 * Nachname-Spalte von Spieler 1: die erste Namensspalte ({@link MeldungenSpalte#getErsteMeldungNameSpalte()},
	 * über alle Systeme hinweg immer die Vorname-Spieler-1-Spalte) plus 1 – analog zu
	 * {@code TeilnehmerNamenLeser#leseSortNachname()}. Hat das System nur eine einzige
	 * Namensspalte, wird diese verwendet.
	 */
	private int nachnameSpieler1Spalte() {
		int ersteNamenSpalte = meldeListe.getMeldungenSpalte().getNamenOderTeamnameSpalte();
		int letzteNamenSpalte = meldeListe.getMeldungenSpalte().getLetzteMeldungNameSpalte();
		return ersteNamenSpalte < letzteNamenSpalte ? ersteNamenSpalte + 1 : ersteNamenSpalte;
	}

	/**
	 * @param turnierSystem
	 * @throws GenerateException
	 */
	public void insertTurnierSystemInHeader(TurnierSystem turnierSystem) throws GenerateException {
		// oben links
		meldeListe.getSheetHelper().setStringValueInCell(StringCellValue
				.from(getXSpreadSheet(), Position.from(0, 0), I18n.get("meldeliste.header.turniersystem", turnierSystem.getBezeichnung()))
				.setEndPosMergeSpaltePlus(1).setCharWeight(FontWeight.BOLD).setHoriJustify(CellHoriJustify.LEFT)
				.setVertJustify(CellVertJustify2.TOP).setShrinkToFit(true).setCharColor("00599d"));
	}

}
