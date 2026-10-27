package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.TeamAnzeige;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.GlobalProperties;
import de.petanqueturniermanager.comp.LibreOfficePtmOnlineSpeicher;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.formulex.meldeliste.FormuleXMeldeListeSheetNew;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.jedergegenjeden.meldeliste.JGJMeldeListeSheet_New;
import de.petanqueturniermanager.kaskade.meldeliste.KaskadeMeldeListeSheetNew;
import de.petanqueturniermanager.ko.meldeliste.KoMeldeListeSheetNew;
import de.petanqueturniermanager.liga.meldeliste.LigaMeldeListeSheetNew;
import de.petanqueturniermanager.maastrichter.meldeliste.MaastrichterMeldeListeSheetNew;
import de.petanqueturniermanager.onlinesync.OnlineTournamentDto;
import de.petanqueturniermanager.poule.meldeliste.PouleMeldeListeSheetNew;
import de.petanqueturniermanager.ptmonline.dto.SyncBindingDto;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetNew;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeKonfigurationSheet;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeMode;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_New;
import de.petanqueturniermanager.triptete.meldeliste.TripTeteMeldeListeSheetNew;

class PtmOnlineOhneVerbindungUITest extends BaseCalcUITest {

    @ParameterizedTest
    @EnumSource(value = TurnierSystem.class, names = "KEIN", mode = EnumSource.Mode.EXCLUDE)
    void ohneKeyOderMitDeaktivierterOptionBleibtAuchVerbundenesTurnierOhneOnlineArbeit(TurnierSystem system)
            throws Exception {
        legeTurnierAn(system);
        OnlineTournamentDto turnier = new OnlineTournamentDto();
        turnier.id = "t1";
        turnier.name = "Verbundenes Testturnier";
        new PtmOnlineRegistrationMapping(wkingSpreadsheet, system, system.hatMehrereSpielTage() ? 1 : null)
                .verbinden(turnier, new SyncBindingDto(true, "e9e9caec-e0b1-4fe0-8fee-a229279b9f73", 1),
                        "01234567890123456789012345678901");
        var beobachter = PtmOnlineLiveBeobachter.init(starter.getxComponentContext());
        for (var daten : java.util.List.of(
                new LibreOfficePtmOnlineSpeicher.Zugangsdaten("", "http://127.0.0.1", true),
                new LibreOfficePtmOnlineSpeicher.Zugangsdaten("test", "http://127.0.0.1", false))) {
            LibreOfficePtmOnlineSpeicher.setZugangsdatenForTest(daten);
            beobachter.onLoad(doc);
            PtmOnlineLiveBeobachter.anstossen(doc);
            assertKeineBeobachtungOderAuftraege(beobachter);
            XComponentContext kontext = mock(XComponentContext.class);
            assertThat(PtmOnlineVerbindung.ermitteln(new WorkingSpreadsheet(kontext, doc), system)).isEmpty();
            verifyNoInteractions(kontext);
        }
    }

    @BeforeEach
    void onlineZugangVorhanden() {
        LibreOfficePtmOnlineSpeicher.setZugangsdatenForTest(
                new LibreOfficePtmOnlineSpeicher.Zugangsdaten("test", "http://127.0.0.1"));
    }

    @AfterEach
    void onlineZugangZuruecksetzen() {
        LibreOfficePtmOnlineSpeicher.setZugangsdatenForTest(null);
    }

    @Test
    void supermeleeVerbindungEinesAnderenSpieltagsStartetKeinenLauf() throws Exception {
        legeTurnierAn(TurnierSystem.SUPERMELEE);
        OnlineTournamentDto turnier = new OnlineTournamentDto();
        turnier.id = "t1";
        turnier.name = "Spieltag 1";
        new PtmOnlineRegistrationMapping(wkingSpreadsheet, TurnierSystem.SUPERMELEE, 1)
                .verbinden(turnier, new SyncBindingDto(true, "e9e9caec-e0b1-4fe0-8fee-a229279b9f73", 1),
                        "01234567890123456789012345678901");
        assertThat(PtmOnlineVerbindung.hatVerbindung(wkingSpreadsheet, TurnierSystem.SUPERMELEE)).isTrue();

        new SuperMeleeKonfigurationSheet(wkingSpreadsheet).setAktiveSpieltag(SpielTagNr.from(2));

        assertThat(PtmOnlineVerbindung.hatVerbindung(wkingSpreadsheet, TurnierSystem.SUPERMELEE)).isFalse();
        XComponentContext kontext = mock(XComponentContext.class);
        assertThat(PtmOnlineVerbindung.ermitteln(new WorkingSpreadsheet(kontext, doc), TurnierSystem.SUPERMELEE))
                .isEmpty();
        verifyNoInteractions(kontext);
        PtmOnlineLiveBeobachter beobachter = PtmOnlineLiveBeobachter.init(starter.getxComponentContext());
        beobachter.onLoad(doc);
        PtmOnlineLiveBeobachter.anstossen(doc);
        assertKeineBeobachtungOderAuftraege(beobachter);
    }

    @ParameterizedTest
    @EnumSource(value = TurnierSystem.class, names = "KEIN", mode = EnumSource.Mode.EXCLUDE)
    void ohneVerbindungWirdNichtBeobachtetUndPlantKeinenLauf(TurnierSystem system) throws Exception {
        legeTurnierAn(system);
        PtmOnlineLiveBeobachter beobachter = PtmOnlineLiveBeobachter.init(starter.getxComponentContext());

        beobachter.onLoad(doc);
        PtmOnlineLiveBeobachter.anstossen(doc);

        assertKeineBeobachtungOderAuftraege(beobachter);
    }

    private static void assertKeineBeobachtungOderAuftraege(PtmOnlineLiveBeobachter beobachter) throws Exception {
        var beobachtungen = PtmOnlineLiveBeobachter.class.getDeclaredField("beobachtungen");
        beobachtungen.setAccessible(true);
        assertThat((Map<?, ?>) beobachtungen.get(beobachter)).isEmpty();
        var executor = PtmOnlineLiveBeobachter.class.getDeclaredField("ausfuehrung");
        executor.setAccessible(true);
        ScheduledThreadPoolExecutor pool = (ScheduledThreadPoolExecutor) executor.get(beobachter);
        assertThat(pool.getQueue()).isEmpty();
        assertThat(pool.getActiveCount()).isZero();
    }

    @ParameterizedTest
    @EnumSource(value = TurnierSystem.class, names = "KEIN", mode = EnumSource.Mode.EXCLUDE)
    void ohneVerbindungWerdenKeineOnlineZugangsdatenGelesen(TurnierSystem system) throws Exception {
        legeTurnierAn(system);
        XComponentContext kontext = mock(XComponentContext.class);

        assertThat(PtmOnlineVerbindung.ermitteln(new WorkingSpreadsheet(kontext, doc), system))
                .isEmpty();
        verifyNoInteractions(kontext);
    }

    @ParameterizedTest
    @EnumSource(value = TurnierSystem.class, names = "KEIN", mode = EnumSource.Mode.EXCLUDE)
    void stillerOnlineLaufAendertLokalesLogNichtUndVerlaengertAutoCloseNicht(TurnierSystem system) throws Exception {
        legeTurnierAn(system);
        // Nur die Test-JVM ändern, keine Plugin-Optionen ins Benutzerprofil schreiben.
        GlobalProperties.get();
        var feld = GlobalProperties.class.getDeclaredField("propMap");
        feld.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> optionen = (Map<String, String>) feld.get(null);
        String anzeigenKey = "prozessbox.automatisch.anzeigen";
        String schliessenKey = "prozessbox.automatisch.schliessen";
        String anzeigenVorher = optionen.put(anzeigenKey, "true");
        String schliessenVorher = optionen.put(schliessenKey, "true");
        ProcessBox.setHeadlessMode(false);
        try {
            ProcessBox.forceinit(starter.getxComponentContext());
            ProcessBox pb = ProcessBox.from();
            pb.clear().run().info("Lokaler Turnier-Lauf: " + system.name()).ready();
            pb.flushUiUpdatesForTest();
            assertThat(pb.istSichtbar()).isTrue();
            String logVorher = pb.getLogText();
            var autoClose = ProcessBox.class.getDeclaredField("autoCloseTask");
            autoClose.setAccessible(true);
            Object timerVorher = autoClose.get(pb);
            assertThat(timerVorher).isNotNull();

            PtmOnlineErfassungsRunner runner = new PtmOnlineErfassungsRunner(wkingSpreadsheet,
                    system, new PtmOnlineCheckin(), null, null);
            runner.startSilent();
            runner.join();
            pb.flushUiUpdatesForTest();

            assertThat(runner.isLetzterLaufFehlgeschlagen()).isFalse();
            assertThat(runner.erfassung()).isEmpty();
            assertThat(pb.getLogText()).isEqualTo(logVorher);
            assertThat(autoClose.get(pb)).as("Hintergrund darf den Schließtimer nicht neu starten")
                    .isSameAs(timerVorher);
            long ende = System.nanoTime() + java.time.Duration.ofSeconds(7).toNanos();
            while (pb.istSichtbar() && System.nanoTime() < ende) {
                Thread.sleep(100);
                pb.flushUiUpdatesForTest();
            }
            assertThat(pb.istSichtbar()).as("Lokales Logfenster schließt weiterhin automatisch").isFalse();
        } finally {
            ProcessBox.dispose();
            ProcessBox.setHeadlessMode(true);
            wiederherstellen(optionen, anzeigenKey, anzeigenVorher);
            wiederherstellen(optionen, schliessenKey, schliessenVorher);
        }
    }

    private void legeTurnierAn(TurnierSystem system) throws Exception {
        switch (system) {
            case SUPERMELEE -> new MeldeListeSheet_New(wkingSpreadsheet)
                    .createMeldelisteWithParams(SuperMeleeMode.Doublette);
            case LIGA -> new LigaMeldeListeSheetNew(wkingSpreadsheet).createMeldelisteWithParams("A");
            case MAASTRICHTER -> new MaastrichterMeldeListeSheetNew(wkingSpreadsheet)
                    .erstelleMeldeliste(Formation.TETE, false, false, TeamAnzeige.NR);
            case SCHWEIZER -> new SchweizerMeldeListeSheetNew(wkingSpreadsheet)
                    .createMeldelisteWithParams(Formation.TETE, false, false);
            case JGJ -> new JGJMeldeListeSheet_New(wkingSpreadsheet)
                    .createMeldelisteWithParams(Formation.TETE, false, false, TeamAnzeige.NR);
            case KO -> new KoMeldeListeSheetNew(wkingSpreadsheet).createMeldelisteWithParams();
            case POULE -> new PouleMeldeListeSheetNew(wkingSpreadsheet)
                    .createMeldelisteWithParams(Formation.TETE, false, false);
            case KASKADE -> new KaskadeMeldeListeSheetNew(wkingSpreadsheet)
                    .createMeldelisteWithParams(Formation.DOUBLETTE, false, false, 2);
            case FORMULEX -> new FormuleXMeldeListeSheetNew(wkingSpreadsheet)
                    .createMeldelisteWithParams(Formation.TRIPLETTE, false, false);
            case TRIPTETE -> new TripTeteMeldeListeSheetNew(wkingSpreadsheet).createMeldeliste();
            default -> throw new IllegalArgumentException("Kein Turniersystem: " + system);
        }
        docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM,
                system.getId());
    }

    private static void wiederherstellen(Map<String, String> optionen, String key, String vorher) {
        if (vorher == null) {
            optionen.remove(key);
        } else {
            optionen.put(key, vorher);
        }
    }
}
