/**
 * Erstellung : 15.03.2026 / Michael Massee
 **/

package de.petanqueturniermanager.supermelee.meldeliste;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.sun.star.sheet.ConditionOperator;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.CellVertJustify2;

import de.petanqueturniermanager.addins.GlobalImpl;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.IMeldeliste;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeHelper;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeKonstanten;
import de.petanqueturniermanager.basesheet.meldeliste.MeldungenSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.SpielrundeGespielt;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.ColorHelper;
import de.petanqueturniermanager.helper.border.BorderFactory;
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
import de.petanqueturniermanager.model.Spieler;
import de.petanqueturniermanager.model.SpielerMeldungen;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeKonfigurationSheet;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleePropertiesSpalte;

class SupermeleeListeDelegate implements MeldeListeKonstanten {

	private static final String SPIELTAG_HEADER_STR = "Spieltag";

	static final String PTM_SPIELTAG = GlobalImpl
			.FORMAT_PTM_INT_PROPERTY(SuperMeleePropertiesSpalte.KONFIG_PROP_NAME_SPIELTAG);
	static final String PTM_SPIELRUNDE = GlobalImpl
			.FORMAT_PTM_INT_PROPERTY(SuperMeleePropertiesSpalte.KONFIG_PROP_NAME_SPIELRUNDE);

	static final String PTM_SM_TRIPL_ANZ_DOUBLETTE = GlobalImpl.PTM_SUPERMELEE_TRIPL_ANZ_DOUBLETTE;
	static final String PTM_SM_TRIPL_ANZ_TRIPLETTE  = GlobalImpl.PTM_SUPERMELEE_TRIPL_ANZ_TRIPLETTE;
	static final String PTM_SM_TRIPL_NUR_DOUBLETTE  = GlobalImpl.PTM_SUPERMELEE_TRIPL_NUR_DOUBLETTE;
	static final String PTM_SM_DOUBL_ANZ_DOUBLETTE  = GlobalImpl.PTM_SUPERMELEE_DOUBL_ANZ_DOUBLETTE;
	static final String PTM_SM_DOUBL_ANZ_TRIPLETTE  = GlobalImpl.PTM_SUPERMELEE_DOUBL_ANZ_TRIPLETTE;
	static final String PTM_SM_DOUBL_NUR_TRIPLETTE  = GlobalImpl.PTM_SUPERMELEE_DOUBL_NUR_TRIPLETTE;

	static final int MIN_ANZAHL_SPIELER_ZEILEN = 100;
	static final int SUMMEN_SPALTE_OFFSET = 2;
	static final int SUMMEN_ERSTE_ZEILE = ERSTE_DATEN_ZEILE + 5;
	static final int SUMMEN_AKTIVE_ZEILE = SUMMEN_ERSTE_ZEILE;
	static final int SUMMEN_INAKTIVE_ZEILE = SUMMEN_ERSTE_ZEILE + 1;
	static final int SUMMEN_AUSGESTIEGENE_ZEILE = SUMMEN_INAKTIVE_ZEILE + 1;
	static final int SUMMEN_ANZ_SPIELER = SUMMEN_AUSGESTIEGENE_ZEILE + 1;
	static final int SUMMEN_GESAMT_ANZ_SPIELER = SUMMEN_ANZ_SPIELER + 1;

	static final int TRIPL_MODE_HEADER = SUMMEN_GESAMT_ANZ_SPIELER + 2;
	static final int TRIPL_MODE_ANZ_DOUBLETTE = TRIPL_MODE_HEADER + 1;
	static final int TRIPL_MODE_ANZ_TRIPLETTE = TRIPL_MODE_ANZ_DOUBLETTE + 1;
	static final int TRIPL_MODE_SUMMEN_KANN_DOUBLETTE_ZEILE = TRIPL_MODE_ANZ_TRIPLETTE + 1;
	static final int TRIPL_MODE_SUMMEN_SPIELBAHNEN = TRIPL_MODE_SUMMEN_KANN_DOUBLETTE_ZEILE + 1;

	static final int DOUBL_MODE_HEADER = TRIPL_MODE_SUMMEN_SPIELBAHNEN + 2;
	static final int DOUBL_MODE_ANZ_DOUBLETTE = DOUBL_MODE_HEADER + 1;
	static final int DOUBL_MODE_ANZ_TRIPLETTE = DOUBL_MODE_ANZ_DOUBLETTE + 1;
	static final int DOUBL_MODE_SUMMEN_KANN_TRIPLETTE_ZEILE = DOUBL_MODE_ANZ_TRIPLETTE + 1;
	static final int DOUBL_MODE_SUMMEN_SPIELBAHNEN = DOUBL_MODE_SUMMEN_KANN_TRIPLETTE_ZEILE + 1;

	static final int ERSTE_ZEILE_INFO = 0;

	private final IMeldeliste<SpielerMeldungen, Spieler> sheet;
	private final SuperMeleeKonfigurationSheet konfigurationSheet;
	private final MeldungenSpalte<SpielerMeldungen, Spieler> meldungenSpalte;
	private final MeldeListeHelper<SpielerMeldungen, Spieler> meldeListeHelper;
	private SpielTagNr spielTag = null;

	SupermeleeListeDelegate(IMeldeliste<SpielerMeldungen, Spieler> sheet, WorkingSpreadsheet ws,
			SuperMeleeKonfigurationSheet konfigurationSheet, String metadatenSchluessel) {
		this.sheet = sheet;
		this.konfigurationSheet = konfigurationSheet;
		meldungenSpalte = MeldungenSpalte.builder().ersteDatenZiele(ERSTE_DATEN_ZEILE)
				.minAnzZeilen(MIN_ANZAHL_SPIELER_ZEILEN).spielerNrSpalte(SPIELER_NR_SPALTE).sheet(sheet)
				.formation(Formation.MELEE).build();
		meldeListeHelper = new MeldeListeHelper<>(sheet, metadatenSchluessel);
	}

	SuperMeleeKonfigurationSheet getKonfigurationSheet() {
		return konfigurationSheet;
	}

	MeldungenSpalte<SpielerMeldungen, Spieler> getMeldungenSpalte() {
		return meldungenSpalte;
	}

	/** Anzahl der Spieltage anhand der Header-Einträge zählen. */
	int countAnzSpieltageInMeldeliste() throws GenerateException {
		int anzSpieltage = 0;
		int ersteSpieltagspalteSpalte = meldeListeHelper.ersteSpieltagSpalte();
		Position posHeader = Position.from(ersteSpieltagspalteSpalte, ZWEITE_HEADER_ZEILE);

		for (int spielTagcntr = 1; spielTagcntr < 90; spielTagcntr++) {
			String header = sheet.getSheetHelper().getTextFromCell(sheet.getXSpreadSheet(), posHeader);
			if (StringUtils.isNotBlank(header) && header.contains(spielTagHeader(SpielTagNr.from(spielTagcntr)))) {
				anzSpieltage++;
			} else {
				break;
			}
			posHeader.spaltePlusEins();
		}
		return anzSpieltage;
	}

	void doSort(int spalteNr, boolean isAscending) throws GenerateException {
		meldeListeHelper.doSort(spalteNr, isAscending);
	}

	void upDateSheet() throws GenerateException {
		PageStyleHelper.from(sheet, PageStyle.PETTURNMNGR).initDefaultFooter().create().applytoSheet();
		sheet.processBoxinfo("processbox.supermelee.meldeliste.aktualisieren");

		meldeListeHelper.testDoppelteMeldungen();
		sheet.getTurnierSheet().setActiv();

		// ------
		// Header einfuegen
		// ------
		int headerBackColor = konfigurationSheet.getMeldeListeHeaderFarbe();
		meldungenSpalte.insertHeaderInSheet(headerBackColor);

		// ------
		// Setzposition
		var columnProp = ColumnProperties.from().setHoriJustify(CellHoriJustify.CENTER).setWidth(800);

		var bezCelVal = StringCellValue
				.from(sheet.getXSpreadSheet(), meldeListeHelper.setzPositionSpalte(), ZWEITE_HEADER_ZEILE, "SP")
				.setComment("1 = Setzposition, Diesen Spieler werden nicht zusammen im gleichen Team gelost.")
				.setCellBackColor(headerBackColor).setBorder(BorderFactory.from().allThin().boldLn().forTop().toBorder())
				.addColumnProperties(columnProp).setVertJustify(CellVertJustify2.CENTER);
		sheet.getSheetHelper().setStringValueInCell(bezCelVal);
		// ------

		formatSpielTagSpalte(getSpielTag());

		// eventuelle luecken in spiele namen nach unten sortieren
		meldeListeHelper.zeileOhneSpielerNamenEntfernen();
		meldeListeHelper.updateMeldungenNr();

		doSort(meldungenSpalte.getErsteMeldungNameSpalte(), true); // nach namen sortieren
		updateSpieltageSummenSpalten();
		insertInfoBlock();
		formatDaten();
		meldungenSpalte.formatSpielrNrUndNamenspalten();

		// TurnierSystem
		meldeListeHelper.insertTurnierSystemInHeader(TurnierSystem.SUPERMELEE);

		// headerlines
		SheetFreeze.from(sheet.getTurnierSheet()).anzZeilen(2).doFreeze();
	}

	/** Aktive Spielrunde und Spieltag in den Info-Block schreiben. */
	private void insertInfoBlock() throws GenerateException {
		sheet.processBoxinfo("processbox.supermelee.meldeliste.einfuegen");
		var xSheet = sheet.getXSpreadSheet();
		int headerBackColor = konfigurationSheet.getMeldeListeHeaderFarbe();
		var border = BorderFactory.from().allThin().toBorder();

		var posInfo = Position.from(ersteSummeSpalte(), ERSTE_ZEILE_INFO);

		var labelVal = StringCellValue.from(xSheet, posInfo, SPIELTAG_HEADER_STR)
				.setComment("Aktive Spieltag").setBorder(border).setCellBackColor(headerBackColor);
		sheet.getSheetHelper().setStringValueInCell(labelVal);
		labelVal.zeilePlus(1).setValue("Spielrunde").setComment("Aktive Spielrunde");
		sheet.getSheetHelper().setStringValueInCell(labelVal);
		// ---------------------------------------------------
		setProperties();
	}

	private void setProperties() throws GenerateException {
		var xSheet = sheet.getXSpreadSheet();
		var border = BorderFactory.from().allThin().toBorder();
		var posInfo = Position.from(ersteSummeSpalte(), ERSTE_ZEILE_INFO);
		var posSpieltagFormula = Position.from(posInfo).spaltePlus(1);
		var spielTagFormula = StringCellValue.from(xSheet, posSpieltagFormula, PTM_SPIELTAG).setBorder(border);
		sheet.getSheetHelper().setFormulaInCell(spielTagFormula);

		var posSpielrundeFormula = Position.from(posSpieltagFormula).zeilePlusEins();
		var spielRundeFormula = StringCellValue.from(xSheet, posSpielrundeFormula, PTM_SPIELRUNDE).setBorder(border);
		sheet.getSheetHelper().setFormulaInCell(spielRundeFormula);
	}

	void formatSpielTagSpalte(SpielTagNr spieltag) throws GenerateException {
		checkNotNull(spieltag);
		sheet.processBoxinfo("processbox.supermelee.meldeliste.sortieren");

		var xSheet = sheet.getXSpreadSheet();
		int hederBackColor = konfigurationSheet.getRanglisteHeaderFarbe();
		var columnProp = ColumnProperties.from().setHoriJustify(CellHoriJustify.CENTER)
				.setVertJustify(CellVertJustify2.CENTER).setWidth(2000).margin(CELL_MARGIN);

		var bezCelSpieltagVal = StringCellValue
				.from(xSheet, meldeListeHelper.spieltagSpalte(spieltag), ZWEITE_HEADER_ZEILE, spielTagHeader(spieltag))
				.setComment("1 = Aktiv, 2 = Ausgestiegen, leer = InAktiv").setCellBackColor(hederBackColor)
				.addColumnProperties(columnProp).setBorder(BorderFactory.from().allThin().boldLn().forTop().toBorder());

		bezCelSpieltagVal.setValue(spielTagHeader(spieltag));
		sheet.getSheetHelper().setStringValueInCell(bezCelSpieltagVal);

		String formulaStr = "IF(" + PTM_SPIELTAG + "=" + spieltag.getNr() + ";\"Aktiv\";\"\")";
		StringCellValue aktivFormula = StringCellValue
				.from(xSheet, meldeListeHelper.spieltagSpalte(spieltag), ERSTE_HEADER_ZEILE, formulaStr)
				.setCharColor(ColorHelper.CHAR_COLOR_GREEN);
		sheet.getSheetHelper().setFormulaInCell(aktivFormula);
	}

	void formatDaten() throws GenerateException {
		sheet.processBoxinfo("processbox.supermelee.meldeliste.formatieren");

		int letzteDatenZeile = meldungenSpalte.getLetzteDatenZeileUseMin();

		// Spieler NR bis letzte Spieltag
		var datenRange = RangePosition.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE, letzteSpielTagSpalte(),
				letzteDatenZeile);
		sheet.getSheetHelper().setPropertiesInRange(sheet.getXSpreadSheet(), datenRange,
				CellProperties.from().setVertJustify(CellVertJustify2.CENTER)
						.setBorder(BorderFactory.from().allThin().boldLn().forTop().forLeft().toBorder())
						.setCharColor(ColorHelper.CHAR_COLOR_BLACK).setCellBackColor(-1).setShrinkToFit(true));

		var meldungenHintergrundFarbeGeradeStyle = konfigurationSheet.getMeldeListeHintergrundFarbeGeradeStyle();
		var meldungenHintergrundFarbeUnGeradeStyle = konfigurationSheet.getMeldeListeHintergrundFarbeUnGeradeStyle();

		meldeListeHelper.insertFormulaFuerDoppelteSpielerNrGeradeUngradeFarbe(letzteDatenZeile, sheet,
				meldungenHintergrundFarbeGeradeStyle, meldungenHintergrundFarbeUnGeradeStyle);

		meldeListeHelper.insertFormulaFuerDoppelteNamenGeradeUngradeFarbe(meldungenSpalte.getErsteMeldungNameSpalte(),
				meldungenSpalte.getLetzteMeldungNameSpalte(), letzteDatenZeile, sheet,
				meldungenHintergrundFarbeGeradeStyle, meldungenHintergrundFarbeUnGeradeStyle);

		// setzposition spalte
		var setzpositionRangePos = RangePosition.from(meldeListeHelper.setzPositionSpalte(),
				ERSTE_DATEN_ZEILE, meldeListeHelper.setzPositionSpalte(), letzteDatenZeile);
		ConditionalFormatHelper.from(sheet, setzpositionRangePos).clear()
				.formula1("0").formula2("90").operator(ConditionOperator.NOT_BETWEEN).styleIsFehler().applyAndDoReset()
				.formulaIsText().styleIsFehler().applyAndDoReset()
				.formulaIsEvenRow().style(meldungenHintergrundFarbeGeradeStyle).applyAndDoReset()
				.formulaIsOddRow().style(meldungenHintergrundFarbeUnGeradeStyle).applyAndDoReset();

		// Spieltag spalten
		var spieltageRangePos = RangePosition.from(meldeListeHelper.ersteSpieltagSpalte(), ERSTE_DATEN_ZEILE,
				letzteSpielTagSpalte(), letzteDatenZeile);
		ConditionalFormatHelper.from(sheet, spieltageRangePos).clear()
				.formula1("0").formula2("2").operator(ConditionOperator.NOT_BETWEEN).styleIsFehler().applyAndDoReset()
				.formulaIsText().styleIsFehler().applyAndDoReset()
				.formulaIsEvenRow().style(meldungenHintergrundFarbeGeradeStyle).applyAndDoReset()
				.formulaIsOddRow().style(meldungenHintergrundFarbeUnGeradeStyle).applyAndDoReset();
	}

	/** Liefert den Header-Text für den gegebenen Spieltag, z.B. "Spieltag 1". */
	String spielTagHeader(SpielTagNr spieltag) {
		return SPIELTAG_HEADER_STR + " " + spieltag.getNr();
	}

	int letzteSpielTagSpalte() throws GenerateException {
		int anzSpieltage = countAnzSpieltageInMeldeliste();
		return meldeListeHelper.ersteSpieltagSpalte() + (anzSpieltage - 1);
	}

	/** Liefert die Spalte des aktuellen Spieltags. */
	int aktuelleSpieltagSpalte() {
		return meldeListeHelper.spieltagSpalte(getSpielTag());
	}

	int ersteSummeSpalte() throws GenerateException {
		return letzteSpielTagSpalte() + SUMMEN_SPALTE_OFFSET;
	}

	String formulaSverweisSpielernamen(String spielrNrAdresse) {
		return meldeListeHelper.formulaSverweisSpielernamen(spielrNrAdresse);
	}

	int getSpielerNameErsteSpalte() {
		return meldungenSpalte.getErsteMeldungNameSpalte();
	}

	private String formulaPtmSupermelee(String formelName, String anzSpielerAddr) {
		return "=" + formelName + "(" + anzSpielerAddr + ")";
	}

	private void updateSpieltageSummenSpalten() throws GenerateException {
		sheet.processBoxinfo("processbox.summenspalten.aktualisieren");

		int headerBackColor = konfigurationSheet.getMeldeListeHeaderFarbe();

		int letzteDatenZeile = meldungenSpalte.getLetzteMitDatenZeileInSpielerNrSpalte();

		if (letzteDatenZeile < MIN_ANZAHL_SPIELER_ZEILEN) {
			letzteDatenZeile = MIN_ANZAHL_SPIELER_ZEILEN;
		}

		var xSheet = sheet.getXSpreadSheet();

		int anzSpieltage = countAnzSpieltageInMeldeliste();

		var cleanUpRange = RangePosition.from(ersteSummeSpalte() - 1, 0,
				ersteSummeSpalte() + anzSpieltage + 10, MeldungenSpalte.MAX_ANZ_MELDUNGEN);
		RangeHelper.from(sheet, cleanUpRange).clearRange();

		var posBezeichnug = Position.from(ersteSummeSpalte(), SUMMEN_ERSTE_ZEILE - 1);

		var border = BorderFactory.from().allThin().toBorder();

		var columnProp = ColumnProperties.from().setHoriJustify(CellHoriJustify.RIGHT).setWidth(3000)
				.margin(CELL_MARGIN);
		sheet.getSheetHelper().setColumnProperties(xSheet, posBezeichnug.getSpalte(), columnProp);

		var bezCelVal = StringCellValue.from(xSheet, posBezeichnug, "").setComment(null).removeCellBackColor();
		sheet.getSheetHelper().setStringValueInCell(bezCelVal);

		bezCelVal.setBorder(border).setCellBackColor(headerBackColor);

		bezCelVal.setComment("Anzahl Spieler mit \"1\" im Spieltag").setValue("Aktiv").zeile(SUMMEN_AKTIVE_ZEILE);
		sheet.getSheetHelper().setStringValueInCell(bezCelVal);

		bezCelVal.setComment("Anzahl Spieler mit \"\" im Spieltag").setValue("InAktiv").zeile(SUMMEN_INAKTIVE_ZEILE);
		sheet.getSheetHelper().setStringValueInCell(bezCelVal);

		bezCelVal.setComment("Anzahl Spieler mit \"2\" im Spieltag").setValue("Ausgestiegen")
				.zeile(SUMMEN_AUSGESTIEGENE_ZEILE);
		sheet.getSheetHelper().setStringValueInCell(bezCelVal);

		bezCelVal.setComment("Anzahl Aktive + Ausgestiegen").setValue("Akt + Ausg").zeile(SUMMEN_ANZ_SPIELER);
		sheet.getSheetHelper().setStringValueInCell(bezCelVal);

		bezCelVal.setComment("Anzahl Aktive + Inaktiv + Ausgestiegen").setValue("Summe")
				.zeile(SUMMEN_GESAMT_ANZ_SPIELER);
		sheet.getSheetHelper().setStringValueInCell(bezCelVal);

		var modusHeaderVal = StringCellValue.from(xSheet).spalte(ersteSummeSpalte())
				.zeile(TRIPL_MODE_HEADER).setHoriJustify(CellHoriJustify.CENTER).setValue("Supermêlée Triplette.")
				.setComment("Triplette Teams, aufüllen mit Doublette.  Siehe Konfiguration, 'Supermêlée Modus'")
				.setEndPosMergeSpaltePlus(getSpielTag().getNr());
		sheet.getSheetHelper().setStringValueInCell(modusHeaderVal);

		bezCelVal.setComment("Modus Triplette, Anzahl Doublette Teams").setValue("∑x2").zeile(TRIPL_MODE_ANZ_DOUBLETTE);
		sheet.getSheetHelper().setStringValueInCell(bezCelVal);

		bezCelVal.setComment("Modus Triplette, Anzahl Triplette Teams").setValue("∑x3").zeile(TRIPL_MODE_ANZ_TRIPLETTE);
		sheet.getSheetHelper().setStringValueInCell(bezCelVal);

		bezCelVal.setComment("Modus Triplette, Kann Doublette gespielt werden").setValue("Doublette");
		sheet.getSheetHelper().setStringValueInCell(bezCelVal.zeile(TRIPL_MODE_SUMMEN_KANN_DOUBLETTE_ZEILE));

		bezCelVal.setComment("Modus Triplette, Anzahl Spielbahnen").setValue("Bahnen");
		sheet.getSheetHelper().setStringValueInCell(bezCelVal.zeile(TRIPL_MODE_SUMMEN_SPIELBAHNEN));

		modusHeaderVal.spalte(ersteSummeSpalte()).zeile(DOUBL_MODE_HEADER)
				.setEndPosMergeSpaltePlus(getSpielTag().getNr()).setValue("Supermêlée Doublette.")
				.setComment("Doublette Teams, aufüllen mit Triplette. Siehe Konfiguration, 'Supermêlée Modus'");
		sheet.getSheetHelper().setStringValueInCell(modusHeaderVal);

		bezCelVal.setComment("Modus Doublette, Anzahl Doublette").setValue("∑x2");
		sheet.getSheetHelper().setStringValueInCell(bezCelVal.zeile(DOUBL_MODE_ANZ_DOUBLETTE));

		bezCelVal.setComment("Modus Doublette, Anzahl Triplette").setValue("∑x3");
		sheet.getSheetHelper().setStringValueInCell(bezCelVal.zeile(DOUBL_MODE_ANZ_TRIPLETTE));

		bezCelVal.setComment("Modus Doublette, Kann Triplette gespielt werden").setValue("Triplette");
		sheet.getSheetHelper().setStringValueInCell(bezCelVal.zeile(DOUBL_MODE_SUMMEN_KANN_TRIPLETTE_ZEILE));

		bezCelVal.setComment("Modus Doublette,Anzahl Spielbahnen").setValue("Bahnen");
		sheet.getSheetHelper().setStringValueInCell(bezCelVal.zeile(DOUBL_MODE_SUMMEN_SPIELBAHNEN));

		var formula = StringCellValue.from(xSheet).setBorder(border);
		var spalteWertProp = ColumnProperties.from().setWidth(1200).centerJustify().margin(CELL_MARGIN);

		for (int spieltagCntr = 1; spieltagCntr <= anzSpieltage; spieltagCntr++) {

			var spielTagNr = SpielTagNr.from(spieltagCntr);
			var posSpieltagWerte = Position.from(ersteSummeSpalte() + spieltagCntr, SUMMEN_ERSTE_ZEILE - 1);

			var tagHeader = StringCellValue.from(xSheet).setPos(posSpieltagWerte)
					.setBorder(border).setValue("Tag " + spieltagCntr).setColumnProperties(spalteWertProp)
					.setCellBackColor(headerBackColor);
			sheet.getSheetHelper().setStringValueInCell(tagHeader);

			String formulaStr = formulaCountSpieler(spielTagNr, "1", letzteDatenZeile);
			sheet.getSheetHelper()
					.setFormulaInCell(formula.setPos(posSpieltagWerte.zeile(SUMMEN_AKTIVE_ZEILE)).setValue(formulaStr));
			String aktivZelle = posSpieltagWerte.getAddress();

			formulaStr = formulaCountSpieler(spielTagNr, "0", letzteDatenZeile) + " + "
					+ formulaCountSpieler(spielTagNr, "\"\"", letzteDatenZeile);
			sheet.getSheetHelper().setFormulaInCell(
					formula.setPos(posSpieltagWerte.zeile(SUMMEN_INAKTIVE_ZEILE)).setValue(formulaStr));
			String inAktivZelle = posSpieltagWerte.getAddress();

			formulaStr = formulaCountSpieler(spielTagNr, "2", letzteDatenZeile);
			sheet.getSheetHelper().setFormulaInCell(
					formula.setPos(posSpieltagWerte.zeile(SUMMEN_AUSGESTIEGENE_ZEILE)).setValue(formulaStr));
			String ausgestiegenZelle = posSpieltagWerte.getAddress();

			formulaStr = aktivZelle + "+" + ausgestiegenZelle;
			sheet.getSheetHelper()
					.setFormulaInCell(formula.setPos(posSpieltagWerte.zeile(SUMMEN_ANZ_SPIELER)).setValue(formulaStr));

			formulaStr = aktivZelle + "+" + inAktivZelle + "+" + ausgestiegenZelle;
			sheet.getSheetHelper().setFormulaInCell(
					formula.setPos(posSpieltagWerte.zeile(SUMMEN_GESAMT_ANZ_SPIELER)).setValue(formulaStr));

			// Triplette mode
			String anzSpielerAddr = sheet.getSheetHelper()
					.getAddressFromColumnRow(getAnzahlAktiveSpielerPosition(spielTagNr));

			sheet.getSheetHelper().setFormulaInCell(formula.setPos(posSpieltagWerte.zeile(TRIPL_MODE_ANZ_DOUBLETTE))
					.setValue(formulaPtmSupermelee(PTM_SM_TRIPL_ANZ_DOUBLETTE, anzSpielerAddr)));
			String anzDoublZelle = posSpieltagWerte.getAddress();

			sheet.getSheetHelper().setFormulaInCell(formula.setPos(posSpieltagWerte.zeile(TRIPL_MODE_ANZ_TRIPLETTE))
					.setValue(formulaPtmSupermelee(PTM_SM_TRIPL_ANZ_TRIPLETTE, anzSpielerAddr)));
			String anzTriplZelle = posSpieltagWerte.getAddress();

			sheet.getSheetHelper()
					.setFormulaInCell(formula.setPos(posSpieltagWerte.zeile(TRIPL_MODE_SUMMEN_KANN_DOUBLETTE_ZEILE))
							.setValue(formulaPtmSupermelee(PTM_SM_TRIPL_NUR_DOUBLETTE, anzSpielerAddr)));

			String formulaAnzSpielbahnen = "=(" + anzDoublZelle + " + " + anzTriplZelle + ")/2";
			sheet.getSheetHelper().setFormulaInCell(formula.setPos(posSpieltagWerte.zeile(TRIPL_MODE_SUMMEN_SPIELBAHNEN))
					.setValue(formulaAnzSpielbahnen));

			// Doublette mode
			sheet.getSheetHelper().setFormulaInCell(formula.setPos(posSpieltagWerte.zeile(DOUBL_MODE_ANZ_DOUBLETTE))
					.setValue(formulaPtmSupermelee(PTM_SM_DOUBL_ANZ_DOUBLETTE, anzSpielerAddr)));
			String doublettteModeAnzDoublZelle = sheet.getSheetHelper()
					.getAddressFromColumnRow(posSpieltagWerte);

			sheet.getSheetHelper().setFormulaInCell(formula.setPos(posSpieltagWerte.zeile(DOUBL_MODE_ANZ_TRIPLETTE))
					.setValue(formulaPtmSupermelee(PTM_SM_DOUBL_ANZ_TRIPLETTE, anzSpielerAddr)));
			String doublettteModeAnzTriplZelle = sheet.getSheetHelper()
					.getAddressFromColumnRow(posSpieltagWerte);

			sheet.getSheetHelper()
					.setFormulaInCell(formula.setPos(posSpieltagWerte.zeile(DOUBL_MODE_SUMMEN_KANN_TRIPLETTE_ZEILE))
							.setValue(formulaPtmSupermelee(PTM_SM_DOUBL_NUR_TRIPLETTE, anzSpielerAddr)));

			String doublettModeFormulaAnzSpielbahnen = "=(" + doublettteModeAnzDoublZelle + " + "
					+ doublettteModeAnzTriplZelle + ")/2";
			sheet.getSheetHelper().setFormulaInCell(formula.setPos(posSpieltagWerte.zeile(DOUBL_MODE_SUMMEN_SPIELBAHNEN))
					.setValue(doublettModeFormulaAnzSpielbahnen));
		}
	}

	int getAnzahlAktiveSpieler(SpielTagNr spieltag) throws GenerateException {
		return sheet.getSheetHelper().getIntFromCell(sheet.getXSpreadSheet(),
				getAnzahlAktiveSpielerPosition(spieltag));
	}

	Position getAnzahlAktiveSpielerPosition(SpielTagNr spieltag) throws GenerateException {
		return Position.from(ersteSummeSpalte() + spieltag.getNr(), SUMMEN_AKTIVE_ZEILE);
	}

	int getAusgestiegenSpieler(SpielTagNr spieltag) throws GenerateException {
		return sheet.getSheetHelper().getIntFromCell(sheet.getXSpreadSheet(),
				getAusgestiegenSpielerPosition(spieltag));
	}

	Position getAusgestiegenSpielerPosition(SpielTagNr spieltag) throws GenerateException {
		return Position.from(ersteSummeSpalte() + spieltag.getNr(), SUMMEN_AUSGESTIEGENE_ZEILE);
	}

	private String formulaCountSpieler(SpielTagNr spieltag, String status, int letzteZeile) {
		if (letzteZeile < ERSTE_DATEN_ZEILE) {
			return "";
		}
		String ersteZelleName = Position.from(getSpielerNameErsteSpalte(), ERSTE_DATEN_ZEILE).getAddress();
		String letzteZelleName = Position.from(getSpielerNameErsteSpalte(), letzteZeile).getAddress();
		int spieltagSpalte = meldeListeHelper.spieltagSpalte(spieltag);
		String ersteZelleSpielTag = Position.from(spieltagSpalte, ERSTE_DATEN_ZEILE).getAddress();
		String letzteZelleSpielTag = Position.from(spieltagSpalte, letzteZeile).getAddress();
		return "COUNTIFS(" + ersteZelleName + ":" + letzteZelleName + ";\"<>\";" + ersteZelleSpielTag + ":"
				+ letzteZelleSpielTag + ";" + status + ")";
	}

	int getSpielerZeileNr(int spielerNr) throws GenerateException {
		return meldungenSpalte.getSpielerZeileNr(spielerNr);
	}

	List<String> getSpielerNamenList() throws GenerateException {
		return meldungenSpalte.getSpielerNamenList();
	}

	List<Integer> getSpielerNrList() throws GenerateException {
		return meldungenSpalte.getSpielerNrList();
	}

	int naechsteFreieDatenZeileInSpielerNrSpalte() throws GenerateException {
		return meldungenSpalte.naechsteFreieDatenZeileInSpielerNrSpalte();
	}

	int getLetzteMitDatenZeileInSpielerNrSpalte() throws GenerateException {
		return meldungenSpalte.getLetzteMitDatenZeileInSpielerNrSpalte();
	}

	int getErsteDatenZiele() {
		return meldungenSpalte.getErsteDatenZiele();
	}

	SpielTagNr getSpielTag() {
		checkNotNull(spielTag, "spielTag == null");
		return spielTag;
	}

	void setSpielTag(SpielTagNr spielTag) {
		checkNotNull(spielTag, "spielTag == null");
		this.spielTag = spielTag;
	}

	void setAktiveSpieltag(SpielTagNr spielTagNr) throws GenerateException {
		konfigurationSheet.setAktiveSpieltag(spielTagNr);
	}

	SpielerMeldungen getAktiveUndAusgesetztMeldungen() throws GenerateException {
		return meldeListeHelperGetMeldungen(getSpielTag(),
				List.of(SpielrundeGespielt.JA, SpielrundeGespielt.AUSGESETZT));
	}

	SpielerMeldungen getAktiveMeldungen() throws GenerateException {
		return meldeListeHelperGetMeldungen(getSpielTag(), List.of(SpielrundeGespielt.JA));
	}

	SpielerMeldungen getInAktiveMeldungen() throws GenerateException {
		return meldeListeHelperGetMeldungen(SpielTagNr.from(1), List.of(SpielrundeGespielt.NEIN));
	}

	SpielerMeldungen getAlleMeldungen() throws GenerateException {
		return meldeListeHelperGetMeldungen(getSpielTag(), null);
	}

	private SpielerMeldungen meldeListeHelperGetMeldungen(SpielTagNr spieltag,
			List<SpielrundeGespielt> spielrundeGespielt) throws GenerateException {
		boolean setzPositionenAktiv = konfigurationSheet.getSetzPositionenAktiv();
		return (SpielerMeldungen) meldeListeHelper.getMeldungen(spieltag, spielrundeGespielt,
				new SpielerMeldungen(setzPositionenAktiv));
	}

	void setAktiveSpielRunde(SpielRundeNr spielRundeNr) throws GenerateException {
		konfigurationSheet.setAktiveSpielRunde(spielRundeNr);
	}

	int getLetzteDatenZeileUseMin() throws GenerateException {
		return meldungenSpalte.getLetzteDatenZeileUseMin();
	}

	int spieltagSpalte(SpielTagNr spieltagNr) {
		return meldeListeHelper.spieltagSpalte(spieltagNr);
	}

	int letzteZeileMitSpielerName() throws GenerateException {
		return meldungenSpalte.letzteZeileMitSpielerName();
	}

	int sucheLetzteZeileMitSpielerNummer() throws GenerateException {
		return meldungenSpalte.sucheLetzteZeileMitSpielerNummer();
	}
}