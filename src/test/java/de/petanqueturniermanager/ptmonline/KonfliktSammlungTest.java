/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.helper.i18n.I18n;

class KonfliktSammlungTest {

    private static final List<Entscheidung> NAMENS_OPTIONEN = List.of(Entscheidung.ONLINE_UEBERNEHMEN,
            Entscheidung.LOKAL_BEHALTEN);

    @BeforeAll
    static void uebersetzungen() {
        I18n.init(null);
    }

    private static KonfliktFall namensKonflikt(String lokal, String online) {
        return new KonfliktFall(KonfliktArt.NAMENSKONFLIKT, "u1", List.of("r1"), lokal, online, "", NAMENS_OPTIONEN);
    }

    @Test
    void schluesselIstReihenfolgeunabhaengigUndSprachneutral() {
        KonfliktFall fall = new KonfliktFall(KonfliktArt.KONTO_KONFLIKT, null, List.of("r2", "r1"), "", "A / B", "x",
                List.of());

        assertThat(fall.schluessel()).isEqualTo("KONTO_KONFLIKT||r1,r2");
    }

    @Test
    void namenskonfliktSchluesselAendertSichMitDenStaenden() {
        assertThat(namensKonflikt("Adler", "Berg").schluessel())
                .isNotEqualTo(namensKonflikt("Adler", "Cramer").schluessel())
                .startsWith("NAMENSKONFLIKT|u1|r1|");
    }

    @Test
    void entscheidungWirdUeberAnzeigetextOderNamenErkanntAberNurAusDenOptionen() {
        KonfliktFall fall = namensKonflikt("Adler", "Berg");

        assertThat(KonfliktSammlung.mit(Map.of(fall.schluessel(), Entscheidung.LOKAL_BEHALTEN.anzeige()))
                .entscheidung(fall)).contains(Entscheidung.LOKAL_BEHALTEN);
        assertThat(KonfliktSammlung.mit(Map.of(fall.schluessel(), " online_uebernehmen ")).entscheidung(fall))
                .contains(Entscheidung.ONLINE_UEBERNEHMEN);
        assertThat(KonfliktSammlung.mit(Map.of(fall.schluessel(), Entscheidung.BEHALTEN.name())).entscheidung(fall))
                .as("passt nicht zum Fall").isEmpty();
    }

    @Test
    void offenerFallBehaeltSeineWahlInDerNeuenListe() {
        KonfliktFall fall = namensKonflikt("Adler", "Berg");
        KonfliktSammlung sammlung = KonfliktSammlung.mit(Map.of(fall.schluessel(), "LOKAL_BEHALTEN"));
        sammlung.melde(fall);
        sammlung.melde(new KonfliktFall(KonfliktArt.MOEGLICH_IDENTISCH, "u2", List.of("r2"), "Dora", "Dora", "",
                List.of(Entscheidung.VERKNUEPFEN, Entscheidung.GETRENNT)));

        assertThat(sammlung.zeilen()).as("nach Art sortiert").hasSize(2).satisfies(zeilen -> {
            assertThat(zeilen.get(0).schluessel()).startsWith("MOEGLICH_IDENTISCH");
            assertThat(zeilen.get(0).entscheidung()).isEmpty();
            assertThat(zeilen.get(1).entscheidung()).isEqualTo(Entscheidung.LOKAL_BEHALTEN.anzeige());
            assertThat(zeilen.get(1).optionen()).containsExactly(Entscheidung.ONLINE_UEBERNEHMEN.anzeige(),
                    Entscheidung.LOKAL_BEHALTEN.anzeige());
        });
        assertThat(sammlung.ausschlussgruende()).extracting(KonfliktFall::art)
                .containsExactly(KonfliktArt.MOEGLICH_IDENTISCH);
    }

    @Test
    void protokollTraegtNurEntscheidungenMitServerCode() {
        String body = TournamentSyncClient.entscheidungenBody(List.of(
                new KonfliktSammlung.Protokoll(Entscheidung.VERKNUEPFEN, "u1", "r1"),
                new KonfliktSammlung.Protokoll(Entscheidung.ONLINE_UEBERNEHMEN, "u2", "r2"),
                new KonfliktSammlung.Protokoll(Entscheidung.TROTZDEM_STARTEN, null, null)));

        assertThat(body).isEqualTo("{\"decisions\":[{\"decision\":\"link\",\"onlineRegistrationId\":\"r1\","
                + "\"localRegistrationUuid\":\"u1\"},{\"decision\":\"start_despite_findings\"}]}");
    }

    @Test
    void ohneBefundeFragtDerVorabcheckNurNachDemStart() {
        assertThat(PtmOnlineSpielrundeSync.vorabcheckText(List.of(), List.of()))
                .isEqualTo(I18n.get("ptmonline.frage.turnierstart"));
        assertThat(PtmOnlineSpielrundeSync.vorabcheckText(List.of("Anna"), List.of("Konto: A / B")))
                .contains("Anna").contains("Konto: A / B");
    }
}
