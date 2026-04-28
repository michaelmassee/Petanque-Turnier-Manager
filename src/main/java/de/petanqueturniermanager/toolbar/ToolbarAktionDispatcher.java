/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.toolbar;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Eintrittspunkt für die drei turniersystem-übergreifenden Toolbar-Aktionen.
 * Ermittelt das aktive Turniersystem aus den Dokumenteigenschaften und
 * delegiert an die passende {@link ITurnierSystemToolbarStrategie}.
 */
public final class ToolbarAktionDispatcher {

    private ToolbarAktionDispatcher() {
    }

    /**
     * Startet die nächste Spielrunde des aktiven Turniersystems.
     */
    public static void weiter(WorkingSpreadsheet ws) throws Exception {
        TurnierSystem system = new DocumentPropertiesHelper(ws).getTurnierSystemAusDocument();
        TurnierSystemToolbarStrategieRegistry.get(system).weiter(ws);
    }

    /**
     * Erstellt die Vorrunden-Rangliste des aktiven Turniersystems.
     */
    public static void vorrundenRangliste(WorkingSpreadsheet ws) throws Exception {
        TurnierSystem system = new DocumentPropertiesHelper(ws).getTurnierSystemAusDocument();
        TurnierSystemToolbarStrategieRegistry.get(system).vorrundenRangliste(ws);
    }

    /**
     * Erstellt das Teilnehmer-Sheet des aktiven Turniersystems.
     */
    public static void teilnehmer(WorkingSpreadsheet ws) throws Exception {
        TurnierSystem system = new DocumentPropertiesHelper(ws).getTurnierSystemAusDocument();
        TurnierSystemToolbarStrategieRegistry.get(system).teilnehmer(ws);
    }

    /**
     * Löst die aktuelle Spielrunde des aktiven Turniersystems neu aus.
     */
    public static void neuAuslosen(WorkingSpreadsheet ws) throws Exception {
        TurnierSystem system = new DocumentPropertiesHelper(ws).getTurnierSystemAusDocument();
        TurnierSystemToolbarStrategieRegistry.get(system).neuAuslosen(ws);
    }

    /**
     * Startet die Abschlussphase des aktiven Turniersystems.
     */
    public static void abschluss(WorkingSpreadsheet ws) throws Exception {
        TurnierSystem system = new DocumentPropertiesHelper(ws).getTurnierSystemAusDocument();
        TurnierSystemToolbarStrategieRegistry.get(system).abschluss(ws);
    }

    /**
     * Wechselt zum nächsten Spieltag des aktiven Turniersystems.
     */
    public static void naechsterSpieltag(WorkingSpreadsheet ws) throws Exception {
        TurnierSystem system = new DocumentPropertiesHelper(ws).getTurnierSystemAusDocument();
        TurnierSystemToolbarStrategieRegistry.get(system).naechsterSpieltag(ws);
    }

    /**
     * Erstellt die Gesamtrangliste des aktiven Turniersystems.
     */
    public static void gesamtrangliste(WorkingSpreadsheet ws) throws Exception {
        TurnierSystem system = new DocumentPropertiesHelper(ws).getTurnierSystemAusDocument();
        TurnierSystemToolbarStrategieRegistry.get(system).gesamtrangliste(ws);
    }
}
