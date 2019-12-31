/**
 * Erstellung 31.12.2019 / Michael Massee
 */
package de.petanqueturniermanager.comp.newrelease;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.swing.JFileChooser;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kohsuke.github.GHAsset;
import org.kohsuke.github.GHRelease;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxResult;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * @author Michael Massee
 *
 */
public class DownloadExtension extends SheetRunner {
	private static final Logger logger = LogManager.getLogger(DownloadExtension.class);

	/**
	 * @param workingSpreadsheet
	 * @param spielSystem
	 */
	public DownloadExtension(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.KEIN, "Download");
	}

	@Override
	protected IKonfigurationSheet getKonfigurationSheet() {
		// kein, weil TurnierSystem.KEIN
		return null;
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

	@Override
	protected void doRun() throws GenerateException {

		NewReleaseChecker newReleaseChecker = new NewReleaseChecker();
		ExtensionsHelper extensionsHelper = ExtensionsHelper.from(getxContext());

		processBoxinfo("Start Download von Pétanque-Turnier-Manager");
		processBoxinfo("Aktuell instalierte Version " + extensionsHelper.getVersionNummer());

		GHRelease latest = newReleaseChecker.readLatestRelease();
		if (latest == null) {
			ProcessBox().fehler("Kein Version verfügbar.");
			return;
		}

		GHAsset oxtAsset = newReleaseChecker.getDownloadGHAsset();
		URL downloadURL = newReleaseChecker.getDownloadURL(oxtAsset);
		if (downloadURL == null || oxtAsset == null) {
			ProcessBox().fehler("kein " + NewReleaseChecker.EXTENSION_FILE_SUFFIX + " Datei zum Download vorhanden.");
			return;
		}
		processBoxinfo("Version " + latest.getName());
		processBoxinfo(downloadURL.toString());

		final JFileChooser fc = new JFileChooser();
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		int answer = fc.showSaveDialog(ProcessBox.from().getFrame());
		if (answer == JFileChooser.APPROVE_OPTION) {
			File selectedPath = fc.getSelectedFile();
			File targetFile = new File(selectedPath, oxtAsset.getName());
			if (targetFile.exists()) {
				processBoxinfo("Datei bereits vorhanden " + targetFile);
				MessageBoxResult answerBereitsVorhanden = MessageBox.from(getxContext(), MessageBoxTypeEnum.QUESTION_YES_NO).caption("Datei bereits vorhanden")
						.message("Datei " + targetFile + " bereits vorhanden.\r\nÜberschreiben ?").show();
				if (answerBereitsVorhanden == MessageBoxResult.NO) {
					processBoxinfo("Abbruch");
					return;
				}
				processBoxinfo("Überschreiben");
			}

			processBoxinfo("Speichern in " + selectedPath.getPath());
			if (selectedPath.canWrite()) {
				try {
					FileUtils.copyURLToFile(downloadURL, targetFile, 10000, 10000);
				} catch (IOException e) {
					logger.error(e);
					ProcessBox().fehler(e.getMessage());
				}
			} else {
				ProcessBox().fehler("keine Schreibrechte");
			}
		} else {
			processBoxinfo("Abbruch");
		}
	}

}
