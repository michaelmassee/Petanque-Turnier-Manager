/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.LibreOfficePtmOnlineSpeicher;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.onlinesync.OnlineTournamentDto;
import de.petanqueturniermanager.ptmonline.PtmOnlineWebApi.Person;
import de.petanqueturniermanager.ptmonline.ui.PtmOnlineVerbindenTestZugang;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetNew;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetUpdate;
import de.petanqueturniermanager.spielerdb.MeldelisteSpielerDaten;
import de.petanqueturniermanager.spielerdb.MeldelisteZiel;
import de.petanqueturniermanager.spielerdb.MeldelisteZielFactory;
import de.petanqueturniermanager.spielerdb.MeldelisteZiel.NeueMeldungTeilnahme;
import de.petanqueturniermanager.spielerdb.SpielerMitVerein;

/**
 * Grundlage der PTM-Online-Zusammenspiel-Tests: echtes Calc-Dokument mit Schweizer Meldeliste, verbunden über den
 * echten Verbinden-Runner mit einem Turnier auf dem lokal gestarteten PTM-Online-Worker
 * ({@link LokalerPtmOnlineServer}). Jeder Test legt sein eigenes Online-Turnier an.
 * <p>
 * Die Zugangsdaten werden nur im Speicher ersetzt ({@link LibreOfficePtmOnlineSpeicher#setZugangsdatenForTest}); das
 * LibreOffice-Benutzerprofil mit den echten Zugangsdaten bleibt unberührt.
 */
abstract class BasePtmOnlineZusammenspielTest extends BaseCalcUITest {

    protected static final TurnierSystem SYSTEM = TurnierSystem.SCHWEIZER;
    private static final Duration WARTEZEIT = Duration.ofSeconds(30);

    protected static LokalerPtmOnlineServer server;
    protected static PtmOnlineWebApi online;

    protected MeldelisteZiel ziel;
    protected PtmOnlineRegistrationMapping mapping;
    protected String turnierId;

    @BeforeAll
    static void lokalenWorkerStarten() {
        server = LokalerPtmOnlineServer.get();
        online = new PtmOnlineWebApi(server);
    }

    @BeforeEach
    void zugangSetzen() {
        MessageBox.setDialogeUeberspringen(true);
        LibreOfficePtmOnlineSpeicher.setZugangsdatenForTest(server.zugangsdaten());
        // Im Betrieb startet die Extension den Beobachter; er verschickt die gepufferten Aufträge nach einem
        // Turnier-Kommando (z. B. den Turnierstart) im Hintergrund. In der Test-JVM übernimmt das der Test.
        PtmOnlineLiveBeobachter.init(wkingSpreadsheet.getxContext());
    }

    @AfterEach
    void zugangZuruecksetzen() {
        LibreOfficePtmOnlineSpeicher.setZugangsdatenForTest(null);
        MessageBox.setDialogeUeberspringen(false);
        if (turnierId != null) {
            PtmOnlineLiveSync.vergessen(turnierId);
        }
    }

    /** Online-Turnier (Schweizer, Formée) und leere lokale Meldeliste in derselben Formation. */
    protected void turnierAnlegen(Formation formation, LocalDate datum) throws Exception {
        turnierId = online.turnierAnlegen("E2E " + getClass().getSimpleName(), "schweizer",
                formation.name().toLowerCase(java.util.Locale.ROOT), datum);
        new SchweizerMeldeListeSheetNew(wkingSpreadsheet).createMeldelisteWithParams(formation, false, false);
        docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM, SYSTEM.getId());
        ziel = MeldelisteZielFactory.fuerPtmOnline(wkingSpreadsheet).orElseThrow();
    }

    /** Wie „Verbinden“ im Menü: Server-Bindung, Blatt „PTMOnline Sync“, Schreib-Lease. */
    protected void verbinden() throws Exception {
        OnlineTournamentDto turnier = new OnlineTournamentDto();
        turnier.id = turnierId;
        turnier.name = "E2E";
        turnier.type = "schweizer";
        turnier.registrationType = "forme";
        turnier.status = "registration";
        assertThat(PtmOnlineVerbindenTestZugang.verbinden(wkingSpreadsheet, SYSTEM, null, server.zugangsdaten(),
                turnier)).as("Verbinden erfolgreich").isTrue();
        mapping = new PtmOnlineRegistrationMapping(wkingSpreadsheet, SYSTEM, null);
        assertThat(mapping.getTournamentId()).contains(turnierId);
    }

    /** Manueller Abgleich (Menü „Abgleichen“). */
    protected void abgleichen() throws Exception {
        PtmOnlineAbgleichSheetRunner runner = new PtmOnlineAbgleichSheetRunner(wkingSpreadsheet, SYSTEM, null,
                server.zugangsdaten(), mapping, turnierId, ziel);
        runner.start();
        runner.join();
        assertThat(runner.isLetzterLaufFehlgeschlagen()).as("Abgleich ohne Fehler").isFalse();
    }

    /** Vor Ort erfasste Meldung (noch nicht eingecheckt); liefert die 1-basierte Zeile. */
    protected int lokaleMeldung(Person... personen) throws Exception {
        List<SpielerMitVerein> spieler = new ArrayList<>();
        for (Person person : personen) {
            spieler.add(new SpielerMitVerein(0, person.vorname(), person.nachname(), null, null, List.of(), List.of(),
                    null));
        }
        return ziel.schreibeBlockUndLiefereZeile(spieler, NeueMeldungTeilnahme.INAKTIV);
    }

    /** Check-in in der Aktiv-Spalte, wie die Turnierleitung vor Ort. */
    protected void einchecken(int zeile1Basiert) throws Exception {
        SchweizerMeldeListeSheetUpdate meldeliste = new SchweizerMeldeListeSheetUpdate(wkingSpreadsheet);
        sheetHlp.setNumberValueInCell(NumberCellValue.from(meldeliste.getXSpreadSheet(),
                Position.from(meldeliste.getAktivSpalte(), zeile1Basiert - 1))
                .setValue(SchweizerMeldeListeSheetUpdate.AKTIV_WERT_NIMMT_TEIL));
    }

    protected int zeile(Person person) {
        int zeile = ziel.findeZeileMitName(person.name());
        assertThat(zeile).as("Zeile von %s", person.name()).isPositive();
        return zeile;
    }

    /** Namen je Meldelistenzeile, sortiert – eine Meldung je Eintrag. */
    protected Map<Integer, Set<String>> lokaleMeldungen() {
        Map<Integer, Set<String>> meldungen = new LinkedHashMap<>();
        for (MeldelisteSpielerDaten spieler : ziel.leseAlleSpielerRoh()) {
            meldungen.computeIfAbsent(spieler.zeile1Basiert(), z -> new TreeSet<>())
                    .add(spieler.vorname() + " " + spieler.nachname());
        }
        return meldungen;
    }

    protected static Set<String> namen(Person... personen) {
        Set<String> namen = new TreeSet<>();
        Arrays.stream(personen).map(Person::name).forEach(namen::add);
        return namen;
    }

    /** Konfliktarten der offenen Fälle in der Konfliktliste (Schlüssel „ART|uuid|ids“). */
    protected Set<KonfliktArt> konfliktArten() throws Exception {
        Set<KonfliktArt> arten = new TreeSet<>();
        for (String schluessel : mapping.konfliktListe().leseFaelle().keySet()) {
            arten.add(KonfliktArt.valueOf(schluessel.substring(0, schluessel.indexOf('|'))));
        }
        return arten;
    }

    /** Wartet auf einen asynchron übertragenen Stand (Auftragsversand im Hintergrund). */
    protected static void warteBis(String was, Callable<Boolean> bedingung) throws Exception {
        Instant frist = Instant.now().plus(WARTEZEIT);
        while (!Boolean.TRUE.equals(bedingung.call())) {
            if (Instant.now().isAfter(frist)) {
                throw new AssertionError("Nicht erreicht nach " + WARTEZEIT.toSeconds() + " s: " + was);
            }
            Thread.sleep(500);
        }
    }
}
