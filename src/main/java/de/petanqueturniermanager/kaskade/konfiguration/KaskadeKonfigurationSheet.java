/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.kaskade.konfiguration;

import de.petanqueturniermanager.basesheet.konfiguration.BaseKonfigurationSheet;
import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.basesheet.konfiguration.IPropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.pagestyle.PageStyle;
import de.petanqueturniermanager.helper.pagestyle.PageStyleHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Konfigurationssheet für das Kaskaden-KO-Turniersystem.
 */
public class KaskadeKonfigurationSheet extends BaseKonfigurationSheet {

    private final KaskadePropertiesSpalte propertiesSpalte;

    public KaskadeKonfigurationSheet(WorkingSpreadsheet workingSpreadsheet) {
        super(workingSpreadsheet, TurnierSystem.KASKADE);
        propertiesSpalte = new KaskadePropertiesSpalte(this);
    }

    @Override
    protected IKonfigurationSheet getKonfigurationSheet() {
        return this;
    }

    @Override
    protected void doRun() throws GenerateException {
        TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet()).setActiv();
    }

    @Override
    public IPropertiesSpalte getPropertiesSpalte() {
        return propertiesSpalte;
    }

    public KaskadePropertiesSpalte getKaskadePropertiesSpalte() {
        return propertiesSpalte;
    }

    @Override
    protected void initPageStylesTurnierSystem() {
        PageStyleHelper.from(this, PageStyle.PETTURNMNGR).initDefaultFooter()
                .setHeaderLeft(getKopfZeileLinks())
                .setHeaderCenter(getKopfZeileMitte())
                .setHeaderRight(getKopfZeileRechts())
                .create();
    }

    public String getKopfZeileLinks() {
        return propertiesSpalte.getKopfZeileLinks();
    }

    public String getKopfZeileMitte() {
        return propertiesSpalte.getKopfZeileMitte();
    }

    public void setKopfZeileMitte(String text) {
        propertiesSpalte.setKopfZeileMitte(text);
    }

    public String getKopfZeileRechts() {
        return propertiesSpalte.getKopfZeileRechts();
    }

    public Formation getMeldeListeFormation() {
        return propertiesSpalte.getMeldeListeFormation();
    }

    public void setMeldeListeFormation(Formation formation) {
        propertiesSpalte.setMeldeListeFormation(formation);
    }

    public boolean isMeldeListeTeamnameAnzeigen() {
        return propertiesSpalte.isMeldeListeTeamnameAnzeigen();
    }

    public void setMeldeListeTeamnameAnzeigen(boolean anzeigen) {
        propertiesSpalte.setMeldeListeTeamnameAnzeigen(anzeigen);
    }

    public boolean isMeldeListeVereinsnameAnzeigen() {
        return propertiesSpalte.isMeldeListeVereinsnameAnzeigen();
    }

    public void setMeldeListeVereinsnameAnzeigen(boolean anzeigen) {
        propertiesSpalte.setMeldeListeVereinsnameAnzeigen(anzeigen);
    }

    public int getKaskadenTabFarbe() {
        return propertiesSpalte.getKaskadenTabFarbe();
    }

    public int getAnzahlKaskaden() {
        return propertiesSpalte.getAnzahlKaskaden();
    }

    public void setAnzahlKaskaden(int anzahl) {
        propertiesSpalte.setAnzahlKaskaden(anzahl);
    }

    public int getAktiveKaskadenRunde() {
        return propertiesSpalte.getAktiveKaskadenRunde();
    }

    public void setAktiveKaskadenRunde(int rundeNr) {
        propertiesSpalte.setAktiveKaskadenRunde(rundeNr);
    }

    public boolean isKoFelderErstellt() {
        return propertiesSpalte.isKoFelderErstellt();
    }

    public void setKoFelderErstellt(boolean erstellt) {
        propertiesSpalte.setKoFelderErstellt(erstellt);
    }
}
