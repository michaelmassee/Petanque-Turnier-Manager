package de.petanqueturniermanager.spielerdb;

/**
 * Fehler aus der Spieler-DB-Schicht. Lock-Fehler (DB durch anderen Prozess
 * blockiert) sind über {@link #istLockFehler()} erkennbar und werden im UI
 * als „bereits geöffnet"-Meldung dargestellt.
 */
public class SpielerDbException extends Exception {

    private static final long serialVersionUID = 1L;

    private final boolean lockFehler;

    public SpielerDbException(String message) {
        super(message);
        this.lockFehler = false;
    }

    public SpielerDbException(String message, Throwable cause) {
        super(message, cause);
        this.lockFehler = false;
    }

    public SpielerDbException(String message, Throwable cause, boolean lockFehler) {
        super(message, cause);
        this.lockFehler = lockFehler;
    }

    public boolean istLockFehler() {
        return lockFehler;
    }
}
