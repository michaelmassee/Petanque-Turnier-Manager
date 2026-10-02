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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerAbstractSpielrundeSheet;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerSpielrundeSheetNaechste;
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
import de.petanqueturniermanager.basesheet.konfiguration.MeleeAnmeldungKonfiguration;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetNew;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeleeAnmeldungSheet;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetUpdate;
import de.petanqueturniermanager.spielerdb.MeldelisteSpielerDaten;
import de.petanqueturniermanager.spielerdb.MeldelisteZiel;
import de.petanqueturniermanager.spielerdb.MeldelisteZielFactory;
import de.petanqueturniermanager.spielerdb.MeldelisteZiel.NeueMeldungTeilnahme;
import de.petanqueturniermanager.spielerdb.SpielerMitVerein;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeKonfigurationSheet;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeMode;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_New;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_Update;

/**
 * Grundlage der PTM-Online-Zusammenspiel-Tests: echtes Calc-Dokument mit Meldeliste (Schweizer oder Supermêlée),
 * verbunden über den echten Verbinden-Runner mit einem Turnier auf dem lokal gestarteten PTM-Online-Worker
 * ({@link LokalerPtmOnlineServer}). Jeder Test legt sein eigenes Online-Turnier an.
 * <p>
 * Die Zugangsdaten werden nur im Speicher ersetzt ({@link LibreOfficePtmOnlineSpeicher#setZugangsdatenForTest}); das
 * LibreOffice-Benutzerprofil mit den echten Zugangsdaten bleibt unberührt.
 */
abstract class BasePtmOnlineZusammenspielTest extends BaseCalcUITest {

    private static final Duration WARTEZEIT = Duration.ofSeconds(30);

    protected static LokalerPtmOnlineServer server;
    protected static PtmOnlineWebApi online;

    /** Turniersystem des Dokuments; bei Supermêlée zusätzlich der verbundene Spieltag. */
    protected TurnierSystem system;
    protected Integer spieltagNr;
    /** Online-Anmeldetyp: „forme“ (Teams), „melee“ (Einzelne, Teams entstehen in PTM) oder „supermelee“. */
    protected String anmeldeTyp;
    /** Turniersystem online (z. B. „schweizer“, „rangliste“, „ko“). */
    protected String onlineTyp;
    protected MeldelisteZiel ziel;
    protected PtmOnlineRegistrationMapping mapping;
    protected String turnierId;
    /** Zuletzt ausgeloste Schweizer Spielrunde. */
    protected SchweizerSpielrundeSheetNaechste letzteRunde;

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
        schweizerAnlegen("forme", formation, datum);
        ziel = MeldelisteZielFactory.fuerPtmOnline(wkingSpreadsheet).orElseThrow();
    }

    /**
     * Schweizer mit Mêlée-Anmeldung: online melden sich Einzelne an, lokal stehen sie in der Mêlée-Anmeldeliste
     * (Sync-Ziel), die Teams der Formation entstehen erst beim Übernehmen in die Meldeliste.
     */
    protected void meleeTurnierAnlegen(Formation formation, LocalDate datum) throws Exception {
        schweizerAnlegen("melee", formation, datum);
        MeleeAnmeldungKonfiguration.einschalten(wkingSpreadsheet);
        new SchweizerMeleeAnmeldungSheet(wkingSpreadsheet).generate();
        ziel = MeldelisteZielFactory.fuerPtmOnline(wkingSpreadsheet).orElseThrow();
    }

    private void schweizerAnlegen(String typ, Formation formation, LocalDate datum) throws Exception {
        system = TurnierSystem.SCHWEIZER;
        spieltagNr = null;
        anmeldeTyp = typ;
        onlineTyp = "schweizer";
        turnierId = online.turnierAnlegen("E2E " + getClass().getSimpleName(), "schweizer", anmeldeTyp,
                formation.name().toLowerCase(Locale.ROOT), datum);
        new SchweizerMeldeListeSheetNew(wkingSpreadsheet).createMeldelisteWithParams(formation, false, false);
        docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM, system.getId());
    }

    /** Leere Supermêlée-Meldeliste (Triplette) mit Spieltag 1 als aktivem Spieltag. */
    protected void supermeleeMeldelisteAnlegen() throws Exception {
        system = TurnierSystem.SUPERMELEE;
        anmeldeTyp = "supermelee";
        onlineTyp = "rangliste";
        new MeldeListeSheet_New(wkingSpreadsheet).createMeldelisteWithParams(SuperMeleeMode.Triplette);
        docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM, system.getId());
        aktivenSpieltagUebernehmen();
    }

    /**
     * Jeder Supermêlée-Spieltag ist online ein eigenes Turnier (E-25): legt es an; verbunden wird es mit dem aktiven
     * Spieltag.
     */
    protected void supermeleeSpieltagOnlineAnlegen(LocalDate datum) throws Exception {
        turnierId = online.turnierAnlegen("E2E Supermêlée Spieltag " + spieltagNr, "rangliste", "supermelee",
                "triplette", datum);
    }

    /** Nach einem Spieltagwechsel: Spieltag-Nr und Sync-Ziel des neuen aktiven Spieltags. */
    protected void aktivenSpieltagUebernehmen() throws Exception {
        spieltagNr = new SuperMeleeKonfigurationSheet(wkingSpreadsheet).getAktiveSpieltag().getNr();
        ziel = MeldelisteZielFactory.fuerPtmOnline(wkingSpreadsheet).orElseThrow();
        mapping = null;
    }

    /** Wie „Verbinden“ im Menü: Server-Bindung, Blatt „PTMOnline Sync“, Schreib-Lease. */
    protected void verbinden() throws Exception {
        OnlineTournamentDto turnier = new OnlineTournamentDto();
        turnier.id = turnierId;
        turnier.name = "E2E";
        turnier.type = onlineTyp;
        turnier.registrationType = anmeldeTyp;
        turnier.status = "registration";
        assertThat(PtmOnlineVerbindenTestZugang.verbinden(wkingSpreadsheet, system, spieltagNr,
                server.zugangsdaten(), turnier)).as("Verbinden erfolgreich").isTrue();
        mapping = new PtmOnlineRegistrationMapping(wkingSpreadsheet, system, spieltagNr);
        assertThat(mapping.getTournamentId()).contains(turnierId);
    }

    /** Manueller Abgleich (Menü „Abgleichen“). */
    protected void abgleichen() throws Exception {
        PtmOnlineAbgleichSheetRunner runner = new PtmOnlineAbgleichSheetRunner(wkingSpreadsheet, system, spieltagNr,
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

    /**
     * Check-in wie die Turnierleitung vor Ort: Aktiv-Spalte bzw. bei Supermêlée die Spalte des aktiven Spieltags
     * (Eintrag = am Spieltag gemeldet).
     */
    protected void einchecken(int zeile1Basiert) throws Exception {
        if (system == TurnierSystem.SUPERMELEE) {
            MeldeListeSheet_Update meldeliste = new MeldeListeSheet_Update(wkingSpreadsheet);
            sheetHlp.setNumberValueInCell(NumberCellValue.from(meldeliste.getXSpreadSheet(),
                    Position.from(meldeliste.spieltagSpalte(SpielTagNr.from(spieltagNr)), zeile1Basiert - 1))
                    .setValue(1));
            return;
        }
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

    /** Nächste Schweizer Spielrunde wie über das Menü; liefert die ausgelosten Team-Nummern. */
    protected Set<Integer> schweizerRundeAuslosen() throws Exception {
        SchweizerSpielrundeSheetNaechste runde = new SchweizerSpielrundeSheetNaechste(wkingSpreadsheet);
        runde.getKonfigurationSheet().setSpielrundeSpielbahn(SpielrundeSpielbahn.N);
        runde.doRun();
        assertThat(runde.getXSpreadSheet()).as("Spielrunde angelegt").isNotNull();
        letzteRunde = runde;
        int erste = SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE;
        Set<Integer> teams = new HashSet<>();
        for (RowData zeile : RangeHelper.from(runde.getXSpreadSheet(), wkingSpreadsheet.getWorkingSpreadsheetDocument(),
                RangePosition.from(SchweizerAbstractSpielrundeSheet.TEAM_A_SPALTE, erste,
                        SchweizerAbstractSpielrundeSheet.TEAM_B_SPALTE, erste + 10))
                .getDataFromRange()) {
            for (int spalte = 0; spalte < 2; spalte++) {
                int nr = zeile.get(spalte).getIntVal(0);
                if (nr > 0) {
                    teams.add(nr);
                }
            }
        }
        return teams;
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
