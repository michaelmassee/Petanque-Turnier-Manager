/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

/**
 * Check-in-Push am Turniertag: nur Änderungen werden gemeldet, ohne bekannten Stand nur aktive Meldungen, und jeder
 * Stand gilt je Online-Turnier (Supermelee: je Spieltag). Eine geänderte Teamnummer zählt wie eine geänderte
 * Teilnahme. Der manuelle Abgleich vergleicht dagegen mit dem Online-Stand und findet Abweichungen in beide Richtungen.
 */
class PtmOnlineCheckinTest {

    private static final String TURNIER = "t1";
    private static final String SPIELTAG_2 = "t2";

    private final PtmOnlineCheckin checkin = new PtmOnlineCheckin();

    @Test
    void ohneBekanntenStandGehenNurAktiveHinaus() {
        Optional<PtmOnlineCheckin.Aenderung> aenderung = checkin.ermittle(TURNIER,
                List.of(eintrag("a", OnlineTeilnahme.AKTIV), eintrag("b", OnlineTeilnahme.INAKTIV),
                        eintrag("c", OnlineTeilnahme.AUSGESETZT)));

        assertThat(aenderung).isPresent();
        assertThat(uuids(aenderung.get())).containsExactly("a");
        assertThat(aenderung.get().auftrag().tournamentId()).isEqualTo(TURNIER);
    }

    @Test
    void ohneAktiveGibtEsNichtsZuMeldenUndDerStandGiltAlsBekannt() {
        assertThat(checkin.ermittle(TURNIER, List.of(eintrag("a", OnlineTeilnahme.INAKTIV)))).isEmpty();

        Optional<PtmOnlineCheckin.Aenderung> eingecheckt = checkin.ermittle(TURNIER,
                List.of(eintrag("a", OnlineTeilnahme.AKTIV)));

        assertThat(eingecheckt.map(PtmOnlineCheckinTest::uuids)).contains(List.of("a"));
    }

    @Test
    void unveraenderterCheckinWirdNurEinmalGemeldet() {
        bestaetige(checkin.ermittle(TURNIER, List.of(eintrag("a", OnlineTeilnahme.AKTIV))));

        assertThat(checkin.ermittle(TURNIER, List.of(eintrag("a", OnlineTeilnahme.AKTIV)))).isEmpty();
    }

    @Test
    void wechselAmTurniertagWirdJeweilsGemeldet() {
        bestaetige(checkin.ermittle(TURNIER, List.of(eintrag("a", OnlineTeilnahme.AKTIV))));

        Optional<PtmOnlineCheckin.Aenderung> ausgesetzt = checkin.ermittle(TURNIER,
                List.of(eintrag("a", OnlineTeilnahme.AUSGESETZT)));
        assertThat(ausgesetzt.map(a -> a.auftrag().eintraege().get(0).teilnahme()))
                .contains(OnlineTeilnahme.AUSGESETZT);
        bestaetige(ausgesetzt);

        Optional<PtmOnlineCheckin.Aenderung> wiederAktiv = checkin.ermittle(TURNIER,
                List.of(eintrag("a", OnlineTeilnahme.AKTIV)));
        assertThat(wiederAktiv.map(a -> a.auftrag().eintraege().get(0).teilnahme())).contains(OnlineTeilnahme.AKTIV);
    }

    @Test
    void nichtBestaetigteAenderungWirdErneutErmittelt() {
        checkin.ermittle(TURNIER, List.of(eintrag("a", OnlineTeilnahme.AKTIV)));

        assertThat(checkin.ermittle(TURNIER, List.of(eintrag("a", OnlineTeilnahme.AKTIV)))).isPresent();
    }

    @Test
    void neuZugeordneteMeldungGehtNurAlsAktivHinaus() {
        bestaetige(checkin.ermittle(TURNIER, List.of(eintrag("a", OnlineTeilnahme.AKTIV))));

        assertThat(checkin.ermittle(TURNIER, List.of(eintrag("a", OnlineTeilnahme.AKTIV),
                eintrag("neu", OnlineTeilnahme.INAKTIV)))).isEmpty();
    }

    @Test
    void neuerSpieltagIstEinEigenesOnlineTurnier() {
        bestaetige(checkin.ermittle(TURNIER, List.of(eintrag("a", OnlineTeilnahme.AKTIV))));

        assertThat(checkin.ermittle(SPIELTAG_2, List.of(eintrag("a", OnlineTeilnahme.AKTIV)))
                .map(PtmOnlineCheckinTest::uuids)).contains(List.of("a"));
    }

    @Test
    void rundenstartAbgleichGiltAlsGemeldet() {
        checkin.uebernehmen(new PtmOnlineStatusAuftrag(TURNIER,
                List.of(eintrag("a", OnlineTeilnahme.AKTIV), eintrag("b", OnlineTeilnahme.INAKTIV))));

        assertThat(checkin.ermittle(TURNIER, List.of(eintrag("a", OnlineTeilnahme.AKTIV),
                eintrag("b", OnlineTeilnahme.INAKTIV)))).isEmpty();
    }

    /** Regression: Check-in am Turniertag gemeldet, danach Runde 1 – der Rundenstart warf beim Übernehmen. */
    @Test
    void rundenstartNachBestaetigtemCheckinErgaenztDenBekanntenStand() {
        bestaetige(checkin.ermittle(TURNIER, List.of(eintrag("a", OnlineTeilnahme.AKTIV))));

        checkin.uebernehmen(new PtmOnlineStatusAuftrag(TURNIER,
                List.of(eintrag("a", OnlineTeilnahme.AKTIV), eintrag("b", OnlineTeilnahme.AKTIV))));

        assertThat(checkin.ermittle(TURNIER, List.of(eintrag("a", OnlineTeilnahme.AKTIV),
                eintrag("b", OnlineTeilnahme.AKTIV)))).isEmpty();
    }

    /** Regression: Nr. 1 → 42 in der Meldeliste erzeugte keinen Auftrag, die Live-Ansicht zeigte weiter Nr. 1. */
    @Test
    void geaenderteTeamnummerWirdGemeldet() {
        bestaetige(checkin.ermittle(TURNIER, List.of(eintrag("a", OnlineTeilnahme.AKTIV, 1))));

        Optional<PtmOnlineCheckin.Aenderung> umnummeriert = checkin.ermittle(TURNIER,
                List.of(eintrag("a", OnlineTeilnahme.AKTIV, 42)));

        assertThat(umnummeriert.map(a -> a.auftrag().eintraege().get(0).teamNummer())).contains(42);
        bestaetige(umnummeriert);
        assertThat(checkin.ermittle(TURNIER, List.of(eintrag("a", OnlineTeilnahme.AKTIV, 42)))).isEmpty();
    }

    @Test
    void entfernteTeamnummerIstNichtZuMelden() {
        bestaetige(checkin.ermittle(TURNIER, List.of(eintrag("a", OnlineTeilnahme.AKTIV, 1))));

        assertThat(checkin.ermittle(TURNIER, List.of(eintrag("a", OnlineTeilnahme.AKTIV)))).isEmpty();
    }

    @Test
    void rundenstartAbgleichUebernimmtDieTeamnummer() {
        checkin.uebernehmen(new PtmOnlineStatusAuftrag(TURNIER, List.of(eintrag("a", OnlineTeilnahme.AKTIV, 7))));

        assertThat(checkin.ermittle(TURNIER, List.of(eintrag("a", OnlineTeilnahme.AKTIV, 7)))).isEmpty();
        assertThat(checkin.ermittle(TURNIER, List.of(eintrag("a", OnlineTeilnahme.AKTIV, 8)))).isPresent();
    }

    @Test
    void abgleichFindetAbweichungenInBeideRichtungen() {
        List<PtmOnlineStatusAuftrag.Eintrag> lokal = List.of(eintrag("leer", OnlineTeilnahme.INAKTIV),
                eintrag("aktiv", OnlineTeilnahme.AKTIV), eintrag("gleich", OnlineTeilnahme.AKTIV));

        List<PtmOnlineStatusAuftrag.Eintrag> abweichend = PtmOnlineCheckin.abweichendVonOnline(lokal,
                Map.of("leer", "o1", "aktiv", "o2", "gleich", "o3"),
                Map.of("o1", online("active"), "o2", online("inactive"), "o3", online("active")));

        assertThat(abweichend).extracting(PtmOnlineStatusAuftrag.Eintrag::lokaleUuid).containsExactly("leer", "aktiv");
    }

    @Test
    void abgleichLaesstMeldungenOhneBekannteOnlineTeilnahmeUnberuehrt() {
        List<PtmOnlineStatusAuftrag.Eintrag> lokal = List.of(eintrag("ohneZuordnung", OnlineTeilnahme.AKTIV),
                eintrag("nichtBestaetigt", OnlineTeilnahme.INAKTIV));

        assertThat(PtmOnlineCheckin.abweichendVonOnline(lokal, Map.of("nichtBestaetigt", "o1"), Map.of())).isEmpty();
    }

    @Test
    void abgleichFindetAbweichendeTeamnummer() {
        List<PtmOnlineStatusAuftrag.Eintrag> lokal = List.of(eintrag("umnummeriert", OnlineTeilnahme.AKTIV, 42),
                eintrag("onlineOhneNr", OnlineTeilnahme.AKTIV, 2), eintrag("gleich", OnlineTeilnahme.AKTIV, 3),
                eintrag("lokalOhneNr", OnlineTeilnahme.AKTIV));

        List<PtmOnlineStatusAuftrag.Eintrag> abweichend = PtmOnlineCheckin.abweichendVonOnline(lokal,
                Map.of("umnummeriert", "o1", "onlineOhneNr", "o2", "gleich", "o3", "lokalOhneNr", "o4"),
                Map.of("o1", new OnlineMeldungsStand("active", 1), "o2", online("active"),
                        "o3", new OnlineMeldungsStand("active", 3), "o4", new OnlineMeldungsStand("active", 4)));

        assertThat(abweichend).extracting(PtmOnlineStatusAuftrag.Eintrag::lokaleUuid)
                .containsExactly("umnummeriert", "onlineOhneNr");
    }

    private void bestaetige(Optional<PtmOnlineCheckin.Aenderung> aenderung) {
        assertThat(aenderung).isPresent();
        checkin.bestaetigen(aenderung.get());
    }

    private static PtmOnlineStatusAuftrag.Eintrag eintrag(String uuid, OnlineTeilnahme teilnahme) {
        return eintrag(uuid, teilnahme, null);
    }

    private static PtmOnlineStatusAuftrag.Eintrag eintrag(String uuid, OnlineTeilnahme teilnahme,
            @Nullable Integer teamNummer) {
        return new PtmOnlineStatusAuftrag.Eintrag(uuid, teilnahme, null, teamNummer);
    }

    private static OnlineMeldungsStand online(String teilnahme) {
        return new OnlineMeldungsStand(teilnahme, null);
    }

    private static List<String> uuids(PtmOnlineCheckin.Aenderung aenderung) {
        return aenderung.auftrag().eintraege().stream().map(PtmOnlineStatusAuftrag.Eintrag::lokaleUuid).toList();
    }
}
