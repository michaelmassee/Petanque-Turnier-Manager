/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * Check-in-Push am Turniertag: nur Änderungen werden gemeldet, ohne bekannten Stand nur aktive Meldungen, und jeder
 * Stand gilt je Online-Turnier (Supermelee: je Spieltag).
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

    private void bestaetige(Optional<PtmOnlineCheckin.Aenderung> aenderung) {
        assertThat(aenderung).isPresent();
        checkin.bestaetigen(aenderung.get());
    }

    private static PtmOnlineStatusAuftrag.Eintrag eintrag(String uuid, OnlineTeilnahme teilnahme) {
        return new PtmOnlineStatusAuftrag.Eintrag(uuid, teilnahme, null);
    }

    private static List<String> uuids(PtmOnlineCheckin.Aenderung aenderung) {
        return aenderung.auftrag().eintraege().stream().map(PtmOnlineStatusAuftrag.Eintrag::lokaleUuid).toList();
    }
}
