package de.petanqueturniermanager.webserver;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Vergleicht zwei {@link TabelleModel}-Snapshots und liefert ein Diff-Modell.
 * <p>
 * Das zurückgegebene Modell enthält:
 * <ul>
 *   <li>Gitter, Spaltenbreiten und Zeilenhöhen immer aus dem neuen Modell (Struktur kann sich ändern)</li>
 *   <li>Nur die geänderten {@link ZelleModel}-Einträge (record-Gleichheit via {@code equals()})</li>
 * </ul>
 * Wenn {@code altesModell == null} (erstmaliger Aufruf), werden alle Zellen zurückgegeben.
 * Ein leeres Zellen-Map im Ergebnis bedeutet: keine Änderungen, kein Push nötig.
 */
public class DiffEngine {

    /**
     * Berechnet die Differenz zwischen altem und neuem Modell.
     *
     * @param altesModell vorheriger Zustand, oder {@code null} für den ersten Aufruf
     * @param neuesModell aktueller Zustand
     * @return Diff-Modell mit geänderten Zellen; leere Zellen-Map = keine Änderungen
     */
    public TabelleModel diff(TabelleModel altesModell, TabelleModel neuesModell) {
        if (altesModell == null) {
            return neuesModell; // erstes Mal → alles als "init" senden
        }

        Map<String, ZelleModel> alteZellen = altesModell.getZellen();
        Map<String, ZelleModel> neueZellen = neuesModell.getZellen();
        Map<String, ZelleModel> geaendert = new LinkedHashMap<>();

        // Geänderte oder neue Zellen
        for (var eintrag : neueZellen.entrySet()) {
            ZelleModel alte = alteZellen.get(eintrag.getKey());
            if (!eintrag.getValue().equals(alte)) {
                geaendert.put(eintrag.getKey(), eintrag.getValue());
            }
        }

        // Gitter und Dimensionen kommen immer aus dem neuen Modell (Struktur kann sich ändern)
        return new TabelleModel(
                neuesModell.getZeilen(),
                neuesModell.getSpalten(),
                neuesModell.getGitter(),
                geaendert,
                neuesModell.getSpaltenBreiten(),
                neuesModell.getZeilenHoehen(),
                neuesModell.getStartZeile(),
                neuesModell.getStartSpalte());
    }
}
