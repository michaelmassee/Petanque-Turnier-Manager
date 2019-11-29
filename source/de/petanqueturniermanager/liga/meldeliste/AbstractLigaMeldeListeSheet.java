/**
* Erstellung : 22.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.liga.meldeliste;

import java.util.Arrays;
import java.util.List;

import com.sun.star.sheet.ConditionOperator;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellVertJustify2;

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
import de.petanqueturniermanager.helper.cellvalue.properties.CellProperties;
import de.petanqueturniermanager.helper.pagestyle.PageStyle;
import de.petanqueturniermanager.helper.pagestyle.PageStyleHelper;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.ConditionalFormatHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.liga.konfiguration.LigaSheet;
import de.petanqueturniermanager.model.Meldungen;
import de.petanqueturniermanager.supermelee.SpielTagNr;

abstract public class AbstractLigaMeldeListeSheet extends LigaSheet implements IMeldeliste {

	private static final int MIN_ANZAHL_MELDUNGEN_ZEILEN = 16; // Tablle immer mit min anzahl von zeilen formatieren

	private final MeldungenSpalte meldungenSpalte;
	private final MeldeListeHelper meldeListeHelper;

	/**
	 * @param workingSpreadsheet
	 */
	public AbstractLigaMeldeListeSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, "Meldeliste");
		// new MeldungenSpalte(ERSTE_DATEN_ZEILE, SPIELER_NR_SPALTE, this, this, Formation.TETE);
		meldungenSpalte = MeldungenSpalte.Builder().ersteDatenZiele(ERSTE_DATEN_ZEILE).spielerNrSpalte(SPIELER_NR_SPALTE).sheet(this).formation(Formation.TETE).build();
		meldeListeHelper = new MeldeListeHelper(this);
	}

	public void upDateSheet() throws GenerateException {
		PageStyleHelper.from(this, PageStyle.PETTURNMNGR).initDefaultFooter().create().applytoSheet();
		processBoxinfo("Aktualisiere Meldungen");
		meldeListeHelper.testDoppelteMeldungen();

		XSpreadsheet sheet = getXSpreadSheet();
		getSheetHelper().setActiveSheet(sheet);

		// ------
		// Header einfuegen
		// ------
		int headerBackColor = getKonfigurationSheet().getMeldeListeHeaderFarbe();
		getMeldungenSpalte().insertHeaderInSheet(headerBackColor);
		// --------------------- TODO doppelt code entfernen

		// eventuelle luecken in spiele namen nach unten sortieren
		meldeListeHelper.zeileOhneSpielerNamenEntfernen();
		meldeListeHelper.updateMeldungenNr();
		meldeListeHelper.doSort(meldungenSpalte.getSpielerNameErsteSpalte(), true); // nach namen sortieren
		meldungenSpalte.formatDaten();
		formatDaten();
	}

	void formatDaten() throws GenerateException {

		processBoxinfo("Formatiere Daten Spalten");

		int letzteDatenZeile = meldungenSpalte.getLetzteDatenZeile();

		if (letzteDatenZeile < MIN_ANZAHL_MELDUNGEN_ZEILEN) {
			letzteDatenZeile = MIN_ANZAHL_MELDUNGEN_ZEILEN;
		}

		if (letzteDatenZeile < ERSTE_DATEN_ZEILE) {
			// keine Daten
			return;
		}

		RangePosition datenRange = RangePosition.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE, getSpielerNameErsteSpalte(), letzteDatenZeile);

		getSheetHelper().setPropertiesInRange(getXSpreadSheet(), datenRange,
				CellProperties.from().setVertJustify(CellVertJustify2.CENTER).setBorder(BorderFactory.from().allThin().boldLn().forTop().forLeft().toBorder())
						.setCharColor(ColorHelper.CHAR_COLOR_BLACK).setCellBackColor(-1).setShrinkToFit(true));

		// gerade / ungrade hintergrund farbe
		// CellBackColor
		Integer geradeColor = getKonfigurationSheet().getMeldeListeHintergrundFarbeGerade();
		Integer unGeradeColor = getKonfigurationSheet().getMeldeListeHintergrundFarbeUnGerade();
		MeldungenHintergrundFarbeGeradeStyle meldungenHintergrundFarbeGeradeStyle = new MeldungenHintergrundFarbeGeradeStyle(geradeColor);
		MeldungenHintergrundFarbeUnGeradeStyle meldungenHintergrundFarbeUnGeradeStyle = new MeldungenHintergrundFarbeUnGeradeStyle(unGeradeColor);

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
				formulaIsEvenRow().style(meldungenHintergrundFarbeGeradeStyle).applyNew().
				// ------------------------------
				formulaIsOddRow().style(meldungenHintergrundFarbeUnGeradeStyle).applyNew();
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
				formulaIsEvenRow().operator(ConditionOperator.FORMULA).style(meldungenHintergrundFarbeGeradeStyle).applyNew().
				// ------------------------------
				formulaIsEvenRow().style(meldungenHintergrundFarbeGeradeStyle).applyNew().formulaIsOddRow().style(meldungenHintergrundFarbeUnGeradeStyle).applyNew();
		// -----------------------------------------------
	}

	public int getSpielerNameErsteSpalte() {
		return meldungenSpalte.getSpielerNameErsteSpalte();
	}

	@Override
	public String formulaSverweisSpielernamen(String spielrNrAdresse) {
		return meldeListeHelper.formulaSverweisSpielernamen(spielrNrAdresse);
	}

	@Override
	public Meldungen getAktiveUndAusgesetztMeldungen() throws GenerateException {
		return meldeListeHelper.getMeldungen(SpielTagNr.from(1), Arrays.asList(SpielrundeGespielt.JA, SpielrundeGespielt.AUSGESETZT));
	}

	@Override
	public int getSpielerZeileNr(int spielerNr) throws GenerateException {
		return meldungenSpalte.getSpielerZeileNr(spielerNr);
	}

	@Override
	public Meldungen getAktiveMeldungen() throws GenerateException {
		return meldeListeHelper.getMeldungen(SpielTagNr.from(1), Arrays.asList(SpielrundeGespielt.JA));
	}

	@Override
	public Meldungen getInAktiveMeldungen() throws GenerateException {
		return meldeListeHelper.getMeldungen(SpielTagNr.from(1), Arrays.asList(SpielrundeGespielt.NEIN));
	}

	@Override
	public Meldungen getAlleMeldungen() throws GenerateException {
		return meldeListeHelper.getMeldungen(SpielTagNr.from(1), null);
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return meldeListeHelper.getXSpreadSheet();
	}

	@Override
	public final TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
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

	@Override
	public List<String> getSpielerNamenList() throws GenerateException {
		return meldungenSpalte.getSpielerNamenList();
	}

	@Override
	public List<Integer> getSpielerNrList() throws GenerateException {
		return meldungenSpalte.getSpielerNrList();
	}

	/**
	 * @return the spielerSpalte
	 */
	@Override
	public final MeldungenSpalte getMeldungenSpalte() {
		return meldungenSpalte;
	}

	@Override
	public int letzteSpielTagSpalte() throws GenerateException {
		return meldeListeHelper.ersteSpieltagSpalte();
	}

	@Override
	public int getSpielerNameSpalte() {
		return meldungenSpalte.getSpielerNameErsteSpalte();
	}

	@Override
	public int letzteZeileMitSpielerName() throws GenerateException {
		return meldungenSpalte.letzteZeileMitSpielerName();
	}

}