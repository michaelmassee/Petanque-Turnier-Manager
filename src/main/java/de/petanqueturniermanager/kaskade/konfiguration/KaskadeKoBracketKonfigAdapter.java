/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.kaskade.konfiguration;

import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;
import de.petanqueturniermanager.ko.konfiguration.IKoBracketKonfiguration;
import de.petanqueturniermanager.ko.konfiguration.KoSpielbaumTeamAnzeige;

/**
 * Adapter: verbindet {@link KaskadeKonfigurationSheet} mit dem {@link IKoBracketKonfiguration}-Interface,
 * das {@link de.petanqueturniermanager.ko.KoTurnierbaumSheet} für die Bracket-Darstellung benötigt.
 */
public class KaskadeKoBracketKonfigAdapter implements IKoBracketKonfiguration {

    private final KaskadeKonfigurationSheet konfiguration;

    public KaskadeKoBracketKonfigAdapter(KaskadeKonfigurationSheet konfiguration) {
        this.konfiguration = konfiguration;
    }

    /** Wird in {@code erstelleGruppeBracket} nicht genutzt – vernünftiger Default. */
    @Override
    public int getGruppenGroesse() {
        return 8;
    }

    /** Wird in {@code erstelleGruppeBracket} nicht genutzt – vernünftiger Default. */
    @Override
    public int getMinRestGroesse() {
        return 4;
    }

    @Override
    public KoSpielbaumTeamAnzeige getSpielbaumTeamAnzeige() {
        return KoSpielbaumTeamAnzeige.NR;
    }

    /** Kaskade hat keine Spielbahn-Zuweisung im KO-Turnierbaum. */
    @Override
    public SpielrundeSpielbahn getSpielbaumSpielbahn() {
        return SpielrundeSpielbahn.X;
    }

    /** Kein Spiel um Platz 3 in den Kaskaden-Endfeldern. */
    @Override
    public boolean isSpielbaumSpielUmPlatz3() {
        return false;
    }

    @Override
    public int getKoTurnierbaumTabFarbe() {
        return konfiguration.getKaskadenTabFarbe();
    }
}
