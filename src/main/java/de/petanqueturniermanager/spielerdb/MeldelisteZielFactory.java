package de.petanqueturniermanager.spielerdb;

import java.util.Optional;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Erkennt anhand des Dokumenten-Turniersystems die richtige Formation und
 * baut einen {@link MeldelisteZiel}-Adapter, der auf das Standard-Meldeliste-Sheet
 * schreibt. Liefert {@link Optional#empty()} wenn das Dokument kein bekanntes
 * Turniersystem hat oder kein Meldeliste-Sheet existiert.
 */
public final class MeldelisteZielFactory {

    private MeldelisteZielFactory() {}

    public static Optional<MeldelisteZiel> fuerAktivesSheet(WorkingSpreadsheet ws) {
        TurnierSystem ts = new DocumentPropertiesHelper(ws).getTurnierSystemAusDocument();
        if (ts == null || ts == TurnierSystem.KEIN) {
            return Optional.empty();
        }
        Formation formation = formationFuer(ts, ws);
        return SheetMeldelisteAdapter.fuer(ws, SheetNamen.meldeliste(), formation);
    }

    /** Property-Key, unter dem alle Systeme die Formation im Konfig-Sheet ablegen. */
    private static final String KONFIG_PROP_FORMATION = "Meldeliste Formation";

    /**
     * Liest die Formation aus dem Konfig-Sheet (gemeinsamer Property-Key über
     * alle Turniersysteme). Supermelee hat keine konfigurierbare Formation —
     * dort gilt immer {@link Formation#MELEE}. Bei Lese-/Parse-Fehlern wird
     * defensiv {@link Formation#TETE} angenommen (1 Spieler pro Übernahme,
     * kein Block-Sammelpanel).
     */
    private static Formation formationFuer(TurnierSystem ts, WorkingSpreadsheet ws) {
        if (ts == TurnierSystem.SUPERMELEE) {
            return Formation.MELEE;
        }
        String roh = new DocumentPropertiesHelper(ws).getStringProperty(KONFIG_PROP_FORMATION, "");
        if (roh.isEmpty()) {
            return Formation.TETE;
        }
        try {
            return Formation.valueOf(roh);
        } catch (IllegalArgumentException e) {
            return Formation.TETE;
        }
    }
}
