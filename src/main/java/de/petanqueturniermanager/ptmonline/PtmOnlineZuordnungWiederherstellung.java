/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.onlinesync.speicher.NeueZuordnung;
import de.petanqueturniermanager.ptmonline.dto.RegistrationDto;
import de.petanqueturniermanager.ptmonline.dto.ServerZuordnungDto;
import de.petanqueturniermanager.spielerdb.MeldelisteSpielerDaten;
import de.petanqueturniermanager.spielerdb.MeldelisteZiel;

/**
 * Baut fehlende Zuordnungen lokale UUID → Online-ID ausschließlich aus dem Mapping des Servers wieder auf (T-21). Nur
 * UUIDs, die in der Meldeliste stehen, werden zugeordnet – nie per Name, nie blind einer Zeile.
 * <p>
 * Der zuletzt abgeglichene Teamname kommt aus dem Mapping. Ohne ihn schickte der Meldelisten-Abgleich einer Meldeliste
 * ohne Teamname-Spalte bei der nächsten Änderung einen leeren Teamnamen und löschte ihn online. Liefert ein älterer
 * Server ihn noch nicht, kommt er aus den Online-Anmeldungen.
 */
public final class PtmOnlineZuordnungWiederherstellung {

    /** Gespeicherter Teamname-Stand „unbekannt“, siehe {@link AbgeglichenerTeamname#lese}. */
    private static final String TEAMNAME_UNBEKANNT = "";

    private PtmOnlineZuordnungWiederherstellung() {}

    /** @return Anzahl wiederhergestellter Zuordnungen */
    public static int ausfuehren(TournamentSyncClient client, PtmOnlineRegistrationMapping mapping,
            String tournamentId, MeldelisteZiel ziel) throws GenerateException, IOException, InterruptedException {
        Set<String> lokaleUuids = lokaleUuids(ziel);
        List<ServerZuordnungDto> fehlende = new ArrayList<>();
        for (ServerZuordnungDto server : client.fetchMapping(tournamentId)) {
            String uuid = server.localRegistrationUuid();
            if (uuid != null && server.onlineRegistrationId() != null && lokaleUuids.contains(uuid)
                    && mapping.getOnlineId(uuid).isEmpty()) {
                fehlende.add(server);
            }
        }
        if (fehlende.isEmpty()) {
            return 0;
        }
        Map<String, String> teamnamen = fehlende.stream().allMatch(server -> server.teamName() != null) ? Map.of()
                : teamnamenProOnlineId(client.fetchRegistrations(tournamentId, null));
        mapping.addMappings(fehlende.stream().map(server -> zuordnung(server, teamnamen)).toList());
        return fehlende.size();
    }

    /**
     * Zuordnung aus dem Server-Mapping samt zuletzt abgeglichenem Teamnamen.
     *
     * @param teamnamen gespeicherter Teamname-Stand je Online-ID, nur für ein Mapping ohne Teamnamen (älterer Server);
     *                  fehlt die Anmeldung auch dort, bleibt er unbekannt
     */
    static NeueZuordnung zuordnung(ServerZuordnungDto server, Map<String, String> teamnamen) {
        String name = (StringUtils.defaultString(server.firstName()) + " "
                + StringUtils.defaultString(server.lastName())).strip();
        return new NeueZuordnung(server.localRegistrationUuid(), server.onlineRegistrationId(), server.revision(),
                name, OnlineAnmeldeStatus.anzeige(server.status()), "", "", server.status(),
                AbgeglicheneBesetzung.ausOnline(server.personen(), server.club()).alsText(), "",
                server.teamName() != null ? AbgeglichenerTeamname.alsText(server.teamName())
                        : teamnamen.getOrDefault(server.onlineRegistrationId(), TEAMNAME_UNBEKANNT));
    }

    /** Gespeicherter Teamname-Stand je Online-ID ({@link AbgeglichenerTeamname#alsText}). */
    static Map<String, String> teamnamenProOnlineId(List<RegistrationDto> anmeldungen) {
        Map<String, String> teamnamen = new HashMap<>();
        for (RegistrationDto anmeldung : anmeldungen) {
            if (anmeldung.id() != null) {
                teamnamen.put(anmeldung.id(), AbgeglichenerTeamname.alsText(anmeldung.teamName()));
            }
        }
        return teamnamen;
    }

    private static Set<String> lokaleUuids(MeldelisteZiel ziel) throws GenerateException {
        List<Integer> zeilen = ziel.leseAlleSpielerRoh().stream().map(MeldelisteSpielerDaten::zeile1Basiert).distinct()
                .toList();
        try {
            return new HashSet<>(ziel.leseLokaleUuids(zeilen).values());
        } catch (MeldelisteZiel.MeldelisteSchreibException e) {
            throw new GenerateException(e.getMessage());
        }
    }
}
