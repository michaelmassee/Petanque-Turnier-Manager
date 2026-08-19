/*
 * Erstellung : 24.03.2018 / Michael Massee
 **/

package de.petanqueturniermanager.supermelee.meldeliste;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeKonstanten;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.NewTestDatenValidator;
import de.petanqueturniermanager.helper.TestnamenLoader;
import de.petanqueturniermanager.helper.TestnamenLoader.SpielerTestname;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.random.RandomSource;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.model.Spieler;
import de.petanqueturniermanager.model.SpielerMeldungen;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeKonfigurationSheet;

public class MeldeListeSheet_TestDaten extends SheetRunner implements ISheet {

	public static final int ANZ_TESTNAMEN = 100;
	/** Garantierte Mindestanzahl aktiver Spieler pro Spieltag/Runde. */
	private static final int MIN_ANZ_AKTIVE_SPIELER = 30;
	/**
	 * Obergrenze (exklusiv) fuer das Aktivierungs-Los der Initialbefuellung. Ein Treffer
	 * ({@code los < AKTIVIERUNGS_SCHWELLE}) aktiviert den Spieler. Bei Los-Bereich {@code [1,5)}
	 * ergibt der Wert 3 eine Trefferquote von ~50 % (Lose 1 und 2 von 1..4) und damit im Schnitt
	 * deutlich mehr als {@link #MIN_ANZ_AKTIVE_SPIELER} Aktive.
	 */
	private static final int AKTIVIERUNGS_SCHWELLE = 3;

	private final SuperMeleeKonfigurationSheet konfigurationSheet;
	private final MeldeListeSheet_New meldeListe;
	private final TestnamenLoader testnamenLoader;

	public MeldeListeSheet_TestDaten(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.SUPERMELEE);
		konfigurationSheet = new SuperMeleeKonfigurationSheet(workingSpreadsheet);
		meldeListe = new MeldeListeSheet_New(workingSpreadsheet);
		testnamenLoader = new TestnamenLoader();
	}

	@Override
	protected SuperMeleeKonfigurationSheet getKonfigurationSheet() {
		return konfigurationSheet;
	}

	/** Öffentlicher Einstiegspunkt für {@link de.petanqueturniermanager.beispielturnier.BeispielturnierRegistrierung}. */
	public void generate() throws GenerateException {
		doRun();
	}

	@Override
	protected void doRun() throws GenerateException {

		if (!NewTestDatenValidator.from(getWorkingSpreadsheet(), getSheetHelper(), TurnierSystem.SUPERMELEE)
				.prefix(getLogPrefix()).validate()) {
			return;
		}

		// clean up first
		getSheetHelper().removeAllSheetsExclude(new String[] { SheetNamen.supermeleeTeams() });
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

			int randomNum = RandomSource.nextInt(1, 5);
			int spielerZeile = meldeListe.getSpielerZeileNr(spieler.getNr());
			numVal.zeile(spielerZeile);
			if (randomNum == 2) {
				getSheetHelper().setNumberValueInCell(numVal.setValue((double) randomNum));
			} else {
				getSheetHelper().setNumberValueInCell(numVal.setValue((double) 1));
			}
		}

		aktiveMeldungenAuffuellenAufMindestanzahl(numVal);
	}

	/**
	 * Aktiviert so lange zufaellig ausgewaehlte inaktive Spieler, bis mindestens
	 * {@link #MIN_ANZ_AKTIVE_SPIELER} aktive Meldungen vorliegen. Es werden nur so viele Spieler
	 * ergaenzt, wie zum Erreichen der Grenze noetig sind (kein Ueber-Auffuellen).
	 */
	private void aktiveMeldungenAuffuellenAufMindestanzahl(NumberCellValue numVal) throws GenerateException {
		int fehlende = MIN_ANZ_AKTIVE_SPIELER - meldeListe.getAktiveMeldungen().size();
		if (fehlende <= 0) {
			return;
		}
		for (Spieler spieler : meldeListe.getInAktiveMeldungen().shuffle().getSpielerList()) {
			SheetRunner.testDoCancelTask();
			int spielerZeile = meldeListe.getSpielerZeileNr(spieler.getNr());
			numVal.zeile(spielerZeile);
			getSheetHelper().setNumberValueInCell(numVal.setValue((double) 1));
			if (--fehlende <= 0) {
				break;
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

			int randomNum = RandomSource.nextInt(1, 5);
			numVal.zeile(zeileCnt);
			if (randomNum < AKTIVIERUNGS_SCHWELLE) {
				getSheetHelper().setNumberValueInCell(numVal.setValue((double) 1));
			}
		}

		aktiveMeldungenAuffuellenAufMindestanzahl(numVal);
	}

	/**
	 * immer 1 spieltag
	 *
	 * @throws GenerateException
	 */

	public void testNamenEinfuegen() throws GenerateException {
		meldeListe.setSpielTag(SpielTagNr.from(1));
		// Meldeliste neu anlegen falls sie nicht existiert (z.B. nach removeAllSheetsExclude)
		if (meldeListe.getXSpreadSheet() == null) {
			meldeListe.createMeldelisteWithParams(konfigurationSheet.getSuperMeleeMode());
		}
		XSpreadsheet meldelisteSheet = meldeListe.getXSpreadSheet();
		getSheetHelper().setActiveSheet(meldelisteSheet);

		List<SpielerTestname> testNamen = testnamenLoader.listeMitSpielerTestNamen(ANZ_TESTNAMEN);

		int vornameSpalte = meldeListe.getMeldungenSpalte().getErsteMeldungNameSpalte();
		int nachnameSpalte = meldeListe.getMeldungenSpalte().getLetzteMeldungNameSpalte();
		Position posVorname = Position.from(vornameSpalte, MeldeListeKonstanten.ERSTE_DATEN_ZEILE - 1);
		Position posNachname = Position.from(nachnameSpalte, MeldeListeKonstanten.ERSTE_DATEN_ZEILE - 1);
		Position posSpielerNr = Position.from(MeldeListeKonstanten.SPIELER_NR_SPALTE,
				MeldeListeKonstanten.ERSTE_DATEN_ZEILE - 1);
		NumberCellValue spielrNr = NumberCellValue.from(meldelisteSheet, posSpielerNr);
		StringCellValue nameCelVal = StringCellValue.from(meldelisteSheet, posVorname);

		for (int spielerCntr = 0; spielerCntr < testNamen.size(); spielerCntr++) {
			SheetRunner.testDoCancelTask();
			posVorname.zeilePlusEins();
			posNachname.zeile(posVorname.getZeile());
			String textFromCell = getSheetHelper().getTextFromCell(meldelisteSheet, posVorname);

			if (StringUtils.isNotEmpty(textFromCell)) {
				throw new GenerateException(I18n.get("error.testdaten.daten.vorhanden"));
			}

			SpielerTestname tn = testNamen.get(spielerCntr);
			getSheetHelper().setStringValueInCell(nameCelVal.setPos(posVorname).setValue(tn.vorname()));
			if (vornameSpalte != nachnameSpalte) {
				getSheetHelper().setStringValueInCell(nameCelVal.setPos(posNachname).setValue(tn.nachname()));
			}

			spielrNr.zeile(posVorname.getZeile());
			int randomNum = RandomSource.nextInt(0, 3);
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
