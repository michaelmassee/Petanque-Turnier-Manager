package de.petanqueturniermanager.algorithmen.kaskaden;
import de.petanqueturniermanager.algorithmen.common.CadrageRechner;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

public class KaskadenKoFeldInfoTest {

    @Test
    public void cadrageRechnerFuer_leerWennZuWenigeTeams() {
        assertThat(KaskadenKoFeldInfo.cadrageRechnerFuer(0)).isEmpty();
        assertThat(KaskadenKoFeldInfo.cadrageRechnerFuer(1)).isEmpty();
    }

    @Test
    public void cadrageRechnerFuer_befuelltAbZweiTeams() {
        assertThat(KaskadenKoFeldInfo.cadrageRechnerFuer(2)).isPresent();
        assertThat(KaskadenKoFeldInfo.cadrageRechnerFuer(7)).isPresent();
    }

    @Test
    public void leeresFeld_alleAbgeleitetenWerteFallbackAufNull() {
        var leer = new KaskadenKoFeldInfo("A", "", 0, Optional.empty());

        assertThat(leer.isCadrageNoetig()).isFalse();
        assertThat(leer.anzCadrageSpiele()).isZero();
        assertThat(leer.anzFreilose()).isZero();
        assertThat(leer.zielTeams()).isEqualTo(0);  // Fallback: gesamtTeams
    }

    @Test
    public void zweierpotenz_zielTeamsIstGesamtKeineCadrage() {
        // 4 Teams = Zweierpotenz → CadrageRechner.anzTeams() == 0
        var feld = new KaskadenKoFeldInfo("A", "SS", 4, KaskadenKoFeldInfo.cadrageRechnerFuer(4));

        assertThat(feld.isCadrageNoetig()).isFalse();
        assertThat(feld.anzCadrageSpiele()).isZero();
        assertThat(feld.zielTeams()).isEqualTo(4);
    }

    @Test
    public void siebenTeams_cadrageNoetigZielVierTeams() {
        // 7 Teams → Ziel = 4 (nächste kleinere Zweierpotenz);
        // 6 Teams spielen Cadrage (= 3 Spiele), 1 Team erhält Freilos.
        var feld = new KaskadenKoFeldInfo("A", "S", 7, KaskadenKoFeldInfo.cadrageRechnerFuer(7));

        assertThat(feld.isCadrageNoetig()).isTrue();
        assertThat(feld.anzCadrageSpiele()).isEqualTo(3);
        assertThat(feld.anzFreilose()).isEqualTo(1);
        assertThat(feld.zielTeams()).isEqualTo(4);
    }
}
