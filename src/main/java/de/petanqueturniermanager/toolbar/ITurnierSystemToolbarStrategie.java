/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.toolbar;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;

/**
 * Strategie-Interface für turniersystem-spezifische Toolbar-Aktionen.
 * Jedes Turniersystem implementiert diese Schnittstelle und stellt
 * die passenden Sheet-Operationen für die drei Toolbar-Buttons bereit.
 */
public interface ITurnierSystemToolbarStrategie {

    /**
     * Startet die nächste Spielrunde bzw. Vorrunde des Turniersystems.
     */
    void weiter(WorkingSpreadsheet ws) throws Exception;

    /**
     * Erstellt oder aktualisiert die Vorrunden-Rangliste des Turniersystems.
     */
    void vorrundenRangliste(WorkingSpreadsheet ws) throws Exception;

    /**
     * Erstellt oder aktualisiert das Teilnehmer-Sheet des Turniersystems.
     */
    void teilnehmer(WorkingSpreadsheet ws) throws Exception;

    /**
     * Wechselt zum nächsten Spieltag. Nur relevant für Systeme mit mehreren Spieltagen.
     * Standard-Implementierung wirft {@link UnsupportedOperationException}.
     */
    default void naechsterSpieltag(WorkingSpreadsheet ws) throws Exception {
        throw new UnsupportedOperationException("naechsterSpieltag wird von diesem Turniersystem nicht unterstützt");
    }

    /**
     * Erstellt oder aktualisiert die Gesamtrangliste. Nur relevant für Systeme mit mehreren Spieltagen.
     * Standard-Implementierung wirft {@link UnsupportedOperationException}.
     */
    default void gesamtrangliste(WorkingSpreadsheet ws) throws Exception {
        throw new UnsupportedOperationException("gesamtrangliste wird von diesem Turniersystem nicht unterstützt");
    }
}
