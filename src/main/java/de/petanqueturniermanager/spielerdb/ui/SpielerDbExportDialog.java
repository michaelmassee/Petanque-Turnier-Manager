package de.petanqueturniermanager.spielerdb.ui;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import com.sun.star.awt.PushButtonType;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XNameContainer;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.ui.dialogs.ExecutableDialogResults;
import com.sun.star.ui.dialogs.ExtendedFilePickerElementIds;
import com.sun.star.ui.dialogs.FilePicker;
import com.sun.star.ui.dialogs.FolderPicker;
import com.sun.star.ui.dialogs.TemplateDescription;
import com.sun.star.ui.dialogs.XFilePicker3;
import com.sun.star.ui.dialogs.XFilePickerControlAccess;
import com.sun.star.ui.dialogs.XFolderPicker2;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.comp.newrelease.ExtensionsHelper;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.konfigdialog.AbstractUnoDialog;
import de.petanqueturniermanager.spielerdb.SpielerDbConnection;
import de.petanqueturniermanager.spielerdb.SpielerDbException;
import de.petanqueturniermanager.spielerdb.export.AllExportFilter;
import de.petanqueturniermanager.spielerdb.export.ExportEntity;
import de.petanqueturniermanager.spielerdb.export.ExportFormat;
import de.petanqueturniermanager.spielerdb.export.ExportRequest;
import de.petanqueturniermanager.spielerdb.export.ExportSettings;
import de.petanqueturniermanager.spielerdb.export.SpielerDbCalcExporter;
import de.petanqueturniermanager.spielerdb.export.SpielerDbCsvExporter;
import de.petanqueturniermanager.spielerdb.export.SpielerDbExportData;
import de.petanqueturniermanager.spielerdb.export.SpielerDbExportLoader;
import de.petanqueturniermanager.spielerdb.export.SpielerDbExporter;
import de.petanqueturniermanager.spielerdb.export.SpielerDbJsonExporter;
import de.petanqueturniermanager.spielerdb.export.SpielerDbSqliteBackupExporter;

/**
 * Dialog für den Export der Spieler-DB. Bietet Format-Auswahl (CSV, JSON,
 * Calc, SQLite-Backup), Umfang-Checkboxen (Spieler/Vereine/Labels) und ein
 * Pfad-Feld, das je nach Format einen Datei- oder Ordner-Picker öffnet.
 *
 * <p>Die Junction {@code SPIELER_LABELS} wird vom {@link SpielerDbExportLoader}
 * automatisch ergänzt, wenn Spieler und Labels beide gewählt sind — daher
 * gibt es im Dialog keine eigene Checkbox dafür.
 */
public final class SpielerDbExportDialog extends AbstractUnoDialog {

    private static final Logger logger = LogManager.getLogger(SpielerDbExportDialog.class);

    private static final int B = 380;
    private static final int H = 220;

    private static final ExportFormat[] FORMATE = ExportFormat.values();

    private final SpielerDbExportLoader loader;
    private final SpielerDbConnection dbConnection;
    private final ExportSettings settings = new ExportSettings();

    @Nullable private UnoControlsHelper controls;

    public SpielerDbExportDialog(XComponentContext xContext, SpielerDbExportLoader loader,
            SpielerDbConnection dbConnection) {
        super(xContext);
        this.loader = loader;
        this.dbConnection = dbConnection;
    }

    public void zeigen() throws com.sun.star.uno.Exception {
        erstelleUndAusfuehren();
    }

    @Override protected String getTitel() { return I18n.get("spielerdb.export.dialog.titel"); }
    @Override protected int getBreite() { return B; }
    @Override protected int getHoehe() { return H; }

    @Override
    protected void erstelleFelder(XMultiComponentFactory mcf, XMultiServiceFactory xMSF,
            XNameContainer cont, XToolkit xToolkit, XWindowPeer peer,
            XPropertySet dlgProps, XDialog xDialog) throws com.sun.star.uno.Exception {

        XControlContainer xcc = Lo.qi(XControlContainer.class, xDialog);
        UnoControlsHelper c = new UnoControlsHelper(xMSF, cont, xcc);
        this.controls = c;

        c.fixedText("lblFormat", I18n.get("spielerdb.export.label.format"), 8, 10, 80, 10);
        String[] formatNamen = new String[FORMATE.length];
        for (int i = 0; i < FORMATE.length; i++) {
            formatNamen[i] = FORMATE[i].anzeigeName();
        }
        c.dropdownListBox("cmbFormat", formatNamen, 90, 8, 200, 14);

        c.fixedText("lblUmfang", I18n.get("spielerdb.export.label.umfang"), 8, 32, 80, 10);
        c.checkBox("cbSpieler", I18n.get("spielerdb.export.scope.spieler"), 90, 32, 200, 10, true);
        c.checkBox("cbVereine", I18n.get("spielerdb.export.scope.vereine"), 90, 46, 200, 10, true);
        c.checkBox("cbLabels", I18n.get("spielerdb.export.scope.labels"), 90, 60, 200, 10, true);

        c.fixedText("lblZiel", I18n.get("spielerdb.export.label.ziel"), 8, 84, 80, 10);
        c.edit("edZiel", "", 90, 82, 200, 14);
        c.button("btnWaehlen", I18n.get("spielerdb.export.btn.waehlen"), 295, 82, 75, 14);

        c.button("btnExport", I18n.get("spielerdb.export.btn.exportieren"),
                90, 175, 90, 16);
        c.button("btnAbbrechen", I18n.get("spielerdb.export.btn.abbrechen"),
                200, 175, 90, 16, (short) PushButtonType.CANCEL_value);

        c.registriereListBoxAuswahl("cmbFormat", this::beimFormatGewechselt, () -> { /* no-op */ });
        c.registriereActionListener("btnWaehlen", this::beimWaehlen);
        c.registriereActionListener("btnExport", this::beimExport);

        wendeFormatAn(ExportFormat.CSV);
    }

    private ExportFormat aktuellesFormat() {
        UnoControlsHelper c = this.controls;
        if (c == null) {
            return ExportFormat.CSV;
        }
        short idx = c.ausgewaehlterIndex("cmbFormat");
        if (idx < 0 || idx >= FORMATE.length) {
            return ExportFormat.CSV;
        }
        return FORMATE[idx];
    }

    private void beimFormatGewechselt() {
        wendeFormatAn(aktuellesFormat());
    }

    private void wendeFormatAn(ExportFormat format) {
        UnoControlsHelper c = this.controls;
        if (c == null) {
            return;
        }
        boolean scopeAktiv = format != ExportFormat.SQLITE_BACKUP;
        c.enabled("cbSpieler", scopeAktiv);
        c.enabled("cbVereine", scopeAktiv);
        c.enabled("cbLabels", scopeAktiv);

        String letzter = settings.letzterPfad(format);
        c.setzeText("edZiel", letzter == null ? "" : letzter);
    }

    private void beimWaehlen() {
        ExportFormat format = aktuellesFormat();
        try {
            String gewaehlt = oeffnePicker(format);
            if (gewaehlt != null) {
                UnoControlsHelper c = this.controls;
                if (c != null) {
                    c.setzeText("edZiel", gewaehlt);
                }
            }
        } catch (com.sun.star.uno.Exception e) {
            logger.error("Picker konnte nicht geöffnet werden", e);
            zeigeFehler(I18n.get("spielerdb.export.fehler.picker",
                    e.getMessage() == null ? "" : e.getMessage()));
        }
    }

    @Nullable
    private String oeffnePicker(ExportFormat format) throws com.sun.star.uno.Exception {
        if (format.zielTyp() == ExportFormat.ZielTyp.ORDNER) {
            return oeffneOrdnerPicker(format);
        }
        return oeffneDateiPicker(format);
    }

    @Nullable
    private String oeffneOrdnerPicker(ExportFormat format) {
        XFolderPicker2 picker = FolderPicker.create(xContext);
        picker.setTitle(I18n.get("spielerdb.export.dialog.titel"));
        String letzter = settings.letzterPfad(format);
        if (letzter != null && !letzter.isEmpty()) {
            picker.setDisplayDirectory(pfadAlsUrl(letzter));
        }
        short res = picker.execute();
        if (res != ExecutableDialogResults.OK) {
            return null;
        }
        return urlAlsPfad(picker.getDirectory());
    }

    @Nullable
    private String oeffneDateiPicker(ExportFormat format) throws com.sun.star.uno.Exception {
        XFilePicker3 picker = FilePicker.createWithMode(xContext,
                TemplateDescription.FILESAVE_AUTOEXTENSION);
        picker.setTitle(I18n.get("spielerdb.export.dialog.titel"));
        picker.appendFilter(format.anzeigeName(), "*." + format.defaultEndung());
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
        String letzter = settings.letzterPfad(format);
        if (letzter != null && !letzter.isEmpty()) {
            Path p = Path.of(letzter);
            Path eltern = p.getParent();
            if (eltern != null) {
                picker.setDisplayDirectory(pfadAlsUrl(eltern.toString()));
            }
            Path dateinameP = p.getFileName();
            if (dateinameP != null) {
                picker.setDefaultName(dateinameP.toString());
            }
        } else {
            picker.setDefaultName("spielerdb." + format.defaultEndung());
        }
        short res = picker.execute();
        if (res != ExecutableDialogResults.OK) {
            return null;
        }
        String[] dateien = picker.getFiles();
        if (dateien.length == 0) {
            return null;
        }
        return urlAlsPfad(dateien[0]);
    }

    private void beimExport() {
        UnoControlsHelper c = this.controls;
        if (c == null) {
            return;
        }
        ExportFormat format = aktuellesFormat();
        String pfadText = c.leseText("edZiel").strip();
        if (pfadText.isEmpty()) {
            zeigeFehler(I18n.get("spielerdb.export.fehler.kein_pfad"));
            return;
        }
        EnumSet<ExportEntity> scope = leseScope(format, c);
        if (format != ExportFormat.SQLITE_BACKUP && scope.isEmpty()) {
            zeigeFehler(I18n.get("spielerdb.export.fehler.kein_scope"));
            return;
        }

        Path target = Path.of(pfadText);
        ExportRequest request = new ExportRequest(format,
                format == ExportFormat.SQLITE_BACKUP ? EnumSet.allOf(ExportEntity.class) : scope,
                new AllExportFilter(), target);

        try {
            SpielerDbExportData data = ladeDaten(format, scope);
            SpielerDbExporter exporter = exporterFuer(format);
            exporter.export(data, request);
            settings.merkeLetztenPfad(format, pfadText);
            zeigeErfolg(format, scope, target, data);
        } catch (SpielerDbException e) {
            logger.error("Export fehlgeschlagen", e);
            zeigeFehler(I18n.get("spielerdb.export.fehler.allgemein",
                    e.getMessage() == null ? "" : e.getMessage()));
        }
    }

    private SpielerDbExportData ladeDaten(ExportFormat format, EnumSet<ExportEntity> scope)
            throws SpielerDbException {
        if (format == ExportFormat.SQLITE_BACKUP) {
            return loader.lade(EnumSet.noneOf(ExportEntity.class), appVersion());
        }
        return loader.lade(scope, appVersion());
    }

    private SpielerDbExporter exporterFuer(ExportFormat format) {
        return switch (format) {
            case CSV -> new SpielerDbCsvExporter();
            case JSON -> new SpielerDbJsonExporter();
            case CALC -> new SpielerDbCalcExporter(xContext);
            case SQLITE_BACKUP -> new SpielerDbSqliteBackupExporter(dbConnection);
        };
    }

    private static EnumSet<ExportEntity> leseScope(ExportFormat format, UnoControlsHelper c) {
        if (format == ExportFormat.SQLITE_BACKUP) {
            return EnumSet.noneOf(ExportEntity.class);
        }
        EnumSet<ExportEntity> scope = EnumSet.noneOf(ExportEntity.class);
        if (c.istAngekreuzt("cbSpieler")) {
            scope.add(ExportEntity.SPIELER);
        }
        if (c.istAngekreuzt("cbVereine")) {
            scope.add(ExportEntity.VEREINE);
        }
        if (c.istAngekreuzt("cbLabels")) {
            scope.add(ExportEntity.LABELS);
        }
        return scope;
    }

    private void zeigeErfolg(ExportFormat format, EnumSet<ExportEntity> scope, Path target,
            SpielerDbExportData data) {
        List<String> zeilen = new ArrayList<>();
        zeilen.add(I18n.get("spielerdb.export.fertig.kopf", target.toString()));
        zeilen.add("");
        if (format == ExportFormat.SQLITE_BACKUP) {
            zeilen.add(I18n.get("spielerdb.export.fertig.sqlite"));
        } else {
            if (scope.contains(ExportEntity.SPIELER)) {
                zeilen.add(I18n.get("spielerdb.export.fertig.spieler", data.spieler().size()));
            }
            if (scope.contains(ExportEntity.VEREINE)) {
                zeilen.add(I18n.get("spielerdb.export.fertig.vereine", data.vereine().size()));
            }
            if (scope.contains(ExportEntity.LABELS)) {
                zeilen.add(I18n.get("spielerdb.export.fertig.labels", data.labels().size()));
            }
            if (scope.contains(ExportEntity.SPIELER) && scope.contains(ExportEntity.LABELS)) {
                zeilen.add(I18n.get("spielerdb.export.fertig.junction", data.spielerLabels().size()));
            }
        }
        MessageBox.from(xContext, MessageBoxTypeEnum.INFO_OK)
                .caption(I18n.get("spielerdb.export.dialog.titel"))
                .message(String.join("\n", zeilen))
                .show();
    }

    private void zeigeFehler(String text) {
        MessageBox.from(xContext, MessageBoxTypeEnum.ERROR_OK)
                .caption(I18n.get("spielerdb.fehler.titel"))
                .message(text)
                .show();
    }

    @Nullable
    private String appVersion() {
        try {
            return ExtensionsHelper.from(xContext).getVersionNummer();
        } catch (RuntimeException e) {
            logger.debug("Plugin-Version nicht verfügbar: {}", e.getMessage());
            return null;
        }
    }

    private static String pfadAlsUrl(String absoluterPfad) {
        try {
            return Path.of(absoluterPfad).toAbsolutePath().toUri().toURL().toExternalForm();
        } catch (java.net.MalformedURLException | RuntimeException e) {
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
