/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.poule.konfiguration;

import de.petanqueturniermanager.basesheet.konfiguration.BaseKonfigurationSheet;
import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.pagestyle.PageStyle;
import de.petanqueturniermanager.helper.pagestyle.PageStyleHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Konfigurationsblatt für das Poule-A/B-Turniersystem.
 */
public class PouleKonfigurationSheet extends BaseKonfigurationSheet implements IPoulePropertiesSpalte {

    private final PoulePropertiesSpalte propertiesSpalte;

    public PouleKonfigurationSheet(WorkingSpreadsheet workingSpreadsheet) {
        super(workingSpreadsheet, TurnierSystem.POULE);
        propertiesSpalte = new PoulePropertiesSpalte(this);
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
    public PoulePropertiesSpalte getPropertiesSpalte() {
        return propertiesSpalte;
    }

    @Override
    protected void initPageStylesTurnierSystem() {
        PageStyleHelper.from(this, PageStyle.PETTURNMNGR).initDefaultFooter().create();
    }

    @Override
    public Formation getMeldeListeFormation() {
        return propertiesSpalte.getMeldeListeFormation();
    }

    @Override
    public boolean isMeldeListeTeamnameAnzeigen() {
        return propertiesSpalte.isMeldeListeTeamnameAnzeigen();
    }

    @Override
    public boolean isMeldeListeVereinsnameAnzeigen() {
        return propertiesSpalte.isMeldeListeVereinsnameAnzeigen();
    }

    @Override
    public void setMeldeListeFormation(Formation formation) {
        propertiesSpalte.setMeldeListeFormation(formation);
    }

    @Override
    public void setMeldeListeTeamnameAnzeigen(boolean anzeigen) {
        propertiesSpalte.setMeldeListeTeamnameAnzeigen(anzeigen);
    }

    @Override
    public void setMeldeListeVereinsnameAnzeigen(boolean anzeigen) {
        propertiesSpalte.setMeldeListeVereinsnameAnzeigen(anzeigen);
    }

    @Override
    public boolean isSpielplanMitBahnspalte() {
        return propertiesSpalte.isSpielplanMitBahnspalte();
    }

    @Override
    public void setSpielplanMitBahnspalte(boolean mitBahnspalte) {
        propertiesSpalte.setSpielplanMitBahnspalte(mitBahnspalte);
    }

}
