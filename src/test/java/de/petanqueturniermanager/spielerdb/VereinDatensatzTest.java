package de.petanqueturniermanager.spielerdb;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class VereinDatensatzTest {

    @Test
    void neu_liefertDatensatzOhneNr() {
        VereinDatensatz d = VereinDatensatz.neu("BC Linden");
        assertThat(d.nr()).isNull();
        assertThat(d.name()).isEqualTo("BC Linden");
    }
}
