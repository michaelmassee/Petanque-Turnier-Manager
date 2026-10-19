/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.util.Optional;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.formulex.konfiguration.FormuleXKonfigurationSheet;
import de.petanqueturniermanager.jedergegenjeden.konfiguration.JGJKonfigurationSheet;
import de.petanqueturniermanager.kaskade.konfiguration.KaskadeKonfigurationSheet;
import de.petanqueturniermanager.ko.konfiguration.KoKonfigurationSheet;
import de.petanqueturniermanager.maastrichter.konfiguration.MaastrichterKonfigurationSheet;
import de.petanqueturniermanager.onlinesync.OnlineVerbindbarkeit;
import de.petanqueturniermanager.poule.konfiguration.PouleKonfigurationSheet;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerKonfigurationSheet;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeKonfigurationSheet;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeMode;

/**
 * Formation des Turnierdokuments, so wie PTM-Online sie kennt: bei Supermêlée der Spielrunden-Modus, bei Trip-Tête
 * immer Triplette. Die Liga hat keine Formation.
 */
public final class PtmOnlineFormation {

    private PtmOnlineFormation() {}

    /** Formation des Dokuments; leer bei Turniersystemen ohne Formation. */
    public static Optional<Formation> ausDokument(WorkingSpreadsheet ws, TurnierSystem turnierSystem) {
        return Optional.ofNullable(switch (turnierSystem) {
            case SCHWEIZER -> new SchweizerKonfigurationSheet(ws).getMeldeListeFormation();
            case MAASTRICHTER -> new MaastrichterKonfigurationSheet(ws).getMeldeListeFormation();
            case FORMULEX -> new FormuleXKonfigurationSheet(ws).getMeldeListeFormation();
            case KO -> new KoKonfigurationSheet(ws).getMeldeListeFormation();
            case JGJ -> new JGJKonfigurationSheet(ws).getMeldeListeFormation();
            case POULE -> new PouleKonfigurationSheet(ws).getMeldeListeFormation();
            case KASKADE -> new KaskadeKonfigurationSheet(ws).getMeldeListeFormation();
            case SUPERMELEE -> ausSupermeleeModus(new SuperMeleeKonfigurationSheet(ws).getSuperMeleeMode());
            case TRIPTETE -> Formation.TRIPLETTE;
            case LIGA, KEIN -> null;
        });
    }

    /** Vergleichswert der Dokument-Formation für die Verbinden-Auswahl ({@link OnlineVerbindbarkeit}). */
    public static Optional<String> vergleichswertAusDokument(WorkingSpreadsheet ws, TurnierSystem turnierSystem) {
        return ausDokument(ws, turnierSystem).map(OnlineVerbindbarkeit::vergleichswert);
    }

    static Formation ausSupermeleeModus(SuperMeleeMode modus) {
        return switch (modus) {
            case Triplette -> Formation.TRIPLETTE;
            case Doublette -> Formation.DOUBLETTE;
        };
    }
}
