/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.ptmonline.PtmOnlineErfassungsRunner.OnlineStandAbruf;
import de.petanqueturniermanager.ptmonline.dto.SyncStandDto;

/**
 * Erkennung eines online geänderten Turnierstatus: Nur ein bekannter, gleicher Status desselben Turniers gilt als
 * unverändert – sonst muss der Hintergrund den Online-Zustand neu abrufen, damit Sync-Blatt und Sidebar ihn zeigen.
 */
class OnlineStandAbrufTest {

    private static final String TURNIER = "t-1";

    @Test
    void gleicherStatusDesselbenTurniersIstBekannt() {
        assertThat(abruf(TURNIER, "draft").kenntStatus(TURNIER, "draft")).isTrue();
    }

    @Test
    void onlineGeaenderterStatusIstNichtBekannt() {
        assertThat(abruf(TURNIER, "draft").kenntStatus(TURNIER, "registration")).isFalse();
    }

    @Test
    void statusEinesAnderenTurniersIstNichtBekannt() {
        assertThat(abruf("t-2", "draft").kenntStatus(TURNIER, "draft")).isFalse();
    }

    @Test
    void gescheiterterAbrufKenntKeinenStatus() {
        assertThat(new OnlineStandAbruf(TURNIER, Optional.empty()).kenntStatus(TURNIER, "draft")).isFalse();
    }

    private static OnlineStandAbruf abruf(String turnierId, String status) {
        return new OnlineStandAbruf(turnierId, Optional.of(new SyncStandDto(status, false, null, 0, 0, null, false)));
    }
}
