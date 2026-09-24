/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.ui;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.StringUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.LibreOfficePtmOnlineSpeicher;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.LoMainThread;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxResult;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.onlinesync.OnlineTournamentDto;
import de.petanqueturniermanager.onlinesync.SpieltagKontext;
import de.petanqueturniermanager.onlinesync.TurnierSystemOnlineTypMapping;
import de.petanqueturniermanager.ptmonline.PtmOnlineHttpException;
import de.petanqueturniermanager.ptmonline.PtmOnlineRegistrationMapping;
import de.petanqueturniermanager.ptmonline.RegistrationImportTask;
import de.petanqueturniermanager.ptmonline.TournamentSyncClient;
import de.petanqueturniermanager.ptmonline.dto.SyncBindingDto;

/**
 * Bindeglied zwischen {@link de.petanqueturniermanager.comp.ProtocolHandler} und der PTM-Online-
 * REST-Anbindung. Muster analog {@link de.petanqueturniermanager.spielerdb.ui.SpielerDbDispatcher}:
 * statische Methoden, Fehler werden als {@link MessageBox} gemeldet statt zu crashen. Netzwerk-I/O
 * laeuft off-thread (Dispatch-Aufrufkette darf nicht blockieren); Sheet-/Dokument-Schreibzugriffe
 * werden per {@link LoMainThread#post} zurueck auf den Main-Thread marshalliert.
 */
public final class PtmOnlineDispatcher {

    private static final Logger logger = LogManager.getLogger(PtmOnlineDispatcher.class);
    private static final Duration RUECKFRAGE_TIMEOUT = Duration.ofMinutes(30);

    private PtmOnlineDispatcher() {}

    /**
     * Verbindet das Dokument (bzw. bei Supermelee den aktiven Spieltag) mit einem bereits
     * bestehenden PTM-Online-Turnier: laedt die zum API-Key gehoerenden, zum lokalen
     * {@link TurnierSystem} passenden Turniere, laesst den Nutzer eines auswaehlen und legt
     * anschliessend das Blatt "PTMOnline Sync" an.
     */
    public static void turnierVerbinden(WorkingSpreadsheet ws) {
        logger.info("PTM-Online: turnierVerbinden() gestartet (Thread={})", Thread.currentThread().getName());
        XComponentContext ctx = ws.getxContext();
        var config = new LibreOfficePtmOnlineSpeicher(ctx).laden();
        if (!config.isConfigured()) {
            logger.info("PTM-Online: nicht konfiguriert, breche ab");
            zeigeFehler(ctx, I18n.get("ptmonline.fehler.nicht_konfiguriert"));
            return;
        }

        TurnierSystem ts;
        Integer spieltagNr;
        try {
            ts = new DocumentPropertiesHelper(ws).getTurnierSystemAusDocument();
            spieltagNr = SpieltagKontext.aktiverSpieltagOderNull(ws, ts);
        } catch (GenerateException e) {
            logger.error("PTM-Online: aktiven Spieltag ermitteln fehlgeschlagen", e);
            zeigeFehler(ctx, e.getMessage());
            return;
        }
        logger.info("PTM-Online: Turniersystem={}, Spieltag={}", ts, spieltagNr);

        if (TurnierSystemOnlineTypMapping.onlineTyp(ts).isEmpty()) {
            logger.info("PTM-Online: Turniersystem {} nicht unterstuetzt, breche ab", ts);
            zeigeFehler(ctx, I18n.get("ptmonline.turnier.verbinden.dialog.fehler.system_nicht_unterstuetzt"));
            return;
        }

        Thread worker = new Thread(
                () -> verbindenImHintergrund(ws, ctx, config, ts, spieltagNr), "PTM-Online-Verbinden");
        worker.start();
        logger.info("PTM-Online: Hintergrund-Thread gestartet, turnierVerbinden() kehrt zurueck");
    }

    private static void verbindenImHintergrund(WorkingSpreadsheet ws, XComponentContext ctx,
            LibreOfficePtmOnlineSpeicher.Zugangsdaten config, TurnierSystem ts, Integer spieltagNr) {
        logger.info("PTM-Online: verbindenImHintergrund() gestartet (Thread={}), lade Turnierliste von {}",
                Thread.currentThread().getName(), config.baseUrl());
        List<OnlineTournamentDto> passende;
        try {
            TournamentSyncClient client = new TournamentSyncClient(config.baseUrl(), config.apiKey());
            passende = client.listTournaments().stream()
                    .filter(t -> TurnierSystemOnlineTypMapping.passtZu(ts, t))
                    .toList();
            logger.info("PTM-Online: {} passende Turniere geladen", passende.size());
        } catch (IOException e) {
            logger.error("PTM-Online: Turnierliste laden fehlgeschlagen", e);
            LoMainThread.post(ctx, () -> zeigeNetzwerkFehler(ctx, e));
            return;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        PtmOnlineRegistrationMapping mapping = new PtmOnlineRegistrationMapping(ws, ts, spieltagNr);
        LokaleBindung bisherige;
        try {
            bisherige = LokaleBindung.aus(mapping);
        } catch (GenerateException e) {
            logger.error("PTM-Online: bisherige Verbindung lesen fehlgeschlagen", e);
            LoMainThread.post(ctx, () -> zeigeFehler(ctx, e.getMessage()));
            return;
        }

        logger.info("PTM-Online: zeige Auswahldialog");
        Optional<OnlineTournamentDto> auswahl = zeigeAuswahlDialog(ws, ctx, passende, bisherige.turnierId());
        logger.info("PTM-Online: Auswahldialog beendet, Auswahl vorhanden={}", auswahl.isPresent());
        if (auswahl.isEmpty()) {
            return; // Abgebrochen oder keine Turniere vorhanden
        }
        OnlineTournamentDto turnier = auswahl.get();

        // Das Dokument behält seine Identität: ein erneutes Verbinden läuft dann nicht gegen die eigene Bindung.
        String syncDocumentId = bisherige.syncDocumentId().orElseGet(() -> UUID.randomUUID().toString());
        String leaseToken = bisherige.leaseToken().orElseGet(() -> UUID.randomUUID().toString() + UUID.randomUUID());
        Optional<SyncBindingDto> binding;
        try {
            if (bisherige.istVerbundenMitAnderem(turnier.id)) {
                gibBisherigesTurnierFrei(config, bisherige);
            }
            binding = verbindeOderUebernehme(ctx, new TournamentSyncClient(config.baseUrl(), config.apiKey()),
                    turnier, syncDocumentId, leaseToken);
        } catch (IOException e) {
            logger.error("PTM-Online: Turnier verbinden fehlgeschlagen", e);
            LoMainThread.post(ctx, () -> zeigeNetzwerkFehler(ctx, e));
            return;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        if (binding.isEmpty()) {
            logger.info("PTM-Online: Übernahme von Turnier {} vom Nutzer abgelehnt", turnier.id);
            return;
        }
        logger.info("PTM-Online: Turnier {} verbunden (Server-Aufruf ok)", turnier.id);

        logger.info("PTM-Online: lege Sync-Blatt fuer Verbindung an");
        try {
            mapping.verbinden(turnier, binding.get(), leaseToken);
            logger.info("PTM-Online: Sync-Blatt angelegt, zeige Erfolg");
            LoMainThread.post(ctx, () -> zeigeErfolg(ctx, turnier));
        } catch (GenerateException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            logger.error("PTM-Online: Sync-Blatt fuer Verbindung anlegen fehlgeschlagen", e);
            LoMainThread.post(ctx, () -> zeigeFehler(ctx, e.getMessage()));
        }
    }

    /** Bisherige Verbindung dieses Dokuments (bzw. Spieltags) laut Blatt „PTMOnline Sync“, jeweils leer ohne. */
    private record LokaleBindung(Optional<String> turnierIdOpt, Optional<String> syncDocumentId,
            Optional<String> leaseToken) {

        static LokaleBindung aus(PtmOnlineRegistrationMapping mapping) throws GenerateException {
            return new LokaleBindung(mapping.getTournamentId(), mapping.getSyncDocumentId(), mapping.getLeaseToken());
        }

        String turnierId() {
            return turnierIdOpt.orElse(null);
        }

        boolean istVerbundenMitAnderem(String turnierId) {
            return turnierIdOpt.isPresent() && !turnierIdOpt.get().equals(turnierId);
        }
    }

    /**
     * Online und Dokument sind immer 1:1 verbunden: wechselt das Dokument das Online-Turnier, wird das bisherige
     * online freigegeben. Scheitert das (z.&nbsp;B. weil es inzwischen ein anderes Dokument übernommen hat), geht
     * das Verbinden trotzdem weiter – das bisherige Turnier gehört dann ohnehin nicht mehr zu diesem Dokument.
     */
    private static void gibBisherigesTurnierFrei(LibreOfficePtmOnlineSpeicher.Zugangsdaten config,
            LokaleBindung bisherige) throws InterruptedException {
        if (bisherige.syncDocumentId().isEmpty() || bisherige.leaseToken().isEmpty()) {
            return;
        }
        try {
            new TournamentSyncClient(config.baseUrl(), config.apiKey(), bisherige.syncDocumentId().get(),
                    bisherige.leaseToken().get()).disconnect(bisherige.turnierId());
            logger.info("PTM-Online: bisheriges Turnier {} freigegeben", bisherige.turnierId());
        } catch (IOException e) {
            logger.warn("PTM-Online: bisheriges Turnier {} konnte nicht freigegeben werden", bisherige.turnierId(), e);
        }
    }

    /**
     * Verbindet; ist das Online-Turnier bereits mit einem anderen Dokument verbunden, wird nach Rückfrage
     * übernommen – das andere Dokument verliert seine Bindung.
     *
     * @return leer, wenn der Nutzer die Übernahme ablehnt
     */
    private static Optional<SyncBindingDto> verbindeOderUebernehme(XComponentContext ctx, TournamentSyncClient client,
            OnlineTournamentDto turnier, String syncDocumentId, String leaseToken)
            throws IOException, InterruptedException {
        try {
            return Optional.of(client.connect(turnier.id, syncDocumentId, leaseToken));
        } catch (PtmOnlineHttpException e) {
            OptionalLong bindingRevision = e.bindingRevision();
            if (!e.istAnderesDokumentGebunden() || bindingRevision.isEmpty()) {
                throw e;
            }
            logger.info("PTM-Online: Turnier {} ist mit einem anderen Dokument verbunden, frage nach Übernahme",
                    turnier.id);
            if (!frageAufMainThread(ctx, I18n.get("ptmonline.frage.verbindung_uebernehmen",
                    StringUtils.defaultString(turnier.name)))) {
                return Optional.empty();
            }
            return Optional.of(client.takeover(turnier.id, syncDocumentId, leaseToken, bindingRevision.getAsLong()));
        }
    }

    /**
     * Ja/Nein-Rückfrage aus dem Hintergrund-Thread: die MessageBox läuft auf dem Main-Thread, der Aufrufer wartet.
     * Nie vom Main-Thread aus aufrufen (Deadlock).
     */
    private static boolean frageAufMainThread(XComponentContext ctx, String frage) throws InterruptedException {
        var antwort = new CompletableFuture<Boolean>();
        LoMainThread.post(ctx, () -> antwort.complete(MessageBox.from(ctx, MessageBoxTypeEnum.WARN_YES_NO)
                .caption(I18n.get("ptmonline.menu.toplevel"))
                .message(frage)
                .show() == MessageBoxResult.YES));
        try {
            return antwort.get(RUECKFRAGE_TIMEOUT.toMinutes(), TimeUnit.MINUTES);
        } catch (ExecutionException | TimeoutException e) {
            logger.warn("PTM-Online: Rückfrage ohne Antwort", e);
            return false;
        }
    }

    /**
     * Läuft auf einem Hintergrund-Thread (siehe {@link #verbindenImHintergrund}), NICHT dem
     * LO-Main-Thread — die {@link ProcessBox} darf hier daher NICHT direkt angefasst werden
     * (setVisible/hide sind VCL-Aufrufe, siehe Threading-Regel in CLAUDE.md). Das eigentliche
     * Anzeigen des Dialogs marshalliert {@link PtmOnlineTurnierVerbindenDialog#zeigen} bereits
     * selbst per {@code LoMainThread.post} zurück auf den Main-Thread.
     */
    private static Optional<OnlineTournamentDto> zeigeAuswahlDialog(WorkingSpreadsheet ws, XComponentContext ctx,
            List<OnlineTournamentDto> passende, String eigeneTurnierId) {
        try {
            return PtmOnlineTurnierVerbindenDialog.zeigen(ctx, ws.getContainerWindowPeer(), passende, eigeneTurnierId);
        } catch (GenerateException e) {
            logger.error("PTM-Online-Verbinden-Dialog fehlgeschlagen", e);
            return Optional.empty();
        }
    }

    private static void zeigeErfolg(XComponentContext ctx, OnlineTournamentDto turnier) {
        MessageBox.from(ctx, MessageBoxTypeEnum.INFO_OK)
                .caption(I18n.get("ptmonline.menu.toplevel"))
                .message(I18n.get("ptmonline.erfolg.turnier_verbunden", turnier.name))
                .show();
    }

    /**
     * Trennt die Verbindung des Dokuments (bzw. bei Supermelee des aktiven Spieltags) zu seinem
     * PTM-Online-Turnier wieder: fragt beim Nutzer nach, hebt serverseitig die
     * Dokument-Verwaltung ({@code document_managed}) wieder auf und entfernt das Blatt
     * "PTMOnline Sync".
     */
    public static void verbindungTrennen(WorkingSpreadsheet ws) {
        logger.info("PTM-Online: verbindungTrennen() gestartet (Thread={})", Thread.currentThread().getName());
        XComponentContext ctx = ws.getxContext();
        var config = new LibreOfficePtmOnlineSpeicher(ctx).laden();
        if (!config.isConfigured()) {
            zeigeFehler(ctx, I18n.get("ptmonline.fehler.nicht_konfiguriert"));
            return;
        }

        TurnierSystem ts;
        Integer spieltagNr;
        PtmOnlineRegistrationMapping mapping;
        Optional<String> tournamentId;
        try {
            ts = new DocumentPropertiesHelper(ws).getTurnierSystemAusDocument();
            spieltagNr = SpieltagKontext.aktiverSpieltagOderNull(ws, ts);
            mapping = new PtmOnlineRegistrationMapping(ws, ts, spieltagNr);
            tournamentId = mapping.getTournamentId();
        } catch (GenerateException e) {
            logger.error("PTM-Online: Verbindungsstatus ermitteln fehlgeschlagen", e);
            zeigeFehler(ctx, e.getMessage());
            return;
        }
        if (tournamentId.isEmpty()) {
            zeigeFehler(ctx, I18n.get("ptmonline.fehler.turnier_nicht_verbunden"));
            return;
        }

        MessageBoxResult antwort = MessageBox.from(ctx, MessageBoxTypeEnum.WARN_YES_NO)
                .caption(I18n.get("ptmonline.menu.toplevel"))
                .message(I18n.get("ptmonline.trennen.dialog.frage"))
                .show();
        if (antwort != MessageBoxResult.YES) {
            logger.info("PTM-Online: Trennen vom Nutzer abgelehnt");
            return;
        }

        PtmOnlineRegistrationMapping finaleMapping = mapping;
        String finaleTournamentId = tournamentId.get();
        var lokalEntfernen = new PtmOnlineVerbindungsRunner(ws, ts, spieltagNr,
                PtmOnlineVerbindungsRunner.Aktion.LOKAL_ENTFERNEN);
        Thread worker = new Thread(
                () -> trennenImHintergrund(ctx, config, finaleMapping, finaleTournamentId, lokalEntfernen),
                "PTM-Online-Trennen");
        worker.start();
    }

    /**
     * Hält den Sync der Verbindung an (bzw. setzt ihn fort) – ohne Server-Aufruf, jederzeit auch offline. Die
     * Verbindung selbst bleibt bestehen; solange pausiert, laufen weder Meldungsabgleich noch Rundenstart-Sync.
     */
    public static void syncPausieren(WorkingSpreadsheet ws) {
        starteVerbindungsAktion(ws, PtmOnlineVerbindungsRunner.Aktion.PAUSIEREN);
    }

    public static void syncFortsetzen(WorkingSpreadsheet ws) {
        starteVerbindungsAktion(ws, PtmOnlineVerbindungsRunner.Aktion.FORTSETZEN);
    }

    private static void starteVerbindungsAktion(WorkingSpreadsheet ws, PtmOnlineVerbindungsRunner.Aktion aktion) {
        try {
            TurnierSystem ts = new DocumentPropertiesHelper(ws).getTurnierSystemAusDocument();
            Integer spieltagNr = SpieltagKontext.aktiverSpieltagOderNull(ws, ts);
            new PtmOnlineVerbindungsRunner(ws, ts, spieltagNr, aktion).start();
        } catch (GenerateException e) {
            logger.error("PTM-Online: {} fehlgeschlagen", aktion, e);
            zeigeFehler(ws.getxContext(), e.getMessage());
        }
    }

    private static void trennenImHintergrund(XComponentContext ctx,
            LibreOfficePtmOnlineSpeicher.Zugangsdaten config, PtmOnlineRegistrationMapping mapping, String tournamentId,
            PtmOnlineVerbindungsRunner lokalEntfernen) {
        try {
            Optional<String> documentId = mapping.getSyncDocumentId();
            Optional<String> leaseToken = mapping.getLeaseToken();
            if (documentId.isEmpty() || leaseToken.isEmpty()) {
                throw new GenerateException(I18n.get("ptmonline.fehler.dokumentbindung_unvollstaendig"));
            }
            TournamentSyncClient client = new TournamentSyncClient(config.baseUrl(), config.apiKey(), documentId.get(), leaseToken.get());
            client.disconnect(tournamentId);
            logger.info("PTM-Online: Turnier {} getrennt (Server-Aufruf ok)", tournamentId);
        } catch (PtmOnlineHttpException e) {
            if (!e.istBindungAbgeloest()) {
                logger.error("PTM-Online: Verbindung trennen (Server-Aufruf) fehlgeschlagen", e);
                LoMainThread.post(ctx, () -> zeigeNetzwerkFehler(ctx, e));
                return;
            }
            // Online hält inzwischen ein anderes Dokument das Turnier (oder niemand) – lokal trotzdem aufräumen.
            logger.info("PTM-Online: Turnier {} online nicht mehr an dieses Dokument gebunden, trenne nur lokal",
                    tournamentId, e);
        } catch (IOException e) {
            logger.error("PTM-Online: Verbindung trennen (Server-Aufruf) fehlgeschlagen", e);
            LoMainThread.post(ctx, () -> zeigeNetzwerkFehler(ctx, e));
            return;
        } catch (GenerateException e) {
            logger.error("PTM-Online: Schreib-Lease lesen fehlgeschlagen", e);
            LoMainThread.post(ctx, () -> zeigeFehler(ctx, e.getMessage()));
            return;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        lokalEntfernen.start();
    }

    public static void anmeldungenImportieren(WorkingSpreadsheet ws) {
        RegistrationImportTask.starte(ws);
    }

    private static void zeigeNetzwerkFehler(XComponentContext ctx, IOException e) {
        if (e instanceof PtmOnlineHttpException http && http.istBindungAbgeloest()) {
            zeigeFehler(ctx, I18n.get("ptmonline.fehler.bindung_abgeloest"));
            return;
        }
        String meldung = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        String key = meldung.contains(" 401") ? "ptmonline.fehler.nicht_freigeschaltet" : "ptmonline.fehler.netzwerk";
        MessageBox.from(ctx, MessageBoxTypeEnum.ERROR_OK)
                .caption(I18n.get("ptmonline.fehler.titel"))
                .message(key.equals("ptmonline.fehler.netzwerk") ? I18n.get(key, meldung) : I18n.get(key))
                .show();
    }

    private static void zeigeFehler(XComponentContext ctx, String meldung) {
        MessageBox.from(ctx, MessageBoxTypeEnum.ERROR_OK)
                .caption(I18n.get("ptmonline.fehler.titel"))
                .message(meldung)
                .show();
    }
}
