package de.petanqueturniermanager.jedergegenjeden.meldeliste;

import java.util.List;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.IMeldeliste;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeHelper;
import de.petanqueturniermanager.basesheet.meldeliste.MeldungenSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.SpielrundeGespielt;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.jedergegenjeden.konfiguration.JGJSheet;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.supermelee.SpielTagNr;

/**
 * Erstellung 01.08.2022 / Michael Massee
 */

public abstract class AbstractJGJMeldeListeSheet extends JGJSheet implements IMeldeliste<TeamMeldungen, Team> {

	private static final int MIN_ANZAHL_MELDUNGEN_ZEILEN = 30; // Tabelle immer mit min anzahl von zeilen formatieren

	private final MeldungenSpalte<TeamMeldungen, Team> meldungenSpalte;
	private final MeldeListeHelper<TeamMeldungen, Team> meldeListeHelper;

	/**
	 * @param workingSpreadsheet
	 */
	public AbstractJGJMeldeListeSheet(WorkingSpreadsheet workingSpreadsheet) {
		this(workingSpreadsheet, "JGJ-Meldeliste");
	}

	public AbstractJGJMeldeListeSheet(WorkingSpreadsheet workingSpreadsheet, String prefix) {
		super(workingSpreadsheet, prefix);
		meldungenSpalte = MeldungenSpalte.Builder().spalteMeldungNameWidth(MELDUNG_NAME_WIDTH)
				.ersteDatenZiele(ERSTE_DATEN_ZEILE).spielerNrSpalte(SPIELER_NR_SPALTE).sheet(this)
				.formation(Formation.TRIPLETTE).minAnzahlAnzeilen(MIN_ANZAHL_MELDUNGEN_ZEILEN)
				.propertiesSpalte(getKonfigurationSheet()).build();
		meldeListeHelper = new MeldeListeHelper<>(this);
	}

	public void upDateSheet() throws GenerateException {
		processBoxinfo("Aktualisiere Meldungen");

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
		// formatDaten();

	}

	void formatDaten() throws GenerateException {

		processBoxinfo("Formatiere Daten Spalten");

		//		// TODO Doppelte Code
		//		// Spieler Nummer
		//		// -----------------------------------------------
		//		RangePosition nrSetPosRange = RangePosition.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE, SPIELER_NR_SPALTE,
		//				letzteDatenZeile);
		//		String conditionfindDoppeltNr = "COUNTIF(" + Position.from(SPIELER_NR_SPALTE, 0).getSpalteAddressWith$() + ";"
		//				+ ConditionalFormatHelper.FORMULA_CURRENT_CELL + ")>1";
		//		ConditionalFormatHelper.from(this, nrSetPosRange).clear().
		//		// ------------------------------
		//				formulaIsText().styleIsFehler().applyAndDoReset().
		//				// ------------------------------
		//				formula1(conditionfindDoppeltNr).operator(ConditionOperator.FORMULA).styleIsFehler().applyAndDoReset().
		//				// ------------------------------
		//				// eigentlich musste 0 = Fehler sein wird es aber nicht
		//				formula1("0").formula2("" + MeldungenSpalte.MAX_ANZ_MELDUNGEN).operator(ConditionOperator.NOT_BETWEEN)
		//				.styleIsFehler().applyAndDoReset(). // nr muss >0 und <999 sein
		//				formulaIsEvenRow().style(meldungenHintergrundFarbeGeradeStyle).applyAndDoReset().
		//				// ------------------------------
		//				formulaIsOddRow().style(meldungenHintergrundFarbeUnGeradeStyle).applyAndDoReset();
		//		// -----------------------------------------------
		//
		//		// TODO Doppelte Code
		//		// -----------------------------------------------
		//		// Spieler Namen
		//		// -----------------------------------------------
		//		RangePosition nameSetPosRange = RangePosition.from(getSpielerNameErsteSpalte(), ERSTE_DATEN_ZEILE,
		//				getSpielerNameErsteSpalte(), letzteDatenZeile);
		//		String conditionfindDoppeltNamen = "COUNTIF("
		//				+ Position.from(getSpielerNameErsteSpalte(), 0).getSpalteAddressWith$() + ";"
		//				+ ConditionalFormatHelper.FORMULA_CURRENT_CELL + ")>1";
		//		ConditionalFormatHelper.from(this, nameSetPosRange).clear().
		//		// ------------------------------
		//				formula1(conditionfindDoppeltNamen).operator(ConditionOperator.FORMULA).styleIsFehler()
		//				.applyAndDoReset().
		//				// ------------------------------
		//				formulaIsEvenRow().operator(ConditionOperator.FORMULA).style(meldungenHintergrundFarbeGeradeStyle)
		//				.applyAndDoReset().
		//				// ------------------------------
		//				formulaIsEvenRow().style(meldungenHintergrundFarbeGeradeStyle).applyAndDoReset().formulaIsOddRow()
		//				.style(meldungenHintergrundFarbeUnGeradeStyle).applyAndDoReset();
		//		// -----------------------------------------------
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
