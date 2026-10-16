package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.sun.star.util.XProtectable;
import com.sun.star.util.XModifiable;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.LibreOfficePtmOnlineSpeicher;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzManager;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzRegistry;
import de.petanqueturniermanager.onlinesync.OnlineTournamentDto;
import de.petanqueturniermanager.ptmonline.auftrag.AuftragsArt;
import de.petanqueturniermanager.ptmonline.auftrag.AuftragsBestand;
import de.petanqueturniermanager.ptmonline.auftrag.versand.SyncAntwort;
import de.petanqueturniermanager.ptmonline.auftrag.versand.VersandErgebnis;
import de.petanqueturniermanager.ptmonline.dto.SyncBindingDto;
import de.petanqueturniermanager.ptmonline.dto.SyncStandDto;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetNew;
import de.petanqueturniermanager.toolbar.TurnierModus;

class PtmOnlineErfassungsBlattschutzUITest extends BaseCalcUITest {
    private PtmOnlineRegistrationMapping mapping;
    private AuftragsBestand bestand;
    private final PtmOnlineCheckin checkin = new PtmOnlineCheckin();
    private final PtmOnlineErfassungsRunner.OnlineStandAbruf stand = new PtmOnlineErfassungsRunner.OnlineStandAbruf(
            "schutz-test", Optional.of(new SyncStandDto("running", true, "", 0, 0, null, false)));

    @BeforeEach
    void vorbereiten() throws Exception {
        LibreOfficePtmOnlineSpeicher.setZugangsdatenForTest(
                new LibreOfficePtmOnlineSpeicher.Zugangsdaten("test", "http://127.0.0.1", true));
        new SchweizerMeldeListeSheetNew(wkingSpreadsheet).createMeldelisteWithParams(Formation.TETE, false, false);
        docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM, TurnierSystem.SCHWEIZER.getId());
        OnlineTournamentDto turnier = new OnlineTournamentDto();
        turnier.id = "schutz-test";
        turnier.name = "Schutztest";
        mapping = new PtmOnlineRegistrationMapping(wkingSpreadsheet, TurnierSystem.SCHWEIZER, null);
        mapping.verbinden(turnier, new SyncBindingDto(true, "e9e9caec-e0b1-4fe0-8fee-a229279b9f73", 1),
                "01234567890123456789012345678901");
        bestand = PtmOnlineAuftraege.bestand(wkingSpreadsheet, mapping, null);
        erfasse(); // Initialen Snapshot und ggf. initialen Bestand vor der Messung erfassen.
        warteBisLoHintergrundFertig();
    }

    @AfterEach
    void aufraeumen() {
        PtmOnlineLiveSync.vergessen("schutz-test");
        PtmOnlineSpieleinstellungen.vergessen("schutz-test");
        PtmOnlineAuftraege.vergessen(doc);
        LibreOfficePtmOnlineSpeicher.setZugangsdatenForTest(null);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void unveraenderteVerbindungLaesstSchutzUnberuehrt(boolean pausiert) throws Exception {
        mapping.setPausiert(pausiert);
        geschuetzt(() -> {
            XModifiable zustand = Lo.qi(XModifiable.class, doc);
            zustand.setModified(false);
            int vorher = schutzAufrufe();
            for (int i = 0; i < 3; i++) {
                erfasse();
            }
            assertThat(schutzAufrufe()).as("Kein erneutes Entsperren/Sperren bei unverändertem Online-Stand")
                    .isEqualTo(vorher);
            assertThat(zustand.isModified()).as("Reine Erfassung verändert das Dokument nicht").isFalse();
        });
    }

    @Test
    void reinerLaufSchliesstKeinenAeusserenScope() throws Exception {
        mapping.setPausiert(true);
        geschuetzt(() -> {
            var konfig = BlattschutzRegistry.fuer(TurnierSystem.SCHWEIZER).orElseThrow();
            BlattschutzManager.get().beginCommandScope(konfig, wkingSpreadsheet);
            try {
                int vorher = schutzAufrufe();
                erfasse();
                assertThat(schutzAufrufe()).isEqualTo(vorher);
            } finally {
                BlattschutzManager.get().endCommandScope();
            }
        });
    }

    @Test
    void neuerAuftragWirdMitGenauEinemScopeGespeichert() throws Exception {
        mapping.setPausiert(true);
        bestand.erzeuge(AuftragsArt.RUNDE, "PUT", "/test", "{}", "{}");
        geschuetzt(() -> {
            int vorher = schutzAufrufe();
            erfasse();
            assertThat(schutzAufrufe()).isEqualTo(vorher + 1);
            assertThat(bestand.istGeaendert()).isFalse();
            // Neben dem Spieleinstellungen-Auftrag der ersten Erfassung genau der neue Auftrag.
            assertThat(mapping.leseAuftragsBestand().eintraege()).extracting(eintrag -> eintrag.auftrag().art())
                    .containsExactly(AuftragsArt.SPIELEINSTELLUNGEN, AuftragsArt.RUNDE);
            erfasse();
            assertThat(schutzAufrufe()).as("Der nächste unveränderte Lauf öffnet keinen Scope")
                    .isEqualTo(vorher + 1);
        });
    }

    @Test
    void versandergebnisWirdGeschuetztAngewendet() throws Exception {
        mapping.setPausiert(true);
        mapping.setRunningAusstehendSeit(Instant.parse("2026-10-13T10:00:00Z"));
        var auftrag = bestand.erzeuge(AuftragsArt.START, "POST", "/test", "{}", "{}");
        bestand.gesendet(new VersandErgebnis(auftrag, new SyncAntwort(200, "{}", false), true));
        geschuetzt(() -> {
            int vorher = schutzAufrufe();
            erfasse();
            assertThat(schutzAufrufe()).isEqualTo(vorher + 1);
            assertThat(mapping.getRunningAusstehendSeit()).isEmpty();
            assertThat(bestand.ergebnisseZumAnwenden()).isEmpty();
            erfasse();
            assertThat(schutzAufrufe()).isEqualTo(vorher + 1);
        });
    }

    private void erfasse() {
        var runner = new PtmOnlineErfassungsRunner(wkingSpreadsheet, TurnierSystem.SCHWEIZER, checkin, stand, null);
        runner.run();
        assertThat(runner.isLetzterLaufFehlgeschlagen()).isFalse();
        assertThat(runner.erfassung()).isPresent();
    }

    private int schutzAufrufe() throws Exception {
        var getter = BlattschutzManager.class.getDeclaredMethod("getProtectCallCount");
        getter.setAccessible(true);
        return (Integer) getter.invoke(BlattschutzManager.get());
    }

    @FunctionalInterface
    private interface Aktion {
        void ausfuehren() throws Exception;
    }

    private void geschuetzt(Aktion aktion) throws Exception {
        var konfig = BlattschutzRegistry.fuer(TurnierSystem.SCHWEIZER).orElseThrow();
        TurnierModus.get().setAktivForTest(true);
        BlattschutzManager.get().schuetzen(konfig, wkingSpreadsheet);
        try {
            aktion.ausfuehren();
            for (var info : konfig.berechneSchutzInfos(wkingSpreadsheet)) {
                assertThat(Lo.qi(XProtectable.class, info.sheet()).isProtected()).isTrue();
            }
        } finally {
            BlattschutzManager.get().endCommandScope();
            BlattschutzManager.get().entsperren(konfig, wkingSpreadsheet);
            TurnierModus.get().setAktivForTest(false);
        }
    }
}
