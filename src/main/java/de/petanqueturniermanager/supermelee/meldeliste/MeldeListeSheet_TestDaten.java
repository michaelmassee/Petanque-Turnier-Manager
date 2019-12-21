/**
* Erstellung : 24.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee.meldeliste;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationKonstanten;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeKonstanten;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.model.SpielerMeldungen;
import de.petanqueturniermanager.model.Spieler;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.SupermeleeTeamPaarungenSheet;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeSheet;

public class MeldeListeSheet_TestDaten extends SuperMeleeSheet {
	/**
	 *
	 */
	private static final int MIN_ANZ_SPIELER = 10;

	private static final Logger logger = LogManager.getLogger(MeldeListeSheet_TestDaten.class);

	private final AbstractSupermeleeMeldeListeSheet meldeListe;

	public MeldeListeSheet_TestDaten(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
		meldeListe = new MeldeListeSheet_New(workingSpreadsheet);
	}

	@Override
	public Logger getLogger() {
		return MeldeListeSheet_TestDaten.logger;
	}

	@Override
	protected void doRun() throws GenerateException {
		// clean up first
		getSheetHelper().removeAllSheetsExclude(new String[] { IKonfigurationKonstanten.SHEETNAME, SupermeleeTeamPaarungenSheet.SHEETNAME });
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
		NumberCellValue numVal = NumberCellValue.from(meldeListe.getXSpreadSheet(), Position.from(aktuelleSpieltagSpalte, MeldeListeKonstanten.ERSTE_DATEN_ZEILE));

		for (Spieler spieler : aktiveUndAusgesetztMeldungenAktuellenSpielTag.spieler()) {
			SheetRunner.testDoCancelTask();

			int randomNum = ThreadLocalRandom.current().nextInt(1, 5);
			int spielerZeile = meldeListe.getSpielerZeileNr(spieler.getNr());
			numVal.zeile(spielerZeile);
			if (randomNum == 2) {
				getSheetHelper().setValInCell(numVal.setValue((double) randomNum));
			} else {
				getSheetHelper().setValInCell(numVal.setValue((double) 1));
			}
		}

		if (meldeListe.getAktiveMeldungen().size() < MIN_ANZ_SPIELER) {
			// zu wenig spieler, einfach 10 dazu
			int cntr = MIN_ANZ_SPIELER;
			Iterable<Spieler> spielerList = meldeListe.getInAktiveMeldungen().shuffle().getSpielerList();
			for (Spieler spieler : spielerList) {
				int spielerZeile = meldeListe.getSpielerZeileNr(spieler.getNr());
				numVal.zeile(spielerZeile);
				getSheetHelper().setValInCell(numVal.setValue((double) 1));
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
		NumberCellValue numVal = NumberCellValue.from(meldeListe.getXSpreadSheet(), Position.from(aktuelleSpieltagSpalte, MeldeListeKonstanten.ERSTE_DATEN_ZEILE));

		int letzteDatenZeile = meldeListe.letzteDatenZeile();

		for (int zeileCnt = MeldeListeKonstanten.ERSTE_DATEN_ZEILE; zeileCnt <= letzteDatenZeile; zeileCnt++) {
			SheetRunner.testDoCancelTask();

			int randomNum = ThreadLocalRandom.current().nextInt(1, 5);
			numVal.zeile(zeileCnt);
			if (randomNum == 1) {
				getSheetHelper().setValInCell(numVal.setValue((double) 1));
			}
		}
	}

	/**
	 * immer 1 speiltag
	 *
	 * @throws GenerateException
	 */

	public void testNamenEinfuegen() throws GenerateException {
		meldeListe.setSpielTag(SpielTagNr.from(1));
		XSpreadsheet meldelisteSheet = meldeListe.getXSpreadSheet();
		getSheetHelper().setActiveSheet(meldelisteSheet);

		List<String> testNamen = listeMitTestNamen();

		Position posSpielerName = Position.from(meldeListe.getSpielerNameSpalte(), MeldeListeKonstanten.ERSTE_DATEN_ZEILE - 1);
		Position posSpielerNr = Position.from(MeldeListeKonstanten.SPIELER_NR_SPALTE, MeldeListeKonstanten.ERSTE_DATEN_ZEILE - 1);
		NumberCellValue spielrNr = NumberCellValue.from(meldelisteSheet, posSpielerNr);
		StringCellValue spielrNamen = StringCellValue.from(meldelisteSheet, posSpielerName);

		for (int spielerCntr = 0; spielerCntr < testNamen.size(); spielerCntr++) {
			SheetRunner.testDoCancelTask();
			posSpielerName.zeilePlusEins();
			String textFromCell = getSheetHelper().getTextFromCell(meldelisteSheet, posSpielerName);

			if (StringUtils.isNotEmpty(textFromCell)) {
				throw new GenerateException("Fehler beim füllen von Testdaten in Meldesheet. Es dürfen keine Daten vorhanden sein");
			}

			getSheetHelper().setStringValueInCell(spielrNamen.setPos(posSpielerName).setValue(testNamen.get(spielerCntr)));

			spielrNr.zeile(posSpielerName.getZeile());
			int randomNum = ThreadLocalRandom.current().nextInt(0, 3);
			if (randomNum == 1) { // nur die einser eintragen
				// zum test spielrnr vorgeben, mix in nr erreichen
				getSheetHelper().setValInCell(spielrNr.setValue((double) spielerCntr + 1));
			} else {
				// andere Nummer leer
				getSheetHelper().setStringValueInCell(StringCellValue.from(spielrNr).setValue(""));
			}
		}

		meldeListe.upDateSheet();
	}

	// Testdaten Generator
	// http://migano.de/testdaten.php

	List<String> listeMitTestNamen() {
		List<String> testNamen = new ArrayList<>();

		testNamen.add("Wegner, Silas");
		testNamen.add("Wright, Silvia");
		testNamen.add("Karrer, Milan");
		testNamen.add("Böhme, Bjarne");
		testNamen.add("Cummings, Kay");
		testNamen.add("Trost, Simon");
		testNamen.add("Adrian, Isabella");
		testNamen.add("Gruber, Chantall");
		testNamen.add("Erpel, Leander");
		testNamen.add("Breunig, Lili");
		testNamen.add("Schulte, Catharina");
		testNamen.add("Lau, Henrik");
		testNamen.add("Seel, Dominic");
		testNamen.add("Edwards, Victor");
		testNamen.add("Hoffmann, Arne");
		testNamen.add("Morgenroth, Waldtraut");
		testNamen.add("Töpfer, Lilian");
		testNamen.add("Reiter, Enno");
		testNamen.add("Schaeffer, Thorsten");
		testNamen.add("Kübler, Matis");
		testNamen.add("Barber, Arne");
		testNamen.add("Sinn, Lya");
		testNamen.add("Schreiber, Justus");
		testNamen.add("Weaver, Erwin");
		testNamen.add("Crawford, Lorena");
		testNamen.add("Malone, Thorben");
		testNamen.add("Hagedorn, Rosemarie");
		testNamen.add("Gäbler, Katharina");
		testNamen.add("Schmidt, Peter");
		testNamen.add("Schubert, Linus");
		testNamen.add("Both, Dominik");
		testNamen.add("Derksen, Cedric");
		testNamen.add("Wieczorek, Kristine");
		testNamen.add("Cooper, Hartmut");
		testNamen.add("Lehmann, Ralf");
		testNamen.add("Gerth, Natalie");
		testNamen.add("Schüller, Joshua");
		testNamen.add("Schreiber, Silas");
		testNamen.add("Axmann, Jamie");
		testNamen.add("Lerch, Cedrik");
		testNamen.add("Wiener, Lennart");
		testNamen.add("Heymann, Anthony");
		testNamen.add("Reuter, Denise");
		testNamen.add("Tietz, Felix");
		testNamen.add("Hertwig, Louise");
		testNamen.add("Dahms, Carlotta");
		testNamen.add("Penner, Elias");
		testNamen.add("Moody, Lieselotte");
		testNamen.add("Clarke, Paula");
		testNamen.add("Sacher, Kurt");
		testNamen.add("Axmann, Jacqueline");
		testNamen.add("Wood, Kilian");
		testNamen.add("Gerhardt, Erna");
		testNamen.add("Goodman, Luc");
		testNamen.add("Wulf, Anette");
		testNamen.add("Bacher, Anneliese");
		testNamen.add("Bridges, Anneliese");
		testNamen.add("Buchner, Edith");
		testNamen.add("Penner, Thomas");
		testNamen.add("Schütz, John");
		testNamen.add("Steuermann, Claudia");
		testNamen.add("Senioren, Piet");
		testNamen.add("Schaub, Timo");
		testNamen.add("Geis, Kira");
		testNamen.add("Bruckner, Karina");
		testNamen.add("Hughes, Astrid");
		testNamen.add("Brehmer, Tristan");
		testNamen.add("Jacobi, Thorsten");
		testNamen.add("Förster, Chris");
		testNamen.add("Friedel, Selina");
		testNamen.add("Wienecke, Marianne");
		testNamen.add("Gehrmann, Michelle");
		testNamen.add("Fisher, Helena");
		testNamen.add("Normann, Petra");
		testNamen.add("Siemon, Henrik");
		testNamen.add("Pauli, Swenja");
		testNamen.add("Langhans, Leonie");
		testNamen.add("Yilmaz, Gabriele");
		testNamen.add("Deckert, Volker");
		testNamen.add("Love, Bruno");
		testNamen.add("Ruppert, Susanne");
		testNamen.add("Scheerer, Mattis");
		testNamen.add("Obermaier, Swen");
		testNamen.add("Kehl, Lennart");
		testNamen.add("Fassbender, Anouk");
		testNamen.add("Zoeller, Tara");
		testNamen.add("Häger, Stina");
		testNamen.add("Powell, Rike");
		testNamen.add("Wilde, Lewin");
		testNamen.add("Hoff, Sophia");
		testNamen.add("Jakobs, Walter");
		testNamen.add("Tag, Madita");
		testNamen.add("Rhodes, Lya");
		testNamen.add("Maass, Wilhelm");
		testNamen.add("Seeber, Rudolph");
		testNamen.add("Otterbach, Malin");
		testNamen.add("Hüttner, Margarethe");
		testNamen.add("Struck, Marlon");
		testNamen.add("Cross, Stephan");
		testNamen.add("Schultheiss, Merle");

		return testNamen;
	}

}
