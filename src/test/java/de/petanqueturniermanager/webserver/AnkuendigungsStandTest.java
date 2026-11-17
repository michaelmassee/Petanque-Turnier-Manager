/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.webserver;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

class AnkuendigungsStandTest {

    private static final Gson GSON = new Gson();

    @Test
    void ohneAnkuendigungGibtEsKeineZusatzNachricht() {
        assertThat(new AnkuendigungsStand().initZusatzJsons()).isEmpty();
    }

    @Test
    void nurEinGeaenderterStandMussGepushtWerden() {
        var stand = new AnkuendigungsStand();
        String json = GSON.toJson(AnkuendigungSseNachricht.von("Mittagspause", "Ankündigung"));

        assertThat(stand.setze(json)).isTrue();
        assertThat(stand.setze(json)).isFalse();
        assertThat(stand.initZusatzJsons()).containsExactly(json);
    }

    @Test
    void nachrichtTraegtTypTextUndTitel() {
        JsonObject json = GSON.fromJson(GSON.toJson(AnkuendigungSseNachricht.von(null, "Ankündigung")),
                JsonObject.class);

        assertThat(json.get("typ").getAsString()).isEqualTo("ankuendigung");
        assertThat(json.get("text").getAsString()).isEmpty();
        assertThat(json.get("titel").getAsString()).isEqualTo("Ankündigung");
    }

    @Test
    void compositeViewLiefertAnkuendigungAnNeueVerbindungen() throws IOException {
        var konfiguration = new CompositeViewKonfiguration(0, "", 100, new SplitBlatt(0), List.of(), false,
                RandKonfiguration.KEINER);
        var instanz = new CompositeViewInstanz(konfiguration);
        String json = GSON.toJson(AnkuendigungSseNachricht.von("Runde 3 startet", "Ankündigung"));
        try {
            instanz.ankuendigungSetzen(json);

            assertThat(instanz.getInitZusatzJsons()).containsExactly(json);
        } finally {
            instanz.stoppen();
        }
    }

    @Test
    void startseiteLiefertAnkuendigungAnNeueVerbindungen() throws IOException {
        var instanz = new TurnierStartseiteWebServerInstanz(0);
        String json = GSON.toJson(AnkuendigungSseNachricht.von("Siegerehrung 18 Uhr", "Ankündigung"));
        try {
            instanz.ankuendigungSetzen(json);

            assertThat(instanz.getInitZusatzJsons()).containsExactly(json);
        } finally {
            instanz.stoppen();
        }
    }
}
