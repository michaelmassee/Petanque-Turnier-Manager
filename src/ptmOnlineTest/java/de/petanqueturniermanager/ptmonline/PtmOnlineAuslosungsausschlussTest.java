/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.formulex.meldeliste.FormuleXMeldeListeSheetNew;
import de.petanqueturniermanager.formulex.spielrunde.FormuleXSpielrundeSheetNaechste;
import de.petanqueturniermanager.helper.random.RandomSource;
import de.petanqueturniermanager.jedergegenjeden.meldeliste.JGJMeldeListeSheet_New;
import de.petanqueturniermanager.jedergegenjeden.spielplan.JGJSpielPlanSheet;
import de.petanqueturniermanager.kaskade.meldeliste.KaskadeMeldeListeSheetNew;
import de.petanqueturniermanager.kaskade.spielrunde.KaskadeSpielrundeSheet;
import de.petanqueturniermanager.ko.KoTurnierbaumSheet;
import de.petanqueturniermanager.ko.konfiguration.KoKonfigurationSheet;
import de.petanqueturniermanager.ko.meldeliste.KoMeldeListeSheetNew;
import de.petanqueturniermanager.onlinesync.TurnierSystemOnlineTypMapping;
import de.petanqueturniermanager.poule.meldeliste.PouleMeldeListeSheetNew;
import de.petanqueturniermanager.poule.vorrunde.PouleVorrundeSheet;
import de.petanqueturniermanager.ptmonline.PtmOnlineWebApi.Person;
import de.petanqueturniermanager.schweizer.konfiguration.SpielplanTeamAnzeige;
import de.petanqueturniermanager.spielerdb.MeldelisteZielFactory;

/**
 * Ausschluss gesperrter Meldungen von der Auslosung (P-30, P-42) in den übrigen Turniersystemen mit Team-Meldeliste,
 * gegen den lokal gestarteten Worker: Ein unvollständiges Triplette-Team bleibt eingecheckt, wird aber nicht
 * ausgelost. Geprüft am Ergebnis, das auch die Spieler sehen – den online übertragenen Paarungen.
 * <p>
 * Schweizer prüft {@link PtmOnlineSchweizerZusammenspielTest}; Supermêlée und Mêlée-Anmeldung haben keine
 * unvollständigen Teams.
 */
class PtmOnlineAuslosungsausschlussTest extends BasePtmOnlineZusammenspielTest {

    private static final LocalDate IN_EINEM_MONAT = LocalDate.now().plusMonths(1);
    private static final Person[] TEAM_ZU_ZWEIT = { Person.gast("Dora", "Dahl"), Person.gast("Dirk", "Decker") };

    /**
     * Ein Turniersystem: Meldeliste anlegen (Triplette), die erste Auslosung wie über das Menü und die Zahl vollständiger
     * Teams, bei der alle in der ersten übertragenen Runde einen Gegner haben.
     */
    record SystemAufbau(TurnierSystem system, Aktion meldelisteAnlegen, Auslosung auslosung, int anzahlTeams) {
        @Override
        public String toString() {
            return system.name();
        }
    }

    @FunctionalInterface
    interface Aktion {
        void ausfuehren(WorkingSpreadsheet ws) throws Exception;
    }

    @FunctionalInterface
    interface Auslosung {
        SheetRunner runner(WorkingSpreadsheet ws);
    }

    static Stream<SystemAufbau> systeme() {
        return Stream.of(
                new SystemAufbau(TurnierSystem.FORMULEX,
                        ws -> new FormuleXMeldeListeSheetNew(ws).createMeldelisteWithParams(Formation.TRIPLETTE, false,
                                false, 4),
                        FormuleXSpielrundeSheetNaechste::new, 7),
                new SystemAufbau(TurnierSystem.KO, ws -> {
                    KoKonfigurationSheet konfiguration = new KoKonfigurationSheet(ws);
                    konfiguration.update();
                    konfiguration.setMeldeListeFormation(Formation.TRIPLETTE);
                    new KoMeldeListeSheetNew(ws).createMeldelisteWithParams();
                    // 8 Teams: ohne Cadrage hat jedes Team in der ersten Runde einen Gegner. Partien mit noch offenem
                    // Gegner überträgt PTM nicht.
                }, KoTurnierbaumSheet::new, 8),
                new SystemAufbau(TurnierSystem.JGJ,
                        ws -> new JGJMeldeListeSheet_New(ws).createMeldelisteWithParams(Formation.TRIPLETTE, false,
                                false, SpielplanTeamAnzeige.NR),
                        JGJSpielPlanSheet::new, 7),
                new SystemAufbau(TurnierSystem.POULE,
                        ws -> new PouleMeldeListeSheetNew(ws).createMeldelisteWithParams(Formation.TRIPLETTE, false,
                                false),
                        PouleVorrundeSheet::new, 7),
                new SystemAufbau(TurnierSystem.KASKADE,
                        ws -> new KaskadeMeldeListeSheetNew(ws).createMeldelisteWithParams(Formation.TRIPLETTE, false,
                                false, 2),
                        KaskadeSpielrundeSheet::new, 7));
    }

    @BeforeEach
    void zufallFestlegen() {
        RandomSource.setSeed(42L);
    }

    @AfterEach
    void zufallZuruecksetzen() {
        RandomSource.reset();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("systeme")
    void unvollstaendigesTeamWirdNichtAusgelostUndDasTurnierStartetOnline(SystemAufbau aufbau) throws Exception {
        anlegen(aufbau);
        Map<String, Person[]> vollstaendig = new LinkedHashMap<>();
        for (int i = 1; i <= aufbau.anzahlTeams(); i++) {
            Person[] team = { Person.gast("Voll" + i, "Erster"), Person.gast("Voll" + i, "Zweiter"),
                    Person.gast("Voll" + i, "Dritter") };
            vollstaendig.put(online.anmelden(turnierId, team), team);
        }
        String idZuZweit = online.anmelden(turnierId, TEAM_ZU_ZWEIT);
        verbinden();
        abgleichen();
        assertThat(lokaleMeldungen()).as("alle Online-Anmeldungen übernommen").hasSize(aufbau.anzahlTeams() + 1);
        for (int zeile : lokaleMeldungen().keySet()) {
            ziel.stelleAktivWertWiederHer(zeile, 1);
        }

        aufbau.auslosung().runner(wkingSpreadsheet).run();
        // Im Betrieb meldet LibreOffice die Änderungen der Auslosung; der Beobachter überträgt sie.
        PtmOnlineLiveBeobachter.anstossen(wkingSpreadsheet.getWorkingSpreadsheetDocument());

        warteBis("Turnier online gestartet",
                () -> "running".equals(online.turnier(turnierId).get("status").getAsString()));
        warteBis("Auslosung online: alle vollständigen Teams, das unvollständige nicht (P-42)",
                () -> ausgelostOnline().equals(vollstaendig.keySet()));
        assertThat(ausgelostOnline()).doesNotContain(idZuZweit);
    }

    private void anlegen(SystemAufbau aufbau) throws Exception {
        system = aufbau.system();
        spieltagNr = null;
        anmeldeTyp = "forme";
        onlineTyp = TurnierSystemOnlineTypMapping.onlineTyp(system).orElseThrow();
        aufbau.meldelisteAnlegen().ausfuehren(wkingSpreadsheet);
        docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM, system.getId());
        ziel = MeldelisteZielFactory.fuerPtmOnline(wkingSpreadsheet).orElseThrow();
        assertThat(ziel.getFormation()).isEqualTo(Formation.TRIPLETTE);
        turnierId = online.turnierAnlegen("E2E " + system.name(), onlineTyp, anmeldeTyp, "triplette", IN_EINEM_MONAT);
    }

    /** Online-IDs aller Teams, die in einer übertragenen Runde vorkommen. */
    private Set<String> ausgelostOnline() throws Exception {
        Set<String> ids = new HashSet<>();
        for (JsonObject runde : online.runden(turnierId)) {
            for (JsonElement spiel : runde.getAsJsonArray("matches")) {
                for (String seite : List.of("teamA", "teamB")) {
                    spiel.getAsJsonObject().getAsJsonArray(seite)
                            .forEach(team -> ids.add(team.getAsJsonObject().get("id").getAsString()));
                }
            }
        }
        return ids;
    }
}
