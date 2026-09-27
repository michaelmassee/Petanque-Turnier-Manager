/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.live;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import com.sun.star.container.NoSuchElementException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheets;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;

/** Gemeinsame Lesehilfen der Live-Quellen. */
final class LiveBlattLeser {

    private static final Logger logger = LogManager.getLogger(LiveBlattLeser.class);

    /** Obergrenze der gelesenen Datenzeilen je Blatt. */
    static final int MAX_DATEN_ZEILEN = 1000;
    /** Obergrenze fortlaufend nummerierter Rundenblätter. */
    static final int MAX_RUNDEN = 999;

    private LiveBlattLeser() {}

    /**
     * Sucht ein Blatt über seinen Metadaten-Schlüssel, bei alten Dokumenten über den Namen. Anders als
     * {@link SheetMetadataHelper#findeSheetUndHeile} wird nichts nachgetragen: die Live-Übertragung läuft auch im
     * Hintergrund und darf das Dokument nicht verändern.
     */
    static @Nullable XSpreadsheet blatt(WorkingSpreadsheet ws, String schluessel, String fallbackName) {
        var xDoc = ws.getWorkingSpreadsheetDocument();
        Optional<XSpreadsheet> gefunden = SheetMetadataHelper.findeSheet(xDoc, schluessel);
        if (gefunden.isPresent()) {
            return gefunden.get();
        }
        XSpreadsheets blaetter = xDoc.getSheets();
        if (!blaetter.hasByName(fallbackName)) {
            return null;
        }
        try {
            return Lo.qi(XSpreadsheet.class, blaetter.getByName(fallbackName));
        } catch (NoSuchElementException | WrappedTargetException e) {
            logger.debug("PTM-Online Live: Blatt '{}' nicht lesbar", fallbackName, e);
            return null;
        }
    }

    /** Liest ab {@code ersteZeile} bis zu {@link #MAX_DATEN_ZEILEN} Zeilen der Spalten {@code 0..letzteSpalte}. */
    static LiveZellbereich datenzeilen(WorkingSpreadsheet ws, XSpreadsheet blatt, int ersteZeile, int letzteSpalte) {
        return LiveZellbereich.lese(ws, blatt, 0, ersteZeile, letzteSpalte, ersteZeile + MAX_DATEN_ZEILEN - 1);
    }
}
