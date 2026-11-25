/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.onlinesync.speicher;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.helper.i18n.I18n;

class PtmOnlineSyncStatusTest {

    @BeforeAll
    static void uebersetzungLaden() {
        I18n.initFuerTest(Locale.GERMAN);
    }

    @Test
    void uebersetztDieBekanntenOnlineStatus() {
        for (String status : new String[] { "draft", "registration", "running", "finished" }) {
            assertThat(PtmOnlineSyncStatus.statusAnzeige(status)).isEqualTo(I18n.get("ptmonline.onlinestatus." + status))
                    .isNotEqualTo("ptmonline.onlinestatus." + status);
        }
    }

    @Test
    void zeigtUnbekanntenStatusUnveraendert() {
        assertThat(PtmOnlineSyncStatus.statusAnzeige("archived")).isEqualTo("archived");
    }

    @Test
    void ohneBekanntenStatusKeineAnzeige() {
        assertThat(PtmOnlineSyncStatus.NICHT_VERBUNDEN.onlineStatusAnzeige()).isEmpty();
        assertThat(new PtmOnlineSyncStatus(true, false, Optional.empty(), Optional.of("draft")).onlineStatusAnzeige())
                .contains(I18n.get("ptmonline.onlinestatus.draft"));
    }
}
