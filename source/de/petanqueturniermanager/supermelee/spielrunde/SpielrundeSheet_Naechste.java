/**
* Erstellung : 26.03.2018 / Michael Massee
**/
package de.petanqueturniermanager.supermelee.spielrunde;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.exception.AlgorithmenException;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.model.Meldungen;
import de.petanqueturniermanager.model.Spieler;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.supermelee.SpielRundeNr;

public class SpielrundeSheet_Naechste extends AbstractSpielrundeSheet {
	private static final Logger logger = LogManager.getLogger(SpielrundeSheet_Naechste.class);

	public SpielrundeSheet_Naechste(XComponentContext xContext) {
		super(xContext);
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

	@Override
	protected void doRun() throws GenerateException {
		setSpielTag(getKonfigurationSheet().getAktiveSpieltag());
		naechsteSpielrundeEinfuegen();
	}

	public void naechsteSpielrundeEinfuegen() throws GenerateException {
		SpielRundeNr aktuelleSpielrunde = getKonfigurationSheet().getAktiveSpielRunde();
		setSpielRundeNr(aktuelleSpielrunde);
		getMeldeListe().upDateSheet();
		Meldungen meldungen = getMeldeListe().getAktiveMeldungen();

		if (!canStart(meldungen)) {
			return;
		}

		// aktuelle vorhanden ?
		int neueSpielrunde = aktuelleSpielrunde.getNr();
		if (getSheetHelper().findByName(getSheetName(getSpielTag(), getSpielRundeNr())) != null) {
			neueSpielrunde++;
		}

		gespieltenRundenEinlesen(meldungen, neueSpielrunde - 1, getKonfigurationSheet().getSpielRundeNeuAuslosenAb());
		neueSpielrunde(meldungen, SpielRundeNr.from(neueSpielrunde));
	}

	/**
	 * in der meldungen liste alle spieler die liste warimTeammit fuellen
	 *
	 * @param meldungen
	 * @param bisSpielrunde bis zu diese spielrunde
	 * @param abSpielrunde ab diese spielrunde = default = 1
	 * @throws GenerateException
	 */

	void gespieltenRundenEinlesen(Meldungen meldungen, int bisSpielrunde, int abSpielrunde) throws GenerateException {

		int spielrunde = 1;

		if (abSpielrunde > 1) {
			spielrunde = abSpielrunde;
		}

		for (; spielrunde <= bisSpielrunde; spielrunde++) {
			SheetRunner.testDoCancelTask();

			// XSpreadsheet sheet = getSpielRundeSheet(getSpielTag(), SpielRundeNr.from(spielrunde));
			XSpreadsheet sheet = getSheetHelper().findByName(getSheetName(getSpielTag(), SpielRundeNr.from(spielrunde)));

			if (sheet == null) {
				continue;
			}
			Position pospielerNr = Position.from(ERSTE_SPIELERNR_SPALTE, ERSTE_DATEN_ZEILE);

			boolean zeileIstLeer = false;
			int maxcntr = 999; // sollte nicht vorkommen, endlos schleife vermeiden in fehlerfall
			while (!zeileIstLeer && maxcntr > 0) {
				maxcntr--;
				for (int teamCntr = 1; teamCntr <= 2; teamCntr++) { // Team A & B
					Team team = new Team(1); // dummy team verwenden um Spieler gegenseitig ein zu tragen
					for (int spielerCntr = 1; spielerCntr <= 3; spielerCntr++) {
						pospielerNr.spalte(ERSTE_SPIELERNR_SPALTE + ((teamCntr - 1) * 3) + spielerCntr - 1);
						int spielerNr = getSheetHelper().getIntFromCell(sheet, pospielerNr); // Spieler aus Rundeliste
						if (spielerNr > 0) {
							Spieler spieler = meldungen.findSpielerByNr(spielerNr);
							if (spieler != null) { // ist dann der fall wenn der spieler Ausgestiegen ist
								try {
									team.addSpielerWennNichtVorhanden(spieler); // im gleichen Team = wird gegenseitig eingetragen
								} catch (AlgorithmenException e) {
									logger.error(e.getMessage(), e);
									return;
								}
							}
						}
					}
				}
				pospielerNr.zeilePlusEins().spalte(ERSTE_SPIELERNR_SPALTE);
				if (getSheetHelper().getIntFromCell(sheet, pospielerNr) == -1) {
					zeileIstLeer = true;
				}
			}
		}
	}

}
