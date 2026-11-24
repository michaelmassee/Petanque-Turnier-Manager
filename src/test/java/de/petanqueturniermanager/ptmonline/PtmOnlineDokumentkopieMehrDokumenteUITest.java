/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.star.awt.XTopWindow;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XModel;
import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.OfficeDocumentHelper;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.onlinesync.OnlineTournamentDto;
import de.petanqueturniermanager.ptmonline.auftrag.AuftragsArt;
import de.petanqueturniermanager.ptmonline.auftrag.AuftragsBestand;
import de.petanqueturniermanager.ptmonline.dto.SpieleinstellungenDto;
import de.petanqueturniermanager.ptmonline.dto.SyncBindingDto;
import de.petanqueturniermanager.ptmonline.live.LivePartie;
import de.petanqueturniermanager.ptmonline.live.LiveRunde;
import de.petanqueturniermanager.ptmonline.live.LiveTurnierStand;

/**
 * Regression: Live-Stand, Spieleinstellungen und entschiedene Ergebnismeldungen merkte sich PTM je Online-Turnier.
 * War eine Kopie des Turnierdokuments geöffnet, erfasste sie z.&nbsp;B. 13:8, bevor PTM-Online ihre Aufträge als
 * Dokumentkopie ablehnte. Stellte danach das Original auf 13:8, entstand dort kein Auftrag – online blieb 13:7.
 * <p>
 * Zwei Dokumente sind gleichzeitig offen und mit demselben Online-Turnier verbunden; der Fokus liegt jeweils auf dem
 * „falschen“ Dokument. Jedes Dokument muss nach seinem eigenen Stand erfassen.
 */
class PtmOnlineDokumentkopieMehrDokumenteUITest extends BaseCalcUITest {

    private static final String TURNIER_ID = "dokumentkopie-test";
    private static final Map<Integer, List<String>> ONLINE_IDS = Map.of(1, List.of("r1"), 2, List.of("r2"));
    private static final SpieleinstellungenDto EINSTELLUNGEN = new SpieleinstellungenDto("triplette", "mit_buchholz",
            null, null);

    private XSpreadsheetDocument kopie;
    private AuftragsBestand originalBestand;
    private AuftragsBestand kopieBestand;

    @BeforeEach
    void zweiVerbundeneDokumente() throws Exception {
        originalBestand = verbinde(wkingSpreadsheet);
        kopie = OfficeDocumentHelper.from(loader).createSichtbaresCalc();
        assertThat(kopie).as("zweites Dokument konnte nicht erstellt werden").isNotNull();
        kopieBestand = verbinde(new WorkingSpreadsheet(starter.getxComponentContext(), kopie));
        assertThat(kopieBestand).as("jedes Dokument hat seinen eigenen Auftragspuffer").isNotSameAs(originalBestand);
    }

    @AfterEach
    void aufraeumen() {
        PtmOnlineAuftraege.vergessen(doc);
        if (kopie != null) {
            PtmOnlineAuftraege.vergessen(kopie);
            OfficeDocumentHelper.closeDoc(kopie);
            kopie = null;
        }
    }

    @Test
    void liveStandDerKopieUnterdruecktDieUebertragungDesOriginalsNicht() {
        PtmOnlineLiveSync.erfasse(originalBestand, TURNIER_ID, stand(7), OptionalInt.of(1));
        fokussiere(doc);
        PtmOnlineLiveSync.erfasse(kopieBestand, TURNIER_ID, stand(8), OptionalInt.of(1));
        fokussiere(kopie);

        PtmOnlineLiveSync.erfasse(originalBestand, TURNIER_ID, stand(8), OptionalInt.of(1));

        assertThat(originalBestand.zuSenden(false)).filteredOn(auftrag -> auftrag.art() == AuftragsArt.RUNDE)
                .as("das Original überträgt 13:8").singleElement()
                .satisfies(auftrag -> assertThat(auftrag.body()).contains("\"scoreB\":8"));
    }

    @Test
    void vergessenDerKopieLaesstDasOriginalUnberuehrt() {
        PtmOnlineLiveSync.erfasse(originalBestand, TURNIER_ID, stand(7), OptionalInt.of(1));
        PtmOnlineLiveSync.erfasse(kopieBestand, TURNIER_ID, stand(7), OptionalInt.of(1));
        fokussiere(doc);

        PtmOnlineLiveSync.vergessen(kopie, TURNIER_ID);

        assertThat(PtmOnlineLiveSync.hatStand(kopieBestand, TURNIER_ID)).isFalse();
        assertThat(PtmOnlineLiveSync.hatStand(originalBestand, TURNIER_ID)).isTrue();
    }

    @Test
    void spieleinstellungenUndEntscheidungenDerKopieGeltenNichtFuerDasOriginal() {
        fokussiere(doc);
        PtmOnlineSpieleinstellungen.erfasse(kopieBestand, TURNIER_ID, EINSTELLUNGEN);
        PtmOnlineErgebnismeldungen.merkeEntschieden(kopieBestand, TURNIER_ID, List.of("meldung-1"));
        fokussiere(kopie);

        assertThat(PtmOnlineSpieleinstellungen.erfasse(originalBestand, TURNIER_ID, EINSTELLUNGEN))
                .as("Einstellungen des Originals werden erfasst").isTrue();
        assertThat(PtmOnlineErgebnismeldungen.unentschieden(originalBestand, TURNIER_ID,
                List.of(PtmOnlineErgebnismeldungenTest.meldung("meldung-1", 1, "r1", "r2", 13, 7))))
                .as("im Original noch offen").hasSize(1);
    }

    private AuftragsBestand verbinde(WorkingSpreadsheet ws) throws Exception {
        OnlineTournamentDto turnier = new OnlineTournamentDto();
        turnier.id = TURNIER_ID;
        turnier.name = "Dokumentkopie";
        PtmOnlineRegistrationMapping mapping = new PtmOnlineRegistrationMapping(ws, TurnierSystem.SCHWEIZER, null);
        mapping.verbinden(turnier, new SyncBindingDto(true, "e9e9caec-e0b1-4fe0-8fee-a229279b9f73", 1),
                "01234567890123456789012345678901");
        return PtmOnlineAuftraege.bestand(ws, mapping, null);
    }

    private static PtmOnlineLiveSync.Momentaufnahme stand(int punkteB) {
        return new PtmOnlineLiveSync.Momentaufnahme(LiveTurnierStand.ohneRangliste(
                List.of(new LiveRunde(1, List.of(LivePartie.teams(1, 2, 13, punkteB, null, null))))), ONLINE_IDS);
    }

    private static void fokussiere(XSpreadsheetDocument dokument) {
        XFrame frame = Lo.qi(XModel.class, dokument).getCurrentController().getFrame();
        frame.activate();
        XTopWindow topWindow = Lo.qi(XTopWindow.class, frame.getContainerWindow());
        if (topWindow != null) {
            topWindow.toFront();
        }
    }
}
