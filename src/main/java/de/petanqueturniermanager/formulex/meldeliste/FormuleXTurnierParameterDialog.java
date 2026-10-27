/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.formulex.meldeliste;

import java.util.Optional;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.MeleeAnmeldungModus;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierStartDialog;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.i18n.I18n;

/**
 * Modaler Dialog zur Abfrage der Turnier-Parameter vor dem Anlegen einer neuen
 * Formule X Meldeliste.
 * <p>
 * Abschnitt Meldeliste: Formation, Zusatzspalten „Teamname“ und „Verein“, Mêlée-Anmeldung.
 */
class FormuleXTurnierParameterDialog {

    record TurnierParameter(
            Formation formation,
            boolean teamnameAnzeigen,
            boolean vereinsnameAnzeigen,
            Optional<MeleeAnmeldungModus> meleeAnmeldung) {
    }

    private final WorkingSpreadsheet workingSpreadsheet;

    private FormuleXTurnierParameterDialog(WorkingSpreadsheet workingSpreadsheet) {
        this.workingSpreadsheet = workingSpreadsheet;
    }

    static FormuleXTurnierParameterDialog from(WorkingSpreadsheet workingSpreadsheet) {
        return new FormuleXTurnierParameterDialog(workingSpreadsheet);
    }

    Optional<TurnierParameter> anzeigen(Formation standardFormation, boolean standardTeamnameAnzeigen,
            boolean standardVereinsnameAnzeigen) throws com.sun.star.uno.Exception {
        TurnierStartDialog dialog = TurnierStartDialog.erstellen(workingSpreadsheet.getxContext(),
                I18n.get("dialog.formulex.turnier.parameter.titel"));
        dialog.meldeliste(standardFormation, standardTeamnameAnzeigen, standardVereinsnameAnzeigen);
        return dialog.anzeigen(d -> new TurnierParameter(d.formation(), d.teamname(), d.vereinsname(),
                d.meleeAnmeldung()));
    }
}
