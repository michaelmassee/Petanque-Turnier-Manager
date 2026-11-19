/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.live;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.ptmonline.live.ErgebnisSchreiber.DirektEintrag;

/** Direkt-Eintrag gemeldeter Ergebnisse, während die Turnierleitung parallel Ergebnisse eintippt. */
class ErgebnisSchreiberTest {

    @Test
    void leerePartieWirdBeschrieben() {
        assertThat(ErgebnisSchreiber.bewerte(null, null, 13, 7)).isEqualTo(DirektEintrag.GESCHRIEBEN);
        assertThat(DirektEintrag.GESCHRIEBEN.istUebernommen()).isTrue();
    }

    @Test
    void gleichesErgebnisDerTurnierleitungGiltAlsUebernommen() {
        assertThat(ErgebnisSchreiber.bewerte("13", "7", 13, 7)).isEqualTo(DirektEintrag.BEREITS_GLEICH);
        assertThat(DirektEintrag.BEREITS_GLEICH.istUebernommen()).isTrue();
    }

    @Test
    void abweichendesErgebnisDerTurnierleitungBleibtStehen() {
        assertThat(ErgebnisSchreiber.bewerte("13", "11", 13, 7)).isEqualTo(DirektEintrag.ABWEICHEND);
        assertThat(ErgebnisSchreiber.bewerte("7", "13", 13, 7)).isEqualTo(DirektEintrag.ABWEICHEND);
        assertThat(DirektEintrag.ABWEICHEND.istUebernommen()).isFalse();
    }

    @Test
    void halbEingetragenesErgebnisWirdNichtUeberschrieben() {
        assertThat(ErgebnisSchreiber.bewerte("13", null, 13, 7)).isEqualTo(DirektEintrag.ABWEICHEND);
        assertThat(ErgebnisSchreiber.bewerte(null, "7", 13, 7)).isEqualTo(DirektEintrag.ABWEICHEND);
    }

    @Test
    void fehlendesBlattGiltNichtAlsUebernommen() {
        assertThat(DirektEintrag.BLATT_FEHLT.istUebernommen()).isFalse();
    }
}
