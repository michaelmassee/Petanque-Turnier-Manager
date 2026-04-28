/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.formulex.konfiguration;

import de.petanqueturniermanager.basesheet.konfiguration.BaseKonfigurationSheet;
import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.cellstyle.SpielrundeHintergrundFarbeGeradeStyle;
import de.petanqueturniermanager.helper.cellstyle.SpielrundeHintergrundFarbeUnGeradeStyle;
import de.petanqueturniermanager.helper.pagestyle.PageStyle;
import de.petanqueturniermanager.helper.pagestyle.PageStyleHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Konfigurationssheet für das Formule X Turniersystem.
 */
public class FormuleXKonfigurationSheet extends BaseKonfigurationSheet {

    private final FormuleXPropertiesSpalte propertiesSpalte;

    public FormuleXKonfigurationSheet(WorkingSpreadsheet workingSpreadsheet) {
        super(workingSpreadsheet, TurnierSystem.FORMULEX);
        propertiesSpalte = new FormuleXPropertiesSpalte(this);
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
    public FormuleXPropertiesSpalte getPropertiesSpalte() {
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

    public void setAktiveSpielRunde(SpielRundeNr spielrunde) {
        propertiesSpalte.setAktiveSpielRunde(spielrunde);
    }

    public SpielRundeNr getAktiveSpielRunde() {
        return propertiesSpalte.getAktiveSpielRunde();
    }

    public int getAnzahlRunden() {
        return propertiesSpalte.getAnzahlRunden();
    }

    public void setAnzahlRunden(int anzahl) {
        propertiesSpalte.setAnzahlRunden(anzahl);
    }

    public SpielrundeSpielbahn getSpielrundeSpielbahn() {
        return propertiesSpalte.getSpielrundeSpielbahn();
    }

    public void setSpielrundeSpielbahn(SpielrundeSpielbahn option) {
        propertiesSpalte.setSpielrundeSpielbahn(option);
    }

    public Integer getSpielRundeHintergrundFarbeGerade() {
        return propertiesSpalte.getSpielRundeHintergrundFarbeGerade();
    }

    public SpielrundeHintergrundFarbeGeradeStyle getSpielRundeHintergrundFarbeGeradeStyle() {
        return propertiesSpalte.getSpielRundeHintergrundFarbeGeradeStyle();
    }

    public Integer getSpielRundeHintergrundFarbeUnGerade() {
        return propertiesSpalte.getSpielRundeHintergrundFarbeUnGerade();
    }

    public SpielrundeHintergrundFarbeUnGeradeStyle getSpielRundeHintergrundFarbeUnGeradeStyle() {
        return propertiesSpalte.getSpielRundeHintergrundFarbeUnGeradeStyle();
    }

    public Integer getSpielRundeHeaderFarbe() {
        return propertiesSpalte.getSpielRundeHeaderFarbe();
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
}
