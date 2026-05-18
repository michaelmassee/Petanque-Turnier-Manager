/*
 * Erstellung 31.12.2019 / Michael Massee
 */
package de.petanqueturniermanager.comp.newrelease;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import com.sun.star.ui.dialogs.ExecutableDialogResults;
import com.sun.star.ui.dialogs.FolderPicker;
import com.sun.star.ui.dialogs.XFolderPicker2;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxResult;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;

/**
 * Lädt das aktuellste OXT-Release in ein vom Nutzer gewähltes Verzeichnis.
 *
 * @author Michael Massee
 */
public class DownloadExtension extends SheetRunner {

    public static final String EXTENSION_FILE_PREFIX = "petanqueturniermanager";
    public static final String EXTENSION_FILE_SUFFIX = "oxt";

    private static final Logger logger = LogManager.getLogger(DownloadExtension.class);

    public DownloadExtension(WorkingSpreadsheet workingSpreadsheet) {
        super(workingSpreadsheet, TurnierSystem.KEIN, "Download");
    }

    @Override
    protected @Nullable IKonfigurationSheet getKonfigurationSheet() {
        return null;
    }

    @Override
    protected void doRun() throws GenerateException {
        ExtensionsHelper extensionsHelper = ExtensionsHelper.from(getxContext());
        processBoxinfo("processbox.download.start");
        processBoxinfo("processbox.installierte.version", extensionsHelper.getVersionNummer());

        ReleaseInfo release;
        try {
            release = ReleaseUpdateService.get().getAktuellesRelease().orElse(null);
        } catch (IllegalStateException e) {
            release = null;
        }
        if (release == null) {
            processBox().fehler("Keine Version verfügbar.");
            return;
        }

        var oxtAsset = release.findeAsset(EXTENSION_FILE_PREFIX, EXTENSION_FILE_SUFFIX).orElse(null);
        if (oxtAsset == null) {
            processBox().fehler("keine " + EXTENSION_FILE_SUFFIX + " Datei zum Download vorhanden.");
            return;
        }
        URL downloadURL;
        try {
            downloadURL = URI.create(oxtAsset.downloadUrl()).toURL();
        } catch (IllegalArgumentException | java.net.MalformedURLException e) {
            processBox().fehler("Ungültige Download-URL: " + e.getMessage());
            return;
        }

        processBoxinfo("processbox.github.version", release.name());
        processBox().info(downloadURL.toString());

        XFolderPicker2 picker = FolderPicker.create(getWorkingSpreadsheet().getxContext());
        picker.setTitle("Download Verzeichnis");
        short res = picker.execute();
        if (res != ExecutableDialogResults.OK) {
            processBoxinfo("processbox.abbruch");
            return;
        }
        try {
            String directoryUrl = picker.getDirectory();
            URI dirURL = new URI(directoryUrl);
            File selectedPath = new File(dirURL);
            File targetFile = new File(selectedPath, oxtAsset.name());
            if (targetFile.exists()) {
                processBoxinfo("processbox.datei.bereits.vorhanden", targetFile);
                MessageBoxResult answer = MessageBox
                        .from(getxContext(), MessageBoxTypeEnum.QUESTION_YES_NO)
                        .caption(I18n.get("msg.caption.datei.vorhanden"))
                        .message(I18n.get("msg.text.datei.vorhanden.ueberschreiben", targetFile))
                        .show();
                if (answer == MessageBoxResult.NO) {
                    processBoxinfo("processbox.abbruch");
                    return;
                }
                processBoxinfo("processbox.ueberschreiben");
            }

            processBoxinfo("processbox.speichern.in", selectedPath.getPath());
            if (selectedPath.canWrite()) {
                FileUtils.copyURLToFile(downloadURL, targetFile, 10000, 10000);
            } else {
                processBox().fehler("keine Schreibrechte");
            }
            processBox().infoText("");
        } catch (IOException | URISyntaxException e) {
            logger.error(e);
            processBox().fehler(e.getMessage());
        }
    }
}
