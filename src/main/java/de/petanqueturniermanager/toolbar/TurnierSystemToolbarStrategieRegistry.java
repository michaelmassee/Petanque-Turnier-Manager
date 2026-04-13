/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.toolbar;

import java.util.Map;

import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;
import de.petanqueturniermanager.toolbar.strategie.JGJToolbarStrategie;
import de.petanqueturniermanager.toolbar.strategie.KaskadeToolbarStrategie;
import de.petanqueturniermanager.toolbar.strategie.KoToolbarStrategie;
import de.petanqueturniermanager.toolbar.strategie.LigaToolbarStrategie;
import de.petanqueturniermanager.toolbar.strategie.MaastrichterToolbarStrategie;
import de.petanqueturniermanager.toolbar.strategie.NichtVerfuegbarToolbarStrategie;
import de.petanqueturniermanager.toolbar.strategie.PouleToolbarStrategie;
import de.petanqueturniermanager.toolbar.strategie.SchweizerToolbarStrategie;
import de.petanqueturniermanager.toolbar.strategie.SupermeleeToolbarStrategie;

/**
 * Registry, die jedem {@link TurnierSystem} eine passende
 * {@link ITurnierSystemToolbarStrategie} zuordnet.
 */
public final class TurnierSystemToolbarStrategieRegistry {

    private static final ITurnierSystemToolbarStrategie FALLBACK = new NichtVerfuegbarToolbarStrategie();

    private static final Map<TurnierSystem, ITurnierSystemToolbarStrategie> REGISTRY = Map.of(
            TurnierSystem.SUPERMELEE,   new SupermeleeToolbarStrategie(),
            TurnierSystem.SCHWEIZER,    new SchweizerToolbarStrategie(),
            TurnierSystem.MAASTRICHTER, new MaastrichterToolbarStrategie(),
            TurnierSystem.POULE,        new PouleToolbarStrategie(),
            TurnierSystem.LIGA,         new LigaToolbarStrategie(),
            TurnierSystem.JGJ,          new JGJToolbarStrategie(),
            TurnierSystem.KO,           new KoToolbarStrategie(),
            TurnierSystem.KASKADE,      new KaskadeToolbarStrategie()
    );

    private TurnierSystemToolbarStrategieRegistry() {
    }

    /**
     * Gibt die Strategie für das angegebene Turniersystem zurück.
     * Falls kein Eintrag existiert (z.B. {@code TurnierSystem.KEIN}),
     * wird der Fallback zurückgegeben.
     */
    public static ITurnierSystemToolbarStrategie get(TurnierSystem system) {
        if (system == null) {
            return FALLBACK;
        }
        return REGISTRY.getOrDefault(system, FALLBACK);
    }
}
