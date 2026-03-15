/**
 * Erstellung 2026 / Michael Massee
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

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.MeldungenSpalte;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.model.Spieler;
import de.petanqueturniermanager.model.SpielerMeldungen;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeKonfigurationSheet;

/**
 * Delegate für SpieltagRangliste-Sheets: hält gemeinsamen Zustand und Hilfsmethoden.
 *
 * @author Michael Massee
 */
class SpieltagRanglisteDelegate {

	static final int RANGLISTE_SPALTE = 2;
	static final int ERSTE_SPIELRUNDE_SPALTE = 3;
	static final int ERSTE_DATEN_ZEILE = 3;
	static final int SPIELER_NR_SPALTE = 0;
	static final String SHEETNAME_SUFFIX = "Spieltag Rangliste";

	private final ISheet sheet;
	private final SuperMeleeKonfigurationSheet konfigurationSheet;
	private final MeldungenSpalte<SpielerMeldungen, Spieler> spielerSpalte;

	private SpielTagNr spieltagNr = null;

	SpieltagRanglisteDelegate(ISheet sheet) {
		this.sheet = checkNotNull(sheet);
		konfigurationSheet = new SuperMeleeKonfigurationSheet(sheet.getWorkingSpreadsheet());
		spielerSpalte = MeldungenSpalte.builder()
				.ersteDatenZiele(ERSTE_DATEN_ZEILE)
				.spielerNrSpalte(SPIELER_NR_SPALTE)
				.sheet(sheet)
				.anzZeilenInHeader(2)
				.formation(Formation.MELEE)
				.spalteMeldungNameWidth(SuperMeleeKonfigurationSheet.SUPER_MELEE_MELDUNG_NAME_WIDTH)
				.build();
	}

	SuperMeleeKonfigurationSheet getKonfigurationSheet() {
		return konfigurationSheet;
	}

	SpielTagNr getSpieltagNr() {
		checkNotNull(spieltagNr, "spieltagNr==null");
		return spieltagNr;
	}

	void setSpieltagNr(SpielTagNr spieltagNr) {
		this.spieltagNr = checkNotNull(spieltagNr, "spieltagNr==null");
	}

	MeldungenSpalte<SpielerMeldungen, Spieler> getSpielerSpalte() {
		return spielerSpalte;
	}

	String getSheetName(SpielTagNr spielTagNr) {
		return spielTagNr.getNr() + ". " + SHEETNAME_SUFFIX;
	}

	XSpreadsheet getSheet(SpielTagNr spielTagNr) throws GenerateException {
		return sheet.getSheetHelper().findByName(getSheetName(spielTagNr));
	}

	// Gleiche Reihenfolge in Spieltag und Endrangliste
	List<Position> getRanglisteSpalten(int ersteSpalteEndsumme, int ersteDatenZeile) {
		Position summeSpielGewonnenZelle1 = Position.from(ersteSpalteEndsumme + SPIELE_PLUS_OFFS, ersteDatenZeile);
		Position summeSpielDiffZelle1 = Position.from(ersteSpalteEndsumme + SPIELE_DIV_OFFS, ersteDatenZeile);
		Position punkteDiffZelle1 = Position.from(ersteSpalteEndsumme + PUNKTE_DIV_OFFS, ersteDatenZeile);
		Position punkteGewonnenZelle1 = Position.from(ersteSpalteEndsumme + PUNKTE_PLUS_OFFS, ersteDatenZeile);
		return Arrays.asList(new Position[] { summeSpielGewonnenZelle1, summeSpielDiffZelle1, punkteDiffZelle1, punkteGewonnenZelle1 });
	}

}
