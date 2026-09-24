package de.petanqueturniermanager.spielerdb;

import java.util.List;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;

/**
 * Schreib-Adapter zwischen {@link de.petanqueturniermanager.spielerdb.ui.SpielerSucheDialog}
 * und einer aktiven Meldeliste. Kapselt die system-spezifische Logik (Spaltenindizes,
 * RangeHelper-Schreibvorgänge, optional Vereinsspalte) hinter einer schmalen API.
 *
 * <p>Eine konkrete Implementierung (Schritt 3 der Spieler-DB-Integration) kennt
 * das Turniersystem und liefert hier die nötigen Werte; der Dialog selbst bleibt
 * frei von Sheet-/UNO-Abhängigkeiten oberhalb dieser Schnittstelle.
 */
public interface MeldelisteZiel extends AbgleichQuelle {

    /** Formation der Meldeliste (TETE/DOUBLETTE/TRIPLETTE/MELEE). */
    Formation getFormation();

    /** Aktuell in der Meldeliste eingetragene Spielernamen (case-insensitiv getrimmt). */
    List<String> getVorhandeneSpielernamen();

    /** Aktuelle Team-Zahlen der Ziel-Meldeliste für den Übernahme-Dialog. */
    MeldelisteStatus getMeldelisteStatus();

    /**
     * Sucht einen Namen (case-insensitiv getrimmt) in der bestehenden Meldeliste.
     *
     * @return 1-basierte Spieler-Nr-Zeile oder {@code -1} wenn nicht vorhanden.
     */
    int findeZeileMitName(String spielerName);

    /**
     * Schreibt einen Block (1, 2 oder 3 Spieler — passend zu {@link #getFormation()})
     * in die nächste freie Meldeliste-Zeile. Falls die Vereinsspalte für das System
     * aktiv ist, wird der Vereinsname in derselben Zeile gesetzt.
     *
     * @return Anzahl tatsächlich geschriebener Zeilen.
     * @throws MeldelisteSchreibException bei Sheet-Fehlern oder voller Liste.
     */
    int schreibeBlock(List<SpielerMitVerein> spieler) throws MeldelisteSchreibException;

    /**
     * Schreibt einen Block und liefert die konkrete, 1-basierte Sheet-Zeile. Der Rückgabewert ist
     * für stabile externe Zuordnungen gedacht; Namen sind bei Mehrfachmeldungen nicht eindeutig.
     *
     * @param teilnahme Anfangswert der Aktiv-Spalte der neuen Zeile.
     * @return 1-basierte Sheet-Zeile, {@code 0} bei leerer Spielerliste.
     */
    int schreibeBlockUndLiefereZeile(List<SpielerMitVerein> spieler, NeueMeldungTeilnahme teilnahme)
            throws MeldelisteSchreibException;

    /**
     * {@code true}, wenn das Ziel das Mêlée-Anmeldung-Sheet ist: jede Zeile ein Einzelspieler, die
     * Teams entstehen erst lokal beim „Mêlée übernehmen“ und haben keine Team-Nr im Sinne der Runden.
     */
    default boolean istMeleeAnmeldung() {
        return false;
    }

    /**
     * Wert der Aktiv-Spalte einer konkreten, 1-basierten Sheet-Zeile (1 = nimmt teil, 2 = ausgestiegen),
     * {@code -1} wenn leer oder nicht lesbar.
     */
    default int getAktivWertAusZeile(int zeile1Basiert) {
        return -1;
    }

    /** Liefert die lokale Team-/Spieler-Nr einer konkreten, 1-basierten Sheet-Zeile. */
    default int getTeamNrAusZeile(int zeile1Basiert) {
        return -1;
    }

    /**
     * Liefert die dauerhafte lokale Kennung der Meldelistenzeile und legt sie bei Bedarf an.
     * Diese Kennung ist unabhängig von Teamnummer, Namen und Sortierung.
     */
    default String getOderErzeugeLokaleUuid(int zeile1Basiert) throws MeldelisteSchreibException {
        throw new MeldelisteSchreibException("Lokale PTM-Online-ID wird von dieser Meldeliste nicht unterstützt");
    }

    /** Setzt eine zuvor gesicherte lokale UUID wieder auf die zugehörige Meldelistenzeile. */
    default void setzeLokaleUuid(int zeile1Basiert, String uuid) throws MeldelisteSchreibException {
        throw new MeldelisteSchreibException("Lokale PTM-Online-ID wird von dieser Meldeliste nicht unterstützt");
    }

    /** Markiert eine Meldung als abgemeldet, ohne die Spieler- oder Teamdaten zu löschen. */
    default void markiereAlsAbgemeldet(int zeile1Basiert) throws MeldelisteSchreibException {
        throw new MeldelisteSchreibException("Abmelden wird von dieser Meldeliste nicht unterstützt");
    }

    /**
     * Hebt eine Abmeldung wieder auf: steht die Meldung auf „abgemeldet“, wird die Aktiv-Spalte
     * geleert (inaktiv, noch nicht eingecheckt). Andere Teilnahme-Werte bleiben unverändert.
     */
    default void hebeAbmeldungAuf(int zeile1Basiert) throws MeldelisteSchreibException {
        throw new MeldelisteSchreibException("Abmelden wird von dieser Meldeliste nicht unterstützt");
    }

    /** Formel, die aus der lokalen UUID stets die aktuell angezeigte Team-/Spielernummer ermittelt. */
    default String formelTeamNrAusLokalerUuid(String uuid) throws MeldelisteSchreibException {
        throw new MeldelisteSchreibException("Lokale PTM-Online-ID wird von dieser Meldeliste nicht unterstützt");
    }

    /** Anfangswert der Aktiv-Spalte für eine neu geschriebene Meldung. */
    enum NeueMeldungTeilnahme {
        /** Aktiv-Spalte = 1: nimmt teil (z.B. Übernahme aus der Spieler-DB vor Ort). */
        AKTIV,
        /** Aktiv-Spalte bleibt leer: noch nicht eingecheckt (z.B. Import aus PTM-Online). */
        INAKTIV
    }

    /**
     * Team-Zähler der Meldeliste.
     *
     * @param angemeldet belegte Teams ohne Checkin-Haken
     * @param checkin belegte Teams mit Checkin-Haken
     * @param gesamt Summe aller belegten Teams
     */
    record MeldelisteStatus(int angemeldet, int checkin, int gesamt) {
    }

    /** Fehler beim Block-Schreiben in die Meldeliste. */
    final class MeldelisteSchreibException extends Exception {
        private static final long serialVersionUID = 1L;
        public MeldelisteSchreibException(String message) {
            super(message);
        }
        public MeldelisteSchreibException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
