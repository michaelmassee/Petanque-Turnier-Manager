/**
 * Erstellung : 15.04.2026 / Michael Massee
 **/

package de.petanqueturniermanager.supermelee.blattschutz;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheets;
import com.sun.star.util.XProtectable;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Schaltet den Blattschutz aller Sheets um (alle ein → alle aus oder umgekehrt).
 */
public class BlattSchutzToggle extends SheetRunner {

	public BlattSchutzToggle(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.SUPERMELEE, "BlattSchutz");
	}

	@Override
	protected IKonfigurationSheet getKonfigurationSheet() {
		return null;
	}

	@Override
	protected void doRun() throws GenerateException {
		var doc = getWorkingSpreadsheet().getWorkingSpreadsheetDocument();
		var sheets = doc.getSheets();
		var sheetnamen = sheets.getElementNames();

		boolean neuerZustand = !alleGeschuetzt(sheets, sheetnamen);

		for (var name : sheetnamen) {
			try {
				TurnierSheet.from(Lo.qi(XSpreadsheet.class, sheets.getByName(name)), getWorkingSpreadsheet())
						.protect(neuerZustand);
			} catch (com.sun.star.container.NoSuchElementException | com.sun.star.lang.WrappedTargetException e) {
				getLogger().error("Blattschutz für '{}' nicht setzbar: {}", name, e.getMessage(), e);
			}
		}

		processBoxinfo(neuerZustand ? "blattschutz.aktiviert" : "blattschutz.deaktiviert");
	}

	/**
	 * Prüft ob alle Sheets des Dokuments geschützt sind.
	 *
	 * @param sheets     die Sheets des Dokuments
	 * @param sheetnamen alle Blattnamen
	 * @return {@code true} wenn alle Sheets geschützt sind
	 */
	public static boolean alleGeschuetzt(XSpreadsheets sheets, String[] sheetnamen) {
		for (var name : sheetnamen) {
			try {
				var sheet = Lo.qi(XSpreadsheet.class, sheets.getByName(name));
				var protectable = Lo.qi(XProtectable.class, sheet);
				if (protectable != null && !protectable.isProtected()) {
					return false;
				}
			} catch (com.sun.star.container.NoSuchElementException | com.sun.star.lang.WrappedTargetException e) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Prüft ob mindestens ein Sheet des Dokuments tab-geschützt ist.
	 *
	 * @param sheets     die Sheets des Dokuments
	 * @param sheetnamen alle Blattnamen
	 * @return {@code true} wenn mindestens ein Sheet geschützt ist
	 */
	public static boolean irgendeinSheetGeschuetzt(XSpreadsheets sheets, String[] sheetnamen) {
		for (var name : sheetnamen) {
			try {
				var sheet = Lo.qi(XSpreadsheet.class, sheets.getByName(name));
				var protectable = Lo.qi(XProtectable.class, sheet);
				if (protectable != null && protectable.isProtected()) {
					return true;
				}
			} catch (com.sun.star.container.NoSuchElementException | com.sun.star.lang.WrappedTargetException e) {
				// Sheet nicht gefunden → ignorieren
			}
		}
		return false;
	}
}
