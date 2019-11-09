/**
* Erstellung : 22.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.liga.meldeliste;

import java.util.List;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.IMeldeliste;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeKonstanten;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.sheet.IMitSpielerSpalte;
import de.petanqueturniermanager.model.Meldungen;

abstract public class AbstractLigaMeldeListeSheet extends SheetRunner implements IMeldeliste, Runnable, ISheet, IMitSpielerSpalte, MeldeListeKonstanten {

	/**
	 * @param workingSpreadsheet
	 */
	public AbstractLigaMeldeListeSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
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
		// TODO Auto-generated method stub
		return null;
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

}
