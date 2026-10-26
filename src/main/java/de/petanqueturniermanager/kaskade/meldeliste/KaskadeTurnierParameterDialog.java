/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.kaskade.meldeliste;

import java.util.Optional;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.MeleeAnmeldungModus;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierStartDialog;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.i18n.I18n;

/**
 * Modaler Dialog zur Abfrage der Turnier-Parameter vor dem Anlegen einer neuen
 * Kaskaden-KO-Meldeliste.
 * <p>
 * Abschnitte:
 * <ul>
 *   <li>Meldeliste: Formation, Zusatzspalten „Teamname“ und „Verein“, Mêlée-Anmeldung</li>
 *   <li>Kaskaden: Anzahl 2 (A/B/C/D) oder 3 (A/B/C/D/E/F/G/H)</li>
 * </ul>
 */
class KaskadeTurnierParameterDialog {

    record TurnierParameter(
            Formation formation,
            boolean teamnameAnzeigen,
            boolean vereinsnameAnzeigen,
            Optional<MeleeAnmeldungModus> meleeAnmeldung,
            int anzahlKaskaden) {
    }

    private static final String LST_KASKADEN = "lstKaskaden";
    private static final int[] ANZAHL_KASKADEN = { 2, 3 };

    private final WorkingSpreadsheet workingSpreadsheet;

    private KaskadeTurnierParameterDialog(WorkingSpreadsheet workingSpreadsheet) {
        this.workingSpreadsheet = workingSpreadsheet;
    }

    static KaskadeTurnierParameterDialog from(WorkingSpreadsheet workingSpreadsheet) {
        return new KaskadeTurnierParameterDialog(workingSpreadsheet);
    }

    Optional<TurnierParameter> anzeigen(Formation standardFormation, boolean standardTeamnameAnzeigen,
            boolean standardVereinsnameAnzeigen, int standardAnzahlKaskaden) throws com.sun.star.uno.Exception {
        TurnierStartDialog dialog = TurnierStartDialog.erstellen(workingSpreadsheet.getxContext(),
                I18n.get("dialog.kaskade.turnier.parameter.titel"));
        dialog.meldeliste(standardFormation, standardTeamnameAnzeigen, standardVereinsnameAnzeigen);

        dialog.abschnitt(I18n.get("dialog.gruppe.kaskaden"));
        dialog.auswahl(LST_KASKADEN, I18n.get("dialog.kaskade.label.anzahl.kaskaden"),
                new String[] { I18n.get("dialog.kaskade.label.kaskaden.2"),
                        I18n.get("dialog.kaskade.label.kaskaden.3") },
                standardAnzahlKaskaden == ANZAHL_KASKADEN[1] ? 1 : 0);

        return dialog.anzeigen(d -> new TurnierParameter(d.formation(), d.teamname(), d.vereinsname(),
                d.meleeAnmeldung(), ANZAHL_KASKADEN[d.gewaehlt(LST_KASKADEN) == 1 ? 1 : 0]));
    }
}
