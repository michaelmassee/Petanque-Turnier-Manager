/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.TeamnameModus;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.formulex.konfiguration.FormuleXKonfigurationSheet;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.ko.konfiguration.KoKonfigurationSheet;
import de.petanqueturniermanager.maastrichter.konfiguration.MaastrichterKonfigurationSheet;
import de.petanqueturniermanager.onlinesync.OnlineTournamentDto;
import de.petanqueturniermanager.ptmonline.auftrag.AuftragsBestand;
import de.petanqueturniermanager.ptmonline.dto.SpieleinstellungenDto;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerKonfigurationSheet;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerRankingModus;

/**
 * Überträgt die Spieleinstellungen des Turnierdokuments nach PTM-Online (Formation, Schweizer Ranglistenmodus,
 * Spiel um Platz 3, Live-Ansicht, Ergebnismeldung durch Spieler). Ein verbundenes Dokument ist Master: Online gelten danach dieselben Werte,
 * die Live-Ansicht zeigt die Spalten passend zum Dokument.
 * <p>
 * Erfasst wird nur bei einer Änderung gegenüber dem zuletzt erfassten Stand; nach dem Öffnen oder Verbinden wird der
 * Stand einmal vollständig übertragen.
 */
final class PtmOnlineSpieleinstellungen {

    private static final Map<String, SpieleinstellungenDto> GEDAECHTNIS = new ConcurrentHashMap<>();

    private PtmOnlineSpieleinstellungen() {}

    /**
     * Liest die Spieleinstellungen des Dokuments; leer bei Systemen ohne Online-Gegenstück. Dazu Live-Ansicht und
     * Ergebnismeldung durch Spieler (Default jeweils an; Trip-Tête kennt keine Ergebnismeldung) und der Teamname-Modus
     * ({@link TeamnameModus}) – in allen Systemen gleich, das Dokument ist Master.
     */
    static SpieleinstellungenDto lese(WorkingSpreadsheet ws, TurnierSystem turnierSystem) {
        if (!hatOnlineGegenstueck(turnierSystem)) {
            return SpieleinstellungenDto.KEINE;
        }
        SpieleinstellungenDto einstellungen = leseSystem(ws, turnierSystem).mitTeamname(PtmOnlineFormation
                .teamnameModusAusDokument(ws, turnierSystem).map(TeamnameModus::teamnameSpalte).orElse(null));
        return DocumentPropertiesHelper.nurLesend(() -> {
            DocumentPropertiesHelper properties = new DocumentPropertiesHelper(ws);
            return einstellungen.mitLive(
                    properties.getBooleanProperty(BasePropertiesSpalte.KONFIG_PROP_PTMONLINE_LIVE_ANSICHT, true),
                    turnierSystem == TurnierSystem.TRIPTETE ? null
                            : properties.getBooleanProperty(BasePropertiesSpalte.KONFIG_PROP_PTMONLINE_ERGEBNISMELDUNG,
                                    true));
        });
    }

    static boolean hatOnlineGegenstueck(TurnierSystem turnierSystem) {
        return turnierSystem != TurnierSystem.LIGA && turnierSystem != TurnierSystem.KEIN;
    }

    private static SpieleinstellungenDto leseSystem(WorkingSpreadsheet ws, TurnierSystem turnierSystem) {
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
                yield nurFormation(konfiguration.getMeldeListeFormation());
            }
            case KO -> {
                KoKonfigurationSheet konfiguration = new KoKonfigurationSheet(ws);
                yield new SpieleinstellungenDto(onlineFormation(konfiguration.getMeldeListeFormation()), null, null,
                        konfiguration.isSpielbaumSpielUmPlatz3());
            }
            case JGJ, POULE, KASKADE, SUPERMELEE, TRIPTETE ->
                nurFormation(PtmOnlineFormation.ausDokument(ws, turnierSystem).orElseThrow());
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
            case NUR_TEAMNAME -> OnlineTournamentDto.FORMATION_ANDERE;
            // Supermêlée-Spieler ohne feste Teamgröße: online bleibt die Formation des Spielrunden-Modus.
            case MELEE -> null;
        };
    }

    static String onlineRankingModus(SchweizerRankingModus modus) {
        return modus.name().toLowerCase(Locale.ROOT);
    }

    private static SpieleinstellungenDto nurFormation(Formation formation) {
        return new SpieleinstellungenDto(onlineFormation(formation), null, null, null);
    }
}
