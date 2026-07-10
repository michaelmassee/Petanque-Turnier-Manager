/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ki;

import java.util.List;

public record KiPlan(
        String summary,
        boolean requiresConfirmation,
        List<KiAktion> actions,
        List<String> warnings,
        String dataPreview) {

    public KiPlan {
        summary = summary == null ? "" : summary.trim();
        actions = actions == null ? List.of() : List.copyOf(actions);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        dataPreview = dataPreview == null ? "" : dataPreview.trim();
    }
}
