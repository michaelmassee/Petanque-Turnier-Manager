package de.petanqueturniermanager.spielerdb.ui;

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
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxResult;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.konfigdialog.AbstractUnoDialog;
import de.petanqueturniermanager.spielerdb.SpielerDbConnection;
import de.petanqueturniermanager.spielerdb.SpielerDbDateiFormat;
import de.petanqueturniermanager.spielerdb.SpielerDbException;
import de.petanqueturniermanager.spielerdb.export.ExportEntity;
import de.petanqueturniermanager.spielerdb.importer.ImportErgebnis;
import de.petanqueturniermanager.spielerdb.importer.ImportModus;
import de.petanqueturniermanager.spielerdb.importer.ImportRequest;
import de.petanqueturniermanager.spielerdb.importer.ImportRohdaten;
import de.petanqueturniermanager.spielerdb.importer.ImportSettings;
import de.petanqueturniermanager.spielerdb.importer.ImportWarnung;
import de.petanqueturniermanager.spielerdb.importer.SpielerDbCalcImportReader;
import de.petanqueturniermanager.spielerdb.importer.SpielerDbCsvImportReader;
import de.petanqueturniermanager.spielerdb.importer.SpielerDbImportReader;
import de.petanqueturniermanager.spielerdb.importer.SpielerDbImportValidator;
import de.petanqueturniermanager.spielerdb.importer.SpielerDbImporter;
import de.petanqueturniermanager.spielerdb.importer.SpielerDbJsonImportReader;
import de.petanqueturniermanager.spielerdb.importer.SpielerDbSqliteRestoreImporter;
import de.petanqueturniermanager.spielerdb.importer.SpielerDbValidationException;
import de.petanqueturniermanager.spielerdb.importer.ValidierteDaten;

/**
 * Dialog für den Import der Spieler-DB. Bietet Format-Auswahl (CSV, JSON, Calc,
 * SQLite-Restore), Umfang-Checkboxen (Spieler/Vereine/Labels) und einen
 * Modus-Dropdown („Nur neue Daten" / „Vorhandene aktualisieren" /
 * „Duplikate separat importieren"). Für SQLite-Restore werden Scope und Modus
 * deaktiviert — die Backup-Datei ersetzt die DB komplett.
 */
public final class SpielerDbImportDialog extends AbstractUnoDialog {

    private static final Logger logger = LogManager.getLogger(SpielerDbImportDialog.class);

    private static final int B = 380;
    private static final int H = 250;

    private static final SpielerDbDateiFormat[] FORMATE = SpielerDbDateiFormat.values();
    private static final ImportModus[] MODI = ImportModus.values();

    private final SpielerDbConnection dbConnection;
    private final ImportSettings settings = new ImportSettings();

    @Nullable private UnoControlsHelper controls;

    public SpielerDbImportDialog(XComponentContext xContext, SpielerDbConnection dbConnection) {
        super(xContext);
        this.dbConnection = dbConnection;
    }

    public void zeigen() throws com.sun.star.uno.Exception {
        erstelleUndAusfuehren();
    }

    @Override protected String getTitel() { return I18n.get("spielerdb.import.dialog.titel"); }
    @Override protected int getBreite() { return B; }
    @Override protected int getHoehe() { return H; }

    @Override
    protected void erstelleFelder(XMultiComponentFactory mcf, XMultiServiceFactory xMSF,
            XNameContainer cont, XToolkit xToolkit, XWindowPeer peer,
            XPropertySet dlgProps, XDialog xDialog) throws com.sun.star.uno.Exception {

        XControlContainer xcc = Lo.qi(XControlContainer.class, xDialog);
        UnoControlsHelper c = new UnoControlsHelper(xMSF, cont, xcc);
        this.controls = c;

        c.fixedText("lblFormat", I18n.get("spielerdb.import.label.format"), 8, 10, 80, 10);
        String[] formatNamen = new String[FORMATE.length];
        for (int i = 0; i < FORMATE.length; i++) {
            formatNamen[i] = FORMATE[i].anzeigeName();
        }
        c.dropdownListBox("cmbFormat", formatNamen, 90, 8, 200, 14);

        c.fixedText("lblUmfang", I18n.get("spielerdb.import.label.umfang"), 8, 32, 80, 10);
        c.checkBox("cbSpieler", I18n.get("spielerdb.import.scope.spieler"), 90, 32, 200, 10, true);
        c.checkBox("cbVereine", I18n.get("spielerdb.import.scope.vereine"), 90, 46, 200, 10, true);
        c.checkBox("cbLabels", I18n.get("spielerdb.import.scope.labels"), 90, 60, 200, 10, true);

        c.fixedText("lblModus", I18n.get("spielerdb.import.label.modus"), 8, 84, 80, 10);
        String[] modusNamen = new String[MODI.length];
        for (int i = 0; i < MODI.length; i++) {
            modusNamen[i] = I18n.get(modusI18nKey(MODI[i]));
        }
        c.dropdownListBox("cmbModus", modusNamen, 90, 82, 200, 14);
        // Default: AKTUALISIEREN (Index 1)
        c.setzeAusgewaehlteIndizes("cmbModus", new short[] { 1 });

        c.fixedText("lblQuelle", I18n.get("spielerdb.import.label.quelle"), 8, 110, 80, 10);
        c.edit("edQuelle", "", 90, 108, 200, 14);
        c.button("btnWaehlen", I18n.get("spielerdb.import.btn.waehlen"), 295, 108, 75, 14);

        c.button("btnImport", I18n.get("spielerdb.import.btn.importieren"),
                90, 205, 90, 16);
        c.button("btnAbbrechen", I18n.get("spielerdb.import.btn.abbrechen"),
                200, 205, 90, 16, (short) PushButtonType.CANCEL_value);

        c.registriereListBoxAuswahl("cmbFormat", this::beimFormatGewechselt, () -> { /* no-op */ });
        c.registriereActionListener("btnWaehlen", this::beimWaehlen);
        c.registriereActionListener("btnImport", this::beimImport);

        wendeFormatAn(SpielerDbDateiFormat.CSV);
    }

    private static String modusI18nKey(ImportModus modus) {
        return switch (modus) {
            case NUR_NEUE -> "spielerdb.import.modus.nur_neue";
            case AKTUALISIEREN -> "spielerdb.import.modus.aktualisieren";
            case DUPLIKATE_SEPARAT -> "spielerdb.import.modus.duplikate";
        };
    }

    private SpielerDbDateiFormat aktuellesFormat() {
        UnoControlsHelper c = this.controls;
        if (c == null) {
            return SpielerDbDateiFormat.CSV;
        }
        short idx = c.ausgewaehlterIndex("cmbFormat");
        if (idx < 0 || idx >= FORMATE.length) {
            return SpielerDbDateiFormat.CSV;
        }
        return FORMATE[idx];
    }

    private ImportModus aktuellerModus() {
        UnoControlsHelper c = this.controls;
        if (c == null) {
            return ImportModus.AKTUALISIEREN;
        }
        short idx = c.ausgewaehlterIndex("cmbModus");
        if (idx < 0 || idx >= MODI.length) {
            return ImportModus.AKTUALISIEREN;
        }
        return MODI[idx];
    }

    private void beimFormatGewechselt() {
        wendeFormatAn(aktuellesFormat());
    }

    private void wendeFormatAn(SpielerDbDateiFormat format) {
        UnoControlsHelper c = this.controls;
        if (c == null) {
            return;
        }
        boolean konfigAktiv = format != SpielerDbDateiFormat.SQLITE_BACKUP;
        // Flache CSV deckt nur Spieler+Vereinsname ab; Vereine/Labels-Scope hat
        // keine Wirkung. Im UI als deaktiviert darstellen.
        boolean csv = format == SpielerDbDateiFormat.CSV;
        c.enabled("cbSpieler", konfigAktiv);
        c.enabled("cbVereine", konfigAktiv && !csv);
        c.enabled("cbLabels", konfigAktiv && !csv);
        c.enabled("cmbModus", konfigAktiv);

        String letzter = settings.letzterPfad(format);
        c.setzeText("edQuelle", letzter == null ? "" : letzter);
    }

    private void beimWaehlen() {
        SpielerDbDateiFormat format = aktuellesFormat();
        try {
            String gewaehlt = oeffnePicker(format);
            if (gewaehlt != null) {
                UnoControlsHelper c = this.controls;
                if (c != null) {
                    c.setzeText("edQuelle", gewaehlt);
                }
            }
        } catch (com.sun.star.uno.Exception e) {
            logger.error("Picker konnte nicht geöffnet werden", e);
            zeigeFehler(I18n.get("spielerdb.import.fehler.picker",
                    e.getMessage() == null ? "" : e.getMessage()));
        }
    }

    @Nullable
    private String oeffnePicker(SpielerDbDateiFormat format) throws com.sun.star.uno.Exception {
        String titel = I18n.get("spielerdb.import.dialog.titel");
        String letzter = settings.letzterPfad(format);
        if (format.zielTyp() == SpielerDbDateiFormat.ZielTyp.ORDNER) {
            return SpielerDbPickerHelfer.oeffneOrdnerPicker(xContext, titel, letzter);
        }
        return SpielerDbPickerHelfer.oeffneDateiPicker(xContext, titel, format, letzter,
                SpielerDbPickerHelfer.PickerModus.OEFFNEN);
    }

    private void beimImport() {
        UnoControlsHelper c = this.controls;
        if (c == null) {
            return;
        }
        SpielerDbDateiFormat format = aktuellesFormat();
        String pfadText = c.leseText("edQuelle").strip();
        if (pfadText.isEmpty()) {
            zeigeFehler(I18n.get("spielerdb.import.fehler.kein_pfad"));
            return;
        }
        Path quelle = Path.of(pfadText);

        if (format == SpielerDbDateiFormat.SQLITE_BACKUP) {
            fuehreSqliteRestoreAus(quelle);
            settings.merkeLetztenPfad(format, pfadText);
            return;
        }

        EnumSet<ExportEntity> scope = scopeFuer(format, c);
        if (scope.isEmpty()) {
            zeigeFehler(I18n.get("spielerdb.import.fehler.kein_scope"));
            return;
        }
        ImportModus modus = aktuellerModus();
        ImportRequest request = new ImportRequest(format, scope, quelle, modus, false);

        try {
            SpielerDbImportReader reader = readerFuer(format);
            ImportRohdaten roh = reader.read(request);
            ValidierteDaten vd = new SpielerDbImportValidator().validiere(roh);
            ImportErgebnis erg = new SpielerDbImporter(dbConnection).importiere(vd, request);
            settings.merkeLetztenPfad(format, pfadText);
            zeigeErfolg(erg);
        } catch (SpielerDbValidationException e) {
            logger.error("Import-Validierung fehlgeschlagen", e);
            zeigeFehler(I18n.get("spielerdb.import.fehler.validierung",
                    String.join("\n• ", e.fehler())));
        } catch (SpielerDbException e) {
            logger.error("Import fehlgeschlagen", e);
            zeigeFehler(I18n.get("spielerdb.import.fehler.allgemein",
                    e.getMessage() == null ? "" : e.getMessage()));
        }
    }

    private void fuehreSqliteRestoreAus(Path quelle) {
        MessageBoxResult antwort = MessageBox.from(xContext, MessageBoxTypeEnum.WARN_YES_NO)
                .caption(I18n.get("spielerdb.import.dialog.titel"))
                .message(I18n.get("spielerdb.import.restore.bestaetigung", quelle.toString()))
                .show();
        if (antwort != MessageBoxResult.YES) {
            return;
        }
        try {
            new SpielerDbSqliteRestoreImporter().restore(quelle);
            MessageBox.from(xContext, MessageBoxTypeEnum.INFO_OK)
                    .caption(I18n.get("spielerdb.import.dialog.titel"))
                    .message(I18n.get("spielerdb.import.restore.fertig"))
                    .show();
        } catch (SpielerDbException e) {
            logger.error("SQLite-Restore fehlgeschlagen", e);
            zeigeFehler(I18n.get("spielerdb.import.fehler.allgemein",
                    e.getMessage() == null ? "" : e.getMessage()));
        }
    }

    private SpielerDbImportReader readerFuer(SpielerDbDateiFormat format)
            throws SpielerDbException {
        return switch (format) {
            case CSV -> new SpielerDbCsvImportReader();
            case JSON -> new SpielerDbJsonImportReader();
            case CALC -> new SpielerDbCalcImportReader(xContext);
            case SQLITE_BACKUP ->
                    throw new SpielerDbException("SQLite-Restore läuft über separaten Pfad");
        };
    }

    private static EnumSet<ExportEntity> scopeFuer(SpielerDbDateiFormat format, UnoControlsHelper c) {
        if (format == SpielerDbDateiFormat.CSV) {
            // Flache CSV deckt nur Spieler+Vereinsname ab — Scope ist fix.
            return EnumSet.of(ExportEntity.SPIELER);
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

    private void zeigeErfolg(ImportErgebnis erg) {
        List<String> zeilen = new ArrayList<>();
        zeilen.add(I18n.get("spielerdb.import.fertig.kopf"));
        zeilen.add("");
        zeilen.add(I18n.get("spielerdb.import.fertig.spieler",
                erg.spielerEingefuegt(), erg.spielerAktualisiert(), erg.spielerUebersprungen()));
        zeilen.add(I18n.get("spielerdb.import.fertig.vereine",
                erg.vereineEingefuegt(), erg.vereineAktualisiert(), erg.vereineUebersprungen()));
        zeilen.add(I18n.get("spielerdb.import.fertig.labels",
                erg.labelsEingefuegt(), erg.labelsAktualisiert(), erg.labelsUebersprungen()));
        zeilen.add(I18n.get("spielerdb.import.fertig.junction", erg.junctionEingefuegt()));
        if (!erg.warnungen().isEmpty()) {
            zeilen.add("");
            zeilen.add(I18n.get("spielerdb.import.fertig.warnungen", erg.warnungen().size()));
            for (ImportWarnung w : erg.warnungen()) {
                zeilen.add("• " + w.text());
            }
        }
        MessageBox.from(xContext, MessageBoxTypeEnum.INFO_OK)
                .caption(I18n.get("spielerdb.import.dialog.titel"))
                .message(String.join("\n", zeilen))
                .show();
    }

    private void zeigeFehler(String text) {
        MessageBox.from(xContext, MessageBoxTypeEnum.ERROR_OK)
                .caption(I18n.get("spielerdb.fehler.titel"))
                .message(text)
                .show();
    }
}
