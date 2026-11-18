/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.live;

import java.util.List;

/**
 * Stand eines Turniers für die Live-Ansicht in PTM-Online.
 *
 * @param runden    alle bisher erzeugten Runden, aufsteigend nummeriert ab 1
 * @param rangliste aktuelle Rangliste; leer, wenn das System keine führt (z.&nbsp;B. KO) – dann bleibt der
 *                  Online-Snapshot unverändert
 * @param koSieger  Sieger der entschiedenen KO-Felder (Maastrichter-Finalrunden), zweiter Teil der Endrangliste
 */
public record LiveTurnierStand(List<LiveRunde> runden, List<LiveRanglistenEintrag> rangliste,
        List<LiveKoSieger> koSieger) {

    public LiveTurnierStand {
        runden = List.copyOf(runden);
        rangliste = List.copyOf(rangliste);
        koSieger = List.copyOf(koSieger);
    }

    public LiveTurnierStand(List<LiveRunde> runden, List<LiveRanglistenEintrag> rangliste) {
        this(runden, rangliste, List.of());
    }

    public static LiveTurnierStand ohneRangliste(List<LiveRunde> runden) {
        return new LiveTurnierStand(runden, List.of());
    }
}
