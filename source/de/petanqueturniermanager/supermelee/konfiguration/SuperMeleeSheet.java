/**
 * Erstellung 08.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.supermelee.konfiguration;

import static com.google.common.base.Preconditions.checkNotNull;
import static de.petanqueturniermanager.helper.sheet.SummenSpalten.PUNKTE_DIV_OFFS;
import static de.petanqueturniermanager.helper.sheet.SummenSpalten.PUNKTE_PLUS_OFFS;
import static de.petanqueturniermanager.helper.sheet.SummenSpalten.SPIELE_DIV_OFFS;
import static de.petanqueturniermanager.helper.sheet.SummenSpalten.SPIELE_PLUS_OFFS;

import java.util.Arrays;
import java.util.List;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * @author Michael Massee
 *
 */
public abstract class SuperMeleeSheet extends SheetRunner {

	protected static final int SUPER_MELEE_MELDUNG_NAME_WIDTH = 4000; // spalte Name

	private SuperMeleeKonfigurationSheet konfigurationSheet;

	/**
	 * @param workingSpreadsheet
	 */
	public SuperMeleeSheet(WorkingSpreadsheet workingSpreadsheet) {
		this(workingSpreadsheet, null);
	}

	public SuperMeleeSheet(WorkingSpreadsheet workingSpreadsheet, String logPrefix) {
		super(checkNotNull(workingSpreadsheet), TurnierSystem.SUPERMELEE, logPrefix);
		konfigurationSheet = new SuperMeleeKonfigurationSheet(workingSpreadsheet);
	}

	@Override
	protected SuperMeleeKonfigurationSheet getKonfigurationSheet() {
		return konfigurationSheet;
	}

	// Gleiche reihenfolge in Speiltag und endrangliste
	protected List<Position> getRanglisteSpalten(int ersteSpalteEndsumme, int ersteDatenZeile) {
		Position summeSpielGewonnenZelle1 = Position.from(ersteSpalteEndsumme + SPIELE_PLUS_OFFS, ersteDatenZeile);
		Position summeSpielDiffZelle1 = Position.from(ersteSpalteEndsumme + SPIELE_DIV_OFFS, ersteDatenZeile);
		Position punkteDiffZelle1 = Position.from(ersteSpalteEndsumme + PUNKTE_DIV_OFFS, ersteDatenZeile);
		Position punkteGewonnenZelle1 = Position.from(ersteSpalteEndsumme + PUNKTE_PLUS_OFFS, ersteDatenZeile);

		Position[] arraylist = new Position[] { summeSpielGewonnenZelle1, summeSpielDiffZelle1, punkteDiffZelle1, punkteGewonnenZelle1 };
		return Arrays.asList(arraylist);
	}

}
