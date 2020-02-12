/**
 * Erstellung 11.02.2020 / Michael Massee
 */
package de.petanqueturniermanager.supermelee.spieltagrangliste;

import static com.google.common.base.Preconditions.checkNotNull;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.MeldungenSpalte;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.model.SpielerMeldungen;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeSheet;

/**
 * @author Michael Massee
 *
 */
abstract class AbstractSpieltagRangliste extends SuperMeleeSheet implements ISheet {

	public static final int RANGLISTE_SPALTE = 2;
	public static final int ERSTE_SPIELRUNDE_SPALTE = 3;
	public static final int ERSTE_DATEN_ZEILE = 3;
	public static final int SPIELER_NR_SPALTE = 0;
	public static final String SHEETNAME_SUFFIX = "Spieltag Rangliste";

	private SpielTagNr spieltagNr = null;
	private final MeldungenSpalte<SpielerMeldungen> spielerSpalte;

	/**
	 * @param workingSpreadsheet
	 * @param logPrefix
	 */
	public AbstractSpieltagRangliste(WorkingSpreadsheet workingSpreadsheet, String logPrefix) {
		super(workingSpreadsheet, logPrefix);

		spielerSpalte = MeldungenSpalte.Builder().ersteDatenZiele(ERSTE_DATEN_ZEILE).spielerNrSpalte(SPIELER_NR_SPALTE).sheet(this).anzZeilenInHeader(2).formation(Formation.MELEE)
				.spalteMeldungNameWidth(SUPER_MELEE_MELDUNG_NAME_WIDTH).build();
	}

	/**
	 * @param workingSpreadsheet
	 */
	public AbstractSpieltagRangliste(WorkingSpreadsheet workingSpreadsheet) {
		this(workingSpreadsheet, null);
	}

	public String getSheetName(SpielTagNr spieltagNr) {
		return spieltagNr.getNr() + ". " + SHEETNAME_SUFFIX;
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return getSheet(getSpieltagNr());
	}

	@Override
	public final TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}

	public XSpreadsheet getSheet(SpielTagNr spielTagNr) throws GenerateException {
		return getSheetHelper().findByName(getSheetName(spielTagNr));
	}

	public SpielTagNr getSpieltagNr() {
		checkNotNull(spieltagNr, "spieltagNr==null");
		return spieltagNr;
	}

	public void setSpieltagNr(SpielTagNr spieltagNr) {
		checkNotNull(spieltagNr, "spieltagNr==null");
		this.spieltagNr = spieltagNr;
	}

	protected MeldungenSpalte<SpielerMeldungen> getSpielerSpalte() {
		return spielerSpalte;
	}

}
