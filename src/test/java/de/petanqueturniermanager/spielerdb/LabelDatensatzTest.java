package de.petanqueturniermanager.spielerdb;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LabelDatensatzTest {

    @Test
    void neu_liefertDatensatzOhneNr() {
        LabelDatensatz d = LabelDatensatz.neu("Anfänger");
        assertThat(d.nr()).isNull();
        assertThat(d.name()).isEqualTo("Anfänger");
    }
}
