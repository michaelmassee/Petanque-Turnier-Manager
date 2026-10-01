/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.util.XProtectable;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzManager;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzRegistry;
import de.petanqueturniermanager.onlinesync.OnlineTournamentDto;
import de.petanqueturniermanager.onlinesync.sheet.PtmOnlineSyncSheet;
import de.petanqueturniermanager.ptmonline.dto.SyncBindingDto;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetNew;
import de.petanqueturniermanager.spielerdb.MeldelisteSpielerDaten;
import de.petanqueturniermanager.spielerdb.MeldelisteZiel;
import de.petanqueturniermanager.spielerdb.MeldelisteZielFactory;
import de.petanqueturniermanager.spielerdb.SpielerMitVerein;
import de.petanqueturniermanager.toolbar.TurnierModus;

/**
 * PTM-Online-Anbindung gegen einen lokalen Test-Server:
 * <ul>
 * <li>Der manuelle Abgleich läuft als SheetRunner, aktualisiert die Meldeliste im selben Runner (kein
 * zweiter Runner, keine „Verarbeitung läuft“-Kollision) und lässt sich während eines Serverabrufs
 * abbrechen.</li>
 * <li>Die Prüfung vor dem Turnierstart importiert nichts und meldet fehlende Online-Meldungen.</li>
 * <li>Vor Ort erfasste, namensgleiche Zeilen stehen als „möglicherweise identisch“ in der Konfliktliste und werden
 * nach der Entscheidung „verknüpfen“ beim nächsten Abgleich verknüpft (KP-06 a2, A-29).</li>
 * </ul>
 */
class PtmOnlineAbgleichSheetRunnerUITest extends BaseCalcUITest {

    private static final String TURNIER_ID = "t1";
    private static final String DOCUMENT_ID = "e9e9caec-e0b1-4fe0-8fee-a229279b9f73";
    private static final String LEASE_TOKEN = "01234567890123456789012345678901";
    /** Spaltenüberschrift „Online-ID“ der Zuordnungstabelle im Blatt „PTMOnline Sync“ (Zeile 7, Spalte F). */
    private static final Position KOPF_ONLINE_ID = Position.from(5, 6);
    private static final String ANMELDUNGEN = """
            {"registrations":[
              {"id":"r1","tournamentId":"t1","firstName":"Anna","lastName":"Schmidt","status":"confirmed"},
              {"id":"r2","tournamentId":"t1","firstName":"Hans","lastName":"Müller","status":"confirmed"},
              {"id":"r3","tournamentId":"t1","firstName":"Offen","lastName":"Noch","status":"pending"}
            ]}""";

    private PtmOnlineTestServer server;
    private MeldelisteZiel ziel;
    private PtmOnlineRegistrationMapping mapping;

    @BeforeEach
    void turnierUndServerAnlegen() throws Exception {
        server = new PtmOnlineTestServer(TURNIER_ID, ANMELDUNGEN);

        new SchweizerMeldeListeSheetNew(wkingSpreadsheet).createMeldelisteWithParams(Formation.TETE, false, false);
        docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM,
                TurnierSystem.SCHWEIZER.getId());
        ziel = MeldelisteZielFactory.fuerAktivesSheet(wkingSpreadsheet).orElseThrow();
        ziel.schreibeBlock(List.of(new SpielerMitVerein(0, "Hans", "Müller", null, null, List.of(), List.of(), null)));

        mapping = new PtmOnlineRegistrationMapping(wkingSpreadsheet, TurnierSystem.SCHWEIZER, null);
        OnlineTournamentDto turnier = new OnlineTournamentDto();
        turnier.id = TURNIER_ID;
        turnier.name = "Testturnier";
        turnier.type = "schweizer";
        mapping.verbinden(turnier, new SyncBindingDto(true, DOCUMENT_ID, 1), LEASE_TOKEN);
    }

    @AfterEach
    void serverStoppen() {
        server.close();
        MessageBox.setDialogeUeberspringen(false);
    }

    @Test
    void abgleichUebernimmtUndVerknuepftNachEntscheidungInDerKonfliktliste() throws Exception {
        abgleichen();

        assertThat(mapping.istBereitsImportiert("r1")).as("neue Online-Anmeldung übernommen").isTrue();
        assertThat(mapping.istBereitsImportiert("r2")).as("gleichnamige Zeile nicht automatisch verknüpft")
                .isFalse();
        assertThat(mapping.istBereitsImportiert("r3")).as("offene Anmeldung nicht übernommen").isFalse();
        assertThat(nachnamen()).containsExactlyInAnyOrder("Schmidt", "Müller");
        assertThat(ziel.getTeamNrAusZeile(ziel.findeZeileMitName("Anna Schmidt")))
                .as("Meldeliste im selben Runner aktualisiert: neue Zeile hat eine Nr").isPositive();
        assertThat(server.anzahlOnlineAngelegt()).as("möglicherweise identische Zeile nicht online angelegt")
                .isZero();
        String fall = KonfliktFall.schluessel(KonfliktArt.MOEGLICH_IDENTISCH, uuidVonHans(), List.of("r2"));
        assertThat(mapping.konfliktListe().leseFaelle()).containsOnlyKeys(fall);

        mapping.konfliktListe().setzeEntscheidung(fall, Entscheidung.VERKNUEPFEN.anzeige());
        abgleichen();

        assertThat(mapping.istBereitsImportiert("r2")).as("nach „verknüpfen“ zugeordnet").isTrue();
        assertThat(server.anzahlOnlineAngelegt()).isZero();
        assertThat(mapping.konfliktListe().leseFaelle()).as("Fall erledigt").isEmpty();
        assertThat(server.entscheidungen()).singleElement().satisfies(entscheidung -> {
            assertThat(entscheidung.get("decision").getAsString()).isEqualTo("link");
            assertThat(entscheidung.get("onlineRegistrationId").getAsString()).isEqualTo("r2");
        });
    }

    @Test
    void bewusstGetrenntLegtDieLokaleZeileOnlineAn() throws Exception {
        abgleichen();
        String fall = KonfliktFall.schluessel(KonfliktArt.MOEGLICH_IDENTISCH, uuidVonHans(), List.of("r2"));
        mapping.konfliktListe().setzeEntscheidung(fall, Entscheidung.GETRENNT.anzeige());

        abgleichen();

        assertThat(server.anzahlOnlineAngelegt()).as("lokale Zeile zusätzlich online angelegt").isEqualTo(1);
        assertThat(mapping.istBereitsImportiert("r2")).isFalse();
        assertThat(mapping.konfliktListe().leseFaelle().keySet())
                .as("gleichnamige Anmeldung bleibt als nicht übertragen gemeldet")
                .allMatch(schluessel -> schluessel.startsWith(KonfliktArt.NICHT_UEBERTRAGEN.name()));
        assertThat(server.entscheidungen()).extracting(entscheidung -> entscheidung.get("decision").getAsString())
                .containsExactly("separate");
    }

    @Test
    void amTurniertagBietetDerAbgleichAnDieOnlineAnmeldungZuSchliessen() throws Exception {
        server.close();
        String heute = LocalDate.now().toString();
        server = new PtmOnlineTestServer(TURNIER_ID, ANMELDUNGEN.replace("{\"registrations\":[",
                "{\"tournament\":{\"status\":\"registration\",\"registrationClosed\":false,\"roundsOnline\":0,"
                        + "\"writeCounter\":0,\"date\":\"" + heute + "\"},\"registrations\":["));
        MessageBox.setDialogeUeberspringen(true);

        abgleichen();

        assertThat(server.anmeldungGeschlossen()).as("Rückfrage mit Ja beantwortet")
                .singleElement().satisfies(body -> assertThat(body.get("closed").getAsBoolean()).isTrue());
    }

    @Test
    void ohneTurnierstandBietetDerAbgleichNichtsAn() throws Exception {
        MessageBox.setDialogeUeberspringen(true);

        abgleichen();

        assertThat(server.anmeldungGeschlossen()).isEmpty();
    }

    private void abgleichen() throws Exception {
        PtmOnlineAbgleichSheetRunner runner = neuerRunner();
        runner.start();
        runner.join();
        assertThat(runner.isLetzterLaufFehlgeschlagen()).isFalse();
    }

    private String uuidVonHans() throws Exception {
        return ziel.getOderErzeugeLokaleUuid(ziel.findeZeileMitName("Hans Müller"));
    }

    /** Wie im Turnierbetrieb: Hans Müller als „möglicherweise identisch“ mit r2 bestätigt. */
    private void abgleichenUndHansVerknuepfen() throws Exception {
        abgleichen();
        mapping.konfliktListe().setzeEntscheidung(
                KonfliktFall.schluessel(KonfliktArt.MOEGLICH_IDENTISCH, uuidVonHans(), List.of("r2")),
                Entscheidung.VERKNUEPFEN.anzeige());
        abgleichen();
    }

    /**
     * Regression: Der Abgleich läuft selbst als SheetRunner. Das Anlegen der Zuordnungstabelle darf darin
     * keinen zweiten Runner starten, sonst meldet LO „Verarbeitung läuft bereits“ und die Tabelle wird nicht
     * aktualisiert.
     */
    @Test
    void abgleichAktualisiertZuordnungstabelleOhneZweitenRunner() throws Exception {
        PtmOnlineSyncSheet syncSheet = new PtmOnlineSyncSheet(wkingSpreadsheet, TurnierSystem.SCHWEIZER, null);
        syncSheet.getSheetHelper().setStringValueInCell(
                StringCellValue.from(syncSheet.getXSpreadSheet(), KOPF_ONLINE_ID, ""));
        PtmOnlineAbgleichSheetRunner runner = neuerRunner();

        runner.start();
        runner.join();

        assertThat(runner.isLetzterLaufFehlgeschlagen()).isFalse();
        assertThat(syncSheet.getSheetHelper().getTextFromCell(syncSheet.getXSpreadSheet(), KOPF_ONLINE_ID))
                .isEqualTo(I18n.get("ptmonline.sheet.mapping.header.onlineid"));
    }

    /**
     * Im Turnier-Modus sind Meldeliste (bis auf Eingabezellen) und „PTMOnline Sync“ gesperrt. Der Abgleich
     * schreibt trotzdem – UUIDs, Zuordnung, Nr-Formeln – und hinterlässt das Sync-Blatt wieder gesperrt.
     */
    @Test
    void abgleichImTurnierModusSchreibtInGesperrteBlaetter() throws Exception {
        var konfig = BlattschutzRegistry.fuer(TurnierSystem.SCHWEIZER).orElseThrow();
        TurnierModus.get().setAktivForTest(true);
        try {
            BlattschutzManager.get().schuetzen(konfig, wkingSpreadsheet);
            XSpreadsheet syncSheet = new PtmOnlineSyncSheet(wkingSpreadsheet, TurnierSystem.SCHWEIZER, null)
                    .getXSpreadSheet();
            assertThat(Lo.qi(XProtectable.class, syncSheet).isProtected()).as("vor dem Abgleich gesperrt").isTrue();
            abgleichenUndHansVerknuepfen();

            assertThat(mapping.istBereitsImportiert("r1")).isTrue();
            assertThat(mapping.istBereitsImportiert("r2")).as("Entscheidung im gesperrten Blatt eingetragen").isTrue();
            assertThat(Lo.qi(XProtectable.class, syncSheet).isProtected()).as("nach dem Abgleich gesperrt").isTrue();
            XSpreadsheet konflikte = mapping.konfliktListe().getXSpreadSheet();
            assertThat(Lo.qi(XProtectable.class, konflikte).isProtected()).as("Konfliktliste gesperrt").isTrue();

            mapping.trennen();

            assertThat(SheetMetadataHelper.findeSheet(wkingSpreadsheet.getWorkingSpreadsheetDocument(),
                    SheetMetadataHelper.SCHLUESSEL_PTM_ONLINE_SYNC)).as("gesperrtes Blatt wird beim Trennen entfernt")
                    .isEmpty();
        } finally {
            BlattschutzManager.get().entsperren(konfig, wkingSpreadsheet);
            TurnierModus.get().setAktivForTest(false);
        }
    }

    @Test
    void abbruchWaehrendDesServerabrufsUebernimmtNichts() throws Exception {
        MessageBox.setDialogeUeberspringen(true);
        server.antwortenZurueckhalten();
        PtmOnlineAbgleichSheetRunner runner = neuerRunner();

        runner.start();
        assertThat(server.warteAufErstenAbruf()).isTrue();
        SheetRunner.cancelRunner();
        runner.join();

        assertThat(runner.isLetzterLaufFehlgeschlagen()).as("Abbruch beendet den Lauf").isTrue();
        assertThat(mapping.istBereitsImportiert("r1")).isFalse();
        assertThat(nachnamen()).containsExactly("Müller");
    }

    @Test
    void pruefungVorTurnierstartMeldetFehlendeUndImportiertNichts() throws Exception {
        List<String> fehlend = RegistrationImportTask.pruefeVorTurnierstart(server.zugangsdaten(), mapping,
                TURNIER_ID, ziel).fehlend();

        assertThat(fehlend).as("nur bestätigte, noch nicht zugeordnete Meldungen; gleichnamige nur als Hinweis")
                .hasSize(2).first().isEqualTo("Anna Schmidt");
        assertThat(fehlend.get(1)).startsWith("Hans Müller (");
        assertThat(nachnamen()).as("nichts importiert").containsExactly("Müller");
        assertThat(mapping.istBereitsImportiert("r1")).isFalse();
        assertThat(mapping.istBereitsImportiert("r2")).as("gleichnamige Zeile nicht automatisch verknüpft")
                .isFalse();
        assertThat(mapping.getLastSync()).as("späterer Abgleich ruft die fehlenden weiter ab").isEmpty();
    }

    /** Rundenstart meldet die lokale Setzposition (Meldeliste ist Master), nicht die Team-Nr. */
    @Test
    void rundenstartMeldetLokaleSetzpositionStattTeamNr() throws Exception {
        abgleichenUndHansVerknuepfen();
        int zeileHans = ziel.findeZeileMitName("Hans Müller");
        ziel.uebernehmeOnlineSetzposition(zeileHans, 3);
        Set<Integer> teams = Set.of(ziel.getTeamNrAusZeile(zeileHans),
                ziel.getTeamNrAusZeile(ziel.findeZeileMitName("Anna Schmidt")));

        var zugang = server.zugangsdaten();
        PtmOnlineAuftragsTestHilfe.teilnahmeSenden(wkingSpreadsheet, mapping,
                new TournamentSyncClient(zugang.baseUrl(), zugang.apiKey(), DOCUMENT_ID, LEASE_TOKEN), TURNIER_ID,
                PtmOnlineSpielrundeSync.statusAuftrag(ziel, TURNIER_ID,
                        PtmOnlineSpielrundeSync.lokaleMeldungen(ziel, ziel, teams, teams, Set.of())).eintraege());

        Map<String, JsonObject> gepusht = server.gepushteErgebnisse().stream()
                .collect(Collectors.toMap(eintrag -> eintrag.get("id").getAsString(), eintrag -> eintrag));
        assertThat(gepusht).containsOnlyKeys("r1", "r2");
        assertThat(gepusht.get("r2").get("seedingPosition").getAsInt()).as("Hans Müller, lokal gesetzt").isEqualTo(3);
        assertThat(gepusht.get("r1").has("seedingPosition")).as("Anna Schmidt ohne SP: online löschen").isFalse();
    }

    private PtmOnlineAbgleichSheetRunner neuerRunner() {
        return new PtmOnlineAbgleichSheetRunner(wkingSpreadsheet, TurnierSystem.SCHWEIZER, null,
                server.zugangsdaten(), mapping, TURNIER_ID, ziel);
    }

    private List<String> nachnamen() {
        return ziel.leseAlleSpielerRoh().stream().map(MeldelisteSpielerDaten::nachname).toList();
    }
}
