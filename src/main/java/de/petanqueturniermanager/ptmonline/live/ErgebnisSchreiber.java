/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.live;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;

/**
 * Trägt ein von der Turnierleitung übernommenes Ergebnis in die beiden Ergebniszellen einer Partie ein, wie es die
 * Turnierleitung von Hand täte. Nur im Dokument-Kontext eines SheetRunners mit Blattschutz-Scope.
 */
public final class ErgebnisSchreiber {

    private ErgebnisSchreiber() {}

    /** @return {@code false}, wenn das Blatt der Partie nicht mehr existiert */
    public static boolean schreibe(WorkingSpreadsheet ws, ErgebnisOrt ort, int punkteA, int punkteB)
            throws GenerateException {
        XSpreadsheet blatt = LiveBlattLeser.blatt(ws, ort.blattSchluessel(), ort.blattName());
        if (blatt == null) {
            return false;
        }
        RangeData ergebnis = new RangeData();
        RowData zeile = ergebnis.addNewRow();
        zeile.newInt(punkteA);
        zeile.newInt(punkteB);
        RangeHelper.from(blatt, ws.getWorkingSpreadsheetDocument(),
                RangePosition.from(ort.spaltePunkteA(), ort.zeile(), ort.spaltePunkteB(), ort.zeile()))
                .setDataInRange(ergebnis);
        return true;
    }
}
