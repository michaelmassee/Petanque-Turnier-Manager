/**
 * Erstellung 11.02.2020 / Michael Massee
 */
package de.petanqueturniermanager.supermelee.spieltagrangliste;

import static com.google.common.base.Preconditions.checkNotNull;
import static de.petanqueturniermanager.supermelee.SuperMeleeSummenSpalten.PUNKTE_DIV_OFFS;
import static de.petanqueturniermanager.supermelee.SuperMeleeSummenSpalten.PUNKTE_PLUS_OFFS;
import static de.petanqueturniermanager.supermelee.SuperMeleeSummenSpalten.SPIELE_DIV_OFFS;
import static de.petanqueturniermanager.supermelee.SuperMeleeSummenSpalten.SPIELE_PLUS_OFFS;

import java.util.Arrays;
import java.util.List;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.MeldungenSpalte;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.model.Spieler;
import de.petanqueturniermanager.model.SpielerMeldungen;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeKonfigurationSheet;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * @author Michael Massee
 *
 */
abstract class AbstractSpieltagRangliste extends SheetRunner implements ISheet {

	public static final int RANGLISTE_SPALTE = 2;
	public static final int ERSTE_SPIELRUNDE_SPALTE = 3;
	public static final int ERSTE_DATEN_ZEILE = 3;
	public static final int SPIELER_NR_SPALTE = 0;
	public static final String SHEETNAME_SUFFIX = "Spieltag Rangliste";

	private final SuperMeleeKonfigurationSheet konfigurationSheet;
	private SpielTagNr spieltagNr = null;
	private final MeldungenSpalte<SpielerMeldungen, Spieler> spielerSpalte;

	/**
	 * @param workingSpreadsheet
	 * @param logPrefix
	 */
	public AbstractSpieltagRangliste(WorkingSpreadsheet workingSpreadsheet, String logPrefix) {
		super(workingSpreadsheet, TurnierSystem.SUPERMELEE, logPrefix);
		konfigurationSheet = new SuperMeleeKonfigurationSheet(workingSpreadsheet);
		spielerSpalte = MeldungenSpalte.builder().ersteDatenZiele(ERSTE_DATEN_ZEILE).spielerNrSpalte(SPIELER_NR_SPALTE).sheet(this).anzZeilenInHeader(2).formation(Formation.MELEE)
				.spalteMeldungNameWidth(SuperMeleeKonfigurationSheet.SUPER_MELEE_MELDUNG_NAME_WIDTH).build();
	}

	/**
	 * @param workingSpreadsheet
	 */
	public AbstractSpieltagRangliste(WorkingSpreadsheet workingSpreadsheet) {
		this(workingSpreadsheet, null);
	}

	@Override
	protected SuperMeleeKonfigurationSheet getKonfigurationSheet() {
		return konfigurationSheet;
	}

	// Gleiche Reihenfolge in Spieltag und Endrangliste
	protected List<Position> getRanglisteSpalten(int ersteSpalteEndsumme, int ersteDatenZeile)
			throws GenerateException {
		Position summeSpielGewonnenZelle1 = Position.from(ersteSpalteEndsumme + SPIELE_PLUS_OFFS, ersteDatenZeile);
		Position summeSpielDiffZelle1 = Position.from(ersteSpalteEndsumme + SPIELE_DIV_OFFS, ersteDatenZeile);
		Position punkteDiffZelle1 = Position.from(ersteSpalteEndsumme + PUNKTE_DIV_OFFS, ersteDatenZeile);
		Position punkteGewonnenZelle1 = Position.from(ersteSpalteEndsumme + PUNKTE_PLUS_OFFS, ersteDatenZeile);
		Position[] arraylist = new Position[] { summeSpielGewonnenZelle1, summeSpielDiffZelle1, punkteDiffZelle1,
				punkteGewonnenZelle1 };
		return Arrays.asList(arraylist);
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

	protected MeldungenSpalte<SpielerMeldungen, Spieler> getSpielerSpalte() {
		return spielerSpalte;
	}

}
