/**
 * Erstellung : 30.04.2018 / Michael Massee
 **/

package de.petanqueturniermanager.supermelee.meldeliste;

import java.util.List;

import com.google.common.annotations.VisibleForTesting;
import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.IMeldeliste;
import de.petanqueturniermanager.basesheet.meldeliste.MeldungenSpalte;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.model.Spieler;
import de.petanqueturniermanager.model.SpielerMeldungen;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeKonfigurationSheet;

public class MeldeListeSheet_NeuerSpieltag extends SheetRunner implements IMeldeliste<SpielerMeldungen, Spieler> {

	private final SupermeleeListeDelegate delegate;

	public MeldeListeSheet_NeuerSpieltag(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.SUPERMELEE, "Meldeliste");
		delegate = new SupermeleeListeDelegate(this, workingSpreadsheet,
				newSuperMeleeKonfigurationSheet(workingSpreadsheet));
	}

	@VisibleForTesting
	protected SuperMeleeKonfigurationSheet newSuperMeleeKonfigurationSheet(WorkingSpreadsheet workingSpreadsheet) {
		return new SuperMeleeKonfigurationSheet(workingSpreadsheet);
	}

	@Override
	protected SuperMeleeKonfigurationSheet getKonfigurationSheet() {
		return delegate.getKonfigurationSheet();
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return getSheetHelper().findByName(SHEETNAME);
	}

	@Override
	public final TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}

	@Override
	public MeldungenSpalte<SpielerMeldungen, Spieler> getMeldungenSpalte() {
		return delegate.getMeldungenSpalte();
	}

	@Override
	public String formulaSverweisSpielernamen(String spielrNrAdresse) {
		return delegate.formulaSverweisSpielernamen(spielrNrAdresse);
	}

	@Override
	public SpielerMeldungen getAktiveUndAusgesetztMeldungen() throws GenerateException {
		return delegate.getAktiveUndAusgesetztMeldungen();
	}

	@Override
	public SpielerMeldungen getAktiveMeldungen() throws GenerateException {
		return delegate.getAktiveMeldungen();
	}

	@Override
	public SpielerMeldungen getInAktiveMeldungen() throws GenerateException {
		return delegate.getInAktiveMeldungen();
	}

	@Override
	public SpielerMeldungen getAlleMeldungen() throws GenerateException {
		return delegate.getAlleMeldungen();
	}

	@Override
	public int letzteSpielTagSpalte() throws GenerateException {
		return delegate.letzteSpielTagSpalte();
	}

	@Override
	public int getSpielerNameErsteSpalte() {
		return delegate.getSpielerNameErsteSpalte();
	}

	@Override
	public int getLetzteDatenZeileUseMin() throws GenerateException {
		return delegate.getLetzteDatenZeileUseMin();
	}

	@Override
	public int getSpielerZeileNr(int spielerNr) throws GenerateException {
		return delegate.getSpielerZeileNr(spielerNr);
	}

	@Override
	public int naechsteFreieDatenZeileInSpielerNrSpalte() throws GenerateException {
		return delegate.naechsteFreieDatenZeileInSpielerNrSpalte();
	}

	@Override
	public int getLetzteMitDatenZeileInSpielerNrSpalte() throws GenerateException {
		return delegate.getLetzteMitDatenZeileInSpielerNrSpalte();
	}

	@Override
	public int getErsteDatenZiele() {
		return delegate.getErsteDatenZiele();
	}

	@Override
	public List<String> getSpielerNamenList() throws GenerateException {
		return delegate.getSpielerNamenList();
	}

	@Override
	public List<Integer> getSpielerNrList() throws GenerateException {
		return delegate.getSpielerNrList();
	}

	@Override
	public int letzteZeileMitSpielerName() throws GenerateException {
		return delegate.letzteZeileMitSpielerName();
	}

	public void upDateSheet() throws GenerateException {
		delegate.upDateSheet();
	}

	public void setSpielTag(SpielTagNr spielTag) {
		delegate.setSpielTag(spielTag);
	}

	public SpielTagNr getSpielTag() {
		return delegate.getSpielTag();
	}

	public int aktuelleSpieltagSpalte() {
		return delegate.aktuelleSpieltagSpalte();
	}

	public int countAnzSpieltageInMeldeliste() throws GenerateException {
		return delegate.countAnzSpieltageInMeldeliste();
	}

	public void setAktiveSpieltag(SpielTagNr spielTagNr) throws GenerateException {
		delegate.setAktiveSpieltag(spielTagNr);
	}

	public void setAktiveSpielRunde(SpielRundeNr spielRundeNr) throws GenerateException {
		delegate.setAktiveSpielRunde(spielRundeNr);
	}

	public void naechsteSpieltag() throws GenerateException {
		int anzSpieltage = countAnzSpieltageInMeldeliste();
		getKonfigurationSheet().setAktiveSpieltag(SpielTagNr.from(anzSpieltage + 1));
		setSpielTag(getKonfigurationSheet().getAktiveSpieltag());
		getKonfigurationSheet().setAktiveSpielRunde(SpielRundeNr.from(1));

		RangePosition cleanUpRange = RangePosition.from(aktuelleSpieltagSpalte(), ERSTE_HEADER_ZEILE,
				aktuelleSpieltagSpalte(), MeldungenSpalte.MAX_ANZ_MELDUNGEN);
		RangeHelper.from(this, cleanUpRange).clearRange();
		upDateSheet();
		getxCalculatable().calculateAll();
	}

	@Override
	protected void doRun() throws GenerateException {
		naechsteSpieltag();
	}
}
