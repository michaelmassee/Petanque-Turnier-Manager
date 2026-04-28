/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.formulex.meldeliste;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeKonstanten;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.formulex.konfiguration.FormuleXKonfigurationSheet;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.NewTestDatenValidator;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Erstellt ein neues Formule X Turnier: Konfigurationsblatt + Meldeliste.
 */
public class FormuleXMeldeListeSheetNew extends SheetRunner implements ISheet, MeldeListeKonstanten {

    private static final Logger logger = LogManager.getLogger(FormuleXMeldeListeSheetNew.class);

    protected static final int ERSTE_DATEN_ZEILE = FormuleXListeDelegate.ERSTE_DATEN_ZEILE;

    private static final String METADATA_SCHLUESSEL = SheetMetadataHelper.SCHLUESSEL_FORMULEX_MELDELISTE;

    private final FormuleXListeDelegate delegate;

    public FormuleXMeldeListeSheetNew(WorkingSpreadsheet workingSpreadsheet) {
        super(workingSpreadsheet, TurnierSystem.FORMULEX, "Formule X Meldeliste");
        delegate = new FormuleXListeDelegate(this);
    }

    @Override
    public XSpreadsheet getXSpreadSheet() throws GenerateException {
        return SheetMetadataHelper.findeSheetUndHeile(
                getWorkingSpreadsheet().getWorkingSpreadsheetDocument(), METADATA_SCHLUESSEL, SheetNamen.LEGACY_MELDELISTE);
    }

    @Override
    public TurnierSheet getTurnierSheet() throws GenerateException {
        return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
    }

    @Override
    protected FormuleXKonfigurationSheet getKonfigurationSheet() {
        return delegate.getKonfigurationSheet();
    }

    // ---------------------------------------------------------------
    // Forwarding-Methoden → Delegate
    // ---------------------------------------------------------------

    public void upDateSheet() throws GenerateException {
        delegate.upDateSheet();
    }

    public int getNrSpalte() {
        return delegate.getNrSpalte();
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

    public int getAktivSpalte() throws GenerateException {
        return delegate.getAktivSpalte();
    }

    public int getSetzPositionSpalte() throws GenerateException {
        return delegate.getSetzPositionSpalte();
    }

    public int getErsteDatenZeile() {
        return delegate.getErsteDatenZeile();
    }

    public TeamMeldungen getAktiveMeldungen() throws GenerateException {
        return delegate.getAktiveMeldungen();
    }

    public TeamMeldungen getMeldungenSortiertNachSetzposition() throws GenerateException {
        return delegate.getMeldungenSortiertNachSetzposition();
    }

    // ---------------------------------------------------------------
    // Eigene Methoden
    // ---------------------------------------------------------------

    @Override
    protected boolean isUpdateKonfigurationSheetBeforeDoRun() {
        return false;
    }

    /**
     * Erstellt die Meldeliste mit den angegebenen Parametern ohne Dialog.
     * Wird auch von TestDaten-Klassen aufgerufen.
     */
    public void createMeldelisteWithParams(Formation formation, boolean teamnameAnzeigen,
            boolean vereinsnameAnzeigen, int anzahlRunden) throws GenerateException {
        logger.info("Erstelle Formule X Meldeliste: formation={}, teamname={}, verein={}, runden={}",
                formation, teamnameAnzeigen, vereinsnameAnzeigen, anzahlRunden);

        var neuesSheet = NewSheet.from(this, SheetNamen.meldeliste(), METADATA_SCHLUESSEL)
                .pos(DefaultSheetPos.MELDELISTE).hideGrid().tabColor(getKonfigurationSheet().getMeldelisteTabFarbe()).setDocVersionWhenNew().create();
        if (neuesSheet.isDidCreate()) {
            getKonfigurationSheet().setMeldeListeFormation(formation);
            getKonfigurationSheet().setMeldeListeTeamnameAnzeigen(teamnameAnzeigen);
            getKonfigurationSheet().setMeldeListeVereinsnameAnzeigen(vereinsnameAnzeigen);
            getKonfigurationSheet().setAnzahlRunden(anzahlRunden);
            upDateSheet();
        }
    }

    @Override
    protected void doRun() throws GenerateException {
        if (!NewTestDatenValidator.from(getWorkingSpreadsheet(), getSheetHelper(), TurnierSystem.FORMULEX)
                .prefix(getLogPrefix()).validate()) {
            return;
        }

        Optional<FormuleXTurnierParameterDialog.TurnierParameter> param;
        try {
            param = FormuleXTurnierParameterDialog.from(getWorkingSpreadsheet())
                    .anzeigen(Formation.TRIPLETTE, true, false, 4);
        } catch (com.sun.star.uno.Exception e) {
            String errMsg = I18n.get("error.dialog.parameterdialog", e.getMessage());
            logger.error(errMsg, e);
            throw new GenerateException(errMsg);
        }

        if (param.isEmpty()) {
            return;
        }

        getKonfigurationSheet().update();
        getSheetHelper().removeAllSheetsExclude();
        createMeldelisteWithParams(
                param.get().formation(),
                param.get().teamnameAnzeigen(),
                param.get().vereinsnameAnzeigen(),
                param.get().anzahlRunden());

        logger.info("Formule X Meldeliste erstellt.");
    }

}
