/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.LibreOfficePtmOnlineSpeicher;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.onlinesync.OnlineTournamentDto;
import de.petanqueturniermanager.ptmonline.dto.SyncBindingDto;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetNew;
import de.petanqueturniermanager.spielerdb.MeldelisteSpielerDaten;
import de.petanqueturniermanager.spielerdb.MeldelisteZiel;
import de.petanqueturniermanager.spielerdb.MeldelisteZielFactory;
import de.petanqueturniermanager.spielerdb.SpielerMitVerein;

/**
 * Manueller PTM-Online-Abgleich als SheetRunner gegen einen lokalen Test-Server: Der Lauf muss die
 * Meldeliste im selben Runner aktualisieren können (kein zweiter Runner, keine „Verarbeitung
 * läuft“-Kollision) und dabei Online-Anmeldungen übernehmen bzw. verknüpfen.
 */
class PtmOnlineAbgleichSheetRunnerUITest extends BaseCalcUITest {

    private static final String TURNIER_ID = "t1";
    private static final String ANMELDUNGEN = """
            {"registrations":[
              {"id":"r1","tournamentId":"t1","firstName":"Anna","lastName":"Schmidt","status":"confirmed"},
              {"id":"r2","tournamentId":"t1","firstName":"Hans","lastName":"Müller","status":"confirmed"}
            ]}""";

    private final AtomicInteger anzahlOnlineAngelegt = new AtomicInteger();
    private final CountDownLatch abrufAngekommen = new CountDownLatch(1);
    private volatile CountDownLatch antwortFreigabe = new CountDownLatch(0);
    private HttpServer server;
    private MeldelisteZiel ziel;
    private PtmOnlineRegistrationMapping mapping;

    @BeforeEach
    void turnierUndServerAnlegen() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/sync/tournaments/" + TURNIER_ID + "/registrations", this::beantworte);
        server.start();

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
        mapping.verbinden(turnier);
        mapping.setSyncBinding(new SyncBindingDto(true, "e9e9caec-e0b1-4fe0-8fee-a229279b9f73", 1),
                "01234567890123456789012345678901");
    }

    @AfterEach
    void serverStoppen() {
        antwortFreigabe.countDown();
        server.stop(0);
        MessageBox.setDialogeUeberspringen(false);
    }

    @Test
    void abgleichUebernimmtUndVerknuepftImSelbenRunner() throws Exception {
        PtmOnlineAbgleichSheetRunner runner = new PtmOnlineAbgleichSheetRunner(wkingSpreadsheet,
                TurnierSystem.SCHWEIZER, zugangsdaten(), mapping, TURNIER_ID, ziel);

        runner.start();
        runner.join();

        assertThat(runner.isLetzterLaufFehlgeschlagen()).isFalse();
        assertThat(mapping.istBereitsImportiert("r1")).as("neue Online-Anmeldung übernommen").isTrue();
        assertThat(mapping.istBereitsImportiert("r2")).as("mit vor Ort erfasster Zeile verknüpft").isTrue();
        assertThat(ziel.leseAlleSpielerRoh()).extracting(MeldelisteSpielerDaten::nachname)
                .containsExactlyInAnyOrder("Schmidt", "Müller");
        assertThat(ziel.getTeamNrAusZeile(ziel.findeZeileMitName("Anna Schmidt")))
                .as("Meldeliste im selben Runner aktualisiert: neue Zeile hat eine Nr").isPositive();
        assertThat(anzahlOnlineAngelegt).as("alle lokalen Meldungen sind schon online").hasValue(0);
    }

    @Test
    void abbruchWaehrendDesServerabrufsUebernimmtNichts() throws Exception {
        MessageBox.setDialogeUeberspringen(true);
        antwortFreigabe = new CountDownLatch(1);
        PtmOnlineAbgleichSheetRunner runner = new PtmOnlineAbgleichSheetRunner(wkingSpreadsheet,
                TurnierSystem.SCHWEIZER, zugangsdaten(), mapping, TURNIER_ID, ziel);

        runner.start();
        assertThat(abrufAngekommen.await(30, TimeUnit.SECONDS)).isTrue();
        SheetRunner.cancelRunner();
        runner.join();

        assertThat(runner.isLetzterLaufFehlgeschlagen()).as("Abbruch beendet den Lauf").isTrue();
        assertThat(mapping.istBereitsImportiert("r1")).isFalse();
        assertThat(ziel.leseAlleSpielerRoh()).extracting(MeldelisteSpielerDaten::nachname).containsExactly("Müller");
    }

    private LibreOfficePtmOnlineSpeicher.Zugangsdaten zugangsdaten() {
        return new LibreOfficePtmOnlineSpeicher.Zugangsdaten("ptm_test",
                "http://127.0.0.1:" + server.getAddress().getPort());
    }

    private void beantworte(HttpExchange exchange) throws IOException {
        abrufAngekommen.countDown();
        try {
            antwortFreigabe.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if ("PUT".equals(exchange.getRequestMethod())) {
            anzahlOnlineAngelegt.incrementAndGet();
        }
        byte[] antwort = ANMELDUNGEN.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, antwort.length);
        try (OutputStream body = exchange.getResponseBody()) {
            body.write(antwort);
        }
    }
}
