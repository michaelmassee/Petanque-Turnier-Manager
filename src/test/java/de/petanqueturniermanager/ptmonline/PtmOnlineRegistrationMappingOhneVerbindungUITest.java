/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;

/**
 * Regression: Ein nie mit PTM-Online verbundenes Dokument hat weder Info- noch Meldungen-Blatt. Das
 * Lesen der Verbindungsdaten muss dann „nicht verbunden“ liefern statt einer NullPointerException,
 * sonst scheitert der Rundenstart-Abgleich und damit das Anlegen der nächsten Spielrunde.
 */
class PtmOnlineRegistrationMappingOhneVerbindungUITest extends BaseCalcUITest {

    @Test
    void dokumentOhneOnlineBlaetterGiltAlsNichtVerbunden() throws Exception {
        PtmOnlineRegistrationMapping mapping = new PtmOnlineRegistrationMapping(wkingSpreadsheet,
                TurnierSystem.SUPERMELEE, 1);

        assertThat(mapping.getTournamentId()).isEmpty();
        assertThat(mapping.getSyncDocumentId()).isEmpty();
        assertThat(mapping.getLeaseToken()).isEmpty();
        assertThat(mapping.getLastSync()).isEmpty();
        assertThat(mapping.istBereitsImportiert("r1")).isFalse();
        assertThat(mapping.getOnlineId("e9e9caec-e0b1-4fe0-8fee-a229279b9f73")).isEmpty();
    }
}
