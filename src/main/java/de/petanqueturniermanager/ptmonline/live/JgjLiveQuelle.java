/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.live;

import static de.petanqueturniermanager.jedergegenjeden.spielplan.JGJSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE;
import static de.petanqueturniermanager.jedergegenjeden.spielplan.JGJSpielPlanSheet.SPIELPNKT_A_SPALTE;
import static de.petanqueturniermanager.jedergegenjeden.spielplan.JGJSpielPlanSheet.SPIELPNKT_B_SPALTE;
import static de.petanqueturniermanager.jedergegenjeden.spielplan.JGJSpielPlanSheet.SPIEL_NR_SPALTE;
import static de.petanqueturniermanager.jedergegenjeden.spielplan.JGJSpielPlanSheet.TEAM_A_NR_SPALTE;
import static de.petanqueturniermanager.jedergegenjeden.spielplan.JGJSpielPlanSheet.TEAM_B_NR_SPALTE;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.jedergegenjeden.rangliste.JGJRanglisteSheet;
import de.petanqueturniermanager.jedergegenjeden.spielplan.JGJSpielPlanSheet;

/**
 * Jeder gegen Jeden: der Spielplan führt alle Partien (Hin- und ggf. Rückrunde) untereinander, bei Gruppen
 * blockweise je Gruppe unter einer Gruppen-Kopfzeile. Die Gruppen spielen gleichzeitig; danach folgen die
 * KO-Finalrunden.
 */
final class JgjLiveQuelle implements LiveStandQuelle {

    private static final RanglistenBlattLeser RANGLISTE = new RanglistenBlattLeser(
            SheetMetadataHelper.SCHLUESSEL_JGJ_RANGLISTE, SheetNamen.LEGACY_RANGLISTE,
            JGJRanglisteSheet.ERSTE_DATEN_ZEILE,
            new RanglistenBlattLeser.Spalten(JGJRanglisteSheet.TEAM_NR_SPALTE, JGJRanglisteSheet.PLATZ_SPALTE,
                    JGJRanglisteSheet.SPIELE_PLUS_SPALTE, JGJRanglisteSheet.SPIELPUNKTE_PLUS_SPALTE,
                    JGJRanglisteSheet.SPIELPUNKTE_MINUS_SPALTE));

    private final WorkingSpreadsheet ws;

    JgjLiveQuelle(WorkingSpreadsheet ws) {
        this.ws = ws;
    }

    @Override
    public LiveTurnierStand lese() {
        LiveRundenFolge folge = new LiveRundenFolge();
        XSpreadsheet spielplan = LiveBlattLeser.blatt(ws, SheetMetadataHelper.SCHLUESSEL_JGJ_SPIELPLAN,
                JGJSpielPlanSheet.LEGACY_SHEET_NAMEN);
        if (spielplan != null) {
            List<Gruppe> gruppen = leseGruppen(
                    LiveBlattLeser.datenzeilen(ws, spielplan, ERSTE_SPIELTAG_DATEN_ZEILE, TEAM_B_NR_SPALTE));
            boolean mitGruppenName = gruppen.size() > 1;
            folge.parallelAnhaengen(gruppen.stream()
                    .map(gruppe -> RundenBloecke.inRundenTeilen(gruppe.partien(mitGruppenName))).toList());
        }
        folge.parallelAnhaengen(TurnierbaumLiveLeser.leseBaeume(ws, SheetMetadataHelper::schluesselJgjFinalrunde,
                SheetNamen::koFinaleGruppe));
        return new LiveTurnierStand(folge.runden(), RANGLISTE.lese(ws));
    }

    /**
     * Partie-Zeilen tragen die Team-Nummern in den Arbeitsspalten; eine Gruppen-Kopfzeile hat dort 0/0 und in der
     * ersten Spalte den Gruppennamen.
     */
    private static List<Gruppe> leseGruppen(LiveZellbereich zellen) {
        List<Gruppe> gruppen = new ArrayList<>();
        Gruppe aktuelle = null;
        for (int zeile = zellen.ersteZeile(); zeile <= zellen.letzteZeile(); zeile++) {
            int teamA = zellen.zahl(TEAM_A_NR_SPALTE, zeile);
            if (teamA > 0) {
                if (aktuelle == null) {
                    aktuelle = new Gruppe(null);
                    gruppen.add(aktuelle);
                }
                aktuelle.zeilen().add(partie(zellen, zeile, teamA));
            } else if (zellen.wert(TEAM_A_NR_SPALTE, zeile) instanceof Number) {
                aktuelle = new Gruppe(zellen.text(SPIEL_NR_SPALTE, zeile));
                gruppen.add(aktuelle);
            } else {
                break;
            }
        }
        return gruppen;
    }

    private static LivePartie partie(LiveZellbereich zellen, int zeile, int teamA) {
        int teamB = zellen.zahl(TEAM_B_NR_SPALTE, zeile);
        return teamB <= 0 ? LivePartie.freilos(teamA, null, null)
                : LivePartie.teams(teamA, teamB, zellen.punkte(SPIELPNKT_A_SPALTE, zeile),
                        zellen.punkte(SPIELPNKT_B_SPALTE, zeile), null, null);
    }

    private record Gruppe(@Nullable String name, List<LivePartie> zeilen) {

        Gruppe(@Nullable String name) {
            this(name, new ArrayList<>());
        }

        List<LivePartie> partien(boolean mitGruppenName) {
            if (!mitGruppenName || name == null) {
                return zeilen;
            }
            return zeilen.stream().map(partie -> new LivePartie(partie.teamA(), partie.teamB(), partie.punkteA(),
                    partie.punkteB(), partie.bahn(), name)).toList();
        }
    }
}
