/**
 * Erstellung : 26.03.2018 / Michael Massee
 **/
package de.petanqueturniermanager.supermelee.spielrunde;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.model.SpielerMeldungen;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeKonfigurationSheet;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_Update;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

public class SpielrundeSheet_Update extends SheetRunner
		implements ISheet, ISpielrundeSheet, SpielrundeSheetKonstanten {

	private final SpielrundeDelegate delegate;

	private SpielTagNr spielTag = null;
	private SpielRundeNr spielRundeNr = null;
	private boolean forceOk = false;

	public SpielrundeSheet_Update(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.SUPERMELEE, "Spielrunde");
		delegate = new SpielrundeDelegate(this, newSuperMeleeKonfigurationSheet(workingSpreadsheet),
				newMeldeListeSheet(workingSpreadsheet));
	}

	@VisibleForTesting
	protected SuperMeleeKonfigurationSheet newSuperMeleeKonfigurationSheet(WorkingSpreadsheet workingSpreadsheet) {
		return new SuperMeleeKonfigurationSheet(workingSpreadsheet);
	}

	@VisibleForTesting
	protected MeldeListeSheet_Update newMeldeListeSheet(WorkingSpreadsheet workingSpreadsheet) {
		return new MeldeListeSheet_Update(workingSpreadsheet);
	}

	@Override
	public SuperMeleeKonfigurationSheet getKonfigurationSheet() {
		return delegate.getKonfigurationSheet();
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return getSheetHelper().findByName(getSheetName(getSpielTag(), getSpielRundeNr()));
	}

	@Override
	public final TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}

	@Override
	public SpielTagNr getSpielTag() {
		checkNotNull(spielTag);
		return spielTag;
	}

	public void setSpielTag(SpielTagNr spielTag) {
		checkNotNull(spielTag);
		this.spielTag = spielTag;
	}

	@Override
	public SpielRundeNr getSpielRundeNr() {
		checkNotNull(spielRundeNr);
		return spielRundeNr;
	}

	@Override
	public void setSpielRundeNr(SpielRundeNr spielRundeNr) {
		checkNotNull(spielRundeNr);
		this.spielRundeNr = spielRundeNr;
	}

	@Override
	public boolean isForceOk() {
		return forceOk;
	}

	public void setForceOk(boolean forceOk) {
		this.forceOk = forceOk;
	}

	@Override
	public Integer getMaxAnzGespielteSpieltage() throws GenerateException {
		return delegate.getMaxAnzGespielteSpieltage();
	}

	public MeldeListeSheet_Update getMeldeListe() {
		return delegate.getMeldeListe();
	}

	public String getSheetName(SpielTagNr spieltag, SpielRundeNr spielrunde) {
		return delegate.getSheetName(spieltag, spielrunde);
	}

	public Position letztePositionRechtsUnten() throws GenerateException {
		return delegate.letztePositionRechtsUnten();
	}

	protected boolean canStart(SpielerMeldungen meldungen) throws GenerateException {
		return delegate.canStart(meldungen);
	}

	protected boolean neueSpielrunde(SpielerMeldungen meldungen, SpielRundeNr neueSpielrundeNr)
			throws GenerateException {
		return delegate.neueSpielrunde(meldungen, neueSpielrundeNr);
	}

	protected boolean neueSpielrunde(SpielerMeldungen meldungen, SpielRundeNr neueSpielrundeNr, boolean force)
			throws GenerateException {
		return delegate.neueSpielrunde(meldungen, neueSpielrundeNr, force);
	}

	void gespieltenRundenEinlesen(SpielerMeldungen aktiveMeldungen, int abSpielrunde, int bisSpielrunde)
			throws GenerateException {
		delegate.gespieltenRundenEinlesen(aktiveMeldungen, abSpielrunde, bisSpielrunde);
	}

	protected void clearSheet() throws GenerateException {
		delegate.clearSheet();
	}

	@Override
	protected void doRun() throws GenerateException {
		setSpielTag(getKonfigurationSheet().getAktiveSpieltag());
		SpielRundeNr aktuelleSpielrunde = getKonfigurationSheet().getAktiveSpielRunde();
		setSpielRundeNr(aktuelleSpielrunde);
		getMeldeListe().upDateSheet();
		SpielerMeldungen aktiveMeldungen = getMeldeListe().getAktiveMeldungen();

		if (!canStart(aktiveMeldungen)) {
			return;
		}

		gespieltenRundenEinlesen(aktiveMeldungen, getKonfigurationSheet().getSpielRundeNeuAuslosenAb(),
				aktuelleSpielrunde.getNr() - 1);
		if (neueSpielrunde(aktiveMeldungen, aktuelleSpielrunde)) {
			new SpielrundeSheet_Validator(getWorkingSpreadsheet()).validateSpieltag(getSpielTag());
			getSheetHelper().setActiveSheet(getXSpreadSheet());
		}
	}

	public SpielerSpielrundeErgebnisList ergebnisseEinlesen() throws GenerateException {
		SpielerSpielrundeErgebnisList spielerSpielrundeErgebnisse = new SpielerSpielrundeErgebnisList();
		XSpreadsheet xsheet = getXSpreadSheet();

		if (xsheet == null) {
			return spielerSpielrundeErgebnisse;
		}

		Position spielerNrPos = Position.from(ERSTE_SPIELERNR_SPALTE, ERSTE_DATEN_ZEILE);

		int tCntr = 0;
		boolean spielerZeilevorhanden = true;
		while (spielerZeilevorhanden && tCntr++ < 999) {
			spielerZeilevorhanden = false;

			for (int spalteCntr = 1; spalteCntr <= 3; spalteCntr++) {
				int spielerNr = getSheetHelper().getIntFromCell(xsheet, spielerNrPos);
				if (spielerNr > 0) {
					spielerZeilevorhanden = true;
					spielerSpielrundeErgebnisse.add(SpielerSpielrundeErgebnis.from(getSpielRundeNr(), spielerNr,
							spielerNrPos, ERSTE_SPALTE_ERGEBNISSE, SpielRundeTeam.A));
				}
				spielerNrPos.spaltePlusEins();
			}

			for (int spalteCntr = 1; spalteCntr <= 3; spalteCntr++) {
				int spielerNr = getSheetHelper().getIntFromCell(xsheet, spielerNrPos);
				if (spielerNr > 0) {
					spielerZeilevorhanden = true;
					spielerSpielrundeErgebnisse.add(SpielerSpielrundeErgebnis.from(getSpielRundeNr(), spielerNr,
							spielerNrPos, ERSTE_SPALTE_ERGEBNISSE, SpielRundeTeam.B));
				}
				spielerNrPos.spaltePlusEins();
			}
			spielerNrPos.zeilePlusEins().spalte(ERSTE_SPIELERNR_SPALTE);
		}
		return spielerSpielrundeErgebnisse;
	}

	public int countNumberOfSpielRundenSheets(SpielTagNr spieltag) throws GenerateException {
		checkNotNull(spieltag);
		int anz = 0;
		int anzSheets = getSheetHelper().getAnzSheets();
		for (int rdnCntr = 1; rdnCntr <= anzSheets; rdnCntr++) {
			if (getSheetHelper().findByName(getSheetName(spieltag, SpielRundeNr.from(rdnCntr))) != null) {
				anz++;
			}
		}
		return anz;
	}
}
