package de.petanqueturniermanager.spielerdb.importer;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.XComponentLoader;
import com.sun.star.lang.XComponent;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.sheet.XSpreadsheets;
import com.sun.star.table.XCell;
import com.sun.star.text.XText;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.comp.OfficeDocumentHelper;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.spielerdb.SpielerDbException;
import de.petanqueturniermanager.spielerdb.export.ExportEntity;
import de.petanqueturniermanager.spielerdb.importer.ImportRohdaten.RohLabel;
import de.petanqueturniermanager.spielerdb.importer.ImportRohdaten.RohSpieler;
import de.petanqueturniermanager.spielerdb.importer.ImportRohdaten.RohSpielerLabel;
import de.petanqueturniermanager.spielerdb.importer.ImportRohdaten.RohVerein;

/**
 * Liest eine ODS-Datei mit Sheets {@code Spieler}/{@code Vereine}/{@code Labels}/
 * {@code SpielerLabels}. <b>Strikt</b>: Sheet-Namen und Header-Zeile 1 müssen
 * exakt mit dem Export-Schema übereinstimmen — abweichende Strukturen führen
 * zum Abbruch.
 *
 * <p>Calc-Import ist als experimentell markiert; ODS ist kein robustes
 * Datenaustauschformat (User können Spalten verschieben, Filter setzen,
 * Leerzeilen einfügen). Strikte Validierung schützt vor stillem Datenverlust.
 */
public final class SpielerDbCalcImportReader implements SpielerDbImportReader {

    private static final Logger logger = LogManager.getLogger(SpielerDbCalcImportReader.class);

    /** Maximale Zeilenzahl, die eingelesen wird (Schutz gegen ewig-Schleife). */
    private static final int MAX_ZEILEN = 100_000;

    private final XComponentContext xContext;

    public SpielerDbCalcImportReader(XComponentContext xContext) {
        this.xContext = xContext;
    }

    @Override
    public ImportRohdaten read(ImportRequest request) throws SpielerDbException {
        Path datei = request.source();
        if (!Files.isRegularFile(datei)) {
            throw new SpielerDbException("ODS-Datei fehlt: " + datei);
        }
        EnumSet<ExportEntity> entities = request.entities();

        XSpreadsheetDocument doc = oeffneVersteckt(datei);
        try {
            XSpreadsheets sheets = doc.getSheets();

            List<RohSpieler> spieler = entities.contains(ExportEntity.SPIELER)
                    ? leseSpieler(sheets)
                    : List.of();
            List<RohVerein> vereine = entities.contains(ExportEntity.VEREINE)
                    ? leseVereine(sheets)
                    : List.of();
            List<RohLabel> labels = entities.contains(ExportEntity.LABELS)
                    ? leseLabels(sheets)
                    : List.of();
            List<RohSpielerLabel> junction =
                    entities.contains(ExportEntity.SPIELER) && entities.contains(ExportEntity.LABELS)
                            ? leseJunction(sheets)
                            : List.of();

            return new ImportRohdaten(spieler, vereine, labels, junction);
        } finally {
            OfficeDocumentHelper.closeDoc(doc);
        }
    }

    private XSpreadsheetDocument oeffneVersteckt(Path datei) throws SpielerDbException {
        try {
            Object desktop = xContext.getServiceManager().createInstanceWithContext(
                    "com.sun.star.frame.Desktop", xContext);
            XComponentLoader loader = Lo.qi(XComponentLoader.class, desktop);
            String url = datei.toAbsolutePath().toUri().toURL().toExternalForm();
            PropertyValue hidden = new PropertyValue();
            hidden.Name = "Hidden";
            hidden.Value = Boolean.TRUE;
            PropertyValue readonly = new PropertyValue();
            readonly.Name = "ReadOnly";
            readonly.Value = Boolean.TRUE;
            XComponent comp = loader.loadComponentFromURL(url, "_blank", 0,
                    new PropertyValue[] { hidden, readonly });
            XSpreadsheetDocument doc = Lo.qi(XSpreadsheetDocument.class, comp);
            if (doc == null) {
                throw new SpielerDbException("ODS-Datei ist kein Calc-Dokument: " + datei);
            }
            return doc;
        } catch (com.sun.star.uno.Exception | MalformedURLException e) {
            throw new SpielerDbException("ODS-Öffnen fehlgeschlagen: " + datei, e);
        }
    }

    private static List<RohSpieler> leseSpieler(XSpreadsheets sheets) throws SpielerDbException {
        XSpreadsheet sheet = holePflichtSheet(sheets, "Spieler");
        validiereHeader(sheet, "Spieler", "nr", "vorname", "nachname",
                "vereinNr", "vereinName", "lizenznr");
        List<RohSpieler> erg = new ArrayList<>();
        for (int row = 1; row < MAX_ZEILEN; row++) {
            String vorname = leseString(sheet, 1, row);
            String nachname = leseString(sheet, 2, row);
            if (vorname.isEmpty() && nachname.isEmpty()
                    && leseString(sheet, 0, row).isEmpty()) {
                break;
            }
            erg.add(new RohSpieler(
                    leseInteger(sheet, 0, row),
                    vorname,
                    nachname,
                    leseInteger(sheet, 3, row),
                    leerAlsNull(leseString(sheet, 4, row)),
                    leerAlsNull(leseString(sheet, 5, row))));
        }
        return erg;
    }

    private static List<RohVerein> leseVereine(XSpreadsheets sheets) throws SpielerDbException {
        XSpreadsheet sheet = holePflichtSheet(sheets, "Vereine");
        validiereHeader(sheet, "Vereine", "nr", "name");
        List<RohVerein> erg = new ArrayList<>();
        for (int row = 1; row < MAX_ZEILEN; row++) {
            String name = leseString(sheet, 1, row);
            if (name.isEmpty() && leseString(sheet, 0, row).isEmpty()) {
                break;
            }
            erg.add(new RohVerein(leseInteger(sheet, 0, row), name));
        }
        return erg;
    }

    private static List<RohLabel> leseLabels(XSpreadsheets sheets) throws SpielerDbException {
        XSpreadsheet sheet = holePflichtSheet(sheets, "Labels");
        validiereHeader(sheet, "Labels", "nr", "name");
        List<RohLabel> erg = new ArrayList<>();
        for (int row = 1; row < MAX_ZEILEN; row++) {
            String name = leseString(sheet, 1, row);
            if (name.isEmpty() && leseString(sheet, 0, row).isEmpty()) {
                break;
            }
            erg.add(new RohLabel(leseInteger(sheet, 0, row), name));
        }
        return erg;
    }

    private static List<RohSpielerLabel> leseJunction(XSpreadsheets sheets)
            throws SpielerDbException {
        XSpreadsheet sheet = holePflichtSheet(sheets, "SpielerLabels");
        validiereHeader(sheet, "SpielerLabels", "spielerNr", "labelNr");
        List<RohSpielerLabel> erg = new ArrayList<>();
        for (int row = 1; row < MAX_ZEILEN; row++) {
            Integer sNr = leseInteger(sheet, 0, row);
            Integer lNr = leseInteger(sheet, 1, row);
            if (sNr == null && lNr == null) {
                break;
            }
            if (sNr == null || lNr == null) {
                throw new SpielerDbException("Junction-Sheet 'SpielerLabels' Zeile "
                        + (row + 1) + ": leere NR");
            }
            erg.add(new RohSpielerLabel(sNr, lNr));
        }
        return erg;
    }

    private static XSpreadsheet holePflichtSheet(XSpreadsheets sheets, String name)
            throws SpielerDbException {
        if (!sheets.hasByName(name)) {
            throw new SpielerDbException("Sheet '" + name + "' fehlt im ODS-Dokument");
        }
        try {
            XSpreadsheet sheet = Lo.qi(XSpreadsheet.class, sheets.getByName(name));
            if (sheet == null) {
                throw new SpielerDbException("Sheet '" + name + "' kann nicht gelesen werden");
            }
            return sheet;
        } catch (com.sun.star.uno.Exception e) {
            throw new SpielerDbException("Sheet '" + name + "' kann nicht gelesen werden", e);
        }
    }

    private static void validiereHeader(XSpreadsheet sheet, String sheetName, String... erwartet)
            throws SpielerDbException {
        for (int i = 0; i < erwartet.length; i++) {
            String wert = leseString(sheet, i, 0);
            if (!erwartet[i].equals(wert)) {
                throw new SpielerDbException("Sheet '" + sheetName + "' Spalte " + (i + 1)
                        + " erwartet Header '" + erwartet[i] + "', gefunden '" + wert + "'");
            }
        }
    }

    private static String leseString(XSpreadsheet sheet, int col, int row) {
        try {
            XCell cell = sheet.getCellByPosition(col, row);
            XText text = Lo.qi(XText.class, cell);
            return text == null ? "" : text.getString();
        } catch (com.sun.star.lang.IndexOutOfBoundsException e) {
            logger.debug("Zelle ({}, {}) nicht zugreifbar: {}", col, row, e.getMessage());
            return "";
        }
    }

    @Nullable
    private static Integer leseInteger(XSpreadsheet sheet, int col, int row) {
        String s = leseString(sheet, col, row).strip();
        if (s.isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(s);
        } catch (NumberFormatException e) {
            // Calc liefert numerische Zellen als „1234" oder „1234,0" je nach Locale.
            try {
                return (int) Double.parseDouble(s.replace(',', '.'));
            } catch (NumberFormatException ex) {
                return null;
            }
        }
    }

    @Nullable
    private static String leerAlsNull(String s) {
        return s.isEmpty() ? null : s;
    }
}
