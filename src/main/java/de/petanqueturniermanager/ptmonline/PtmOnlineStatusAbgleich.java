/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.onlinesync.sheet.NeueZuordnung;
import de.petanqueturniermanager.ptmonline.dto.RegistrationDto;
import de.petanqueturniermanager.ptmonline.dto.RegistrationResultDto;

/**
 * Sendet einen {@link PtmOnlineStatusAuftrag} an PTM-Online: startet bei Bedarf das Turnier, legt vor Ort erfasste
 * aktive Meldungen online an und pusht die Teilnahme aller zugeordneten Meldungen. Erst anlegen, dann pushen:
 * PTM-Online legt Nachmeldungen eines im Dokument durchgeführten Turniers inaktiv an – der Push setzt sie aktiv.
 * <p>
 * Senden und Zurückschreiben sind getrennt: {@link #senden} liest das Blatt „PTMOnline Sync“ nur und kann im
 * Hintergrund laufen, {@link #schreiben} braucht einen SheetRunner (Blattschutz). Das Anlegen ist über die lokale
 * UUID idempotent – scheitert das Senden mittendrin, legt die Wiederholung nichts doppelt an.
 */
final class PtmOnlineStatusAbgleich {

    private static final Logger logger = LogManager.getLogger(PtmOnlineStatusAbgleich.class);

    private PtmOnlineStatusAbgleich() {}

    /**
     * @param neueZuordnungen online angelegte Meldungen, für das Blatt „PTMOnline Sync“
     * @param revisionen      neue Ausführungsrevisionen je lokaler UUID (leer, wenn der Push unvollständig war)
     * @param abgelehnt       Bezeichnungen der Meldungen, die PTM-Online als bereits angemeldet abgelehnt hat
     */
    record Ergebnis(List<NeueZuordnung> neueZuordnungen, Map<String, Integer> revisionen, List<String> abgelehnt) {

        Ergebnis {
            neueZuordnungen = List.copyOf(neueZuordnungen);
            revisionen = Map.copyOf(revisionen);
            abgelehnt = List.copyOf(abgelehnt);
        }
    }

    static Ergebnis senden(PtmOnlineStatusAuftrag auftrag, PtmOnlineRegistrationMapping mapping,
            TournamentSyncClient client) throws IOException, InterruptedException, GenerateException {
        if (auftrag.turnierStarten()) {
            client.start(auftrag.tournamentId());
        }
        Map<String, String> onlineIds = new LinkedHashMap<>(mapping.getOnlineIdsProUuid());
        List<NeueZuordnung> neueZuordnungen = new ArrayList<>();
        List<String> abgelehnt = new ArrayList<>();
        for (PtmOnlineStatusAuftrag.Eintrag eintrag : auftrag.eintraege()) {
            if (eintrag.neueAnlage() == null || onlineIds.containsKey(eintrag.lokaleUuid())) {
                continue;
            }
            Optional<RegistrationDto> angelegt = legeOnlineAn(client, auftrag.tournamentId(), eintrag, abgelehnt);
            if (angelegt.isPresent()) {
                onlineIds.put(eintrag.lokaleUuid(), angelegt.get().id());
                neueZuordnungen.add(PtmOnlineRegistrationMapping.neueZuordnung(eintrag.lokaleUuid(),
                        eintrag.neueAnlage().nummerFormel(), eintrag.neueAnlage().bezeichnung(), angelegt.get()));
            }
        }
        Map<String, Integer> revisionen = teilnahmePushen(auftrag, client, onlineIds,
                mapping.getExecutionRevisionenProUuid());
        return new Ergebnis(neueZuordnungen, revisionen, abgelehnt);
    }

    static void schreiben(PtmOnlineRegistrationMapping mapping, Ergebnis ergebnis) throws GenerateException {
        mapping.addMappings(ergebnis.neueZuordnungen());
        if (!ergebnis.revisionen().isEmpty()) {
            mapping.setExecutionRevisionen(ergebnis.revisionen());
        }
    }

    /** @return leer, wenn PTM-Online die Meldung als bereits angemeldet ablehnt (dann in {@code abgelehnt}). */
    private static Optional<RegistrationDto> legeOnlineAn(TournamentSyncClient client, String tournamentId,
            PtmOnlineStatusAuftrag.Eintrag eintrag, List<String> abgelehnt) throws IOException, InterruptedException {
        PtmOnlineStatusAuftrag.NeueAnlage anlage = eintrag.neueAnlage();
        try {
            return Optional.of(client.upsertRegistration(tournamentId, eintrag.lokaleUuid(), anlage.anmeldung()));
        } catch (PtmOnlineHttpException e) {
            if (!e.istBereitsAngemeldet()) {
                throw e;
            }
            logger.warn("PTM-Online: Meldung {} online abgelehnt (bereits angemeldet)", eintrag.lokaleUuid(), e);
            abgelehnt.add(anlage.bezeichnung());
            return Optional.empty();
        }
    }

    /** @return neue Revisionen je UUID; leer, wenn PTM-Online nicht alle Meldungen aktualisiert hat */
    private static Map<String, Integer> teilnahmePushen(PtmOnlineStatusAuftrag auftrag, TournamentSyncClient client,
            Map<String, String> onlineIds, Map<String, Integer> revisionen) throws IOException, InterruptedException {
        List<RegistrationResultDto> results = new ArrayList<>();
        Map<String, Integer> neueRevisionen = new LinkedHashMap<>();
        for (PtmOnlineStatusAuftrag.Eintrag eintrag : auftrag.eintraege()) {
            String onlineId = onlineIds.get(eintrag.lokaleUuid());
            if (onlineId == null) {
                continue;
            }
            int revision = revisionen.getOrDefault(eintrag.lokaleUuid(), 1);
            results.add(new RegistrationResultDto(onlineId, null, eintrag.seedingPosition(),
                    eintrag.teilnahme().apiWert(), revision));
            neueRevisionen.put(eintrag.lokaleUuid(), revision + 1);
        }
        if (results.isEmpty()) {
            return Map.of();
        }
        int updatedCount = client.pushResults(auftrag.tournamentId(), results);
        if (updatedCount != results.size()) {
            logger.warn("PTM-Online: Status-Push aktualisierte nur {} von {} Anmeldungen; "
                    + "lokale executionRevision bleibt unveraendert fuer den naechsten Abgleich", updatedCount,
                    results.size());
            return Map.of();
        }
        return neueRevisionen;
    }
}
