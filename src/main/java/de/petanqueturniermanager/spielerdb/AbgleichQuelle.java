package de.petanqueturniermanager.spielerdb;

import java.util.List;

/**
 * Strikt nur lesende Quelle für den Spieler-DB-Abgleich. Liefert die in einem
 * Sheet (Meldeliste, Vorlage, …) eingetragenen Spieler als Rohdaten.
 *
 * <p>Diese Schnittstelle MUSS read-only bleiben. Schreibverhalten gehört in
 * spezialisierte Sub-Interfaces wie {@link MeldelisteZiel} – sonst entsteht
 * wieder ein God-Interface.
 */
public interface AbgleichQuelle {

    /**
     * Anzeigename der Quelle (z.B. „Supermelee", „Schweizer", „Vorlage").
     * Wird in Dialog-Texten / Status-Zeilen verwendet.
     */
    String getSystemBezeichnung();

    /**
     * Liest alle eingetragenen Spieler als Rohdaten (Vorname, Nachname, optional
     * Vereinsname pro Slot). Leere Slots werden übersprungen. Wird vom
     * Abgleich-Dialog genutzt, um fehlende Spieler in die Spieler-DB zu
     * übernehmen.
     */
    List<MeldelisteSpielerDaten> leseAlleSpielerRoh();
}
