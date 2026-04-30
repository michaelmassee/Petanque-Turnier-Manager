/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.star.beans.PropertyValue;
import com.sun.star.container.XIndexAccess;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeMode;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_New;

/**
 * Regressionstest für den Crash beim Verlassen der Druckvorschau.
 * <p>
 * Reproduziert das exakte Crash-Szenario: leere Supermelee-Meldeliste öffnen →
 * Druckvorschau öffnen → Druckvorschau schließen.
 * Beim Verlassen der Druckvorschau wechselt LO intern den Controller von {@code ScPreviewController}
 * zurück zu {@code ScTabViewShell}. Ohne die Druckvorschau-Guards in den Toolbar-Steuerungsklassen
 * führte dieser Übergang zu einem nativen LO-Crash.
 * </p>
 */
@Tag("beispielturnier")
class DruckvorschauUITest extends BaseCalcUITest {

    @Test
    void druckvorschauOeffnenUndSchliessen_KeinAbsturz() throws GenerateException, InterruptedException {
        // Leere Supermelee-Meldeliste anlegen – reproduziert das exakte Crash-Szenario
        new MeldeListeSheet_New(wkingSpreadsheet).createMeldelisteWithParams(SuperMeleeMode.Doublette);

        // Kurz warten, damit alle asynchronen Events nach der Dokumenterstellung abgearbeitet sind
        Thread.sleep(500);

        // Druckvorschau öffnen – LO wechselt auf ScPreviewController
        wkingSpreadsheet.executeDispatch(".uno:PrintPreview", "_self", 0, new PropertyValue[0]);
        Thread.sleep(500);

        // Druckvorschau schließen – das war der Crash-Auslöser:
        // LO wechselt zurück auf ScTabViewShell, Timer und Events feuern während des Übergangs
        wkingSpreadsheet.executeDispatch(".uno:ClosePreview", "_self", 0, new PropertyValue[0]);
        Thread.sleep(500);

        // Sicherstellen dass LibreOffice nach dem Druckvorschau-Exit noch vollständig reaktionsfähig ist
        assertThat(wkingSpreadsheet.getWorkingSpreadsheetDocument())
                .as("LibreOffice muss nach dem Druckvorschau-Exit noch reagieren")
                .isNotNull();

        int anzahlSheets = Lo.qi(XIndexAccess.class,
                wkingSpreadsheet.getWorkingSpreadsheetDocument().getSheets()).getCount();
        assertThat(anzahlSheets)
                .as("Dokument muss nach dem Druckvorschau-Exit noch Sheets enthalten")
                .isGreaterThan(0);
    }

    @Test
    void druckvorschauMehrfach_KeinAbsturz() throws GenerateException, InterruptedException {
        new MeldeListeSheet_New(wkingSpreadsheet).createMeldelisteWithParams(SuperMeleeMode.Doublette);
        Thread.sleep(500);

        // Drei Zyklen Druckvorschau öffnen/schließen – prüft auf kumulierende Fehler
        for (int i = 0; i < 3; i++) {
            wkingSpreadsheet.executeDispatch(".uno:PrintPreview", "_self", 0, new PropertyValue[0]);
            Thread.sleep(300);
            wkingSpreadsheet.executeDispatch(".uno:ClosePreview", "_self", 0, new PropertyValue[0]);
            Thread.sleep(300);
        }

        assertThat(wkingSpreadsheet.getWorkingSpreadsheetDocument())
                .as("LibreOffice muss nach mehrfachem Druckvorschau-Zyklus noch reagieren")
                .isNotNull();
    }
}
