package de.petanqueturniermanager.spielerdb.ui;

import java.net.URI;
import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import com.sun.star.ui.dialogs.ExecutableDialogResults;
import com.sun.star.ui.dialogs.ExtendedFilePickerElementIds;
import com.sun.star.ui.dialogs.FilePicker;
import com.sun.star.ui.dialogs.FolderPicker;
import com.sun.star.ui.dialogs.TemplateDescription;
import com.sun.star.ui.dialogs.XFilePicker3;
import com.sun.star.ui.dialogs.XFilePickerControlAccess;
import com.sun.star.ui.dialogs.XFolderPicker2;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.spielerdb.SpielerDbDateiFormat;

/**
 * Wiederverwendbare Datei-/Ordner-Picker-Logik für Spieler-DB-Dialoge
 * (Export wie Import). Kümmert sich um AutoExtension, letzte-Pfad-Vorbelegung
 * und URL↔Pfad-Konvertierung.
 */
final class SpielerDbPickerHelfer {

    private static final Logger logger = LogManager.getLogger(SpielerDbPickerHelfer.class);

    /** Wird im Picker für Filter und Default-Dateinamen verwendet. */
    enum PickerModus { OEFFNEN, SPEICHERN }

    private SpielerDbPickerHelfer() { /* Utility */ }

    /**
     * Ordner-Picker. Vorbelegung mit {@code letzterPfad}, falls vorhanden.
     * Liefert {@code null} bei Abbruch.
     */
    @Nullable
    static String oeffneOrdnerPicker(XComponentContext xContext, String titel,
            @Nullable String letzterPfad) {
        XFolderPicker2 picker = FolderPicker.create(xContext);
        picker.setTitle(titel);
        if (letzterPfad != null && !letzterPfad.isEmpty()) {
            picker.setDisplayDirectory(pfadAlsUrl(letzterPfad));
        }
        if (picker.execute() != ExecutableDialogResults.OK) {
            return null;
        }
        return urlAlsPfad(picker.getDirectory());
    }

    /**
     * Datei-Picker. Bei {@link PickerModus#SPEICHERN} mit AutoExtension und
     * {@code FILESAVE_AUTOEXTENSION}-Template; sonst {@code FILEOPEN_SIMPLE}.
     */
    @Nullable
    static String oeffneDateiPicker(XComponentContext xContext, String titel,
            SpielerDbDateiFormat format, @Nullable String letzterPfad,
            PickerModus modus) throws com.sun.star.uno.Exception {
        short template = modus == PickerModus.SPEICHERN
                ? TemplateDescription.FILESAVE_AUTOEXTENSION
                : TemplateDescription.FILEOPEN_SIMPLE;
        XFilePicker3 picker = FilePicker.createWithMode(xContext, template);
        picker.setTitle(titel);
        picker.appendFilter(format.anzeigeName(), "*." + format.defaultEndung());
        if (modus == PickerModus.SPEICHERN) {
            XFilePickerControlAccess access = Lo.qi(XFilePickerControlAccess.class, picker);
            if (access != null) {
                try {
                    access.setValue(ExtendedFilePickerElementIds.CHECKBOX_AUTOEXTENSION,
                            (short) 0, Boolean.TRUE);
                } catch (RuntimeException e) {
                    // optional — Picker funktioniert auch ohne explizite AutoExtension.
                    logger.debug("AutoExtension-Property konnte nicht gesetzt werden: {}", e.getMessage());
                }
            }
        }
        if (letzterPfad != null && !letzterPfad.isEmpty()) {
            Path p = Path.of(letzterPfad);
            Path eltern = p.getParent();
            if (eltern != null) {
                picker.setDisplayDirectory(pfadAlsUrl(eltern.toString()));
            }
            Path dateinameP = p.getFileName();
            if (dateinameP != null && modus == PickerModus.SPEICHERN) {
                picker.setDefaultName(dateinameP.toString());
            }
        } else if (modus == PickerModus.SPEICHERN) {
            picker.setDefaultName("spielerdb." + format.defaultEndung());
        }
        if (picker.execute() != ExecutableDialogResults.OK) {
            return null;
        }
        String[] dateien = picker.getFiles();
        if (dateien.length == 0) {
            return null;
        }
        return urlAlsPfad(dateien[0]);
    }

    static String pfadAlsUrl(String absoluterPfad) {
        try {
            return Path.of(absoluterPfad).toAbsolutePath().toUri().toURL().toExternalForm();
        } catch (java.net.MalformedURLException | RuntimeException e) {
            return "";
        }
    }

    static String urlAlsPfad(String url) {
        try {
            return Path.of(URI.create(url)).toString();
        } catch (RuntimeException e) {
            return url;
        }
    }
}
