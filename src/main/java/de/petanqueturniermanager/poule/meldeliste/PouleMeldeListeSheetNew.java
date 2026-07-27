/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.poule.meldeliste;

import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.uno.Exception;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.IMeldeliste;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeKonstanten;
import de.petanqueturniermanager.basesheet.meldeliste.MeldungenSpalte;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.NewTestDatenValidator;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.poule.konfiguration.PouleKonfigurationSheet;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;

/**
 * Erstellt eine neue Poule-A/B-Meldeliste nach Benutzereingabe im Parameter-Dialog.
 */
public class PouleMeldeListeSheetNew extends SheetRunner
        implements IMeldeliste<TeamMeldungen, Team>, MeldeListeKonstanten {

    private static final Logger logger = LogManager.getLogger(PouleMeldeListeSheetNew.class);

    protected static final int ERSTE_DATEN_ZEILE = PouleListeDelegate.ERSTE_DATEN_ZEILE;

    private static final String METADATA_SCHLUESSEL = SheetMetadataHelper.SCHLUESSEL_POULE_MELDELISTE;

    private final PouleListeDelegate delegate;

    public PouleMeldeListeSheetNew(WorkingSpreadsheet workingSpreadsheet) {
        super(workingSpreadsheet, TurnierSystem.POULE, "Poule-Meldeliste");
        delegate = new PouleListeDelegate(this);
    }

    @Override
    public XSpreadsheet getXSpreadSheet() throws GenerateException {
        return SheetMetadataHelper.findeSheetUndHeile(
                getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
                METADATA_SCHLUESSEL, SheetNamen.pouleMeldeliste());
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

    public void upDateSheet() throws GenerateException {
        delegate.upDateSheet();
    }

    public int getTeamNrSpalte() {
        return delegate.getTeamNrSpalte();
    }

    public int getTeamnameSpalte() throws GenerateException {
        return delegate.getTeamnameSpalte();
    }

    @Override
    public int getSpielerNameErsteSpalte() {
        try {
            return delegate.getSpielerNameErsteSpalte();
        } catch (GenerateException e) {
            throw new IllegalStateException(e);
        }
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

    public int getSetzPositionSpalte() throws GenerateException {
        return delegate.getSetzPositionSpalte();
    }

    public int getAktivSpalte() throws GenerateException {
        return delegate.getAktivSpalte();
    }

    @Override
    public int getErsteDatenZiele() {
        return delegate.getErsteDatenZiele();
    }

    @Override
    public TeamMeldungen getAktiveMeldungen() throws GenerateException {
        return delegate.getAktiveMeldungen();
    }

    @Override
    public TeamMeldungen getAlleMeldungen() throws GenerateException {
        return delegate.getAlleMeldungen();
    }

    @Override
    public TeamMeldungen getAktiveUndAusgesetztMeldungen() throws GenerateException {
        return getAlleMeldungen();
    }

    @Override
    public TeamMeldungen getInAktiveMeldungen() throws GenerateException {
        return new TeamMeldungen();
    }

    @Override
    public MeldungenSpalte<TeamMeldungen, Team> getMeldungenSpalte() {
        try {
            return delegate.getMeldungenSpalte();
        } catch (GenerateException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String formulaSverweisSpielernamen(String spielrNrAdresse) {
        try {
            return delegate.formulaSverweisSpielernamen(spielrNrAdresse);
        } catch (GenerateException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public int letzteSpielTagSpalte() throws GenerateException {
        return delegate.getAktivSpalte();
    }

    @Override
    public int getLetzteDatenZeileUseMin() throws GenerateException {
        return delegate.getLetzteDatenZeileUseMin();
    }

    @Override
    public int getLetzteMitDatenZeileInSpielerNrSpalte() throws GenerateException {
        return delegate.getLetzteMitDatenZeileInSpielerNrSpalte();
    }

    @Override
    public int naechsteFreieDatenZeileInSpielerNrSpalte() throws GenerateException {
        return delegate.naechsteFreieDatenZeileInSpielerNrSpalte();
    }

    @Override
    public int letzteZeileMitSpielerName() throws GenerateException {
        return delegate.letzteZeileMitSpielerName();
    }

    @Override
    public int getSpielerZeileNr(int spielerNr) throws GenerateException {
        return delegate.getSpielerZeileNr(spielerNr);
    }

    @Override
    public List<String> getSpielerNamenList() throws GenerateException {
        return delegate.getSpielerNamenList();
    }

    @Override
    public List<Integer> getSpielerNrList() throws GenerateException {
        return delegate.getSpielerNrList();
    }

    // ---------------------------------------------------------------
    // Eigene Methoden
    // ---------------------------------------------------------------

    @Override
    protected boolean isUpdateKonfigurationSheetBeforeDoRun() {
        return false;
    }

    @Override
    protected void doRun() throws GenerateException {
        if (!NewTestDatenValidator.from(getWorkingSpreadsheet(), getSheetHelper(), TurnierSystem.POULE)
                .prefix(getLogPrefix()).validate()) {
            return;
        }

        Optional<PouleTurnierParameterDialog.TurnierParameter> param;
        try {
            param = PouleTurnierParameterDialog.from(getWorkingSpreadsheet())
                    .anzeigen(Formation.TRIPLETTE, false, false);
        } catch (Exception e) {
            String errMsg = I18n.get("error.dialog.parameterdialog", e.getMessage());
            logger.error(errMsg, e);
            throw new GenerateException(errMsg);
        }

        if (param.isEmpty()) {
            return;
        }

        getKonfigurationSheet().update();
        getSheetHelper().removeAllSheetsExclude();
        createMeldelisteWithParams(param.get().formation(), param.get().teamnameAnzeigen(), param.get().vereinsnameAnzeigen());
    }

    /**
     * Erstellt die Meldeliste mit den angegebenen Parametern ohne Dialog.
     * Wird auch von TestDaten-Klassen aufgerufen.
     */
    public void createMeldelisteWithParams(Formation formation, boolean teamnameAnzeigen, boolean vereinsnameAnzeigen)
            throws GenerateException {
        logger.info("Erstelle Poule-Meldeliste: formation={}, teamname={}, verein={}", formation, teamnameAnzeigen,
                vereinsnameAnzeigen);

        var neuesSheet = NewSheet.from(this, SheetNamen.pouleMeldeliste(), METADATA_SCHLUESSEL)
                .pos(DefaultSheetPos.MELDELISTE).hideGrid().tabColor(getKonfigurationSheet().getMeldelisteTabFarbe()).setDocVersionWhenNew().create();
        if (neuesSheet.isDidCreate()) {
            getKonfigurationSheet().setMeldeListeFormation(formation);
            getKonfigurationSheet().setMeldeListeTeamnameAnzeigen(teamnameAnzeigen);
            getKonfigurationSheet().setMeldeListeVereinsnameAnzeigen(vereinsnameAnzeigen);
            upDateSheet();
        }
    }
}
