/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.comp.newrelease;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kohsuke.github.GHAsset;

import com.sun.star.beans.NamedValue;
import com.sun.star.deployment.XExtensionManager;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Lädt die neueste Extension-Version herunter und installiert sie direkt in LibreOffice.
 *
 * @author Michael Massee
 */
public class DirectUpdate extends SheetRunner {

	private static final Logger logger = LogManager.getLogger(DirectUpdate.class);

	public DirectUpdate(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.KEIN, "Direkt-Aktualisierung");
	}

	@Override
	protected IKonfigurationSheet getKonfigurationSheet() {
		// kein, weil TurnierSystem.KEIN
		return null;
	}

	@Override
	protected void doRun() throws GenerateException {
		NewReleaseChecker checker = new NewReleaseChecker();
		ExtensionsHelper extensionsHelper = ExtensionsHelper.from(getxContext());

		processBoxinfo("Start Direkt-Aktualisierung von Pétanque-Turnier-Manager");
		processBoxinfo("Aktuell installierte Version " + extensionsHelper.getVersionNummer());

		GHAsset oxtAsset = checker.getDownloadGHAsset();
		URL downloadURL = checker.getDownloadURL(oxtAsset);

		if (downloadURL == null || oxtAsset == null) {
			processBox().fehler("Keine " + NewReleaseChecker.EXTENSION_FILE_SUFFIX + " Datei zum Download vorhanden.");
			return;
		}

		processBoxinfo("Lade herunter: " + downloadURL.toString());

		try {
			Path tempDir = Files.createTempDirectory("ptm_update");
			Path tempFile = tempDir.resolve(oxtAsset.getName());

			FileUtils.copyURLToFile(downloadURL, tempFile.toFile(), 30000, 30000);
			processBoxinfo("Download abgeschlossen. Installation läuft...");

			Object emObj = getxContext().getServiceManager()
					.createInstanceWithContext("com.sun.star.deployment.ExtensionManager", getxContext());
			XExtensionManager em = Lo.qi(XExtensionManager.class, emObj);
			em.addExtension(tempFile.toUri().toString(), new NamedValue[0], "user", null, null);

			processBoxinfo("Neue Version erfolgreich installiert.");
			MessageBox.from(getxContext(), MessageBoxTypeEnum.INFO_OK)
					.caption("Aktualisierung abgeschlossen")
					.message("Die neue Version wurde erfolgreich installiert.\n"
							+ "Bitte LibreOffice neu starten, um die neue Version zu aktivieren.")
					.show();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			processBox().fehler(e.getMessage());
		}
	}
}
