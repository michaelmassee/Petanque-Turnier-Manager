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
import de.petanqueturniermanager.ptmonline.dto.ErgebnismeldungDto;
import de.petanqueturniermanager.ptmonline.live.ErgebnisOrt;
import de.petanqueturniermanager.ptmonline.live.LivePartie;
import de.petanqueturniermanager.ptmonline.live.LiveRunde;
import de.petanqueturniermanager.ptmonline.live.LiveTurnierStand;

class PtmOnlineErgebnismeldungenTest {

    private static final Map<Integer, List<String>> ONLINE_IDS = Map.of(1, List.of("r1"), 2, List.of("r2"), 3,
            List.of("r3"), 4, List.of("r4"));
    private static final ErgebnisOrt ORT = new ErgebnisOrt("schluessel", "1. Spielrunde", 3, 3);

    @BeforeAll
    static void uebersetzungen() {
        I18n.init(null);
    }

    private static LiveTurnierStand runde1(LivePartie... partien) {
        return LiveTurnierStand.ohneRangliste(List.of(new LiveRunde(1, List.of(partien))));
    }

    private static ErgebnismeldungDto meldung(String id, int runde, String a, String b, int punkteA, int punkteB) {
        return new ErgebnismeldungDto(id, runde, List.of(a), List.of(b), punkteA, punkteB, "Team " + a, "");
    }

    @Test
    void ordnetDieMeldungDerPartieMitDenselbenAnmeldungenZu() {
        LivePartie partie = LivePartie.teams(3, 4, null, null, null, null).mitErgebnisOrt(ORT);
        var zuordnung = PtmOnlineErgebnismeldungen.ordneZu(List.of(meldung("m1", 1, "r3", "r4", 13, 7)),
                runde1(LivePartie.teams(1, 2, null, null, null, null), partie), ONLINE_IDS).getFirst();

        assertThat(zuordnung.partie()).isEqualTo(partie);
        assertThat(zuordnung.ort()).isEqualTo(ORT);
        assertThat(List.of(zuordnung.punkteA(), zuordnung.punkteB())).containsExactly(13, 7);
        assertThat(zuordnung.fall().optionen()).containsExactly(Entscheidung.ERGEBNIS_UEBERNEHMEN,
                Entscheidung.ERGEBNIS_VERWERFEN);
    }

    @Test
    void drehtDiePunkteWennDieSeitenImDokumentVertauschtSind() {
        LivePartie partie = LivePartie.teams(4, 3, null, null, null, null).mitErgebnisOrt(ORT);
        var zuordnung = PtmOnlineErgebnismeldungen.ordneZu(List.of(meldung("m1", 1, "r3", "r4", 13, 7)),
                runde1(partie), ONLINE_IDS).getFirst();

        assertThat(zuordnung.vertauscht()).isTrue();
        assertThat(List.of(zuordnung.punkteA(), zuordnung.punkteB())).containsExactly(7, 13);
    }

    @Test
    void ohneEintragbarePartieBleibtNurVerwerfen() {
        LivePartie ohneOrt = LivePartie.teams(3, 4, null, null, null, null);
        var andereRunde = PtmOnlineErgebnismeldungen.ordneZu(List.of(meldung("m1", 2, "r3", "r4", 13, 7)),
                runde1(ohneOrt.mitErgebnisOrt(ORT)), ONLINE_IDS).getFirst();
        var turnierbaum = PtmOnlineErgebnismeldungen.ordneZu(List.of(meldung("m2", 1, "r3", "r4", 13, 7)),
                runde1(ohneOrt), ONLINE_IDS).getFirst();

        assertThat(andereRunde.partie()).isNull();
        assertThat(andereRunde.fall().optionen()).containsExactly(Entscheidung.ERGEBNIS_VERWERFEN);
        assertThat(turnierbaum.fall().optionen()).containsExactly(Entscheidung.ERGEBNIS_VERWERFEN);
        assertThat(andereRunde.fall().lokal()).contains("3").contains("4");
    }

    @Test
    void nenntEinBereitsEingetragenesErgebnis() {
        LivePartie partie = LivePartie.teams(3, 4, 13, 11, null, null).mitErgebnisOrt(ORT);
        var fall = PtmOnlineErgebnismeldungen.ordneZu(List.of(meldung("m1", 1, "r3", "r4", 13, 7)), runde1(partie),
                ONLINE_IDS).getFirst().fall();

        assertThat(fall.hinweis()).contains("13:11");
        assertThat(fall.schluessel()).isEqualTo("ERGEBNIS_GEMELDET||m1").startsWith(
                PtmOnlineErgebnismeldungen.SCHLUESSEL_PRAEFIX);
    }

    @Test
    void ordnetSupermeleeTeamsUeberAlleSpielerZu() {
        LivePartie partie = new LivePartie(List.of(1, 2), List.of(3, 4), null, null, null, null).mitErgebnisOrt(ORT);
        var meldung = new ErgebnismeldungDto("m1", 1, List.of("r4", "r3"), List.of("r2", "r1"), 13, 2, "x", "");

        var zuordnung = PtmOnlineErgebnismeldungen.ordneZu(List.of(meldung), runde1(partie), ONLINE_IDS).getFirst();

        assertThat(zuordnung.partie()).isEqualTo(partie);
        assertThat(List.of(zuordnung.punkteA(), zuordnung.punkteB())).containsExactly(2, 13);
    }

    @Test
    void vergisstEntschiedeneMeldungenErstWennPtmOnlineSieNichtMehrLiefert() {
        String turnier = "t-unentschieden";
        ErgebnismeldungDto m1 = meldung("m1", 1, "r1", "r2", 13, 7);
        ErgebnismeldungDto m2 = meldung("m2", 1, "r3", "r4", 13, 7);
        PtmOnlineErgebnismeldungen.vergessen(turnier);
        assertThat(PtmOnlineErgebnismeldungen.unentschieden(turnier, List.of(m1, m2))).containsExactly(m1, m2);

        PtmOnlineErgebnismeldungen.merkeEntschieden(turnier, List.of("m1"));

        assertThat(PtmOnlineErgebnismeldungen.unentschieden(turnier, List.of(m1, m2))).containsExactly(m2);
        assertThat(PtmOnlineErgebnismeldungen.unentschieden(turnier, List.of(m2))).containsExactly(m2);
        assertThat(PtmOnlineErgebnismeldungen.unentschieden(turnier, List.of(m1, m2)))
                .as("nicht mehr geliefert, also vergessen: ein erneut offenes m1 erscheint wieder").containsExactly(m1, m2);
    }
}
