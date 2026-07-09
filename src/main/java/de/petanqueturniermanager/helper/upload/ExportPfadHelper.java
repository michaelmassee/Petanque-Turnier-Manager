package de.petanqueturniermanager.helper.upload;

import java.net.URI;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.ui.dialogs.ExecutableDialogResults;
import com.sun.star.ui.dialogs.FolderPicker;
import com.sun.star.ui.dialogs.XFolderPicker2;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.i18n.I18n;

/**
 * Öffnet den Ordner-Picker auf dem LO-Main-Thread.
 * Darf ausschließlich aus {@code ProtocolHandler.dispatch()} aufgerufen werden.
 */
public final class ExportPfadHelper {

    private static final Logger logger = LogManager.getLogger(ExportPfadHelper.class);

    static final String DOC_PROP_LETZTER_PFAD = "Export Verzeichnis";
    static final String DOC_PROP_LETZTES_FORMAT = "Export Format";

    private ExportPfadHelper() {
    }

    public record ExportEinstellungen(Path verzeichnis, ExportFormat format) {
    }

    /**
     * Fragt Zielverzeichnis und Exportformat ab. Bricht der Benutzer einen der
     * beiden Schritte ab, liefert die Methode {@link Optional#empty()}.
     */
    public static Optional<ExportEinstellungen> waehleExportEinstellungen(XComponentContext xContext,
            WorkingSpreadsheet ws) throws GenerateException {
        var pfadOpt = waehlePfad(xContext, ws);
        if (pfadOpt.isEmpty()) {
            return Optional.empty();
        }
        var helper = new DocumentPropertiesHelper(ws);
        ExportFormat letztesFormat = formatAusProperty(helper.getStringProperty(DOC_PROP_LETZTES_FORMAT, ""));

        var formatOpt = ExportFormatAuswahlDialog.zeigen(ws, letztesFormat);
        if (formatOpt.isEmpty()) {
            return Optional.empty();
        }
        helper.setStringPropertyOhneEvent(DOC_PROP_LETZTES_FORMAT, formatOpt.get().name());
        return Optional.of(new ExportEinstellungen(pfadOpt.get(), formatOpt.get()));
    }

    private static ExportFormat formatAusProperty(String wert) {
        try {
            return ExportFormat.valueOf(wert);
        } catch (IllegalArgumentException e) {
            return ExportFormat.HTML_UND_PDFS;
        }
    }

    public static Optional<Path> waehlePfad(XComponentContext xContext, WorkingSpreadsheet ws) {
        var helper = new DocumentPropertiesHelper(ws);
        String letzterPfad = helper.getStringProperty(DOC_PROP_LETZTER_PFAD, "");

        String ausgewaehlt = oeffneOrdnerPicker(xContext,
                I18n.get("export.verzeichnis.dialog.titel"),
                letzterPfad.isEmpty() ? null : letzterPfad);

        if (ausgewaehlt == null) {
            return Optional.empty();
        }
        helper.setStringPropertyOhneEvent(DOC_PROP_LETZTER_PFAD, ausgewaehlt);
        return Optional.of(Path.of(ausgewaehlt));
    }

    private static String oeffneOrdnerPicker(XComponentContext xContext, String titel, String letzterPfad) {
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

    private static String pfadAlsUrl(String absoluterPfad) {
        try {
            return Path.of(absoluterPfad).toAbsolutePath().toUri().toURL().toExternalForm();
        } catch (MalformedURLException | RuntimeException e) {
            logger.debug("Pfad → URL fehlgeschlagen: {}", absoluterPfad);
            return "";
        }
    }

    private static String urlAlsPfad(String url) {
        try {
            return Path.of(URI.create(url)).toString();
        } catch (RuntimeException e) {
            return url;
        }
    }
}
