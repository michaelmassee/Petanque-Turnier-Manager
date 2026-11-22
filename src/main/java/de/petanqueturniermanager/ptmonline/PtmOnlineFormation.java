/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.util.Optional;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.IFormationKonfiguration;
import de.petanqueturniermanager.basesheet.meldeliste.TeamnameModus;
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
import de.petanqueturniermanager.triptete.konfiguration.TripTeteKonfigurationSheet;

/**
 * Formation und Teamname-Modus des Turnierdokuments, so wie PTM-Online sie kennt: bei Supermêlée der
 * Spielrunden-Modus, bei Trip-Tête immer Triplette. Die Liga hat keine Formation.
 */
public final class PtmOnlineFormation {

    private PtmOnlineFormation() {}

    /** Formation des Dokuments; leer bei Turniersystemen ohne Formation. */
    public static Optional<Formation> ausDokument(WorkingSpreadsheet ws, TurnierSystem turnierSystem) {
        return switch (turnierSystem) {
            case SUPERMELEE -> Optional.of(ausSupermeleeModus(new SuperMeleeKonfigurationSheet(ws).getSuperMeleeMode()));
            case TRIPTETE -> Optional.of(Formation.TRIPLETTE);
            default -> formationKonfiguration(ws, turnierSystem).map(IFormationKonfiguration::getMeldeListeFormation);
        };
    }

    /**
     * Teamname-Modus des Dokuments, wie er PTM-Online übertragen wird; leer bei Turniersystemen ohne Formation (Liga).
     * Supermêlée meldet Einzelspieler und hat nie Teamnamen.
     */
    public static Optional<TeamnameModus> teamnameModusAusDokument(WorkingSpreadsheet ws,
            TurnierSystem turnierSystem) {
        return switch (turnierSystem) {
            case SUPERMELEE -> Optional.of(TeamnameModus.KEIN);
            case TRIPTETE -> Optional.of(TeamnameModus.von(Formation.TRIPLETTE,
                    new TripTeteKonfigurationSheet(ws).isMeldeListeTeamnameAnzeigen()));
            default -> formationKonfiguration(ws, turnierSystem).map(
                    konfiguration -> TeamnameModus.von(konfiguration.getMeldeListeFormation(),
                            konfiguration.isMeldeListeTeamnameAnzeigen()));
        };
    }

    /** Konfiguration der Systeme mit einstellbarer Formation; leer für alle übrigen. */
    private static Optional<IFormationKonfiguration> formationKonfiguration(WorkingSpreadsheet ws,
            TurnierSystem turnierSystem) {
        return Optional.ofNullable(switch (turnierSystem) {
            case SCHWEIZER -> new SchweizerKonfigurationSheet(ws);
            case MAASTRICHTER -> new MaastrichterKonfigurationSheet(ws);
            case FORMULEX -> new FormuleXKonfigurationSheet(ws);
            case KO -> new KoKonfigurationSheet(ws);
            case JGJ -> new JGJKonfigurationSheet(ws);
            case POULE -> new PouleKonfigurationSheet(ws);
            case KASKADE -> new KaskadeKonfigurationSheet(ws);
            case SUPERMELEE, TRIPTETE, LIGA, KEIN -> null;
        });
    }

    /** Vergleichswert der Dokument-Formation für die Verbinden-Auswahl ({@link OnlineVerbindbarkeit}). */
    public static Optional<String> vergleichswertAusDokument(WorkingSpreadsheet ws, TurnierSystem turnierSystem) {
        return ausDokument(ws, turnierSystem).map(OnlineVerbindbarkeit::vergleichswert);
    }

    public static Formation ausSupermeleeModus(SuperMeleeMode modus) {
        return switch (modus) {
            case Triplette -> Formation.TRIPLETTE;
            case Doublette -> Formation.DOUBLETTE;
        };
    }
}
