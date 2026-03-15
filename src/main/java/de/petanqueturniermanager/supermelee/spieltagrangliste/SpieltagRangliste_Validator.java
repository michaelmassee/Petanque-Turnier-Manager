/**
 * Erstellung 11.02.2020 / Michael Massee
 */
package de.petanqueturniermanager.supermelee.spieltagrangliste;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.model.Spieler;
import de.petanqueturniermanager.model.SpielerMeldungen;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeKonfigurationSheet;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;
import de.petanqueturniermanager.supermelee.spielrunde.SpielrundeSheet_Validator;
import de.petanqueturniermanager.supermelee.spielrunde.SpielrundeSheetKonstanten;
import de.petanqueturniermanager.basesheet.meldeliste.MeldungenSpalte;

/**
 * @author Michael Massee
 *
 */
public class SpieltagRangliste_Validator extends SheetRunner implements ISheet {

	private final SpieltagRanglisteDelegate delegate;
	private final SpielrundeSheet_Validator spielrundeSheetValidator;

	/**
	 * @param workingSpreadsheet
	 */
	public SpieltagRangliste_Validator(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.SUPERMELEE, "SpieltagRangliste_Validator");
		delegate = new SpieltagRanglisteDelegate(this);
		spielrundeSheetValidator = new SpielrundeSheet_Validator(workingSpreadsheet);
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return delegate.getSheet(delegate.getSpieltagNr());
	}

	@Override
	public TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}

	@Override
	protected SuperMeleeKonfigurationSheet getKonfigurationSheet() {
		return delegate.getKonfigurationSheet();
	}

	public SpielTagNr getSpieltagNr() {
		return delegate.getSpieltagNr();
	}

	public void setSpieltagNr(SpielTagNr spieltagNr) {
		delegate.setSpieltagNr(spieltagNr);
	}

	protected MeldungenSpalte<SpielerMeldungen, Spieler> getSpielerSpalte() {
		return delegate.getSpielerSpalte();
	}

	public String getSheetName(SpielTagNr spielTagNr) {
		return delegate.getSheetName(spielTagNr);
	}

	@Override
	protected void doRun() throws GenerateException {
		doValidate(getKonfigurationSheet().getAktiveSpieltag());
	}

	public void doValidate(SpielTagNr spielTagNr) throws GenerateException {
		setSpieltagNr(spielTagNr);
		spielrundeSheetValidator.setSpielTag(spielTagNr);
		validateSpieler();
		processBox().info("Kein Fehler gefunden in \"" + getSheetName(getSpieltagNr()) + "\"");
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
			RangePosition rangePosSpielrNr = RangePosition.from(SpielrundeSheetKonstanten.ERSTE_SPIELERNR_SPALTE, SpielrundeSheetKonstanten.ERSTE_DATEN_ZEILE,
					SpielrundeSheetKonstanten.LETZTE_SPALTE, 99);
			RangeData dataFromRange = RangeHelper.from(spielrundeSheetValidator, rangePosSpielrNr).getDataFromRange();

			// flatten to list
			List<Integer> nr = dataFromRange.stream().flatMap(rowData -> rowData.stream()).map(celldata -> celldata.getIntVal(0)).filter(num -> num > 0)
					.collect(Collectors.toList());
			alleSpielrNrausSpielrunden.addAll(nr);
		}
		List<Integer> spielerNrListAusRangliste = getSpielerSpalte().getSpielerNrList();

		// prüfen ob die Anzahl spieler stimmt
		if (spielerNrListAusRangliste.size() != alleSpielrNrausSpielrunden.size()) {
			throw new GenerateException("Spieltagrangliste, Anzahl Spieler stimmen nicht");
		}

		boolean allMatch = spielerNrListAusRangliste.stream().allMatch(nr -> alleSpielrNrausSpielrunden.contains(nr));

		if (!allMatch) {
			throw new GenerateException("Spieltagrangliste, Spieler Nummer stimmen nicht");
		}
	}
}
