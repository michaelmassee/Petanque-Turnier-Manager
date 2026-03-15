/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.comp.newrelease;

import org.kohsuke.github.GHRelease;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Zeigt Release-Informationen der neuesten GitHub-Version in der ProcessBox an.
 *
 * @author Michael Massee
 */
public class ReleaseInfosAnzeigen extends SheetRunner {

	public ReleaseInfosAnzeigen(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.KEIN, "Release-Infos");
	}

	@Override
	protected IKonfigurationSheet getKonfigurationSheet() {
		// kein, weil TurnierSystem.KEIN
		return null;
	}

	/**
	 * Factory-Methode – kann in Tests überschrieben werden.
	 */
	NewReleaseChecker newReleaseChecker() {
		return new NewReleaseChecker();
	}

	@Override
	protected void doRun() throws GenerateException {
		NewReleaseChecker checker = newReleaseChecker();
		GHRelease rel = checker.readLatestReleaseFromCacheFile();
		if (rel == null) {
			processBox().fehler("Keine Release-Informationen verfügbar.");
			return;
		}
		processBoxinfo("Release: " + rel.getName());
		processBoxinfo("Release-Infos:");
		processBoxinfo(rel.getBody());
	}
}
