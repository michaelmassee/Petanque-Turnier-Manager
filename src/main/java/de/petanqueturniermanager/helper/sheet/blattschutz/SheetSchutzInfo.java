/**
 * Erstellung : 2026 / Michael Massee
 **/

package de.petanqueturniermanager.helper.sheet.blattschutz;

import java.util.List;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.helper.position.RangePosition;

/**
 * Unveränderlicher Record, der ein Sheet mit seinen editierbaren Zellbereichen verknüpft.
 * <p>
 * Leere {@code editierbareBereich}-Liste → Sheet wird vollständig gesperrt.
 */
public record SheetSchutzInfo(XSpreadsheet sheet, List<RangePosition> editierbareBereich) {

    /**
     * Erstellt einen Eintrag für ein vollständig gesperrtes Sheet (keine editierbaren Bereiche).
     */
    public static SheetSchutzInfo vollGesperrt(XSpreadsheet sheet) {
        return new SheetSchutzInfo(sheet, List.of());
    }

    /**
     * Erstellt einen Eintrag für ein Sheet mit editierbaren Zellbereichen.
     *
     * @param sheet    das zu schützende Sheet
     * @param bereiche Liste der Ranges, die trotz Schutz editierbar bleiben
     */
    public static SheetSchutzInfo mitEditierbarenBereichen(XSpreadsheet sheet,
            List<RangePosition> bereiche) {
        return new SheetSchutzInfo(sheet, List.copyOf(bereiche));
    }
}
