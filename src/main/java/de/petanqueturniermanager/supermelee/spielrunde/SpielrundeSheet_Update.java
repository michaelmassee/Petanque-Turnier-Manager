/**
 * Erstellung : 26.03.2018 / Michael Massee
 **/
package de.petanqueturniermanager.supermelee.spielrunde;

import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.model.SpielerMeldungen;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.SpielTagNr;

public class SpielrundeSheet_Update extends AbstractSpielrundeSheet {
	private static final Logger logger = LogManager.getLogger(SpielrundeSheet_Update.class);

	public SpielrundeSheet_Update(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
	}

	@Override
	public Logger getLogger() {
		return logger;
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
			new SpielrundeSheet_Validator(getWorkingSpreadsheet()).validateSpieltag(getSpielTag()); // validieren
			// sicher gehen das aktive spielrunde sheet ist activ
			getSheetHelper().setActiveSheet(getXSpreadSheet());
		}
	}

	public SpielerSpielrundeErgebnisList ergebnisseEinlesen() throws GenerateException {

		SpielerSpielrundeErgebnisList spielerSpielrundeErgebnisse = new SpielerSpielrundeErgebnisList();
		XSpreadsheet sheet = getXSpreadSheet();

		if (sheet == null) {
			return spielerSpielrundeErgebnisse;
		}

		Position spielerNrPos = Position.from(ERSTE_SPIELERNR_SPALTE, ERSTE_DATEN_ZEILE);

		int tCntr = 0;
		boolean spielerZeilevorhanden = true;
		while (spielerZeilevorhanden && tCntr++ < 999) {
			spielerZeilevorhanden = false;

			// Team A
			for (int spalteCntr = 1; spalteCntr <= 3; spalteCntr++) {
				int spielerNr = getSheetHelper().getIntFromCell(sheet, spielerNrPos);
				if (spielerNr > 0) {
					spielerZeilevorhanden = true;
					spielerSpielrundeErgebnisse.add(SpielerSpielrundeErgebnis.from(getSpielRundeNr(), spielerNr,
							spielerNrPos, ERSTE_SPALTE_ERGEBNISSE, SpielRundeTeam.A));
				}
				spielerNrPos.spaltePlusEins();
			}

			// Team B
			for (int spalteCntr = 1; spalteCntr <= 3; spalteCntr++) {
				int spielerNr = getSheetHelper().getIntFromCell(sheet, spielerNrPos);
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

	/**
	 * anzahl spierunden sheets zaehlen mit lücken
	 * 
	 * @param spieltag
	 * @return
	 * @throws GenerateException
	 */
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
