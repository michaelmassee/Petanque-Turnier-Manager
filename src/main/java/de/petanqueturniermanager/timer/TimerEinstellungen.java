package de.petanqueturniermanager.timer;

import de.petanqueturniermanager.comp.GlobalProperties;
import de.petanqueturniermanager.helper.i18n.I18n;

/**
 * Liest und schreibt die letzten Timer-Einstellungen via {@link GlobalProperties}.
 * Ermöglicht die Vorbelegung des {@link TimerDialog} mit den zuletzt verwendeten Werten.
 */
public class TimerEinstellungen {

    static final int DEFAULT_PORT             = 8085;
    static final int DEFAULT_HINTERGRUNDFARBE = 0x000000;

    private TimerEinstellungen() {
    }

    /** Liefert die zuletzt gespeicherte Dauer als "MM:SS"-String oder den i18n-Default. */
    public static String letzteDauer() {
        return GlobalProperties.get().getTimerLetzteDauer(
                I18n.get("timer.dialog.vorbelegung.dauer"));
    }

    /** Liefert den zuletzt gespeicherten Webserver-Port (Default: {@value #DEFAULT_PORT}). */
    public static int letzterPort() {
        return GlobalProperties.get().getTimerLetzterPort(DEFAULT_PORT);
    }

    /** Liefert die zuletzt gespeicherte Bezeichnung (kann leer sein). */
    public static String letzteBezeichnung() {
        return GlobalProperties.get().getTimerLetzteBezeichnung();
    }

    /** Liefert die zuletzt gespeicherte Hintergrundfarbe als RGB-Integer (Default: Schwarz). */
    public static int letzteHintergrundFarbe() {
        return GlobalProperties.get().getTimerHintergrundFarbe(DEFAULT_HINTERGRUNDFARBE);
    }

    /**
     * Speichert die zuletzt verwendeten Timer-Einstellungen dauerhaft.
     *
     * @param dauer            Dauer als "MM:SS"
     * @param port             Webserver-Port
     * @param bezeichnung      optionaler Rundenname
     * @param hintergrundFarbe Hintergrundfarbe als RGB-Integer
     */
    public static void speichern(String dauer, int port, String bezeichnung, int hintergrundFarbe) {
        GlobalProperties.get().speichernTimerEinstellungen(dauer, port, bezeichnung, hintergrundFarbe);
    }
}
