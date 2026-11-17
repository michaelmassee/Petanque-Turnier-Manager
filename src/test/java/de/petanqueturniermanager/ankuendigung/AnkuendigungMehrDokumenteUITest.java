/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ankuendigung;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.sun.star.awt.XTopWindow;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XModel;
import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.comp.OfficeDocumentHelper;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.Lo;

/**
 * Die Ankündigung ist dokumentbezogen: Bei zwei gleichzeitig offenen Turnierdokumenten liest und schreibt jede
 * Aktion die Ankündigung des gemeinten Dokuments – auch wenn gerade das andere Dokument den Fokus hält.
 */
public class AnkuendigungMehrDokumenteUITest extends BaseCalcUITest {

    private XSpreadsheetDocument zweitesDokument;

    @AfterEach
    public void schliesseZweitesDokument() {
        if (zweitesDokument != null) {
            OfficeDocumentHelper.closeDoc(zweitesDokument);
            zweitesDokument = null;
        }
    }

    @Test
    public void ankuendigungGehoertZumGemeintenDokumentTrotzFokusAufDemAnderen() {
        WorkingSpreadsheet erstesWs = new WorkingSpreadsheet(starter.getxComponentContext(), doc);
        Ankuendigung.speichere(erstesWs, "Dokument A: Mittagspause bis 13 Uhr");

        zweitesDokument = OfficeDocumentHelper.from(loader).createSichtbaresCalc();
        assertThat(zweitesDokument).as("zweites Dokument konnte nicht erstellt werden").isNotNull();
        WorkingSpreadsheet zweitesWs = new WorkingSpreadsheet(starter.getxComponentContext(), zweitesDokument);
        Ankuendigung.speichere(zweitesWs, "Dokument B: Runde 3 startet");

        fokussiere(doc);

        assertThat(Ankuendigung.lese(zweitesWs))
                .as("Die Ankündigung von B darf nicht vom fokussierten Dokument A kommen")
                .isEqualTo("Dokument B: Runde 3 startet");
        assertThat(Ankuendigung.lese(erstesWs)).isEqualTo("Dokument A: Mittagspause bis 13 Uhr");

        Ankuendigung.speichere(zweitesWs, "");

        assertThat(Ankuendigung.lese(zweitesWs)).as("Löschen in B").isEmpty();
        assertThat(Ankuendigung.lese(erstesWs)).as("A bleibt beim Löschen in B unverändert")
                .isEqualTo("Dokument A: Mittagspause bis 13 Uhr");
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
