/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.onlinesync.sheet.NeueZuordnung;
import de.petanqueturniermanager.ptmonline.dto.ServerZuordnungDto;
import de.petanqueturniermanager.spielerdb.MeldelisteSpielerDaten;
import de.petanqueturniermanager.spielerdb.MeldelisteZiel;

/**
 * Baut fehlende Zuordnungen lokale UUID → Online-ID ausschließlich aus dem Mapping des Servers wieder auf (T-21). Nur
 * UUIDs, die in der Meldeliste stehen, werden zugeordnet – nie per Name, nie blind einer Zeile.
 */
public final class PtmOnlineZuordnungWiederherstellung {

    private PtmOnlineZuordnungWiederherstellung() {}

    /** @return Anzahl wiederhergestellter Zuordnungen */
    public static int ausfuehren(TournamentSyncClient client, PtmOnlineRegistrationMapping mapping,
            String tournamentId, MeldelisteZiel ziel) throws GenerateException, IOException, InterruptedException {
        Set<String> lokaleUuids = lokaleUuids(ziel);
        List<NeueZuordnung> fehlende = new ArrayList<>();
        for (ServerZuordnungDto server : client.fetchMapping(tournamentId)) {
            String uuid = server.localRegistrationUuid();
            if (uuid == null || server.onlineRegistrationId() == null || !lokaleUuids.contains(uuid)
                    || mapping.getOnlineId(uuid).isPresent()) {
                continue;
            }
            String name = (StringUtils.defaultString(server.firstName()) + " "
                    + StringUtils.defaultString(server.lastName())).strip();
            fehlende.add(new NeueZuordnung(uuid, server.onlineRegistrationId(), formel(ziel, uuid), server.revision(),
                    name, OnlineAnmeldeStatus.anzeige(server.status()), "", "", server.status()));
        }
        mapping.addMappings(fehlende);
        return fehlende.size();
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

    private static String formel(MeldelisteZiel ziel, String uuid) throws GenerateException {
        try {
            return ziel.formelTeamNrAusLokalerUuid(uuid);
        } catch (MeldelisteZiel.MeldelisteSchreibException e) {
            throw new GenerateException(e.getMessage());
        }
    }
}
