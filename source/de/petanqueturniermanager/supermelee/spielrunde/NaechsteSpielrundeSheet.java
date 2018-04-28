/**
* Erstellung : 26.03.2018 / Michael Massee
**/
package de.petanqueturniermanager.supermelee.spielrunde;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.exception.AlgorithmenException;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.model.Meldungen;
import de.petanqueturniermanager.model.Spieler;
import de.petanqueturniermanager.model.Team;

public class NaechsteSpielrundeSheet extends AbstractSpielrundeSheet {
	private static final Logger logger = LogManager.getLogger(NaechsteSpielrundeSheet.class);

	public NaechsteSpielrundeSheet(XComponentContext xContext) {
		super(xContext);
	}

	@Override
	protected Logger getLogger() {
		return logger;
	}

	@Override
	protected void doRun() {
		naechsteSpielrundeEinfuegen();
	}

	public void naechsteSpielrundeEinfuegen() {
		int aktuelleSpielrunde = getPropertiesSpalte().getSpielRunde();
		Meldungen meldungen = this.getMeldeListe().getAktiveMeldungenAktuellenSpielTag();

		if (!canStart(meldungen, aktuelleSpielrunde)) {
			return;
		}

		// aktuelle vorhanden ?
		int neueSpielrunde = aktuelleSpielrunde;
		if (getSheetHelper().findByName(getSheetName(aktuelleSpielrunde)) != null) {
			neueSpielrunde++;
		} else {
			aktuelleSpielrunde--; // noch nicht vorhanden
		}

		gespieltenRundenEinlesen(meldungen, neueSpielrunde - 1, this.getMeldeListe().getSpielRundeNeuAuslosenAb());
		neueSpielrunde(meldungen, neueSpielrunde);
	}

	/**
	 * in der meldungen liste alle spieler die liste warimTeammit fuellen
	 *
	 * @param meldungen
	 * @param aktuelleSpielrunde bis zu diese spielrunde
	 * @param aktuelleSpielrunde ab diese spielrunde = default = 1
	 */

	void gespieltenRundenEinlesen(Meldungen meldungen, int aktuelleSpielrunde, int abSpielrunde) {

		int spielrunde = 1;

		if (abSpielrunde > 1) {
			spielrunde = abSpielrunde;
		}

		for (; spielrunde <= aktuelleSpielrunde; spielrunde++) {
			XSpreadsheet sheet = getSpielRundeSheet(spielrunde);
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
							if (spieler != null) {
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
