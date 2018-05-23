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
		int aktuelleSpielrunde = getKonfigurationSheet().getAktiveSpielRunde();
		this.setSpielRundeNr(SpielRundeNr.from(aktuelleSpielrunde));
		this.getMeldeListe().upDateSheet();
		Meldungen meldungen = this.getMeldeListe().getAktiveMeldungen();

		if (!canStart(meldungen)) {
			return;
		}

		// aktuelle vorhanden ?
		int neueSpielrunde = aktuelleSpielrunde;
		if (getSheetHelper().findByName(getSheetName(getSpielTag(), getSpielRundeNr())) != null) {
			neueSpielrunde++;
		}

		gespieltenRundenEinlesen(meldungen, neueSpielrunde - 1, this.getKonfigurationSheet().getSpielRundeNeuAuslosenAb());
		neueSpielrunde(meldungen, neueSpielrunde);
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
			XSpreadsheet sheet = getSpielRundeSheet(getSpielTag(), SpielRundeNr.from(spielrunde));
			if (sheet == null) {
				continue;
			}
			Position pospielerNr = Position.from(ERSTE_SPIELERNR_SPALTE, ERSTE_DATEN_ZEILE);

			boolean zeileIstLeer = false;
			while (!zeileIstLeer) {
				for (int teamCntr = 1; teamCntr <= 2; teamCntr++) {
					Team team = new Team(1);
					for (int spielerCntr = 1; spielerCntr <= 3; spielerCntr++) {
						pospielerNr.spalte(ERSTE_SPIELERNR_SPALTE + ((teamCntr - 1) * 3) + spielerCntr - 1);
						int spielerNr = getSheetHelper().getIntFromCell(sheet, pospielerNr);
						if (spielerNr > -1) {
							// team verwenden um Spieler gegenseitig ein zu tragen
							Spieler spieler = meldungen.findSpielerByNr(spielerNr);
							if (spieler != null) { // ist dann der fall wenn der spieler Ausgestiegen
								try {
									team.addSpielerWennNichtVorhanden(spieler);
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
