/**
 * Erstellung : 01.03.2024 / Michael Massee
 * 
 */
package de.petanqueturniermanager.schweizer.meldeliste;

import java.util.List;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.IMeldeliste;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeHelper;
import de.petanqueturniermanager.basesheet.meldeliste.MeldungenSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.SpielrundeGespielt;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.cellstyle.MeldungenHintergrundFarbeGeradeStyle;
import de.petanqueturniermanager.helper.cellstyle.MeldungenHintergrundFarbeUnGeradeStyle;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerSheet;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.SpielTagNr;

/**
 * @author Michael Massee
 *
 */
abstract class AbstractSchweizerMeldeListeSheet extends SchweizerSheet implements IMeldeliste<TeamMeldungen, Team> {

	private static final int MIN_ANZAHL_MELDUNGEN_ZEILEN = 32; // Tabelle immer mit min anzahl von zeilen formatieren

	private final MeldungenSpalte<TeamMeldungen, Team> meldungenSpalte;
	private final MeldeListeHelper<TeamMeldungen, Team> meldeListeHelper;

	/**
	 * @param workingSpreadsheet
	 */
	protected AbstractSchweizerMeldeListeSheet(WorkingSpreadsheet workingSpreadsheet) {
		this(workingSpreadsheet, "Schweizer-Meldeliste");
	}

	protected AbstractSchweizerMeldeListeSheet(WorkingSpreadsheet workingSpreadsheet, String prefix) {
		super(workingSpreadsheet, prefix);
		meldungenSpalte = MeldungenSpalte.builder().spalteMeldungNameWidth(MELDUNG_NAME_WIDTH)
				.ersteDatenZiele(ERSTE_DATEN_ZEILE).spielerNrSpalte(SPIELER_NR_SPALTE).sheet(this)
				.minAnzZeilen(MIN_ANZAHL_MELDUNGEN_ZEILEN).formation(Formation.TRIPLETTE).build();
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

		// eventuelle luecken in spiele namen nach unten sortieren
		meldeListeHelper.zeileOhneSpielerNamenEntfernen();
		meldeListeHelper.updateMeldungenNr();
		meldeListeHelper.doSort(meldungenSpalte.getSpielerNrSpalte(), true); // nach nr sortieren
		meldungenSpalte.formatSpielrNrUndNamenspalten(); // Format NR + namen Spalten

		// ---------------------------------------------------------
		int letzteDatenZeile = getLetzteDatenZeileUseMin();

		MeldungenHintergrundFarbeGeradeStyle meldungenHintergrundFarbeGeradeStyle = getKonfigurationSheet()
				.getMeldeListeHintergrundFarbeGeradeStyle();
		MeldungenHintergrundFarbeUnGeradeStyle meldungenHintergrundFarbeUnGeradeStyle = getKonfigurationSheet()
				.getMeldeListeHintergrundFarbeUnGeradeStyle();

		// Spieler Nummer
		// -----------------------------------------------
		meldeListeHelper.insertFormulaFuerDoppelteSpielerNrGeradeUngradeFarbe(letzteDatenZeile, this,
				meldungenHintergrundFarbeGeradeStyle, meldungenHintergrundFarbeUnGeradeStyle);
		// -----------------------------------------------

		meldeListeHelper.insertFormulaFuerDoppelteNamenGeradeUngradeFarbe(meldungenSpalte.getErsteMeldungNameSpalte(),
				meldungenSpalte.getLetzteMeldungNameSpalte(), letzteDatenZeile, this,
				meldungenHintergrundFarbeGeradeStyle, meldungenHintergrundFarbeUnGeradeStyle);

	}

	@Override
	public String formulaSverweisSpielernamen(String spielrNrAdresse) {
		return meldeListeHelper.formulaSverweisSpielernamen(spielrNrAdresse);
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
		return new TeamMeldungen();
	}

	@Override
	public TeamMeldungen getAlleMeldungen() throws GenerateException {
		return meldeListeHelperGetMeldungen(SpielTagNr.from(1), null);
	}

	private TeamMeldungen meldeListeHelperGetMeldungen(final SpielTagNr spieltag,
			final List<SpielrundeGespielt> spielrundeGespielt) throws GenerateException {
		return (TeamMeldungen) meldeListeHelper.getMeldungen(spieltag, spielrundeGespielt, new TeamMeldungen());
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
	public int getLetzteDatenZeileUseMin() throws GenerateException {
		return meldungenSpalte.getLetzteDatenZeileUseMin();
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
	public int getSpielerNameErsteSpalte() {
		return meldungenSpalte.getErsteMeldungNameSpalte();
	}

	@Override
	public int letzteZeileMitSpielerName() throws GenerateException {
		return meldungenSpalte.letzteZeileMitSpielerName();
	}

	/**
	 * @param SpielRundeNr
	 * @throws GenerateException
	 */
	public void setAktiveSpielRunde(SpielRundeNr spielRundeNr) throws GenerateException {
		getKonfigurationSheet().setAktiveSpielRunde(spielRundeNr);
	}

}