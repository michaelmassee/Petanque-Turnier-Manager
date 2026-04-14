/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.kaskade;

import java.util.concurrent.ThreadLocalRandom;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.NewTestDatenValidator;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.kaskade.konfiguration.KaskadeKonfigurationSheet;
import de.petanqueturniermanager.kaskade.meldeliste.KaskadeMeldeListeSheetTestDaten;
import de.petanqueturniermanager.kaskade.spielrunde.KaskadeSpielrundeSheet;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Erstellt ein vollständiges Kaskaden-KO-Testturnier ohne Dialoge:<br>
 * 97 Teams, Kaskade bis H (3 Kaskadenrunden → 8 Endfelder A–H).<br>
 * <br>
 * Ablauf:
 * <ol>
 *   <li>Meldeliste mit Testdaten (97 Doubletten) anlegen</li>
 *   <li>Kaskadenrunden-Anzahl auf 3 setzen</li>
 *   <li>Kaskadenrunden 1–3 erstellen und mit Zufallsergebnissen füllen</li>
 *   <li>KO-Felder A–H erstellen (automatisch durch 4. Aufruf der Spielrunde)</li>
 * </ol>
 */
public class KaskadeTurnierTestDaten extends SheetRunner implements ISheet {

    private static final int ANZ_TEAMS    = 97;
    private static final int ANZ_KASKADEN = 3;  // → Endfelder A bis H

    private final KaskadeMeldeListeSheetTestDaten meldelisteTestDaten;
    private final KaskadeSpielrundeSheet          spielrundeSheet;
    private final KaskadeKonfigurationSheet       konfigurationSheet;

    public KaskadeTurnierTestDaten(WorkingSpreadsheet workingSpreadsheet) {
        super(workingSpreadsheet, TurnierSystem.KASKADE, "Kaskaden-Turnier-Testdaten");
        meldelisteTestDaten = new KaskadeMeldeListeSheetTestDaten(workingSpreadsheet, ANZ_TEAMS);
        spielrundeSheet     = new KaskadeSpielrundeSheet(workingSpreadsheet);
        konfigurationSheet  = new KaskadeKonfigurationSheet(workingSpreadsheet);
    }

    @Override
    public XSpreadsheet getXSpreadSheet() throws GenerateException {
        return getSheetHelper().findByName(SheetNamen.meldeliste());
    }

    @Override
    public TurnierSheet getTurnierSheet() throws GenerateException {
        return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
    }

    @Override
    protected KaskadeKonfigurationSheet getKonfigurationSheet() {
        return konfigurationSheet;
    }

    /**
     * Öffentlicher Einstiegspunkt für Tests: generiert das vollständige Kaskaden-KO-Turnier
     * ohne Dialoge direkt auf dem aktuellen Dokument.
     */
    public void generate() throws GenerateException {
        doRun();
    }

    @Override
    protected void doRun() throws GenerateException {
        if (!NewTestDatenValidator.from(getWorkingSpreadsheet(), getSheetHelper(), TurnierSystem.KASKADE)
                .prefix(getLogPrefix()).validate()) {
            return;
        }

        // 1. Meldeliste mit 97 Teams anlegen (setzt intern anzahlKaskaden=2)
        meldelisteTestDaten.erstelleMeldelisteWithTestdaten();

        // 2. Kaskadenrunden-Anzahl auf 3 überschreiben (→ Endfelder A–H)
        konfigurationSheet.setAnzahlKaskaden(ANZ_KASKADEN);

        // 3. Kaskadenrunden 1–3 erstellen und mit Zufallsergebnissen füllen
        spielrundeSheet.setForceOk(true);
        for (int rundeNr = 1; rundeNr <= ANZ_KASKADEN; rundeNr++) {
            SheetRunner.testDoCancelTask();
            processBoxinfo("processbox.erstelle.spielrunde", rundeNr, ANZ_KASKADEN);
            spielrundeSheet.naechsteRunde();
            ergebnisseEinfuegen(rundeNr);
        }

        // 4. Gruppenrangliste + KO-Felder A–H erstellen
        //    (aktiveRunde=3 >= anzahlKaskaden=3 → KaskadeSpielrundeSheet erkennt Abschluss)
        SheetRunner.testDoCancelTask();
        spielrundeSheet.naechsteRunde();

        // 5. Kopfzeile setzen
        konfigurationSheet.setKopfZeileMitte("Kaskaden-KO 97 Teams (A\u2013H)");
        konfigurationSheet.seitenstileAktualisieren();
    }

    // ---------------------------------------------------------------

    /**
     * Füllt alle Spielpaarungen der angegebenen Kaskadenrunde mit Zufallsergebnissen.
     * Freilos-Zeilen werden übersprungen (Team B leer).
     */
    private void ergebnisseEinfuegen(int rundeNr) throws GenerateException {
        var xDoc  = getWorkingSpreadsheet().getWorkingSpreadsheetDocument();
        var sheet = SheetMetadataHelper.findeSheetUndHeile(
                xDoc, SheetMetadataHelper.schluesselKaskadenRunde(rundeNr),
                SheetNamen.kaskadenRunde(rundeNr));
        if (sheet == null) {
            return;
        }

        // Lese Spalten SPIEL_NR..ERG_TEAM_B; Offsets im Ergebnis:
        // 0=SPIEL_NR, 1=TEAM_A, 2=TEAM_B, 3=ERG_A, 4=ERG_B
        var readRange = RangePosition.from(
                KaskadeSpielrundeSheet.SPIEL_NR_SPALTE, KaskadeSpielrundeSheet.ERSTE_DATEN_ZEILE,
                KaskadeSpielrundeSheet.ERG_TEAM_B_SPALTE, KaskadeSpielrundeSheet.ERSTE_DATEN_ZEILE + 200);
        var daten = RangeHelper.from(sheet, xDoc, readRange).getDataFromRange();

        for (int i = 0; i < daten.size(); i++) {
            var zeile = daten.get(i);
            if (zeile.isEmpty() || zeile.get(0).getIntVal(0) <= 0) {
                break;  // Ende der Spieldaten
            }
            // Team B = 0/leer → Freilos, Ergebnis ist vorbelegt
            if (zeile.size() < 3 || zeile.get(2).getIntVal(0) <= 0) {
                continue;
            }

            int datenZeile = KaskadeSpielrundeSheet.ERSTE_DATEN_ZEILE + i;
            int gewinner   = ThreadLocalRandom.current().nextInt(2);
            int verliererP = ThreadLocalRandom.current().nextInt(0, 13);
            int ergA = (gewinner == 0) ? 13 : verliererP;
            int ergB = (gewinner == 0) ? verliererP : 13;

            getSheetHelper().setNumberValueInCell(
                    NumberCellValue.from(sheet,
                            Position.from(KaskadeSpielrundeSheet.ERG_TEAM_A_SPALTE, datenZeile))
                            .setValue(ergA));
            getSheetHelper().setNumberValueInCell(
                    NumberCellValue.from(sheet,
                            Position.from(KaskadeSpielrundeSheet.ERG_TEAM_B_SPALTE, datenZeile))
                            .setValue(ergB));
        }
    }
}
