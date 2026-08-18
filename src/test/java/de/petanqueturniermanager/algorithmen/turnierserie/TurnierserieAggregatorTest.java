package de.petanqueturniermanager.algorithmen.turnierserie;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.algorithmen.turnierserie.ergebnis.TeamEndranglisteErgebnis;
import de.petanqueturniermanager.algorithmen.turnierserie.ergebnis.TeamSpieltagErgebnis;
import de.petanqueturniermanager.supermelee.SpielTagNr;

public class TurnierserieAggregatorTest {

    private static Map<Integer, Map<Integer, TeamSpieltagErgebnis>> cacheMit(TeamSpieltagErgebnis... ergebnisse) {
        Map<Integer, Map<Integer, TeamSpieltagErgebnis>> cache = new HashMap<>();
        for (TeamSpieltagErgebnis erg : ergebnisse) {
            cache.computeIfAbsent(erg.getSpielTagNr(), k -> new HashMap<>()).put(erg.getTeamNr(), erg);
        }
        return cache;
    }

    @Test
    public void testAggregationUeberZweiSpieltage() throws Exception {
        TeamSpieltagErgebnis tag1 = new TeamSpieltagErgebnis(SpielTagNr.from(1), 1)
                .setSpielPlus(4).setSpielMinus(2).setPunktePlus(30).setPunkteMinus(20);
        TeamSpieltagErgebnis tag2 = new TeamSpieltagErgebnis(SpielTagNr.from(2), 1)
                .setSpielPlus(3).setSpielMinus(3).setPunktePlus(25).setPunkteMinus(25);

        Map<Integer, TeamEndranglisteErgebnis> endrangliste = TurnierserieAggregator
                .berechneEndrangliste(cacheMit(tag1, tag2), 2, false);

        TeamEndranglisteErgebnis team1 = endrangliste.get(1);
        assertThat(team1.getAnzGespielteSpieltage()).isEqualTo(2);
        // schlechterer Spieltag (tag2) wird gestrichen -> nur tag1 zaehlt
        assertThat(team1.getStreichSpieltag().getNr()).isEqualTo(2);
        assertThat(team1.getSpielPlus()).isEqualTo(4);
        assertThat(team1.getSpielMinus()).isEqualTo(2);
        assertThat(team1.getPunktePlus()).isEqualTo(30);
        assertThat(team1.getPunkteMinus()).isEqualTo(20);
    }

    @Test
    public void testTeamFehltAnEinemSpieltagWirdAlsNichtTeilgenommenGefuehrt() throws Exception {
        TeamSpieltagErgebnis tag1 = new TeamSpieltagErgebnis(SpielTagNr.from(1), 7)
                .setSpielPlus(5).setSpielMinus(1).setPunktePlus(40).setPunkteMinus(10);
        // Team 7 fehlt an Spieltag 2 komplett

        Map<Integer, TeamEndranglisteErgebnis> endrangliste = TurnierserieAggregator
                .berechneEndrangliste(cacheMit(tag1), 2, false);

        TeamEndranglisteErgebnis team7 = endrangliste.get(7);
        assertThat(team7.getAnzGespielteSpieltage()).isEqualTo(1);
        assertThat(team7.isValid()).isTrue();
        // Der fehlende Spieltag ist automatisch der "Streich" (Nuller-Ergebnis sortiert immer
        // schlechtester) - wirkt sich aber nicht auf die Summe aus, da nicht teilgenommen ohnehin
        // ausgeklammert wird.
        assertThat(team7.getStreichSpieltag().getNr()).isEqualTo(2);
        assertThat(team7.getSpielPlus()).isEqualTo(5);
    }

    @Test
    public void testEinzelnerSpieltagKeinStreich() throws Exception {
        TeamSpieltagErgebnis tag1 = new TeamSpieltagErgebnis(SpielTagNr.from(1), 1)
                .setSpielPlus(6).setSpielMinus(0).setPunktePlus(39).setPunkteMinus(10);

        Map<Integer, TeamEndranglisteErgebnis> endrangliste = TurnierserieAggregator
                .berechneEndrangliste(cacheMit(tag1), 1, false);

        assertThat(endrangliste.get(1).getStreichSpieltag()).isNull();
        assertThat(endrangliste.get(1).getSpielPlus()).isEqualTo(6);
    }

    @Test
    public void testFreilosWirdVonStreichAusgeschlossen() throws Exception {
        // Spieltag 1: schlechtestes Ergebnis, aber Freilos -> darf nicht gestrichen werden
        TeamSpieltagErgebnis freilosTag = new TeamSpieltagErgebnis(SpielTagNr.from(1), 3)
                .setSpielPlus(1).setSpielMinus(0).setPunktePlus(13).setPunkteMinus(0).setFreilos(true);
        // Spieltag 2: schlechter als Spieltag 3, aber kein Freilos -> wird gestrichen
        TeamSpieltagErgebnis schlechtesterEchterTag = new TeamSpieltagErgebnis(SpielTagNr.from(2), 3)
                .setSpielPlus(1).setSpielMinus(5).setPunktePlus(10).setPunkteMinus(30);
        TeamSpieltagErgebnis besterTag = new TeamSpieltagErgebnis(SpielTagNr.from(3), 3)
                .setSpielPlus(5).setSpielMinus(1).setPunktePlus(35).setPunkteMinus(15);

        Map<Integer, TeamEndranglisteErgebnis> endrangliste = TurnierserieAggregator
                .berechneEndrangliste(cacheMit(freilosTag, schlechtesterEchterTag, besterTag), 3, true);

        TeamEndranglisteErgebnis team3 = endrangliste.get(3);
        assertThat(team3.getStreichSpieltag().getNr()).isEqualTo(2);
        // Freilos- und Bester-Tag zaehlen, Spieltag 2 (Streich) nicht
        assertThat(team3.getSpielPlus()).isEqualTo(1 + 5);
        assertThat(team3.getPunktePlus()).isEqualTo(13 + 35);
    }

    @Test
    public void testAusschliesslichFreilosSpieltageKeinStreichMoeglich() throws Exception {
        TeamSpieltagErgebnis freilos1 = new TeamSpieltagErgebnis(SpielTagNr.from(1), 4)
                .setSpielPlus(1).setSpielMinus(0).setPunktePlus(13).setPunkteMinus(0).setFreilos(true);
        TeamSpieltagErgebnis freilos2 = new TeamSpieltagErgebnis(SpielTagNr.from(2), 4)
                .setSpielPlus(1).setSpielMinus(0).setPunktePlus(13).setPunkteMinus(0).setFreilos(true);

        List<TeamSpieltagErgebnis> ergebnisse = List.of(freilos1, freilos2);
        assertThat(TurnierserieAggregator.ermittleStreichSpieltag(ergebnisse, true)).isNull();
    }

    @Test
    public void testMehrereTeamsUnabhaengigAggregiert() throws Exception {
        TeamSpieltagErgebnis team1Tag1 = new TeamSpieltagErgebnis(SpielTagNr.from(1), 1)
                .setSpielPlus(6).setSpielMinus(0).setPunktePlus(39).setPunkteMinus(10);
        TeamSpieltagErgebnis team2Tag1 = new TeamSpieltagErgebnis(SpielTagNr.from(1), 2)
                .setSpielPlus(0).setSpielMinus(6).setPunktePlus(10).setPunkteMinus(39);

        Map<Integer, TeamEndranglisteErgebnis> endrangliste = TurnierserieAggregator
                .berechneEndrangliste(cacheMit(team1Tag1, team2Tag1), 1, false);

        assertThat(endrangliste).hasSize(2);
        assertThat(endrangliste.get(1).getSpielPlus()).isEqualTo(6);
        assertThat(endrangliste.get(2).getSpielPlus()).isEqualTo(0);
    }

}
