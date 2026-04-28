/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.formulex.meldeliste;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeKonstanten;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.formulex.konfiguration.FormuleXKonfigurationSheet;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.SortHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Aktualisiert die Formule X Meldeliste:
 * Nummern vergeben → nach Nr sortieren → Blatt formatieren.
 */
public class FormuleXMeldeListeSheetUpdate extends SheetRunner implements ISheet, MeldeListeKonstanten {

    private static final Logger logger = LogManager.getLogger(FormuleXMeldeListeSheetUpdate.class);

    protected static final int ERSTE_DATEN_ZEILE = FormuleXListeDelegate.ERSTE_DATEN_ZEILE;
    protected static final int MIN_ANZAHL_MELDUNGEN_ZEILEN = FormuleXListeDelegate.MIN_ANZAHL_MELDUNGEN_ZEILEN;

    public static final int AKTIV_WERT_NIMMT_TEIL = FormuleXListeDelegate.AKTIV_WERT_NIMMT_TEIL;
    public static final int AKTIV_WERT_AUSGESTIEGEN = FormuleXListeDelegate.AKTIV_WERT_AUSGESTIEGEN;

    private static final String METADATA_SCHLUESSEL = SheetMetadataHelper.SCHLUESSEL_FORMULEX_MELDELISTE;

    private final FormuleXListeDelegate delegate;

    public FormuleXMeldeListeSheetUpdate(WorkingSpreadsheet workingSpreadsheet) {
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

    public TeamMeldungen getAlleMeldungen() throws GenerateException {
        return delegate.getAlleMeldungen();
    }

    public void alleTeamsAktivieren() throws GenerateException {
        delegate.alleTeamsAktivieren();
    }

    public TeamMeldungen getMeldungenSortiertNachSetzposition() throws GenerateException {
        return delegate.getMeldungenSortiertNachSetzposition();
    }

    public int letzteZeileMitDaten(XSpreadsheet xSheet) throws GenerateException {
        return delegate.letzteZeileMitDaten(xSheet);
    }

    /**
     * Sortiert die Meldeliste nach der angegebenen Spalte.
     */
    public void doSort(int spalteNr, boolean aufsteigend) throws GenerateException {
        XSpreadsheet xSheet = getXSpreadSheet();
        int letzteZeile = delegate.letzteZeileMitDaten(xSheet);
        if (letzteZeile < ERSTE_DATEN_ZEILE) {
            return;
        }
        RangePosition range = RangePosition.from(getNrSpalte(), ERSTE_DATEN_ZEILE,
                delegate.getAktivSpalte(), letzteZeile);
        SortHelper.from(this, range).spalteToSort(spalteNr).aufSteigendSortieren(aufsteigend).doSort();
    }

    public String validiereSetzpositionSpalte() {
        return delegate.validiereSetzpositionSpalte();
    }

    private void stringsBesinigen(XSpreadsheet xSheet) throws GenerateException {
        delegate.stringsBesinigen(xSheet);
    }

    protected void pruefeAufDoppelteTeamNr(XSpreadsheet xSheet) throws GenerateException {
        delegate.pruefeAufDoppelteTeamNr(xSheet);
    }

    // ---------------------------------------------------------------
    // Eigene Methoden
    // ---------------------------------------------------------------

    @Override
    protected void doRun() throws GenerateException {
        vollstaendigAktualisieren();
    }

    /**
     * Bereinigt, vergibt Nummern, setzt Aktiv-Standard, prüft auf Duplikate,
     * sortiert und aktualisiert das Sheet vollständig.
     *
     * @return {@code true} wenn das Sheet gefunden und aktualisiert wurde, {@code false} wenn kein Sheet vorhanden
     */
    public boolean vollstaendigAktualisieren() throws GenerateException {
        XSpreadsheet xSheet = getXSpreadSheet();
        if (xSheet == null) {
            logger.warn("Formule X Meldeliste nicht gefunden");
            return false;
        }
        stringsBesinigen(xSheet);
        teamnummernVergeben(xSheet);
        aktivDefaultSetzen(xSheet);
        pruefeAufDoppelteTeamNr(xSheet);
        nachNrSortieren(xSheet);
        upDateSheet();
        return true;
    }

    private void aktivDefaultSetzen(XSpreadsheet xSheet) throws GenerateException {
        int letzteZeile = delegate.letzteZeileMitDaten(xSheet);
        if (letzteZeile < ERSTE_DATEN_ZEILE) {
            return;
        }
        int vornameSpalte = delegate.getVornameSpalte(0);
        int aktivSpalte = delegate.getAktivSpalte();

        for (int zeile = ERSTE_DATEN_ZEILE; zeile <= letzteZeile; zeile++) {
            String vorname = getSheetHelper().getTextFromCell(xSheet, Position.from(vornameSpalte, zeile));
            if (vorname == null || vorname.isBlank()) {
                continue;
            }
            int aktiv = getSheetHelper().getIntFromCell(xSheet, Position.from(aktivSpalte, zeile));
            if (aktiv <= 0) {
                getSheetHelper().setNumberValueInCell(
                        NumberCellValue.from(xSheet, Position.from(aktivSpalte, zeile))
                                .setValue(AKTIV_WERT_NIMMT_TEIL));
            }
        }
    }

    private void teamnummernVergeben(XSpreadsheet xSheet) throws GenerateException {
        int letzteZeile = delegate.letzteZeileMitDaten(xSheet);
        if (letzteZeile < ERSTE_DATEN_ZEILE) {
            return;
        }

        int nrSpalte = getNrSpalte();
        int vornameSpalte = delegate.getVornameSpalte(0);

        RangePosition sortRange = RangePosition.from(nrSpalte, ERSTE_DATEN_ZEILE, delegate.getAktivSpalte(),
                letzteZeile);
        SortHelper.from(this, sortRange).spalteToSort(nrSpalte).abSteigendSortieren().doSort();

        int letztNr = Math.max(0,
                getSheetHelper().getIntFromCell(xSheet, Position.from(nrSpalte, ERSTE_DATEN_ZEILE)));

        for (int zeile = ERSTE_DATEN_ZEILE; zeile <= letzteZeile; zeile++) {
            String vorname = getSheetHelper().getTextFromCell(xSheet, Position.from(vornameSpalte, zeile));
            if (vorname == null || vorname.isBlank()) {
                continue;
            }
            int nr = getSheetHelper().getIntFromCell(xSheet, Position.from(nrSpalte, zeile));
            if (nr <= 0) {
                letztNr++;
                getSheetHelper().setNumberValueInCell(
                        NumberCellValue.from(xSheet, Position.from(nrSpalte, zeile)).setValue(letztNr));
            }
        }
    }

    private void nachNrSortieren(XSpreadsheet xSheet) throws GenerateException {
        int letzteZeile = delegate.letzteZeileMitDaten(xSheet);
        if (letzteZeile < ERSTE_DATEN_ZEILE) {
            return;
        }
        RangePosition range = RangePosition.from(getNrSpalte(), ERSTE_DATEN_ZEILE,
                delegate.getAktivSpalte(), letzteZeile);
        SortHelper.from(this, range).spalteToSort(getNrSpalte()).aufSteigendSortieren(true).doSort();
    }

}
