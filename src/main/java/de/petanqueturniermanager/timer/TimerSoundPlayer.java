package de.petanqueturniermanager.timer;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Spielt im Zustand {@link TimerZustand#BEENDET} wiederholt einen Gong ab,
 * solange der Timer nicht per Snooze stummgeschaltet wurde.
 * <p>
 * Verhalten:
 * <ul>
 *   <li>Beim Wechsel in {@code BEENDET}: sofort einmal Gong + danach alle
 *       {@value #REPEAT_INTERVALL_SEKUNDEN} Sekunden erneut.</li>
 *   <li>Bei {@code snoozed=true} oder Verlassen von {@code BEENDET}: der
 *       Wiederholungs-Task wird abgebrochen.</li>
 * </ul>
 * Fehler (z.B. fehlendes Audio-Device auf Headless-Systemen) werden nur
 * geloggt und nie an den Aufrufer durchgereicht.
 */
final class TimerSoundPlayer implements TimerListener {

    private static final Logger logger = LogManager.getLogger(TimerSoundPlayer.class);

    private static final String GONG_RESSOURCE = "de/petanqueturniermanager/timer/gong.wav";
    private static final long REPEAT_INTERVALL_SEKUNDEN = 5L;

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        var t = new Thread(r, "PTM-Timer-Gong");
        t.setDaemon(true);
        return t;
    });

    private volatile ScheduledFuture<?> wiederholungsTask;

    @Override
    public synchronized void onChange(TimerState state) {
        if (state.zustand() == TimerZustand.BEENDET && !state.snoozed()) {
            if (wiederholungsTask == null) {
                wiederholungsTask = executor.scheduleAtFixedRate(
                        this::spieleGong, 0, REPEAT_INTERVALL_SEKUNDEN, TimeUnit.SECONDS);
            }
        } else {
            stoppeWiederholung();
        }
    }

    private void stoppeWiederholung() {
        var task = wiederholungsTask;
        if (task != null) {
            task.cancel(false);
            wiederholungsTask = null;
        }
    }

    private void spieleGong() {
        InputStream raw = getClass().getClassLoader().getResourceAsStream(GONG_RESSOURCE);
        if (raw == null) {
            logger.warn("Gong-Ressource nicht gefunden: {}", GONG_RESSOURCE);
            return;
        }
        try (AudioInputStream ais = AudioSystem.getAudioInputStream(new BufferedInputStream(raw))) {
            Clip clip = AudioSystem.getClip();
            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    event.getLine().close();
                }
            });
            clip.open(ais);
            clip.start();
        } catch (UnsupportedAudioFileException | LineUnavailableException | IOException
                | IllegalArgumentException e) {
            logger.warn("Gong konnte nicht abgespielt werden: {}", e.getMessage(), e);
        }
    }
}
