/*
 * Erstellung : 2026 / Michael Massee
 **/

package de.petanqueturniermanager.helper.sheet.blattschutz;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.helper.position.RangePosition;

/**
 * Unveränderlicher Record, der ein Sheet mit seinen editierbaren Zellbereichen verknüpft.
 * <p>
 * Leere {@code editierbareBereich}-Liste → Sheet wird vollständig gesperrt.
 * <p>
 * Optional kann ein {@code zuSperrenderGesamtbereich} angegeben werden: Dieser Bereich
 * wird beim Schützen zuerst vollständig gesperrt ({@code IsLocked=true}), erst danach
 * werden die {@code editierbareBereich}-Ranges wieder entsperrt. So wird das Schutz-Modell
 * „alles gesperrt außer editierbare Bereiche" idempotent durchgesetzt – auch auf
 * Bestandsdokumenten, in denen eine Spalte früher editierbar (entsperrt) war und durch
 * eine Konfigurationsänderung nun nicht mehr editierbar sein soll. Ohne diesen Bereich
 * (Standard {@code null}) wird – wie bisher – nur entsperrt, nie zurückgesperrt.
 */
public record SheetSchutzInfo(XSpreadsheet sheet, List<RangePosition> editierbareBereich,
        @Nullable RangePosition zuSperrenderGesamtbereich) {

    /**
     * Erstellt einen Eintrag für ein vollständig gesperrtes Sheet (keine editierbaren Bereiche).
     */
    public static SheetSchutzInfo vollGesperrt(XSpreadsheet sheet) {
        return new SheetSchutzInfo(sheet, List.of(), null);
    }

    /**
     * Erstellt einen Eintrag für ein Sheet mit editierbaren Zellbereichen.
     *
     * @param sheet    das zu schützende Sheet
     * @param bereiche Liste der Ranges, die trotz Schutz editierbar bleiben
     */
    public static SheetSchutzInfo mitEditierbarenBereichen(XSpreadsheet sheet,
            List<RangePosition> bereiche) {
        return new SheetSchutzInfo(sheet, List.copyOf(bereiche), null);
    }

    /**
     * Wie {@link #mitEditierbarenBereichen(XSpreadsheet, List)}, sperrt aber zuvor den
     * gesamten {@code gesamtBereich} explizit ({@code IsLocked=true}). Dadurch werden
     * Zellen innerhalb des Gesamtbereichs, die <em>nicht</em> in {@code bereiche} liegen,
     * zuverlässig gesperrt – auch wenn sie in einem Bestandsdokument noch entsperrt waren.
     *
     * @param sheet        das zu schützende Sheet
     * @param gesamtBereich Bereich, der vor dem Entsperren komplett gesperrt wird
     * @param bereiche     Liste der Ranges, die trotz Schutz editierbar bleiben
     */
    public static SheetSchutzInfo mitGesperrtemGesamtbereich(XSpreadsheet sheet,
            RangePosition gesamtBereich, List<RangePosition> bereiche) {
        return new SheetSchutzInfo(sheet, List.copyOf(bereiche), gesamtBereich);
    }
}
