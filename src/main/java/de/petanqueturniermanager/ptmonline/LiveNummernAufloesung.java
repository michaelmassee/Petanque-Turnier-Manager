/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
 * <li>Bei Mêlée-Anmeldung gibt es online nur Einzelspieler; ein lokal gemischtes Team wird über die Namen seiner
 * Spieler auf deren Online-IDs abgebildet (wie {@link MeleeTeilnahme}). Mehrdeutige Namen werden nicht geraten.</li>
 * </ul>
 * Teams, für die nicht alle Spieler eine Online-ID haben, fehlen in der Zuordnung.
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
        Map<Integer, String> uuidProZeile = PtmOnlineSpielrundeSync.lokaleUuids(ziel, zeileProTeam.values());
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
        Map<String, String> onlineIdProName = onlineIdProName(melee, onlineIdProUuid);
        Map<Integer, List<String>> ergebnis = new LinkedHashMap<>();
        PtmOnlineSpielrundeSync.spielerProTeam(meldeliste).forEach((teamNr, spielerListe) -> {
            List<String> ids = new ArrayList<>();
            for (MeldelisteSpielerDaten spieler : spielerListe) {
                String name = OnlineSpielerName.schluessel(spieler.vorname(), spieler.nachname());
                String onlineId = onlineIdProName.get(name);
                if (onlineId == null) {
                    return;
                }
                ids.add(onlineId);
            }
            if (!ids.isEmpty()) {
                ergebnis.put(teamNr, List.copyOf(ids));
            }
        });
        return ergebnis;
    }

    private static @Nullable String onlineId(Map<String, String> onlineIdProUuid, @Nullable String uuid) {
        return uuid == null ? null : onlineIdProUuid.get(uuid);
    }

    /** Eindeutige Namen der übernommenen Mêlée-Anmeldungen → Online-ID. */
    private static Map<String, String> onlineIdProName(MeleeAnmeldungZiel melee, Map<String, String> onlineIdProUuid)
            throws GenerateException {
        List<MeleeAnmeldungZeile> zeilen = melee.leseMeleeZeilen().stream().filter(MeleeAnmeldungZeile::uebernommen)
                .toList();
        Map<Integer, String> uuidProZeile = PtmOnlineSpielrundeSync.lokaleUuids(melee,
                zeilen.stream().map(zeile -> zeile.zeile() + 1).toList());
        Map<String, String> ergebnis = new HashMap<>();
        Set<String> mehrdeutig = new HashSet<>();
        for (MeleeAnmeldungZeile zeile : zeilen) {
            String onlineId = onlineId(onlineIdProUuid, uuidProZeile.get(zeile.zeile() + 1));
            if (onlineId == null) {
                continue;
            }
            String name = OnlineSpielerName.schluessel(zeile.vorname(), zeile.nachname());
            if (ergebnis.putIfAbsent(name, onlineId) != null) {
                mehrdeutig.add(name);
            }
        }
        mehrdeutig.forEach(ergebnis::remove);
        return ergebnis;
    }
}
