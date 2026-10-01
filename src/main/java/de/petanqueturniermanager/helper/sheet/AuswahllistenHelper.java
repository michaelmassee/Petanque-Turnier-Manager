/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper.sheet;

import java.util.List;
import java.util.stream.Collectors;

import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertySet;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.sheet.TableValidationVisibility;
import com.sun.star.sheet.ValidationAlertStyle;
import com.sun.star.sheet.ValidationType;
import com.sun.star.sheet.XSheetCondition;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.XCellRange;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.position.RangePosition;

/**
 * Auswahllisten (Gültigkeitsprüfung „Liste“) für einen Zellbereich: ein Aufruf je Bereich, nicht je Zelle. Leere
 * Zellen bleiben erlaubt; andere Werte lehnt Calc mit einer Fehlermeldung ab.
 */
public final class AuswahllistenHelper {

    private static final String VALIDATION = "Validation";

    private AuswahllistenHelper() {}

    /** Setzt für {@code bereich} eine Auswahlliste mit den Werten in der angegebenen Reihenfolge. */
    public static void setzeAuswahlliste(XSpreadsheet sheet, RangePosition bereich, List<String> werte)
            throws GenerateException {
        String liste = werte.stream().map(wert -> "\"" + wert.replace("\"", "\"\"") + "\"")
                .collect(Collectors.joining(";"));
        setzeGueltigkeit(sheet, bereich, ValidationType.LIST, liste);
    }

    /** Hebt jede Gültigkeitsprüfung in {@code bereich} auf. */
    public static void entferneAuswahlliste(XSpreadsheet sheet, RangePosition bereich) throws GenerateException {
        setzeGueltigkeit(sheet, bereich, ValidationType.ANY, "");
    }

    private static void setzeGueltigkeit(XSpreadsheet sheet, RangePosition bereich, ValidationType typ, String formel)
            throws GenerateException {
        try {
            XCellRange range = sheet.getCellRangeByPosition(bereich.getStartSpalte(), bereich.getStartZeile(),
                    bereich.getEndeSpalte(), bereich.getEndeZeile());
            XPropertySet bereichEigenschaften = Lo.qi(XPropertySet.class, range);
            Object gueltigkeit = bereichEigenschaften.getPropertyValue(VALIDATION);
            XPropertySet eigenschaften = Lo.qi(XPropertySet.class, gueltigkeit);
            eigenschaften.setPropertyValue("Type", typ);
            eigenschaften.setPropertyValue("IgnoreBlankCells", Boolean.TRUE);
            eigenschaften.setPropertyValue("ShowList", TableValidationVisibility.UNSORTED);
            eigenschaften.setPropertyValue("ShowErrorMessage", typ == ValidationType.LIST);
            eigenschaften.setPropertyValue("ErrorAlertStyle", ValidationAlertStyle.STOP);
            Lo.qi(XSheetCondition.class, gueltigkeit).setFormula1(formel);
            bereichEigenschaften.setPropertyValue(VALIDATION, gueltigkeit);
        } catch (UnknownPropertyException | WrappedTargetException | PropertyVetoException
                | IllegalArgumentException | com.sun.star.lang.IndexOutOfBoundsException e) {
            throw new GenerateException("Auswahlliste konnte nicht gesetzt werden: " + e.getMessage());
        }
    }
}
