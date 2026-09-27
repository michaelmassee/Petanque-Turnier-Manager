/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.live;

import static de.petanqueturniermanager.triptete.spielplan.TripTeteSpielPlanSheet.BAHN_DOU_SPALTE;
import static de.petanqueturniermanager.triptete.spielplan.TripTeteSpielPlanSheet.BAHN_TETE_SPALTE;
import static de.petanqueturniermanager.triptete.spielplan.TripTeteSpielPlanSheet.BAHN_TRI_SPALTE;
import static de.petanqueturniermanager.triptete.spielplan.TripTeteSpielPlanSheet.DOU_A_SPALTE;
import static de.petanqueturniermanager.triptete.spielplan.TripTeteSpielPlanSheet.DOU_B_SPALTE;
import static de.petanqueturniermanager.triptete.spielplan.TripTeteSpielPlanSheet.ERSTE_DATEN_ZEILE;
import static de.petanqueturniermanager.triptete.spielplan.TripTeteSpielPlanSheet.TEAM_A_NR_SPALTE;
import static de.petanqueturniermanager.triptete.spielplan.TripTeteSpielPlanSheet.TEAM_B_NR_SPALTE;
import static de.petanqueturniermanager.triptete.spielplan.TripTeteSpielPlanSheet.TETE_A_SPALTE;
import static de.petanqueturniermanager.triptete.spielplan.TripTeteSpielPlanSheet.TETE_B_SPALTE;
import static de.petanqueturniermanager.triptete.spielplan.TripTeteSpielPlanSheet.TRI_A_SPALTE;
import static de.petanqueturniermanager.triptete.spielplan.TripTeteSpielPlanSheet.TRI_B_SPALTE;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.triptete.rangliste.TripTeteRanglisteSheet;
import de.petanqueturniermanager.triptete.spielplan.TripTeteSpielPlanSheet;

/**
 * Trip-Tête: eine Begegnung zweier Teams besteht aus Triplette, Doublette und Tête. PTM-Online kennt je Runde
 * nur eine Partie pro Meldung, daher wird die Begegnung als eine Partie übertragen: Ergebnis = gewonnene
 * Einzelpartien (sobald alle drei eingetragen sind), Bahn = die Bahnen der Einzelpartien.
 */
final class TripTeteLiveQuelle implements LiveStandQuelle {

    private static final int[][] EINZELPARTIEN = { { TRI_A_SPALTE, TRI_B_SPALTE }, { DOU_A_SPALTE, DOU_B_SPALTE },
            { TETE_A_SPALTE, TETE_B_SPALTE } };
    private static final int[] BAHNEN = { BAHN_TRI_SPALTE, BAHN_DOU_SPALTE, BAHN_TETE_SPALTE };

    /** Siege = gewonnene Begegnungen, Punkte = Spielpunkte der Einzelpartien. */
    private static final RanglistenBlattLeser RANGLISTE = new RanglistenBlattLeser(
            SheetMetadataHelper.SCHLUESSEL_TRIPTETE_RANGLISTE, SheetNamen.LEGACY_RANGLISTE,
            TripTeteRanglisteSheet.ERSTE_DATEN_ZEILE,
            new RanglistenBlattLeser.Spalten(TripTeteRanglisteSheet.TEAM_NR_SPALTE, TripTeteRanglisteSheet.RANG_SPALTE,
                    TripTeteRanglisteSheet.BEG_GEW_SPALTE, TripTeteRanglisteSheet.SP_PUNKTE_PLUS_SPALTE,
                    TripTeteRanglisteSheet.SP_PUNKTE_MINUS_SPALTE));

    private final WorkingSpreadsheet ws;

    TripTeteLiveQuelle(WorkingSpreadsheet ws) {
        this.ws = ws;
    }

    @Override
    public LiveTurnierStand lese() {
        XSpreadsheet spielplan = LiveBlattLeser.blatt(ws, SheetMetadataHelper.SCHLUESSEL_TRIPTETE_SPIELPLAN,
                TripTeteSpielPlanSheet.LEGACY_SHEET_NAMEN);
        if (spielplan == null) {
            return new LiveTurnierStand(List.of(), RANGLISTE.lese(ws));
        }
        LiveZellbereich zellen = LiveBlattLeser.datenzeilen(ws, spielplan, ERSTE_DATEN_ZEILE, TEAM_B_NR_SPALTE);
        List<LivePartie> partien = new ArrayList<>();
        for (int zeile = zellen.ersteZeile(); zeile <= zellen.letzteZeile(); zeile++) {
            int teamA = zellen.zahl(TEAM_A_NR_SPALTE, zeile);
            if (teamA <= 0) {
                break;
            }
            partien.add(partie(zellen, zeile, teamA));
        }
        return new LiveTurnierStand(new LiveRundenFolge().anhaengen(RundenBloecke.inRundenTeilen(partien)).runden(),
                RANGLISTE.lese(ws));
    }

    private static LivePartie partie(LiveZellbereich zellen, int zeile, int teamA) {
        int teamB = zellen.zahl(TEAM_B_NR_SPALTE, zeile);
        String bahn = bahnen(zellen, zeile);
        if (teamB <= 0) {
            return LivePartie.freilos(teamA, bahn, null);
        }
        int siegeA = 0;
        int siegeB = 0;
        for (int[] einzel : EINZELPARTIEN) {
            Integer punkteA = zellen.punkte(einzel[0], zeile);
            Integer punkteB = zellen.punkte(einzel[1], zeile);
            if (punkteA == null || punkteB == null) {
                return LivePartie.teams(teamA, teamB, null, null, bahn, null);
            }
            siegeA += punkteA > punkteB ? 1 : 0;
            siegeB += punkteB > punkteA ? 1 : 0;
        }
        return LivePartie.teams(teamA, teamB, siegeA, siegeB, bahn, null);
    }

    private static @Nullable String bahnen(LiveZellbereich zellen, int zeile) {
        Set<String> bahnen = new LinkedHashSet<>();
        for (int spalte : BAHNEN) {
            String bahn = zellen.text(spalte, zeile);
            if (bahn != null) {
                bahnen.add(bahn);
            }
        }
        return bahnen.isEmpty() ? null : String.join("/", bahnen);
    }
}
