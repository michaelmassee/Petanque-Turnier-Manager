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
public interface MeldelisteZiel {

    /** Formation der Meldeliste (TETE/DOUBLETTE/TRIPLETTE/MELEE). */
    Formation getFormation();

    /** Anzeigename des Turniersystems (z.B. „Supermelee", „Schweizer"). */
    String getSystemBezeichnung();

    /** Aktuell in der Meldeliste eingetragene Spielernamen (case-insensitiv getrimmt). */
    List<String> getVorhandeneSpielernamen();

    /**
     * Liest alle eingetragenen Spieler als Rohdaten (Vorname, Nachname, optional
     * Vereinsname pro Slot) aus der Meldeliste. Leere Slots werden übersprungen.
     * Wird vom Abgleich-Dialog genutzt, um fehlende Spieler in die Spieler-DB
     * zu übernehmen.
     */
    List<MeldelisteSpielerDaten> leseAlleSpielerRoh();

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
