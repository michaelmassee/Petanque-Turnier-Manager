package de.petanqueturniermanager.formulex.spielrunde;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.formulex.endrangliste.FormuleXEndranglisteSheet;
import de.petanqueturniermanager.formulex.meldeliste.FormuleXMeldeListeSheetNew;
import de.petanqueturniermanager.formulex.meldeliste.FormuleXMeldeListeSheetTestDaten;
import de.petanqueturniermanager.formulex.spieltagrangliste.FormuleXSpieltagRanglisteSheet;
import de.petanqueturniermanager.helper.NewTestDatenValidator;
import de.petanqueturniermanager.basesheet.meldeliste.SpielTagNr;

/**
 * Generiert ein vollständiges FormuleX-Beispielturnier mit mehreren Spieltagen und
 * Serien-Endrangliste – Beispielturnier-Pendant zu {@link FormuleXTurnierTestDaten}
 * (analog {@code SchweizerSerienTurnierTestDaten}).
 * <p>
 * Standard: 16 Teams, 2 Spieltage à 3 Runden.
 */
public class FormuleXSerienTurnierTestDaten extends FormuleXAbstractSpielrundeSheet {

    private static final int ANZ_TEAMS_DEFAULT = 16;
    private static final int ANZ_SPIELTAGE_DEFAULT = 2;
    private static final int ANZ_RUNDEN_PRO_SPIELTAG_DEFAULT = 3;

    private final int anzSpieltage;
    private final int anzRundenProSpieltag;

    private final FormuleXMeldeListeSheetTestDaten meldelisteTestDaten;
    private final FormuleXMeldeListeSheetNew meldeListeSheetNew;
    public final FormuleXSpielrundeSheetNaechste naechsteSpielrunde;
    private final FormuleXTurnierTestDaten ergebnisHelfer;

    /** Standard-Konstruktor: 16 Teams, 2 Spieltage à 3 Runden. */
    public FormuleXSerienTurnierTestDaten(WorkingSpreadsheet workingSpreadsheet) {
        this(workingSpreadsheet, ANZ_TEAMS_DEFAULT, ANZ_SPIELTAGE_DEFAULT, ANZ_RUNDEN_PRO_SPIELTAG_DEFAULT);
    }

    /**
     * Parametrisierter Konstruktor.
     *
     * @param anzTeams             Anzahl zu generierender Teams
     * @param anzSpieltage         Anzahl Spieltage der Serie (mind. 2, damit die Endrangliste ein Streichresultat kennt)
     * @param anzRundenProSpieltag Anzahl Spielrunden je Spieltag
     */
    public FormuleXSerienTurnierTestDaten(WorkingSpreadsheet workingSpreadsheet, int anzTeams, int anzSpieltage,
            int anzRundenProSpieltag) {
        super(workingSpreadsheet);
        this.anzSpieltage = anzSpieltage;
        this.anzRundenProSpieltag = anzRundenProSpieltag;
        meldelisteTestDaten = new FormuleXMeldeListeSheetTestDaten(workingSpreadsheet, anzTeams);
        meldeListeSheetNew = new FormuleXMeldeListeSheetNew(workingSpreadsheet);
        naechsteSpielrunde = new FormuleXSpielrundeSheetNaechste(workingSpreadsheet);
        ergebnisHelfer = new FormuleXTurnierTestDaten(workingSpreadsheet);
    }

    @Override
    protected void doRun() throws GenerateException {
        if (!NewTestDatenValidator.from(getWorkingSpreadsheet(), getSheetHelper(), TurnierSystem.FORMULEX)
                .prefix(getLogPrefix()).validate()) {
            return;
        }
        getSheetHelper().removeAllSheetsExclude();
        generate();
    }

    public void generate() throws GenerateException {
        meldelisteTestDaten.erstelleMeldelisteWithTestdaten();
        naechsteSpielrunde.getKonfigurationSheet().setSpielrundeSpielbahn(SpielrundeSpielbahn.R);
        naechsteSpielrunde.getKonfigurationSheet().setAnzahlRunden(anzRundenProSpieltag);

        for (int spieltag = 1; spieltag <= anzSpieltage; spieltag++) {
            SheetRunner.testDoCancelTask();
            processBoxinfo("processbox.formulex.serie.spieltag", spieltag, anzSpieltage);

            for (int runde = 1; runde <= anzRundenProSpieltag; runde++) {
                SheetRunner.testDoCancelTask();
                naechsteSpielrunde.doRun();
                XSpreadsheet sheet = naechsteSpielrunde.getXSpreadSheet();
                if (sheet != null) {
                    ergebnisHelfer.ergebnisseEinfuegen(sheet);
                }
            }

            if (spieltag < anzSpieltage) {
                // Archiviert die Rangliste dieses Spieltags und wechselt auf den nächsten.
                meldeListeSheetNew.naechsteSpieltag();
            } else {
                // Letzter Spieltag: nur archivieren, kein Wechsel mehr nötig.
                new FormuleXSpieltagRanglisteSheet(getWorkingSpreadsheet(), SpielTagNr.from(spieltag)).doRun();
            }
        }

        new FormuleXEndranglisteSheet(getWorkingSpreadsheet()).doRun();

        var konfig = naechsteSpielrunde.getKonfigurationSheet();
        konfig.setKopfZeileMitte(getTurnierSystem().getBezeichnung());
        konfig.seitenstileAktualisieren();
    }

}
