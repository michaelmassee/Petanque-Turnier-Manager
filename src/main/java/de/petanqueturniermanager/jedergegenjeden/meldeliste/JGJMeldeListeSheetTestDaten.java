package de.petanqueturniermanager.jedergegenjeden.meldeliste;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.TestnamenLoader;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.jedergegenjeden.konfiguration.JGJKonfigurationSheet;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.schweizer.konfiguration.SpielplanTeamAnzeige;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;

/**
 * Erstellung 04.05.2026 / Michael Massee
 */
public class JGJMeldeListeSheetTestDaten extends SheetRunner implements ISheet {

    private final JGJMeldeListeDelegate delegate;
    private final Formation formation;
    private final int anzTeams;
    private final int gruppengroesse;
    private final JGJMeldeListeSheet_New meldeListe;
    private final TestnamenLoader testnamenLoader;

    public JGJMeldeListeSheetTestDaten(WorkingSpreadsheet ws, Formation formation, int anzTeams) {
        this(ws, formation, anzTeams, 0);
    }

    public JGJMeldeListeSheetTestDaten(WorkingSpreadsheet ws, Formation formation, int anzTeams, int gruppengroesse) {
        super(ws, TurnierSystem.JGJ, "JGJ-MeldelisteTestDaten");
        this.formation = formation;
        this.anzTeams = anzTeams;
        this.gruppengroesse = gruppengroesse;
        delegate = new JGJMeldeListeDelegate(this, ws, TurnierSystem.JGJ);
        meldeListe = new JGJMeldeListeSheet_New(ws);
        testnamenLoader = new TestnamenLoader();
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
    protected JGJKonfigurationSheet getKonfigurationSheet() {
        return delegate.getKonfigurationSheet();
    }

    public TeamMeldungen getAktiveMeldungen() throws GenerateException {
        return delegate.getAktiveMeldungen();
    }

    public void erstellenUndBefuellen() throws GenerateException {
        meldeListe.createMeldelisteWithParams(formation, false, false, SpielplanTeamAnzeige.NR, gruppengroesse);
        testNamenEinfuegen();
    }

    private void testNamenEinfuegen() throws GenerateException {
        int anzSpielerProTeam = formation.getAnzSpieler();
        var spieler = testnamenLoader.listeMitSpielerTestNamen(anzTeams * anzSpielerProTeam);

        var data = new RangeData();
        for (int team = 0; team < anzTeams; team++) {
            testDoCancelTask();
            var zeile = data.addNewRow();
            zeile.newInt(team + 1);
            for (int s = 0; s < anzSpielerProTeam; s++) {
                var stn = spieler.get(team * anzSpielerProTeam + s);
                zeile.newString(stn.vorname());
                zeile.newString(stn.nachname());
            }
            zeile.newEmpty(); // Setzposition
            zeile.newInt(JGJMeldeListeDelegate.AKTIV_WERT_NIMMT_TEIL);
        }

        var meldelisteSheet = meldeListe.getXSpreadSheet();
        var startPos = Position.from(delegate.getTeamNrSpalte(), JGJMeldeListeDelegate.ERSTE_DATEN_ZEILE);
        RangeHelper.from(meldelisteSheet, getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
                data.getRangePosition(startPos)).setDataInRange(data);

        meldeListe.upDateSheet();
    }

    @Override
    protected void doRun() throws GenerateException {
        erstellenUndBefuellen();
    }

}
