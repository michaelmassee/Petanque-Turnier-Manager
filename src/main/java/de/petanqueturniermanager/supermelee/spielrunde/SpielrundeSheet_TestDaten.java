/**
 * Erstellung : 26.03.2018 / Michael Massee
 **/
package de.petanqueturniermanager.supermelee.spielrunde;

import java.util.concurrent.ThreadLocalRandom;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.NewTestDatenValidator;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.model.SpielerMeldungen;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.SupermeleeTeamPaarungenSheet;
import de.petanqueturniermanager.supermelee.meldeliste.AnmeldungenSheet;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_TestDaten;
import de.petanqueturniermanager.supermelee.meldeliste.TielnehmerSheet;
import de.petanqueturniermanager.supermelee.spieltagrangliste.SpieltagRanglisteSheet;
import de.petanqueturniermanager.supermelee.spieltagrangliste.SpieltagRangliste_Validator;

public class SpielrundeSheet_TestDaten extends AbstractSpielrundeSheet {
	private static final Logger logger = LogManager.getLogger(SpielrundeSheet_TestDaten.class);

	private final SpielrundeSheet_Naechste naechsteSpielrundeSheet;
	private final MeldeListeSheet_TestDaten meldeListeTestDatenGenerator;
	private final SpieltagRanglisteSheet spieltagRanglisteSheet;
	private final AnmeldungenSheet anmeldungenSheet;
	private final TielnehmerSheet tielnehmerSheet;

	public SpielrundeSheet_TestDaten(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
		naechsteSpielrundeSheet = new SpielrundeSheet_Naechste(workingSpreadsheet);
		meldeListeTestDatenGenerator = new MeldeListeSheet_TestDaten(workingSpreadsheet);
		spieltagRanglisteSheet = new SpieltagRanglisteSheet(workingSpreadsheet);
		anmeldungenSheet = new AnmeldungenSheet(workingSpreadsheet);
		tielnehmerSheet = new TielnehmerSheet(workingSpreadsheet);

	}

	@Override
	public Logger getLogger() {
		return logger;
	}

	@Override
	protected void doRun() throws GenerateException {

		if (!NewTestDatenValidator.from(getWorkingSpreadsheet(), getSheetHelper()).prefix(getLogPrefix()).validate()) {
			return;
		}

		// clean up first
		getSheetHelper().removeAllSheetsExclude(new String[] { SupermeleeTeamPaarungenSheet.SHEETNAME });
		setSpielTag(SpielTagNr.from(1));
		getKonfigurationSheet().setAktiveSpieltag(getSpielTag());
		generate();
		new SpielrundeSheet_Validator(getWorkingSpreadsheet()).validateSpieltag(getSpielTag()); // validieren
		// sicher gehen das aktive spielrunde sheet ist activ
		getSheetHelper().setActiveSheet(getXSpreadSheet());
	}

	/**
	 * 4 spielrunden testdaten generieren
	 *
	 * @throws GenerateException
	 */
	public void generate() throws GenerateException {

		anmeldungenSheet.setSpielTag(getSpielTag());
		tielnehmerSheet.setSpielTagNr(getSpielTag());
		spieltagRanglisteSheet.setSpieltagNr(getSpielTag());
		naechsteSpielrundeSheet.setSpielTag(getSpielTag());

		if (getSpielTag().getNr() == 1) {
			meldeListeTestDatenGenerator.testNamenEinfuegen();
		}

		meldeListeTestDatenGenerator.initialAktuellenSpielTagMitAktivenMeldungenFuellen(getSpielTag());

		anmeldungenSheet.generate();
		tielnehmerSheet.generate();

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
				return; // raus wenn nicht erstellt
			}

			// ------------------------------------
			// spiel test ergebnisse einfuegen
			// ------------------------------------
			XSpreadsheet sheet = getXSpreadSheet();
			Position letztePos = letzteErgbnissPosition();

			if (letztePos != null && sheet != null) {
				for (int zeileCntr = ERSTE_DATEN_ZEILE; zeileCntr <= letztePos.getZeile(); zeileCntr++) {
					SheetRunner.testDoCancelTask();

					Position pos = Position.from(ERSTE_SPALTE_ERGEBNISSE, zeileCntr);

					int welchenTeamHatGewonnen = ThreadLocalRandom.current().nextInt(0, 2); // 0,1
					int verliererPunkte = ThreadLocalRandom.current().nextInt(0, 13); // 0 - 12
					// gewinner kann auch weniger als 13 punkte
					int gewinnerPunkte = ThreadLocalRandom.current().nextInt(verliererPunkte + 1, 14); // random rest bis 13
					int valA = (welchenTeamHatGewonnen == 0 ? verliererPunkte : gewinnerPunkte);
					int valB = (welchenTeamHatGewonnen == 0 ? gewinnerPunkte : verliererPunkte);

					NumberCellValue numberCellValue = NumberCellValue.from(sheet, pos, valA);
					getSheetHelper().setNumberValueInCell(numberCellValue);
					getSheetHelper().setNumberValueInCell(numberCellValue.spaltePlusEins().setValue((double) valB));
				}
			}
		}
	}

}
