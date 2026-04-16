/**
 * Erstellung : 2026 / Michael Massee
 **/

package de.petanqueturniermanager.helper.sheet.blattschutz;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import de.petanqueturniermanager.supermelee.blattschutz.SupermeleeBlattschutzKonfiguration;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Registry für turniersystemspezifische {@link IBlattschutzKonfiguration}-Implementierungen.
 * <p>
 * Folgt dem Open/Closed Principle: neue Turniersysteme werden durch
 * {@link #register(TurnierSystem, IBlattschutzKonfiguration)} hinzugefügt,
 * ohne bestehenden Code zu ändern.
 */
public class BlattschutzRegistry {

    private static final ConcurrentHashMap<TurnierSystem, IBlattschutzKonfiguration> REGISTRY =
            new ConcurrentHashMap<>();

    static {
        REGISTRY.put(TurnierSystem.SUPERMELEE, SupermeleeBlattschutzKonfiguration.get());
    }

    private BlattschutzRegistry() {
    }

    /**
     * Registriert eine Konfiguration für ein Turniersystem.
     * Überschreibt eine ggf. vorhandene Registrierung (für Tests nützlich).
     *
     * @param ts  Turniersystem
     * @param cfg zugehörige Blattschutz-Konfiguration
     */
    public static void register(TurnierSystem ts, IBlattschutzKonfiguration cfg) {
        REGISTRY.put(ts, cfg);
    }

    /**
     * Gibt die Konfiguration für das angegebene Turniersystem zurück,
     * oder {@link Optional#empty()} wenn keine registriert ist.
     *
     * @param ts Turniersystem
     * @return registrierte Konfiguration, oder leer
     */
    public static Optional<IBlattschutzKonfiguration> fuer(TurnierSystem ts) {
        return Optional.ofNullable(REGISTRY.get(ts));
    }
}
