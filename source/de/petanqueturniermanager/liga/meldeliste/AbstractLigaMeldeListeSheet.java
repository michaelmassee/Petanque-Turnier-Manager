/**
* Erstellung : 22.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.liga.meldeliste;

import java.util.List;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.IMeldeliste;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeHelper;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeKonstanten;
import de.petanqueturniermanager.basesheet.meldeliste.MeldungenSpalte;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.pagestyle.PageStyle;
import de.petanqueturniermanager.helper.pagestyle.PageStyleHelper;
import de.petanqueturniermanager.helper.sheet.IMitSpielerSpalte;
import de.petanqueturniermanager.liga.konfiguration.LigaSheet;
import de.petanqueturniermanager.model.Meldungen;

abstract public class AbstractLigaMeldeListeSheet extends LigaSheet implements IMeldeliste, Runnable, IMitSpielerSpalte, MeldeListeKonstanten {

	private final MeldungenSpalte spielerSpalte;
	private final MeldeListeHelper meldeListeHelper;

	/**
	 * @param workingSpreadsheet
	 */
	public AbstractLigaMeldeListeSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, "Meldeliste");
		spielerSpalte = new MeldungenSpalte(ERSTE_DATEN_ZEILE, SPIELER_NR_SPALTE, this, this, Formation.TETE);
		meldeListeHelper = new MeldeListeHelper(this);
	}

	public void upDateSheet() throws GenerateException {
		PageStyleHelper.from(this, PageStyle.PETTURNMNGR).initDefaultFooter().create().applytoSheet();
		processBoxinfo("Aktualisiere Meldungen");
		meldeListeHelper.testDoppelteMeldungen();
	}

	@Override
	public String formulaSverweisSpielernamen(String spielrNrAdresse) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Meldungen getAktiveUndAusgesetztMeldungen() throws GenerateException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getSpielerZeileNr(int spielerNr) throws GenerateException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Meldungen getAktiveMeldungen() throws GenerateException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Meldungen getAlleMeldungen() throws GenerateException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public XSpreadsheet getSheet() throws GenerateException {
		return meldeListeHelper.getSheet();
	}

	@Override
	public int neachsteFreieDatenZeile() throws GenerateException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void spielerEinfuegenWennNichtVorhanden(int spielerNr) throws GenerateException {
		// TODO Auto-generated method stub

	}

	@Override
	public int letzteDatenZeile() throws GenerateException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getErsteDatenZiele() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<String> getSpielerNamenList() throws GenerateException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Integer> getSpielerNrList() throws GenerateException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeldungenSpalte getMeldungenSpalte() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int letzteSpielTagSpalte() throws GenerateException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getSpielerNameSpalte() {
		return 0;
	}

}
