package de.petanqueturniermanager.spielerdb.importer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.spielerdb.importer.ImportRohdaten.RohLabel;
import de.petanqueturniermanager.spielerdb.importer.ImportRohdaten.RohSpieler;
import de.petanqueturniermanager.spielerdb.importer.ImportRohdaten.RohSpielerLabel;
import de.petanqueturniermanager.spielerdb.importer.ImportRohdaten.RohVerein;

class SpielerDbImportValidatorTest {

    @Test
    void leerePflichtfelder_sammelteFehler() {
        ImportRohdaten roh = new ImportRohdaten(
                List.of(new RohSpieler(1, "", "", null, null, null)),
                List.of(new RohVerein(10, "")),
                List.of(new RohLabel(5, "")),
                List.of());
        SpielerDbValidationException ex =
                org.assertj.core.api.Assertions.catchThrowableOfType(
                        () -> new SpielerDbImportValidator().validiere(roh),
                        SpielerDbValidationException.class);
        assertThat(ex).isNotNull();
        assertThat(ex.fehler()).hasSize(3);
    }

    @Test
    void duplikateInDatei_werdenAlsWarnungenLastWinsZusammengefuehrt() throws Exception {
        ImportRohdaten roh = new ImportRohdaten(
                List.of(
                        new RohSpieler(1, "Hans", "Müller", 10, "BC Linden", null),
                        new RohSpieler(2, "  hans ", "MÜLLER", 10, "BC Linden", "LIZ-NEU")),
                List.of(),
                List.of(),
                List.of());
        ValidierteDaten vd = new SpielerDbImportValidator().validiere(roh);

        assertThat(vd.spieler()).hasSize(1);
        assertThat(vd.spieler().get(0).altNr()).isEqualTo(2);  // Last wins
        assertThat(vd.spieler().get(0).lizenznr()).isEqualTo("LIZ-NEU");
        assertThat(vd.warnungen()).hasSize(1);
        assertThat(vd.warnungen().get(0).text()).contains("mehrfach");
    }

    @Test
    void junctionAufUnbekannteId_wirdAlsWarnungVerworfen() throws Exception {
        ImportRohdaten roh = new ImportRohdaten(
                List.of(new RohSpieler(1, "Hans", "Müller", null, null, null)),
                List.of(),
                List.of(new RohLabel(5, "Senior")),
                List.of(
                        new RohSpielerLabel(1, 5),
                        new RohSpielerLabel(99, 5),
                        new RohSpielerLabel(1, 999)));
        ValidierteDaten vd = new SpielerDbImportValidator().validiere(roh);

        assertThat(vd.spielerLabels()).hasSize(1);
        assertThat(vd.warnungen()).hasSizeGreaterThanOrEqualTo(2);
    }
}
