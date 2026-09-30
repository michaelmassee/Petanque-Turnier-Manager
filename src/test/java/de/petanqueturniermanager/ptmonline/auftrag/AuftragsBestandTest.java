/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.auftrag;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.ptmonline.auftrag.versand.SyncAntwort;
import de.petanqueturniermanager.ptmonline.auftrag.versand.VersandErgebnis;

class AuftragsBestandTest {

    private static SyncAuftrag erzeuge(AuftragsBestand bestand, AuftragsArt art) {
        return bestand.erzeuge(art, "POST", "/api/x", "{}", "{}");
    }

    private static VersandErgebnis ergebnis(SyncAuftrag auftrag, boolean angenommen) {
        return new VersandErgebnis(auftrag, new SyncAntwort(angenommen ? 200 : 409, "{}", false), angenommen);
    }

    @Test
    void vergibtFortlaufendeZaehlerUndNeueAuftragsIds() {
        AuftragsBestand bestand = AuftragsBestand.leer();

        SyncAuftrag erster = erzeuge(bestand, AuftragsArt.TEILNAHME);
        SyncAuftrag zweiter = erzeuge(bestand, AuftragsArt.RUNDE);

        assertThat(erster.zaehler()).isEqualTo(1);
        assertThat(zweiter.zaehler()).isEqualTo(2);
        assertThat(erster.auftragsId()).isNotEqualTo(zweiter.auftragsId());
        assertThat(bestand.zuSenden(false)).containsExactly(erster, zweiter);
        assertThat(bestand.istGeaendert()).isTrue();
    }

    @Test
    void sendetGesendeteAuftraegeBisZumAnwendenNichtErneut() {
        AuftragsBestand bestand = AuftragsBestand.leer();
        SyncAuftrag auftrag = erzeuge(bestand, AuftragsArt.TEILNAHME);

        bestand.gesendet(ergebnis(auftrag, true));

        assertThat(bestand.zuSenden(false)).isEmpty();
        assertThat(bestand.hatOffene()).isTrue();
        bestand.angewendet(bestand.ergebnisseZumAnwenden().getFirst(), "");
        assertThat(bestand.hatOffene()).isFalse();
        assertThat(bestand.eintraege()).isEmpty();
    }

    @Test
    void behaeltAbgelehnteAuftraegeAlsProtokollzeile() {
        AuftragsBestand bestand = AuftragsBestand.leer();
        SyncAuftrag auftrag = erzeuge(bestand, AuftragsArt.ANMELDUNG_ANLEGEN);

        bestand.angewendet(ergebnis(auftrag, false), "tournament_running");

        assertThat(bestand.eintraege()).singleElement().satisfies(eintrag -> {
            assertThat(eintrag.zustand()).isEqualTo(AuftragsBestand.Zustand.ABGELEHNT);
            assertThat(eintrag.grund()).isEqualTo("tournament_running");
        });
        assertThat(bestand.zuSenden(false)).isEmpty();
    }

    @Test
    void verwirftBeimStartNurUngesendeteAuftraegeDerAnmeldephase() {
        AuftragsBestand bestand = AuftragsBestand.leer();
        SyncAuftrag gesendet = erzeuge(bestand, AuftragsArt.TEILNAHME);
        SyncAuftrag anlage = erzeuge(bestand, AuftragsArt.ANMELDUNG_ANLEGEN);
        SyncAuftrag runde = erzeuge(bestand, AuftragsArt.RUNDE);
        bestand.gesendet(ergebnis(gesendet, true));

        List<SyncAuftrag> verworfen = bestand.verwerfe(AuftragsArt::wirdBeimStartVerworfen, "Turnierstart");
        SyncAuftrag start = erzeuge(bestand, AuftragsArt.START);

        assertThat(verworfen).containsExactly(anlage);
        assertThat(bestand.zuSenden(false)).containsExactly(runde, start);
        assertThat(start.zaehler()).isEqualTo(4);
        assertThat(bestand.eintraege()).filteredOn(eintrag -> eintrag.zustand() == AuftragsBestand.Zustand.VERWORFEN)
                .extracting(AuftragsBestand.Eintrag::grund).containsExactly("Turnierstart");
    }

    @Test
    void sendetInDerPauseNurDenStartAmAnfangDerReihe() {
        AuftragsBestand bestand = AuftragsBestand.leer();
        SyncAuftrag start = erzeuge(bestand, AuftragsArt.START);
        erzeuge(bestand, AuftragsArt.TEILNAHME);

        assertThat(bestand.zuSenden(true)).containsExactly(start);

        AuftragsBestand andererBestand = AuftragsBestand.leer();
        erzeuge(andererBestand, AuftragsArt.RUNDE);
        erzeuge(andererBestand, AuftragsArt.START);
        assertThat(andererBestand.zuSenden(true)).isEmpty();
    }

    @Test
    void neueBindungVerwirftOffeneAuftraegeUndUebernimmtDenServerZaehler() {
        AuftragsBestand bestand = AuftragsBestand.leer();
        erzeuge(bestand, AuftragsArt.TEILNAHME);
        erzeuge(bestand, AuftragsArt.TEILNAHME);

        bestand.neueBindung(0, "neue Bindung");

        assertThat(bestand.zuSenden(false)).isEmpty();
        assertThat(erzeuge(bestand, AuftragsArt.TEILNAHME).zaehler()).isEqualTo(1);
    }

    @Test
    void entferntUeberholteAuftraegeOhneProtokoll() {
        AuftragsBestand bestand = AuftragsBestand.leer();
        SyncAuftrag alt = bestand.erzeuge(AuftragsArt.RUNDE, "PUT", "/api/r/1", "{\"a\":1}", "{}");
        SyncAuftrag neu = bestand.erzeuge(AuftragsArt.RUNDE, "PUT", "/api/r/1", "{\"a\":2}", "{}");

        bestand.entferneUeberholte(auftrag -> auftrag.pfad().equals(neu.pfad()) && auftrag.zaehler() < neu.zaehler());

        assertThat(bestand.zuSenden(false)).containsExactly(neu).doesNotContain(alt);
    }

    @Test
    void laedtGespeichertenBestandUndSetztDenZaehlerNieUnterDenHoechstenAuftrag() {
        AuftragsBestand quelle = AuftragsBestand.leer();
        erzeuge(quelle, AuftragsArt.TEILNAHME);
        erzeuge(quelle, AuftragsArt.TEILNAHME);

        AuftragsBestand geladen = AuftragsBestand.aus(1, quelle.eintraege());

        assertThat(geladen.zaehler()).isEqualTo(2);
        assertThat(geladen.zuSenden(false)).hasSize(2);
        assertThat(geladen.istGeaendert()).isFalse();
    }

    @Test
    void kuerztDasProtokollOhneOffeneAuftraegeZuVerlieren() {
        AuftragsBestand bestand = AuftragsBestand.leer();
        SyncAuftrag offen = erzeuge(bestand, AuftragsArt.RUNDE);
        for (int i = 0; i < AuftragsBestand.MAX_PROTOKOLL + 5; i++) {
            bestand.angewendet(ergebnis(erzeuge(bestand, AuftragsArt.TEILNAHME), false), "x");
        }

        assertThat(bestand.eintraege()).hasSize(AuftragsBestand.MAX_PROTOKOLL + 1);
        assertThat(bestand.zuSenden(false)).containsExactly(offen);
    }
}
