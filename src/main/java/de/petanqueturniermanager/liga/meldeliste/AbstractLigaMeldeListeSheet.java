/**
 * Erstellung : 22.03.2018 / Michael Massee
 **/

package de.petanqueturniermanager.liga.meldeliste;

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
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxResult;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.ConditionalFormatHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.liga.konfiguration.LigaSheet;
import de.petanqueturniermanager.liga.spielplan.LigaSpielPlanSheet;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.supermelee.SpielTagNr;

abstract public class AbstractLigaMeldeListeSheet extends LigaSheet implements IMeldeliste<TeamMeldungen, Team> {

	private static final int MIN_ANZAHL_MELDUNGEN_ZEILEN = 4; // Tabelle immer mit min anzahl von zeilen formatieren

	private final MeldungenSpalte<TeamMeldungen, Team> meldungenSpalte;
	private final MeldeListeHelper<TeamMeldungen, Team> meldeListeHelper;

	/**
	 * @param workingSpreadsheet
	 */
	public AbstractLigaMeldeListeSheet(WorkingSpreadsheet workingSpreadsheet) {
		this(workingSpreadsheet, "Liga-Meldeliste");
	}

	public AbstractLigaMeldeListeSheet(WorkingSpreadsheet workingSpreadsheet, String prefix) {
		super(workingSpreadsheet, prefix);
		meldungenSpalte = MeldungenSpalte.Builder().spalteMeldungNameWidth(LIGA_MELDUNG_NAME_WIDTH)
				.ersteDatenZiele(ERSTE_DATEN_ZEILE).spielerNrSpalte(SPIELER_NR_SPALTE).sheet(this)
				.formation(Formation.TETE).build();
		meldeListeHelper = new MeldeListeHelper<>(this);
	}

	/**
	 * ab der Version 3.3.0 neuer Name
	 * 
	 * @throws GenerateException
	 */
	private void renameSpielPlanSheet() throws GenerateException {
		String oldName = "Liga Spielplan";
		processBoxinfo("Pruefe ob " + oldName + " vorhanden");
		if (getSheetHelper().findByName(oldName) != null) {
			MessageBoxResult answer = MessageBox.from(getxContext(), MessageBoxTypeEnum.WARN_YES_NO)
					.caption("Scheet " + oldName).message("Achtung die Tabelle " + oldName + " muss unbenant werden in "
							+ LigaSpielPlanSheet.SHEET_NAMEN)
					.show();

			if (answer == MessageBoxResult.YES) {
				XSpreadsheet spielplan = getSheetHelper().findByName(oldName);
				if (getSheetHelper().reNameSheet(spielplan, LigaSpielPlanSheet.SHEET_NAMEN)) {
					MessageBox.from(getxContext(), MessageBoxTypeEnum.INFO_OK).caption("Scheet " + oldName)
							.message("Die Tabelle " + oldName + "wurde unbenant in " + LigaSpielPlanSheet.SHEET_NAMEN)
							.show();
				} else {
					MessageBox.from(getxContext(), MessageBoxTypeEnum.ERROR_OK).caption("Scheet " + oldName)
							.message("Fehler: Die Tabelle " + oldName + " wurde nicht unbenant in "
									+ LigaSpielPlanSheet.SHEET_NAMEN)
							.show();
					throw new GenerateException("Sheet mit Namen " + oldName + " Vorhanden. Bitte umbenen in "
							+ LigaSpielPlanSheet.SHEET_NAMEN);
				}
			} else {
				throw new GenerateException("Sheet mit Namen " + oldName + " Vorhanden. Bitte umbenen in "
						+ LigaSpielPlanSheet.SHEET_NAMEN);
			}
		}
	}

	public void upDateSheet() throws GenerateException {
		processBoxinfo("Aktualisiere Meldungen");
		renameSpielPlanSheet();

		TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet()).setActiv();
		meldeListeHelper.testDoppelteMeldungen();

		// ------
		// Header einfuegen
		// ------
		int headerBackColor = getKonfigurationSheet().getMeldeListeHeaderFarbe();
		getMeldungenSpalte().insertHeaderInSheet(headerBackColor);
		// --------------------- TODO doppelt code entfernen

		// eventuelle luecken in spiele namen nach unten sortieren
		meldeListeHelper.zeileOhneErsteSpielerNamenEntfernen();
		meldeListeHelper.updateMeldungenNr();
		meldeListeHelper.doSort(meldungenSpalte.getSpielerNrSpalte(), true); // nach nr sortieren
		meldungenSpalte.formatDaten();
		formatDaten();

		// TurnierSystem
		// meldeListeHelper.insertTurnierSystemInHeader(getTurnierSystem());
	}

	void formatDaten() throws GenerateException {

		processBoxinfo("Formatiere Daten Spalten");

		int letzteDatenZeile = meldungenSpalte.getLetzteMitDatenZeileInSpielerNrSpalte();

		if (letzteDatenZeile < MIN_ANZAHL_MELDUNGEN_ZEILEN) {
			letzteDatenZeile = MIN_ANZAHL_MELDUNGEN_ZEILEN;
		}

		if (letzteDatenZeile < ERSTE_DATEN_ZEILE) {
			// keine Daten
			return;
		}

		RangePosition datenRange = RangePosition.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE, getSpielerNameErsteSpalte(),
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

		// TODO Doppelte Code
		// Spieler Nummer
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
				// eigentlich musste 0 = Fehler sein wird es aber nicht
				formula1("0").formula2("" + MeldungenSpalte.MAX_ANZ_MELDUNGEN).operator(ConditionOperator.NOT_BETWEEN)
				.styleIsFehler().applyAndDoReset(). // nr muss >0 und
																																							// <999 sein
																																							// ------------------------------
				formulaIsEvenRow().style(meldungenHintergrundFarbeGeradeStyle).applyAndDoReset().
				// ------------------------------
				formulaIsOddRow().style(meldungenHintergrundFarbeUnGeradeStyle).applyAndDoReset();
		// -----------------------------------------------

		// TODO Doppelte Code
		// -----------------------------------------------
		// Spieler Namen
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
	}

	public int getSpielerNameErsteSpalte() {
		return meldungenSpalte.getSpielerNameErsteSpalte();
	}

	@Override
	public String formulaSverweisSpielernamen(String spielrNrAdresse) {
		return meldeListeHelper.formulaSverweisErsteSpielernamen(spielrNrAdresse);
	}

	@Override
	public TeamMeldungen getAktiveUndAusgesetztMeldungen() throws GenerateException {
		return getAlleMeldungen();
	}

	@Override
	public int getSpielerZeileNr(int spielerNr) throws GenerateException {
		return meldungenSpalte.getSpielerZeileNr(spielerNr);
	}

	@Override
	public TeamMeldungen getAktiveMeldungen() throws GenerateException {
		return getAlleMeldungen();
	}

	@Override
	public TeamMeldungen getInAktiveMeldungen() throws GenerateException {
		// leer, gibt es nicht
		return new TeamMeldungen();
	}

	@Override
	public TeamMeldungen getAlleMeldungen() throws GenerateException {
		return meldeListeHelperGetMeldungen(SpielTagNr.from(1), null);
	}

	private TeamMeldungen meldeListeHelperGetMeldungen(final SpielTagNr spieltag,
			final List<SpielrundeGespielt> spielrundeGespielt) throws GenerateException {
		return (TeamMeldungen) meldeListeHelper.getMeldungenForSpieltag(spieltag, spielrundeGespielt,
				new TeamMeldungen());
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
	public int neachsteFreieDatenZeileInSpielerNrSpalte() throws GenerateException {
		return meldungenSpalte.neachsteFreieDatenZeileInSpielerNrSpalte();
	}

	@Override
	public int getLetzteMitDatenZeileInSpielerNrSpalte() throws GenerateException {
		return meldungenSpalte.getLetzteMitDatenZeileInSpielerNrSpalte();
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
	public final MeldungenSpalte<TeamMeldungen, Team> getMeldungenSpalte() {
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
