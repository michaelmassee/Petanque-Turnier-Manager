/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.formulex.konfiguration.FormuleXKonfigurationSheet;
import de.petanqueturniermanager.jedergegenjeden.konfiguration.JGJKonfigurationSheet;
import de.petanqueturniermanager.kaskade.konfiguration.KaskadeKonfigurationSheet;
import de.petanqueturniermanager.ko.konfiguration.KoKonfigurationSheet;
import de.petanqueturniermanager.maastrichter.konfiguration.MaastrichterKonfigurationSheet;
import de.petanqueturniermanager.poule.konfiguration.PouleKonfigurationSheet;
import de.petanqueturniermanager.ptmonline.auftrag.AuftragsBestand;
import de.petanqueturniermanager.ptmonline.dto.SpieleinstellungenDto;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerKonfigurationSheet;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerRankingModus;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeKonfigurationSheet;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeMode;

/**
 * Überträgt die Spieleinstellungen des Turnierdokuments nach PTM-Online (Formation, Schweizer Ranglistenmodus,
 * Formule-X-Rundenzahl, Spiel um Platz 3). Ein verbundenes Dokument ist Master: Online gelten danach dieselben Werte,
 * die Live-Ansicht zeigt die Spalten passend zum Dokument.
 * <p>
 * Erfasst wird nur bei einer Änderung gegenüber dem zuletzt erfassten Stand; nach dem Öffnen oder Verbinden wird der
 * Stand einmal vollständig übertragen.
 */
final class PtmOnlineSpieleinstellungen {

    private static final Map<String, SpieleinstellungenDto> GEDAECHTNIS = new ConcurrentHashMap<>();

    private PtmOnlineSpieleinstellungen() {}

    /** Liest die Spieleinstellungen des Dokuments; leer bei Systemen ohne Online-Gegenstück. */
    static SpieleinstellungenDto lese(WorkingSpreadsheet ws, TurnierSystem turnierSystem) {
        return switch (turnierSystem) {
            case SCHWEIZER -> {
                SchweizerKonfigurationSheet konfiguration = new SchweizerKonfigurationSheet(ws);
                yield new SpieleinstellungenDto(onlineFormation(konfiguration.getMeldeListeFormation()),
                        onlineRankingModus(konfiguration.getRankingModus()), null, null);
            }
            case MAASTRICHTER -> {
                MaastrichterKonfigurationSheet konfiguration = new MaastrichterKonfigurationSheet(ws);
                yield new SpieleinstellungenDto(onlineFormation(konfiguration.getMeldeListeFormation()),
                        onlineRankingModus(konfiguration.getRankingModus()), null, null);
            }
            case FORMULEX -> {
                FormuleXKonfigurationSheet konfiguration = new FormuleXKonfigurationSheet(ws);
                yield new SpieleinstellungenDto(onlineFormation(konfiguration.getMeldeListeFormation()), null,
                        konfiguration.getAnzahlRunden(), null);
            }
            case KO -> {
                KoKonfigurationSheet konfiguration = new KoKonfigurationSheet(ws);
                yield new SpieleinstellungenDto(onlineFormation(konfiguration.getMeldeListeFormation()), null, null,
                        konfiguration.isSpielbaumSpielUmPlatz3());
            }
            case JGJ -> nurFormation(new JGJKonfigurationSheet(ws).getMeldeListeFormation());
            case POULE -> nurFormation(new PouleKonfigurationSheet(ws).getMeldeListeFormation());
            case KASKADE -> nurFormation(new KaskadeKonfigurationSheet(ws).getMeldeListeFormation());
            case SUPERMELEE -> new SpieleinstellungenDto(
                    supermeleeFormation(new SuperMeleeKonfigurationSheet(ws).getSuperMeleeMode()), null, null, null);
            case TRIPTETE -> nurFormation(Formation.TRIPLETTE);
            case LIGA, KEIN -> SpieleinstellungenDto.KEINE;
        };
    }

    /**
     * Erfasst einen Auftrag, wenn sich die Einstellungen seit dem zuletzt erfassten Stand geändert haben.
     *
     * @return {@code true}, wenn ein Auftrag erfasst wurde
     */
    static boolean erfasse(AuftragsBestand bestand, String tournamentId, SpieleinstellungenDto einstellungen) {
        if (einstellungen.istLeer() || einstellungen.equals(GEDAECHTNIS.get(tournamentId))) {
            return false;
        }
        PtmOnlineAuftraege.spieleinstellungen(bestand, tournamentId, einstellungen);
        GEDAECHTNIS.put(tournamentId, einstellungen);
        return true;
    }

    /** Die nächste Erfassung überträgt die Einstellungen erneut (manueller Abgleich, neue Bindung). */
    static void vergessen(String tournamentId) {
        GEDAECHTNIS.remove(tournamentId);
    }

    static @Nullable String onlineFormation(Formation formation) {
        return switch (formation) {
            case TETE, DOUBLETTE, TRIPLETTE -> formation.name().toLowerCase(Locale.ROOT);
            // Ohne feste Teamgröße gibt es online keine passende Formation.
            case MELEE, NUR_TEAMNAME -> null;
        };
    }

    static String supermeleeFormation(SuperMeleeMode modus) {
        return switch (modus) {
            case Triplette -> onlineFormation(Formation.TRIPLETTE);
            case Doublette -> onlineFormation(Formation.DOUBLETTE);
        };
    }

    static String onlineRankingModus(SchweizerRankingModus modus) {
        return modus.name().toLowerCase(Locale.ROOT);
    }

    private static SpieleinstellungenDto nurFormation(Formation formation) {
        return new SpieleinstellungenDto(onlineFormation(formation), null, null, null);
    }
}
