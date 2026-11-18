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
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.onlinesync.sheet.PtmOnlineKonfliktSheet.Zeile;
import de.petanqueturniermanager.ptmonline.dto.ErgebnismeldungDto;
import de.petanqueturniermanager.ptmonline.live.ErgebnisOrt;
import de.petanqueturniermanager.ptmonline.live.LivePartie;
import de.petanqueturniermanager.ptmonline.live.LiveRunde;
import de.petanqueturniermanager.ptmonline.live.LiveTurnierStand;

class PtmOnlineErgebnismeldungenTest {

    private static final Map<Integer, List<String>> ONLINE_IDS = Map.of(1, List.of("r1"), 2, List.of("r2"), 3,
            List.of("r3"), 4, List.of("r4"));
    private static final ErgebnisOrt ORT = ErgebnisOrt.inZeile("schluessel", "1. Spielrunde", 3, 3);

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

    /** Im Turnierbaum stehen die Ergebnis-Zellen untereinander; übernommen werden kann trotzdem. */
    @Test
    void partieImTurnierbaumLaesstSichUebernehmen() {
        ErgebnisOrt imBaum = ErgebnisOrt.inZellen("baum", "A-Finale", Position.from(4, 6), Position.from(4, 8));
        LivePartie partie = LivePartie.teams(3, 4, null, null, null, "A-Finale · Halbfinale").mitErgebnisOrt(imBaum);
        var zuordnung = PtmOnlineErgebnismeldungen.ordneZu(List.of(meldung("m1", 3, "r4", "r3", 13, 9)),
                LiveTurnierStand.ohneRangliste(List.of(new LiveRunde(3, List.of(partie)))), ONLINE_IDS).getFirst();

        assertThat(zuordnung.ort()).isEqualTo(imBaum);
        assertThat(List.of(zuordnung.punkteA(), zuordnung.punkteB())).as("Seiten im Baum vertauscht")
                .containsExactly(9, 13);
        assertThat(zuordnung.fall().optionen()).containsExactly(Entscheidung.ERGEBNIS_UEBERNEHMEN,
                Entscheidung.ERGEBNIS_VERWERFEN);
    }

    @Test
    void ergebnisOrtInDerZeileHatSeiteBRechtsDaneben() {
        assertThat(ORT.punkteA()).isEqualTo(Position.from(3, 3));
        assertThat(ORT.punkteB()).isEqualTo(Position.from(4, 3));
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
        var ohneErgebnisZellen = PtmOnlineErgebnismeldungen.ordneZu(List.of(meldung("m2", 1, "r3", "r4", 13, 7)),
                runde1(ohneOrt), ONLINE_IDS).getFirst();

        assertThat(andereRunde.partie()).isNull();
        assertThat(andereRunde.fall().optionen()).containsExactly(Entscheidung.ERGEBNIS_VERWERFEN);
        assertThat(ohneErgebnisZellen.fall().optionen()).containsExactly(Entscheidung.ERGEBNIS_VERWERFEN);
        assertThat(andereRunde.fall().lokal()).contains("3").contains("4");
    }

    @Test
    void protokollNenntMeldungBisherigesErgebnisUndZellen() {
        LivePartie partie = LivePartie.teams(4, 3, 13, 11, null, null).mitErgebnisOrt(ORT);
        var zuordnung = PtmOnlineErgebnismeldungen.ordneZu(List.of(meldung("m1", 1, "r3", "r4", 13, 7)),
                runde1(partie), ONLINE_IDS).getFirst();

        assertThat(PtmOnlineErgebnismeldungen.protokoll(zuordnung, zuordnung.fall())).contains("Meldung m1")
                .contains("gemeldet 7:13 (Seiten vertauscht)").contains("von 'Team r3'").contains("bisher 13:11")
                .contains("1. Spielrunde " + ORT.punkteA().getAddress() + "/" + ORT.punkteB().getAddress());
    }

    @Test
    void protokollOhneEintragbarePartie() {
        var zuordnung = PtmOnlineErgebnismeldungen.ordneZu(List.of(meldung("m1", 2, "r3", "r4", 13, 7)),
                runde1(LivePartie.teams(3, 4, null, null, null, null)), ONLINE_IDS).getFirst();

        assertThat(PtmOnlineErgebnismeldungen.protokoll(zuordnung, zuordnung.fall())).contains("bisher –")
                .endsWith("Zellen –");
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

    /**
     * Die Turnierleitung wählt in Zeile 1 „übernehmen“ und gleich danach in Zeile 2. Schreibt der Hintergrund den
     * Block dazwischen neu, muss Zeile 2 noch denselben Fall zeigen – sonst landete die Wahl beim Fall aus Zeile 3.
     */
    @Test
    void entschiedenerFallBleibtStehenDamitDieNaechsteWahlBeimGemeintenFallLandet() {
        String uebernehmen = Entscheidung.ERGEBNIS_UEBERNEHMEN.anzeige();
        List<Zeile> bisher = List.of(waehlbar("m1", uebernehmen), waehlbar("m2", ""), waehlbar("m3", ""));

        List<Zeile> neu = PtmOnlineErgebnismeldungen.stabilerBlock(bisher,
                List.of(waehlbar("m2", ""), waehlbar("m3", "")), false);

        assertThat(neu).extracting(Zeile::schluessel).containsExactly(
                schluessel("m1") + PtmOnlineErgebnismeldungen.ERLEDIGT_SUFFIX, schluessel("m2"), schluessel("m3"));
        Zeile erledigt = neu.getFirst();
        assertThat(PtmOnlineErgebnismeldungen.istErledigt(erledigt)).isTrue();
        assertThat(erledigt.optionen()).as("keine Auswahlliste mehr").isEmpty();
        assertThat(erledigt.entscheidung()).contains(uebernehmen);
        assertThat(neu.get(1)).as("Zeile 2 zeigt weiter m2").isEqualTo(waehlbar("m2", ""));
    }

    @Test
    void eineEingetrageneWahlBleibtBeiIhremFall() {
        String verwerfen = Entscheidung.ERGEBNIS_VERWERFEN.anzeige();
        List<Zeile> neu = PtmOnlineErgebnismeldungen.stabilerBlock(
                List.of(waehlbar("m1", ""), waehlbar("m2", "")),
                List.of(waehlbar("m1", ""), waehlbar("m2", verwerfen)), false);

        assertThat(neu).containsExactly(waehlbar("m1", ""), waehlbar("m2", verwerfen));
    }

    @Test
    void neueMeldungKommtAnsEndeAuchWennPtmOnlineSieVorneLiefert() {
        List<Zeile> neu = PtmOnlineErgebnismeldungen.stabilerBlock(List.of(waehlbar("m1", ""), waehlbar("m2", "")),
                List.of(waehlbar("m0", ""), waehlbar("m1", ""), waehlbar("m2", "")), false);

        assertThat(neu).extracting(Zeile::schluessel).containsExactly(schluessel("m1"), schluessel("m2"),
                schluessel("m0"));
    }

    @Test
    void erledigteZeilenEntfallenNurAmEnde() {
        List<Zeile> bisher = List.of(waehlbar("m1", ""), waehlbar("m2", ""), waehlbar("m3", ""), waehlbar("m4", ""));

        List<Zeile> neu = PtmOnlineErgebnismeldungen.stabilerBlock(bisher,
                List.of(waehlbar("m1", ""), waehlbar("m3", "")), false);

        assertThat(neu).extracting(Zeile::schluessel).containsExactly(schluessel("m1"),
                schluessel("m2") + PtmOnlineErgebnismeldungen.ERLEDIGT_SUFFIX, schluessel("m3"));
    }

    @Test
    void ohneOffenenFallIstDerBlockLeer() {
        assertThat(PtmOnlineErgebnismeldungen.stabilerBlock(List.of(waehlbar("m1", ""), waehlbar("m2", "")),
                List.of(), false)).isEmpty();
    }

    /** Ein zweiter Durchlauf ohne Änderung liefert denselben Block, es wird also nichts neu geschrieben. */
    @Test
    void erledigteZeileBleibtBeimNaechstenDurchlaufUnveraendert() {
        List<Zeile> offen = List.of(waehlbar("m2", ""));
        List<Zeile> erster = PtmOnlineErgebnismeldungen.stabilerBlock(
                List.of(waehlbar("m1", ""), waehlbar("m2", "")), offen, false);

        assertThat(PtmOnlineErgebnismeldungen.stabilerBlock(erster, offen, false)).isEqualTo(erster);
    }

    @Test
    void zusammenschiebenEntferntErledigteZeilen() {
        List<Zeile> erster = PtmOnlineErgebnismeldungen.stabilerBlock(
                List.of(waehlbar("m1", ""), waehlbar("m2", "")), List.of(waehlbar("m2", "")), false);

        assertThat(PtmOnlineErgebnismeldungen.stabilerBlock(erster, List.of(waehlbar("m2", "")), true))
                .containsExactly(waehlbar("m2", ""));
    }

    private static String schluessel(String meldungsId) {
        return PtmOnlineErgebnismeldungen.SCHLUESSEL_PRAEFIX + "|" + meldungsId;
    }

    private static Zeile waehlbar(String meldungsId, String entscheidung) {
        return new Zeile("Ergebnis gemeldet", "Runde 1: " + meldungsId, "13:7", "Hinweis", schluessel(meldungsId),
                List.of(Entscheidung.ERGEBNIS_UEBERNEHMEN.anzeige(), Entscheidung.ERGEBNIS_VERWERFEN.anzeige()),
                entscheidung);
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
