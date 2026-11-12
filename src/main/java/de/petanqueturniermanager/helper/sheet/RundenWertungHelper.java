/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper.sheet;

import static com.google.common.base.Preconditions.checkNotNull;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;

/**
 * Markierung „Spielrunde wird nicht gewertet".
 * <p>
 * Eine nicht gewertete Runde bleibt als Blatt mit ihrer Nummer erhalten, zählt aber weder in
 * der Rangliste noch in der Paarungshistorie der nächsten Auslosung.
 * <p>
 * Die Markierung ist ein blattgebundener Zusatz-Schlüssel (Named Range) neben dem
 * Identitäts-Schlüssel des Spielrunden-Blatts, siehe
 * {@link SheetMetadataHelper#schluesselNichtGewertet(String)}. Sie hängt damit am Dokument (nicht
 * am UI-Fokus), überlebt Umbenennungen und verschwindet automatisch, wenn das Blatt gelöscht oder
 * neu ausgelost wird.
 */
public final class RundenWertungHelper {

    private RundenWertungHelper() {
        // Utility-Klasse – keine Instanzen
    }

    /**
     * Prüft, ob das Spielrunden-Blatt als „nicht gewertet" markiert ist.
     *
     * @param xDoc             Spreadsheet-Dokument
     * @param rundeSheet       Blatt der Spielrunde
     * @param rundenSchluessel Identitäts-Schlüssel der Spielrunde (z.B. {@code __PTM_SCHWEIZER_SPIELRUNDE_3__})
     */
    public static boolean istNichtGewertet(XSpreadsheetDocument xDoc, XSpreadsheet rundeSheet,
            String rundenSchluessel) {
        checkNotNull(xDoc);
        checkNotNull(rundeSheet);
        // Die Markierung muss auf genau dieses Blatt zeigen – ein Rest auf einem anderen Blatt
        // (z.B. nach manuellem Kopieren) zählt nicht.
        return SheetMetadataHelper.istRegistriertesSheet(xDoc, rundeSheet,
                SheetMetadataHelper.schluesselNichtGewertet(rundenSchluessel));
    }

    /**
     * Prüft, ob die Spielrunde mit dem Identitäts-Schlüssel {@code rundenSchluessel} als
     * „nicht gewertet" markiert ist. Existiert das Blatt nicht, ist die Runde nicht markiert.
     */
    public static boolean istNichtGewertet(XSpreadsheetDocument xDoc, String rundenSchluessel) {
        checkNotNull(xDoc);
        return SheetMetadataHelper.findeSheet(xDoc, rundenSchluessel)
                .map(sheet -> istNichtGewertet(xDoc, sheet, rundenSchluessel))
                .orElse(false);
    }

    /**
     * Setzt oder entfernt die Markierung „nicht gewertet" für das Spielrunden-Blatt.
     *
     * @param nichtGewertet {@code true} = Runde nicht werten, {@code false} = Runde wieder werten
     */
    public static void setzeNichtGewertet(XSpreadsheetDocument xDoc, XSpreadsheet rundeSheet,
            String rundenSchluessel, boolean nichtGewertet) {
        checkNotNull(xDoc);
        checkNotNull(rundeSheet);
        String markierung = SheetMetadataHelper.schluesselNichtGewertet(rundenSchluessel);
        if (nichtGewertet) {
            SheetMetadataHelper.schreibeSheetMetadaten(xDoc, rundeSheet, markierung);
        } else {
            SheetMetadataHelper.entferneSchluessel(xDoc, markierung);
        }
    }
}
