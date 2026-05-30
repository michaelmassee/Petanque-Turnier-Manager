package de.petanqueturniermanager.triptete.meldeliste;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.basesheet.meldeliste.AbstractTeilnehmerNamenCheckinListeSheet;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeKonstanten;
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

    private static final int MELDELISTE_ERSTE_DATEN_ZEILE = MeldeListeKonstanten.ERSTE_DATEN_ZEILE;

    /**
     * Spalte jenseits der Namensspalte – in TripTête gibt es keine Aktiv-Spalte.
     * Da alle Teams immer aktiv sind, liefert diese leere Spalte stets blank → Checkboxen
     * starten unmarkiert (= noch nicht erschienen).
     */
    private static final int MELDELISTE_AKTIV_SPALTE = MeldeListeKonstanten.SPIELER_NR_SPALTE + 2;

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
    protected int getMeldelisteAktivSpalte() {
        return MELDELISTE_AKTIV_SPALTE;
    }

    @Override
    protected Formation getFormation() {
        return Formation.TETE;
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
