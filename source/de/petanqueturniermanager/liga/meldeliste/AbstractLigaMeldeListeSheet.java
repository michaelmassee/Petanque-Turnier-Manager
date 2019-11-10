/**
* Erstellung : 22.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.liga.meldeliste;

import java.util.Arrays;
import java.util.List;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.IMeldeliste;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeHelper;
import de.petanqueturniermanager.basesheet.meldeliste.MeldungenSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.SpielrundeGespielt;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.pagestyle.PageStyle;
import de.petanqueturniermanager.helper.pagestyle.PageStyleHelper;
import de.petanqueturniermanager.liga.konfiguration.LigaSheet;
import de.petanqueturniermanager.model.Meldungen;
import de.petanqueturniermanager.supermelee.SpielTagNr;

abstract public class AbstractLigaMeldeListeSheet extends LigaSheet implements IMeldeliste {

	private final MeldungenSpalte meldungenSpalte;
	private final MeldeListeHelper meldeListeHelper;

	/**
	 * @param workingSpreadsheet
	 */
	public AbstractLigaMeldeListeSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, "Meldeliste");
		meldungenSpalte = new MeldungenSpalte(ERSTE_DATEN_ZEILE, SPIELER_NR_SPALTE, this, this, Formation.TETE);
		meldeListeHelper = new MeldeListeHelper(this);
	}

	public void upDateSheet() throws GenerateException {
		PageStyleHelper.from(this, PageStyle.PETTURNMNGR).initDefaultFooter().create().applytoSheet();
		processBoxinfo("Aktualisiere Meldungen");
		meldeListeHelper.testDoppelteMeldungen();

		XSpreadsheet sheet = getSheet();
		getSheetHelper().setActiveSheet(sheet);

		// ------
		// Header einfuegen
		// ------
		int headerBackColor = getKonfigurationSheet().getMeldeListeHeaderFarbe();
		getMeldungenSpalte().insertHeaderInSheet(headerBackColor);
		// --------------------- TODO doppelt code entfernen

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
	public Meldungen getAlleMeldungen() throws GenerateException {
		return meldeListeHelper.getMeldungen(SpielTagNr.from(1), null);
	}

	@Override
	public XSpreadsheet getSheet() throws GenerateException {
		return meldeListeHelper.getSheet();
	}

	@Override
	public int neachsteFreieDatenZeile() throws GenerateException {
		return meldungenSpalte.neachsteFreieDatenZeile();
	}

	@Override
	public void spielerEinfuegenWennNichtVorhanden(int spielerNr) throws GenerateException {
		meldungenSpalte.spielerEinfuegenWennNichtVorhanden(spielerNr);
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

}
