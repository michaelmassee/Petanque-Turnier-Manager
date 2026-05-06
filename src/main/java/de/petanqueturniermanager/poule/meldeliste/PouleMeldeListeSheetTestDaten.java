/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.poule.meldeliste;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeKonstanten;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.NewTestDatenValidator;
import de.petanqueturniermanager.helper.TestnamenLoader;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.poule.konfiguration.PouleKonfigurationSheet;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Erzeugt eine Poule-A/B-Meldeliste mit Testdaten (ohne Dialog).
 * Formation: Triplette, Teamname aktiv, Vereinsname aktiv.
 */
public class PouleMeldeListeSheetTestDaten extends SheetRunner implements ISheet, MeldeListeKonstanten {

    public static final int ANZ_TEAMS_DEFAULT = 16;
    private static final Formation TEST_FORMATION = Formation.TRIPLETTE;

    protected static final int ERSTE_DATEN_ZEILE = PouleListeDelegate.ERSTE_DATEN_ZEILE;

    private final PouleListeDelegate delegate;
    private final int anzTeams;
    private final PouleMeldeListeSheetNew meldeListe;
    private final TestnamenLoader testnamenLoader;

    public PouleMeldeListeSheetTestDaten(WorkingSpreadsheet workingSpreadsheet) {
        this(workingSpreadsheet, ANZ_TEAMS_DEFAULT);
    }

    public PouleMeldeListeSheetTestDaten(WorkingSpreadsheet workingSpreadsheet, int anzTeams) {
        super(workingSpreadsheet, TurnierSystem.POULE, "Poule-Meldeliste");
        delegate = new PouleListeDelegate(this);
        this.anzTeams = anzTeams;
        meldeListe = new PouleMeldeListeSheetNew(workingSpreadsheet);
        testnamenLoader = new TestnamenLoader();
    }

    @Override
    public XSpreadsheet getXSpreadSheet() throws GenerateException {
        return getSheetHelper().findByName(SheetNamen.pouleMeldeliste());
    }

    @Override
    public TurnierSheet getTurnierSheet() throws GenerateException {
        return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
    }

    @Override
    protected PouleKonfigurationSheet getKonfigurationSheet() {
        return delegate.getKonfigurationSheet();
    }

    // ---------------------------------------------------------------
    // Forwarding-Methoden → Delegate
    // ---------------------------------------------------------------

    public int getTeamNrSpalte() {
        return delegate.getTeamNrSpalte();
    }

    public int getTeamnameSpalte() throws GenerateException {
        return delegate.getTeamnameSpalte();
    }

    public int getVornameSpalte(int spielerIdx) throws GenerateException {
        return delegate.getVornameSpalte(spielerIdx);
    }

    public int getNachnameSpalte(int spielerIdx) throws GenerateException {
        return delegate.getNachnameSpalte(spielerIdx);
    }

    public int getVereinsnameSpalte(int spielerIdx) throws GenerateException {
        return delegate.getVereinsnameSpalte(spielerIdx);
    }

    public int getAktivSpalte() throws GenerateException {
        return delegate.getAktivSpalte();
    }

    public TeamMeldungen getAktiveMeldungen() throws GenerateException {
        return delegate.getAktiveMeldungen();
    }

    public int getSpielerNameErsteSpalte() throws GenerateException {
        return delegate.getSpielerNameErsteSpalte();
    }

    public int getErsteDatenZiele() {
        return delegate.getErsteDatenZiele();
    }

    public int getLetzteDatenZeileUseMin() throws GenerateException {
        return delegate.getLetzteDatenZeileUseMin();
    }

    // ---------------------------------------------------------------
    // Eigene Methoden
    // ---------------------------------------------------------------

    @Override
    public void doRun() throws GenerateException {
        if (!NewTestDatenValidator.from(getWorkingSpreadsheet(), getSheetHelper(), TurnierSystem.POULE)
                .prefix(getLogPrefix()).validate()) {
            return;
        }

        getSheetHelper().removeAllSheetsExclude();

        // Meldeliste mit Standardparametern anlegen (kein Dialog)
        meldeListe.createMeldelisteWithParams(TEST_FORMATION, true, true);

        testNamenEinfuegen();

        // Teamnummern vergeben und Meldeliste aktualisieren
        new PouleMeldeListeSheetUpdate(getWorkingSpreadsheet()).doRun();
    }

    private void testNamenEinfuegen() throws GenerateException {
        int anzSpielerProTeam = getKonfigurationSheet().getMeldeListeFormation().getAnzSpieler();
        boolean teamnameAktiv = getKonfigurationSheet().isMeldeListeTeamnameAnzeigen();
        boolean vereinsnameAktiv = getKonfigurationSheet().isMeldeListeVereinsnameAnzeigen();
        var spieler = testnamenLoader.listeMitSpielerTestNamen(anzTeams * anzSpielerProTeam);

        var data = new RangeData();
        for (int team = 0; team < anzTeams; team++) {
            testDoCancelTask();
            var zeile = data.addNewRow();
            if (teamnameAktiv) {
                zeile.newString("Team " + (team + 1));
            }
            for (int s = 0; s < anzSpielerProTeam; s++) {
                var stn = spieler.get(team * anzSpielerProTeam + s);
                zeile.newString(stn.vorname());
                zeile.newString(stn.nachname());
                if (vereinsnameAktiv) {
                    zeile.newString("Verein " + ((team % 5) + 1));
                }
            }
            zeile.newEmpty(); // Setzposition
            zeile.newInt(PouleListeDelegate.AKTIV_WERT_NIMMT_TEIL);
        }

        var xSheet = meldeListe.getXSpreadSheet();
        var startPos = Position.from(1, ERSTE_DATEN_ZEILE);
        RangeHelper.from(xSheet, getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
                data.getRangePosition(startPos)).setDataInRange(data);

        meldeListe.upDateSheet();
    }
}
