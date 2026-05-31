package de.petanqueturniermanager.triptete.meldeliste;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.NewTestDatenValidator;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.random.RandomSource;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.triptete.konfiguration.TripTeteKonfigurationSheet;

/**
 * TestDaten-Generator für die Trip-Tête-Meldeliste.
 * Schreibt Triplett-Teams mit Vorname + Nachname für 3 Spieler und Aktiv=1.
 */
public class TripTeteMeldeListeSheetTestDaten extends SheetRunner implements ISheet {

    private static final int AKTIV_NIMMT_TEIL = 1;

    private final TripTeteKonfigurationSheet konfigurationSheet;
    private final TripTeteMeldeListeSheetNew meldeListe;
    private final TripTeteMeldeListeDelegate delegate;
    private final int anzTeams;

    public TripTeteMeldeListeSheetTestDaten(WorkingSpreadsheet workingSpreadsheet) {
        this(workingSpreadsheet, 6);
    }

    public TripTeteMeldeListeSheetTestDaten(WorkingSpreadsheet workingSpreadsheet, int anzTeams) {
        super(workingSpreadsheet, TurnierSystem.TRIPTETE);
        konfigurationSheet = new TripTeteKonfigurationSheet(workingSpreadsheet);
        meldeListe = new TripTeteMeldeListeSheetNew(workingSpreadsheet);
        delegate = new TripTeteMeldeListeDelegate(meldeListe, workingSpreadsheet);
        this.anzTeams = anzTeams;
    }

    @Override
    protected TripTeteKonfigurationSheet getKonfigurationSheet() {
        return konfigurationSheet;
    }

    public void generate() throws GenerateException {
        doRun();
    }

    @Override
    protected void doRun() throws GenerateException {
        if (!NewTestDatenValidator.from(getWorkingSpreadsheet(), getSheetHelper(), TurnierSystem.TRIPTETE)
                .prefix(getLogPrefix()).validate()) {
            return;
        }
        getSheetHelper().removeAllSheetsExclude(new String[] {});
        meldeListe.createMeldeliste();
        testNamenEinfuegen();
    }

    public void erstelleUndFuelleTestDaten() throws GenerateException {
        meldeListe.createMeldeliste();
        testNamenEinfuegen();
    }

    public void testNamenEinfuegen() throws GenerateException {
        XSpreadsheet meldelisteSheet = meldeListe.getXSpreadSheet();
        TurnierSheet.from(meldelisteSheet, getWorkingSpreadsheet()).setActiv();

        List<Triplett> testTeams = listeMitTestTripletts();
        Collections.shuffle(testTeams, RandomSource.asJavaRandom());

        int vornameSpalte0 = delegate.getVornameSpalte(0);
        int nachnameSpalte0 = delegate.getNachnameSpalte(0);
        int vornameSpalte1 = delegate.getVornameSpalte(1);
        int nachnameSpalte1 = delegate.getNachnameSpalte(1);
        int vornameSpalte2 = delegate.getVornameSpalte(2);
        int nachnameSpalte2 = delegate.getNachnameSpalte(2);
        int aktivSpalte = delegate.getAktivSpalte();
        int ersteDatenZeile = TripTeteMeldeListeDelegate.ERSTE_DATEN_ZEILE_OVERRIDE;

        int zeile = ersteDatenZeile;
        int cntr = 0;
        for (Triplett team : testTeams) {
            schreibeSpieler(meldelisteSheet, zeile, vornameSpalte0, nachnameSpalte0, team.sp1Vorname(), team.sp1Nachname());
            schreibeSpieler(meldelisteSheet, zeile, vornameSpalte1, nachnameSpalte1, team.sp2Vorname(), team.sp2Nachname());
            schreibeSpieler(meldelisteSheet, zeile, vornameSpalte2, nachnameSpalte2, team.sp3Vorname(), team.sp3Nachname());
            getSheetHelper().setNumberValueInCell(
                    NumberCellValue.from(meldelisteSheet, Position.from(aktivSpalte, zeile)).setValue(AKTIV_NIMMT_TEIL));
            zeile++;
            if (++cntr >= anzTeams) {
                break;
            }
        }
        meldeListe.upDateSheet();
    }

    private void schreibeSpieler(XSpreadsheet sheet, int zeile, int vornameSpalte, int nachnameSpalte,
            String vorname, String nachname) throws GenerateException {
        getSheetHelper().setStringValueInCell(
                StringCellValue.from(sheet, Position.from(vornameSpalte, zeile), vorname));
        getSheetHelper().setStringValueInCell(
                StringCellValue.from(sheet, Position.from(nachnameSpalte, zeile), nachname));
    }

    record Triplett(String sp1Vorname, String sp1Nachname,
                    String sp2Vorname, String sp2Nachname,
                    String sp3Vorname, String sp3Nachname) {}

    List<Triplett> listeMitTestTripletts() {
        List<Triplett> teams = new ArrayList<>();
        teams.add(new Triplett("Hans", "Müller", "Klaus", "Schreiber", "Anna", "Wagner"));
        teams.add(new Triplett("Peter", "Schmitt", "Maria", "Bauer", "Thomas", "Klein"));
        teams.add(new Triplett("Lukas", "Fischer", "Eva", "Hofmann", "Markus", "Weber"));
        teams.add(new Triplett("Sophie", "Meyer", "Stefan", "Wolf", "Laura", "Richter"));
        teams.add(new Triplett("Christian", "Braun", "Julia", "Koch", "Michael", "Schäfer"));
        teams.add(new Triplett("Felix", "Lange", "Nicole", "Becker", "Patrick", "Schwarz"));
        teams.add(new Triplett("Tobias", "Zimmermann", "Sandra", "Hartmann", "David", "Krause"));
        teams.add(new Triplett("Sabine", "Schneider", "Andreas", "Roth", "Monika", "Lehmann"));
        return teams;
    }

    @Override
    public XSpreadsheet getXSpreadSheet() throws GenerateException {
        return meldeListe.getXSpreadSheet();
    }

    @Override
    public TurnierSheet getTurnierSheet() throws GenerateException {
        return meldeListe.getTurnierSheet();
    }
}
