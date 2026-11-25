/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.onlinesync.speicher;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

class PtmOnlineZuordnungsTabelleTest {

    private static NeueZuordnung neu(String uuid, int revision) {
        return new NeueZuordnung(uuid, "online-" + uuid, revision, "Name " + uuid, "Bestätigt", "Tarif", "Frage",
                "confirmed", "Besetzung " + uuid, "", "Team " + uuid);
    }

    private static PtmOnlineZuordnungsTabelle tabelle(String... uuids) {
        return PtmOnlineZuordnungsTabelle.LEER.mitNeuen(List.of(uuids).stream().map(uuid -> neu(uuid, 2)).toList());
    }

    @Test
    void neueZuordnungenWerdenAngehaengtBestehendeBleiben() {
        PtmOnlineZuordnungsTabelle tabelle = tabelle("a", "b").mitNeuen(List.of(neu("b", 9), neu("c", 0)));

        assertThat(tabelle.uuids()).containsExactly("a", "b", "c");
        assertThat(tabelle.zuordnung("b").map(PtmOnlineZuordnung::revision)).as("bestehende bleibt").contains(2);
        assertThat(tabelle.zuordnung("c").map(PtmOnlineZuordnung::revision)).as("mindestens 1").contains(1);
    }

    @Test
    void anzeigeOhneDetailsLaesstTarifeFragenUndRohStatusStehen() {
        PtmOnlineZuordnungsTabelle tabelle = tabelle("a", "b").mitAnzeigen(Map.of(
                "a", new ZuordnungsAnzeige("Neu A", "Storniert", Optional.of(
                        new ZuordnungsAnzeige.OnlineDetails("T2", "F2", "cancelled"))),
                "b", new ZuordnungsAnzeige("Neu B", "", Optional.empty())));

        PtmOnlineZuordnung a = tabelle.zuordnung("a").orElseThrow();
        assertThat(List.of(a.lokaleBezeichnung(), a.onlineStatus(), a.tarife(), a.fragen(), a.rohStatus()))
                .containsExactly("Neu A", "Storniert", "T2", "F2", "cancelled");
        PtmOnlineZuordnung b = tabelle.zuordnung("b").orElseThrow();
        assertThat(List.of(b.lokaleBezeichnung(), b.onlineStatus(), b.tarife(), b.fragen(), b.rohStatus()))
                .containsExactly("Neu B", "", "Tarif", "Frage", "confirmed");
    }

    @Test
    void zusatzMitNullLaesstDenBisherigenWertStehen() {
        PtmOnlineZuordnungsTabelle tabelle = tabelle("a").mitZusaetzen(
                Map.of("a", ZuordnungsZusatz.nurVermerk("LOKAL_ENTFERNT")));

        assertThat(tabelle.zuordnung("a").map(PtmOnlineZuordnung::zusatz))
                .contains(new ZuordnungsZusatz("Besetzung a", "LOKAL_ENTFERNT", "Team a"));
    }

    @Test
    void revisionenUndBezeichnungTreffenNurGenannteZuordnungen() {
        PtmOnlineZuordnungsTabelle tabelle = tabelle("a", "b").mitRevisionen(Map.of("b", 5))
                .mitBezeichnung("a", null, "Bestätigt").mitBezeichnung("unbekannt", "X", "Y");

        assertThat(tabelle.proUuid(PtmOnlineZuordnung::revision)).containsExactly(Map.entry("a", 2), Map.entry("b", 5));
        assertThat(tabelle.zuordnung("a").map(PtmOnlineZuordnung::lokaleBezeichnung)).contains("");
        assertThat(tabelle.anzahl()).isEqualTo(2);
    }

    @Test
    void entfernenBehaeltDieReihenfolgeDerUebrigen() {
        PtmOnlineZuordnungsTabelle tabelle = tabelle("a", "b", "c", "d").ohne(Set.of("b", "x"));

        assertThat(tabelle.uuids()).containsExactly("a", "c", "d");
    }

    @Test
    void unveraenderteTabelleIstGleich() {
        PtmOnlineZuordnungsTabelle tabelle = tabelle("a", "b");

        assertThat(tabelle.mitRevisionen(Map.of())).isEqualTo(tabelle);
        assertThat(tabelle.mitZusaetzen(Map.of("x", ZuordnungsZusatz.nurVermerk("v")))).isEqualTo(tabelle);
    }
}
