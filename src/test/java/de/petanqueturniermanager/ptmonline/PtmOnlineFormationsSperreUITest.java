/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.star.frame.XController;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XModel;
import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.NrUuidSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.OfficeDocumentHelper;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.onlinesync.OnlineTournamentDto;
import de.petanqueturniermanager.ptmonline.dto.SyncBindingDto;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerKonfigurationSheet;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetNew;

/**
 * Formation im Dokument: fest, sobald das Dokument verbunden ist und ihm Anmeldungen zugeordnet sind – auch ohne
 * Netz, nur anhand des Blatts „PTMOnline Sync“.
 */
class PtmOnlineFormationsSperreUITest extends BaseCalcUITest {

    private static final String DOCUMENT_ID = "e9e9caec-e0b1-4fe0-8fee-a229279b9f73";
    private static final String LEASE_TOKEN = "01234567890123456789012345678901";
    private static final String LOKALE_UUID = "3b0b8a3e-6c52-4f2d-9d57-1c2c7c1d9e10";

    private XSpreadsheetDocument zweitesDokument;

    @BeforeEach
    void schweizerTurnierAnlegen() throws Exception {
        schweizerTurnier(wkingSpreadsheet);
    }

    @AfterEach
    void zweitesDokumentSchliessen() {
        if (zweitesDokument != null) {
            OfficeDocumentHelper.closeDoc(zweitesDokument);
            zweitesDokument = null;
        }
    }

    @Test
    void liestDieFormationDesDokuments() {
        assertThat(PtmOnlineFormation.vergleichswertAusDokument(wkingSpreadsheet, TurnierSystem.SCHWEIZER))
                .contains("doublette");
        assertThat(PtmOnlineFormation.vergleichswertAusDokument(wkingSpreadsheet, TurnierSystem.LIGA)).isEmpty();
    }

    @Test
    void ohneVerbindungIstDieFormationFrei() throws Exception {
        assertThat(PtmOnlineFormationsSperre.istGesperrt(wkingSpreadsheet, TurnierSystem.SCHWEIZER)).isFalse();
        assertThatCode(() -> PtmOnlineFormationsSperre.pruefeAenderung(wkingSpreadsheet, TurnierSystem.SCHWEIZER,
                Formation.TRIPLETTE)).doesNotThrowAnyException();
    }

    @Test
    void verbundenOhneAnmeldungenIstDieFormationFrei() throws Exception {
        verbinde(wkingSpreadsheet);

        assertThat(PtmOnlineFormationsSperre.istGesperrt(wkingSpreadsheet, TurnierSystem.SCHWEIZER)).isFalse();
    }

    @Test
    void verbundenMitAnmeldungenIstDieFormationGesperrt() throws Exception {
        ordneAnmeldungZu(verbinde(wkingSpreadsheet));

        assertThat(PtmOnlineFormationsSperre.istGesperrt(wkingSpreadsheet, TurnierSystem.SCHWEIZER)).isTrue();
        assertThat(PtmOnlineFormationsSperre.istAenderbar(wkingSpreadsheet, TurnierSystem.SCHWEIZER)).isFalse();
        assertThatThrownBy(() -> PtmOnlineFormationsSperre.pruefeAenderung(wkingSpreadsheet, TurnierSystem.SCHWEIZER,
                Formation.TRIPLETTE)).isInstanceOf(GenerateException.class)
                .hasMessage(I18n.get("ptmonline.formation.gesperrt", Formation.DOUBLETTE.getBezeichnung()));
        assertThatCode(() -> PtmOnlineFormationsSperre.pruefeAenderung(wkingSpreadsheet, TurnierSystem.SCHWEIZER,
                Formation.DOUBLETTE)).as("unveränderte Formation").doesNotThrowAnyException();
    }

    @Test
    void anderesTurniersystemIstNieGesperrt() throws Exception {
        ordneAnmeldungZu(verbinde(wkingSpreadsheet));

        assertThat(PtmOnlineFormationsSperre.istGesperrt(wkingSpreadsheet, TurnierSystem.KO)).isFalse();
    }

    /** Die Sperre gilt dem gemeinten Dokument, auch wenn ein anderes, gesperrtes Dokument den Fokus hält. */
    @Test
    void sperreGiltNurFuerDasGemeinteDokument() throws Exception {
        ordneAnmeldungZu(verbinde(wkingSpreadsheet));
        zweitesDokument = OfficeDocumentHelper.from(loader).createSichtbaresCalc();
        WorkingSpreadsheet zweitesWs = new WorkingSpreadsheet(starter.getxComponentContext(), zweitesDokument);
        schweizerTurnier(zweitesWs);
        verbinde(zweitesWs);

        fokussiere(doc);

        assertThat(PtmOnlineFormationsSperre.istGesperrt(zweitesWs, TurnierSystem.SCHWEIZER)).isFalse();
        assertThat(PtmOnlineFormationsSperre.istGesperrt(wkingSpreadsheet, TurnierSystem.SCHWEIZER)).isTrue();
    }

    private static void schweizerTurnier(WorkingSpreadsheet ws) throws GenerateException {
        new SchweizerKonfigurationSheet(ws).update();
        new SchweizerMeldeListeSheetNew(ws).createMeldelisteWithParams(Formation.DOUBLETTE, false, false);
    }

    private static PtmOnlineRegistrationMapping verbinde(WorkingSpreadsheet ws) throws GenerateException {
        PtmOnlineRegistrationMapping mapping = new PtmOnlineRegistrationMapping(ws, TurnierSystem.SCHWEIZER, null);
        OnlineTournamentDto turnier = new OnlineTournamentDto();
        turnier.id = "t1";
        turnier.name = "Testturnier";
        mapping.verbinden(turnier, new SyncBindingDto(true, DOCUMENT_ID, 1), LEASE_TOKEN);
        return mapping;
    }

    private static void ordneAnmeldungZu(PtmOnlineRegistrationMapping mapping) throws GenerateException {
        mapping.addMapping(LOKALE_UUID, "r1", NrUuidSpalte.formelNrAusUuid("$A$1:$A$999", LOKALE_UUID), 1,
                "Hans Müller", "Bestätigt");
    }

    private static void fokussiere(XSpreadsheetDocument dokument) {
        XController controller = Lo.qi(XModel.class, dokument).getCurrentController();
        XFrame frame = controller.getFrame();
        frame.activate();
    }
}
