/**
* Erstellung : 30.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.schweizer.meldeliste;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.basesheet.meldeliste.MeldungenSpalte;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.model.IMeldungen;
import de.petanqueturniermanager.model.Spieler;
import de.petanqueturniermanager.model.SpielerMeldungen;

public class SchweizerMeldeListeTeteSheet_New extends AbstractSchweizerMeldeListeSheet {
	private static final Logger logger = LogManager.getLogger(SchweizerMeldeListeTeteSheet_New.class);

	public SchweizerMeldeListeTeteSheet_New(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
	}

	@Override
	protected void doRun() throws GenerateException {
		if (NewSheet.from(this, SHEETNAME).pos(DefaultSheetPos.MELDELISTE).hideGrid().tabColor(SHEET_COLOR).create().isDidCreate()) {
			// upDateSheet();
		}
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

	@Override
	public String formulaSverweisSpielernamen(String spielrNrAdresse) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IMeldungen<SpielerMeldungen, Spieler> getAktiveUndAusgesetztMeldungen() throws GenerateException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IMeldungen<SpielerMeldungen, Spieler> getAktiveMeldungen() throws GenerateException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IMeldungen<SpielerMeldungen, Spieler> getInAktiveMeldungen() throws GenerateException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IMeldungen<SpielerMeldungen, Spieler> getAlleMeldungen() throws GenerateException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeldungenSpalte<SpielerMeldungen, Spieler> getMeldungenSpalte() {
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
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TurnierSheet getTurnierSheet() throws GenerateException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getErsteDatenZiele() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getLetzteMitDatenZeileInSpielerNrSpalte() throws GenerateException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int neachsteFreieDatenZeileInSpielerNrSpalte() throws GenerateException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int letzteZeileMitSpielerName() throws GenerateException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getSpielerZeileNr(int spielerNr) throws GenerateException {
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
