/*
 * Erstellung : 07.05.2018 / Michael Massee
 **/

package de.petanqueturniermanager.supermelee.spieltagrangliste;

import static com.google.common.base.Preconditions.checkArgument;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.NewTestDatenValidator;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeKonfigurationSheet;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_NeuerSpieltag;
import de.petanqueturniermanager.supermelee.spielrunde.SpielrundeSheet_TestDaten;
import de.petanqueturniermanager.supermelee.spielrunde.SpielrundeSheet_Validator;

/**
 * Generiert komplette Spieltage (Standard: 5) mit Spielrunden und Spieltag-Ranglisten.
 *
 * @author michael
 */
public class SpieltagRanglisteSheet_TestDaten extends SheetRunner {

	/** Anzahl Spieltage, die ohne explizite Angabe erzeugt werden. */
	public static final int STANDARD_ANZAHL_SPIELTAGE = 5;

	private final SuperMeleeKonfigurationSheet konfigurationSheet;
	private final SpielrundeSheet_TestDaten spielrundeSheetTestDaten;
	private final MeldeListeSheet_NeuerSpieltag meldeListeSheetNeuerSpieltag;
	private final int anzahlSpieltage;

	public SpieltagRanglisteSheet_TestDaten(WorkingSpreadsheet workingSpreadsheet) {
		this(workingSpreadsheet, STANDARD_ANZAHL_SPIELTAGE);
	}

	/**
	 * @param anzahlSpieltage Anzahl der zu erzeugenden Spieltage, mindestens 1
	 */
	public SpieltagRanglisteSheet_TestDaten(WorkingSpreadsheet workingSpreadsheet, int anzahlSpieltage) {
		super(workingSpreadsheet, TurnierSystem.SUPERMELEE);
		checkArgument(anzahlSpieltage >= 1, "anzahlSpieltage muss mindestens 1 sein: %s", anzahlSpieltage);
		this.anzahlSpieltage = anzahlSpieltage;
		konfigurationSheet = new SuperMeleeKonfigurationSheet(workingSpreadsheet);
		spielrundeSheetTestDaten = new SpielrundeSheet_TestDaten(workingSpreadsheet);
		meldeListeSheetNeuerSpieltag = new MeldeListeSheet_NeuerSpieltag(workingSpreadsheet);
	}

	@Override
	protected SuperMeleeKonfigurationSheet getKonfigurationSheet() {
		return konfigurationSheet;
	}

	@Override
	protected void doRun() throws GenerateException {
		if (!NewTestDatenValidator.from(getWorkingSpreadsheet(), getSheetHelper(), TurnierSystem.SUPERMELEE)
				.prefix(getLogPrefix()).validateFuerTestdaten()) {
			return;
		}

		getSheetHelper().removeAllSheetsExclude(new String[] { SheetNamen.supermeleeTeams() });

		for (int spieltagCntr = 1; spieltagCntr <= anzahlSpieltage; spieltagCntr++) {
			SheetRunner.testDoCancelTask();
			erzeugeSpieltag(SpielTagNr.from(spieltagCntr));
		}
	}

	private void erzeugeSpieltag(SpielTagNr spieltagNr) throws GenerateException {
		if (spieltagNr.getNr() > 1) {
			meldeListeSheetNeuerSpieltag.setSpielTag(spieltagNr);
			meldeListeSheetNeuerSpieltag.naechsteSpieltag();
		}
		spielrundeSheetTestDaten.setSpielTag(spieltagNr);
		getKonfigurationSheet().setAktiveSpieltag(spieltagNr);

		spielrundeSheetTestDaten.generate();

		new SpielrundeSheet_Validator(getWorkingSpreadsheet()).validateSpieltag(spieltagNr);
	}

}
