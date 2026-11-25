/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.comp;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Das PTM-Online-Untermenü folgt dem Ablauf eines Turniers: Verbinden zuerst, Trennen zuletzt. */
class PtmOnlineMenuControllerTest {

    @Test
    void menueBeginntMitVerbindenUndEndetMitTrennen() {
        assertThat(PtmOnlineMenuController.BEFEHLE).first().isEqualTo(ProtocolHandler.CMD_PTMONLINE_TURNIER_VERBINDEN);
        assertThat(PtmOnlineMenuController.BEFEHLE).last().isEqualTo(ProtocolHandler.CMD_PTMONLINE_VERBINDUNG_TRENNEN);
    }

    @Test
    void jederBefehlStehtGenauEinmalImMenue() {
        assertThat(PtmOnlineMenuController.BEFEHLE).doesNotHaveDuplicates().hasSize(12);
    }
}
