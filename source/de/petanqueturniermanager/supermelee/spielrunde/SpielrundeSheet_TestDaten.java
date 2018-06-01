/**
* Erstellung : 26.03.2018 / Michael Massee
**/
package de.petanqueturniermanager.supermelee.spielrunde;

import java.util.concurrent.ThreadLocalRandom;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.konfiguration.KonfigurationSheet;
import de.petanqueturniermanager.model.Meldungen;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.SupermeleeTeamPaarungenSheet;
import de.petanqueturniermanager.supermelee.meldeliste.AnmeldungenSheet;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_TestDaten;
import de.petanqueturniermanager.supermelee.meldeliste.TielnehmerSheet;
import de.petanqueturniermanager.supermelee.spieltagrangliste.SpieltagRanglisteSheet;

public class SpielrundeSheet_TestDaten extends AbstractSpielrundeSheet {
	private static final Logger logger = LogManager.getLogger(SpielrundeSheet_TestDaten.class);

	private final SpielrundeSheet_Naechste naechsteSpielrundeSheet;
	private final MeldeListeSheet_TestDaten meldeListeTestDatenGenerator;
	private final SpieltagRanglisteSheet spieltagRanglisteSheet;
	private final AnmeldungenSheet anmeldungenSheet;
	private final TielnehmerSheet tielnehmerSheet;

	public SpielrundeSheet_TestDaten(XComponentContext xContext) {
		super(xContext);
		this.naechsteSpielrundeSheet = new SpielrundeSheet_Naechste(xContext);
		this.meldeListeTestDatenGenerator = new MeldeListeSheet_TestDaten(xContext);
		this.spieltagRanglisteSheet = new SpieltagRanglisteSheet(xContext);
		this.anmeldungenSheet = new AnmeldungenSheet(xContext);
		this.tielnehmerSheet = new TielnehmerSheet(xContext);

	}

	@Override
	public Logger getLogger() {
		return logger;
	}

	@Override
	protected void doRun() throws GenerateException {
		// clean up first
		this.getSheetHelper().removeAllSheetsExclude(new String[] { KonfigurationSheet.SHEETNAME, SupermeleeTeamPaarungenSheet.SHEETNAME });
		setSpielTag(SpielTagNr.from(1));
		generate();
	}

	/**
	 * 4 spielrunden testdaten generieren
	 *
	 * @throws GenerateException
	 */
	public void generate() throws GenerateException {

		this.anmeldungenSheet.setSpielTag(getSpielTag());
		this.tielnehmerSheet.setSpielTagNr(getSpielTag());
		this.spieltagRanglisteSheet.setSpieltagNr(getSpielTag());
		this.naechsteSpielrundeSheet.setSpielTag(getSpielTag());

		if (getSpielTag().getNr() == 1) {
			this.meldeListeTestDatenGenerator.testNamenEinfuegen(getSpielTag());
		}

		this.meldeListeTestDatenGenerator.initialAktuellenSpielTagMitAktivenMeldungenFuellen(getSpielTag());

		this.anmeldungenSheet.generate();
		this.tielnehmerSheet.generate();

		int maxspielrundeNr = 4;

		for (int spielrundeNr = 1; spielrundeNr <= maxspielrundeNr; spielrundeNr++) {
			SheetRunner.testDoCancelTask();
			setSpielRundeNr(SpielRundeNr.from(spielrundeNr));

			if (spielrundeNr > 1) {
				this.meldeListeTestDatenGenerator.spielerAufAktivInaktivMischen(getSpielTag());
			}

			Meldungen meldungen = this.getMeldeListe().getAktiveMeldungen();
			this.naechsteSpielrundeSheet.gespieltenRundenEinlesen(meldungen, spielrundeNr - 1, getKonfigurationSheet().getSpielRundeNeuAuslosenAb());
			neueSpielrunde(meldungen, SpielRundeNr.from(spielrundeNr), true);

			// ------------------------------------
			// spiel test ergebnisse einfuegen
			// ------------------------------------
			XSpreadsheet sheet = getSpielRundeSheet(getSpielTag(), getSpielRundeNr());
			Position letztePos = letzteZeile();

			if (letztePos != null && sheet != null) {
				for (int zeileCntr = ERSTE_DATEN_ZEILE; zeileCntr <= letztePos.getZeile(); zeileCntr++) {
					SheetRunner.testDoCancelTask();

					Position pos = Position.from(ERSTE_SPALTE_ERGEBNISSE, zeileCntr);

					int welchenTeamHatGewonnen = ThreadLocalRandom.current().nextInt(0, 2); // 0,1
					int verliererPunkte = ThreadLocalRandom.current().nextInt(0, 13); // 0 - 12
					// gewinner kann auch weniger als 13 punkte
					int gewinnerPunkte = ThreadLocalRandom.current().nextInt(verliererPunkte + 1, 14); // random rest
																										// bis 13
					int valA = (welchenTeamHatGewonnen == 0 ? verliererPunkte : gewinnerPunkte);
					int valB = (welchenTeamHatGewonnen == 0 ? gewinnerPunkte : verliererPunkte);

					NumberCellValue numberCellValue = NumberCellValue.from(sheet, pos, valA);
					getSheetHelper().setValInCell(numberCellValue);
					getSheetHelper().setValInCell(numberCellValue.spaltePlusEins().setValue((double) valB));
				}
			}
		}
		SheetRunner.testDoCancelTask();
		this.spieltagRanglisteSheet.generate();

		this.getKonfigurationSheet().setAktiveSpieltag(getSpielTag());
		this.getKonfigurationSheet().setAktiveSpielRunde(SpielRundeNr.from(maxspielrundeNr));
		this.spieltagRanglisteSheet.isErrorInSheet();
	}
}
