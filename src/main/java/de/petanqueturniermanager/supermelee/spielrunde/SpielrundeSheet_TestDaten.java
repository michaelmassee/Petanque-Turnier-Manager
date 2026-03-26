/**
 * Erstellung : 26.03.2018 / Michael Massee
 **/
package de.petanqueturniermanager.supermelee.spielrunde;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.ThreadLocalRandom;

import com.google.common.annotations.VisibleForTesting;
import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.NewTestDatenValidator;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.model.SpielerMeldungen;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeKonfigurationSheet;
import de.petanqueturniermanager.supermelee.meldeliste.AnmeldungenSheet;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_TestDaten;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_Update;
import de.petanqueturniermanager.supermelee.meldeliste.TeilnehmerSheet;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;
import de.petanqueturniermanager.supermelee.spieltagrangliste.SpieltagRanglisteSheet;
import de.petanqueturniermanager.supermelee.spieltagrangliste.SpieltagRangliste_Validator;

public class SpielrundeSheet_TestDaten extends SheetRunner
		implements ISheet, ISpielrundeSheet, SpielrundeSheetKonstanten {

	private final SpielrundeDelegate delegate;
	private final SpielrundeSheet_Naechste naechsteSpielrundeSheet;
	private final MeldeListeSheet_TestDaten meldeListeTestDatenGenerator;
	private final SpieltagRanglisteSheet spieltagRanglisteSheet;
	private final AnmeldungenSheet anmeldungenSheet;
	private final TeilnehmerSheet teilnehmerSheet;

	private SpielTagNr spielTag = null;
	private SpielRundeNr spielRundeNr = null;
	private boolean forceOk = false;

	public SpielrundeSheet_TestDaten(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.SUPERMELEE, "Spielrunde");
		delegate = new SpielrundeDelegate(this, newSuperMeleeKonfigurationSheet(workingSpreadsheet),
				newMeldeListeSheet(workingSpreadsheet));
		naechsteSpielrundeSheet = new SpielrundeSheet_Naechste(workingSpreadsheet);
		meldeListeTestDatenGenerator = new MeldeListeSheet_TestDaten(workingSpreadsheet);
		spieltagRanglisteSheet = new SpieltagRanglisteSheet(workingSpreadsheet);
		anmeldungenSheet = new AnmeldungenSheet(workingSpreadsheet);
		teilnehmerSheet = new TeilnehmerSheet(workingSpreadsheet);
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
	public TurnierSheet getTurnierSheet() throws GenerateException {
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

	protected boolean neueSpielrunde(SpielerMeldungen meldungen, SpielRundeNr neueSpielrundeNr, boolean force)
			throws GenerateException {
		return delegate.neueSpielrunde(meldungen, neueSpielrundeNr, force);
	}

	@Override
	protected void doRun() throws GenerateException {
		if (!NewTestDatenValidator.from(getWorkingSpreadsheet(), getSheetHelper(), TurnierSystem.SUPERMELEE)
				.prefix(getLogPrefix()).validate()) {
			return;
		}

		getSheetHelper().removeAllSheetsExclude(new String[] { SheetNamen.supermeleeTeams() });
		setSpielTag(SpielTagNr.from(1));
		getKonfigurationSheet().setAktiveSpieltag(getSpielTag());
		generate();
		new SpielrundeSheet_Validator(getWorkingSpreadsheet()).validateSpieltag(getSpielTag());
		getSheetHelper().setActiveSheet(getXSpreadSheet());
	}

	public void generate() throws GenerateException {
		anmeldungenSheet.setSpielTag(getSpielTag());
		teilnehmerSheet.setSpielTagNr(getSpielTag());
		spieltagRanglisteSheet.setSpieltagNr(getSpielTag());
		naechsteSpielrundeSheet.setSpielTag(getSpielTag());

		if (getSpielTag().getNr() == 1) {
			meldeListeTestDatenGenerator.testNamenEinfuegen();
		}

		meldeListeTestDatenGenerator.initialAktuellenSpielTagMitAktivenMeldungenFuellen(getSpielTag());

		anmeldungenSheet.generate();
		teilnehmerSheet.generate();

		int maxspielrundeNr = 4;
		genRunden(maxspielrundeNr, true);

		SheetRunner.testDoCancelTask();
		spieltagRanglisteSheet.generate(getSpielTag());
		new SpieltagRangliste_Validator(getWorkingSpreadsheet()).doValidate(getSpielTag());

		getKonfigurationSheet().setAktiveSpieltag(getSpielTag());
		getKonfigurationSheet().setAktiveSpielRunde(SpielRundeNr.from(maxspielrundeNr));

		spieltagRanglisteSheet.isErrorInSheet();
	}

	public void genRunden(int maxspielrundeNr, boolean shuffleAktivInaktiv) throws GenerateException {
		naechsteSpielrundeSheet.setSpielTag(getSpielTag());

		for (int spielrundeNr = 1; spielrundeNr <= maxspielrundeNr; spielrundeNr++) {
			SheetRunner.testDoCancelTask();
			setSpielRundeNr(SpielRundeNr.from(spielrundeNr));

			if (shuffleAktivInaktiv && spielrundeNr > 1) {
				meldeListeTestDatenGenerator.spielerAufAktivInaktivMischen(getSpielTag());
			}

			SpielerMeldungen aktiveMeldungen = getMeldeListe().getAktiveMeldungen();
			naechsteSpielrundeSheet.gespieltenRundenEinlesen(aktiveMeldungen,
					getKonfigurationSheet().getSpielRundeNeuAuslosenAb(), spielrundeNr - 1);
			if (!neueSpielrunde(aktiveMeldungen, SpielRundeNr.from(spielrundeNr), true)) {
				return;
			}

			XSpreadsheet xsheet = getXSpreadSheet();
			Position letztePos = letztePositionRechtsUnten();

			if (letztePos != null && xsheet != null) {
				for (int zeileCntr = ERSTE_DATEN_ZEILE; zeileCntr <= letztePos.getZeile(); zeileCntr++) {
					SheetRunner.testDoCancelTask();

					Position pos = Position.from(ERSTE_SPALTE_ERGEBNISSE, zeileCntr);

					int welchenTeamHatGewonnen = ThreadLocalRandom.current().nextInt(0, 2);
					int verliererPunkte = ThreadLocalRandom.current().nextInt(0, 13);
					int gewinnerPunkte = ThreadLocalRandom.current().nextInt(verliererPunkte + 1, 14);
					int valA = (welchenTeamHatGewonnen == 0 ? verliererPunkte : gewinnerPunkte);
					int valB = (welchenTeamHatGewonnen == 0 ? gewinnerPunkte : verliererPunkte);

					NumberCellValue numberCellValue = NumberCellValue.from(xsheet, pos, valA);
					getSheetHelper().setNumberValueInCell(numberCellValue);
					getSheetHelper().setNumberValueInCell(numberCellValue.spaltePlusEins().setValue((double) valB));
				}
			}
		}
	}
}
