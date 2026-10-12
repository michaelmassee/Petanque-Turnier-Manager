/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.basesheet.meldeliste.MeleeAnmeldungZeile;
import de.petanqueturniermanager.ptmonline.dto.MeleeTeamDto;
import de.petanqueturniermanager.spielerdb.MeldelisteSpielerDaten;

class MeleeTeilnahmeTest {

    private static final String TEAM_UUID = "aaaaaaaa-0000-4000-8000-000000000001";

    private static final Map<Integer, List<MeldelisteSpielerDaten>> TEAMS = Map.of(
            1, List.of(spieler("Hans", "Müller", 3), spieler("Anna", "Schmidt", 3)),
            2, List.of(spieler("Jean-Paul", "Dupont", 4), spieler("Eva", "Klein", 4)));

    @Test
    void uebernommeneSpielerErhaltenTeilnahmeIhresTeams() {
        List<LokaleOnlineMeldung> meldungen = MeleeTeilnahme.ermittle(
                List.of(uebernommen(1, "Hans", "Müller", 1), uebernommen(2, "Anna", "Schmidt", 1),
                        uebernommen(3, "Jean Paul", "Dupont", 2), uebernommen(4, "Eva", "Klein", 2)),
                Set.of(1), Set.of(2));

        assertThat(meldungen).extracting(LokaleOnlineMeldung::teilnahme).containsExactly(
                OnlineTeilnahme.AKTIV, OnlineTeilnahme.AKTIV, OnlineTeilnahme.AUSGESETZT, OnlineTeilnahme.AUSGESETZT);
        assertThat(meldungen).extracting(LokaleOnlineMeldung::zeile1Basiert).containsExactly(2, 3, 4, 5);
    }

    @Test
    void nichtUebernommeneSpielerSindInaktiv() {
        MeleeAnmeldungZeile offen = new MeleeAnmeldungZeile(1, 1, "Hans", "Müller", 0, true, false);

        List<LokaleOnlineMeldung> meldungen = MeleeTeilnahme.ermittle(List.of(offen), Set.of(1), Set.of());

        assertThat(meldungen).extracting(LokaleOnlineMeldung::teilnahme).containsExactly(OnlineTeilnahme.INAKTIV);
    }

    @Test
    void markiertOhneTeamVerweisIstInaktivStattGeraten() {
        // „Übernommen“ von Hand markiert: der Name „Hans Müller“ steht zwar in Team 1, zugeordnet wird aber nicht.
        MeleeAnmeldungZeile ohneVerweis = new MeleeAnmeldungZeile(1, 1, "Hans", "Müller", 0, true, true);

        List<LokaleOnlineMeldung> meldungen = MeleeTeilnahme.ermittle(List.of(ohneVerweis), Set.of(1, 2), Set.of());

        assertThat(meldungen).extracting(LokaleOnlineMeldung::teilnahme).containsExactly(OnlineTeilnahme.INAKTIV);
        assertThat(MeleeTeilnahme.zeilenProTeam(List.of(ohneVerweis), TEAMS)).isEmpty();
    }

    @Test
    void setzpositionDerMeleeZeileWirdGemeldet() {
        MeleeAnmeldungZeile gesetzt = new MeleeAnmeldungZeile(1, 1, "Hans", "Müller", 2, true, true, 1);

        List<LokaleOnlineMeldung> meldungen = MeleeTeilnahme.ermittle(
                List.of(gesetzt, uebernommen(2, "Anna", "Schmidt", 1)), Set.of(1), Set.of());

        assertThat(meldungen).extracting(LokaleOnlineMeldung::seedingPosition).containsExactly(2, null);
    }

    @Test
    void zeilenProTeamFolgenDerMeldelisteUndLassenNichtUebernommeneWeg() {
        Map<Integer, List<MeldelisteSpielerDaten>> teams = new LinkedHashMap<>();
        teams.put(2, List.of(spieler("Eva", "Klein", 5), spieler("Jean-Paul", "Dupont", 4)));
        teams.put(1, List.of(spieler("Hans", "Müller", 3), spieler("Otto", "Normal", 6)));
        MeleeAnmeldungZeile offen = new MeleeAnmeldungZeile(5, 5, "Anna", "Schmidt", 0, true, false);

        Map<Integer, List<Integer>> zeilen = MeleeTeilnahme.zeilenProTeam(List.of(uebernommen(1, "Hans", "Müller", 1),
                uebernommen(3, "Jean Paul", "Dupont", 2), uebernommen(4, "Eva", "Klein", 2), offen), teams);

        assertThat(zeilen).containsExactly(Map.entry(1, List.of(2)), Map.entry(2, List.of(5, 4)));
    }

    @Test
    void meleeTeamsTragenNurZugeordneteAnmeldungenUndDieTeamUuidDerMeldeliste() {
        Map<Integer, List<Integer>> zeilenProTeam = new TreeMap<>(Map.of(1, List.of(2, 3), 2, List.of(4)));

        List<MeleeTeamDto> teams = PtmOnlineSpielrundeSync.meleeTeams("t1", Map.of(1, TEAM_UUID), zeilenProTeam,
                Map.of(2, "u2", 3, "u3", 4, "u4"), Map.of("u2", "r2", "u3", "r3"));

        assertThat(teams).singleElement().satisfies(team -> {
            assertThat(team.registrationIds()).containsExactly("r2", "r3");
            assertThat(team.teamUuid()).as("Team-UUID der Meldeliste, unabhängig von der Nummer").isEqualTo(TEAM_UUID);
        });
    }

    @Test
    void meleeTeamOhneTeamUuidFaelltAufAbgeleiteteUuidZurueck() {
        List<MeleeTeamDto> teams = PtmOnlineSpielrundeSync.meleeTeams("t1", Map.of(),
                Map.of(1, List.of(2)), Map.of(2, "u2"), Map.of("u2", "r2"));

        assertThat(teams).singleElement().satisfies(team -> assertThat(team.teamUuid())
                .isEqualTo(PtmOnlineSpielrundeSync.meleeTeamUuid("t1", 1))
                .matches("[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}"));
        assertThat(PtmOnlineSpielrundeSync.meleeTeamUuid("t1", 1))
                .isNotEqualTo(PtmOnlineSpielrundeSync.meleeTeamUuid("t1", 2))
                .isNotEqualTo(PtmOnlineSpielrundeSync.meleeTeamUuid("t2", 1));
    }

    @Test
    void teamVerweisInUebernommenGiltVorDemNamen() {
        // Name in der Meldeliste korrigiert („Hans“ → „Johannes“): der Verweis auf Team 2 trägt trotzdem.
        MeleeAnmeldungZeile mitVerweis = new MeleeAnmeldungZeile(1, 1, "Hans", "Müller", 0, true, true, 2);

        List<LokaleOnlineMeldung> meldungen = MeleeTeilnahme.ermittle(List.of(mitVerweis), Set.of(2), Set.of());
        Map<Integer, List<Integer>> zeilen = MeleeTeilnahme.zeilenProTeam(List.of(mitVerweis), TEAMS);

        assertThat(meldungen).extracting(LokaleOnlineMeldung::teilnahme).containsExactly(OnlineTeilnahme.AKTIV);
        assertThat(zeilen).containsExactly(Map.entry(2, List.of(2)));
    }

    @Test
    void teamVerweisAufUnbekanntesTeamWirdIgnoriert() {
        MeleeAnmeldungZeile verwaist = new MeleeAnmeldungZeile(1, 1, "Hans", "Müller", 0, true, true, 9);

        assertThat(MeleeTeilnahme.zeilenProTeam(List.of(verwaist), TEAMS)).isEmpty();
    }

    /** Übernommene Mêlée-Zeile mit Verweis auf das Team {@code teamNr} der Meldeliste. */
    private static MeleeAnmeldungZeile uebernommen(int zeile, String vorname, String nachname, int teamNr) {
        return new MeleeAnmeldungZeile(zeile, zeile, vorname, nachname, 0, true, true, teamNr);
    }

    private static MeldelisteSpielerDaten spieler(String vorname, String nachname, int zeile1Basiert) {
        return new MeldelisteSpielerDaten(vorname, nachname, null, zeile1Basiert);
    }
}
