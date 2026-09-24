/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.ui;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.ptmonline.PtmOnlineRegistrationMapping;

/**
 * Lokale Änderungen an einer PTM-Online-Verbindung ohne Server-Aufruf – funktionieren daher auch offline. Läuft
 * als {@link SheetRunner}: eigener Blattschutz-Scope (das Blatt „PTMOnline Sync“ ist im Turnier-Modus gesperrt),
 * und die Sidebar aktualisiert ihren Verbindungsstatus über den Runner-Zustandswechsel.
 */
final class PtmOnlineVerbindungsRunner extends SheetRunner {

    enum Aktion {
        /** Sync anhalten: kein Meldungsabgleich, kein Rundenstart-Sync. Die Verbindung bleibt bestehen. */
        PAUSIEREN,
        FORTSETZEN,
        /** Blatt „PTMOnline Sync“ entfernen, nachdem die Verbindung online gelöst wurde (oder nicht mehr gilt). */
        LOKAL_ENTFERNEN
    }

    private final Aktion aktion;
    private final Integer spieltagNr;

    PtmOnlineVerbindungsRunner(WorkingSpreadsheet ws, TurnierSystem turnierSystem, Integer spieltagNrOderNull,
            Aktion aktion) {
        super(ws, turnierSystem, "PTM-Online");
        this.aktion = aktion;
        this.spieltagNr = spieltagNrOderNull;
    }

    @Override
    protected IKonfigurationSheet getKonfigurationSheet() {
        return null;
    }

    @Override
    protected void doRun() throws GenerateException {
        PtmOnlineRegistrationMapping mapping = new PtmOnlineRegistrationMapping(getWorkingSpreadsheet(),
                getTurnierSystem(), spieltagNr);
        switch (aktion) {
            case LOKAL_ENTFERNEN -> {
                mapping.trennen();
                zeigeInfo(I18n.get("ptmonline.erfolg.verbindung_getrennt"));
            }
            case PAUSIEREN, FORTSETZEN -> schalteSync(mapping, aktion == Aktion.PAUSIEREN);
        }
    }

    private void schalteSync(PtmOnlineRegistrationMapping mapping, boolean pausieren) throws GenerateException {
        if (mapping.getTournamentId().isEmpty()) {
            MessageBox.from(getxContext(), MessageBoxTypeEnum.ERROR_OK)
                    .caption(I18n.get("ptmonline.fehler.titel"))
                    .message(I18n.get("ptmonline.fehler.turnier_nicht_verbunden"))
                    .show();
            return;
        }
        if (mapping.istPausiert() == pausieren) {
            zeigeInfo(I18n.get(pausieren ? "ptmonline.hinweis.sync_bereits_pausiert"
                    : "ptmonline.hinweis.sync_bereits_aktiv"));
            return;
        }
        mapping.setPausiert(pausieren);
        zeigeInfo(I18n.get(pausieren ? "ptmonline.erfolg.sync_pausiert" : "ptmonline.erfolg.sync_fortgesetzt"));
    }

    private void zeigeInfo(String meldung) {
        MessageBox.from(getxContext(), MessageBoxTypeEnum.INFO_OK)
                .caption(I18n.get("ptmonline.menu.toplevel"))
                .message(meldung)
                .show();
    }
}
