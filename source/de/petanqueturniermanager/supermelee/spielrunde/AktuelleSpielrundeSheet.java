/**
* Erstellung : 26.03.2018 / Michael Massee
**/
package de.petanqueturniermanager.supermelee.spielrunde;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.model.Meldungen;

public class AktuelleSpielrundeSheet extends AbstractSpielrundeSheet {
	private static final Logger logger = LogManager.getLogger(AktuelleSpielrundeSheet.class);

	public AktuelleSpielrundeSheet(XComponentContext xContext) {
		super(xContext);
	}

	@Override
	protected Logger getLogger() {
		return logger;
	}

	@Override
	protected void doRun() {

		int aktuelleSpielrunde = getKonfigurationSheet().getSpielRunde();
		Meldungen meldungen = this.getMeldeListe().getAktiveMeldungenAktuellenSpielTag();

		if (!canStart(meldungen, aktuelleSpielrunde)) {
			return;
		}
		neueSpielrunde(meldungen, aktuelleSpielrunde);
	}

	public SpielerSpielrundeErgebnisList ergebnisseEinlesen(int spielrunde) throws GenerateException {

		SpielerSpielrundeErgebnisList spielerSpielrundeErgebnisse = new SpielerSpielrundeErgebnisList();
		XSpreadsheet sheet = getSpielRundeSheet(spielrunde);

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
					spielerSpielrundeErgebnisse.add(SpielerSpielrundeErgebnis.from(spielrunde, spielerNr, spielerNrPos,
							ERSTE_SPALTE_ERGEBNISSE, SpielRundeTeam.A));
				}
				spielerNrPos.spaltePlusEins();
			}

			// Team B
			for (int spalteCntr = 1; spalteCntr <= 3; spalteCntr++) {
				int spielerNr = getSheetHelper().getIntFromCell(sheet, spielerNrPos);
				if (spielerNr > 0) {
					spielerZeilevorhanden = true;
					spielerSpielrundeErgebnisse.add(SpielerSpielrundeErgebnis.from(spielrunde, spielerNr, spielerNrPos,
							ERSTE_SPALTE_ERGEBNISSE, SpielRundeTeam.B));
				}
				spielerNrPos.spaltePlusEins();
			}
			spielerNrPos.zeilePlusEins().spalte(ERSTE_SPIELERNR_SPALTE);
		}
		return spielerSpielrundeErgebnisse;
	}

	public int countNumberOfSpielRunden() {
		int anz = 0;
		for (int rdnCntr = 1; rdnCntr < 99; rdnCntr++) {
			if (this.getSheetHelper().findByName(getSheetName(rdnCntr)) != null) {
				anz++;
			} else {
				break;
			}
		}
		return anz;
	}
}
