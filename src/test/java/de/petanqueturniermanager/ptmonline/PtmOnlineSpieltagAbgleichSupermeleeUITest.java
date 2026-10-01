/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeKonstanten;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.random.RandomSource;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.onlinesync.OnlineTournamentDto;
import de.petanqueturniermanager.ptmonline.dto.SyncBindingDto;
import de.petanqueturniermanager.spielerdb.MeldelisteSpielerDaten;
import de.petanqueturniermanager.spielerdb.MeldelisteZiel;
import de.petanqueturniermanager.spielerdb.MeldelisteZielFactory;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeKonfigurationSheet;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_Update;
import de.petanqueturniermanager.supermelee.spieltagrangliste.SupermeleeTurnierTestDaten;

/**
 * Supermêlée mit mehreren Spieltagen: Die Meldeliste ist der Spielerpool aller Spieltage, jeder Spieltag hat sein
 * eigenes Online-Turnier. Der Abgleich bleibt auf den Spieltag beschränkt – ein online angemeldeter Stammspieler wird
 * mit seiner Zeile verknüpft, online angelegt werden nur die am Spieltag gemeldeten Spieler, der übrige Pool bleibt
 * rein lokal.
 */
class PtmOnlineSpieltagAbgleichSupermeleeUITest extends BaseCalcUITest {

    private static final String TURNIER_ID = "t1";
    private static final String DOCUMENT_ID = "e9e9caec-e0b1-4fe0-8fee-a229279b9f73";
    private static final String LEASE_TOKEN = "01234567890123456789012345678901";

    private PtmOnlineTestServer server;
    private int spieltag;
    private MeldelisteZiel ziel;
    private List<MeldelisteSpielerDaten> ersteSpieler;

    @BeforeEach
    void vorbereiten() throws Exception {
        RandomSource.setSeed(42L);
        MessageBox.setDialogeUeberspringen(true);
        new SupermeleeTurnierTestDaten(wkingSpreadsheet).generate();
        docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM,
                TurnierSystem.SUPERMELEE.getId());
        spieltag = new SuperMeleeKonfigurationSheet(wkingSpreadsheet).getAktiveSpieltag().getNr();
        ziel = MeldelisteZielFactory.fuerPtmOnline(wkingSpreadsheet).orElseThrow();
        Map<Integer, MeldelisteSpielerDaten> proZeile = new LinkedHashMap<>();
        ziel.leseAlleSpielerRoh().forEach(spieler -> proZeile.putIfAbsent(spieler.zeile1Basiert(), spieler));
        ersteSpieler = List.copyOf(proZeile.values()).subList(0, 3);
    }

    @AfterEach
    void aufraeumen() {
        if (server != null) {
            server.close();
        }
        MessageBox.setDialogeUeberspringen(false);
        RandomSource.reset();
    }

    @Test
    void abgleichBleibtAufDenSpieltagBeschraenkt() throws Exception {
        assertThat(spieltag).as("Testdaten mit mehreren Spieltagen").isGreaterThan(1);
        MeldelisteSpielerDaten stammspieler = ersteSpieler.get(0);
        MeldelisteSpielerDaten gemeldet = ersteSpieler.get(1);
        MeldelisteSpielerDaten heuteNichtDabei = ersteSpieler.get(2);
        // Stammspieler hat sich online angemeldet, Spieler 2 ist nur lokal für heute gemeldet, Spieler 3 spielt nicht.
        schreibeSpieltagSpalte(List.of("", "1", ""));
        Set<Integer> spieltagZeilen = ziel.zeilenDesAktivenSpieltags().orElseThrow();
        assertThat(spieltagZeilen).contains(gemeldet.zeile1Basiert())
                .doesNotContain(stammspieler.zeile1Basiert(), heuteNichtDabei.zeile1Basiert());
        server = new PtmOnlineTestServer(TURNIER_ID, anmeldungen(stammspieler));
        PtmOnlineRegistrationMapping mapping = verbinden();

        PtmOnlineAbgleichSheetRunner runner = new PtmOnlineAbgleichSheetRunner(wkingSpreadsheet,
                TurnierSystem.SUPERMELEE, spieltag, server.zugangsdaten(), mapping, TURNIER_ID, ziel);
        runner.start();
        runner.join();

        assertThat(runner.isLetzterLaufFehlgeschlagen()).isFalse();
        int zeileStammspieler = ziel.findeZeileMitName(name(stammspieler));
        assertThat(mapping.getLokaleUuid("r1")).as("Stammspieler ohne Rückfrage mit seiner Pool-Zeile verknüpft")
                .contains(ziel.leseLokaleUuids(List.of(zeileStammspieler)).get(zeileStammspieler));
        assertThat(mapping.istBereitsImportiert("r2")).as("neuer Online-Spieler übernommen").isTrue();
        assertThat(mapping.konfliktListe().leseFaelle().keySet())
                .noneMatch(schluessel -> schluessel.startsWith(KonfliktArt.MOEGLICH_IDENTISCH.name()));

        Set<String> angelegt = server.onlineAngelegt().stream()
                .map(body -> body.get("firstName").getAsString() + " " + body.get("lastName").getAsString())
                .collect(Collectors.toSet());
        assertThat(angelegt).as("heute gemeldet, noch nicht online").contains(name(gemeldet))
                .doesNotContain(name(stammspieler), name(heuteNichtDabei));
        Set<String> heuteGemeldet = ziel.leseAlleSpielerRoh().stream()
                .filter(spieler -> spieltagZeilen.contains(spieler.zeile1Basiert())).map(this::name)
                .collect(Collectors.toSet());
        assertThat(heuteGemeldet).as("nur Spieler des Spieltags gehen online").containsAll(angelegt);
    }

    private PtmOnlineRegistrationMapping verbinden() throws Exception {
        PtmOnlineRegistrationMapping mapping = new PtmOnlineRegistrationMapping(wkingSpreadsheet,
                TurnierSystem.SUPERMELEE, spieltag);
        OnlineTournamentDto turnier = new OnlineTournamentDto();
        turnier.id = TURNIER_ID;
        turnier.name = "Supermelee " + spieltag + ". Spieltag";
        turnier.type = "supermelee";
        mapping.verbinden(turnier, new SyncBindingDto(true, DOCUMENT_ID, 1), LEASE_TOKEN);
        return mapping;
    }

    private static String anmeldungen(MeldelisteSpielerDaten stammspieler) {
        JsonArray registrations = new JsonArray();
        registrations.add(anmeldung("r1", stammspieler.vorname(), stammspieler.nachname()));
        registrations.add(anmeldung("r2", "Neu", "Spielerin"));
        JsonObject antwort = new JsonObject();
        antwort.add("registrations", registrations);
        return antwort.toString();
    }

    private static JsonObject anmeldung(String id, String vorname, String nachname) {
        JsonObject anmeldung = new JsonObject();
        anmeldung.addProperty("id", id);
        anmeldung.addProperty("tournamentId", TURNIER_ID);
        anmeldung.addProperty("firstName", vorname);
        anmeldung.addProperty("lastName", nachname);
        anmeldung.addProperty("status", "confirmed");
        return anmeldung;
    }

    private String name(MeldelisteSpielerDaten spieler) {
        return spieler.vorname() + " " + spieler.nachname();
    }

    /** Schreibt die Werte der ersten Spieler als Block in die Spalte des aktiven Spieltags. */
    private void schreibeSpieltagSpalte(List<String> werte) throws Exception {
        MeldeListeSheet_Update meldeliste = new MeldeListeSheet_Update(wkingSpreadsheet);
        int spalte = meldeliste.spieltagSpalte(SpielTagNr.from(spieltag));
        RangeData daten = new RangeData();
        werte.forEach(wert -> {
            RowData zeile = daten.addNewRow();
            if (wert.isEmpty()) {
                zeile.newEmpty();
            } else {
                zeile.newInt(Integer.parseInt(wert));
            }
        });
        RangeHelper.from(meldeliste.getXSpreadSheet(), wkingSpreadsheet.getWorkingSpreadsheetDocument(),
                daten.getRangePosition(Position.from(spalte, MeldeListeKonstanten.ERSTE_DATEN_ZEILE)))
                .setDataInRange(daten);
    }
}
