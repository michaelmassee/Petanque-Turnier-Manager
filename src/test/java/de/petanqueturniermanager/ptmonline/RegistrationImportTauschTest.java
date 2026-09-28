/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

class RegistrationImportTauschTest {

    private static final Map<String, String> BISHERIGE_ONLINE_IDS = Map.of(
            "zeileA", "onlineA",
            "zeileB", "onlineB",
            "zeileC", "onlineC");

    @Test
    void vollstaendigerTauschIstGeschlossen() {
        Map<String, String> tausch = Map.of("zeileA", "onlineB", "zeileB", "onlineA");
        assertThat(RegistrationImportTask.nichtMitgetauschteOnlineId(tausch, BISHERIGE_ONLINE_IDS)).isEmpty();
    }

    @Test
    void vollstaendigerRingtauschIstGeschlossen() {
        Map<String, String> ringtausch = Map.of("zeileA", "onlineB", "zeileB", "onlineC", "zeileC", "onlineA");
        assertThat(RegistrationImportTask.nichtMitgetauschteOnlineId(ringtausch, BISHERIGE_ONLINE_IDS)).isEmpty();
    }

    @Test
    void unvollstaendigerRingtauschMeldetNichtMitgetauschteAnmeldung() {
        // Zeile C steht unverändert, ihre Anmeldung soll trotzdem an Zeile B wandern (z. B. doppelter Eintrag).
        Map<String, String> unvollstaendig = Map.of("zeileA", "onlineB", "zeileB", "onlineC");
        assertThat(RegistrationImportTask.nichtMitgetauschteOnlineId(unvollstaendig, BISHERIGE_ONLINE_IDS))
                .hasValue("onlineC");
    }
}
