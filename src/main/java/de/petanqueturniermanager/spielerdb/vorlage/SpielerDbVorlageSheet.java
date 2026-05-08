package de.petanqueturniermanager.spielerdb.vorlage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.FontWeight;
import com.sun.star.beans.XPropertySet;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.XCellRange;
import com.sun.star.util.CellProtection;
import com.sun.star.util.XProtectable;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetFreeze;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;

/**
 * Erzeugt das Vorlage-Sheet (Vorname | Nachname | Verein | Status |
 * Fehlerursache) für den Spieler-DB-Abgleich. Spalten A–C sind Eingabe-Spalten
 * (vom User befüllt), D–E werden nach jedem Abgleich vom Tool aktualisiert.
 * Existiert das Sheet bereits, wird es lediglich aktiviert – Inhalt bleibt
 * unangetastet, damit der User seine Daten nicht verliert.
 */
public final class SpielerDbVorlageSheet {

    private static final Logger logger = LogManager.getLogger(SpielerDbVorlageSheet.class);

    static final int SPALTE_VORNAME         = 0;
    static final int SPALTE_NACHNAME        = 1;
    static final int SPALTE_VEREIN          = 2;
    static final int SPALTE_STATUS          = 3;
    static final int SPALTE_FEHLERURSACHE   = 4;
    /** Header-validierte Eingabe-Spalten (A..C) – die Status/Fehler-Spalten werden vom Tool gepflegt. */
    static final int LETZTE_EINGABE_SPALTE  = SPALTE_VEREIN;
    static final int HEADER_ZEILE           = 0;

    /** Daten ab Zeile 2 (1-basiert) bis hinreichend großer Reserve-Range. */
    static final int MAX_DATEN_ZEILE = 999;

    /** Hellblauer Header-Hintergrund (RGB 0xCFE2F3). */
    private static final int HEADER_HG_FARBE = 0xCFE2F3;

    /** Tab-Farbe (mittleres Blau, hex-String wie von TurnierSheet erwartet). */
    private static final String TAB_FARBE = "1F4E79";

    /** Spaltenbreite Vor-/Nachname (1/100 mm) ≈ 4 cm. */
    private static final int SPALTENBREITE_NAME = 4000;
    /** Spaltenbreite Verein ≈ 6 cm. */
    private static final int SPALTENBREITE_VEREIN = 6000;
    /** Spaltenbreite Status ≈ 3 cm. */
    private static final int SPALTENBREITE_STATUS = 3000;
    /** Spaltenbreite Fehlerursache ≈ 8 cm. */
    private static final int SPALTENBREITE_FEHLER = 8000;

    private SpielerDbVorlageSheet() {}

    /**
     * Stellt das Vorlage-Sheet bereit (anlegen oder aktivieren) und gibt die
     * sichtbare Tabelle in den Vordergrund.
     */
    public static XSpreadsheet bereitstellen(WorkingSpreadsheet ws) throws GenerateException {
        SheetHelper sh = new SheetHelper(ws);
        String sheetName = SheetNamen.spielerDbVorlage();
        XSpreadsheet vorhandenes = sh.findByName(sheetName);
        if (vorhandenes != null) {
            sh.setActiveSheet(vorhandenes);
            return vorhandenes;
        }

        short pos = anhaengePosition(sh);
        XSpreadsheet sheet = sh.newIfNotExist(sheetName, pos, TAB_FARBE);
        if (sheet == null) {
            throw new GenerateException(I18n.get("error.tabelle.nicht.vorhanden", sheetName));
        }

        schreibeHeader(ws, sheet);
        formatiereHeader(sheet);
        setzeSpaltenbreiten(sh, sheet);
        gebeDatenbereichFrei(sheet);
        sh.setActiveSheet(sheet);
        freezeKopfzeile(sheet, ws);
        schuetzeSheet(sheet);
        return sheet;
    }

    private static short anhaengePosition(SheetHelper sh) {
        try {
            return (short) sh.getSheets().getElementNames().length;
        } catch (RuntimeException e) {
            logger.warn("Position für Vorlage-Sheet nicht ermittelbar, Fallback hintenan", e);
            return Short.MAX_VALUE;
        }
    }

    private static void schreibeHeader(WorkingSpreadsheet ws, XSpreadsheet sheet) throws GenerateException {
        RowData header = new RowData();
        header.newString(I18n.get("spielerdb.vorlage.spalte.vorname"));
        header.newString(I18n.get("spielerdb.vorlage.spalte.nachname"));
        header.newString(I18n.get("spielerdb.vorlage.spalte.verein"));
        header.newString(I18n.get("spielerdb.vorlage.spalte.status"));
        header.newString(I18n.get("spielerdb.vorlage.spalte.fehlerursache"));

        RangeData rd = new RangeData();
        rd.add(header);

        RangePosition kopfPos = RangePosition.from(SPALTE_VORNAME, HEADER_ZEILE,
                SPALTE_FEHLERURSACHE, HEADER_ZEILE);
        RangeHelper.from(sheet, ws.getWorkingSpreadsheetDocument(), kopfPos).setDataInRange(rd);
    }

    private static void formatiereHeader(XSpreadsheet sheet) {
        try {
            XCellRange header = sheet.getCellRangeByPosition(SPALTE_VORNAME, HEADER_ZEILE,
                    SPALTE_FEHLERURSACHE, HEADER_ZEILE);
            XPropertySet props = Lo.qi(XPropertySet.class, header);
            props.setPropertyValue("CharWeight", FontWeight.BOLD);
            props.setPropertyValue("CellBackColor", HEADER_HG_FARBE);
        } catch (com.sun.star.uno.Exception e) {
            logger.warn("Header-Formatierung fehlgeschlagen: {}", e.getMessage());
        }
    }

    private static void setzeSpaltenbreiten(SheetHelper sh, XSpreadsheet sheet) {
        sh.setColumnWidth(sheet, SPALTE_VORNAME,        SPALTENBREITE_NAME);
        sh.setColumnWidth(sheet, SPALTE_NACHNAME,       SPALTENBREITE_NAME);
        sh.setColumnWidth(sheet, SPALTE_VEREIN,         SPALTENBREITE_VEREIN);
        sh.setColumnWidth(sheet, SPALTE_STATUS,         SPALTENBREITE_STATUS);
        sh.setColumnWidth(sheet, SPALTE_FEHLERURSACHE,  SPALTENBREITE_FEHLER);
    }

    /**
     * Gibt den Datenbereich (Zeilen ab 2) zur Bearbeitung frei, sodass nach
     * dem anschließenden Sheet-Schutz nur die Header-Zeile gesperrt bleibt.
     * Status- und Fehlerursache-Spalten werden mit freigegeben, damit das Tool
     * sie nach dem Abgleich aktualisieren kann (Sheet-Schutz blockiert nur
     * UI-seitiges Editieren, nicht das programmatische Schreiben — die
     * Freigabe erlaubt dem User aber auch manuelle Korrekturen, falls nötig).
     */
    private static void gebeDatenbereichFrei(XSpreadsheet sheet) {
        try {
            XCellRange daten = sheet.getCellRangeByPosition(SPALTE_VORNAME, HEADER_ZEILE + 1,
                    SPALTE_FEHLERURSACHE, MAX_DATEN_ZEILE);
            XPropertySet props = Lo.qi(XPropertySet.class, daten);
            CellProtection cp = new CellProtection();
            cp.IsLocked = false;
            props.setPropertyValue("CellProtection", cp);
        } catch (com.sun.star.uno.Exception e) {
            logger.warn("Zellschutz-Freigabe Datenbereich fehlgeschlagen: {}", e.getMessage());
        }
    }

    private static void freezeKopfzeile(XSpreadsheet sheet, WorkingSpreadsheet ws) {
        try {
            SheetFreeze.from(sheet, ws).anzZeilen(1).doFreeze();
        } catch (RuntimeException e) {
            logger.warn("Freeze Row 1 fehlgeschlagen: {}", e.getMessage());
        }
    }

    private static void schuetzeSheet(XSpreadsheet sheet) {
        try {
            XProtectable prot = Lo.qi(XProtectable.class, sheet);
            if (prot != null && !prot.isProtected()) {
                prot.protect("");
            }
        } catch (RuntimeException e) {
            logger.warn("Sheet-Schutz konnte nicht aktiviert werden: {}", e.getMessage());
        }
    }
}
