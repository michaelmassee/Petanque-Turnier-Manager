/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.onlinesync.OnlineTournamentDto;
import de.petanqueturniermanager.ptmonline.auftrag.AuftragsBestand;
import de.petanqueturniermanager.ptmonline.dto.SyncBindingDto;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerKonfigurationSheet;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetNew;

/**
 * Das (Wieder-)Verbinden läuft unter der Versandsperre der Bindung: Ein Hintergrundversand wartet, bis die neue
 * Bindung samt Schreibzähler eingetragen ist, und bekommt so nie denselben Zählerstand wie der nächste neue Auftrag.
 */
class PtmOnlineUnterVersandsperreUITest extends BaseCalcUITest {

    private static final String DOCUMENT_ID = "e9e9caec-e0b1-4fe0-8fee-a229279b9f73";
    private static final String LEASE_TOKEN = "01234567890123456789012345678901";
    private static final long SERVER_ZAEHLER = 7;

    private PtmOnlineRegistrationMapping mapping;

    @BeforeEach
    void verbundenesSchweizerTurnier() throws GenerateException {
        new SchweizerKonfigurationSheet(wkingSpreadsheet).update();
        new SchweizerMeldeListeSheetNew(wkingSpreadsheet).createMeldelisteWithParams(Formation.DOUBLETTE, false, false);
        mapping = new PtmOnlineRegistrationMapping(wkingSpreadsheet, TurnierSystem.SCHWEIZER, null);
        OnlineTournamentDto turnier = new OnlineTournamentDto();
        turnier.id = "t1";
        turnier.name = "Testturnier";
        mapping.verbinden(turnier, new SyncBindingDto(true, DOCUMENT_ID, 1), LEASE_TOKEN);
    }

    @Test
    void hintergrundversandKommtErstNachDerNeuenBindungZumZug() throws Exception {
        AuftragsBestand bestand = PtmOnlineAuftraege.bestand(wkingSpreadsheet, mapping, null);

        boolean versandWaehrendDesVerbindens = PtmOnlineAuftraege.unterVersandsperre(wkingSpreadsheet, mapping, null,
                () -> {
                    boolean frei = versandsperreAusAnderemThreadFrei(bestand);
                    PtmOnlineAuftraege.neueBindung(wkingSpreadsheet, mapping, null, SERVER_ZAEHLER, false,
                            "neue Bindung");
                    return frei;
                });

        assertThat(versandWaehrendDesVerbindens).as("Hintergrundversand während des Verbindens").isFalse();
        assertThat(versandsperreAusAnderemThreadFrei(bestand)).as("Sperre nach dem Verbinden wieder frei").isTrue();
        assertThat(bestand.zaehler()).isEqualTo(SERVER_ZAEHLER);
    }

    /** Wie der Hintergrund-Versand: aus einem fremden Thread, ohne zu warten. */
    private static boolean versandsperreAusAnderemThreadFrei(AuftragsBestand bestand) {
        return CompletableFuture.supplyAsync(() -> {
            boolean frei = bestand.versandSperre().tryLock();
            if (frei) {
                bestand.versandSperre().unlock();
            }
            return frei;
        }).orTimeout(10, TimeUnit.SECONDS).join();
    }
}
