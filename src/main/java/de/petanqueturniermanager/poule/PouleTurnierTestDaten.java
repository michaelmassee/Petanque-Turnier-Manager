/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.poule;

import java.util.concurrent.ThreadLocalRandom;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.algorithmen.PouleGruppenRechner;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.NewTestDatenValidator;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.poule.konfiguration.PouleKonfigurationSheet;
import de.petanqueturniermanager.poule.meldeliste.PouleMeldeListeSheetTestDaten;
import de.petanqueturniermanager.poule.meldeliste.PouleMeldeListeSheetUpdate;
import de.petanqueturniermanager.poule.meldeliste.PouleTeilnehmerSheet;
import de.petanqueturniermanager.poule.vorrunde.PouleSpielplaeneSheet;
import de.petanqueturniermanager.poule.vorrunde.PouleVorrundeSheet;
import de.petanqueturniermanager.poule.vorrunde.PouleSeedingService;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Generiert ein vollständiges Poule-A/B-Beispielturnier ohne Dialoge:
 * <ol>
 *   <li>Meldeliste (16 Teams, Triplette)</li>
 *   <li>Teilnehmerliste</li>
 *   <li>Vorrunde (alle Poule-Gruppen mit Spielplan)</li>
 *   <li>Spielpläne (je ein Sheet pro Gruppe)</li>
 *   <li>Zufallsergebnisse für alle Matches</li>
 * </ol>
 */
public class PouleTurnierTestDaten extends SheetRunner implements ISheet {

    private final PouleMeldeListeSheetTestDaten meldelisteTestDaten;
    private final PouleMeldeListeSheetUpdate meldeliste;
    private final PouleTeilnehmerSheet teilnehmerSheet;
    private final PouleVorrundeSheet vorrundeSheet;
    private final PouleSpielplaeneSheet spielplaeneSheet;
    private final PouleKonfigurationSheet konfigurationSheet;

    public PouleTurnierTestDaten(WorkingSpreadsheet workingSpreadsheet) {
        super(workingSpreadsheet, TurnierSystem.POULE, "Poule-Turnier-Testdaten");
        meldelisteTestDaten = new PouleMeldeListeSheetTestDaten(workingSpreadsheet);
        meldeliste = new PouleMeldeListeSheetUpdate(workingSpreadsheet);
        teilnehmerSheet = new PouleTeilnehmerSheet(workingSpreadsheet);
        vorrundeSheet = new PouleVorrundeSheet(workingSpreadsheet);
        spielplaeneSheet = new PouleSpielplaeneSheet(workingSpreadsheet);
        konfigurationSheet = new PouleKonfigurationSheet(workingSpreadsheet);
    }

    @Override
    protected PouleKonfigurationSheet getKonfigurationSheet() {
        return konfigurationSheet;
    }

    @Override
    public XSpreadsheet getXSpreadSheet() throws GenerateException {
        return getSheetHelper().findByName(SheetNamen.pouleMeldeliste());
    }

    @Override
    public TurnierSheet getTurnierSheet() throws GenerateException {
        return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
    }

    /**
     * Öffentlicher Einstiegspunkt für Tests und Registrierung: generiert das vollständige
     * Poule-Beispielturnier ohne SheetRunner.start().
     */
    public void generate() throws GenerateException {
        doRun();
    }

    @Override
    protected void doRun() throws GenerateException {
        if (!NewTestDatenValidator.from(getWorkingSpreadsheet(), getSheetHelper(), TurnierSystem.POULE)
                .prefix(getLogPrefix()).validate()) {
            return;
        }

        // 1. Meldeliste erstellen
        meldelisteTestDaten.doRun();

        // 2. Meldeliste aktualisieren – befüllt this.meldeliste für ergebnisseEinfuegen()
        meldeliste.upDateSheet();

        // 3. Teilnehmerliste erstellen
        teilnehmerSheet.generate();

        // 4. Vorrunde erstellen
        vorrundeSheet.doRun();

        // 5. Spielpläne erstellen (je ein Sheet pro Gruppe)
        spielplaeneSheet.doRun();

        // 6. Ergebnisse einfügen
        ergebnisseEinfuegen();

        // 7. Seitenstile setzen
        konfigurationSheet.seitenstileAktualisieren();
    }

    /**
     * Füllt alle Vorrunden-Spiele mit Zufallsergebnissen (13:x).
     * <p>
     * Reihenfolge pro 4er-Poule: R1 (SpielA, SpielB) → R2 (Sieger, Verlierer) → R3 (Barrage).
     * Für 3er-Poule: alle 3 Spiele direkt.
     */
    private void ergebnisseEinfuegen() throws GenerateException {
        var meldungen = meldeliste.getAktiveMeldungen();
        if (meldungen.size() < 3) {
            return;
        }

        var gruppenGroessen = PouleGruppenRechner.berechneGruppenGroessen(meldungen.size());
        var poules = PouleSeedingService.verteileTeams(meldungen, gruppenGroessen);

        var xSheet = SheetMetadataHelper.findeSheetUndHeile(
                getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
                SheetMetadataHelper.SCHLUESSEL_POULE_VORRUNDE,
                SheetNamen.pouleVorrunde());

        if (xSheet == null) {
            return;
        }

        int aktuelleZeile = PouleVorrundeSheet.ERSTE_DATEN_ZEILE;

        for (var poule : poules) {
            SheetRunner.testDoCancelTask();
            if (poule.teams().size() == 4) {
                schreibeViererPouleErgebnisse(xSheet, aktuelleZeile);
                aktuelleZeile += PouleVorrundeSheet.VIERER_POULE_ZEILEN;
            } else {
                schreibeDreierPouleErgebnisse(xSheet, aktuelleZeile);
                aktuelleZeile += PouleVorrundeSheet.DREIER_POULE_ZEILEN;
            }
        }
    }

    /**
     * Schreibt Zufallsergebnisse für eine 4er-Poule (5 Spiele in 3 Runden).
     */
    private void schreibeViererPouleErgebnisse(XSpreadsheet xSheet, int basisZeile) throws GenerateException {
        // R1: SpielA und SpielB
        schreibeErgebnis(xSheet, basisZeile);
        schreibeErgebnis(xSheet, basisZeile + 1);

        // R2: Siegerspiel und Verliererspiel
        schreibeErgebnis(xSheet, basisZeile + 2);
        schreibeErgebnis(xSheet, basisZeile + 3);

        // R3: Barrage
        schreibeErgebnis(xSheet, basisZeile + 4);
    }

    /**
     * Schreibt Zufallsergebnisse für eine 3er-Poule (3 Spiele).
     */
    private void schreibeDreierPouleErgebnisse(XSpreadsheet xSheet, int basisZeile) throws GenerateException {
        schreibeErgebnis(xSheet, basisZeile);
        schreibeErgebnis(xSheet, basisZeile + 1);
        schreibeErgebnis(xSheet, basisZeile + 2);
    }

    /**
     * Schreibt ein Zufallsergebnis (13:x mit x ∈ 0..12) in die ERG-Spalten der angegebenen Zeile.
     */
    private void schreibeErgebnis(XSpreadsheet xSheet, int zeile) throws GenerateException {
        int loserPts = ThreadLocalRandom.current().nextInt(0, 13);
        int winner = ThreadLocalRandom.current().nextInt(2);
        int ergA = (winner == 0) ? 13 : loserPts;
        int ergB = (winner == 0) ? loserPts : 13;

        getSheetHelper().setNumberValueInCell(
                NumberCellValue.from(xSheet, Position.from(PouleVorrundeSheet.SPALTE_ERG_A, zeile)).setValue(ergA));
        getSheetHelper().setNumberValueInCell(
                NumberCellValue.from(xSheet, Position.from(PouleVorrundeSheet.SPALTE_ERG_B, zeile)).setValue(ergB));
    }
}
