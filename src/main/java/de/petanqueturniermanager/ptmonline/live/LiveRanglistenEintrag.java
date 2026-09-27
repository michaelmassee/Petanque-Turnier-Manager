/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.live;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Eine Zeile der Rangliste, wie sie das Turnierdokument berechnet hat.
 *
 * @param platz   Platz (bei Gleichstand mehrfach vergeben)
 * @param nummern lokale Team- bzw. Spieler-Nummern des Eintrags
 */
public record LiveRanglistenEintrag(int platz, List<Integer> nummern, @Nullable Integer siege,
        @Nullable Integer punktePlus, @Nullable Integer punkteMinus) {

    public LiveRanglistenEintrag {
        nummern = List.copyOf(nummern);
    }

    public static LiveRanglistenEintrag team(int platz, int nummer, @Nullable Integer siege,
            @Nullable Integer punktePlus, @Nullable Integer punkteMinus) {
        return new LiveRanglistenEintrag(platz, List.of(nummer), siege, punktePlus, punkteMinus);
    }
}
