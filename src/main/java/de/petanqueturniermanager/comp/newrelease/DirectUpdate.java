/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.comp.newrelease;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import com.sun.star.system.SystemShellExecuteFlags;
import com.sun.star.system.XSystemShellExecute;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.I18n;
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
    protected @Nullable IKonfigurationSheet getKonfigurationSheet() {
        return null;
    }

    @Override
    protected void doRun() throws GenerateException {
        ExtensionsHelper extensionsHelper = ExtensionsHelper.from(getxContext());

        processBoxinfo("processbox.direkt.aktualisierung.start");
        processBoxinfo("processbox.installierte.version", extensionsHelper.getVersionNummer());

        ReleaseInfo release;
        try {
            release = ReleaseUpdateService.get().getAktuellesRelease().orElse(null);
        } catch (IllegalStateException e) {
            release = null;
        }
        if (release == null) {
            processBox().fehler("Keine " + DownloadExtension.EXTENSION_FILE_SUFFIX
                    + " Datei zum Download vorhanden.");
            return;
        }
        var oxtAsset = release.findeAsset(
                DownloadExtension.EXTENSION_FILE_PREFIX,
                DownloadExtension.EXTENSION_FILE_SUFFIX).orElse(null);
        if (oxtAsset == null) {
            processBox().fehler("Keine " + DownloadExtension.EXTENSION_FILE_SUFFIX
                    + " Datei zum Download vorhanden.");
            return;
        }
        URL downloadURL;
        try {
            downloadURL = URI.create(oxtAsset.downloadUrl()).toURL();
        } catch (IllegalArgumentException | MalformedURLException e) {
            processBox().fehler("Ungültige Download-URL: " + e.getMessage());
            return;
        }

        processBoxinfo("processbox.lade.herunter", downloadURL.toString());

        try {
            Path tempDir = Files.createTempDirectory("ptm_update");
            Path tempFile = tempDir.resolve(oxtAsset.name());

            FileUtils.copyURLToFile(downloadURL, tempFile.toFile(), 30000, 30000);
            processBoxinfo("processbox.download.abgeschlossen.installation");

            var xShell = Lo.createInstanceMCF(
                    XSystemShellExecute.class,
                    "com.sun.star.system.SystemShellExecute",
                    getxContext().getServiceManager(),
                    getxContext());
            if (xShell == null) {
                throw new GenerateException("SystemShellExecute nicht verfügbar.");
            }
            xShell.execute(tempFile.toUri().toString(), "", (short) SystemShellExecuteFlags.URIS_ONLY);

            processBoxinfo("processbox.neue.version.installiert");
            MessageBox.from(getxContext(), MessageBoxTypeEnum.INFO_OK)
                    .caption(I18n.get("msg.caption.aktualisierung.abgeschlossen"))
                    .message(I18n.get("msg.text.aktualisierung.abgeschlossen"))
                    .show();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            processBox().fehler(e.getMessage());
        }
    }
}
