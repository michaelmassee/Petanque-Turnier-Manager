/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.util.Optional;

import org.jspecify.annotations.Nullable;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.LibreOfficePtmOnlineSpeicher;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.onlinesync.SpieltagKontext;
import de.petanqueturniermanager.spielerdb.MeldelisteZiel;
import de.petanqueturniermanager.spielerdb.MeldelisteZielFactory;

/**
 * Verbindung eines Turnierdokuments (bzw. bei Supermelee des aktiven Spieltags) mit einem PTM-Online-Turnier –
 * gemeinsame Grundlage für den Status-Abgleich beim Rundenstart und die Live-Übertragung.
 *
 * @param ziel       Sync-Ziel mit den lokalen PTM-Online-IDs (Meldeliste bzw. Mêlée-Anmeldung)
 * @param meldeliste Team-Meldeliste der Spielrunden; ohne Mêlée identisch mit {@code ziel}
 * @param spieltagNr Spieltag der Verbindung (nur Supermelee), sonst {@code null}
 */
record PtmOnlineVerbindung(LibreOfficePtmOnlineSpeicher.Zugangsdaten config, MeldelisteZiel ziel,
        MeldelisteZiel meldeliste, PtmOnlineRegistrationMapping mapping, String tournamentId,
        @Nullable Integer spieltagNr) {

    /**
     * @return leer, wenn kein PTM-Online-Zugang eingerichtet, keine Meldeliste vorhanden oder das Dokument nicht
     *         verbunden ist
     * @throws GenerateException wenn die Verbindungsdaten nicht lesbar sind
     */
    static Optional<PtmOnlineVerbindung> ermitteln(WorkingSpreadsheet ws, TurnierSystem ts) throws GenerateException {
        var config = new LibreOfficePtmOnlineSpeicher(ws.getxContext()).laden();
        if (!config.isConfigured()) {
            return Optional.empty();
        }
        Integer spieltagNr = SpieltagKontext.aktiverSpieltagOderNull(ws, ts);
        PtmOnlineRegistrationMapping mapping = new PtmOnlineRegistrationMapping(ws, ts, spieltagNr);
        Optional<String> tournamentId = mapping.getTournamentId();
        if (tournamentId.isEmpty()) {
            return Optional.empty();
        }
        Optional<MeldelisteZiel> ziel = MeldelisteZielFactory.fuerPtmOnline(ws);
        Optional<MeldelisteZiel> meldeliste = MeldelisteZielFactory.fuerAktivesSheet(ws);
        if (ziel.isEmpty() || meldeliste.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new PtmOnlineVerbindung(config, ziel.get(), meldeliste.get(), mapping,
                tournamentId.get(), spieltagNr));
    }

    /**
     * Client für die schreibenden Sync-Aufrufe dieses Dokuments (mit Dokumentbindung und Schreib-Lease).
     *
     * @throws GenerateException wenn die Dokumentbindung lokal unvollständig oder nicht lesbar ist
     */
    TournamentSyncClient gebundenerClient() throws GenerateException {
        Optional<String> syncDocumentId = mapping.getSyncDocumentId();
        Optional<String> leaseToken = mapping.getLeaseToken();
        if (syncDocumentId.isEmpty() || leaseToken.isEmpty()) {
            throw new GenerateException(I18n.get("ptmonline.fehler.dokumentbindung_unvollstaendig"));
        }
        return new TournamentSyncClient(config.baseUrl(), config.apiKey(), syncDocumentId.get(), leaseToken.get());
    }
}
