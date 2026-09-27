/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.live;

import org.jspecify.annotations.Nullable;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;

/** Gemeinsame Lesehilfen der Live-Quellen. */
final class LiveBlattLeser {

    /** Obergrenze der gelesenen Datenzeilen je Blatt. */
    static final int MAX_DATEN_ZEILEN = 1000;
    /** Obergrenze fortlaufend nummerierter Rundenblätter. */
    static final int MAX_RUNDEN = 999;

    private LiveBlattLeser() {}

    /** Sucht ein Blatt über seinen Metadaten-Schlüssel, bei alten Dokumenten über den Namen. */
    static @Nullable XSpreadsheet blatt(WorkingSpreadsheet ws, String schluessel, String fallbackName) {
        return SheetMetadataHelper.findeSheetUndHeile(ws.getWorkingSpreadsheetDocument(), schluessel, fallbackName);
    }

    /** Liest ab {@code ersteZeile} bis zu {@link #MAX_DATEN_ZEILEN} Zeilen der Spalten {@code 0..letzteSpalte}. */
    static LiveZellbereich datenzeilen(WorkingSpreadsheet ws, XSpreadsheet blatt, int ersteZeile, int letzteSpalte) {
        return LiveZellbereich.lese(ws, blatt, 0, ersteZeile, letzteSpalte, ersteZeile + MAX_DATEN_ZEILEN - 1);
    }
}
