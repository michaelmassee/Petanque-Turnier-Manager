/**
* Erstellung : 22.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee.meldeliste;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.google.common.annotations.VisibleForTesting;
import com.sun.star.sheet.ConditionOperator;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.CellVertJustify2;
import com.sun.star.table.TableBorder2;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.IMeldeliste;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeHelper;
import de.petanqueturniermanager.basesheet.meldeliste.MeldungenSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.SpielrundeGespielt;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ColorHelper;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellstyle.MeldungenHintergrundFarbeGeradeStyle;
import de.petanqueturniermanager.helper.cellstyle.MeldungenHintergrundFarbeUnGeradeStyle;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.CellProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.pagestyle.PageStyle;
import de.petanqueturniermanager.helper.pagestyle.PageStyleHelper;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.ConditionalFormatHelper;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetFreeze;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.model.Spieler;
import de.petanqueturniermanager.model.SpielerMeldungen;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.SupermeleeTeamPaarungenSheet;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeSheet;

abstract public class AbstractSupermeleeMeldeListeSheet extends SuperMeleeSheet
		implements IMeldeliste<SpielerMeldungen, Spieler> {
	private static final String SPIELTAG_HEADER_STR = "Spieltag";

	public static final int SPALTE_FORMATION = 0; // siehe enum #Formation Spalte 0
	public static final int ZEILE_FORMATION = 0; // Zeile 0

	public static final int MIN_ANZAHL_SPIELER_ZEILEN = 200; // Tablle immer mit min anzahl von zeilen formatieren

	public static final int SUMMEN_SPALTE_OFFSET = 2; // 2 Spalten weiter zur letzte Spieltag
	public static final int SUMMEN_ERSTE_ZEILE = ERSTE_DATEN_ZEILE + 5; // Zeile 3
	public static final int SUMMEN_AKTIVE_ZEILE = SUMMEN_ERSTE_ZEILE; // Zeile 6
	public static final int SUMMEN_INAKTIVE_ZEILE = SUMMEN_ERSTE_ZEILE + 1;
	public static final int SUMMEN_AUSGESTIEGENE_ZEILE = SUMMEN_INAKTIVE_ZEILE + 1; // Zeile 8
	public static final int SUMMEN_ANZ_SPIELER = SUMMEN_AUSGESTIEGENE_ZEILE + 1;
	public static final int SUMMEN_GESAMT_ANZ_SPIELER = SUMMEN_ANZ_SPIELER + 1;

	// ab hier summen für Supermelee Mode Triplette
	public static final int TRIPL_MODE_HEADER = SUMMEN_GESAMT_ANZ_SPIELER + 2;
	public static final int TRIPL_MODE_ANZ_DOUBLETTE = TRIPL_MODE_HEADER + 1;
	public static final int TRIPL_MODE_ANZ_TRIPLETTE = TRIPL_MODE_ANZ_DOUBLETTE + 1;
	public static final int TRIPL_MODE_SUMMEN_KANN_DOUBLETTE_ZEILE = TRIPL_MODE_ANZ_TRIPLETTE + 1;
	public static final int TRIPL_MODE_SUMMEN_SPIELBAHNEN = TRIPL_MODE_SUMMEN_KANN_DOUBLETTE_ZEILE + 1;

	// ab hier summen für Supermelee Mode Doublette
	public static final int DOUBL_MODE_HEADER = TRIPL_MODE_SUMMEN_SPIELBAHNEN + 2;
	public static final int DOUBL_MODE_ANZ_DOUBLETTE = DOUBL_MODE_HEADER + 1;
	public static final int DOUBL_MODE_ANZ_TRIPLETTE = DOUBL_MODE_ANZ_DOUBLETTE + 1;
	public static final int DOUBL_MODE_SUMMEN_KANN_TRIPLETTE_ZEILE = DOUBL_MODE_ANZ_TRIPLETTE + 1;
	public static final int DOUBL_MODE_SUMMEN_SPIELBAHNEN = DOUBL_MODE_SUMMEN_KANN_TRIPLETTE_ZEILE + 1;

	public static final int ERSTE_ZEILE_INFO = 0; // Zeile 1

	private final MeldungenSpalte<SpielerMeldungen, Spieler> meldungenSpalte;
	private final SupermeleeTeamPaarungenSheet supermeleeTeamPaarungen;
	private final MeldeListeHelper<SpielerMeldungen, Spieler> meldeListeHelper;
	private SpielTagNr spielTag = null;

	public AbstractSupermeleeMeldeListeSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, "Meldeliste");
		meldungenSpalte = MeldungenSpalte.Builder().ersteDatenZiele(ERSTE_DATEN_ZEILE)
				.spielerNrSpalte(SPIELER_NR_SPALTE).sheet(this).formation(Formation.MELEE).build();
		supermeleeTeamPaarungen = initSupermeleeTeamPaarungenSheet();
		meldeListeHelper = new MeldeListeHelper<>(this);
	}

	@VisibleForTesting
	SupermeleeTeamPaarungenSheet initSupermeleeTeamPaarungenSheet() {
		return new SupermeleeTeamPaarungenSheet(getWorkingSpreadsheet());

	}

	/**
	 * anzahl header zählen
	 *
	 * @return
	 * @throws GenerateException
	 */
	public int countAnzSpieltageInMeldeliste() throws GenerateException {
		int anzSpieltage = 0;
		int ersteSpieltagspalteSpalte = meldeListeHelper.ersteSpieltagSpalte();
		Position posHeader = Position.from(ersteSpieltagspalteSpalte, ZWEITE_HEADER_ZEILE);

		for (int spielTagcntr = 1; spielTagcntr < 90; spielTagcntr++) {
			String header = getSheetHelper().getTextFromCell(getXSpreadSheet(), posHeader);

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

	// Delegate
	public void doSort(int spalteNr, boolean isAscending) throws GenerateException {
		meldeListeHelper.doSort(spalteNr, isAscending);
	}

	@Override
	public final TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return meldeListeHelper.getXSpreadSheet();
	}

	public void upDateSheet() throws GenerateException {
		PageStyleHelper.from(this, PageStyle.PETTURNMNGR).initDefaultFooter().create().applytoSheet();
		processBoxinfo("Aktualisiere Meldungen");

		meldeListeHelper.testDoppelteMeldungen();
		getTurnierSheet().setActiv();

		// for test only
		// String formula = getSheetHelper().getFormulaFromCell(getXSpreadSheet(), Position.from(6, 1));

		// ------
		// Header einfuegen
		// ------
		int headerBackColor = getKonfigurationSheet().getMeldeListeHeaderFarbe();
		meldungenSpalte.insertHeaderInSheet(headerBackColor);

		// ------
		// Setzposition
		ColumnProperties columnProp = ColumnProperties.from().setHoriJustify(CellHoriJustify.CENTER).setWidth(800);
		StringCellValue bezCelVal = StringCellValue
				.from(getXSpreadSheet(), meldeListeHelper.setzPositionSpalte(), ZWEITE_HEADER_ZEILE, "SP")
				.setComment("1 = Setzposition, Diesen Spieler werden nicht zusammen im gleichen Team gelost.")
				.setCellBackColor(headerBackColor).setBorder(BorderFactory.from().allThin().toBorder())
				.addColumnProperties(columnProp).setVertJustify(CellVertJustify2.CENTER);
		getSheetHelper().setStringValueInCell(bezCelVal);
		// ------

		formatSpielTagSpalte(getSpielTag());

		// eventuelle luecken in spiele namen nach unten sortieren
		meldeListeHelper.zeileOhneSpielerNamenEntfernen();
		meldeListeHelper.updateMeldungenNr();

		doSort(meldungenSpalte.getSpielerNameErsteSpalte(), true); // nach namen sortieren
		updateSpieltageSummenSpalten();
		insertInfoBlock();
		meldungenSpalte.formatDaten();
		formatDaten();

		// TurnierSystem
		meldeListeHelper.insertTurnierSystemInHeader(getTurnierSystem());

		// headerlines
		SheetFreeze.from(getTurnierSheet()).anzZeilen(2).doFreeze();
	}

	/**
	 * Aktive Spielrunde und Spieltag
	 * 
	 * @throws GenerateException
	 */

	private void insertInfoBlock() throws GenerateException {
		processBoxinfo("Info Block");
		XSpreadsheet sheet = getXSpreadSheet();
		int headerBackColor = getKonfigurationSheet().getMeldeListeHeaderFarbe();
		TableBorder2 border = BorderFactory.from().allThin().toBorder();

		Position posInfo = Position.from(ersteSummeSpalte(), ERSTE_ZEILE_INFO);

		StringCellValue labelVal = StringCellValue.from(sheet, posInfo, "Spieltag").setComment("Aktive Spieltag")
				.setBorder(border).setCellBackColor(headerBackColor);
		getSheetHelper().setStringValueInCell(labelVal);
		labelVal.zeilePlus(1).setValue("Spielrunde").setComment("Aktive Spielrunde");
		getSheetHelper().setStringValueInCell(labelVal);
		// ---------------------------------------------------
		setProperties();

	}

	protected void setProperties() throws GenerateException {
		XSpreadsheet sheet = getXSpreadSheet();
		TableBorder2 border = BorderFactory.from().allThin().toBorder();
		Position posInfo = Position.from(ersteSummeSpalte(), ERSTE_ZEILE_INFO);
		Position posSpieltagFormula = Position.from(posInfo).spaltePlus(1);
		StringCellValue spielTagFormula = StringCellValue.from(sheet, posSpieltagFormula, SuperMeleeSheet.PTM_SPIELTAG)
				.setBorder(border);
		getSheetHelper().setFormulaInCell(spielTagFormula);
//		NumberCellValue spielTagNr = NumberCellValue
//				.from(sheet, posSpieltagFormula, this.getKonfigurationSheet().getAktiveSpieltag().getNr())
//				.setBorder(border);
//		getSheetHelper().setValInCell(spielTagNr);

		Position posSpielrundeFormula = Position.from(posSpieltagFormula).zeilePlusEins();
		StringCellValue spielRundeFormula = StringCellValue
				.from(sheet, posSpielrundeFormula, SuperMeleeSheet.PTM_SPIELRUNDE).setBorder(border);
		getSheetHelper().setFormulaInCell(spielRundeFormula);
//		NumberCellValue spielRndNr = NumberCellValue
//				.from(sheet, posSpielrundeFormula, this.getKonfigurationSheet().getAktiveSpielRunde().getNr())
//				.setBorder(border);
//		getSheetHelper().setValInCell(spielRndNr);

	}

	protected void formatSpielTagSpalte(SpielTagNr spieltag) throws GenerateException {
		checkNotNull(spieltag);

		processBoxinfo("Formatiere Spieltagspalte");

		XSpreadsheet sheet = getXSpreadSheet();
		int hederBackColor = getKonfigurationSheet().getRanglisteHeaderFarbe();
		ColumnProperties columnProp = ColumnProperties.from().setHoriJustify(CellHoriJustify.CENTER)
				.setVertJustify(CellVertJustify2.CENTER).setWidth(2000).margin(CELL_MARGIN);

		StringCellValue bezCelSpieltagVal = StringCellValue
				.from(sheet, meldeListeHelper.spieltagSpalte(spieltag), ZWEITE_HEADER_ZEILE, spielTagHeader(spieltag))
				.setComment("1 = Aktiv, 2 = Ausgestiegen, leer = InAktiv").setCellBackColor(hederBackColor)
				.addColumnProperties(columnProp).setBorder(BorderFactory.from().allThin().toBorder());

		// Spieltag header
		bezCelSpieltagVal.setValue(spielTagHeader(spieltag));
		getSheetHelper().setStringValueInCell(bezCelSpieltagVal);

		// // Aktiv / Inaktiv spieltag
		// TODO wieder einbauen
		// // =WENN(WENNNV(SVERWEIS("Spieltag";$Konfiguration.$A$2:$B$101;2;0);0)=2;"Aktiv";"")
		// String formulaStr = "IF(IFNA(VLOOKUP(\"" + SuperMeleePropertiesSpalte.KONFIG_PROP_NAME_SPIELTAG + "\";$" + IKonfigurationKonstanten.SHEETNAME + "."
		// + getKonfigurationSheet().suchMatrixProperty() + ";2;0);0)=" + spieltag.getNr() + ";\"Aktiv\";\"\"";

		String formulaStr = "IF(" + SuperMeleeSheet.PTM_SPIELTAG + "=" + spieltag.getNr() + ";\"Aktiv\";\"\")";
		StringCellValue aktivFormula = StringCellValue
				.from(sheet, meldeListeHelper.spieltagSpalte(spieltag), ERSTE_HEADER_ZEILE, formulaStr)
				.setCharColor(ColorHelper.CHAR_COLOR_GREEN);
		getSheetHelper().setFormulaInCell(aktivFormula);
	}

	void formatDaten() throws GenerateException {

		processBoxinfo("Formatiere Daten Spalten");

		int letzteDatenZeile = meldungenSpalte.getLetzteDatenZeile();

		if (letzteDatenZeile < MIN_ANZAHL_SPIELER_ZEILEN) {
			letzteDatenZeile = MIN_ANZAHL_SPIELER_ZEILEN;
		}

		if (letzteDatenZeile < ERSTE_DATEN_ZEILE) {
			// keine Daten
			return;
		}

		RangePosition datenRange = RangePosition.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE, letzteSpielTagSpalte(),
				letzteDatenZeile);

		getSheetHelper().setPropertiesInRange(getXSpreadSheet(), datenRange,
				CellProperties.from().setVertJustify(CellVertJustify2.CENTER)
						.setBorder(BorderFactory.from().allThin().boldLn().forTop().forLeft().toBorder())
						.setCharColor(ColorHelper.CHAR_COLOR_BLACK).setCellBackColor(-1).setShrinkToFit(true));

		// gerade / ungrade hintergrund farbe
		// CellBackColor
		Integer geradeColor = getKonfigurationSheet().getMeldeListeHintergrundFarbeGerade();
		Integer unGeradeColor = getKonfigurationSheet().getMeldeListeHintergrundFarbeUnGerade();
		MeldungenHintergrundFarbeGeradeStyle meldungenHintergrundFarbeGeradeStyle = new MeldungenHintergrundFarbeGeradeStyle(
				geradeColor);
		MeldungenHintergrundFarbeUnGeradeStyle meldungenHintergrundFarbeUnGeradeStyle = new MeldungenHintergrundFarbeUnGeradeStyle(
				unGeradeColor);

		// Meldung Nummer: gerade + ungerade + prufe auf doppelte nummer
		// TODO Move this nach Meldungen
		// -----------------------------------------------
		RangePosition nrSetPosRange = RangePosition.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE, SPIELER_NR_SPALTE,
				letzteDatenZeile);
		String conditionfindDoppeltNr = "COUNTIF(" + Position.from(SPIELER_NR_SPALTE, 0).getSpalteAddressWith$() + ";"
				+ ConditionalFormatHelper.FORMULA_CURRENT_CELL + ")>1";
		ConditionalFormatHelper.from(this, nrSetPosRange).clear().
		// ------------------------------
				formulaIsText().styleIsFehler().applyAndDoReset().
				// ------------------------------
				formula1(conditionfindDoppeltNr).operator(ConditionOperator.FORMULA).styleIsFehler().applyAndDoReset().
				// ------------------------------
				formula1("0").formula2("" + MeldungenSpalte.MAX_ANZ_MELDUNGEN).operator(ConditionOperator.NOT_BETWEEN)
				.styleIsFehler().applyAndDoReset(). // nr muss >0 und
													// <999 sein
				// ------------------------------
				formulaIsEvenRow().style(meldungenHintergrundFarbeGeradeStyle).applyAndDoReset().
				// ------------------------------
				formulaIsOddRow().style(meldungenHintergrundFarbeUnGeradeStyle).applyAndDoReset();
		// -----------------------------------------------

		// -----------------------------------------------
		// Spieler Namen: gerade + ungerade + prufe auf doppelte namen
		// TODO Move this nach Abstract Meldungen
		// -----------------------------------------------
		RangePosition nameSetPosRange = RangePosition.from(getSpielerNameErsteSpalte(), ERSTE_DATEN_ZEILE,
				getSpielerNameErsteSpalte(), letzteDatenZeile);
		String conditionfindDoppeltNamen = "COUNTIF("
				+ Position.from(getSpielerNameErsteSpalte(), 0).getSpalteAddressWith$() + ";"
				+ ConditionalFormatHelper.FORMULA_CURRENT_CELL + ")>1";
		ConditionalFormatHelper.from(this, nameSetPosRange).clear().
		// ------------------------------
				formula1(conditionfindDoppeltNamen).operator(ConditionOperator.FORMULA).styleIsFehler()
				.applyAndDoReset().
				// ------------------------------
				formulaIsEvenRow().operator(ConditionOperator.FORMULA).style(meldungenHintergrundFarbeGeradeStyle)
				.applyAndDoReset().
				// ------------------------------
				formulaIsEvenRow().style(meldungenHintergrundFarbeGeradeStyle).applyAndDoReset().formulaIsOddRow()
				.style(meldungenHintergrundFarbeUnGeradeStyle).applyAndDoReset();
		// -----------------------------------------------

		// -----------------------------------------------
		// setzposition spalte
		// -----------------------------------------------
		RangePosition setzpositionRangePos = RangePosition.from(meldeListeHelper.setzPositionSpalte(),
				ERSTE_DATEN_ZEILE, meldeListeHelper.setzPositionSpalte(), letzteDatenZeile);
		ConditionalFormatHelper.from(this, setzpositionRangePos).clear().
		// ------------------------------
				formula1("0").formula2("90").operator(ConditionOperator.NOT_BETWEEN).styleIsFehler().applyAndDoReset().
				// ------------------------------
				formulaIsText().styleIsFehler().applyAndDoReset().
				// ------------------------------
				formulaIsEvenRow().style(meldungenHintergrundFarbeGeradeStyle).applyAndDoReset().
				// ------------------------------
				formulaIsOddRow().style(meldungenHintergrundFarbeUnGeradeStyle).applyAndDoReset();

		// -----------------------------------------------
		// Spieltag spalten
		// prüfe wenn <0 und > 2
		// test if Text mit FORMULA
		// reihenfolge beachten
		// ------------------------------
		RangePosition spieltageRangePos = RangePosition.from(meldeListeHelper.ersteSpieltagSpalte(), ERSTE_DATEN_ZEILE,
				letzteSpielTagSpalte(), letzteDatenZeile);
		ConditionalFormatHelper.from(this, spieltageRangePos).clear().
		// ------------------------------
				formula1("0").formula2("2").operator(ConditionOperator.NOT_BETWEEN).styleIsFehler().applyAndDoReset().
				// ------------------------------
				formulaIsText().styleIsFehler().applyAndDoReset().
				// ------------------------------
				formulaIsEvenRow().style(meldungenHintergrundFarbeGeradeStyle).applyAndDoReset().
				// ------------------------------
				formulaIsOddRow().style(meldungenHintergrundFarbeUnGeradeStyle).applyAndDoReset();
	}

	/**
	 * @param spieltag = 1 bis x
	 * @return
	 * @throws GenerateException
	 */
	public String spielTagHeader(SpielTagNr spieltag) throws GenerateException {
		return spieltag.getNr() + ". " + SPIELTAG_HEADER_STR;
	}

	@Override
	public int letzteSpielTagSpalte() throws GenerateException {
		int anzSpieltage = countAnzSpieltageInMeldeliste();
		return meldeListeHelper.ersteSpieltagSpalte() + (anzSpieltage - 1);
	}

	/**
	 *
	 * @return spalte zum getSpielTag()
	 * @throws GenerateException
	 */

	public int aktuelleSpieltagSpalte() throws GenerateException {
		return meldeListeHelper.spieltagSpalte(getSpielTag());
	}

	private int ersteSummeSpalte() throws GenerateException {
		return letzteSpielTagSpalte() + SUMMEN_SPALTE_OFFSET;
	}

	@Override
	public String formulaSverweisSpielernamen(String spielrNrAdresse) {
		return meldeListeHelper.formulaSverweisSpielernamen(spielrNrAdresse);
	}

	@Override
	public int getSpielerNameSpalte() {
		return meldungenSpalte.getSpielerNameErsteSpalte();
	}

	private void updateSpieltageSummenSpalten() throws GenerateException {

		processBoxinfo("Aktualisiere Summen Spalten");

		int headerBackColor = getKonfigurationSheet().getMeldeListeHeaderFarbe();

		int letzteDatenZeile = meldungenSpalte.getLetzteDatenZeile();

		if (letzteDatenZeile < MIN_ANZAHL_SPIELER_ZEILEN) {
			letzteDatenZeile = MIN_ANZAHL_SPIELER_ZEILEN;
		}

		XSpreadsheet sheet = getXSpreadSheet();

		int anzSpieltage = countAnzSpieltageInMeldeliste();

		RangePosition cleanUpRange = RangePosition.from(ersteSummeSpalte() - 1, 0,
				ersteSummeSpalte() + anzSpieltage + 10, MeldungenSpalte.MAX_ANZ_MELDUNGEN);
		RangeHelper.from(this, cleanUpRange).clearRange();

		Position posBezeichnug = Position.from(ersteSummeSpalte(), SUMMEN_ERSTE_ZEILE - 1);

		TableBorder2 border = BorderFactory.from().allThin().toBorder();

		ColumnProperties columnProp = ColumnProperties.from().setHoriJustify(CellHoriJustify.RIGHT).setWidth(3000)
				.margin(CELL_MARGIN);
		getSheetHelper().setColumnProperties(getXSpreadSheet(), posBezeichnug.getSpalte(), columnProp);

		StringCellValue bezCelVal = StringCellValue.from(sheet, posBezeichnug, "").setComment(null)
				.removeCellBackColor();
		getSheetHelper().setStringValueInCell(bezCelVal);

		bezCelVal.setBorder(border).setCellBackColor(headerBackColor);

		// ------------------------------------------------------------------------------------
		// public static final int SUMMEN_AKTIVE_ZEILE = SUMMEN_ERSTE_ZEILE; // Zeile 6
		// public static final int SUMMEN_INAKTIVE_ZEILE = SUMMEN_ERSTE_ZEILE + 1;
		// public static final int SUMMEN_AUSGESTIEGENE_ZEILE = SUMMEN_INAKTIVE_ZEILE + 1; // Zeile 8
		// public static final int SUMMEN_ANZ_SPIELER = SUMMEN_AUSGESTIEGENE_ZEILE + 1;
		// public static final int SUMMEN_GESAMT_ANZ_SPIELER = SUMMEN_ANZ_SPIELER + 1;
		// ------------------------------------------------------------------------------------

		bezCelVal.setComment("Anzahl Spieler mit \"1\" im Spieltag").setValue("Aktiv").zeile(SUMMEN_AKTIVE_ZEILE);
		getSheetHelper().setStringValueInCell(bezCelVal);

		bezCelVal.setComment("Anzahl Spieler mit \"\" im Spieltag").setValue("InAktiv").zeile(SUMMEN_INAKTIVE_ZEILE);
		getSheetHelper().setStringValueInCell(bezCelVal);

		bezCelVal.setComment("Anzahl Spieler mit \"2\" im Spieltag").setValue("Ausgestiegen")
				.zeile(SUMMEN_AUSGESTIEGENE_ZEILE);
		getSheetHelper().setStringValueInCell(bezCelVal);

		bezCelVal.setComment("Anzahl Aktive + Ausgestiegen").setValue("Akt + Ausg").zeile(SUMMEN_ANZ_SPIELER);
		getSheetHelper().setStringValueInCell(bezCelVal);

		bezCelVal.setComment("Anzahl Aktive + Inaktiv + Ausgestiegen").setValue("Summe")
				.zeile(SUMMEN_GESAMT_ANZ_SPIELER);
		getSheetHelper().setStringValueInCell(bezCelVal);

		// ------------------------------------------------------------------------------------
		// ab hier summen für Supermelee Mode Triplette
		// public static final int TRIPL_MODE_HEADER = SUMMEN_AUSGESTIEGENE_ZEILE + 2;
		// public static final int TRIPL_MODE_ANZ_DOUBLETTE = TRIPL_MODE_HEADER + 1;
		// public static final int TRIPL_MODE_ANZ_TRIPLETTE = TRIPL_MODE_ANZ_DOUBLETTE + 1;
		// public static final int TRIPL_MODE_SUMMEN_KANN_DOUBLETTE_ZEILE = TRIPL_MODE_ANZ_TRIPLETTE + 1;
		// public static final int TRIPL_MODE_SUMMEN_SPIELBAHNEN = TRIPL_MODE_SUMMEN_KANN_DOUBLETTE_ZEILE + 1;
		// ------------------------------------------------------------------------------------

		// Modus Header
		// Summen wenn Supermêlée Modus
		StringCellValue modusHeaderVal = StringCellValue.from(getXSpreadSheet()).spalte(ersteSummeSpalte())
				.zeile(TRIPL_MODE_HEADER).setHoriJustify(CellHoriJustify.CENTER).setValue("Supermêlée Triplette.")
				.setComment("Triplette Teams, aufüllen mit Doublette.  Siehe Konfiguration, 'Supermêlée Modus'")
				.setEndPosMergeSpaltePlus(getSpielTag().getNr());
		getSheetHelper().setStringValueInCell(modusHeaderVal);

		// Daten
		bezCelVal.setComment("Modus Triplette, Anzahl Doublette Teams").setValue("∑x2").zeile(TRIPL_MODE_ANZ_DOUBLETTE);
		getSheetHelper().setStringValueInCell(bezCelVal);

		bezCelVal.setComment("Modus Triplette, Anzahl Triplette Teams").setValue("∑x3").zeile(TRIPL_MODE_ANZ_TRIPLETTE);
		getSheetHelper().setStringValueInCell(bezCelVal);

		bezCelVal.setComment("Modus Triplette, Kann Doublette gespielt werden").setValue("Doublette");
		getSheetHelper().setStringValueInCell(bezCelVal.zeile(TRIPL_MODE_SUMMEN_KANN_DOUBLETTE_ZEILE));

		bezCelVal.setComment("Modus Triplette, Anzahl Spielbahnen").setValue("Bahnen");
		getSheetHelper().setStringValueInCell(bezCelVal.zeile(TRIPL_MODE_SUMMEN_SPIELBAHNEN));

		// ------------------------------------------------------------------------------------
		// ab hier summen für Supermelee Mode Doublette
		// public static final int DOUBL_MODE_HEADER = TRIPL_MODE_SUMMEN_SPIELBAHNEN + 2;
		// public static final int DOUBL_MODE_ANZ_DOUBLETTE = DOUBL_MODE_HEADER + 1;
		// public static final int DOUBL_MODE_ANZ_TRIPLETTE = DOUBL_MODE_ANZ_DOUBLETTE + 1;
		// public static final int DOUBL_MODE_SUMMEN_KANN_TRIPLETTE_ZEILE = DOUBL_MODE_ANZ_TRIPLETTE + 1;
		// public static final int DOUBL_MODE_SUMMEN_SPIELBAHNEN = DOUBL_MODE_SUMMEN_KANN_TRIPLETTE_ZEILE + 1;
		// ------------------------------------------------------------------------------------

		modusHeaderVal.spalte(ersteSummeSpalte()).zeile(DOUBL_MODE_HEADER)
				.setEndPosMergeSpaltePlus(getSpielTag().getNr()).setValue("Supermêlée Doublette.")
				.setComment("Doublette Teams, aufüllen mit Triplette. Siehe Konfiguration, 'Supermêlée Modus'");
		getSheetHelper().setStringValueInCell(modusHeaderVal);

		bezCelVal.setComment("Modus Doublette, Anzahl Doublette").setValue("∑x2");
		getSheetHelper().setStringValueInCell(bezCelVal.zeile(DOUBL_MODE_ANZ_DOUBLETTE));

		bezCelVal.setComment("Modus Doublette, Anzahl Triplette").setValue("∑x3");
		getSheetHelper().setStringValueInCell(bezCelVal.zeile(DOUBL_MODE_ANZ_TRIPLETTE));

		bezCelVal.setComment("Modus Doublette, Kann Triplette gespielt werden").setValue("Triplette");
		getSheetHelper().setStringValueInCell(bezCelVal.zeile(DOUBL_MODE_SUMMEN_KANN_TRIPLETTE_ZEILE));

		bezCelVal.setComment("Modus Doublette,Anzahl Spielbahnen").setValue("Bahnen");
		getSheetHelper().setStringValueInCell(bezCelVal.zeile(DOUBL_MODE_SUMMEN_SPIELBAHNEN));
		// ------------------------------------------------------------------------------------

		StringCellValue formula = StringCellValue.from(getXSpreadSheet()).setBorder(border);
		ColumnProperties spalteWertProp = ColumnProperties.from().setWidth(1200).centerJustify().margin(CELL_MARGIN);

		for (int spieltagCntr = 1; spieltagCntr <= anzSpieltage; spieltagCntr++) {

			SpielTagNr spielTagNr = new SpielTagNr(spieltagCntr);

			Position posSpieltagWerte = Position.from(ersteSummeSpalte() + spieltagCntr, SUMMEN_ERSTE_ZEILE - 1);

			// Tag Header
			StringCellValue tagHeader = StringCellValue.from(getXSpreadSheet()).setPos(posSpieltagWerte)
					.setBorder(border).setValue("Tag " + spieltagCntr).setColumnProperties(spalteWertProp)
					.setCellBackColor(headerBackColor);
			getSheetHelper().setStringValueInCell(tagHeader);

			// ------------------------------------------------------------------------------------
			// public static final int SUMMEN_AKTIVE_ZEILE = SUMMEN_ERSTE_ZEILE; // Zeile 6
			// public static final int SUMMEN_INAKTIVE_ZEILE = SUMMEN_ERSTE_ZEILE + 1;
			// public static final int SUMMEN_AUSGESTIEGENE_ZEILE = SUMMEN_INAKTIVE_ZEILE + 1; // Zeile 8
			// public static final int SUMMEN_ANZ_SPIELER = SUMMEN_AUSGESTIEGENE_ZEILE + 1;
			// public static final int SUMMEN_GESAMT_ANZ_SPIELER = SUMMEN_ANZ_SPIELER + 1;
			// ------------------------------------------------------------------------------------
			String formulaStr = formulaCountSpieler(spielTagNr, "1", letzteDatenZeile);
			getSheetHelper()
					.setFormulaInCell(formula.setPos(posSpieltagWerte.zeile(SUMMEN_AKTIVE_ZEILE)).setValue(formulaStr));
			String aktivZelle = posSpieltagWerte.getAddress(); // Pos Merken

			formulaStr = formulaCountSpieler(spielTagNr, "0", letzteDatenZeile) + " + "
					+ formulaCountSpieler(spielTagNr, "\"\"", letzteDatenZeile);
			getSheetHelper().setFormulaInCell(
					formula.setPos(posSpieltagWerte.zeile(SUMMEN_INAKTIVE_ZEILE)).setValue(formulaStr));
			String inAktivZelle = posSpieltagWerte.getAddress(); // Pos Merken

			formulaStr = formulaCountSpieler(spielTagNr, "2", letzteDatenZeile);
			getSheetHelper().setFormulaInCell(
					formula.setPos(posSpieltagWerte.zeile(SUMMEN_AUSGESTIEGENE_ZEILE)).setValue(formulaStr));
			String ausgestiegenZelle = posSpieltagWerte.getAddress(); // Pos Merken

			// -----------------------------------
			// Aktiv + Ausgestiegen
			formulaStr = aktivZelle + "+" + ausgestiegenZelle;
			getSheetHelper()
					.setFormulaInCell(formula.setPos(posSpieltagWerte.zeile(SUMMEN_ANZ_SPIELER)).setValue(formulaStr));
			// -----------------------------------
			// =K7+K8+K9
			// Aktiv + Ausgestiegen + inaktive
			formulaStr = aktivZelle + "+" + inAktivZelle + "+" + ausgestiegenZelle;
			getSheetHelper().setFormulaInCell(
					formula.setPos(posSpieltagWerte.zeile(SUMMEN_GESAMT_ANZ_SPIELER)).setValue(formulaStr));
			// -----------------------------------

			// ------------------------------------------------------------------------------------
			// Triplette mode
			// ------------------------------------------------------------------------------------
			String anzSpielerAddr = getSheetHelper()
					.getAddressFromColumnRow(getAnzahlAktiveSpielerPosition(spielTagNr));
			String formulaSverweisAnzDoublette = supermeleeTeamPaarungen.formulaSverweisAnzDoublette(anzSpielerAddr);
			getSheetHelper().setFormulaInCell(formula.setPos(posSpieltagWerte.zeile(TRIPL_MODE_ANZ_DOUBLETTE))
					.setValue(formulaSverweisAnzDoublette));
			String anzDoublZelle = posSpieltagWerte.getAddress(); // Position merken

			String formulaSverweisAnzTriplette = supermeleeTeamPaarungen.formulaSverweisAnzTriplette(anzSpielerAddr);
			getSheetHelper().setFormulaInCell(formula.setPos(posSpieltagWerte.zeile(TRIPL_MODE_ANZ_TRIPLETTE))
					.setValue(formulaSverweisAnzTriplette));

			String anzTriplZelle = posSpieltagWerte.getAddress(); // Position merken

			String formulaSverweisNurDoublette = supermeleeTeamPaarungen.formulaSverweisNurDoublette(anzSpielerAddr);
			getSheetHelper()
					.setFormulaInCell(formula.setPos(posSpieltagWerte.zeile(TRIPL_MODE_SUMMEN_KANN_DOUBLETTE_ZEILE))
							.setValue(formulaSverweisNurDoublette));

			// -----------------------------------
			String formulaAnzSpielbahnen = "=(" + anzDoublZelle + " + " + anzTriplZelle + ")/2";
			getSheetHelper().setFormulaInCell(formula.setPos(posSpieltagWerte.zeile(TRIPL_MODE_SUMMEN_SPIELBAHNEN))
					.setValue(formulaAnzSpielbahnen));

			// ------------------------------------------------------------------------------------
			// Doublette mode
			// ------------------------------------------------------------------------------------
			String doublettModeformulaSverweisAnzDoublette = supermeleeTeamPaarungen
					.formulaSverweisDoubletteModeAnzDoublette(anzSpielerAddr);
			getSheetHelper().setFormulaInCell(formula.setPos(posSpieltagWerte.zeile(DOUBL_MODE_ANZ_DOUBLETTE))
					.setValue(doublettModeformulaSverweisAnzDoublette));

			String doublettteModeAnzDoublZelle = getSheetHelper().getAddressFromColumnRow(posSpieltagWerte); // Position merken

			String doublettModeformulaSverweisAnzTriplette = supermeleeTeamPaarungen
					.formulaSverweisAnzDoubletteModeAnzTriplette(anzSpielerAddr);
			getSheetHelper().setFormulaInCell(formula.setPos(posSpieltagWerte.zeile(DOUBL_MODE_ANZ_TRIPLETTE))
					.setValue(doublettModeformulaSverweisAnzTriplette));

			String doublettteModeAnzTriplZelle = getSheetHelper().getAddressFromColumnRow(posSpieltagWerte); // Position merken

			String doublettModeformulaSverweisNurTriplette = supermeleeTeamPaarungen
					.formulaSverweisDoubletteModeNurTriplette(anzSpielerAddr);
			getSheetHelper()
					.setFormulaInCell(formula.setPos(posSpieltagWerte.zeile(DOUBL_MODE_SUMMEN_KANN_TRIPLETTE_ZEILE))
							.setValue(doublettModeformulaSverweisNurTriplette));

			// -----------------------------------
			String doublettModeFormulaAnzSpielbahnen = "=(" + doublettteModeAnzDoublZelle + " + "
					+ doublettteModeAnzTriplZelle + ")/2";
			getSheetHelper().setFormulaInCell(formula.setPos(posSpieltagWerte.zeile(DOUBL_MODE_SUMMEN_SPIELBAHNEN))
					.setValue(doublettModeFormulaAnzSpielbahnen));
		}

		// Welchen Supermêlée Modus ist Aktiv ?
		// Triplette
		// =WENN(WENNNV(SVERWEIS("Supermêlée Modus";$Konfiguration.$B$3:$C$101;2;0);0)="T";"Aktiv";"")
		// Zelle rechts neben Block
		// {
		// Position aktivAnzeigePos = Position.from(ersteSummeSpalte() + anzSpieltage + 1, TRIPL_MODE_ANZ_DOUBLETTE);
		// String formulaStr = "IF(IFNA(VLOOKUP(\"" + SuperMeleePropertiesSpalte.KONFIG_PROP_SUPERMELEE_MODE + "\";$" + IKonfigurationKonstanten.SHEETNAME + "."
		// + getKonfigurationSheet().suchMatrixProperty() + ";2;0);0)" + "<>\"D\"" // Alle Werte ungleich D = Triplette Mode
		// + ";\"Aktiv\";\"\"";
		// StringCellValue aktivFormula = StringCellValue.from(getXSpreadSheet()).setPos(aktivAnzeigePos).setValue(formulaStr).setRotateAngle(27000)
		// .setVertJustify(CellVertJustify2.CENTER).setEndPosMergeZeile(TRIPL_MODE_SUMMEN_SPIELBAHNEN).setCharColor(ColorHelper.CHAR_COLOR_GREEN)
		// .setCharWeight(FontWeight.BOLD);
		// getSheetHelper().setFormulaInCell(aktivFormula);
		// }
		//
		// // Doublette
		// // Zelle rechts neben Block
		// {
		// Position aktivAnzeigePos = Position.from(ersteSummeSpalte() + anzSpieltage + 1, DOUBL_MODE_ANZ_DOUBLETTE);
		// String formulaStr = "IF(IFNA(VLOOKUP(\"" + SuperMeleePropertiesSpalte.KONFIG_PROP_SUPERMELEE_MODE + "\";$" + IKonfigurationKonstanten.SHEETNAME + "."
		// + getKonfigurationSheet().suchMatrixProperty() + ";2;0);0)" + "=\"D\"" // Alle Werte D = Doublette
		// + ";\"Aktiv\";\"\"";
		// StringCellValue aktivFormula = StringCellValue.from(getXSpreadSheet()).setPos(aktivAnzeigePos).setValue(formulaStr).setRotateAngle(27000)
		// .setVertJustify(CellVertJustify2.CENTER).setEndPosMergeZeile(DOUBL_MODE_SUMMEN_SPIELBAHNEN).setCharColor(ColorHelper.CHAR_COLOR_GREEN)
		// .setCharWeight(FontWeight.BOLD);
		// getSheetHelper().setFormulaInCell(aktivFormula);
		// }

	}

	// ---------------------------------------------
	public int getAnzahlAktiveSpieler(SpielTagNr Spieltag) throws GenerateException {
		return getSheetHelper().getIntFromCell(getXSpreadSheet(), getAnzahlAktiveSpielerPosition(Spieltag));
	}

	public Position getAnzahlAktiveSpielerPosition(SpielTagNr spieltag) throws GenerateException {
		return Position.from(ersteSummeSpalte() + spieltag.getNr(), SUMMEN_AKTIVE_ZEILE);
	}

	// ---------------------------------------------
	public int getAnzahlInAktiveSpieler(SpielTagNr spieltag) throws GenerateException {
		return getSheetHelper().getIntFromCell(getXSpreadSheet(), getAnzahlInAktiveSpielerPosition(spieltag));
	}

	public Position getAnzahlInAktiveSpielerPosition(SpielTagNr spieltag) throws GenerateException {
		return Position.from(ersteSummeSpalte() + spieltag.getNr(), SUMMEN_INAKTIVE_ZEILE);
	}

	// ---------------------------------------------
	public int getAusgestiegenSpieler(SpielTagNr spieltag) throws GenerateException {
		return getSheetHelper().getIntFromCell(getXSpreadSheet(), getAusgestiegenSpielerPosition(spieltag));
	}

	public Position getAusgestiegenSpielerPosition(SpielTagNr spieltag) throws GenerateException {
		return Position.from(ersteSummeSpalte() + spieltag.getNr(), SUMMEN_AUSGESTIEGENE_ZEILE);
	}
	// ---------------------------------------------

	public Boolean isKannNurDoubletteInTripletteMode(SpielTagNr Spieltag) throws GenerateException {
		return StringUtils.isNotBlank(getSheetHelper().getTextFromCell(getXSpreadSheet(),
				getKannNurDoubletteInTripletteModePosition(Spieltag)));
	}

	public Position getKannNurDoubletteInTripletteModePosition(SpielTagNr Spieltag) throws GenerateException {
		return Position.from(ersteSummeSpalte() + Spieltag.getNr(), TRIPL_MODE_SUMMEN_KANN_DOUBLETTE_ZEILE);
	}

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

		int spieltagSpalte = meldeListeHelper.spieltagSpalte(spieltag);
		String ersteZelleSpielTag = Position.from(spieltagSpalte, ERSTE_DATEN_ZEILE).getAddress();
		String letzteZelleSpielTag = Position.from(spieltagSpalte, letzteZeile).getAddress();

		// nur dann zählen wenn name gefüllt
		return "COUNTIFS(" + ersteZelleName + ":" + letzteZelleName + ";\"<>\";" + ersteZelleSpielTag + ":"
				+ letzteZelleSpielTag + ";" + status + ")";
	}

	@Override
	public int getSpielerZeileNr(int spielerNr) throws GenerateException {
		return meldungenSpalte.getSpielerZeileNr(spielerNr);
	}

	@Override
	public List<String> getSpielerNamenList() throws GenerateException {
		return meldungenSpalte.getSpielerNamenList();
	}

	@Override
	public List<Integer> getSpielerNrList() throws GenerateException {
		return meldungenSpalte.getSpielerNrList();
	}

	@Override
	public int neachsteFreieDatenOhneSpielerNrZeile() throws GenerateException {
		return meldungenSpalte.neachsteFreieDatenOhneSpielerNrZeile();
	}

	@Override
	public int letzteDatenZeile() throws GenerateException {
		return meldungenSpalte.getLetzteDatenZeile();
	}

	@Override
	public int getErsteDatenZiele() {
		return meldungenSpalte.getErsteDatenZiele();
	}

	public final SpielTagNr getSpielTag() {
		checkNotNull(spielTag, "spielTag == null");
		return spielTag;
	}

	public final void setSpielTag(SpielTagNr spielTag) {
		checkNotNull(spielTag, "spielTag == null");
		this.spielTag = spielTag;
	}

	public void setAktiveSpieltag(SpielTagNr spielTagNr) throws GenerateException {
		getKonfigurationSheet().setAktiveSpieltag(spielTagNr);
	}

	@Override
	public SpielerMeldungen getAktiveUndAusgesetztMeldungen() throws GenerateException {
		return meldeListeHelperGetMeldungen(getSpielTag(),
				Arrays.asList(SpielrundeGespielt.JA, SpielrundeGespielt.AUSGESETZT));
	}

	@Override
	public SpielerMeldungen getAktiveMeldungen() throws GenerateException {
		return meldeListeHelperGetMeldungen(getSpielTag(), Arrays.asList(SpielrundeGespielt.JA));
	}

	@Override
	public SpielerMeldungen getInAktiveMeldungen() throws GenerateException {
		return meldeListeHelperGetMeldungen(SpielTagNr.from(1), Arrays.asList(SpielrundeGespielt.NEIN));
	}

	@Override
	public SpielerMeldungen getAlleMeldungen() throws GenerateException {
		return meldeListeHelperGetMeldungen(getSpielTag(), null);
	}

	private SpielerMeldungen meldeListeHelperGetMeldungen(final SpielTagNr spieltag,
			final List<SpielrundeGespielt> spielrundeGespielt) throws GenerateException {

		boolean setzPositionenAktiv = getKonfigurationSheet().getSetzPositionenAktiv();
		return (SpielerMeldungen) meldeListeHelper.getMeldungen(spieltag, spielrundeGespielt,
				new SpielerMeldungen(setzPositionenAktiv));
	}

	public int getSpielerNameErsteSpalte() {
		return meldungenSpalte.getSpielerNameErsteSpalte();
	}

	/**
	 * @param SpielRundeNr
	 * @throws GenerateException
	 */
	public void setAktiveSpielRunde(SpielRundeNr spielRundeNr) throws GenerateException {
		getKonfigurationSheet().setAktiveSpielRunde(spielRundeNr);
	}

	/**
	 * @return the spielerSpalte
	 */
	@Override
	public final MeldungenSpalte<SpielerMeldungen, Spieler> getMeldungenSpalte() {
		return meldungenSpalte;
	}

	/**
	 * @param from
	 * @return
	 */
	public int spieltagSpalte(SpielTagNr spieltagNr) {
		return meldeListeHelper.spieltagSpalte(spieltagNr);
	}

	@Override
	public int letzteZeileMitSpielerName() throws GenerateException {
		return meldungenSpalte.letzteZeileMitSpielerName();
	}
}
