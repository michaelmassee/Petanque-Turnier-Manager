package de.petanqueturniermanager.toolbar;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.sun.star.awt.XTopWindow;
import com.sun.star.beans.XPropertySet;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XLayoutManager;
import com.sun.star.frame.XModel;
import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.comp.OfficeDocumentHelper;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.Lo;

/**
 * Regression: Status, ausgeblendete Leisten und Rechenleiste des Turniermodus wurden für alle
 * Dokumente gemeinsam gespeichert. Aktivieren/Beenden in einem zweiten Dokument veränderte damit
 * Status und Wiederherstellung des ersten. Zwei Dokumente gleichzeitig offen, Fokus explizit auf
 * dem jeweils anderen Dokument.
 */
class TurnierModusMehrDokumenteUITest extends BaseCalcUITest {

    private static final String MENUEZEILE = "private:resource/menubar/menubar";

    private XSpreadsheetDocument zweitesDokument;
    private WorkingSpreadsheet zweitesWs;

    @AfterEach
    void raeumeAuf() {
        var turnierModus = TurnierModus.get();
        turnierModus.wiederherstellenAlleElemente(wkingSpreadsheet);
        warteAufLoLeerlauf();
        if (zweitesDokument != null) {
            turnierModus.wiederherstellenAlleElemente(zweitesWs);
            warteAufLoLeerlauf();
            OfficeDocumentHelper.closeDoc(zweitesDokument);
            zweitesDokument = null;
        }
    }

    @Test
    void statusGiltJeDokument() throws Exception {
        oeffneZweitesDokument();
        var turnierModus = TurnierModus.get();

        turnierModus.aktivieren(wkingSpreadsheet);
        warteAufLoLeerlauf();
        fokussiere(zweitesDokument);
        warteAufLoLeerlauf();

        assertThat(turnierModus.istAktiv(wkingSpreadsheet)).isTrue();
        assertThat(turnierModus.istAktiv(zweitesWs)).as("Dokument B ist nicht im Turniermodus").isFalse();
    }

    @Test
    void beendenImZweitenDokumentLaesstErstesDokumentImTurniermodus() throws Exception {
        oeffneZweitesDokument();
        var turnierModus = TurnierModus.get();
        turnierModus.aktivieren(wkingSpreadsheet);
        warteAufLoLeerlauf();
        assertThat(istMenueSichtbar(doc)).as("A nach Aktivieren in A").isFalse();
        turnierModus.aktivieren(zweitesWs);
        warteAufLoLeerlauf();
        assertThat(istMenueSichtbar(doc)).as("A nach Aktivieren in B").isFalse();

        turnierModus.wiederherstellenAlleElemente(zweitesWs);
        warteAufLoLeerlauf();
        assertThat(istMenueSichtbar(doc)).as("A nach Beenden in B").isFalse();
        fokussiere(doc);
        warteAufLoLeerlauf();

        assertThat(turnierModus.istAktiv(zweitesWs)).isFalse();
        assertThat(istMenueSichtbar(zweitesDokument)).as("B bekommt seine eigenen Leisten zurück").isTrue();
        assertThat(turnierModus.istAktiv(wkingSpreadsheet)).as("A bleibt im Turniermodus").isTrue();
        assertThat(istMenueSichtbar(doc)).as("A bleibt ohne Menüleiste").isFalse();
    }

    @Test
    void jedesDokumentStelltSeineEigenenLeistenWiederHer() throws Exception {
        oeffneZweitesDokument();
        var turnierModus = TurnierModus.get();
        turnierModus.aktivieren(wkingSpreadsheet);
        warteAufLoLeerlauf();
        turnierModus.aktivieren(zweitesWs);
        warteAufLoLeerlauf();

        turnierModus.wiederherstellenAlleElemente(wkingSpreadsheet);
        warteAufLoLeerlauf();
        turnierModus.wiederherstellenAlleElemente(zweitesWs);
        warteAufLoLeerlauf();

        assertThat(istMenueSichtbar(doc)).isTrue();
        assertThat(istMenueSichtbar(zweitesDokument)).isTrue();
    }

    @Test
    void erneutesAktivierenBehaeltDenAusgangszustand() throws Exception {
        var turnierModus = TurnierModus.get();
        turnierModus.aktivieren(wkingSpreadsheet);
        warteAufLoLeerlauf();
        turnierModus.aktivieren(wkingSpreadsheet);
        warteAufLoLeerlauf();

        turnierModus.wiederherstellenAlleElemente(wkingSpreadsheet);
        warteAufLoLeerlauf();

        assertThat(turnierModus.istAktiv(wkingSpreadsheet)).isFalse();
        assertThat(istMenueSichtbar(doc)).isTrue();
    }

    private void oeffneZweitesDokument() {
        zweitesDokument = OfficeDocumentHelper.from(loader).createSichtbaresCalc();
        assertThat(zweitesDokument).as("zweites Dokument konnte nicht erstellt werden").isNotNull();
        zweitesWs = new WorkingSpreadsheet(starter.getxComponentContext(), zweitesDokument);
        warteAufLoLeerlauf();
    }

    private static boolean istMenueSichtbar(XSpreadsheetDocument dokument) throws Exception {
        XFrame frame = Lo.qi(XModel.class, dokument).getCurrentController().getFrame();
        XLayoutManager lm = Lo.qi(XLayoutManager.class,
                Lo.qi(XPropertySet.class, frame).getPropertyValue("LayoutManager"));
        return lm.isElementVisible(MENUEZEILE);
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
