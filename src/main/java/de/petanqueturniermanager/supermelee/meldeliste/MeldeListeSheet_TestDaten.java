/**
 * Erstellung : 24.03.2018 / Michael Massee
 **/

package de.petanqueturniermanager.supermelee.meldeliste;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeKonstanten;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.NewTestDatenValidator;
import de.petanqueturniermanager.helper.TestnamenLoader;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.model.Spieler;
import de.petanqueturniermanager.model.SpielerMeldungen;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.SupermeleeTeamPaarungenSheet;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeSheet;

public class MeldeListeSheet_TestDaten extends SuperMeleeSheet implements ISheet {

	public static final int ANZ_TESTNAMEN = 100;
	private static final int MIN_ANZ_SPIELER = 10;
	private static final Logger logger = LogManager.getLogger(MeldeListeSheet_TestDaten.class);

	private final AbstractSupermeleeMeldeListeSheet meldeListe;
	private final TestnamenLoader testnamenLoader;

	public MeldeListeSheet_TestDaten(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
		meldeListe = new MeldeListeSheet_New(workingSpreadsheet);
		testnamenLoader = new TestnamenLoader();
	}

	@Override
	public Logger getLogger() {
		return MeldeListeSheet_TestDaten.logger;
	}

	@Override
	protected void doRun() throws GenerateException {

		if (!NewTestDatenValidator.from(getWorkingSpreadsheet(), getSheetHelper(), TurnierSystem.SUPERMELEE)
				.prefix(getLogPrefix()).validate()) {
			return;
		}

		// clean up first
		getSheetHelper().removeAllSheetsExclude(new String[] { SupermeleeTeamPaarungenSheet.SHEETNAME });
		meldeListe.setSpielTag(SpielTagNr.from(1));
		meldeListe.setAktiveSpieltag(SpielTagNr.from(1));
		meldeListe.setAktiveSpielRunde(SpielRundeNr.from(1));

		testNamenEinfuegen();
		initialAktuellenSpielTagMitAktivenMeldungenFuellen(meldeListe.getSpielTag());

	}

	public void spielerAufAktivInaktivMischen(SpielTagNr spielTagNr) throws GenerateException {
		meldeListe.setSpielTag(spielTagNr);

		SpielerMeldungen aktiveUndAusgesetztMeldungenAktuellenSpielTag = meldeListe.getAktiveUndAusgesetztMeldungen();

		int aktuelleSpieltagSpalte = meldeListe.aktuelleSpieltagSpalte();
		NumberCellValue numVal = NumberCellValue.from(meldeListe.getXSpreadSheet(),
				Position.from(aktuelleSpieltagSpalte, MeldeListeKonstanten.ERSTE_DATEN_ZEILE));

		for (Spieler spieler : aktiveUndAusgesetztMeldungenAktuellenSpielTag.spieler()) {
			SheetRunner.testDoCancelTask();

			int randomNum = ThreadLocalRandom.current().nextInt(1, 5);
			int spielerZeile = meldeListe.getSpielerZeileNr(spieler.getNr());
			numVal.zeile(spielerZeile);
			if (randomNum == 2) {
				getSheetHelper().setNumberValueInCell(numVal.setValue((double) randomNum));
			} else {
				getSheetHelper().setNumberValueInCell(numVal.setValue((double) 1));
			}
		}

		if (meldeListe.getAktiveMeldungen().size() < MIN_ANZ_SPIELER) {
			// zu wenig spieler, einfach 10 dazu
			int cntr = MIN_ANZ_SPIELER;
			Iterable<Spieler> spielerList = meldeListe.getInAktiveMeldungen().shuffle().getSpielerList();
			for (Spieler spieler : spielerList) {
				int spielerZeile = meldeListe.getSpielerZeileNr(spieler.getNr());
				numVal.zeile(spielerZeile);
				getSheetHelper().setNumberValueInCell(numVal.setValue((double) 1));
				cntr--;
				if (cntr < 0) {
					break;
				}
			}
		}
	}

	public void initialAktuellenSpielTagMitAktivenMeldungenFuellen(SpielTagNr spielTagNr) throws GenerateException {
		meldeListe.setSpielTag(spielTagNr);

		int aktuelleSpieltagSpalte = meldeListe.aktuelleSpieltagSpalte();
		NumberCellValue numVal = NumberCellValue.from(meldeListe.getXSpreadSheet(),
				Position.from(aktuelleSpieltagSpalte, MeldeListeKonstanten.ERSTE_DATEN_ZEILE));

		int letzteDatenZeile = meldeListe.getLetzteMitDatenZeileInSpielerNrSpalte();

		for (int zeileCnt = MeldeListeKonstanten.ERSTE_DATEN_ZEILE; zeileCnt <= letzteDatenZeile; zeileCnt++) {
			SheetRunner.testDoCancelTask();

			int randomNum = ThreadLocalRandom.current().nextInt(1, 5);
			numVal.zeile(zeileCnt);
			if (randomNum == 1) {
				getSheetHelper().setNumberValueInCell(numVal.setValue((double) 1));
			}
		}
	}

	/**
	 * immer 1 spieltag
	 *
	 * @throws GenerateException
	 */

	public void testNamenEinfuegen() throws GenerateException {
		meldeListe.setSpielTag(SpielTagNr.from(1));
		XSpreadsheet meldelisteSheet = meldeListe.getXSpreadSheet();
		getSheetHelper().setActiveSheet(meldelisteSheet);

		List<String> testNamen = testnamenLoader.listeMitTestNamen(ANZ_TESTNAMEN);

		Position posSpielerName = Position.from(meldeListe.getSpielerNameSpalte(),
				MeldeListeKonstanten.ERSTE_DATEN_ZEILE - 1);
		Position posSpielerNr = Position.from(MeldeListeKonstanten.SPIELER_NR_SPALTE,
				MeldeListeKonstanten.ERSTE_DATEN_ZEILE - 1);
		NumberCellValue spielrNr = NumberCellValue.from(meldelisteSheet, posSpielerNr);
		StringCellValue spielrNamen = StringCellValue.from(meldelisteSheet, posSpielerName);

		for (int spielerCntr = 0; spielerCntr < testNamen.size(); spielerCntr++) {
			SheetRunner.testDoCancelTask();
			posSpielerName.zeilePlusEins();
			String textFromCell = getSheetHelper().getTextFromCell(meldelisteSheet, posSpielerName);

			if (StringUtils.isNotEmpty(textFromCell)) {
				throw new GenerateException(
						"Fehler beim füllen von Testdaten in Meldesheet. Es dürfen keine Daten vorhanden sein");
			}

			getSheetHelper()
					.setStringValueInCell(spielrNamen.setPos(posSpielerName).setValue(testNamen.get(spielerCntr)));

			spielrNr.zeile(posSpielerName.getZeile());
			int randomNum = ThreadLocalRandom.current().nextInt(0, 3);
			if (randomNum == 1) { // nur die einser eintragen
				// zum test spielrnr vorgeben, mix in nr erreichen
				getSheetHelper().setNumberValueInCell(spielrNr.setValue((double) spielerCntr + 1));
			} else {
				// andere Nummer leer
				getSheetHelper().setStringValueInCell(StringCellValue.from(spielrNr).setValue(""));
			}
		}

		meldeListe.upDateSheet();
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return meldeListe.getXSpreadSheet();
	}

	@Override
	public TurnierSheet getTurnierSheet() throws GenerateException {
		return meldeListe.getTurnierSheet();
	}

}
