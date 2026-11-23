package de.petanqueturniermanager.supermelee.spieltagrangliste;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeKonfigurationSheet;

/**
 * Generiert ein vollständiges Supermelee-Beispielturnier:
 * Meldeliste + komplette Spieltage (Standard: 5) mit Spielrunden und Spieltag-Ranglisten.
 * <p>
 * Wrapper für {@link SpieltagRanglisteSheet_TestDaten} mit öffentlicher
 * {@code generate()}-Methode für den direkten Aufruf in Tests.
 */
public class SupermeleeTurnierTestDaten extends SpieltagRanglisteSheet_TestDaten {

    public SupermeleeTurnierTestDaten(WorkingSpreadsheet workingSpreadsheet) {
        super(workingSpreadsheet);
    }

    /**
     * @param anzahlSpieltage Anzahl der zu erzeugenden Spieltage, mindestens 1
     */
    public SupermeleeTurnierTestDaten(WorkingSpreadsheet workingSpreadsheet, int anzahlSpieltage) {
        super(workingSpreadsheet, anzahlSpieltage);
    }

    /**
     * Öffentlicher Einstiegspunkt für Tests: generiert das vollständige Supermelee-Turnier
     * ohne Dialoge direkt auf dem aktuellen Dokument.
     */
    public void generate() throws GenerateException {
        doRun();
    }

    @Override
    protected void doRun() throws GenerateException {
        super.doRun();
        // Seitenstile mit Spieltag-Kopfzeilen (Standardwerte "1. Spieltag" etc.) und Werbefußzeile anwenden
        SuperMeleeKonfigurationSheet konfig = getKonfigurationSheet();
        konfig.seitenstileAktualisieren();
    }
}
