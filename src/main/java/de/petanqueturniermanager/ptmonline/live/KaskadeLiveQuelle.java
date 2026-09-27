/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.live;

import static de.petanqueturniermanager.kaskade.spielrunde.KaskadeSpielrundeSheet.ERG_TEAM_A_SPALTE;
import static de.petanqueturniermanager.kaskade.spielrunde.KaskadeSpielrundeSheet.ERG_TEAM_B_SPALTE;
import static de.petanqueturniermanager.kaskade.spielrunde.KaskadeSpielrundeSheet.ERSTE_DATEN_ZEILE;
import static de.petanqueturniermanager.kaskade.spielrunde.KaskadeSpielrundeSheet.GRUPPE_SPALTE;
import static de.petanqueturniermanager.kaskade.spielrunde.KaskadeSpielrundeSheet.TEAM_A_SPALTE;
import static de.petanqueturniermanager.kaskade.spielrunde.KaskadeSpielrundeSheet.TEAM_B_SPALTE;

import java.util.ArrayList;
import java.util.List;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;

/**
 * Kaskade: ein Blatt je Kaskadenrunde, darin alle Gruppen untereinander (Gruppenname in der ersten Zeile des
 * Blocks, Team B leer = Freilos). Danach folgen die KO-Felder, die gleichzeitig gespielt werden.
 */
final class KaskadeLiveQuelle implements LiveStandQuelle {

    private final WorkingSpreadsheet ws;

    KaskadeLiveQuelle(WorkingSpreadsheet ws) {
        this.ws = ws;
    }

    @Override
    public LiveTurnierStand lese() throws GenerateException {
        List<List<LivePartie>> runden = new ArrayList<>();
        for (int nr = 1; nr <= LiveBlattLeser.MAX_RUNDEN; nr++) {
            SheetRunner.testDoCancelTask();
            XSpreadsheet blatt = LiveBlattLeser.blatt(ws, SheetMetadataHelper.schluesselKaskadenRunde(nr),
                    SheetNamen.kaskadenRunde(nr));
            if (blatt == null) {
                break;
            }
            runden.add(lesePartien(LiveBlattLeser.datenzeilen(ws, blatt, ERSTE_DATEN_ZEILE, ERG_TEAM_B_SPALTE)));
        }
        LiveRundenFolge folge = new LiveRundenFolge().anhaengen(runden);
        folge.parallelAnhaengen(TurnierbaumLiveLeser.leseBaeume(ws, SheetMetadataHelper::schluesselKaskadenFeld,
                SheetNamen::kaskadenFeld));
        return LiveTurnierStand.ohneRangliste(folge.runden());
    }

    private static List<LivePartie> lesePartien(LiveZellbereich zellen) {
        List<LivePartie> partien = new ArrayList<>();
        String gruppe = null;
        for (int zeile = zellen.ersteZeile(); zeile <= zellen.letzteZeile(); zeile++) {
            int teamA = zellen.teamNr(TEAM_A_SPALTE, zeile);
            if (teamA <= 0) {
                break;
            }
            String gruppeInZeile = zellen.text(GRUPPE_SPALTE, zeile);
            gruppe = gruppeInZeile == null ? gruppe : gruppeInZeile;
            int teamB = zellen.teamNr(TEAM_B_SPALTE, zeile);
            partien.add(teamB <= 0 ? LivePartie.freilos(teamA, null, gruppe)
                    : LivePartie.teams(teamA, teamB, zellen.punkte(ERG_TEAM_A_SPALTE, zeile),
                            zellen.punkte(ERG_TEAM_B_SPALTE, zeile), null, gruppe));
        }
        return partien;
    }
}
