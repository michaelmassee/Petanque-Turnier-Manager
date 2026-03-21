/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.comp.newrelease;

import org.kohsuke.github.GHRelease;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.I18n;
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
		String name = rel.getName() != null ? rel.getName() : rel.getTagName();
		processBoxinfo(I18n.get("processbox.release.info", (name != null ? name : "?")));
		processBoxinfo(I18n.get("processbox.release.infos"));
		String body = rel.getBody();
		processBoxinfo(body != null && !body.isBlank() ? body : I18n.get("processbox.keine.beschreibung"));
	}
}
