/**
* Erstellung : 26.03.2018 / Michael Massee
**/
package de.petanqueturniermanager.supermelee.spielrunde;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.uno.XComponentContext;

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

		int aktuelleSpielrunde = getPropertiesSpalte().getSpielRunde();
		Meldungen meldungen = this.getMeldeListe().getAktiveMeldungenAktuellenSpielTag();

		if (!canStart(meldungen, aktuelleSpielrunde)) {
			return;
		}
		neueSpielrunde(meldungen, aktuelleSpielrunde);
	}

	public List<SpielerSpielrundeErgebnis> ergebnisseEinlesen(int spielrunde) {
		List<SpielerSpielrundeErgebnis> spielerSpielrundeErgebnisse = new ArrayList<>();
		XSpreadsheet sheet = getSpielRundeSheet(spielrunde);

		if (sheet == null) {
			return spielerSpielrundeErgebnisse;
		}

		Position spielerNrPos = Position.from(ERSTE_SPIELERNR_SPALTE, ERSTE_DATEN_ZEILE);

		int maxCntr = 999;
		while (maxCntr-- > 0) {
			int spielerNrErsteSpalte = getSheetHelper().getIntFromCell(sheet, spielerNrPos);
			if (spielerNrErsteSpalte < 1) {
				break;
			}
			// Team A
			for (int spalteCntr = 1; spalteCntr <= 3; spalteCntr++) {
				int spielerNr = getSheetHelper().getIntFromCell(sheet, spielerNrPos);
				if (spielerNr > 0) {
					spielerSpielrundeErgebnisse.add(SpielerSpielrundeErgebnis.from(spielrunde, spielerNr, spielerNrPos,
							ERSTE_SPALTE_ERGEBNISSE, SpielRundeTeam.A));
				}
				spielerNrPos.spaltePlusEins();
			}

			// Team B
			for (int spalteCntr = 1; spalteCntr <= 3; spalteCntr++) {
				int spielerNr = getSheetHelper().getIntFromCell(sheet, spielerNrPos);
				if (spielerNr > 0) {
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
