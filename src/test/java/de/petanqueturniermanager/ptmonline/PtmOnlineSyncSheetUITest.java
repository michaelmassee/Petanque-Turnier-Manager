/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.util.XProtectable;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzManager;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzRegistry;
import de.petanqueturniermanager.onlinesync.OnlineTournamentDto;
import de.petanqueturniermanager.onlinesync.sheet.PtmOnlineSyncSheet;
import de.petanqueturniermanager.onlinesync.sheet.PtmOnlineSyncStatus;
import de.petanqueturniermanager.ptmonline.dto.RegistrationDto;
import de.petanqueturniermanager.ptmonline.dto.SyncBindingDto;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetNew;
import de.petanqueturniermanager.toolbar.TurnierModus;

/**
 * Das Blatt „PTMOnline Sync“: genau ein Blatt je Verbindung, bei Supermelee eines je Spieltag.
 */
class PtmOnlineSyncSheetUITest extends BaseCalcUITest {

    private static final String DOCUMENT_ID = "e9e9caec-e0b1-4fe0-8fee-a229279b9f73";
    private static final String LEASE_TOKEN = "01234567890123456789012345678901";
    private static final String LOKALE_UUID = "3b0b8a3e-6c52-4f2d-9d57-1c2c7c1d9e10";

    /** Im echten Dokument gibt es immer eine Meldeliste – LO löscht nie das letzte Blatt. */
    @BeforeEach
    void meldelisteAnlegen() throws Exception {
        new SchweizerMeldeListeSheetNew(wkingSpreadsheet).createMeldelisteWithParams(Formation.TETE, false, false);
    }

    @Test
    void verbindenLegtEinBlattAnUndTrennenEntferntEs() throws Exception {
        PtmOnlineRegistrationMapping mapping = verbinde(TurnierSystem.SCHWEIZER, null, "t1");

        assertThat(sheetVorhanden(SheetMetadataHelper.SCHLUESSEL_PTM_ONLINE_SYNC)).isTrue();
        assertThat(sheetHlp.findByName(SheetNamen.ptmOnlineSync())).isNotNull();
        assertThat(mapping.getTournamentId()).contains("t1");
        assertThat(mapping.getSyncDocumentId()).contains(DOCUMENT_ID);
        assertThat(mapping.getLeaseToken()).contains(LEASE_TOKEN);

        mapping.trennen();

        assertThat(sheetVorhanden(SheetMetadataHelper.SCHLUESSEL_PTM_ONLINE_SYNC)).isFalse();
    }

    @Test
    void stornoWirdAmUnuebersetztenStatusErkannt() throws Exception {
        PtmOnlineRegistrationMapping mapping = verbinde(TurnierSystem.SCHWEIZER, null, "t1");
        mapping.addMapping(LOKALE_UUID, "r1", "1", 1, "Hans Müller", "Bestätigt");

        mapping.setOnlineDetails(LOKALE_UUID, anmeldung("r1", "confirmed"));
        assertThat(mapping.istOnlineStorniert(LOKALE_UUID)).isFalse();

        mapping.setOnlineDetails(LOKALE_UUID, anmeldung("r1", "cancelled"));
        assertThat(mapping.istOnlineStorniert(LOKALE_UUID)).isTrue();
    }

    @Test
    void jederSpieltagHatSeinEigenesBlatt() throws Exception {
        PtmOnlineRegistrationMapping spieltag1 = verbinde(TurnierSystem.SUPERMELEE, 1, "t1");
        PtmOnlineRegistrationMapping spieltag10 = verbinde(TurnierSystem.SUPERMELEE, 10, "t10");

        assertThat(sheetHlp.findByName(SheetNamen.ptmOnlineSync(1))).isNotNull();
        assertThat(sheetHlp.findByName(SheetNamen.ptmOnlineSync(10))).isNotNull();
        assertThat(spieltag1.getTournamentId()).contains("t1");
        assertThat(spieltag10.getTournamentId()).contains("t10");

        spieltag1.trennen();

        assertThat(sheetVorhanden(SheetMetadataHelper.schluesselPtmOnlineSync(1))).isFalse();
        assertThat(sheetVorhanden(SheetMetadataHelper.schluesselPtmOnlineSync(10)))
                .as("Trennen von Spieltag 1 lässt Spieltag 10 stehen").isTrue();
        assertThat(spieltag10.getTournamentId()).contains("t10");
    }

    @Test
    void pauseBleibtVerbundenUndStopptDenRundenstartSync() throws Exception {
        PtmOnlineRegistrationMapping mapping = verbinde(TurnierSystem.SCHWEIZER, null, "t1");
        assertThat(PtmOnlineSpielrundeSync.istSyncAktiv(mapping)).isTrue();

        mapping.setPausiert(true);

        assertThat(mapping.istPausiert()).isTrue();
        assertThat(mapping.getTournamentId()).as("Verbindung bleibt bestehen").contains("t1");
        assertThat(PtmOnlineSpielrundeSync.istSyncAktiv(mapping)).isFalse();
        assertThat(PtmOnlineSyncSheet.leseStatus(wkingSpreadsheet, null))
                .isEqualTo(new PtmOnlineSyncStatus(true, true, Optional.empty()));

        mapping.setPausiert(false);

        assertThat(mapping.istPausiert()).isFalse();
        assertThat(PtmOnlineSpielrundeSync.istSyncAktiv(mapping)).isTrue();
    }

    @Test
    void neuesVerbindenHebtDiePauseAuf() throws Exception {
        PtmOnlineRegistrationMapping mapping = verbinde(TurnierSystem.SCHWEIZER, null, "t1");
        mapping.setPausiert(true);

        PtmOnlineRegistrationMapping neu = verbinde(TurnierSystem.SCHWEIZER, null, "t1");

        assertThat(neu.istPausiert()).isFalse();
    }

    @Test
    void pausierenImTurnierModusSchreibtInsGesperrteBlatt() throws Exception {
        PtmOnlineRegistrationMapping mapping = verbinde(TurnierSystem.SCHWEIZER, null, "t1");
        docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM,
                TurnierSystem.SCHWEIZER.getId());
        var konfig = BlattschutzRegistry.fuer(TurnierSystem.SCHWEIZER).orElseThrow();
        TurnierModus.get().setAktivForTest(true);
        try {
            BlattschutzManager.get().schuetzen(konfig, wkingSpreadsheet);
            BlattschutzManager.get().beginCommandScope(konfig, wkingSpreadsheet);
            try {
                mapping.setPausiert(true);
            } finally {
                BlattschutzManager.get().endCommandScope();
            }

            assertThat(mapping.istPausiert()).isTrue();
            XSpreadsheet sync = SheetMetadataHelper.findeSheet(wkingSpreadsheet.getWorkingSpreadsheetDocument(),
                    SheetMetadataHelper.SCHLUESSEL_PTM_ONLINE_SYNC).orElseThrow();
            assertThat(Lo.qi(XProtectable.class, sync).isProtected()).as("danach wieder gesperrt").isTrue();
        } finally {
            BlattschutzManager.get().entsperren(konfig, wkingSpreadsheet);
            TurnierModus.get().setAktivForTest(false);
        }
    }

    private PtmOnlineRegistrationMapping verbinde(TurnierSystem system, Integer spieltagNr, String turnierId)
            throws Exception {
        PtmOnlineRegistrationMapping mapping = new PtmOnlineRegistrationMapping(wkingSpreadsheet, system, spieltagNr);
        OnlineTournamentDto turnier = new OnlineTournamentDto();
        turnier.id = turnierId;
        turnier.name = "Testturnier " + turnierId;
        mapping.verbinden(turnier, new SyncBindingDto(true, DOCUMENT_ID, 1), LEASE_TOKEN);
        return mapping;
    }

    private boolean sheetVorhanden(String schluessel) {
        return SheetMetadataHelper.findeSheet(wkingSpreadsheet.getWorkingSpreadsheetDocument(), schluessel).isPresent();
    }

    private static RegistrationDto anmeldung(String id, String status) {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("status", status);
        return new Gson().fromJson(json, RegistrationDto.class);
    }
}
