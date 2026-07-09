package de.petanqueturniermanager.spielerdb;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MeldelisteSpielerDatenTest {

    @Test
    void trimmtVorUndNachname() {
        MeldelisteSpielerDaten d = new MeldelisteSpielerDaten("  Max  ", "  Mustermann  ", null, 3);
        assertThat(d.vorname()).isEqualTo("Max");
        assertThat(d.nachname()).isEqualTo("Mustermann");
    }

    @Test
    void nullNamen_werdenZuLeerstring() {
        MeldelisteSpielerDaten d = new MeldelisteSpielerDaten(null, null, null, 1);
        assertThat(d.vorname()).isEmpty();
        assertThat(d.nachname()).isEmpty();
    }

    @Test
    void vereinName_wirdGetrimmt() {
        MeldelisteSpielerDaten d = new MeldelisteSpielerDaten("Max", "Mustermann", "  BC Linden  ", 1);
        assertThat(d.vereinName()).isEqualTo("BC Linden");
    }

    @Test
    void leererVereinName_wirdZuNull() {
        MeldelisteSpielerDaten d = new MeldelisteSpielerDaten("Max", "Mustermann", "   ", 1);
        assertThat(d.vereinName()).isNull();
    }

    @Test
    void zeile1BasiertWirdUebernommen() {
        MeldelisteSpielerDaten d = new MeldelisteSpielerDaten("Max", "Mustermann", null, 7);
        assertThat(d.zeile1Basiert()).isEqualTo(7);
    }
}
