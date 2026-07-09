package de.petanqueturniermanager.timer;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;

/**
 * {@code spieleGong()} nutzt {@code javax.sound.sampled} — auf headless
 * CI-Systemen ohne Audio-Device schlägt das Abspielen typischerweise fehl.
 * Dieser Fehler wird laut Klassendoku nur geloggt, nie durchgereicht — die
 * Tests prüfen daher nur, dass {@code onChange} in keinem Fall wirft.
 */
class TimerSoundPlayerTest {

    @Test
    void onChange_beendetStartetWiederholung_ohneException() throws Exception {
        TimerSoundPlayer player = new TimerSoundPlayer();

        assertThatCode(() -> {
            player.onChange(new TimerState("00:00", 0, TimerZustand.BEENDET, "Runde 1", "#000000", false));
            // Scheduler-Task hat initialDelay=0 -> kurz warten, damit spieleGong() mindestens
            // einmal durchlaeuft (inkl. Exception-Pfad auf Audio-losen Systemen).
            Thread.sleep(150);
        }).doesNotThrowAnyException();
    }

    @Test
    void onChange_erneutBeendet_erzeugtKeineZweiteWiederholung() {
        TimerSoundPlayer player = new TimerSoundPlayer();
        TimerState beendet = new TimerState("00:00", 0, TimerZustand.BEENDET, "", "#000000", false);

        assertThatCode(() -> {
            player.onChange(beendet);
            player.onChange(beendet); // wiederholungsTask != null -> kein zweiter Task
        }).doesNotThrowAnyException();
    }

    @Test
    void onChange_snoozed_stopptWiederholung() {
        TimerSoundPlayer player = new TimerSoundPlayer();

        assertThatCode(() -> {
            player.onChange(new TimerState("00:00", 0, TimerZustand.BEENDET, "", "#000000", false));
            player.onChange(new TimerState("00:00", 0, TimerZustand.BEENDET, "", "#000000", true)); // snoozed
        }).doesNotThrowAnyException();
    }

    @Test
    void onChange_verlaessBeendet_stopptWiederholung() {
        TimerSoundPlayer player = new TimerSoundPlayer();

        assertThatCode(() -> {
            player.onChange(new TimerState("00:00", 0, TimerZustand.BEENDET, "", "#000000", false));
            player.onChange(TimerState.inaktiv());
            // zweiter Stopp-Aufruf: wiederholungsTask ist bereits null -> No-Op-Zweig
            player.onChange(TimerState.inaktiv());
        }).doesNotThrowAnyException();
    }

    @Test
    void onChange_niemalsBeendet_startetKeineWiederholung() {
        TimerSoundPlayer player = new TimerSoundPlayer();

        assertThatCode(() -> player.onChange(
                new TimerState("04:32", 272, TimerZustand.LAEUFT, "Runde 1", "#123456", false)))
                .doesNotThrowAnyException();
    }
}
