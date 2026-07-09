package de.petanqueturniermanager.spielerdb;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.spielerdb.AbgleichStatusSenke.AbgleichStatus;
import de.petanqueturniermanager.spielerdb.AbgleichStatusSenke.ZeilenStatus;

class AbgleichStatusSenkeTest {

    @Test
    void zeilenStatus_speichertAlleFelder() {
        ZeilenStatus status = new ZeilenStatus(3, AbgleichStatus.FEHLER, "Verein unbekannt");

        assertThat(status.zeile1Basiert()).isEqualTo(3);
        assertThat(status.status()).isEqualTo(AbgleichStatus.FEHLER);
        assertThat(status.fehlerursache()).isEqualTo("Verein unbekannt");
    }

    @Test
    void abgleichStatus_alleWerteVorhanden() {
        assertThat(AbgleichStatus.values()).containsExactly(
                AbgleichStatus.IN_DB, AbgleichStatus.NEU, AbgleichStatus.FEHLER, AbgleichStatus.FEHLT);
    }
}
