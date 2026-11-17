/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ankuendigung;

import java.util.regex.Pattern;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;

/**
 * Ankündigung der Turnierleitung (z. B. „Runde 3 startet um 14:30“). Es gibt je Dokument genau eine aktuelle
 * Ankündigung; sie liegt als Dokumenteigenschaft im Turnierdokument, eine leere Ankündigung gilt als gelöscht.
 * Regeln wie bei PTM-Online: höchstens {@value #MAX_LAENGE} Zeichen, Zeilenumbrüche erlaubt, sonst keine
 * Steuerzeichen.
 */
public final class Ankuendigung {

    /** Höchstlänge wie bei PTM-Online. */
    public static final int MAX_LAENGE = 300;

    static final String PROP_ANKUENDIGUNG = "Ankuendigung";

    private static final Pattern STEUERZEICHEN = Pattern.compile("[\\u0000-\\u0009\\u000b-\\u001f\\u007f]");

    private Ankuendigung() {
    }

    /**
     * Vereinheitlicht Zeilenumbrüche, entfernt Steuerzeichen und Leerraum am Rand und kürzt auf
     * {@value #MAX_LAENGE} Zeichen.
     */
    public static String bereinige(String text) {
        if (text == null) {
            return "";
        }
        String bereinigt = STEUERZEICHEN.matcher(text.replaceAll("\\r\\n?", "\n")).replaceAll("").strip();
        return bereinigt.length() <= MAX_LAENGE ? bereinigt : bereinigt.substring(0, MAX_LAENGE).strip();
    }

    /** Aktuelle Ankündigung des Dokuments, leer wenn keine. */
    public static String lese(WorkingSpreadsheet ws) {
        return new DocumentPropertiesHelper(ws).getStringProperty(PROP_ANKUENDIGUNG, "");
    }

    /** Speichert die bereinigte Ankündigung im Dokument; muss auf dem LO-Main-Thread laufen (Properties-Event). */
    public static String speichere(WorkingSpreadsheet ws, String text) {
        String bereinigt = bereinige(text);
        new DocumentPropertiesHelper(ws).setStringProperty(PROP_ANKUENDIGUNG, bereinigt);
        return bereinigt;
    }
}
