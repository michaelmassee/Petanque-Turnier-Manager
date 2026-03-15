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
import de.petanqueturniermanager.supermelee.spieltagrangliste.SpieltagRanglisteSheet;

public class SpielrundeSheet_Naechste extends SheetRunner
		implements ISheet, ISpielrundeSheet, SpielrundeSheetKonstanten {

	private final SpielrundeDelegate delegate;

	private SpielTagNr spielTag = null;
	private SpielRundeNr spielRundeNr = null;
	private boolean forceOk = false;

	public SpielrundeSheet_Naechste(WorkingSpreadsheet workingSpreadsheet) {
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

	void gespieltenRundenEinlesen(SpielerMeldungen aktiveMeldungen, int abSpielrunde, int bisSpielrunde)
			throws GenerateException {
		delegate.gespieltenRundenEinlesen(aktiveMeldungen, abSpielrunde, bisSpielrunde);
	}

	@Override
	protected void doRun() throws GenerateException {
		setSpielTag(getKonfigurationSheet().getAktiveSpieltag());
		if (naechsteSpielrundeEinfuegen()) {
			new SpielrundeSheet_Validator(getWorkingSpreadsheet()).validateSpieltag(getSpielTag());

			SpieltagRanglisteSheet spieltagRanglisteSheet = new SpieltagRanglisteSheet(getWorkingSpreadsheet());
			String ranglisteSheetName = spieltagRanglisteSheet.getSheetName(getSpielTag());
			XSpreadsheet xSpieltagRanglisteSheet = getSheetHelper().findByName(ranglisteSheetName);
			if (xSpieltagRanglisteSheet != null) {
				spieltagRanglisteSheet.generate(getSpielTag());
			}

			getSheetHelper().setActiveSheet(getXSpreadSheet());
		}
	}

	public boolean naechsteSpielrundeEinfuegen() throws GenerateException {
		SpielRundeNr aktuelleSpielrunde = getKonfigurationSheet().getAktiveSpielRunde();
		setSpielRundeNr(aktuelleSpielrunde);
		getMeldeListe().upDateSheet();
		SpielerMeldungen aktiveMeldungen = getMeldeListe().getAktiveMeldungen();

		if (!canStart(aktiveMeldungen)) {
			return false;
		}

		int neueSpielrunde = aktuelleSpielrunde.getNr();
		if (getSheetHelper().findByName(getSheetName(getSpielTag(), getSpielRundeNr())) != null) {
			neueSpielrunde++;
		}

		gespieltenRundenEinlesen(aktiveMeldungen, getKonfigurationSheet().getSpielRundeNeuAuslosenAb(),
				neueSpielrunde - 1);
		return neueSpielrunde(aktiveMeldungen, SpielRundeNr.from(neueSpielrunde));
	}
}
