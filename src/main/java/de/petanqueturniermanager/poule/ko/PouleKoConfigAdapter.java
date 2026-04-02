/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.poule.ko;

import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;
import de.petanqueturniermanager.ko.konfiguration.IKoBracketKonfiguration;
import de.petanqueturniermanager.ko.konfiguration.KoSpielbaumTeamAnzeige;
import de.petanqueturniermanager.poule.konfiguration.PouleKonfigurationSheet;

/**
 * Adapter zwischen {@link PouleKonfigurationSheet} und {@link IKoBracketKonfiguration}.
 * <p>
 * Ermöglicht die Nutzung des Poule-Konfigurationssheets als Konfigurationsquelle
 * für {@code KoTurnierbaumSheet}, ohne dass das Poule-System ein eigenes
 * KO-Konfigurationsblatt benötigt.
 * <p>
 * Alle Methoden, für die das Poule-System keine eigene Konfiguration enthält,
 * liefern die in {@link IKoBracketKonfiguration} definierten Standardwerte.
 */
public class PouleKoConfigAdapter implements IKoBracketKonfiguration {

    private final PouleKonfigurationSheet konfigurationSheet;

    public PouleKoConfigAdapter(PouleKonfigurationSheet konfigurationSheet) {
        this.konfigurationSheet = konfigurationSheet;
    }

    @Override
    public int getGruppenGroesse() {
        // Maximale Größe: KO-Bracket wird nicht weiter aufgeteilt
        return 64;
    }

    @Override
    public int getMinRestGroesse() {
        return 2;
    }

    @Override
    public KoSpielbaumTeamAnzeige getSpielbaumTeamAnzeige() {
        return KoSpielbaumTeamAnzeige.NR;
    }

    @Override
    public SpielrundeSpielbahn getSpielbaumSpielbahn() {
        return SpielrundeSpielbahn.X;
    }

    @Override
    public boolean isSpielbaumSpielUmPlatz3() {
        return false;
    }
}
