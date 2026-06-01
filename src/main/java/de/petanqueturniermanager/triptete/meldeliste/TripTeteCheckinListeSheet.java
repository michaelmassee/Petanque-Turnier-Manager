package de.petanqueturniermanager.triptete.meldeliste;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.basesheet.meldeliste.AbstractTeilnehmerNamenCheckinListeSheet;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.triptete.konfiguration.TripTeteKonfigurationSheet;

/**
 * Checkin-Liste des Trip-Tête-Systems: kompakte Druckansicht zum Abhaken der Teams.
 */
public class TripTeteCheckinListeSheet extends AbstractTeilnehmerNamenCheckinListeSheet {

    private static final int MELDELISTE_ERSTE_DATEN_ZEILE = TripTeteMeldeListeDelegate.ERSTE_DATEN_ZEILE_OVERRIDE;

    private final TripTeteKonfigurationSheet konfigurationSheet;
    private final TripTeteMeldeListeSheetUpdate meldeliste;

    public TripTeteCheckinListeSheet(WorkingSpreadsheet workingSpreadsheet) {
        super(workingSpreadsheet, TurnierSystem.TRIPTETE, "Checkin Liste");
        konfigurationSheet = new TripTeteKonfigurationSheet(workingSpreadsheet);
        meldeliste = new TripTeteMeldeListeSheetUpdate(workingSpreadsheet);
    }

    @Override
    protected TripTeteKonfigurationSheet getKonfigurationSheet() {
        return konfigurationSheet;
    }

    @Override
    protected void meldelisteVorbereiten() throws GenerateException {
        meldeliste.upDateSheet();
    }

    @Override
    protected ISheet getMeldelisteSheet() {
        return meldeliste;
    }

    @Override
    protected int getMeldelisteErsteDatenZeile() {
        return MELDELISTE_ERSTE_DATEN_ZEILE;
    }

    @Override
    protected int getMeldelisteAktivSpalte() throws GenerateException {
        // TripTête hat keine Checkin-Spalte; letzteSpielTagSpalte() liegt hinter dem Datenbereich
        // → immer leer → alle Checkboxen starten unmarkiert (= noch nicht erschienen)
        return meldeliste.letzteSpielTagSpalte();
    }

    @Override
    protected Formation getFormation() {
        return Formation.TRIPLETTE;
    }

    @Override
    protected boolean istTeamnameAktiv() {
        return false;
    }

    @Override
    protected boolean istVereinsnameAktiv() {
        return false;
    }

    @Override
    protected short getSheetPos() {
        return DefaultSheetPos.TRIPTETE_WORK;
    }

    @Override
    protected String getCheckinSheetName() {
        return SheetNamen.checkinListe();
    }

    @Override
    protected String getMetadatenSchluessel() {
        return SheetMetadataHelper.SCHLUESSEL_TRIPTETE_CHECKIN_LISTE;
    }

    @Override
    public XSpreadsheet getXSpreadSheet() throws GenerateException {
        return SheetMetadataHelper.findeSheetUndHeile(
                getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
                SheetMetadataHelper.SCHLUESSEL_TRIPTETE_CHECKIN_LISTE,
                getCheckinSheetName());
    }

    @Override
    public final TurnierSheet getTurnierSheet() throws GenerateException {
        return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
    }
}
