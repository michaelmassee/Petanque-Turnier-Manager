/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.LoMainThread;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.ptmonline.live.LiveRunde;
import de.petanqueturniermanager.ptmonline.live.LiveStandQuelle;
import de.petanqueturniermanager.ptmonline.live.LiveStandQuellen;
import de.petanqueturniermanager.ptmonline.live.LiveTurnierStand;

/**
 * Überträgt Spielrunden (Paarungen, Bahn, Ergebnisse) und Rangliste eines im Turnierdokument durchgeführten
 * Turniers an PTM-Online – Grundlage der persönlichen Live-Ansicht der Spieler („Aktuelle Partie“, „Meine
 * Partien“, Rangliste).
 * <p>
 * Übertragen wird immer der komplette Stand: PTM-Online ersetzt jede Runde atomar, Korrekturen im Dokument
 * (neu ausgeloste Runde, geänderte Ergebnisse) kommen so 1:1 an. Lokal gelöschte Runden werden online ebenfalls
 * gelöscht.
 * <p>
 * Läuft synchron im aufrufenden {@code SheetRunner}. No-Op, wenn das Dokument nicht verbunden oder der Sync
 * pausiert ist. Fehler werden gesammelt gemeldet – die Live-Übertragung blockiert den Turnierbetrieb nie.
 */
public final class PtmOnlineLiveSync {

    private static final Logger logger = LogManager.getLogger(PtmOnlineLiveSync.class);
    private static final int MAX_RUNDEN_NR = 999;

    private PtmOnlineLiveSync() {}

    /**
     * Überträgt den aktuellen Stand des Dokuments bzw. bei Supermelee des aktiven Spieltags.
     *
     * @param fehlerMelden {@code false} für Hintergrundläufe: Fehler nur protokollieren, damit z.&nbsp;B. ohne Netz
     *                     nicht bei jeder Ranglisten-Aktualisierung ein Dialog erscheint
     */
    public static void uebertragen(WorkingSpreadsheet ws, TurnierSystem ts, boolean fehlerMelden) {
        Consumer<String> fehlerAnzeige = fehlerMelden ? fehler -> zeigeFehler(ws.getxContext(), fehler)
                : fehler -> { /* Hintergrundlauf: nur protokolliert */ };
        try {
            Optional<PtmOnlineVerbindung> verbindung = PtmOnlineVerbindung.ermitteln(ws, ts);
            if (verbindung.isEmpty() || !PtmOnlineSpielrundeSync.istSyncAktiv(verbindung.get().mapping())) {
                return;
            }
            Optional<LiveStandQuelle> quelle = LiveStandQuellen.fuer(ws, ts, verbindung.get().spieltagNr());
            if (quelle.isPresent()) {
                uebertragen(verbindung.get(), quelle.get());
            }
        } catch (IOException e) {
            logger.warn("PTM-Online: Live-Übertragung fehlgeschlagen", e);
            fehlerAnzeige.accept(PtmOnlineFehlerText.fuer(e));
        } catch (GenerateException e) {
            logger.error("PTM-Online: Live-Stand nicht lesbar", e);
            fehlerAnzeige.accept(e.getMessage());
        } catch (InterruptedException e) {
            logger.debug("PTM-Online: Live-Übertragung abgebrochen", e);
            Thread.currentThread().interrupt();
        } catch (RuntimeException e) {
            logger.error("PTM-Online: Unerwarteter Fehler bei der Live-Übertragung", e);
            fehlerAnzeige.accept(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
    }

    static void uebertragen(PtmOnlineVerbindung verbindung, LiveStandQuelle quelle)
            throws GenerateException, IOException, InterruptedException {
        LiveTurnierStand stand = quelle.lese();
        Map<Integer, List<String>> onlineIds = LiveNummernAufloesung.ermitteln(verbindung);
        uebertragen(verbindung.gebundenerClient(), verbindung.tournamentId(), stand, onlineIds);
    }

    static void uebertragen(TournamentSyncClient client, String tournamentId, LiveTurnierStand stand,
            Map<Integer, List<String>> onlineIds) throws IOException, InterruptedException {
        for (LiveRunde runde : stand.runden()) {
            client.putRound(tournamentId, runde.nr(), LiveUebertragungsDaten.matches(runde, onlineIds));
        }
        loescheUeberzaehligeRunden(client, tournamentId,
                stand.runden().stream().mapToInt(LiveRunde::nr).max().orElse(0) + 1);
        if (!stand.rangliste().isEmpty()) {
            client.putRanking(tournamentId, LiveUebertragungsDaten.rangliste(stand.rangliste(), onlineIds));
        }
    }

    /** Runden, die lokal nicht mehr existieren (z.&nbsp;B. gelöschte letzte Spielrunde), auch online löschen. */
    private static void loescheUeberzaehligeRunden(TournamentSyncClient client, String tournamentId, int abRunde)
            throws IOException, InterruptedException {
        int runde = abRunde;
        while (runde <= MAX_RUNDEN_NR && client.deleteRound(tournamentId, runde)) {
            runde++;
        }
    }

    private static void zeigeFehler(XComponentContext ctx, String fehler) {
        LoMainThread.post(ctx, () -> MessageBox.from(ctx, MessageBoxTypeEnum.WARN_OK)
                .caption(I18n.get("ptmonline.fehler.titel"))
                .message(I18n.get("ptmonline.fehler.live_uebertragung", fehler))
                .show());
    }
}
