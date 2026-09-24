/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.basesheet.meldeliste.MeleeAnmeldungZeile;
import de.petanqueturniermanager.spielerdb.MeldelisteSpielerDaten;

class MeleeTeilnahmeTest {

    private static final Map<Integer, List<MeldelisteSpielerDaten>> TEAMS = Map.of(
            1, List.of(spieler("Hans", "Müller", 3), spieler("Anna", "Schmidt", 3)),
            2, List.of(spieler("Jean-Paul", "Dupont", 4), spieler("Eva", "Klein", 4)));

    @Test
    void uebernommeneSpielerErhaltenTeilnahmeIhresTeams() {
        List<LokaleOnlineMeldung> meldungen = MeleeTeilnahme.ermittle(
                List.of(uebernommen(1, "Hans", "Müller"), uebernommen(2, "Anna", "Schmidt"),
                        uebernommen(3, "Jean Paul", "Dupont"), uebernommen(4, "Eva", "Klein")),
                TEAMS, Set.of(1), Set.of(2));

        assertThat(meldungen).extracting(LokaleOnlineMeldung::teilnahme).containsExactly(
                OnlineTeilnahme.AKTIV, OnlineTeilnahme.AKTIV, OnlineTeilnahme.AUSGESETZT, OnlineTeilnahme.AUSGESETZT);
        assertThat(meldungen).extracting(LokaleOnlineMeldung::zeile1Basiert).containsExactly(2, 3, 4, 5);
    }

    @Test
    void nichtUebernommeneSpielerSindInaktiv() {
        MeleeAnmeldungZeile offen = new MeleeAnmeldungZeile(1, 1, "Hans", "Müller", 0, true, false);

        List<LokaleOnlineMeldung> meldungen = MeleeTeilnahme.ermittle(List.of(offen), TEAMS, Set.of(1), Set.of());

        assertThat(meldungen).extracting(LokaleOnlineMeldung::teilnahme).containsExactly(OnlineTeilnahme.INAKTIV);
    }

    @Test
    void spielerOhneTeamInDerMeldelisteIstInaktiv() {
        List<LokaleOnlineMeldung> meldungen = MeleeTeilnahme.ermittle(List.of(uebernommen(1, "Otto", "Normal")),
                TEAMS, Set.of(1, 2), Set.of());

        assertThat(meldungen).extracting(LokaleOnlineMeldung::teilnahme).containsExactly(OnlineTeilnahme.INAKTIV);
    }

    @Test
    void mehrdeutigerNameWirdNichtGeraten() {
        Map<Integer, List<MeldelisteSpielerDaten>> teams = Map.of(
                1, List.of(spieler("Hans", "Müller", 3)),
                2, List.of(spieler("Hans", "Muller", 4)));

        List<LokaleOnlineMeldung> meldungen = MeleeTeilnahme.ermittle(List.of(uebernommen(1, "Hans", "Müller")),
                teams, Set.of(1, 2), Set.of());

        assertThat(meldungen).extracting(LokaleOnlineMeldung::teilnahme).containsExactly(OnlineTeilnahme.INAKTIV);
    }

    @Test
    void setzpositionDerMeleeZeileWirdGemeldet() {
        MeleeAnmeldungZeile gesetzt = new MeleeAnmeldungZeile(1, 1, "Hans", "Müller", 2, true, true);

        List<LokaleOnlineMeldung> meldungen = MeleeTeilnahme.ermittle(
                List.of(gesetzt, uebernommen(2, "Anna", "Schmidt")), TEAMS, Set.of(1), Set.of());

        assertThat(meldungen).extracting(LokaleOnlineMeldung::seedingPosition).containsExactly(2, null);
    }

    private static MeleeAnmeldungZeile uebernommen(int zeile, String vorname, String nachname) {
        return new MeleeAnmeldungZeile(zeile, zeile, vorname, nachname, 0, true, true);
    }

    private static MeldelisteSpielerDaten spieler(String vorname, String nachname, int zeile1Basiert) {
        return new MeldelisteSpielerDaten(vorname, nachname, null, zeile1Basiert);
    }
}
