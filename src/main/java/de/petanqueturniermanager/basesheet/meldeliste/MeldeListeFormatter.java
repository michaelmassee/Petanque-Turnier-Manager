package de.petanqueturniermanager.basesheet.meldeliste;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.sun.star.sheet.ConditionOperator;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.CellVertJustify2;

import de.petanqueturniermanager.basesheet.konfiguration.IPropertiesSpalte;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ColorHelper;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellstyle.MeldungenHintergrundFarbeGeradeStyle;
import de.petanqueturniermanager.helper.cellstyle.MeldungenHintergrundFarbeUnGeradeStyle;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.CellProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.ConditionalFormatHelper;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.WeakRefHelper;

/**
 * Erstellung 24.08.2022 / Michael Massee
 */

public class MeldeListeFormatter {

	private static final String HEADER_SETZPOS = "SP";
	private static final int SETZPOS_SPALTE_OFFS = 1; // offset zu letzte name spalte 

	private final Formation formation;
	private final int ersteDatenZiele; // Zeile 1 = 0
	private final int meldungNrSpalte; // Spalte A=0, B=1
	private final int meldung1NameSpalte;
	private final int anzZeilenInHeader; // weiviele Zeilen sollen in header verwendet werden
	private final int spalteMeldungNameWidth;
	private final int minAnzahlAnzeilen; // 
	private final IPropertiesSpalte propertiesSpalte;
	private final WeakRefHelper<ISheet> sheet;

	public MeldeListeFormatter(int ersteDatenZiele, int spielerNrSpalte, ISheet iSheet, Formation formation,
			int anzZeilenInHeader, int spalteMeldungNameWidth, int minAnzahlAnzeilen,
			IPropertiesSpalte propertiesSpalte) {
		checkArgument(ersteDatenZiele > -1);
		checkArgument(spielerNrSpalte > -1);
		checkArgument(anzZeilenInHeader > 0);
		checkArgument(spalteMeldungNameWidth > 1);
		checkArgument(minAnzahlAnzeilen > 1);

		this.ersteDatenZiele = ersteDatenZiele;
		this.anzZeilenInHeader = anzZeilenInHeader;
		this.spalteMeldungNameWidth = spalteMeldungNameWidth;
		meldungNrSpalte = spielerNrSpalte;
		meldung1NameSpalte = spielerNrSpalte + 1;
		sheet = new WeakRefHelper<>(checkNotNull(iSheet));
		this.formation = checkNotNull(formation);
		this.minAnzahlAnzeilen = minAnzahlAnzeilen;
		this.propertiesSpalte = checkNotNull(propertiesSpalte);
	}

	private final ISheet getISheet() {
		return sheet.get();
	}

	private final XSpreadsheet getXSpreadsheet() throws GenerateException {
		return getISheet().getXSpreadSheet();
	}

	private SheetHelper getSheetHelper() throws GenerateException {
		return getISheet().getSheetHelper();
	}

	public int getAnzahlSpielerNamenSpalten() {
		return formation.getAnzSpielerImTeam();
	}

	public void formatDaten() throws GenerateException {

		getISheet().processBoxinfo("Formatiere Meldungen Spalten");

		int letzteDatenZeile = getLetzteMitDatenZeileInSpielerNrSpalte();

		if (letzteDatenZeile < minAnzahlAnzeilen) {
			letzteDatenZeile = minAnzahlAnzeilen;
		}

		if (letzteDatenZeile < ersteDatenZiele) {
			// keine Daten
			return;
		}

		// gerade / ungrade hintergrund farbe
		Integer geradeColor = propertiesSpalte.getMeldeListeHintergrundFarbeGerade();
		Integer unGeradeColor = propertiesSpalte.getMeldeListeHintergrundFarbeUnGerade();

		MeldungenHintergrundFarbeGeradeStyle meldungenHintergrundFarbeGeradeStyle = new MeldungenHintergrundFarbeGeradeStyle(
				geradeColor);
		MeldungenHintergrundFarbeUnGeradeStyle meldungenHintergrundFarbeUnGeradeStyle = new MeldungenHintergrundFarbeUnGeradeStyle(
				unGeradeColor);

		formatSpielrnSpalte(letzteDatenZeile, meldungenHintergrundFarbeGeradeStyle,
				meldungenHintergrundFarbeUnGeradeStyle);
		formatSpielerNameSpalte(letzteDatenZeile, meldungenHintergrundFarbeGeradeStyle,
				meldungenHintergrundFarbeUnGeradeStyle);
		formatSetzPosSpalte(letzteDatenZeile, meldungenHintergrundFarbeGeradeStyle,
				meldungenHintergrundFarbeUnGeradeStyle);
		// Alias
		// Notitz
		// Akt mehrere Spieltage

	}

	private void formatSpielrnSpalte(int letzteDatenZeile,
			MeldungenHintergrundFarbeGeradeStyle meldungenHintergrundFarbeGeradeStyle,
			MeldungenHintergrundFarbeUnGeradeStyle meldungenHintergrundFarbeUnGeradeStyle) throws GenerateException {

		RangePosition spielrNrdatenRange = RangePosition.from(meldungNrSpalte, ersteDatenZiele, meldungNrSpalte,
				letzteDatenZeile);

		getSheetHelper().setPropertiesInRange(getXSpreadsheet(), spielrNrdatenRange,
				CellProperties.from().centerJustify().setCharColor(ColorHelper.CHAR_COLOR_BLACK)
						.setBorder(BorderFactory.from().allThin().boldLn().forTop().forLeft().toBorder())
						.setShrinkToFit());

		// -----------------------------------------------
		ConditionalFormatHelper.from(sheet.get(), spielrNrdatenRange).clear().
		// ------------------------------
				formulaIsText().styleIsFehler().applyAndDoReset().
				// ------------------------------
				zellwertIstMehrmalsVorhanden().styleIsFehler().applyAndDoReset().
				// ------------------------------
				// eigentlich musste 0 = Fehler sein wird es aber nicht ?
				formula1("0").formula2("" + MeldungenSpalte.MAX_ANZ_MELDUNGEN).operator(ConditionOperator.NOT_BETWEEN)
				.styleIsFehler().applyAndDoReset(). // nr muss >0 und <999 sein
				formulaIsEvenRow().style(meldungenHintergrundFarbeGeradeStyle).applyAndDoReset().
				// ------------------------------
				formulaIsOddRow().style(meldungenHintergrundFarbeUnGeradeStyle).applyAndDoReset();
		// -----------------------------------------------		

	}

	private void formatSpielerNameSpalte(int letzteDatenZeile,
			MeldungenHintergrundFarbeGeradeStyle meldungenHintergrundFarbeGeradeStyle,
			MeldungenHintergrundFarbeUnGeradeStyle meldungenHintergrundFarbeUnGeradeStyle) throws GenerateException {

		RangePosition datenRange = RangePosition.from(meldung1NameSpalte, ersteDatenZiele,
				meldung1NameSpalte + (getAnzahlSpielerNamenSpalten() - 1), letzteDatenZeile);

		getSheetHelper().setPropertiesInRange(getXSpreadsheet(), datenRange, CellProperties.from().centerJustify()
				.setBorder(BorderFactory.from().allThin().boldLn().forTop().toBorder()).setShrinkToFit());

		// -----------------------------------------------
		ConditionalFormatHelper.from(sheet.get(), datenRange).clear().
		// ------------------------------
				zellwertIstMehrmalsVorhanden().styleIsFehler().applyAndDoReset().
				// ------------------------------
				formulaIsEvenRow().style(meldungenHintergrundFarbeGeradeStyle).applyAndDoReset().
				// ------------------------------
				formulaIsOddRow().style(meldungenHintergrundFarbeUnGeradeStyle).applyAndDoReset();
		// -----------------------------------------------				
	}

	public int getSetzPosSpalte() {
		return meldung1NameSpalte + (getAnzahlSpielerNamenSpalten() - 1) + SETZPOS_SPALTE_OFFS;
	}

	private void formatSetzPosSpalte(int letzteDatenZeile,
			MeldungenHintergrundFarbeGeradeStyle meldungenHintergrundFarbeGeradeStyle,
			MeldungenHintergrundFarbeUnGeradeStyle meldungenHintergrundFarbeUnGeradeStyle) throws GenerateException {

		RangePosition datenRange = RangePosition.from(getSetzPosSpalte(), ersteDatenZiele, getSetzPosSpalte(),
				letzteDatenZeile);

		getSheetHelper().setPropertiesInRange(getXSpreadsheet(), datenRange, CellProperties.from().centerJustify()
				.setBorder(BorderFactory.from().allThin().boldLn().forTop().toBorder()).setShrinkToFit());

		// -----------------------------------------------
		ConditionalFormatHelper.from(sheet.get(), datenRange).clear().
		// ------------------------------
				formulaIsText().styleIsFehler().applyAndDoReset().
				// ------------------------------
				//formula1("2").operator(ConditionOperator.GREATER).styleIsFehler().applyAndDoReset().
				// ------------------------------
				formulaIsEvenRow().style(meldungenHintergrundFarbeGeradeStyle).applyAndDoReset().
				// ------------------------------
				formulaIsOddRow().style(meldungenHintergrundFarbeUnGeradeStyle).applyAndDoReset();
		// -----------------------------------------------				
	}

	public void insertHeaderInSheetAndFormatColumn(int headerBackColor) throws GenerateException {

		getISheet().processBoxinfo("Meldungen Spalten Header");

		ColumnProperties columnProperties = ColumnProperties.from().setWidth(DEFAULT_SPALTE_NUMBER_WIDTH)
				.setHoriJustify(CellHoriJustify.CENTER).setVertJustify(CellVertJustify2.CENTER)
				.margin(MeldeListeKonstanten.CELL_MARGIN);

		// --------------------------------------------------------------------------------------------
		// Spieler NR
		StringCellValue celVal = StringCellValue
				.from(getXSpreadsheet(), Position.from(meldungNrSpalte, getErsteDatenZiele() - anzZeilenInHeader),
						HEADER_SPIELER_NR)
				.setComment("Meldenummer (manuell nicht ändern)").addColumnProperties(columnProperties)
				.setBorder(BorderFactory.from().allThin().toBorder()).setCellBackColor(headerBackColor)
				.setVertJustify(CellVertJustify2.CENTER);

		if (anzZeilenInHeader > 1) {
			celVal.setEndPosMergeZeilePlus(1);
		}
		getSheetHelper().setStringValueInCell(celVal); // spieler nr
		// --------------------------------------------------------------------------------------------
		// Spieler Name
		celVal.addColumnProperties(columnProperties.setWidth(spalteMeldungNameWidth)).setComment(null)
				.spalte(meldung1NameSpalte).setValue(HEADER_SPIELER_NAME);

		int anzahlSpielerNamenSpalten = getAnzahlSpielerNamenSpalten();

		for (int anzSpieler = 0; anzSpieler < anzahlSpielerNamenSpalten; anzSpieler++) {

			if (anzahlSpielerNamenSpalten > 1) {
				celVal.setValue(HEADER_SPIELER_NAME + " " + (anzSpieler + 1));
			}

			getSheetHelper().setStringValueInCell(celVal);
			celVal.spaltePlusEins();
		}

		// --------------------------------------------------------------------------------------------
		// setzpos
		celVal.addColumnProperties(columnProperties.setWidth(DEFAULT_SPALTE_NUMBER_WIDTH)).setComment("Setzposition.")
				.spalte(getSetzPosSpalte()).setValue(HEADER_SETZPOS);
		getSheetHelper().setStringValueInCell(celVal);
		// --------------------------------------------------------------------------------------------

	}
}
