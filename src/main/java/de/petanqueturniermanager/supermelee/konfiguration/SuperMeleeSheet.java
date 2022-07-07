/**
 * Erstellung 08.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.supermelee.konfiguration;

import static com.google.common.base.Preconditions.checkNotNull;
import static de.petanqueturniermanager.supermelee.SuperMeleeSummenSpalten.PUNKTE_DIV_OFFS;
import static de.petanqueturniermanager.supermelee.SuperMeleeSummenSpalten.PUNKTE_PLUS_OFFS;
import static de.petanqueturniermanager.supermelee.SuperMeleeSummenSpalten.SPIELE_DIV_OFFS;
import static de.petanqueturniermanager.supermelee.SuperMeleeSummenSpalten.SPIELE_PLUS_OFFS;

import java.util.Arrays;
import java.util.List;

import com.google.common.annotations.VisibleForTesting;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.addins.GlobalImpl;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * @author Michael Massee
 *
 */
public abstract class SuperMeleeSheet extends SheetRunner {

	protected static final int SUPER_MELEE_MELDUNG_NAME_WIDTH = 4000; // spalte Name

	public static final String PTM_SPIELTAG = GlobalImpl
			.FORMAT_PTM_INT_PROPERTY(SuperMeleePropertiesSpalte.KONFIG_PROP_NAME_SPIELTAG);
	public static final String PTM_SPIELRUNDE = GlobalImpl
			.FORMAT_PTM_INT_PROPERTY(SuperMeleePropertiesSpalte.KONFIG_PROP_NAME_SPIELRUNDE);

	private SuperMeleeKonfigurationSheet konfigurationSheet;

	/**
	 * @param workingSpreadsheet
	 */
	public SuperMeleeSheet(WorkingSpreadsheet workingSpreadsheet) {
		this(workingSpreadsheet, null);
	}

	public SuperMeleeSheet(WorkingSpreadsheet workingSpreadsheet, String logPrefix) {
		super(checkNotNull(workingSpreadsheet), TurnierSystem.SUPERMELEE, logPrefix);
		konfigurationSheet = newSuperMeleeKonfigurationSheet(workingSpreadsheet);
	}

	@VisibleForTesting
	protected SuperMeleeKonfigurationSheet newSuperMeleeKonfigurationSheet(WorkingSpreadsheet workingSpreadsheet) {
		return new SuperMeleeKonfigurationSheet(workingSpreadsheet);
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

		Position[] arraylist = new Position[] { summeSpielGewonnenZelle1, summeSpielDiffZelle1, punkteDiffZelle1,
				punkteGewonnenZelle1 };
		return Arrays.asList(arraylist);
	}

}
