package de.petanqueturniermanager.helper.sheetsync;

import java.util.List;

import org.jspecify.annotations.Nullable;

import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.blattschutz.IBlattschutzKonfiguration;

/**
 * Konfiguration für einen {@link SpielplanFormatiererSheetRunner}.
 * Enthält alle zur Formatierung benötigten Range- und Farbdaten sowie
 * die optionale Blattschutz-Konfiguration für das Turniersystem.
 *
 * @param datenRange         gesamter Datenbereich (für Zebra)
 * @param editierbareRanges  Bereiche editierbarer Felder (Editierbar-CF)
 * @param cfLoeschenRanges   Bereiche, deren bedingte Formatierung beim Lauf entfernt wird –
 *                           z.B. Formelspalten, die in einer früheren Version fälschlich eine
 *                           Editierbar-CF hatten und nun nur noch normales Zebra zeigen sollen.
 * @param geradeFarbe        Zebra-Farbe gerade Zeilen
 * @param ungeradeFarbe      Zebra-Farbe ungerade Zeilen
 * @param blattschutzKonfig  optionale Blattschutz-Konfiguration des Turniersystems
 */
public record SpielplanFormatiererKonfig(
        RangePosition datenRange,
        List<RangePosition> editierbareRanges,
        List<RangePosition> cfLoeschenRanges,
        int geradeFarbe,
        int ungeradeFarbe,
        @Nullable IBlattschutzKonfiguration blattschutzKonfig) {

    /**
     * Convenience-Konstruktor ohne {@code cfLoeschenRanges} (leere Liste) – für Systeme,
     * die keine veralteten CF-Reste auf Formelspalten entfernen müssen.
     */
    public SpielplanFormatiererKonfig(RangePosition datenRange, List<RangePosition> editierbareRanges,
            int geradeFarbe, int ungeradeFarbe, @Nullable IBlattschutzKonfiguration blattschutzKonfig) {
        this(datenRange, editierbareRanges, List.of(), geradeFarbe, ungeradeFarbe, blattschutzKonfig);
    }
}
