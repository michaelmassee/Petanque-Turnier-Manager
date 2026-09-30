/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.stream.Collectors;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.ptmonline.auftrag.AuftragsBestand;
import de.petanqueturniermanager.ptmonline.auftrag.versand.VersandStopp;
import de.petanqueturniermanager.ptmonline.live.LiveStandQuelle;
import de.petanqueturniermanager.spielerdb.MeldelisteSpielerDaten;
import de.petanqueturniermanager.spielerdb.MeldelisteZiel;

/**
 * Erzeugt in UITests Aufträge wie die Produktivwege und sendet sie synchron an den {@link PtmOnlineTestServer} –
 * ohne Hintergrund-Beobachter.
 */
final class PtmOnlineAuftragsTestHilfe {

    private PtmOnlineAuftragsTestHilfe() {}

    /** Erfasst den Live-Stand als Aufträge und sendet sie, sofern der Sync nicht pausiert ist. */
    static VersandStopp liveUebertragen(WorkingSpreadsheet ws, PtmOnlineVerbindung verbindung, LiveStandQuelle quelle,
            OptionalInt rundenOnline) throws Exception {
        AuftragsBestand bestand = PtmOnlineAuftraege.bestand(ws, verbindung.mapping(), verbindung.spieltagNr());
        boolean pausiert = !PtmOnlineSpielrundeSync.istSyncAktiv(verbindung.mapping());
        if (!pausiert) {
            PtmOnlineLiveSync.erfasse(bestand, verbindung.tournamentId(), PtmOnlineLiveSync.lese(verbindung, quelle),
                    rundenOnline);
        }
        return PtmOnlineAuftraege.sendeSynchron(bestand, verbindung.mapping(), verbindung.gebundenerClient(), pausiert)
                .stopp();
    }

    /** Teilnahme-Auftrag aus den Einträgen erzeugen und senden. */
    static PtmOnlineAuftraege.Anwendung teilnahmeSenden(WorkingSpreadsheet ws, PtmOnlineRegistrationMapping mapping,
            TournamentSyncClient client, String tournamentId, List<PtmOnlineStatusAuftrag.Eintrag> eintraege)
            throws Exception {
        AuftragsBestand bestand = PtmOnlineAuftraege.bestand(ws, mapping, null);
        PtmOnlineAuftraege.teilnahme(bestand, tournamentId, eintraege, mapping.getOnlineIdsProUuid(),
                mapping.getExecutionRevisionenProUuid());
        return PtmOnlineAuftraege.sendeSynchron(bestand, mapping, client, false).anwendung();
    }

    /**
     * Wie ein Abgleich vor dem Start: aktive Meldungen ohne Online-Zuordnung werden angelegt (mit ihrer Teilnahme),
     * danach geht die Teilnahme aller zugeordneten Meldungen hinaus.
     *
     * @return Bezeichnungen der Meldungen, die PTM-Online als bereits angemeldet abgelehnt hat
     */
    static List<String> anlegenUndTeilnahmeSenden(WorkingSpreadsheet ws, MeldelisteZiel ziel,
            PtmOnlineRegistrationMapping mapping, TournamentSyncClient client, String tournamentId,
            List<LokaleOnlineMeldung> meldungen) throws Exception {
        AuftragsBestand bestand = PtmOnlineAuftraege.bestand(ws, mapping, null);
        PtmOnlineStatusAuftrag status = PtmOnlineSpielrundeSync.statusAuftrag(ziel, tournamentId, meldungen);
        Map<String, String> onlineIds = mapping.getOnlineIdsProUuid();
        Map<Integer, List<MeldelisteSpielerDaten>> spielerProZeile = ziel.leseAlleSpielerRoh().stream()
                .collect(Collectors.groupingBy(MeldelisteSpielerDaten::zeile1Basiert));
        Map<Integer, String> uuidProZeile = PtmOnlineSpielrundeSync.lokaleUuids(ziel,
                meldungen.stream().map(LokaleOnlineMeldung::zeile1Basiert).toList());
        for (LokaleOnlineMeldung meldung : meldungen) {
            String uuid = uuidProZeile.get(meldung.zeile1Basiert());
            if (meldung.teilnahme() == OnlineTeilnahme.AKTIV && !onlineIds.containsKey(uuid)) {
                List<MeldelisteSpielerDaten> spieler = spielerProZeile.get(meldung.zeile1Basiert());
                MeldelisteSpielerDaten erster = spieler.getFirst();
                PtmOnlineAuftraege.anlage(bestand, tournamentId, uuid, new de.petanqueturniermanager.ptmonline.dto
                        .NeueOnlineAnmeldung(erster.vorname(), erster.nachname(), null, null, null, null, null, null,
                                null, true, true, List.of(), List.of()),
                        meldung.teilnahme(), meldung.seedingPosition(), ziel.formelTeamNrAusLokalerUuid(uuid),
                        erster.vorname() + " " + erster.nachname());
            }
        }
        PtmOnlineAuftraege.teilnahme(bestand, tournamentId, status.eintraege(), onlineIds,
                mapping.getExecutionRevisionenProUuid());
        return PtmOnlineAuftraege.sendeSynchron(bestand, mapping, client, false).anwendung().abgelehnt();
    }
}
