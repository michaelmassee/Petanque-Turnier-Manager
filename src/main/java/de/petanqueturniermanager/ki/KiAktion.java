/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ki;

import java.util.Map;

public record KiAktion(String type, String target, Map<String, Object> parameters) {
    public KiAktion {
        type = type == null ? "" : type.trim();
        target = target == null ? "" : target.trim();
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
    }
}
