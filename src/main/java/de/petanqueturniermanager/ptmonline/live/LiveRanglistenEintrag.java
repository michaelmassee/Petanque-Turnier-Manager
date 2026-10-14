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
 * @param wertung systemabhängige Wertungskriterien; leer, wenn das System sie nicht führt
 */
public record LiveRanglistenEintrag(int platz, List<Integer> nummern, @Nullable Integer siege,
        @Nullable Integer punktePlus, @Nullable Integer punkteMinus, Wertungskriterien wertung) {

    public LiveRanglistenEintrag {
        nummern = List.copyOf(nummern);
    }

    public static LiveRanglistenEintrag team(int platz, int nummer, @Nullable Integer siege,
            @Nullable Integer punktePlus, @Nullable Integer punkteMinus) {
        return team(platz, nummer, siege, punktePlus, punkteMinus, Wertungskriterien.KEINE);
    }

    public static LiveRanglistenEintrag team(int platz, int nummer, @Nullable Integer siege,
            @Nullable Integer punktePlus, @Nullable Integer punkteMinus, Wertungskriterien wertung) {
        return new LiveRanglistenEintrag(platz, List.of(nummer), siege, punktePlus, punkteMinus, wertung);
    }

    /**
     * Wertungskriterien, die die Live-Ansicht je System als eigene Spalten zeigt (wie bei einem online durchgeführten
     * Turnier).
     *
     * @param niederlagen    verlorene Spiele bzw. Begegnungen
     * @param spielDifferenz Spiele+ minus Spiele−
     * @param buchholz       Buchholz (Schweizer, Maastrichter)
     * @param feinBuchholz   Feinbuchholz (Schweizer, Maastrichter)
     * @param formuleX       Formule-X-Wertung
     */
    public record Wertungskriterien(@Nullable Integer niederlagen, @Nullable Integer spielDifferenz,
            @Nullable Integer buchholz, @Nullable Integer feinBuchholz, @Nullable Integer formuleX) {

        public static final Wertungskriterien KEINE = new Wertungskriterien(null, null, null, null, null);
    }
}
