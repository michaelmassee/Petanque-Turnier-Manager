/**
* Erstellung : 22.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee.meldeliste;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.sun.star.awt.FontWeight;
import com.sun.star.beans.PropertyValue;
import com.sun.star.lang.IndexOutOfBoundsException;
import com.sun.star.sheet.ConditionOperator;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.CellVertJustify2;
import com.sun.star.table.TableSortField;
import com.sun.star.table.XCellRange;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.util.XSortable;

import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationKonstanten;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.IMeldeliste;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeKonstanten;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ColorHelper;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellstyle.RanglisteHintergrundFarbeGeradeStyle;
import de.petanqueturniermanager.helper.cellstyle.RanglisteHintergrundFarbeUnGeradeStyle;
import de.petanqueturniermanager.helper.cellvalue.CellProperties;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.helper.pagestyle.PageStyle;
import de.petanqueturniermanager.helper.pagestyle.PageStyleHelper;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.ConditionalFormatHelper;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.IMitSpielerSpalte;
import de.petanqueturniermanager.helper.sheet.SpielerSpalte;
import de.petanqueturniermanager.model.Meldungen;
import de.petanqueturniermanager.model.Spieler;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.SupermeleeTeamPaarungenSheet;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeKonfigurationSheet;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleePropertiesSpalte;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeSheet;

abstract public class AbstractSupermeleeMeldeListeSheet extends SuperMeleeSheet implements IMeldeliste, Runnable, ISheet, IMitSpielerSpalte, MeldeListeKonstanten {
	private static final String SPIELTAG_HEADER_STR = "Spieltag";

	public static final int SPALTE_FORMATION = 0; // siehe enum #Formation Spalte 0
	public static final int ZEILE_FORMATION = 0; // Zeile 0

	public static final int MIN_ANZAHL_SPIELER_ZEILEN = 200; // Tablle immer mit min anzahl von zeilen formatieren

	public static final int SUMMEN_SPALTE_OFFSET = 2; // 2 Spalten weiter zur letzte Spieltag
	public static final int SUMMEN_ERSTE_ZEILE = ERSTE_DATEN_ZEILE + 5; // Zeile 3
	public static final int SUMMEN_AKTIVE_ZEILE = SUMMEN_ERSTE_ZEILE; // Zeile 6
	public static final int SUMMEN_INAKTIVE_ZEILE = SUMMEN_ERSTE_ZEILE + 1;
	public static final int SUMMEN_AUSGESTIEGENE_ZEILE = SUMMEN_ERSTE_ZEILE + 2; // Zeile 8

	public static final int SUMMEN_KANN_DOUBLETTE_ZEILE = SUMMEN_ERSTE_ZEILE + 7; // Zeile 10
	public static final int SUMMEN_SPIELBAHNEN = SUMMEN_ERSTE_ZEILE + 8; // Zeile 11

	// ab hier summen für Doublette Mode
	public static final int DOUBL_MODE_ANZ_DOUBLETTE = SUMMEN_SPIELBAHNEN + 2;
	public static final int DOUBL_MODE_ANZ_TRIPLETTE = DOUBL_MODE_ANZ_DOUBLETTE + 1;
	public static final int DOUBL_MODE_SUMMEN_KANN_TRIPLETTE_ZEILE = DOUBL_MODE_ANZ_TRIPLETTE + 1;
	public static final int DOUBL_MODE_SUMMEN_SPIELBAHNEN = DOUBL_MODE_SUMMEN_KANN_TRIPLETTE_ZEILE + 1;

	public static final int ERSTE_ZEILE_INFO = ERSTE_DATEN_ZEILE - 1; // Zeile 2

	private final SpielerSpalte spielerSpalte;
	private final SupermeleeTeamPaarungenSheet supermeleeTeamPaarungen;
	private SpielTagNr spielTag = null;

	public AbstractSupermeleeMeldeListeSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, "Meldeliste");
		spielerSpalte = new SpielerSpalte(ERSTE_DATEN_ZEILE, SPIELER_NR_SPALTE, this, this, Formation.MELEE);
		supermeleeTeamPaarungen = new SupermeleeTeamPaarungenSheet(workingSpreadsheet);
	}

	/**
	 * anzahl header zählen
	 *
	 * @return
	 * @throws GenerateException
	 */
	public int countAnzSpieltageInMeldeliste() throws GenerateException {
		int anzSpieltage = 0;
		int ersteSpieltagspalteSpalte = ersteSpieltagSpalte();
		Position posHeader = Position.from(ersteSpieltagspalteSpalte, ZWEITE_HEADER_ZEILE);

		for (int spielTagcntr = 1; spielTagcntr < 90; spielTagcntr++) {
			String header = getSheetHelper().getTextFromCell(getSheet(), posHeader);

			if (StringUtils.isEmpty(header)) {
				break;
			}

			if (header != null && header.contains(spielTagHeader(SpielTagNr.from(spielTagcntr)))) {
				anzSpieltage++;
			} else {
				break;
			}
			posHeader.spaltePlusEins();
		}
		return anzSpieltage;
	}

	@Override
	public XSpreadsheet getSheet() throws GenerateException {
		return getSheetHelper().newIfNotExist(SHEETNAME, DefaultSheetPos.MELDELISTE, SHEET_COLOR);
	}

	public void show() throws GenerateException {
		getSheetHelper().setActiveSheet(getSheet());
	}

	public void upDateSheet() throws GenerateException {
		PageStyleHelper.from(this, PageStyle.PETTURNMNGR).initDefaultFooter().create().applytoSheet();
		processBoxinfo("Aktualisiere Meldungen");

		testDoppelteDaten();

		XSpreadsheet sheet = getSheet();
		getSheetHelper().setActiveSheet(sheet);

		// ------
		// Header einfuegen
		// ------
		int headerBackColor = getKonfigurationSheet().getRanglisteHeaderFarbe();
		spielerSpalte.insertHeaderInSheet(headerBackColor);

		// ------
		// Setzposition
		CellProperties columnProp = CellProperties.from().setHoriJustify(CellHoriJustify.CENTER).setWidth(800);
		StringCellValue bezCelVal = StringCellValue.from(sheet, setzPositionSpalte(), ZWEITE_HEADER_ZEILE, "SP")
				.setComment("1 = Setzposition, Diesen Spieler werden nicht zusammen im gleichen Team gelost.").setCellBackColor(headerBackColor)
				.setBorder(BorderFactory.from().allThin().toBorder()).addColumnProperties(columnProp).setVertJustify(CellVertJustify2.CENTER);
		getSheetHelper().setTextInCell(bezCelVal);
		// ------

		formatSpielTagSpalte(getSpielTag());

		// eventuelle luecken in spiele namen nach unten sortieren
		zeileOhneSpielerNamenEntfernen();
		updateSpielerNr();

		doSort(spielerSpalte.getSpielerNameErsteSpalte(), true); // nach namen sortieren
		updateSpieltageSummenSpalten();
		insertInfoSpalte();
		spielerSpalte.formatDaten();
		formatDaten();
	}

	/**
	 * aktuelle Spieltag + Spielrunde infos
	 *
	 * @throws GenerateException
	 */
	private void insertInfoSpalte() throws GenerateException {
		XSpreadsheet sheet = getSheet();
		Position posBezeichnug = Position.from(ersteSummeSpalte(), ERSTE_ZEILE_INFO);

		String formulaStrSpieltag = "VLOOKUP(\"" + SuperMeleePropertiesSpalte.KONFIG_PROP_NAME_SPIELTAG + "\";$" + IKonfigurationKonstanten.SHEETNAME + "." + suchMatrixProperty()
				+ ";2;0)";
		String formulaStrSpielRunde = "VLOOKUP(\"" + SuperMeleePropertiesSpalte.KONFIG_PROP_NAME_SPIELRUNDE + "\";$" + IKonfigurationKonstanten.SHEETNAME + "."
				+ suchMatrixProperty() + ";2;0)";

		StringCellValue bezeichnugVal = StringCellValue.from(sheet, posBezeichnug, SuperMeleePropertiesSpalte.KONFIG_PROP_NAME_SPIELTAG).setComment("Aktive Spieltag")
				.setEndPosMergeZeilePlus(1).setCharHeight(14).setCharWeight(FontWeight.BOLD).setVertJustify(CellVertJustify2.CENTER);
		getSheetHelper().setTextInCell(bezeichnugVal);
		getSheetHelper().setFormulaInCell(StringCellValue.from(bezeichnugVal).spaltePlusEins().setComment(null).setValue(formulaStrSpieltag));

		bezeichnugVal.setValue(SuperMeleePropertiesSpalte.KONFIG_PROP_NAME_SPIELRUNDE).setComment("Aktive Spielrunde").zeilePlus(2);
		getSheetHelper().setTextInCell(bezeichnugVal);
		getSheetHelper().setFormulaInCell(StringCellValue.from(bezeichnugVal).spaltePlusEins().setComment(null).setValue(formulaStrSpielRunde));
	}

	protected void formatSpielTagSpalte(SpielTagNr spieltag) throws GenerateException {
		checkNotNull(spieltag);

		processBoxinfo("Formatiere Spieltagspalte");

		XSpreadsheet sheet = getSheet();
		int hederBackColor = getKonfigurationSheet().getRanglisteHeaderFarbe();
		CellProperties columnProp = CellProperties.from().setHoriJustify(CellHoriJustify.CENTER).setWidth(2000);

		StringCellValue bezCelSpieltagVal = StringCellValue.from(sheet, spieltagSpalte(spieltag), ZWEITE_HEADER_ZEILE, spielTagHeader(spieltag))
				.setComment("1 = Aktiv, 2 = Ausgestiegen, leer = InAktiv").setCellBackColor(hederBackColor).addColumnProperties(columnProp)
				.setBorder(BorderFactory.from().allThin().toBorder());

		// Spieltag header
		bezCelSpieltagVal.setValue(spielTagHeader(spieltag));
		getSheetHelper().setTextInCell(bezCelSpieltagVal);

		// Aktiv / Inaktiv spieltag
		// =WENN(WENNNV(SVERWEIS("Spieltag";$Konfiguration.$A$2:$B$101;2;0);0)=2;"Aktiv";"")
		String formulaStr = "IF(IFNA(VLOOKUP(\"" + SuperMeleePropertiesSpalte.KONFIG_PROP_NAME_SPIELTAG + "\";$" + IKonfigurationKonstanten.SHEETNAME + "." + suchMatrixProperty()
				+ ";2;0);0)=" + spieltag.getNr() + ";\"Aktiv\";\"\"";
		StringCellValue aktivFormula = StringCellValue.from(sheet, spieltagSpalte(spieltag), ERSTE_HEADER_ZEILE, formulaStr).setCharColor(ColorHelper.CHAR_COLOR_GREEN);
		getSheetHelper().setFormulaInCell(aktivFormula);

	}

	private String suchMatrixProperty() {
		Position start = Position.from(SuperMeleeKonfigurationSheet.PROPERTIESSPALTE, SuperMeleeKonfigurationSheet.ERSTE_ZEILE_PROPERTIES);
		Position end = Position.from(start).spaltePlusEins().zeile(100);
		return start.getAddressWith$() + ":" + end.getAddressWith$();
	}

	void formatDaten() throws GenerateException {

		processBoxinfo("Formatiere Daten Spalten");

		int letzteDatenZeile = spielerSpalte.getLetzteDatenZeile();

		if (letzteDatenZeile < MIN_ANZAHL_SPIELER_ZEILEN) {
			letzteDatenZeile = MIN_ANZAHL_SPIELER_ZEILEN;
		}

		if (letzteDatenZeile < ERSTE_DATEN_ZEILE) {
			// keine Daten
			return;
		}

		RangePosition datenRange = RangePosition.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE, letzteSpielTagSpalte(), letzteDatenZeile);

		getSheetHelper().setPropertiesInRange(getSheet(), datenRange,
				CellProperties.from().setVertJustify(CellVertJustify2.CENTER).setBorder(BorderFactory.from().allThin().boldLn().forTop().forLeft().toBorder())
						.setCharColor(ColorHelper.CHAR_COLOR_BLACK).setCellBackColor(-1).setShrinkToFit(true));

		// gerade / ungrade hintergrund farbe
		// CellBackColor
		Integer geradeColor = getKonfigurationSheet().getRanglisteHintergrundFarbeGerade();
		Integer unGeradeColor = getKonfigurationSheet().getRanglisteHintergrundFarbeUnGerade();
		RanglisteHintergrundFarbeGeradeStyle ranglisteHintergrundFarbeGeradeStyle = new RanglisteHintergrundFarbeGeradeStyle(geradeColor);
		RanglisteHintergrundFarbeUnGeradeStyle ranglisteHintergrundFarbeUnGeradeStyle = new RanglisteHintergrundFarbeUnGeradeStyle(unGeradeColor);

		// Spieler Nummer
		// -----------------------------------------------
		RangePosition nrSetPosRange = RangePosition.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE, SPIELER_NR_SPALTE, letzteDatenZeile);
		String conditionfindDoppeltNr = "COUNTIF(" + Position.from(SPIELER_NR_SPALTE, 0).getSpalteAddressWith$() + ";" + ConditionalFormatHelper.FORMULA_CURRENT_CELL + ")>1";
		ConditionalFormatHelper.from(this, nrSetPosRange).clear().
		// ------------------------------
				formulaIsText().styleIsFehler().applyNew().
				// ------------------------------
				formula1(conditionfindDoppeltNr).operator(ConditionOperator.FORMULA).styleIsFehler().applyNew().
				// ------------------------------
				formulaIsEvenRow().style(ranglisteHintergrundFarbeGeradeStyle).applyNew().
				// ------------------------------
				formulaIsOddRow().style(ranglisteHintergrundFarbeUnGeradeStyle).applyNew();
		// -----------------------------------------------

		// -----------------------------------------------
		// Spieler Namen
		// -----------------------------------------------
		RangePosition nameSetPosRange = RangePosition.from(getSpielerNameErsteSpalte(), ERSTE_DATEN_ZEILE, getSpielerNameErsteSpalte(), letzteDatenZeile);
		String conditionfindDoppeltNamen = "COUNTIF(" + Position.from(getSpielerNameErsteSpalte(), 0).getSpalteAddressWith$() + ";" + ConditionalFormatHelper.FORMULA_CURRENT_CELL
				+ ")>1";
		ConditionalFormatHelper.from(this, nameSetPosRange).clear().
		// ------------------------------
				formula1(conditionfindDoppeltNamen).operator(ConditionOperator.FORMULA).styleIsFehler().applyNew().
				// ------------------------------
				formulaIsEvenRow().operator(ConditionOperator.FORMULA).style(ranglisteHintergrundFarbeGeradeStyle).applyNew().
				// ------------------------------
				formulaIsEvenRow().style(ranglisteHintergrundFarbeGeradeStyle).applyNew().formulaIsOddRow().style(ranglisteHintergrundFarbeUnGeradeStyle).applyNew();
		// -----------------------------------------------

		// -----------------------------------------------
		// setzposition spalte
		// -----------------------------------------------
		RangePosition setzpositionRangePos = RangePosition.from(setzPositionSpalte(), ERSTE_DATEN_ZEILE, setzPositionSpalte(), letzteDatenZeile);
		ConditionalFormatHelper.from(this, setzpositionRangePos).clear().
		// ------------------------------
				formula1("0").formula2("90").operator(ConditionOperator.NOT_BETWEEN).styleIsFehler().applyNew().
				// ------------------------------
				formulaIsText().styleIsFehler().applyNew().
				// ------------------------------
				formulaIsEvenRow().style(ranglisteHintergrundFarbeGeradeStyle).applyNew().
				// ------------------------------
				formulaIsOddRow().style(ranglisteHintergrundFarbeUnGeradeStyle).applyNew();

		// -----------------------------------------------
		// Spieltag spalten
		// prüfe wenn <0 und > 2
		// test if Text mit FORMULA
		// reihenfolge beachten
		// ------------------------------
		RangePosition spieltageRangePos = RangePosition.from(ersteSpieltagSpalte(), ERSTE_DATEN_ZEILE, letzteSpielTagSpalte(), letzteDatenZeile);
		ConditionalFormatHelper.from(this, spieltageRangePos).clear().
		// ------------------------------
				formula1("0").formula2("2").operator(ConditionOperator.NOT_BETWEEN).styleIsFehler().applyNew().
				// ------------------------------
				formulaIsText().styleIsFehler().applyNew().
				// ------------------------------
				formulaIsEvenRow().style(ranglisteHintergrundFarbeGeradeStyle).applyNew().
				// ------------------------------
				formulaIsOddRow().style(ranglisteHintergrundFarbeUnGeradeStyle).applyNew();
	}

	/**
	 * @param spieltag = 1 bis x
	 * @return
	 * @throws GenerateException
	 */
	public String spielTagHeader(SpielTagNr spieltag) throws GenerateException {
		return spieltag.getNr() + ". " + SPIELTAG_HEADER_STR;
	}

	/**
	 * @throws GenerateException
	 */
	public int setzPositionSpalte() throws GenerateException {
		return spielerSpalte.getSpielerNameErsteSpalte() + 1;
	}

	public int letzteSpielTagSpalte() throws GenerateException {
		int anzSpieltage = countAnzSpieltageInMeldeliste();
		return ersteSpieltagSpalte() + (anzSpieltage - 1);
	}

	public int ersteSpieltagSpalte() throws GenerateException {
		if (setzPositionSpalte() > -1) {
			return setzPositionSpalte() + 1;
		}
		return SPIELER_NR_SPALTE + spielerSpalte.getAnzahlSpielerNamenSpalten();
	}

	/**
	 *
	 * @return spalte zum getSpielTag()
	 * @throws GenerateException
	 */

	public int aktuelleSpieltagSpalte() throws GenerateException {
		return spieltagSpalte(getSpielTag());
	}

	public int spieltagSpalte(SpielTagNr spieltag) throws GenerateException {
		return ersteSpieltagSpalte() + spieltag.getNr() - 1;
	}

	public int ersteSummeSpalte() throws GenerateException {
		return letzteSpielTagSpalte() + SUMMEN_SPALTE_OFFSET;
	}

	@Override
	public String formulaSverweisSpielernamen(String spielrNrAdresse) {
		String ersteZelleAddress = Position.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE).getAddressWith$();
		String letzteZelleAddress = Position.from(spielerSpalte.getSpielerNameErsteSpalte(), 999).getAddressWith$();
		return "VLOOKUP(" + spielrNrAdresse + ";$'" + SHEETNAME + "'." + ersteZelleAddress + ":" + letzteZelleAddress + ";2;0)";
	}

	public void zeileOhneSpielerNamenEntfernen() throws GenerateException {

		processBoxinfo("Zeilen ohne Spielernamen entfernen");

		doSort(spielerSpalte.getSpielerNameErsteSpalte(), true); // alle zeilen ohne namen nach unten sortieren, egal ob daten oder nicht
		int letzteNrZeile = spielerSpalte.neachsteFreieDatenZeile();
		if (letzteNrZeile < ERSTE_DATEN_ZEILE) { // daten vorhanden ?
			return; // keine Daten
		}
		XSpreadsheet xSheet = getSheet();

		StringCellValue emptyVal = StringCellValue.from(xSheet, Position.from(SPIELER_NR_SPALTE, 0)).setValue("");

		for (int spielerNrZeilecntr = ERSTE_DATEN_ZEILE; spielerNrZeilecntr < letzteNrZeile; spielerNrZeilecntr++) {
			Position posSpielerName = Position.from(spielerSpalte.getSpielerNameErsteSpalte(), spielerNrZeilecntr);
			String spielerNamen = getSheetHelper().getTextFromCell(xSheet, posSpielerName);
			// Achtung alle durchgehen weil eventuell lücken in der nr spalte!
			if (StringUtils.isBlank(spielerNamen)) { // null oder leer oder leerzeichen
				// nr ohne spieler namen entfernen
				getSheetHelper().setTextInCell(emptyVal.zeile(spielerNrZeilecntr));
			}
		}
	}

	public int getSpielerNameSpalte() {
		return spielerSpalte.getSpielerNameErsteSpalte();
	}

	/**
	 * prüft auf doppelte spieler nr oder namen
	 *
	 * @return
	 * @throws GenerateException wenn doppelt daten
	 */
	public void testDoppelteDaten() throws GenerateException {
		processBoxinfo("Prüfe Doppelte Daten in Meldungen");
		XSpreadsheet xSheet = getSheet();

		int letzteSpielZeile = spielerSpalte.letzteZeileMitSpielerName();
		if (letzteSpielZeile <= ERSTE_DATEN_ZEILE) { // daten vorhanden ?
			return; // keine Daten
		}

		doSort(SPIELER_NR_SPALTE, false); // hoechste nummer oben, ohne nummer nach unten

		// doppelte spieler Nummer entfernen !?!?!
		HashSet<Integer> spielrNrInSheet = new HashSet<>();
		HashSet<String> spielrNamenInSheet = new HashSet<>();

		int spielrNr;
		String spielerName;
		NumberCellValue errCelVal = NumberCellValue.from(xSheet, Position.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE)).setCharColor(ColorHelper.CHAR_COLOR_RED);

		StringCellValue errStrCelVal = StringCellValue.from(xSheet, Position.from(spielerSpalte.getSpielerNameErsteSpalte(), ERSTE_DATEN_ZEILE))
				.setCharColor(ColorHelper.CHAR_COLOR_RED);

		for (int spielerZeilecntr = ERSTE_DATEN_ZEILE; spielerZeilecntr <= letzteSpielZeile; spielerZeilecntr++) {
			// -------------------
			// Spieler nr testen
			// -------------------
			spielrNr = getSheetHelper().getIntFromCell(xSheet, Position.from(SPIELER_NR_SPALTE, spielerZeilecntr));
			if (spielrNr > -1) {
				if (spielrNrInSheet.contains(spielrNr)) {
					// RED Color
					getSheetHelper().setValInCell(errCelVal.setValue((double) spielrNr).zeile(spielerZeilecntr));
					throw new GenerateException("Meldeliste wurde nicht Aktualisiert.\r\nSpieler Nr. " + spielrNr + " ist doppelt in der Meldeliste !!!");
				}
				spielrNrInSheet.add(spielrNr);
			}

			// -------------------
			// spieler namen testen
			// -------------------
			// Supermelee hat nur ein name spalte
			spielerName = getSheetHelper().getTextFromCell(xSheet, Position.from(spielerSpalte.getSpielerNameErsteSpalte(), spielerZeilecntr)); // wird trim gemacht

			if (StringUtils.isNotEmpty(spielerName)) {
				if (spielrNamenInSheet.contains(cleanUpSpielerName(spielerName))) {
					// RED Color
					getSheetHelper().setTextInCell(errStrCelVal.setValue(spielerName).zeile(spielerZeilecntr));
					throw new GenerateException(
							"Meldeliste wurde nicht Aktualisiert.\r\nSpieler Namen " + spielerName + " ist doppelt in der Meldeliste. Zeile:" + spielerZeilecntr);
				}
				spielrNamenInSheet.add(cleanUpSpielerName(spielerName));
			}
		}
	}

	/**
	 * für ein vergleich ,.: und leerzeichen entfernen
	 *
	 * @param name
	 * @return
	 */
	String cleanUpSpielerName(String name) {
		return name.replaceAll("[^a-zA-Z0-9öäüÄÖÜß]+", "").toLowerCase();
	}

	public void updateSpielerNr() throws GenerateException {

		processBoxinfo("Aktualisiere Spieler Nummer");

		int letzteSpielZeile = spielerSpalte.letzteZeileMitSpielerName();
		if (letzteSpielZeile < ERSTE_DATEN_ZEILE) { // daten vorhanden ?
			return; // keine Daten
		}
		XSpreadsheet xSheet = getSheet();
		doSort(SPIELER_NR_SPALTE, false); // hoechste nummer oben, ohne nummer nach unten

		int letzteSpielerNr = 0;
		int spielrNr = getSheetHelper().getIntFromCell(xSheet, Position.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE));
		if (spielrNr > -1) {
			letzteSpielerNr = spielrNr;
		}
		// spieler nach Alphabet sortieren
		doSort(spielerSpalte.getSpielerNameErsteSpalte(), true);

		// lücken füllen
		NumberCellValue celVal = NumberCellValue.from(xSheet, Position.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE));
		for (int spielerZeilecntr = ERSTE_DATEN_ZEILE; spielerZeilecntr <= letzteSpielZeile; spielerZeilecntr++) {
			spielrNr = getSheetHelper().getIntFromCell(xSheet, Position.from(SPIELER_NR_SPALTE, spielerZeilecntr));
			if (spielrNr == -1) {
				getSheetHelper().setValInCell(celVal.setValue((double) ++letzteSpielerNr).zeile(spielerZeilecntr));
			}
		}
	}

	/**
	 * alle sortierbare daten, ohne header !
	 *
	 * @return
	 * @throws GenerateException
	 */
	private XCellRange getxCellRangeAlleDaten() throws GenerateException {
		XSpreadsheet xSheet = getSheet();
		XCellRange xCellRange = null;
		try {
			int letzteSpielZeile = spielerSpalte.letzteZeileMitSpielerName();
			if (letzteSpielZeile > ERSTE_DATEN_ZEILE) { // daten vorhanden ?
				// (column, row, column, row)
				xCellRange = xSheet.getCellRangeByPosition(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE, letzteSpielTagSpalte(), letzteSpielZeile);
			}
		} catch (IndexOutOfBoundsException e) {
			getLogger().error(e.getMessage(), e);
			return null;
		}
		return xCellRange;
	}

	public void doSort(int spalteNr, boolean isAscending) throws GenerateException {

		XCellRange xCellRange = getxCellRangeAlleDaten();

		if (xCellRange == null) {
			return;
		}

		XSortable xSortable = UnoRuntime.queryInterface(XSortable.class, xCellRange);

		TableSortField[] aSortFields = new TableSortField[1];
		TableSortField field1 = new TableSortField();
		field1.Field = spalteNr; // 0 = erste spalte, nur eine Spalte sortieren
		field1.IsAscending = isAscending;
		// Note – The FieldType member, that is used to select textual or numeric sorting in
		// text documents is ignored in the spreadsheet application. In a spreadsheet, a cell
		// always has a known type of text or value, which is used for sorting, with numbers
		// sorted before text cells.
		aSortFields[0] = field1;

		PropertyValue[] aSortDesc = new PropertyValue[2];
		PropertyValue propVal = new PropertyValue();
		propVal.Name = "SortFields";
		propVal.Value = aSortFields;
		aSortDesc[0] = propVal;

		// specifies if cell formats are moved with the contents they belong to.
		propVal = new PropertyValue();
		propVal.Name = "BindFormatsToContent";
		propVal.Value = false;
		aSortDesc[1] = propVal;

		xSortable.sort(aSortDesc);
	}

	public void updateSpieltageSummenSpalten() throws GenerateException {

		processBoxinfo("Aktualisiere Summen Spalten");

		int letzteDatenZeile = spielerSpalte.getLetzteDatenZeile();

		if (letzteDatenZeile < MIN_ANZAHL_SPIELER_ZEILEN) {
			letzteDatenZeile = MIN_ANZAHL_SPIELER_ZEILEN;
		}

		XSpreadsheet sheet = getSheet();

		int anzSpieltage = countAnzSpieltageInMeldeliste();

		RangePosition cleanUpRange = RangePosition.from(ersteSummeSpalte() - 1, 0, ersteSummeSpalte() + anzSpieltage + 10, 999);
		getSheetHelper().clearRange(sheet, cleanUpRange);

		Position posBezeichnug = Position.from(ersteSummeSpalte(), SUMMEN_ERSTE_ZEILE - 1);

		CellProperties columnProp = CellProperties.from().setHoriJustify(CellHoriJustify.RIGHT).setWidth(3000);
		StringCellValue bezCelVal = StringCellValue.from(sheet, posBezeichnug, "").addColumnProperties(columnProp).setComment(null).removeCellBackColor();
		getSheetHelper().setTextInCell(bezCelVal);

		bezCelVal.setComment(null).setValue("Aktiv").zeile(SUMMEN_AKTIVE_ZEILE);
		getSheetHelper().setTextInCell(bezCelVal);

		bezCelVal.setComment(null).setValue("InAktiv").zeile(SUMMEN_INAKTIVE_ZEILE);
		getSheetHelper().setTextInCell(bezCelVal);

		bezCelVal.setComment("Spieler mit \"2\" im Spieltag").setValue("Ausgestiegen").zeile(SUMMEN_AUSGESTIEGENE_ZEILE);
		getSheetHelper().setTextInCell(bezCelVal);

		bezCelVal.setComment("Aktive + Ausgestiegen").setValue("Anz. Spieler").zeilePlusEins();
		getSheetHelper().setTextInCell(bezCelVal);

		bezCelVal.setComment("Aktive + Inaktiv + Ausgestiegen").setValue("Summe").zeilePlusEins();
		getSheetHelper().setTextInCell(bezCelVal);

		bezCelVal.setComment("Triplette Mode, Doublette Teams").setValue("∑x2").zeilePlusEins();
		getSheetHelper().setTextInCell(bezCelVal);

		bezCelVal.setComment("Triplette Mode, Triplette Teams").setValue("∑x3").zeilePlusEins();
		getSheetHelper().setTextInCell(bezCelVal);

		bezCelVal.setComment("Triplette Mode, Kann Doublette gespielt werden").setValue("Doublette");
		getSheetHelper().setTextInCell(bezCelVal.zeile(SUMMEN_KANN_DOUBLETTE_ZEILE));

		bezCelVal.setComment("Triplette Mode, Anzahl Spielbahnen").setValue("Bahnen");
		getSheetHelper().setTextInCell(bezCelVal.zeile(SUMMEN_SPIELBAHNEN));

		// ------------------------------------------------------------------------------------
		bezCelVal.setComment("Doublette Mode, Anzahl Doublette").setValue("∑x2");
		getSheetHelper().setTextInCell(bezCelVal.zeile(DOUBL_MODE_ANZ_DOUBLETTE));

		bezCelVal.setComment("Doublette Mode, Anzahl Triplette").setValue("∑x3");
		getSheetHelper().setTextInCell(bezCelVal.zeile(DOUBL_MODE_ANZ_TRIPLETTE));

		bezCelVal.setComment("Doublette Mode, Kann Triplette gespielt werden").setValue("Triplette");
		getSheetHelper().setTextInCell(bezCelVal.zeile(DOUBL_MODE_SUMMEN_KANN_TRIPLETTE_ZEILE));

		bezCelVal.setComment("Doublette Mode,Anzahl Spielbahnen").setValue("Bahnen");
		getSheetHelper().setTextInCell(bezCelVal.zeile(DOUBL_MODE_SUMMEN_SPIELBAHNEN));
		// ------------------------------------------------------------------------------------

		for (int spieltagCntr = 1; spieltagCntr <= anzSpieltage; spieltagCntr++) {

			SpielTagNr spielTagNr = new SpielTagNr(spieltagCntr);

			Position posSpieltagWerte = Position.from(ersteSummeSpalte() + spieltagCntr, SUMMEN_ERSTE_ZEILE - 1);

			// Header
			getSheetHelper().setColumnWidthAndHoriJustifyCenter(sheet, posSpieltagWerte, 1000, "Tag " + spieltagCntr);

			// Summe Aktive Spieler "=ZÄHLENWENN(D3:D102;1)"
			getSheetHelper().setFormulaInCell(sheet, posSpieltagWerte.zeile(SUMMEN_AKTIVE_ZEILE), "=" + formulaCountSpieler(spielTagNr, "1", letzteDatenZeile));

			// =ZÄHLENWENNS(B3:B201;"*";D3:D201;"")
			// Summe inAktive Spieler "=ZÄHLENWENN(D3:D102;0) + ZÄHLENWENN(D3:D102;"")"
			getSheetHelper().setFormulaInCell(sheet, posSpieltagWerte.zeile(SUMMEN_INAKTIVE_ZEILE),
					"=" + formulaCountSpieler(spielTagNr, "0", letzteDatenZeile) + " + " + formulaCountSpieler(spielTagNr, "\"\"", letzteDatenZeile));

			// Ausgestiegen =ZÄHLENWENN(D3:D102;2)
			getSheetHelper().setFormulaInCell(sheet, posSpieltagWerte.zeile(SUMMEN_AUSGESTIEGENE_ZEILE), "=" + formulaCountSpieler(spielTagNr, "2", letzteDatenZeile));
			// -----------------------------------
			// Aktiv + Ausgestiegen
			Position anzahlAktiveSpielerPosition = getAnzahlAktiveSpielerPosition(spielTagNr);
			String aktivZelle = getSheetHelper().getAddressFromColumnRow(anzahlAktiveSpielerPosition);
			String ausgestiegenZelle = getSheetHelper().getAddressFromColumnRow(getAusgestiegenSpielerPosition(spielTagNr));
			getSheetHelper().setFormulaInCell(sheet, posSpieltagWerte.zeilePlusEins(), "=" + aktivZelle + "+" + ausgestiegenZelle);
			// -----------------------------------
			// =K7+K8+K9
			// Aktiv + Ausgestiegen + inaktive
			String inAktivZelle = getSheetHelper().getAddressFromColumnRow(anzahlAktiveSpielerPosition.zeilePlusEins());
			getSheetHelper().setFormulaInCell(sheet, posSpieltagWerte.zeilePlusEins(), "=" + aktivZelle + "+" + inAktivZelle + "+" + ausgestiegenZelle);
			// -----------------------------------
			String anzSpielerAddr = getSheetHelper().getAddressFromColumnRow(getAnzahlAktiveSpielerPosition(spielTagNr));
			String formulaSverweisAnzDoublette = supermeleeTeamPaarungen.formulaSverweisAnzDoublette(anzSpielerAddr);
			getSheetHelper().setFormulaInCell(sheet, posSpieltagWerte.zeilePlusEins(), formulaSverweisAnzDoublette);
			String anzDoublZelle = getSheetHelper().getAddressFromColumnRow(posSpieltagWerte); // Position merken

			String formulaSverweisAnzTriplette = supermeleeTeamPaarungen.formulaSverweisAnzTriplette(anzSpielerAddr);
			getSheetHelper().setFormulaInCell(sheet, posSpieltagWerte.zeilePlusEins(), formulaSverweisAnzTriplette);
			String anzTriplZelle = getSheetHelper().getAddressFromColumnRow(posSpieltagWerte); // Position merken

			String formulaSverweisNurDoublette = supermeleeTeamPaarungen.formulaSverweisNurDoublette(anzSpielerAddr);
			getSheetHelper().setFormulaInCell(sheet, posSpieltagWerte.zeile(SUMMEN_KANN_DOUBLETTE_ZEILE), formulaSverweisNurDoublette);
			// -----------------------------------
			String formulaAnzSpielbahnen = "=(" + anzDoublZelle + " + " + anzTriplZelle + ")/2";
			getSheetHelper().setFormulaInCell(sheet, posSpieltagWerte.zeile(SUMMEN_SPIELBAHNEN), formulaAnzSpielbahnen);

			// ------------------------------------------------------------------------------------
			// Doublette mode
			// ------------------------------------------------------------------------------------
			String doublettModeformulaSverweisAnzDoublette = supermeleeTeamPaarungen.formulaSverweisDoubletteModeAnzDoublette(anzSpielerAddr);
			getSheetHelper().setFormulaInCell(sheet, posSpieltagWerte.zeile(DOUBL_MODE_ANZ_DOUBLETTE), doublettModeformulaSverweisAnzDoublette);
			String doublettteModeAnzDoublZelle = getSheetHelper().getAddressFromColumnRow(posSpieltagWerte); // Position merken

			String doublettModeformulaSverweisAnzTriplette = supermeleeTeamPaarungen.formulaSverweisAnzDoubletteModeAnzTriplette(anzSpielerAddr);
			getSheetHelper().setFormulaInCell(sheet, posSpieltagWerte.zeile(DOUBL_MODE_ANZ_TRIPLETTE), doublettModeformulaSverweisAnzTriplette);
			String doublettteModeAnzTriplZelle = getSheetHelper().getAddressFromColumnRow(posSpieltagWerte); // Position merken

			String doublettModeformulaSverweisNurTriplette = supermeleeTeamPaarungen.formulaSverweisDoubletteModeNurTriplette(anzSpielerAddr);
			getSheetHelper().setFormulaInCell(sheet, posSpieltagWerte.zeile(DOUBL_MODE_SUMMEN_KANN_TRIPLETTE_ZEILE), doublettModeformulaSverweisNurTriplette);
			// -----------------------------------
			String doublettModeFormulaAnzSpielbahnen = "=(" + doublettteModeAnzDoublZelle + " + " + doublettteModeAnzTriplZelle + ")/2";
			getSheetHelper().setFormulaInCell(sheet, posSpieltagWerte.zeile(DOUBL_MODE_SUMMEN_SPIELBAHNEN), doublettModeFormulaAnzSpielbahnen);

		}
	}

	// ---------------------------------------------
	public int getAnzahlAktiveSpieler(SpielTagNr Spieltag) throws GenerateException {
		return getSheetHelper().getIntFromCell(getSheet(), getAnzahlAktiveSpielerPosition(Spieltag));
	}

	public Position getAnzahlAktiveSpielerPosition(SpielTagNr spieltag) throws GenerateException {
		return Position.from(ersteSummeSpalte() + spieltag.getNr(), SUMMEN_AKTIVE_ZEILE);
	}

	// ---------------------------------------------
	public int getAnzahlInAktiveSpieler(SpielTagNr spieltag) throws GenerateException {
		return getSheetHelper().getIntFromCell(getSheet(), getAnzahlInAktiveSpielerPosition(spieltag));
	}

	public Position getAnzahlInAktiveSpielerPosition(SpielTagNr spieltag) throws GenerateException {
		return Position.from(ersteSummeSpalte() + spieltag.getNr(), SUMMEN_INAKTIVE_ZEILE);
	}

	// ---------------------------------------------
	public int getAusgestiegenSpieler(SpielTagNr spieltag) throws GenerateException {
		return getSheetHelper().getIntFromCell(getSheet(), getAusgestiegenSpielerPosition(spieltag));
	}

	public Position getAusgestiegenSpielerPosition(SpielTagNr spieltag) throws GenerateException {
		return Position.from(ersteSummeSpalte() + spieltag.getNr(), SUMMEN_AUSGESTIEGENE_ZEILE);
	}
	// ---------------------------------------------

	public Boolean isKannNurDoublette(SpielTagNr Spieltag) throws GenerateException {
		return StringUtils.isNotBlank(getSheetHelper().getTextFromCell(getSheet(), getKannNurDoublettePosition(Spieltag)));
	}

	public Position getKannNurDoublettePosition(SpielTagNr Spieltag) throws GenerateException {
		return Position.from(ersteSummeSpalte() + Spieltag.getNr(), SUMMEN_KANN_DOUBLETTE_ZEILE);
	}
	// ---------------------------------------------

	/**
	 *
	 * @param spieltag 1 = erste spieltag
	 * @param status = 1,2
	 * @return "==ZÄHLENWENNS(B3:B201;"<>";D3:D201;"")"
	 * @throws GenerateException
	 */
	private String formulaCountSpieler(SpielTagNr spieltag, String status, int letzteZeile) throws GenerateException {

		if (letzteZeile < ERSTE_DATEN_ZEILE) {
			return "";
		}

		String ersteZelleName = Position.from(getSpielerNameErsteSpalte(), ERSTE_DATEN_ZEILE).getAddress();
		String letzteZelleName = Position.from(getSpielerNameErsteSpalte(), letzteZeile).getAddress();

		int spieltagSpalte = spieltagSpalte(spieltag);
		String ersteZelleSpielTag = Position.from(spieltagSpalte, ERSTE_DATEN_ZEILE).getAddress();
		String letzteZelleSpielTag = Position.from(spieltagSpalte, letzteZeile).getAddress();

		// nur dann zählen wenn name gefüllt
		return "COUNTIFS(" + ersteZelleName + ":" + letzteZelleName + ";\"<>\";" + ersteZelleSpielTag + ":" + letzteZelleSpielTag + ";" + status + ")";
	}

	/**
	 *
	 * @param spieltag
	 * @param spielrundeGespielt list mit Flags. null für alle
	 * @return
	 * @throws GenerateException
	 */

	public Meldungen getMeldungen(SpielTagNr spieltag, List<SpielrundeGespielt> spielrundeGespielt) throws GenerateException {
		checkNotNull(spieltag, "spieltag == null");
		Meldungen meldung = new Meldungen();
		int letzteZeile = spielerSpalte.getLetzteDatenZeile();

		if (letzteZeile >= ERSTE_DATEN_ZEILE) {
			// daten vorhanden
			int nichtZusammenSpielenSpalte = setzPositionSpalte();
			int spieltagSpalte = spieltagSpalte(spieltag);

			Position posSpieltag = Position.from(spieltagSpalte, ERSTE_DATEN_ZEILE);
			XSpreadsheet sheet = getSheet();

			for (int spielerZeile = ERSTE_DATEN_ZEILE; spielerZeile <= letzteZeile; spielerZeile++) {

				int isAktiv = getSheetHelper().getIntFromCell(sheet, posSpieltag.zeile(spielerZeile));
				SpielrundeGespielt status = SpielrundeGespielt.findById(isAktiv);

				if (spielrundeGespielt == null || spielrundeGespielt.contains(status)) {
					int spielerNr = getSheetHelper().getIntFromCell(sheet, Position.from(posSpieltag).spalte(SPIELER_NR_SPALTE));
					if (spielerNr > 0) {
						Spieler spieler = Spieler.from(spielerNr);

						if (nichtZusammenSpielenSpalte > -1) {
							int nichtzusammen = getSheetHelper().getIntFromCell(sheet, Position.from(posSpieltag).spalte(nichtZusammenSpielenSpalte));
							if (nichtzusammen > 0) {
								spieler.setSetzPos(nichtzusammen);
							}
						}
						meldung.addSpielerWennNichtVorhanden(spieler);
					}
				}
			}
		}
		return meldung;
	}

	@Override
	public int getSpielerZeileNr(int spielerNr) throws GenerateException {
		return spielerSpalte.getSpielerZeileNr(spielerNr);
	}

	@Override
	public List<String> getSpielerNamenList() throws GenerateException {
		return spielerSpalte.getSpielerNamenList();
	}

	@Override
	public List<Integer> getSpielerNrList() throws GenerateException {
		return spielerSpalte.getSpielerNrList();
	}

	@Override
	public int neachsteFreieDatenZeile() throws GenerateException {
		return spielerSpalte.neachsteFreieDatenZeile();
	}

	@Override
	public void spielerEinfuegenWennNichtVorhanden(int spielerNr) throws GenerateException {
		spielerSpalte.spielerEinfuegenWennNichtVorhanden(spielerNr);
	}

	@Override
	public int letzteDatenZeile() throws GenerateException {
		return spielerSpalte.getLetzteDatenZeile();
	}

	@Override
	public int getErsteDatenZiele() {
		return spielerSpalte.getErsteDatenZiele();
	}

	public final SpielTagNr getSpielTag() {
		checkNotNull(spielTag, "spielTag == null");
		return spielTag;
	}

	public final void setSpielTag(SpielTagNr spielTag) throws GenerateException {
		checkNotNull(spielTag, "spielTag == null");
		ProcessBox.from().spielTag(spielTag);
		this.spielTag = spielTag;
	}

	public void setAktiveSpieltag(SpielTagNr spielTagNr) throws GenerateException {
		getKonfigurationSheet().setAktiveSpieltag(spielTagNr);
	}

	@Override
	public Meldungen getAktiveUndAusgesetztMeldungen() throws GenerateException {
		return getMeldungen(getSpielTag(), Arrays.asList(SpielrundeGespielt.JA, SpielrundeGespielt.AUSGESETZT));
	}

	@Override
	public Meldungen getAktiveMeldungen() throws GenerateException {
		return getMeldungen(getSpielTag(), Arrays.asList(SpielrundeGespielt.JA));
	}

	@Override
	public Meldungen getAlleMeldungen() throws GenerateException {
		return getMeldungen(getSpielTag(), null);
	}

	public int getSpielerNameErsteSpalte() {
		return spielerSpalte.getSpielerNameErsteSpalte();
	}

	/**
	 * @param SpielRundeNr
	 * @throws GenerateException
	 */
	public void setAktiveSpielRunde(SpielRundeNr spielRundeNr) throws GenerateException {
		getKonfigurationSheet().setAktiveSpielRunde(spielRundeNr);
	}
}
