/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.onlinesync.OnlineTournamentDto;
import de.petanqueturniermanager.onlinesync.OnlineVerbindbarkeit;
import de.petanqueturniermanager.ptmonline.PtmOnlineWebApi.Person;
import de.petanqueturniermanager.ptmonline.ui.PtmOnlineVerbindenTestZugang;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerKonfigurationSheet;
import de.petanqueturniermanager.spielerdb.MeldelisteZielFactory;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeKonfigurationSheet;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeMode;

/**
 * Verbinden aus einem leeren Dokument (E-08b): Das Dokument hat noch kein Turniersystem. Turniersystem, Formation und
 * Anmeldeart kommen aus dem Online-Turnier, die Meldeliste entsteht ohne Start-Dialog, und die Online-Meldungen werden
 * gleich übernommen.
 */
class PtmOnlineLeeresDokumentZusammenspielTest extends BasePtmOnlineZusammenspielTest {

    private static final LocalDate IN_EINEM_MONAT = LocalDate.now().plusMonths(1);

    private static final Person[] TEAM_A = { Person.gast("Leon", "Leer"), Person.gast("Lisa", "Lehn") };
    private static final Person[] TEAM_B = { Person.gast("Malte", "Mohn"), Person.gast("Mara", "Mai") };
    private static final Person SPIELER_1 = Person.gast("Nils", "Neu");
    private static final Person SPIELER_2 = Person.gast("Nora", "Nolte");

    @Test
    void schweizerDoubletteWirdAngelegtVerbundenUndUebernimmtDieMeldungen() throws Exception {
        assertThat(docPropHelper.getTurnierSystemAusDocument()).as("leeres Dokument").isEqualTo(TurnierSystem.KEIN);
        turnierId = online.turnierAnlegen("E2E leeres Dokument Schweizer", "schweizer", "forme", "doublette",
                IN_EINEM_MONAT);
        String idA = online.anmelden(turnierId, TEAM_A);
        String idB = online.anmelden(turnierId, TEAM_B);
        OnlineTournamentDto turnier = turnierAusListe();
        assertThat(OnlineVerbindbarkeit.fuerLeeresDokument().bewerte(List.of(turnier)))
                .as("freies Turnier ist wählbar").singleElement()
                .satisfies(bewertung -> assertThat(bewertung.istVerbindbar()).isTrue());

        verbindenUndAbgleichAbwarten(turnier, TurnierSystem.SCHWEIZER);

        assertThat(new SchweizerKonfigurationSheet(wkingSpreadsheet).getMeldeListeFormation())
                .as("Formation aus dem Online-Turnier").isEqualTo(Formation.DOUBLETTE);
        assertThat(lokaleMeldungen().values()).as("Online-Anmeldungen ohne weiteren Klick übernommen")
                .containsExactlyInAnyOrder(namen(TEAM_A), namen(TEAM_B));
        assertThat(mapping.istBereitsImportiert(idA)).isTrue();
        assertThat(mapping.istBereitsImportiert(idB)).isTrue();
        assertThat(OnlineVerbindbarkeit.fuerLeeresDokument().bewerte(List.of(turnierAusListe())).getFirst()
                .hinderungsgrund())
                .as("jetzt gebunden: für ein weiteres leeres Dokument nicht mehr wählbar")
                .contains(I18n.get("ptmonline.turnier.verbinden.grund.bereits_verbunden"));
    }

    @Test
    void supermeleeWirdMitSpieltagEinsAngelegtUndVerbunden() throws Exception {
        turnierId = online.turnierAnlegen("E2E leeres Dokument Supermêlée", "rangliste", "supermelee", "triplette",
                IN_EINEM_MONAT);
        String id1 = online.anmelden(turnierId, SPIELER_1);
        String id2 = online.anmelden(turnierId, SPIELER_2);

        verbindenUndAbgleichAbwarten(turnierAusListe(), TurnierSystem.SUPERMELEE);

        SuperMeleeKonfigurationSheet konfiguration = new SuperMeleeKonfigurationSheet(wkingSpreadsheet);
        assertThat(konfiguration.getSuperMeleeMode()).isEqualTo(SuperMeleeMode.Triplette);
        assertThat(spieltagNr).as("verbunden ist Spieltag 1").isEqualTo(1);
        assertThat(lokaleMeldungen().values()).containsExactlyInAnyOrder(namen(SPIELER_1), namen(SPIELER_2));
        assertThat(mapping.istBereitsImportiert(id1)).isTrue();
        assertThat(mapping.istBereitsImportiert(id2)).isTrue();
    }

    /** Wie im Menü nach der Turnierauswahl; danach gilt das Dokument als normales verbundenes Turnierdokument. */
    private void verbindenUndAbgleichAbwarten(OnlineTournamentDto turnier, TurnierSystem erwartet) throws Exception {
        warteBisKeineVerarbeitungLaeuft();
        assertThat(PtmOnlineVerbindenTestZugang.verbindenLeeresDokument(wkingSpreadsheet, server.zugangsdaten(),
                turnier)).as("verbunden").isTrue();
        warteBisKeineVerarbeitungLaeuft();

        system = docPropHelper.getTurnierSystemAusDocument();
        assertThat(system).as("Turniersystem aus dem Online-Turnier").isEqualTo(erwartet);
        spieltagNr = system == TurnierSystem.SUPERMELEE
                ? new SuperMeleeKonfigurationSheet(wkingSpreadsheet).getAktiveSpieltag().getNr()
                : null;
        mapping = new PtmOnlineRegistrationMapping(wkingSpreadsheet, system, spieltagNr);
        assertThat(mapping.getTournamentId()).contains(turnierId);
        ziel = MeldelisteZielFactory.fuerPtmOnline(wkingSpreadsheet).orElseThrow();
        assertThat(turnierAusListe().documentManaged).as("online als dokumentverwaltet markiert").isTrue();
    }

    /** Das Turnier so, wie die Verbinden-Auswahl es vom Server bekommt. */
    private OnlineTournamentDto turnierAusListe() throws Exception {
        return new TournamentSyncClient(server.zugangsdaten().baseUrl(), server.zugangsdaten().apiKey())
                .listTournaments().stream().filter(t -> turnierId.equals(t.id)).findFirst().orElseThrow();
    }
}
