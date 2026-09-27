/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.live;

import static de.petanqueturniermanager.poule.vorrunde.AbstractPouleVorrundeSheet.ERSTE_DATEN_ZEILE;
import static de.petanqueturniermanager.poule.vorrunde.AbstractPouleVorrundeSheet.SPALTE_BESCHR;
import static de.petanqueturniermanager.poule.vorrunde.AbstractPouleVorrundeSheet.SPALTE_ERG_A;
import static de.petanqueturniermanager.poule.vorrunde.AbstractPouleVorrundeSheet.SPALTE_ERG_B;
import static de.petanqueturniermanager.poule.vorrunde.AbstractPouleVorrundeSheet.SPALTE_POULE_NR;
import static de.petanqueturniermanager.poule.vorrunde.AbstractPouleVorrundeSheet.SPALTE_TEAM_A_NR;
import static de.petanqueturniermanager.poule.vorrunde.AbstractPouleVorrundeSheet.SPALTE_TEAM_B_NR;
import static de.petanqueturniermanager.poule.vorrunde.AbstractPouleVorrundeSheet.VIERER_POULE_ZEILEN;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.poule.konfiguration.PouleKonfigurationSheet;

/**
 * Poule: die Vorrunde führt je Poule einen Block (Vierer-Poule: Spiel A/B, Sieger-/Verliererspiel, Barrage;
 * Dreier-Poule: Spiel 1–3), alle Poules spielen gleichzeitig in drei Runden. Partien späterer Runden stehen erst
 * fest, wenn die Ergebnisse davor eingetragen sind. Bahnen stehen in den Spielplänen je Poule. Danach folgen die
 * KO-Turnierbäume (A-/B-Finale).
 */
final class PouleLiveQuelle implements LiveStandQuelle {

    /** Runde je Zeile eines Vierer-Poule-Blocks; Dreier-Poules spielen je Zeile eine Runde. */
    private static final int[] VIERER_RUNDEN = { 0, 0, 1, 1, 2 };
    private static final int BAHN_SPALTE = 0;

    private final WorkingSpreadsheet ws;

    PouleLiveQuelle(WorkingSpreadsheet ws) {
        this.ws = ws;
    }

    @Override
    public LiveTurnierStand lese() throws GenerateException {
        LiveRundenFolge folge = new LiveRundenFolge();
        XSpreadsheet vorrunde = LiveBlattLeser.blatt(ws, SheetMetadataHelper.SCHLUESSEL_POULE_VORRUNDE,
                SheetNamen.pouleVorrunde());
        if (vorrunde != null) {
            boolean mitBahn = new PouleKonfigurationSheet(ws).isSpielplanMitBahnspalte();
            List<List<List<LivePartie>>> poules = new ArrayList<>();
            LiveZellbereich zellen = LiveBlattLeser.datenzeilen(ws, vorrunde, ERSTE_DATEN_ZEILE, SPALTE_ERG_B);
            List<List<Integer>> bloecke = bloecke(zellen);
            for (int i = 0; i < bloecke.size(); i++) {
                poules.add(pouleRunden(zellen, bloecke.get(i), mitBahn ? bahnen(i + 1) : null));
            }
            folge.parallelAnhaengen(poules);
        }
        folge.parallelAnhaengen(TurnierbaumLiveLeser.leseBaeume(ws, SheetMetadataHelper::schluesselPouleKo,
                SheetNamen::koFinaleGruppe));
        return LiveTurnierStand.ohneRangliste(folge.runden());
    }

    /** Zeilen je Poule-Block: ein Block beginnt mit der Poule-Bezeichnung, jede Partie hat eine Beschreibung. */
    private static List<List<Integer>> bloecke(LiveZellbereich zellen) {
        List<List<Integer>> bloecke = new ArrayList<>();
        for (int zeile = zellen.ersteZeile(); zeile <= zellen.letzteZeile(); zeile++) {
            if (zellen.text(SPALTE_BESCHR, zeile) == null) {
                break;
            }
            if (zellen.text(SPALTE_POULE_NR, zeile) != null || bloecke.isEmpty()) {
                bloecke.add(new ArrayList<>());
            }
            bloecke.get(bloecke.size() - 1).add(zeile);
        }
        return bloecke;
    }

    private static List<List<LivePartie>> pouleRunden(LiveZellbereich zellen, List<Integer> zeilen,
            @Nullable LiveZellbereich bahnen) {
        String poule = zellen.text(SPALTE_POULE_NR, zeilen.get(0));
        List<List<LivePartie>> runden = new ArrayList<>(List.of(new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>()));
        for (int i = 0; i < zeilen.size(); i++) {
            int zeile = zeilen.get(i);
            int teamA = zellen.teamNr(SPALTE_TEAM_A_NR, zeile);
            int teamB = zellen.teamNr(SPALTE_TEAM_B_NR, zeile);
            if (teamA <= 0 || teamB <= 0) {
                continue;
            }
            int runde = zeilen.size() == VIERER_POULE_ZEILEN ? VIERER_RUNDEN[i] : Math.min(i, 2);
            String bahn = bahnen == null ? null : bahnen.text(BAHN_SPALTE, bahnen.ersteZeile() + i);
            String beschreibung = zellen.text(SPALTE_BESCHR, zeile);
            String stufe = poule == null ? beschreibung : poule + " · " + beschreibung;
            runden.get(runde).add(LivePartie.teams(teamA, teamB, zellen.punkte(SPALTE_ERG_A, zeile),
                    zellen.punkte(SPALTE_ERG_B, zeile), bahn, stufe));
        }
        return runden;
    }

    /** Bahnen der Poule aus ihrem Spielplan (gleiche Zeilenfolge wie der Vorrunden-Block). */
    private @Nullable LiveZellbereich bahnen(int pouleNr) {
        XSpreadsheet spielplan = LiveBlattLeser.blatt(ws, SheetMetadataHelper.schluesselPouleSpielplan(pouleNr),
                SheetNamen.pouleSpielplan(pouleNr));
        return spielplan == null ? null
                : LiveZellbereich.lese(ws, spielplan, BAHN_SPALTE, ERSTE_DATEN_ZEILE, BAHN_SPALTE,
                        ERSTE_DATEN_ZEILE + VIERER_POULE_ZEILEN - 1);
    }
}
