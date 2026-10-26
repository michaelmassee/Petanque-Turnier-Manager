/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.poule.meldeliste;

import java.util.Optional;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.MeleeAnmeldungModus;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierStartDialog;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.i18n.I18n;

/**
 * Modaler Dialog zur Abfrage der Turnier-Parameter vor dem Anlegen einer neuen
 * Poule-A/B-Meldeliste.
 * <p>
 * Abschnitt Meldeliste: Formation, Zusatzspalten „Teamname“ und „Verein“, Mêlée-Anmeldung.
 * <p>
 * TODO: Zukünftige Erweiterungen: Anzahl Gruppen, Setzlogik
 */
class PouleTurnierParameterDialog {

    /**
     * Ergebnis des Dialogs.
     * TODO: Erweiterung um gruppenAnzahl und seedingModus wenn Spielrunden implementiert werden
     */
    record TurnierParameter(
            Formation formation,
            boolean teamnameAnzeigen,
            boolean vereinsnameAnzeigen,
            Optional<MeleeAnmeldungModus> meleeAnmeldung) {
    }

    private final WorkingSpreadsheet workingSpreadsheet;

    private PouleTurnierParameterDialog(WorkingSpreadsheet workingSpreadsheet) {
        this.workingSpreadsheet = workingSpreadsheet;
    }

    static PouleTurnierParameterDialog from(WorkingSpreadsheet workingSpreadsheet) {
        return new PouleTurnierParameterDialog(workingSpreadsheet);
    }

    /**
     * Zeigt den Dialog an und gibt das Ergebnis zurück.
     *
     * @param standardFormation           vorausgewählte Formation
     * @param standardTeamnameAnzeigen    vorausgewählter Teamname-Status
     * @param standardVereinsnameAnzeigen vorausgewählter Vereinsname-Status
     * @return Optional mit TurnierParameter bei OK, leer bei Abbrechen
     */
    Optional<TurnierParameter> anzeigen(Formation standardFormation, boolean standardTeamnameAnzeigen,
            boolean standardVereinsnameAnzeigen) throws com.sun.star.uno.Exception {
        TurnierStartDialog dialog = TurnierStartDialog.erstellen(workingSpreadsheet.getxContext(),
                I18n.get("dialog.poule.turnier.parameter.titel"));
        dialog.meldeliste(standardFormation, standardTeamnameAnzeigen, standardVereinsnameAnzeigen);
        return dialog.anzeigen(d -> new TurnierParameter(d.formation(), d.teamname(), d.vereinsname(),
                d.meleeAnmeldung()));
    }
}
