/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.formulex.spielrunde;

import de.petanqueturniermanager.basesheet.konfiguration.BaseKonfigurationSheet;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.basesheet.spielrunde.AbstractSpielrundeWertungUmschalten;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.formulex.konfiguration.FormuleXKonfigurationSheet;
import de.petanqueturniermanager.formulex.rangliste.FormuleXRanglisteSheet;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;

/**
 * Formule X: aktive Spielrunde werten bzw. nicht werten.
 */
public class FormuleXSpielrundeWertungUmschalten extends AbstractSpielrundeWertungUmschalten {

    private final FormuleXKonfigurationSheet konfigurationSheet;

    public FormuleXSpielrundeWertungUmschalten(WorkingSpreadsheet workingSpreadsheet) {
        super(workingSpreadsheet, TurnierSystem.FORMULEX);
        konfigurationSheet = new FormuleXKonfigurationSheet(workingSpreadsheet);
    }

    @Override
    protected BaseKonfigurationSheet getKonfigurationSheet() {
        return konfigurationSheet;
    }

    @Override
    protected String getRundenSchluesselPraefix() {
        return SheetMetadataHelper.SCHLUESSEL_FORMULEX_SPIELRUNDE_PREFIX;
    }

    @Override
    protected int letzteTabellenSpalte() {
        return konfigurationSheet.isZeitplanAktiv() ? FormuleXAbstractSpielrundeSheet.ZEIT_SPALTE
                : FormuleXAbstractSpielrundeSheet.ERG_TEAM_B_SPALTE;
    }

    @Override
    protected void ranglisteNeuAufbauen(String rundenSchluessel) throws GenerateException {
        wennVorhanden(SheetMetadataHelper.SCHLUESSEL_FORMULEX_RANGLISTE,
                () -> new FormuleXRanglisteSheet(getWorkingSpreadsheet()).doRun());
    }
}
