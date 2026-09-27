/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.live;

import static de.petanqueturniermanager.supermelee.spielrunde.SpielrundeSheetKonstanten.ERSTE_DATEN_ZEILE;
import static de.petanqueturniermanager.supermelee.spielrunde.SpielrundeSheetKonstanten.ERSTE_SPALTE_ERGEBNISSE;
import static de.petanqueturniermanager.supermelee.spielrunde.SpielrundeSheetKonstanten.ERSTE_SPIELERNR_SPALTE;
import static de.petanqueturniermanager.supermelee.spielrunde.SpielrundeSheetKonstanten.NUMMER_SPALTE_RUNDESPIELPLAN;

import java.util.ArrayList;
import java.util.List;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.supermelee.SuperMeleeSummenSpalten;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeKonfigurationSheet;
import de.petanqueturniermanager.supermelee.spieltagrangliste.SpieltagRanglisteSheet;

/**
 * Supermelee: jeder Spieltag ist online ein eigenes Turnier, die Spielrunden des Spieltags sind die Runden der
 * Live-Ansicht. Eine Zeile der Spielrunde ist eine Partie; die Teams bestehen aus den Spieler-Nummern A1–A3 bzw.
 * B1–B3, jeder Spieler ist online eine eigene Meldung. Die Rangliste ist die Spieltag-Rangliste.
 */
final class SupermeleeLiveQuelle implements LiveStandQuelle {

    private static final int SPIELER_PRO_TEAM = 3;
    private static final int LETZTE_SPIELERNR_SPALTE = ERSTE_SPIELERNR_SPALTE + 2 * SPIELER_PRO_TEAM - 1;

    private final WorkingSpreadsheet ws;
    private final int spieltagNr;

    SupermeleeLiveQuelle(WorkingSpreadsheet ws, int spieltagNr) {
        this.ws = ws;
        this.spieltagNr = spieltagNr;
    }

    @Override
    public LiveTurnierStand lese() throws GenerateException {
        boolean mitBahn = new SuperMeleeKonfigurationSheet(ws).getSpielrundeSpielbahn() != SpielrundeSpielbahn.X;
        List<List<LivePartie>> runden = new ArrayList<>();
        for (int nr = 1; nr <= LiveBlattLeser.MAX_RUNDEN; nr++) {
            SheetRunner.testDoCancelTask();
            XSpreadsheet blatt = LiveBlattLeser.blatt(ws,
                    SheetMetadataHelper.schluesselSupermeleeSpielrunde(spieltagNr, nr),
                    SheetNamen.supermeleeSpielrunde(spieltagNr, nr));
            if (blatt == null) {
                break;
            }
            runden.add(lesePartien(LiveBlattLeser.datenzeilen(ws, blatt, ERSTE_DATEN_ZEILE, LETZTE_SPIELERNR_SPALTE),
                    mitBahn));
        }
        return new LiveTurnierStand(new LiveRundenFolge().anhaengen(runden).runden(), rangliste(runden.size()));
    }

    /**
     * Spieltag-Rangliste: je Spielrunde zwei Ergebnisspalten, danach die Summenspalten (Spiele+/−/Δ, Punkte+/−/Δ).
     * Die Rangliste wird bei jeder neuen Spielrunde neu aufgebaut, ihre Spaltenzahl passt daher zu den Runden.
     */
    private List<LiveRanglistenEintrag> rangliste(int anzahlRunden) {
        int ersteSummenSpalte = SpieltagRanglisteSheet.ERSTE_SPIELRUNDE_SPALTE + 2 * anzahlRunden;
        return new RanglistenBlattLeser(SheetMetadataHelper.schluesselSpieltagRangliste(spieltagNr),
                SheetNamen.spieltagRangliste(spieltagNr), SpieltagRanglisteSheet.ERSTE_DATEN_ZEILE,
                new RanglistenBlattLeser.Spalten(SpieltagRanglisteSheet.SPIELER_NR_SPALTE,
                        SpieltagRanglisteSheet.RANGLISTE_SPALTE,
                        ersteSummenSpalte + SuperMeleeSummenSpalten.SPIELE_PLUS_OFFS,
                        ersteSummenSpalte + SuperMeleeSummenSpalten.PUNKTE_PLUS_OFFS,
                        ersteSummenSpalte + SuperMeleeSummenSpalten.PUNKTE_MINUS_OFFS)).lese(ws);
    }

    private static List<LivePartie> lesePartien(LiveZellbereich zellen, boolean mitBahn) {
        List<LivePartie> partien = new ArrayList<>();
        for (int zeile = zellen.ersteZeile(); zeile <= zellen.letzteZeile(); zeile++) {
            List<Integer> teamA = spieler(zellen, zeile, ERSTE_SPIELERNR_SPALTE);
            if (teamA.isEmpty()) {
                break;
            }
            partien.add(new LivePartie(teamA, spieler(zellen, zeile, ERSTE_SPIELERNR_SPALTE + SPIELER_PRO_TEAM),
                    zellen.punkte(ERSTE_SPALTE_ERGEBNISSE, zeile), zellen.punkte(ERSTE_SPALTE_ERGEBNISSE + 1, zeile),
                    mitBahn ? zellen.text(NUMMER_SPALTE_RUNDESPIELPLAN, zeile) : null, null));
        }
        return partien;
    }

    private static List<Integer> spieler(LiveZellbereich zellen, int zeile, int ersteSpalte) {
        List<Integer> nummern = new ArrayList<>();
        for (int spalte = ersteSpalte; spalte < ersteSpalte + SPIELER_PRO_TEAM; spalte++) {
            int nr = zellen.zahl(spalte, zeile);
            if (nr > 0) {
                nummern.add(nr);
            }
        }
        return nummern;
    }
}
