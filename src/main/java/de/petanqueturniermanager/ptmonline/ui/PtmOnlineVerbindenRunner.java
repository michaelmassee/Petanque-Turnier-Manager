/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.ui;

import java.io.IOException;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.LibreOfficePtmOnlineSpeicher;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxResult;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.onlinesync.OnlineTournamentDto;
import de.petanqueturniermanager.ptmonline.PtmOnlineAuftraege;
import de.petanqueturniermanager.ptmonline.PtmOnlineFehlerText;
import de.petanqueturniermanager.ptmonline.PtmOnlineHttpException;
import de.petanqueturniermanager.ptmonline.PtmOnlineLiveBeobachter;
import de.petanqueturniermanager.ptmonline.PtmOnlineRegistrationMapping;
import de.petanqueturniermanager.ptmonline.PtmOnlineTrennung;
import de.petanqueturniermanager.ptmonline.TournamentSyncClient;
import de.petanqueturniermanager.ptmonline.auftrag.AuftragsArt;
import de.petanqueturniermanager.ptmonline.auftrag.SyncAuftrag;
import de.petanqueturniermanager.ptmonline.dto.SyncBindingDto;

/**
 * Verbindet das Dokument (bzw. den Spieltag) mit dem ausgewählten Online-Turnier – Server-Bindung, Blatt
 * „PTMOnline Sync“ und Freigabe des bisherigen Turniers in einem einzigen {@link SheetRunner}. Läuft bereits ein
 * anderer Runner, lehnt {@link #start()} ab, bevor der Server berührt wird: Online und Dokument können so nicht
 * auseinanderlaufen. Das bisherige Turnier wird erst freigegeben, wenn das neue lokal eingetragen ist.
 */
final class PtmOnlineVerbindenRunner extends SheetRunner {

    private final LibreOfficePtmOnlineSpeicher.Zugangsdaten config;
    private final Integer spieltagNr;
    private final OnlineTournamentDto turnier;

    PtmOnlineVerbindenRunner(WorkingSpreadsheet ws, TurnierSystem turnierSystem, Integer spieltagNrOderNull,
            LibreOfficePtmOnlineSpeicher.Zugangsdaten config, OnlineTournamentDto turnier) {
        super(ws, turnierSystem, "PTM-Online");
        this.config = config;
        this.spieltagNr = spieltagNrOderNull;
        this.turnier = turnier;
    }

    @Override
    protected IKonfigurationSheet getKonfigurationSheet() {
        return null;
    }

    @Override
    protected void doRun() throws GenerateException {
        PtmOnlineRegistrationMapping mapping = new PtmOnlineRegistrationMapping(getWorkingSpreadsheet(),
                getTurnierSystem(), spieltagNr);
        LokaleBindung bisherige = LokaleBindung.aus(mapping);
        // Das Dokument behält seine Identität: ein erneutes Verbinden läuft dann nicht gegen die eigene Bindung.
        String syncDocumentId = bisherige.syncDocumentId().orElseGet(() -> UUID.randomUUID().toString());
        String leaseToken = bisherige.leaseToken().orElseGet(() -> UUID.randomUUID().toString() + UUID.randomUUID());
        // Stabile ID des Verbindungsversuchs: eine Wiederholung nach verlorener Antwort ist dieselbe Verbindung (P-22).
        String connectRequestId = mapping.getConnectRequestId().orElseGet(() -> UUID.randomUUID().toString());
        if (bisherige.syncDocumentId().isPresent()) {
            mapping.setConnectRequestId(connectRequestId);
        }
        TournamentSyncClient client = new TournamentSyncClient(config.baseUrl(), config.apiKey());

        Optional<SyncBindingDto> binding = serverAufruf(
                () -> verbindeOderUebernehme(client, syncDocumentId, leaseToken, connectRequestId));
        if (binding.isEmpty()) {
            getLogger().info("PTM-Online: Verbinden von Turnier {} vom Nutzer abgebrochen", turnier.id);
            return;
        }
        getLogger().info("PTM-Online: Turnier {} verbunden (Server-Aufruf ok)", turnier.id);

        if (bisherige.istVerbundenMitAnderem(turnier.id)) {
            gibBisherigesTurnierFrei(mapping);
        }
        try {
            mapping.verbinden(turnier, binding.get(), leaseToken);
            PtmOnlineAuftraege.neueBindung(getWorkingSpreadsheet(), mapping, spieltagNr,
                    binding.get().writeCounter(), I18n.get("ptmonline.auftrag.verworfen.neue_bindung"));
            mapping.setConnectRequestId("");
        } catch (GenerateException | RuntimeException e) {
            getLogger().error("PTM-Online: Sync-Blatt für Verbindung anlegen fehlgeschlagen", e);
            if (!turnier.id.equals(bisherige.turnierId())) {
                gibNeueBindungFrei(syncDocumentId, leaseToken, binding.get().writeCounter());
            }
            throw new GenerateException(I18n.get("ptmonline.sheet.fehler.anlegen"));
        }
        PtmOnlineLiveBeobachter.bindungErneuert(getWorkingSpreadsheet().getWorkingSpreadsheetDocument(), turnier.id);
        MessageBox.from(getxContext(), MessageBoxTypeEnum.INFO_OK)
                .caption(I18n.get("ptmonline.menu.toplevel"))
                .message(I18n.get("ptmonline.erfolg.turnier_verbunden", turnier.name))
                .show();
    }

    /**
     * Verbindet; ist das Online-Turnier bereits mit einem anderen Dokument verbunden, wird nach Rückfrage
     * übernommen – das andere Dokument verliert seine Bindung. Läuft das Turnier online schon, ist beides nur als
     * ausdrückliche Wiederherstellung zulässig (E-03, P-32).
     *
     * @return leer, wenn der Nutzer Übernahme oder Wiederherstellung ablehnt
     */
    private Optional<SyncBindingDto> verbindeOderUebernehme(TournamentSyncClient client, String syncDocumentId,
            String leaseToken, String connectRequestId) throws IOException, InterruptedException {
        try {
            return Optional.of(client.connect(turnier.id, syncDocumentId, leaseToken, connectRequestId, false));
        } catch (PtmOnlineHttpException e) {
            if (e.istWiederherstellungNoetig()) {
                return bestaetigeWiederherstellung(e.rundenOnline()) ? Optional.of(
                        client.connect(turnier.id, syncDocumentId, leaseToken, connectRequestId, true))
                        : Optional.empty();
            }
            OptionalLong bindingRevision = e.bindingRevision();
            if (!e.istAnderesDokumentGebunden() || bindingRevision.isEmpty()) {
                throw e;
            }
            getLogger().info("PTM-Online: Turnier {} ist mit einem anderen Dokument verbunden, frage nach Übernahme",
                    turnier.id);
            MessageBoxResult antwort = MessageBox.from(getxContext(), MessageBoxTypeEnum.WARN_YES_NO)
                    .caption(I18n.get("ptmonline.menu.toplevel"))
                    .message(I18n.get("ptmonline.frage.verbindung_uebernehmen", StringUtils.defaultString(turnier.name)))
                    .show();
            if (antwort != MessageBoxResult.YES) {
                return Optional.empty();
            }
            return uebernehme(client, syncDocumentId, leaseToken, bindingRevision.getAsLong());
        }
    }

    private Optional<SyncBindingDto> uebernehme(TournamentSyncClient client, String syncDocumentId,
            String leaseToken, long bindingRevision) throws IOException, InterruptedException {
        String takeoverRequestId = UUID.randomUUID().toString();
        try {
            return Optional.of(client.takeover(turnier.id, syncDocumentId, leaseToken, bindingRevision,
                    takeoverRequestId, false));
        } catch (PtmOnlineHttpException e) {
            if (!e.istWiederherstellungNoetig()) {
                throw e;
            }
            return bestaetigeWiederherstellung(e.rundenOnline()) ? Optional.of(client.takeover(turnier.id,
                    syncDocumentId, leaseToken, bindingRevision, takeoverRequestId, true)) : Optional.empty();
        }
    }

    /**
     * Das Online-Turnier läuft bereits: dieses Dokument wird es als Wiederherstellung übernehmen. Die Rückfrage nennt
     * die online vorhandenen Runden; sie werden beim nächsten Rundenpush durch den Stand dieses Dokuments ersetzt
     * (KP-08, P-32).
     */
    private boolean bestaetigeWiederherstellung(int rundenOnline) {
        return MessageBox.from(getxContext(), MessageBoxTypeEnum.WARN_YES_NO)
                .caption(I18n.get("ptmonline.frage.wiederherstellung.titel"))
                .message(I18n.get("ptmonline.frage.wiederherstellung", StringUtils.defaultString(turnier.name),
                        rundenOnline))
                .show() == MessageBoxResult.YES;
    }

    /**
     * Online und Dokument sind immer 1:1 verbunden: wechselt das Dokument das Online-Turnier, wird das bisherige
     * online freigegeben – noch mit der bisherigen Bindung und ihrem Schreibzähler. Scheitert das (z.&nbsp;B. weil es
     * inzwischen ein anderes Dokument übernommen hat), bleibt die neue Verbindung trotzdem bestehen – das bisherige
     * Turnier gehört dann ohnehin nicht mehr zu diesem Dokument.
     */
    private void gibBisherigesTurnierFrei(PtmOnlineRegistrationMapping mapping) throws GenerateException {
        try {
            PtmOnlineTrennung.online(getWorkingSpreadsheet(), spieltagNr, config, mapping);
        } catch (GenerateException | IOException e) {
            getLogger().warn("PTM-Online: bisheriges Turnier konnte nicht freigegeben werden", e);
        } catch (InterruptedException e) {
            getLogger().debug("PTM-Online: Freigabe des bisherigen Turniers abgebrochen", e);
            throw verarbeitungAbgebrochen();
        }
    }

    /** Gibt eine gerade erzeugte Bindung wieder frei, deren Blatt nicht angelegt werden konnte. */
    private void gibNeueBindungFrei(String syncDocumentId, String leaseToken, long writeCounter)
            throws GenerateException {
        SyncAuftrag trennen = new SyncAuftrag(UUID.randomUUID().toString(), writeCounter + 1, AuftragsArt.TRENNEN,
                "POST", TournamentSyncClient.trennenPfad(turnier.id), "{}", "{}");
        try {
            new TournamentSyncClient(config.baseUrl(), config.apiKey(), syncDocumentId, leaseToken).sende(trennen);
            getLogger().info("PTM-Online: Turnier {} freigegeben", turnier.id);
        } catch (IOException e) {
            getLogger().warn("PTM-Online: Turnier {} konnte nicht freigegeben werden", turnier.id, e);
        } catch (InterruptedException e) {
            getLogger().debug("PTM-Online: Freigabe von Turnier {} abgebrochen", turnier.id, e);
            throw verarbeitungAbgebrochen();
        }
    }

    /**
     * Netzwerkfehler werden zur Anwendermeldung des Runners. Eine Unterbrechung (Stop-Knopf) gilt als Abbruch;
     * das Interrupt-Flag bleibt verbraucht, damit die UNO-Aufräumaufrufe nicht auf einem unterbrochenen Thread laufen.
     */
    private <T> T serverAufruf(ServerAufruf<T> aufruf) throws GenerateException {
        try {
            return aufruf.ausfuehren();
        } catch (IOException e) {
            getLogger().error("PTM-Online: Turnier verbinden fehlgeschlagen", e);
            throw new GenerateException(PtmOnlineFehlerText.fuer(e));
        } catch (InterruptedException e) {
            getLogger().debug("PTM-Online: Verbinden während eines Serveraufrufs abgebrochen", e);
            throw verarbeitungAbgebrochen();
        }
    }

    @FunctionalInterface
    private interface ServerAufruf<T> {
        T ausfuehren() throws IOException, InterruptedException;
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
}
