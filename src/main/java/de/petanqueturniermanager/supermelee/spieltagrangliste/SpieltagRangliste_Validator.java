/**
 * Erstellung 11.02.2020 / Michael Massee
 */
package de.petanqueturniermanager.supermelee.spieltagrangliste;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.spielrunde.AbstractSpielrundeSheet;
import de.petanqueturniermanager.supermelee.spielrunde.SpielrundeSheet_Validator;

/**
 * @author Michael Massee
 *
 */
public class SpieltagRangliste_Validator extends AbstractSpieltagRangliste {

	private final SpielrundeSheet_Validator spielrundeSheetValidator;

	/**
	 * @param workingSpreadsheet
	 */
	public SpieltagRangliste_Validator(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, "SpieltagRangliste_Validator");
		spielrundeSheetValidator = new SpielrundeSheet_Validator(workingSpreadsheet);
	}

	private static final Logger logger = LogManager.getLogger(SpieltagRangliste_Validator.class);

	@Override
	public Logger getLogger() {
		return logger;
	}

	@Override
	protected void doRun() throws GenerateException {
		setSpieltagNr(getKonfigurationSheet().getAktiveSpieltag());
		spielrundeSheetValidator.setSpielTag(getKonfigurationSheet().getAktiveSpieltag());
		validateSpieler();
	}

	private void validateSpieler() throws GenerateException {

		// Spielrunden einlesen
		int spielrunden = getKonfigurationSheet().getAktiveSpielRunde().getNr();

		HashSet<Integer> alleSpielrNrausSpielrunden = new HashSet<>();

		for (int splRundeCntr = 1; splRundeCntr <= spielrunden; splRundeCntr++) {
			SpielRundeNr spielRundeNr = SpielRundeNr.from(splRundeCntr);
			String spielrundeName = spielrundeSheetValidator.getSheetName(getSpieltagNr(), spielRundeNr);
			spielrundeSheetValidator.setSpielRundeNr(spielRundeNr);

			// Spieler einlesen
			XSpreadsheet spielrundeSheet = getSheetHelper().findByName(spielrundeName);

			if (spielrundeSheet == null) {
				throw new GenerateException("Spielrunde " + spielrundeName + " fehlt!");
			}

			// Spieler Nummer Block rechts neben die ergebnisse spalten einlesen
			RangePosition rangePosSpielrNr = RangePosition.from(AbstractSpielrundeSheet.ERSTE_SPIELERNR_SPALTE, AbstractSpielrundeSheet.ERSTE_DATEN_ZEILE,
					AbstractSpielrundeSheet.LETZTE_SPALTE, 99);
			RangeData dataFromRange = RangeHelper.from(spielrundeSheetValidator, rangePosSpielrNr).getDataFromRange();

			// flatten to list
			List<Integer> nr = dataFromRange.stream().flatMap(rowData -> rowData.stream()).map(celldata -> celldata.getIntVal(0)).filter(num -> num > 0)
					.collect(Collectors.toList());
			alleSpielrNrausSpielrunden.addAll(nr);

			// nr.stream().forEach(System.out::println);
		}
	}

}
