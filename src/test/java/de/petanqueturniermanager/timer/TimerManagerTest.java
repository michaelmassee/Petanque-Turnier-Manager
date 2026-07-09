package de.petanqueturniermanager.timer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.sun.star.uno.XComponentContext;

/**
 * {@code XComponentContext} wird nie tatsächlich dereferenziert (nur beim
 * Port-Konflikt-Fehlerpfad für eine MessageBox — der bleibt hier bewusst
 * ausgespart, da er echte UNO-Toolkit-Integration braucht). Ein reiner
 * Mockito-Mock reicht daher für alle übrigen Pfade.
 */
class TimerManagerTest {

    private static final XComponentContext MOCK_CONTEXT = mock(XComponentContext.class);

    @AfterEach
    void tearDown() {
        TimerManager.dispose();
    }

    private static int freienPortFinden() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────

    @Test
    void get_ohneInit_wirftIllegalStateException() {
        assertThatThrownBy(TimerManager::get).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void init_undGet_liefertInitialenZustand() {
        TimerManager.init(MOCK_CONTEXT);
        assertThat(TimerManager.get().getZustand()).isEqualTo(TimerZustand.INAKTIV);
    }

    @Test
    void init_zweimal_bleibtIdempotent() {
        TimerManager.init(MOCK_CONTEXT);
        var erste = TimerManager.get();
        TimerManager.init(MOCK_CONTEXT);
        assertThat(TimerManager.get()).isSameAs(erste);
    }

    @Test
    void dispose_ohneInit_wirftNicht() {
        TimerManager.dispose(); // kein init() zuvor -> No-Op-Zweig
        assertThatThrownBy(TimerManager::get).isInstanceOf(IllegalStateException.class);
    }

    // ── formatiere / parseDauer ──────────────────────────────────────────────

    @Test
    void formatiere_verschiedeneSekundenwerte() {
        assertThat(TimerManager.formatiere(0)).isEqualTo("00:00");
        assertThat(TimerManager.formatiere(59)).isEqualTo("00:59");
        assertThat(TimerManager.formatiere(60)).isEqualTo("01:00");
        assertThat(TimerManager.formatiere(272)).isEqualTo("04:32");
        assertThat(TimerManager.formatiere(-5)).isEqualTo("00:00");
    }

    @Test
    void parseDauer_gueltigeFormate() {
        assertThat(TimerManager.parseDauer("4:32")).isEqualTo(272);
        assertThat(TimerManager.parseDauer("04:32")).isEqualTo(272);
        assertThat(TimerManager.parseDauer("0:01")).isEqualTo(1);
        assertThat(TimerManager.parseDauer("99:59")).isEqualTo(99 * 60 + 59);
    }

    @Test
    void parseDauer_nullString_wirft() {
        assertThatThrownBy(() -> TimerManager.parseDauer(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parseDauer_ungueltigesFormat_wirft() {
        assertThatThrownBy(() -> TimerManager.parseDauer("abc")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TimerManager.parseDauer("12-34")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TimerManager.parseDauer("1:60")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TimerManager.parseDauer("123:45")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parseDauer_nullDauer_wirft() {
        assertThatThrownBy(() -> TimerManager.parseDauer("0:00")).isInstanceOf(IllegalArgumentException.class);
    }

    // ── No-Op-Zweige ohne laufenden Timer ────────────────────────────────────

    @Test
    void pauseOderFortsetzen_imInaktivenZustand_bleibtInaktiv() {
        TimerManager.init(MOCK_CONTEXT);
        TimerManager.get().pauseOderFortsetzen();
        assertThat(TimerManager.get().getZustand()).isEqualTo(TimerZustand.INAKTIV);
    }

    @Test
    void zeitAnpassen_imInaktivenZustand_wirdIgnoriert() {
        TimerManager.init(MOCK_CONTEXT);
        TimerManager.get().zeitAnpassen(60);
        assertThat(TimerManager.get().getZustand()).isEqualTo(TimerZustand.INAKTIV);
    }

    @Test
    void snooze_ohneBeendet_wirdIgnoriert() {
        TimerManager.init(MOCK_CONTEXT);
        TimerManager.get().snooze();
        assertThat(TimerManager.get().getZustand()).isEqualTo(TimerZustand.INAKTIV);
    }

    @Test
    void starten_mitUngueltigerDauer_wirdIgnoriert() throws Exception {
        TimerManager.init(MOCK_CONTEXT);
        TimerManager.get().starten(0, "x", freienPortFinden(), 0x123456);
        assertThat(TimerManager.get().getZustand()).isEqualTo(TimerZustand.INAKTIV);
    }

    @Test
    void stoppen_imInaktivenZustand_emittiertInaktivState() {
        TimerManager.init(MOCK_CONTEXT);
        var empfangen = new CopyOnWriteArrayList<TimerState>();
        TimerManager.get().addListener(empfangen::add);

        TimerManager.get().stoppen();

        assertThat(empfangen).isNotEmpty();
        assertThat(empfangen.getLast().zustand()).isEqualTo(TimerZustand.INAKTIV);
    }

    @Test
    void addUndRemoveListener_removeVerhindertWeitereBenachrichtigung() {
        TimerManager.init(MOCK_CONTEXT);
        var empfangen = new CopyOnWriteArrayList<TimerState>();
        TimerListener listener = empfangen::add;

        TimerManager.get().addListener(listener);
        TimerManager.get().stoppen();
        int nachErstemStopp = empfangen.size();

        TimerManager.get().removeListener(listener);
        TimerManager.get().stoppen();

        assertThat(empfangen).hasSize(nachErstemStopp);
    }

    // ── Voller Zyklus mit echtem Timer-Webserver ─────────────────────────────

    @Test
    void starten_pausierenFortsetzenUndStoppen_kompletterZyklus() throws Exception {
        TimerManager.init(MOCK_CONTEXT);
        int port = freienPortFinden();
        var empfangen = new CopyOnWriteArrayList<TimerState>();
        TimerManager.get().addListener(empfangen::add);

        TimerManager.get().starten(30, "Testrunde", port, 0x112233);
        assertThat(TimerManager.get().getZustand()).isEqualTo(TimerZustand.LAEUFT);

        TimerManager.get().pauseOderFortsetzen();
        assertThat(TimerManager.get().getZustand()).isEqualTo(TimerZustand.PAUSIERT);

        TimerManager.get().zeitAnpassen(-10); // PAUSIERT-Zweig: restNanos anpassen
        TimerManager.get().pauseOderFortsetzen();
        assertThat(TimerManager.get().getZustand()).isEqualTo(TimerZustand.LAEUFT);

        TimerManager.get().zeitAnpassen(60); // LAEUFT-Zweig: endNanos anpassen
        assertThat(empfangen).isNotEmpty();

        TimerState aktuell = TimerManager.get().getAktuellerZustand();
        assertThat(aktuell.zustand()).isEqualTo(TimerZustand.LAEUFT);

        TimerManager.get().stoppen();
        assertThat(TimerManager.get().getZustand()).isEqualTo(TimerZustand.INAKTIV);
    }

    @Test
    void timerLaeuftAb_wechseltZuBeendetUndSnoozeStummschaltet() throws Exception {
        TimerManager.init(MOCK_CONTEXT);
        int port = freienPortFinden();
        var empfangen = new CopyOnWriteArrayList<TimerState>();
        TimerManager.get().addListener(empfangen::add);

        TimerManager.get().starten(1, null, port, 0);

        // Bis zu 5s auf BEENDET warten (Tick-Intervall 1s + Toleranz)
        long deadline = System.nanoTime() + 5_000_000_000L;
        while (TimerManager.get().getZustand() != TimerZustand.BEENDET && System.nanoTime() < deadline) {
            Thread.sleep(100);
        }
        assertThat(TimerManager.get().getZustand()).isEqualTo(TimerZustand.BEENDET);

        TimerManager.get().snooze();
        assertThat(empfangen.getLast().snoozed()).isTrue();

        // Zweiter snooze()-Aufruf: bereits snoozed -> No-Op-Zweig
        int groesseVorher = empfangen.size();
        TimerManager.get().snooze();
        assertThat(empfangen).hasSize(groesseVorher);
    }
}
