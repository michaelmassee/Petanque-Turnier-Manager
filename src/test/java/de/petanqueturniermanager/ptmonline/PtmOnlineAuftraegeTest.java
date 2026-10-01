/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.ptmonline.auftrag.AuftragsArt;
import de.petanqueturniermanager.ptmonline.auftrag.AuftragsBestand;
import de.petanqueturniermanager.ptmonline.auftrag.SyncAuftrag;
import de.petanqueturniermanager.ptmonline.auftrag.versand.SyncAntwort;
import de.petanqueturniermanager.ptmonline.auftrag.versand.VersandErgebnis;
import de.petanqueturniermanager.ptmonline.dto.MeleeTeamDto;

class PtmOnlineAuftraegeTest {

    private static final String TURNIER = "t1";

    private static PtmOnlineStatusAuftrag.Eintrag aktiv(String uuid) {
        return new PtmOnlineStatusAuftrag.Eintrag(uuid, OnlineTeilnahme.AKTIV, null);
    }

    private static int erwarteteRevision(SyncAuftrag auftrag) {
        JsonObject body = JsonParser.parseString(auftrag.body()).getAsJsonObject();
        return body.getAsJsonArray("registrations").get(0).getAsJsonObject().get("expectedExecutionRevision").getAsInt();
    }

    @Test
    void teilnahmeUebergehtNichtZugeordneteMeldungen() {
        AuftragsBestand bestand = AuftragsBestand.leer();

        assertThat(PtmOnlineAuftraege.teilnahme(bestand, TURNIER, List.of(aktiv("u1")), Map.of(), Map.of()))
                .isEmpty();
        assertThat(bestand.hatOffene()).isFalse();
    }

    @Test
    void teilnahmeErwartetDieRevisionNachAllenOffenenAuftraegen() {
        AuftragsBestand bestand = AuftragsBestand.leer();
        Map<String, String> onlineIds = Map.of("u1", "r1");

        SyncAuftrag erster = PtmOnlineAuftraege
                .teilnahme(bestand, TURNIER, List.of(aktiv("u1")), onlineIds, Map.of("u1", 3)).orElseThrow();
        SyncAuftrag zweiter = PtmOnlineAuftraege
                .teilnahme(bestand, TURNIER, List.of(aktiv("u1")), onlineIds, Map.of("u1", 3)).orElseThrow();

        assertThat(erwarteteRevision(erster)).isEqualTo(3);
        assertThat(erwarteteRevision(zweiter)).as("der offene erste Auftrag erhöht die Revision").isEqualTo(4);
    }

    @Test
    void startVerwirftUngesendeteAnlagenUndKommtZuletzt() {
        AuftragsBestand bestand = AuftragsBestand.leer();
        PtmOnlineAuftraege.teilnahme(bestand, TURNIER, List.of(aktiv("u1")), Map.of("u1", "r1"), Map.of());
        PtmOnlineAuftraege.runde(bestand, TURNIER, 1, List.of());

        List<SyncAuftrag> verworfen = PtmOnlineAuftraege.start(bestand, TURNIER, Instant.EPOCH, "Start");

        assertThat(verworfen).extracting(SyncAuftrag::art).containsExactly(AuftragsArt.TEILNAHME);
        assertThat(bestand.zuSenden(false)).extracting(SyncAuftrag::art).containsExactly(AuftragsArt.RUNDE,
                AuftragsArt.START);
    }

    @Test
    void neuerRundenstandUeberholtDenUngesendetenAlten() {
        AuftragsBestand bestand = AuftragsBestand.leer();
        PtmOnlineAuftraege.runde(bestand, TURNIER, 1, List.of());
        PtmOnlineAuftraege.runde(bestand, TURNIER, 2, List.of());
        PtmOnlineAuftraege.rundeLoeschen(bestand, TURNIER, 1);

        assertThat(bestand.zuSenden(false)).extracting(SyncAuftrag::art).containsExactly(AuftragsArt.RUNDE,
                AuftragsArt.RUNDE_LOESCHEN);
    }

    @Test
    void trennenVerwirftAlleOffenenAuftraege() {
        AuftragsBestand bestand = AuftragsBestand.leer();
        PtmOnlineAuftraege.runde(bestand, TURNIER, 1, List.of());

        PtmOnlineAuftraege.trennen(bestand, TURNIER, "getrennt");

        assertThat(bestand.zuSenden(false)).extracting(SyncAuftrag::art).containsExactly(AuftragsArt.TRENNEN);
    }

    @Test
    void neueMeleeTeamzuordnungUeberholtDieUngesendeteAlte() {
        AuftragsBestand bestand = AuftragsBestand.leer();
        PtmOnlineAuftraege.meleeTeams(bestand, TURNIER, List.of(new MeleeTeamDto("a", List.of("r1"))));
        PtmOnlineAuftraege.meleeTeams(bestand, TURNIER, List.of(new MeleeTeamDto("b", List.of("r1", "r2"))));
        PtmOnlineAuftraege.meleeTeams(bestand, TURNIER, List.of());

        assertThat(bestand.zuSenden(false)).singleElement().satisfies(auftrag -> {
            assertThat(auftrag.art()).isEqualTo(AuftragsArt.MELEE_TEAMS);
            assertThat(auftrag.methode()).isEqualTo("PUT");
            assertThat(auftrag.pfad()).endsWith("/t1/melee-teams");
            assertThat(JsonParser.parseString(auftrag.body()).getAsJsonObject().getAsJsonArray("teams").get(0)
                    .getAsJsonObject().getAsJsonArray("registrationIds")).hasSize(2);
        });
    }

    @Test
    void abgelehnteAuftraegeWerdenZuFaellenBisSieQuittiertSind() {
        AuftragsBestand bestand = AuftragsBestand.leer();
        SyncAuftrag aenderung = bestand.erzeuge(AuftragsArt.ANMELDUNG_AENDERN, "PUT", "/x", "{}",
                "{\"lokaleUuid\":\"u1\",\"bezeichnung\":\"Anna Adler\"}");
        SyncAuftrag runde = bestand.erzeuge(AuftragsArt.RUNDE, "PUT", "/r", "{}", "{}");
        for (SyncAuftrag auftrag : List.of(aenderung, runde)) {
            VersandErgebnis abgelehnt = new VersandErgebnis(auftrag, new SyncAntwort(409, "{}", false), false);
            bestand.gesendet(abgelehnt);
            bestand.angewendet(abgelehnt, auftrag == aenderung ? "composition_locked" : "HTTP 409");
        }

        List<PtmOnlineAuftraege.AbgelehnterFall> faelle = PtmOnlineAuftraege.abgelehnteFaelle(bestand);

        assertThat(faelle).extracting(fall -> fall.fall().lokal()).containsExactly("Anna Adler (#1)",
                AuftragsArt.RUNDE.anzeige() + " (#2)");
        assertThat(faelle.getFirst().fall().lokaleUuid()).isEqualTo("u1");
        assertThat(faelle.getFirst().fall().optionen()).containsExactly(Entscheidung.ZUR_KENNTNIS);
        assertThat(faelle.get(0).fall().schluessel()).isNotEqualTo(faelle.get(1).fall().schluessel());

        bestand.quittiere(aenderung.auftragsId());

        assertThat(PtmOnlineAuftraege.abgelehnteFaelle(bestand)).extracting(PtmOnlineAuftraege.AbgelehnterFall::auftragsId)
                .containsExactly(runde.auftragsId());
    }

    @Test
    void bekannteAblehnungscodesErhaltenEigeneHinweise() {
        I18n.init(null);

        assertThat(PtmOnlineAuftraege.ablehnungsHinweis("composition_locked"))
                .isEqualTo(I18n.get("ptmonline.konflikt.hinweis.abgelehnt.composition_locked"));
        assertThat(PtmOnlineAuftraege.ablehnungsHinweis("HTTP 500")).contains("HTTP 500");
    }
}
