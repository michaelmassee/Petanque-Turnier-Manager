/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import de.petanqueturniermanager.basesheet.meldeliste.MeleeAnmeldungZeile;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.spielerdb.MeldelisteSpielerDaten;
import de.petanqueturniermanager.spielerdb.MeldelisteZiel;
import de.petanqueturniermanager.spielerdb.MeleeAnmeldungZiel;

/**
 * Übersetzt die lokalen Team-Nummern der Spielrunden (bei Supermelee: Spieler-Nummern) in die
 * Online-Registration-IDs von PTM-Online.
 * <ul>
 * <li>Ohne Mêlée ist jedes Team eine Online-Meldung: Team-Nr → Meldelistenzeile → lokale UUID → Online-ID.</li>
 * <li>Bei Mêlée-Anmeldung gibt es online nur Einzelspieler; ein lokal gemischtes Team wird über die Mêlée-Zeilen,
 * die in „Übernommen“ auf das Team verweisen, auf deren Online-IDs abgebildet ({@link MeleeTeilnahme#zeilenProTeam}).
 * Spieler ohne Verweis fehlen; ein Team wird nicht geraten.</li>
 * </ul>
 * Teams, für die nicht alle Spieler eine Online-ID haben, fehlen in der Zuordnung. Gelesen wird nur: Zeilen ohne
 * lokale UUID haben ohnehin keine Online-Zuordnung, und die Übertragung darf im Hintergrund nichts ins Dokument
 * schreiben.
 */
final class LiveNummernAufloesung {

    private LiveNummernAufloesung() {}

    /** @return Online-IDs je lokaler Team-/Spieler-Nr */
    static Map<Integer, List<String>> ermitteln(PtmOnlineVerbindung verbindung) throws GenerateException {
        Map<String, String> onlineIdProUuid = verbindung.mapping().getOnlineIdsProUuid();
        if (verbindung.ziel() instanceof MeleeAnmeldungZiel melee) {
            return meleeTeams(melee, verbindung.meldeliste(), onlineIdProUuid);
        }
        return teams(verbindung.ziel(), onlineIdProUuid);
    }

    private static Map<Integer, List<String>> teams(MeldelisteZiel ziel, Map<String, String> onlineIdProUuid)
            throws GenerateException {
        Map<Integer, Integer> zeileProTeam = PtmOnlineSpielrundeSync.zeileProTeam(ziel);
        Map<Integer, String> uuidProZeile = vorhandeneUuids(ziel, zeileProTeam.values());
        Map<Integer, List<String>> ergebnis = new LinkedHashMap<>();
        zeileProTeam.forEach((teamNr, zeile) -> {
            String onlineId = onlineId(onlineIdProUuid, uuidProZeile.get(zeile));
            if (onlineId != null) {
                ergebnis.put(teamNr, List.of(onlineId));
            }
        });
        return ergebnis;
    }

    private static Map<Integer, List<String>> meleeTeams(MeleeAnmeldungZiel melee, MeldelisteZiel meldeliste,
            Map<String, String> onlineIdProUuid) throws GenerateException {
        List<MeleeAnmeldungZeile> meleeZeilen = melee.leseMeleeZeilen();
        Map<Integer, List<MeldelisteSpielerDaten>> spielerProTeam = PtmOnlineSpielrundeSync.spielerProTeam(meldeliste);
        Map<Integer, List<Integer>> zeilenProTeam = MeleeTeilnahme.zeilenProTeam(meleeZeilen, spielerProTeam);
        Map<Integer, String> uuidProZeile = vorhandeneUuids(melee,
                zeilenProTeam.values().stream().flatMap(List::stream).toList());
        Map<Integer, List<String>> ergebnis = new LinkedHashMap<>();
        zeilenProTeam.forEach((teamNr, zeilen) -> {
            List<String> ids = new ArrayList<>();
            for (int zeile : zeilen) {
                String onlineId = onlineId(onlineIdProUuid, uuidProZeile.get(zeile));
                if (onlineId == null) {
                    return;
                }
                ids.add(onlineId);
            }
            ergebnis.put(teamNr, List.copyOf(ids));
        });
        return ergebnis;
    }

    private static Map<Integer, String> vorhandeneUuids(MeldelisteZiel ziel, Collection<Integer> zeilen1Basiert)
            throws GenerateException {
        try {
            return ziel.leseLokaleUuids(zeilen1Basiert);
        } catch (MeldelisteZiel.MeldelisteSchreibException e) {
            throw new GenerateException(e.getMessage());
        }
    }

    private static @Nullable String onlineId(Map<String, String> onlineIdProUuid, @Nullable String uuid) {
        return uuid == null ? null : onlineIdProUuid.get(uuid);
    }
}
