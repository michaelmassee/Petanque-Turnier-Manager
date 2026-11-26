/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.ptmonline.auftrag.AuftragsArt;
import de.petanqueturniermanager.ptmonline.auftrag.AuftragsBestand;
import de.petanqueturniermanager.ptmonline.auftrag.SyncAuftrag;
import de.petanqueturniermanager.ptmonline.auftrag.versand.AuftragsVersand;
import de.petanqueturniermanager.ptmonline.auftrag.versand.SyncAntwort;
import de.petanqueturniermanager.ptmonline.auftrag.versand.VersandErgebnis;
import de.petanqueturniermanager.ptmonline.dto.MeleeTeamDto;
import de.petanqueturniermanager.timer.TimerState;
import de.petanqueturniermanager.timer.TimerZustand;

class PtmOnlineAuftraegeTest {

    private static final String TURNIER = "t1";
    private static final Instant JETZT = Instant.parse("2026-11-19T10:00:00Z");

    @ParameterizedTest
    @EnumSource(value = AuftragsArt.class, names = { "ANMELDUNG_ANLEGEN", "ANMELDUNG_AENDERN", "ANMELDUNG_STORNIEREN" })
    void fachlicheAblehnungBlockiertAutomatischeWiederholungenBisZurQuittierung(AuftragsArt art) {
        AuftragsBestand bestand = AuftragsBestand.leer();
        SyncAuftrag auftrag = bestand.erzeuge(art, "PUT", "/test", "{}", "{\"lokaleUuid\":\"u1\"}");
        assertThat(PtmOnlineAuftraege.gesperrteMeldungen(bestand)).containsExactly("u1");

        VersandErgebnis abgelehnt = new VersandErgebnis(auftrag, new SyncAntwort(409, "{}", false), false);
        bestand.gesendet(abgelehnt);
        bestand.angewendet(abgelehnt, "registration_drawn");

        assertThat(PtmOnlineAuftraege.gesperrteMeldungen(bestand)).containsExactly("u1");
        bestand.quittiere(auftrag.auftragsId());
        assertThat(PtmOnlineAuftraege.gesperrteMeldungen(bestand)).isEmpty();
    }

    @Test
    void neueBindungAnAnderesTurnierGibtAbgelehnteMeldungenFrei() {
        AuftragsBestand bestand = mitAbgelehnterAnlage("u1");

        bestand.neueBindung(0, "neue Bindung", true);

        assertThat(PtmOnlineAuftraege.gesperrteMeldungen(bestand)).isEmpty();
        assertThat(bestand.eintraege()).extracting(AuftragsBestand.Eintrag::zustand)
                .containsExactly(AuftragsBestand.Zustand.VERWORFEN);
    }

    @Test
    void erneuteBindungAnDasselbeTurnierBehaeltUngeklaerteAblehnungen() {
        AuftragsBestand bestand = mitAbgelehnterAnlage("u1");

        bestand.neueBindung(0, "neue Bindung", false);

        assertThat(PtmOnlineAuftraege.gesperrteMeldungen(bestand)).containsExactly("u1");
    }

    private static AuftragsBestand mitAbgelehnterAnlage(String uuid) {
        AuftragsBestand bestand = AuftragsBestand.leer();
        SyncAuftrag auftrag = bestand.erzeuge(AuftragsArt.ANMELDUNG_ANLEGEN, "PUT", "/test", "{}",
                "{\"lokaleUuid\":\"" + uuid + "\"}");
        VersandErgebnis abgelehnt = new VersandErgebnis(auftrag, new SyncAntwort(409, "{}", false), false);
        bestand.gesendet(abgelehnt);
        bestand.angewendet(abgelehnt, "possible_same_registration");
        return bestand;
    }

    private static PtmOnlineStatusAuftrag.Eintrag aktiv(String uuid) {
        return new PtmOnlineStatusAuftrag.Eintrag(uuid, OnlineTeilnahme.AKTIV, null, null);
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
    void teilnahmeSendetDieTeamnummerDerMeldeliste() {
        AuftragsBestand bestand = AuftragsBestand.leer();
        PtmOnlineStatusAuftrag.Eintrag eingecheckt = new PtmOnlineStatusAuftrag.Eintrag("u1", OnlineTeilnahme.AKTIV,
                null, 7);

        SyncAuftrag auftrag = PtmOnlineAuftraege
                .teilnahme(bestand, TURNIER, List.of(eingecheckt), Map.of("u1", "r1"), Map.of()).orElseThrow();

        JsonObject meldung = JsonParser.parseString(auftrag.body()).getAsJsonObject().getAsJsonArray("registrations")
                .get(0).getAsJsonObject();
        assertThat(meldung.get("teamNumber").getAsInt()).isEqualTo(7);
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
    void startTrotzPauseGehtVorAelterenAuftraegenAllein() {
        AuftragsBestand bestand = AuftragsBestand.leer();
        PtmOnlineAuftraege.rangliste(bestand, TURNIER, List.of(), List.of());

        PtmOnlineAuftraege.startTrotzPause(bestand, TURNIER, Instant.EPOCH, "Start");

        assertThat(bestand.zuSenden(true)).extracting(SyncAuftrag::art).containsExactly(AuftragsArt.START);
        assertThat(bestand.zuSenden(false)).extracting(SyncAuftrag::art).containsExactly(AuftragsArt.START,
                AuftragsArt.RANGLISTE);
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
    void wartenderAbschlussBleibtHinterSpaeterErfassterRanglisteUndRunde() {
        AuftragsBestand bestand = AuftragsBestand.leer();
        PtmOnlineAuftraege.rangliste(bestand, TURNIER, List.of(), List.of());
        String abschlussId = PtmOnlineAuftraege.abschluss(bestand, TURNIER, true, false).auftragsId();

        // Ergebniskorrektur nach einem Netzfehler: der alte Snapshot entfällt, der neue kommt hinzu.
        PtmOnlineAuftraege.runde(bestand, TURNIER, 3, List.of());
        PtmOnlineAuftraege.rangliste(bestand, TURNIER, List.of(), List.of());

        assertThat(bestand.zuSenden(false)).extracting(SyncAuftrag::art).containsExactly(AuftragsArt.RUNDE,
                AuftragsArt.RANGLISTE, AuftragsArt.ABSCHLUSS);
        assertThat(bestand.zuSenden(false).getLast().auftragsId())
                .as("neue Auftrags-ID: PTM-Online friert den neuen Stand ein, auch wenn der alte Abschluss ankam")
                .isNotEqualTo(abschlussId);
    }

    @Test
    void wiederholterAbschlussNachVerlorenerAntwortBekommtNeueAuftragsId() {
        AuftragsBestand bestand = AuftragsBestand.leer();
        PtmOnlineAuftraege.abschluss(bestand, TURNIER, true, false);
        SyncAuftrag gesendet = bestand.zuSenden(false).getFirst();
        // Versand ohne Antwort: PTM-Online kann den Abschluss schon ausgeführt haben.
        assertThatThrownBy(() -> bestand.mitVersandVermerk(auftrag -> {
            throw new IOException("Antwort verloren");
        }).sende(gesendet)).isInstanceOf(IOException.class);

        PtmOnlineAuftraege.rangliste(bestand, TURNIER, List.of(), List.of());

        List<SyncAuftrag> zuSenden = bestand.zuSenden(false);
        assertThat(zuSenden).extracting(SyncAuftrag::art).containsExactly(AuftragsArt.RANGLISTE,
                AuftragsArt.ABSCHLUSS);
        assertThat(zuSenden.getLast().auftragsId()).isNotEqualTo(gesendet.auftragsId());
        assertThat(zuSenden.getLast().body()).isEqualTo(gesendet.body());
        assertThat(bestand.nieGesendete()).as("die Wiederholung hat PTM-Online noch nie erreicht")
                .extracting(SyncAuftrag::art).contains(AuftragsArt.ABSCHLUSS);
    }

    @Test
    void mehrAls200ErgebnismeldungsEntscheidungenVerteilenSichAufMehrereAuftraege() {
        AuftragsBestand bestand = AuftragsBestand.leer();
        Map<String, Boolean> entscheidungen = new LinkedHashMap<>();
        for (int i = 1; i <= 401; i++) {
            entscheidungen.put("m" + i, i % 2 == 0);
        }

        PtmOnlineAuftraege.ergebnismeldungen(bestand, TURNIER, entscheidungen);

        assertThat(bestand.zuSenden(false)).extracting(auftrag -> JsonParser.parseString(auftrag.body())
                .getAsJsonObject().getAsJsonArray("decisions").size()).containsExactly(200, 200, 1);
        assertThat(PtmOnlineAuftraege.beauftragteErgebnismeldungen(bestand)).hasSize(401);
    }

    @Test
    void ergebnismeldungsEntscheidungGehtNachDerRundeMitDemUebernommenenErgebnis() {
        AuftragsBestand bestand = AuftragsBestand.leer();
        PtmOnlineAuftraege.ergebnismeldungen(bestand, TURNIER, Map.of("m-uebernommen", Boolean.TRUE));
        String entscheidungId = bestand.zuSenden(false).getFirst().auftragsId();

        // Nach dem Öffnen des Dokuments werden alle Runden neu erfasst – nach der Entscheidung.
        PtmOnlineAuftraege.runde(bestand, TURNIER, 1, List.of());
        PtmOnlineAuftraege.runde(bestand, TURNIER, 2, List.of());
        PtmOnlineAuftraege.rangliste(bestand, TURNIER, List.of(), List.of());

        List<SyncAuftrag> zuSenden = bestand.zuSenden(false);
        assertThat(zuSenden).as("sonst zeigt die Live-Ansicht bis zur Runde wieder die Eingabefelder")
                .extracting(SyncAuftrag::art).containsExactly(AuftragsArt.RUNDE, AuftragsArt.RUNDE,
                        AuftragsArt.ERGEBNISMELDUNGEN, AuftragsArt.RANGLISTE);
        assertThat(zuSenden.get(2).auftragsId()).isNotEqualTo(entscheidungId);
        assertThat(PtmOnlineAuftraege.beauftragteErgebnismeldungen(bestand)).containsExactly("m-uebernommen");
    }

    @Test
    void ungesendeteErgebnismeldungsEntscheidungUeberstehtDasNeuLaden() {
        AuftragsBestand bestand = AuftragsBestand.leer();
        PtmOnlineAuftraege.ergebnismeldungen(bestand, TURNIER, Map.of("m-verworfen", Boolean.FALSE));
        PtmOnlineAuftraege.ergebnismeldungen(bestand, TURNIER, Map.of("m-uebernommen", Boolean.TRUE));
        AuftragsBestand neuGeladen = AuftragsBestand.aus(bestand.zaehler(), bestand.eintraege());

        assertThat(PtmOnlineAuftraege.beauftragteErgebnismeldungen(neuGeladen))
                .containsExactlyInAnyOrder("m-verworfen", "m-uebernommen");
    }

    @Test
    void trennenGehtVorUndVerwirftDieUebrigenErstNachDemErfolg() {
        AuftragsBestand bestand = AuftragsBestand.leer();
        PtmOnlineAuftraege.runde(bestand, TURNIER, 1, List.of());

        PtmOnlineAuftraege.trennen(bestand, TURNIER);

        assertThat(bestand.zuSenden(false)).extracting(SyncAuftrag::art).containsExactly(AuftragsArt.TRENNEN,
                AuftragsArt.RUNDE);

        PtmOnlineAuftraege.nachTrennen(bestand, "getrennt");

        assertThat(bestand.zuSenden(false)).isEmpty();
    }

    @Test
    void gescheitertesTrennenLaesstDieOffenenAuftraegeUnveraendert() {
        AuftragsBestand bestand = AuftragsBestand.leer();
        PtmOnlineAuftraege.runde(bestand, TURNIER, 1, List.of());
        String rundeId = bestand.zuSenden(false).getFirst().auftragsId();
        SyncAuftrag trennen = PtmOnlineAuftraege.trennen(bestand, TURNIER);
        // Versand scheitert am Netz, „nur lokal trennen“ abgelehnt.
        assertThatThrownBy(() -> bestand.mitVersandVermerk(auftrag -> {
            throw new IOException("HTTP 503");
        }).sende(trennen)).isInstanceOf(IOException.class);

        PtmOnlineAuftraege.trennenZuruecknehmen(bestand, trennen);

        assertThat(bestand.zuSenden(false)).as("kein Trennen mehr, das der Hintergrund später senden könnte")
                .extracting(SyncAuftrag::art, SyncAuftrag::auftragsId)
                .containsExactly(tuple(AuftragsArt.RUNDE, rundeId));
        assertThat(bestand.eintraege()).extracting(AuftragsBestand.Eintrag::zustand)
                .containsOnly(AuftragsBestand.Zustand.OFFEN);
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
        assertThat(PtmOnlineAuftraege.ablehnungsHinweis("formation_locked"))
                .isEqualTo(I18n.get("ptmonline.konflikt.hinweis.abgelehnt.formation_locked"));
        assertThat(PtmOnlineAuftraege.ablehnungsHinweis("HTTP 500")).contains("HTTP 500");
    }

    @Test
    void schliessenDerAnmeldungIstEinGezaehlterAuftragOhneDoppelung() {
        AuftragsBestand bestand = AuftragsBestand.leer();
        PtmOnlineAuftraege.anmeldungSchliessen(bestand, TURNIER);
        PtmOnlineAuftraege.anmeldungSchliessen(bestand, TURNIER);

        assertThat(bestand.zuSenden(false)).singleElement().satisfies(auftrag -> {
            assertThat(auftrag.art()).isEqualTo(AuftragsArt.ANMELDUNG_SCHLIESSEN);
            assertThat(auftrag.methode()).isEqualTo("PUT");
            assertThat(auftrag.pfad()).endsWith("/t1/registration-closed");
            assertThat(auftrag.body()).isEqualTo("{\"closed\":true}");
        });
    }

    @Test
    void ankuendigungIstEinGezaehlterAuftragUndNurDieLetzteWirdGesendet() {
        AuftragsBestand bestand = AuftragsBestand.leer();
        PtmOnlineAuftraege.ankuendigung(bestand, TURNIER, "Runde 3 startet", false);
        PtmOnlineAuftraege.ankuendigung(bestand, TURNIER, "Runde 3 startet \"später\"", false);

        assertThat(bestand.zuSenden(false)).singleElement().satisfies(auftrag -> {
            assertThat(auftrag.art()).isEqualTo(AuftragsArt.ANKUENDIGUNG);
            assertThat(auftrag.methode()).isEqualTo("PUT");
            assertThat(auftrag.pfad()).endsWith("/t1/announcement");
            assertThat(JsonParser.parseString(auftrag.body()).getAsJsonObject().get("text").getAsString())
                    .isEqualTo("Runde 3 startet \"später\"");
        });
    }

    @Test
    void timerIstEinGezaehlterAuftragUndNurDerLetzteStandWirdGesendet() {
        AuftragsBestand bestand = AuftragsBestand.leer();
        PtmOnlineAuftraege.timer(bestand, TURNIER, TournamentSyncClient.timerBody(
                new TimerState("10:00", 600, TimerZustand.LAEUFT, "Runde 1", "#000000", false), 600), JETZT, false);
        PtmOnlineAuftraege.timer(bestand, TURNIER, TournamentSyncClient.timerBody(
                new TimerState("09:00", 540, TimerZustand.PAUSIERT, "Runde 1", "#000000", false), 600), JETZT, false);

        assertThat(bestand.zuSenden(false)).singleElement().satisfies(auftrag -> {
            assertThat(auftrag.art()).isEqualTo(AuftragsArt.TIMER);
            assertThat(auftrag.methode()).isEqualTo("PUT");
            assertThat(auftrag.pfad()).endsWith("/t1/timer");
            assertThat(JsonParser.parseString(auftrag.body()).getAsJsonObject().get("state").getAsString())
                    .isEqualTo("paused");
        });
    }

    @Test
    void timerGehtInDerPauseAllein() {
        AuftragsBestand bestand = AuftragsBestand.leer();
        PtmOnlineAuftraege.runde(bestand, TURNIER, 2, List.of());
        PtmOnlineAuftraege.timer(bestand, TURNIER, "{\"state\":\"stopped\"}", JETZT, true);

        assertThat(bestand.zuSenden(true)).extracting(SyncAuftrag::art).containsExactly(AuftragsArt.TIMER);
    }

    private static String laufenderTimer(long restSekunden) {
        return TournamentSyncClient.timerBody(
                new TimerState("", restSekunden, TimerZustand.LAEUFT, "Runde 1", "#000000", false), 600);
    }

    private static long restzeit(SyncAuftrag auftrag) {
        return JsonParser.parseString(auftrag.body()).getAsJsonObject().get("remainingSec").getAsLong();
    }

    @Test
    void verzoegerterLaufenderTimerGehtMitVerbleibenderRestzeitAlsNeuerAuftragHinaus() {
        AuftragsBestand bestand = AuftragsBestand.leer();
        PtmOnlineAuftraege.timer(bestand, TURNIER, laufenderTimer(600), JETZT, false);
        SyncAuftrag liegengeblieben = bestand.zuSenden(false).getFirst();

        PtmOnlineAuftraege.timerNachstellen(bestand, JETZT.plusSeconds(300), false);

        assertThat(bestand.zuSenden(false)).singleElement().satisfies(auftrag -> {
            assertThat(restzeit(auftrag)).isEqualTo(300);
            // Neue ID: Eine schon angekommene ID mit anderer Nutzlast lehnt PTM-Online ab.
            assertThat(auftrag.auftragsId()).isNotEqualTo(liegengeblieben.auftragsId());
            assertThat(auftrag.zaehler()).isGreaterThan(liegengeblieben.zaehler());
        });
    }

    /**
     * Der Ersatz für einen liegengebliebenen Timer muss gespeichert sein, bevor er hinausgeht: Sonst vergibt das
     * Dokument nach einem Neustart seinen Zähler an eine andere ID (document_forked).
     */
    @Test
    void nachgestellterTimerWirdVorDemVersandGespeichert() throws Exception {
        AuftragsBestand bestand = AuftragsBestand.leer();
        PtmOnlineAuftraege.timer(bestand, TURNIER, laufenderTimer(600), Instant.now().minusSeconds(300), false);
        bestand.gespeichert();
        SyncAuftrag liegengeblieben = bestand.zuSenden(false).getFirst();
        PtmOnlineRegistrationMapping mapping = mock(PtmOnlineRegistrationMapping.class);

        PtmOnlineAuftraege.timerNachstellenUndSpeichern(bestand, mapping, false);

        verify(mapping).schreibeAuftragsBestand(argThat(gespeichert -> gespeichert.zuSenden(false).stream()
                .noneMatch(auftrag -> auftrag.auftragsId().equals(liegengeblieben.auftragsId()))));
        assertThat(bestand.versandSperre().isLocked()).isFalse();
    }

    @Test
    void abgelaufenerTimerBleibtBeiRestzeitNull() {
        AuftragsBestand bestand = AuftragsBestand.leer();
        PtmOnlineAuftraege.timer(bestand, TURNIER, laufenderTimer(60), JETZT, false);

        PtmOnlineAuftraege.timerNachstellen(bestand, JETZT.plusSeconds(300), false);

        assertThat(bestand.zuSenden(false)).singleElement().extracting(PtmOnlineAuftraegeTest::restzeit)
                .isEqualTo(0L);
    }

    @Test
    void timerInnerhalbDerToleranzBleibtUnveraendert() {
        AuftragsBestand bestand = AuftragsBestand.leer();
        PtmOnlineAuftraege.timer(bestand, TURNIER, laufenderTimer(600), JETZT, false);
        SyncAuftrag vorher = bestand.zuSenden(false).getFirst();

        PtmOnlineAuftraege.timerNachstellen(bestand, JETZT.plus(PtmOnlineAuftraege.TIMER_NACHSTELLEN_AB).minusMillis(1),
                false);

        assertThat(bestand.zuSenden(false)).containsExactly(vorher);
    }

    @Test
    void pausierterTimerWirdNichtNachgestellt() {
        AuftragsBestand bestand = AuftragsBestand.leer();
        PtmOnlineAuftraege.timer(bestand, TURNIER, TournamentSyncClient.timerBody(
                new TimerState("", 540, TimerZustand.PAUSIERT, "Runde 1", "#000000", false), 600), JETZT, false);
        SyncAuftrag vorher = bestand.zuSenden(false).getFirst();

        PtmOnlineAuftraege.timerNachstellen(bestand, JETZT.plusSeconds(300), false);

        assertThat(bestand.zuSenden(false)).containsExactly(vorher);
    }

    @Test
    void nachgestellterTimerGehtInDerPauseWeiterAllein() {
        AuftragsBestand bestand = AuftragsBestand.leer();
        PtmOnlineAuftraege.runde(bestand, TURNIER, 2, List.of());
        PtmOnlineAuftraege.timer(bestand, TURNIER, laufenderTimer(600), JETZT, true);

        PtmOnlineAuftraege.timerNachstellen(bestand, JETZT.plusSeconds(60), true);

        assertThat(bestand.zuSenden(true)).singleElement().extracting(PtmOnlineAuftraegeTest::restzeit)
                .isEqualTo(540L);
    }

    @Test
    void ankuendigungGehtInDerPauseAllein() {
        AuftragsBestand bestand = AuftragsBestand.leer();
        PtmOnlineAuftraege.runde(bestand, TURNIER, 2, List.of());
        PtmOnlineAuftraege.ankuendigung(bestand, TURNIER, "Mittagspause", true);

        assertThat(bestand.zuSenden(true)).extracting(SyncAuftrag::art).containsExactly(AuftragsArt.ANKUENDIGUNG);
        assertThat(bestand.zuSenden(false)).extracting(SyncAuftrag::art, SyncAuftrag::zaehler)
                .containsExactly(tuple(AuftragsArt.ANKUENDIGUNG, 2L), tuple(AuftragsArt.RUNDE, 3L));
    }

    @Test
    void leereAnkuendigungLoeschtOnline() {
        AuftragsBestand bestand = AuftragsBestand.leer();
        PtmOnlineAuftraege.ankuendigung(bestand, TURNIER, "", false);

        assertThat(bestand.zuSenden(false)).singleElement()
                .satisfies(auftrag -> assertThat(auftrag.body()).isEqualTo("{\"text\":\"\"}"));
    }

    @Test
    void abschlussIstEinGezaehlterAuftragUndErsetztEinenNochNichtGesendeten() {
        AuftragsBestand bestand = AuftragsBestand.leer();
        PtmOnlineAuftraege.abschluss(bestand, TURNIER, false, false);
        PtmOnlineAuftraege.abschluss(bestand, TURNIER, true, true);

        assertThat(bestand.zuSenden(false)).singleElement().satisfies(auftrag -> {
            assertThat(auftrag.art()).isEqualTo(AuftragsArt.ABSCHLUSS);
            assertThat(auftrag.methode()).isEqualTo("POST");
            assertThat(auftrag.pfad()).endsWith("/t1/finish");
            assertThat(auftrag.body()).isEqualTo("{\"publishRanking\":true,\"notify\":true}");
        });
    }

    @Test
    void benachrichtigtWirdNurZusammenMitDerVeroeffentlichung() {
        assertThat(TournamentSyncClient.abschlussBody(false, true))
                .isEqualTo("{\"publishRanking\":false,\"notify\":false}");
        assertThat(TournamentSyncClient.abschlussBody(true, false))
                .isEqualTo("{\"publishRanking\":true,\"notify\":false}");
    }

    @Test
    void verknuepfungenBuendelnPaareUndLassenOffeneAus() {
        AuftragsBestand bestand = AuftragsBestand.leer();
        Map<String, String> paare = Map.of("u1", "r1", "u2", "r2");

        assertThat(PtmOnlineAuftraege.verknuepfungen(bestand, TURNIER, paare)).isTrue();
        assertThat(PtmOnlineAuftraege.verknuepfungen(bestand, TURNIER, paare)).as("schon im offenen Auftrag").isFalse();

        assertThat(bestand.zuSenden(false)).singleElement().satisfies(auftrag -> {
            assertThat(auftrag.art()).isEqualTo(AuftragsArt.ANMELDUNG_VERKNUEPFEN);
            assertThat(auftrag.methode()).isEqualTo("POST");
            assertThat(auftrag.pfad()).endsWith("/t1/registration-links");
            assertThat(JsonParser.parseString(auftrag.body()).getAsJsonObject().getAsJsonArray("links")).hasSize(2);
        });
        assertThat(PtmOnlineAuftraege.offeneVerknuepfungen(bestand)).isEqualTo(paare);
    }

    @Test
    void verknuepfungenTeilenSichAufHoechstens200JeAuftrag() {
        AuftragsBestand bestand = AuftragsBestand.leer();
        Map<String, String> paare = new LinkedHashMap<>();
        for (int i = 0; i < 201; i++) {
            paare.put("u" + i, "r" + i);
        }

        PtmOnlineAuftraege.verknuepfungen(bestand, TURNIER, paare);

        assertThat(bestand.zuSenden(false)).extracting(auftrag -> JsonParser.parseString(auftrag.body())
                .getAsJsonObject().getAsJsonArray("links").size()).containsExactly(200, 1);
    }

    @Test
    void offeneVerknuepfungenKennenAuchAenderungenUndVergessenAngenommene() {
        AuftragsBestand bestand = AuftragsBestand.leer();
        bestand.erzeuge(AuftragsArt.ANMELDUNG_AENDERN, "PUT", "/x", "{}", "{\"lokaleUuid\":\"u3\",\"onlineId\":\"r3\"}");
        PtmOnlineAuftraege.verknuepfungen(bestand, TURNIER, Map.of("u1", "r1"));
        SyncAuftrag verknuepfung = bestand.zuSenden(false).getLast();

        assertThat(PtmOnlineAuftraege.offeneVerknuepfungen(bestand)).isEqualTo(Map.of("u3", "r3", "u1", "r1"));

        VersandErgebnis angenommen = new VersandErgebnis(verknuepfung, new SyncAntwort(200, "{}", false), true);
        bestand.gesendet(angenommen);
        bestand.angewendet(angenommen, "");

        assertThat(PtmOnlineAuftraege.offeneVerknuepfungen(bestand)).isEqualTo(Map.of("u3", "r3"));
    }

    @Test
    void zurueckgekehrteZeileNimmtIhrenUngesendetenStornoZurueck() {
        AuftragsBestand bestand = AuftragsBestand.leer();
        PtmOnlineAuftraege.storno(bestand, TURNIER, "u1", "r1", 2);
        PtmOnlineAuftraege.storno(bestand, TURNIER, "u2", "r2", 1);

        assertThat(PtmOnlineAuftraege.stornosZuruecknehmen(bestand, Set.of("u1", "u3"))).isTrue();

        assertThat(PtmOnlineAuftraege.gesperrteMeldungen(bestand)).containsExactly("u2");
        assertThat(bestand.zuSenden(false)).singleElement()
                .satisfies(auftrag -> assertThat(auftrag.pfad()).contains("u2"));
    }

    @Test
    void gesendeterStornoWirdNichtZurueckgenommen() {
        AuftragsBestand bestand = AuftragsBestand.leer();
        PtmOnlineAuftraege.storno(bestand, TURNIER, "u1", "r1", 2);
        SyncAuftrag storno = bestand.zuSenden(false).getFirst();
        bestand.gesendet(new VersandErgebnis(storno, new SyncAntwort(200, "{}", false), true));

        assertThat(PtmOnlineAuftraege.stornosZuruecknehmen(bestand, Set.of("u1"))).isTrue();

        assertThat(bestand.ergebnisseZumAnwenden()).hasSize(1);
        assertThat(PtmOnlineAuftraege.gesperrteMeldungen(bestand)).containsExactly("u1");
    }

    @Test
    void waehrendDesVersandsBleibtDerStornoBisZumNaechstenLauf() throws Exception {
        AuftragsBestand bestand = AuftragsBestand.leer();
        PtmOnlineAuftraege.storno(bestand, TURNIER, "u1", "r1", 2);
        CountDownLatch gesperrt = new CountDownLatch(1);
        CountDownLatch freigeben = new CountDownLatch(1);
        Thread versand = Thread.ofPlatform().start(() -> {
            bestand.versandSperre().lock();
            try {
                gesperrt.countDown();
                freigeben.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                bestand.versandSperre().unlock();
            }
        });
        try {
            gesperrt.await();

            assertThat(PtmOnlineAuftraege.stornosZuruecknehmen(bestand, Set.of("u1"))).as("Nachlauf nötig").isFalse();
            assertThat(PtmOnlineAuftraege.stornosZuruecknehmen(bestand, Set.of("u2"))).as("nichts zurückzunehmen")
                    .isTrue();
        } finally {
            freigeben.countDown();
            versand.join();
        }
        assertThat(PtmOnlineAuftraege.stornosZuruecknehmen(bestand, Set.of("u1"))).isTrue();
        assertThat(bestand.hatOffene()).isFalse();
    }

    @Test
    void stornoOhneAntwortWirdNichtZurueckgenommen() throws Exception {
        AuftragsBestand bestand = AuftragsBestand.leer();
        PtmOnlineAuftraege.storno(bestand, TURNIER, "u1", "r1", 2);
        AuftragsVersand.sende(bestand.zuSenden(false), bestand.mitVersandVermerk(auftrag -> {
            throw new IOException("Antwort verloren");
        }), bestand::gesendet);

        assertThat(PtmOnlineAuftraege.stornosZuruecknehmen(bestand, Set.of("u1"))).isTrue();

        assertThat(bestand.zuSenden(false)).as("geht erneut hinaus, PTM-Online liefert die gespeicherte Antwort")
                .hasSize(1);
    }

    @Test
    void wiederholterTeilnahmeAuftragErwartetDieGespeicherteRevision() {
        AuftragsBestand bestand = AuftragsBestand.leer();
        bestand.erzeuge(AuftragsArt.ANMELDUNG_AENDERN, "PUT", "/x", "{}",
                "{\"lokaleUuid\":\"u1\",\"revisionen\":{\"u1\":4}}");
        SyncAuftrag gescheitert = PtmOnlineAuftraege.teilnahme(bestand, TURNIER,
                List.of(aktiv("u1"), new PtmOnlineStatusAuftrag.Eintrag("u2", OnlineTeilnahme.INAKTIV, 7, 12)),
                Map.of("u1", "r1", "u2", "r2"), Map.of("u1", 3, "u2", 1)).orElseThrow();

        SyncAuftrag neu = PtmOnlineAuftraege.teilnahmeErneut(bestand, gescheitert, Map.of("u1", 3, "u2", 1), Set.of())
                .orElseThrow();

        JsonArray registrations = JsonParser.parseString(neu.body()).getAsJsonObject().getAsJsonArray("registrations");
        assertThat(neu.art()).isEqualTo(AuftragsArt.TEILNAHME);
        assertThat(neu.zaehler()).isGreaterThan(gescheitert.zaehler());
        assertThat(registrations).extracting(e -> e.getAsJsonObject().get("expectedExecutionRevision").getAsInt())
                .containsExactly(3, 1);
        assertThat(registrations.get(1).getAsJsonObject().get("participation").getAsString())
                .as("auch eine Abmeldung geht erneut hinaus").isEqualTo(OnlineTeilnahme.INAKTIV.apiWert());
        assertThat(PtmOnlineAuftraege.folgefehlerTeilnahme(neu, Map.of("u1", 3, "u2", 1)))
                .as("passt zur gespeicherten Revision").isEmpty();
    }

    @Test
    void stornoNachRevisionskonfliktEntstehtMitDerOnlineRevisionNeu() {
        AuftragsBestand bestand = AuftragsBestand.leer();
        PtmOnlineAuftraege.storno(bestand, TURNIER, "u1", "r1", 2);
        SyncAuftrag gescheitert = bestand.zuSenden(false).getFirst();

        SyncAuftrag neu = PtmOnlineAuftraege.stornoErneut(bestand, gescheitert, 5);

        JsonObject body = JsonParser.parseString(neu.body()).getAsJsonObject();
        assertThat(neu.art()).isEqualTo(AuftragsArt.ANMELDUNG_STORNIEREN);
        assertThat(neu.auftragsId()).isNotEqualTo(gescheitert.auftragsId());
        assertThat(neu.pfad()).isEqualTo(gescheitert.pfad());
        assertThat(body.get("onlineRegistrationId").getAsString()).isEqualTo("r1");
        assertThat(body.get("expectedExecutionRevision").getAsInt()).isEqualTo(5);
        assertThat(PtmOnlineAuftraege.gesperrteMeldungen(bestand)).as("Kontext bleibt").containsExactly("u1");
    }

    @Test
    void teilnahmeNachAbgelehnterAenderungIstFolgefehlerUndWirdErneutGemeldet() {
        AuftragsBestand bestand = AuftragsBestand.leer();
        Map<String, String> onlineIds = Map.of("u1", "r1", "u2", "r2");
        Map<String, Integer> gespeichert = Map.of("u1", 3, "u2", 1);
        bestand.erzeuge(AuftragsArt.ANMELDUNG_AENDERN, "PUT", "/x", "{}",
                "{\"lokaleUuid\":\"u1\",\"revisionen\":{\"u1\":4}}");
        SyncAuftrag teilnahme = PtmOnlineAuftraege
                .teilnahme(bestand, TURNIER, List.of(aktiv("u1"), aktiv("u2")), onlineIds, gespeichert).orElseThrow();

        assertThat(PtmOnlineAuftraege.folgefehlerTeilnahme(teilnahme, gespeichert))
                .as("erwartete Revision 4 statt der gespeicherten 3").containsExactlyInAnyOrder("u1", "u2");
    }

    @Test
    void teilnahmeKonfliktOhneVorausgehendenAuftragIstKeinFolgefehler() {
        AuftragsBestand bestand = AuftragsBestand.leer();
        Map<String, Integer> gespeichert = Map.of("u1", 3);
        SyncAuftrag teilnahme = PtmOnlineAuftraege
                .teilnahme(bestand, TURNIER, List.of(aktiv("u1")), Map.of("u1", "r1"), gespeichert).orElseThrow();

        assertThat(PtmOnlineAuftraege.folgefehlerTeilnahme(teilnahme, gespeichert))
                .as("online geändert – eine Wiederholung scheiterte erneut").isEmpty();
    }

    @Test
    void ungeklaerteAblehnungSperrtAuchNachLangemProtokoll() {
        AuftragsBestand bestand = AuftragsBestand.leer();
        PtmOnlineAuftraege.storno(bestand, TURNIER, "u1", "r1", 2);
        SyncAuftrag storno = bestand.zuSenden(false).getFirst();
        VersandErgebnis abgelehnt = new VersandErgebnis(storno, new SyncAntwort(409, "{}", false), false);
        bestand.gesendet(abgelehnt);
        bestand.angewendet(abgelehnt, "registration_drawn");
        for (int i = 0; i < 150; i++) {
            SyncAuftrag runde = bestand.erzeuge(AuftragsArt.RUNDE, "PUT", "/r" + i, "{}", "{}");
            bestand.angewendet(new VersandErgebnis(runde, new SyncAntwort(409, "{}", false), false), "HTTP 409");
        }

        assertThat(PtmOnlineAuftraege.gesperrteMeldungen(bestand)).containsExactly("u1");
    }

    @Test
    void neuererAngenommenerCheckinWirdNichtVonEinerAelterenWiederholungUeberschrieben() {
        AuftragsBestand bestand = AuftragsBestand.leer();
        Map<String, String> onlineIds = Map.of("u1", "r1");
        Map<String, Integer> gespeichert = Map.of("u1", 3);
        SyncAuftrag aenderung = bestand.erzeuge(AuftragsArt.ANMELDUNG_AENDERN, "PUT", "/x", "{}",
                "{\"lokaleUuid\":\"u1\",\"revisionen\":{\"u1\":4}}");
        SyncAuftrag aktiv = teilnahme(bestand, "u1", OnlineTeilnahme.AKTIV, onlineIds, gespeichert);
        SyncAuftrag inaktiv = teilnahme(bestand, "u1", OnlineTeilnahme.INAKTIV, onlineIds, gespeichert);
        SyncAuftrag wiederAktiv = teilnahme(bestand, "u1", OnlineTeilnahme.AKTIV, onlineIds, gespeichert);
        List<VersandErgebnis> ergebnisse = List.of(abgelehnt(aenderung, "composition_locked"),
                abgelehnt(aktiv, "execution_conflict"), abgelehnt(inaktiv, "execution_conflict"),
                new VersandErgebnis(wiederAktiv, new SyncAntwort(200, "{\"updatedCount\":1}", false), true));
        ergebnisse.forEach(ergebnis -> bestand.angewendet(ergebnis, ergebnis.angenommen() ? "" : "x"));

        PtmOnlineAuftraege.wiederholeTeilnahmen(bestand, ergebnisse, ergebnisse.subList(1, 3), gespeichert);

        assertThat(bestand.zuSenden(false)).as("der neuere Check-in gilt").isEmpty();
        assertThat(PtmOnlineAuftraege.abgelehnteFaelle(bestand)).extracting(PtmOnlineAuftraege.AbgelehnterFall::auftragsId)
                .as("überholte Folgefehler sind erledigt").containsExactly(aenderung.auftragsId());
    }

    @Test
    void nurDerNeuesteGescheiterteCheckinWirdWiederholt() {
        AuftragsBestand bestand = AuftragsBestand.leer();
        Map<String, String> onlineIds = Map.of("u1", "r1", "u2", "r2");
        Map<String, Integer> gespeichert = Map.of("u1", 3, "u2", 1);
        bestand.erzeuge(AuftragsArt.ANMELDUNG_AENDERN, "PUT", "/x", "{}",
                "{\"lokaleUuid\":\"u1\",\"revisionen\":{\"u1\":4}}");
        SyncAuftrag beide = PtmOnlineAuftraege.teilnahme(bestand, TURNIER, List.of(aktiv("u1"), aktiv("u2")),
                onlineIds, gespeichert).orElseThrow();
        SyncAuftrag abmeldung = teilnahme(bestand, "u1", OnlineTeilnahme.INAKTIV, onlineIds, gespeichert);
        List<VersandErgebnis> konflikte = List.of(abgelehnt(beide, "execution_conflict"),
                abgelehnt(abmeldung, "execution_conflict"));
        konflikte.forEach(ergebnis -> bestand.angewendet(ergebnis, "execution_conflict"));
        bestand.eintraege().stream().filter(eintrag -> eintrag.auftrag().art() == AuftragsArt.ANMELDUNG_AENDERN)
                .forEach(eintrag -> bestand.angewendet(abgelehnt(eintrag.auftrag(), "composition_locked"), "x"));

        PtmOnlineAuftraege.wiederholeTeilnahmen(bestand, konflikte, konflikte, gespeichert);

        List<SyncAuftrag> wiederholt = bestand.zuSenden(false);
        assertThat(wiederholt).hasSize(2);
        assertThat(teilnahmen(wiederholt.get(0))).as("u1 trägt der neuere Auftrag").containsExactly(tuple("r2", "active"));
        assertThat(teilnahmen(wiederholt.get(1))).containsExactly(tuple("r1", "inactive"));
    }

    private static SyncAuftrag teilnahme(AuftragsBestand bestand, String uuid, OnlineTeilnahme teilnahme,
            Map<String, String> onlineIds, Map<String, Integer> gespeichert) {
        return PtmOnlineAuftraege.teilnahme(bestand, TURNIER,
                List.of(new PtmOnlineStatusAuftrag.Eintrag(uuid, teilnahme, null, null)), onlineIds, gespeichert)
                .orElseThrow();
    }

    private static VersandErgebnis abgelehnt(SyncAuftrag auftrag, String code) {
        return new VersandErgebnis(auftrag, new SyncAntwort(409,
                "{\"error\":\"x\",\"details\":{\"code\":\"" + code + "\"}}", false), false);
    }

    private static List<Tuple> teilnahmen(SyncAuftrag auftrag) {
        return JsonParser.parseString(auftrag.body()).getAsJsonObject().getAsJsonArray("registrations").asList()
                .stream().map(JsonElement::getAsJsonObject)
                .map(e -> tuple(e.get("id").getAsString(), e.get("participation").getAsString())).toList();
    }
}
