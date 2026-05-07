package de.petanqueturniermanager.spielerdb.export;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import com.sun.star.awt.FontWeight;
import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XIndexAccess;
import com.sun.star.frame.XComponentLoader;
import com.sun.star.frame.XStorable;
import com.sun.star.io.IOException;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.sheet.XSpreadsheets;
import com.sun.star.table.XCellRange;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.comp.OfficeDocumentHelper;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.spielerdb.LabelDatensatz;
import de.petanqueturniermanager.spielerdb.SpielerDbException;
import de.petanqueturniermanager.spielerdb.SpielerMitVerein;
import de.petanqueturniermanager.spielerdb.VereinDatensatz;

/**
 * Schreibt einen Calc-Export in eine ODS-Datei. Erstellt ein verstecktes
 * Calc-Dokument, schreibt pro gewählter Entity ein Sheet als Block (via
 * {@link RangeHelper}), formatiert die Header-Zeile fett und speichert das
 * Ergebnis. Es wird kein Frame/Window sichtbar — der Export verändert den
 * UI-Fokus nicht.
 */
public final class SpielerDbCalcExporter implements SpielerDbExporter {

    private static final Logger logger = LogManager.getLogger(SpielerDbCalcExporter.class);

    /** ODS-Filtername für {@code XStorable.storeToURL}. */
    private static final String FILTER_ODS = "calc8";

    private final XComponentContext xContext;

    public SpielerDbCalcExporter(XComponentContext xContext) {
        this.xContext = xContext;
    }

    @Override
    public void export(SpielerDbExportData data, ExportRequest request) throws SpielerDbException {
        XSpreadsheetDocument doc = erstelleVerstecktesCalc();
        if (doc == null) {
            throw new SpielerDbException("Verstecktes Calc-Dokument konnte nicht erstellt werden");
        }
        try {
            schreibeAlleSheets(doc, data, request);
            speichereAls(doc, request.target().toAbsolutePath().toString());
        } finally {
            OfficeDocumentHelper.closeDoc(doc);
        }
    }

    @Nullable
    private XSpreadsheetDocument erstelleVerstecktesCalc() {
        try {
            Object desktop = xContext.getServiceManager().createInstanceWithContext(
                    "com.sun.star.frame.Desktop", xContext);
            XComponentLoader loader = Lo.qi(XComponentLoader.class, desktop);
            // OfficeDocumentHelper.createCalc() setzt intern Hidden=true.
            return OfficeDocumentHelper.from(loader).createCalc();
        } catch (com.sun.star.uno.Exception e) {
            logger.error("Calc-Dokument-Erzeugung fehlgeschlagen", e);
            return null;
        }
    }

    private void schreibeAlleSheets(XSpreadsheetDocument doc, SpielerDbExportData data,
            ExportRequest request) throws SpielerDbException {
        XSpreadsheets sheets = doc.getSheets();
        List<String> geschriebeneSheetNamen = new ArrayList<>();

        if (request.entities().contains(ExportEntity.SPIELER)) {
            geschriebeneSheetNamen.add(schreibeSpielerSheet(doc, sheets, data));
        }
        if (request.entities().contains(ExportEntity.VEREINE)) {
            geschriebeneSheetNamen.add(schreibeVereineSheet(doc, sheets, data));
        }
        if (request.entities().contains(ExportEntity.LABELS)) {
            geschriebeneSheetNamen.add(schreibeLabelsSheet(doc, sheets, data));
        }
        if (request.entities().contains(ExportEntity.SPIELER)
                && request.entities().contains(ExportEntity.LABELS)) {
            geschriebeneSheetNamen.add(schreibeJunctionSheet(doc, sheets, data));
        }

        if (geschriebeneSheetNamen.isEmpty()) {
            throw new SpielerDbException("Keine Entities im Scope — Export würde leeres Dokument erzeugen");
        }
        entferneStandardSheet(sheets, geschriebeneSheetNamen);
    }

    private String schreibeSpielerSheet(XSpreadsheetDocument doc, XSpreadsheets sheets,
            SpielerDbExportData data) throws SpielerDbException {
        String name = "Spieler";
        XSpreadsheet sheet = sheetAnlegen(sheets, name);
        String[] header = { "nr", "vorname", "nachname", "vereinNr", "vereinName", "lizenznr" };
        RangeData rd = new RangeData();
        rd.add(headerZeile(header));
        for (SpielerMitVerein s : data.spieler()) {
            RowData row = new RowData();
            row.newInt(s.nr());
            row.newString(s.vorname());
            row.newString(s.nachname());
            if (s.vereinNr() == null) {
                row.newEmpty();
            } else {
                row.newInt(s.vereinNr());
            }
            row.newString(s.vereinName() == null ? "" : s.vereinName());
            row.newString(s.lizenznr() == null ? "" : s.lizenznr());
            rd.add(row);
        }
        schreibeBlock(doc, sheet, rd, header.length);
        return name;
    }

    private String schreibeVereineSheet(XSpreadsheetDocument doc, XSpreadsheets sheets,
            SpielerDbExportData data) throws SpielerDbException {
        String name = "Vereine";
        XSpreadsheet sheet = sheetAnlegen(sheets, name);
        String[] header = { "nr", "name" };
        RangeData rd = new RangeData();
        rd.add(headerZeile(header));
        for (VereinDatensatz v : data.vereine()) {
            RowData row = new RowData();
            if (v.nr() == null) {
                row.newEmpty();
            } else {
                row.newInt(v.nr());
            }
            row.newString(v.name());
            rd.add(row);
        }
        schreibeBlock(doc, sheet, rd, header.length);
        return name;
    }

    private String schreibeLabelsSheet(XSpreadsheetDocument doc, XSpreadsheets sheets,
            SpielerDbExportData data) throws SpielerDbException {
        String name = "Labels";
        XSpreadsheet sheet = sheetAnlegen(sheets, name);
        String[] header = { "nr", "name" };
        RangeData rd = new RangeData();
        rd.add(headerZeile(header));
        for (LabelDatensatz l : data.labels()) {
            RowData row = new RowData();
            if (l.nr() == null) {
                row.newEmpty();
            } else {
                row.newInt(l.nr());
            }
            row.newString(l.name());
            rd.add(row);
        }
        schreibeBlock(doc, sheet, rd, header.length);
        return name;
    }

    private String schreibeJunctionSheet(XSpreadsheetDocument doc, XSpreadsheets sheets,
            SpielerDbExportData data) throws SpielerDbException {
        String name = "SpielerLabels";
        XSpreadsheet sheet = sheetAnlegen(sheets, name);
        String[] header = { "spielerNr", "labelNr" };
        RangeData rd = new RangeData();
        rd.add(headerZeile(header));
        for (SpielerLabelZuordnung z : data.spielerLabels()) {
            RowData row = new RowData();
            row.newInt(z.spielerNr());
            row.newInt(z.labelNr());
            rd.add(row);
        }
        schreibeBlock(doc, sheet, rd, header.length);
        return name;
    }

    private static RowData headerZeile(String[] spalten) {
        RowData row = new RowData();
        for (String s : spalten) {
            row.newString(s);
        }
        return row;
    }

    private static XSpreadsheet sheetAnlegen(XSpreadsheets sheets, String name)
            throws SpielerDbException {
        try {
            short pos = (short) Lo.qi(XIndexAccess.class, sheets).getCount();
            sheets.insertNewByName(name, pos);
            return Lo.qi(XSpreadsheet.class, sheets.getByName(name));
        } catch (com.sun.star.uno.Exception e) {
            throw new SpielerDbException("Sheet '" + name + "' konnte nicht angelegt werden", e);
        }
    }

    private static void schreibeBlock(XSpreadsheetDocument doc, XSpreadsheet sheet,
            RangeData rd, int spalten) throws SpielerDbException {
        if (rd.isEmpty()) {
            return;
        }
        int letzteZeile = rd.size() - 1;
        RangePosition pos = RangePosition.from(0, 0, spalten - 1, letzteZeile);
        try {
            RangeHelper.from(sheet, doc, pos).setDataInRange(rd);
        } catch (GenerateException e) {
            throw new SpielerDbException("Block-Schreibvorgang fehlgeschlagen", e);
        }
        macheKopfZeileFett(sheet, spalten);
    }

    private static void macheKopfZeileFett(XSpreadsheet sheet, int spalten) {
        try {
            XCellRange header = sheet.getCellRangeByPosition(0, 0, spalten - 1, 0);
            XPropertySet props = Lo.qi(XPropertySet.class, header);
            props.setPropertyValue("CharWeight", FontWeight.BOLD);
        } catch (com.sun.star.uno.Exception e) {
            // Formatierung ist Polish — Export bleibt nutzbar, also nur loggen.
            logger.warn("Header-Formatierung fehlgeschlagen: {}", e.getMessage());
        }
    }

    /**
     * Entfernt das von Calc beim {@code createCalc()} angelegte Default-Sheet
     * (Locale-abhängig „Sheet1"/„Tabelle1"/…), wenn es nicht zu den selbst
     * geschriebenen Sheets gehört.
     */
    private static void entferneStandardSheet(XSpreadsheets sheets,
            List<String> geschriebene) {
        try {
            XIndexAccess idx = Lo.qi(XIndexAccess.class, sheets);
            int anz = idx.getCount();
            for (int i = 0; i < anz; i++) {
                XSpreadsheet sh = Lo.qi(XSpreadsheet.class, idx.getByIndex(i));
                if (sh == null) {
                    continue;
                }
                String name = Lo.qi(com.sun.star.container.XNamed.class, sh).getName();
                if (!geschriebene.contains(name)) {
                    sheets.removeByName(name);
                    return;
                }
            }
        } catch (com.sun.star.uno.Exception e) {
            // Default-Sheet bleibt drin — Export funktioniert trotzdem.
            logger.warn("Default-Sheet konnte nicht entfernt werden: {}", e.getMessage());
        }
    }

    private static void speichereAls(XSpreadsheetDocument doc, String absoluterPfad)
            throws SpielerDbException {
        XStorable storable = Lo.qi(XStorable.class, doc);
        if (storable == null) {
            throw new SpielerDbException("XStorable nicht verfügbar");
        }
        PropertyValue[] props = {
                propertyValue("FilterName", FILTER_ODS),
                propertyValue("Overwrite", Boolean.TRUE)
        };
        try {
            String url = pfadAlsUrl(absoluterPfad);
            storable.storeToURL(url, props);
        } catch (IOException | MalformedURLException e) {
            throw new SpielerDbException("Calc-Export-Speichern fehlgeschlagen: " + absoluterPfad, e);
        }
    }

    private static String pfadAlsUrl(String absoluterPfad) throws MalformedURLException {
        return java.nio.file.Paths.get(absoluterPfad).toUri().toURL().toExternalForm();
    }

    private static PropertyValue propertyValue(String name, Object value) {
        PropertyValue pv = new PropertyValue();
        pv.Name = name;
        pv.Value = value;
        return pv;
    }
}
