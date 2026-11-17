/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.live;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;

/**
 * Trägt ein von der Turnierleitung übernommenes Ergebnis in die beiden Ergebniszellen einer Partie ein, wie es die
 * Turnierleitung von Hand täte. Im Turnierbaum rückt der Sieger dann über die Formeln des Baums vor. Nur im
 * Dokument-Kontext eines SheetRunners mit Blattschutz-Scope.
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
        Position a = ort.punkteA();
        Position b = ort.punkteB();
        if (a.getZeile() == b.getZeile() && b.getSpalte() == a.getSpalte() + 1) {
            schreibe(ws, blatt, a, punkteA, punkteB);
        } else {
            schreibe(ws, blatt, a, punkteA);
            schreibe(ws, blatt, b, punkteB);
        }
        return true;
    }

    /** Ein Block ab {@code start}, die Punkte nebeneinander. */
    private static void schreibe(WorkingSpreadsheet ws, XSpreadsheet blatt, Position start, int... punkte)
            throws GenerateException {
        RangeData ergebnis = new RangeData();
        RowData zeile = ergebnis.addNewRow();
        for (int wert : punkte) {
            zeile.newInt(wert);
        }
        RangeHelper.from(blatt, ws.getWorkingSpreadsheetDocument(), RangePosition.from(start.getSpalte(),
                start.getZeile(), start.getSpalte() + punkte.length - 1, start.getZeile())).setDataInRange(ergebnis);
    }
}
