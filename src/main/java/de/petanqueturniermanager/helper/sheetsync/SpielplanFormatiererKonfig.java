package de.petanqueturniermanager.helper.sheetsync;

import java.util.List;

import org.jspecify.annotations.Nullable;

import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.blattschutz.IBlattschutzKonfiguration;

/**
 * Konfiguration für einen {@link SpielplanFormatiererSheetRunner}.
 * Enthält alle zur Formatierung benötigten Range- und Farbdaten sowie
 * die optionale Blattschutz-Konfiguration für das Turniersystem.
 */
public record SpielplanFormatiererKonfig(
        RangePosition datenRange,
        List<RangePosition> editierbareRanges,
        int geradeFarbe,
        int ungeradeFarbe,
        @Nullable IBlattschutzKonfiguration blattschutzKonfig) {
}
