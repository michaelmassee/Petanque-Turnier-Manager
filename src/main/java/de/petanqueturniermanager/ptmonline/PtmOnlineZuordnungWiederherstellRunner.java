/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.io.IOException;

import org.jspecify.annotations.Nullable;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.LibreOfficePtmOnlineSpeicher;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.spielerdb.MeldelisteZiel;
import de.petanqueturniermanager.spielerdb.MeldelisteZielFactory;

/** Menüpunkt „Zuordnung vom Server wiederherstellen“ (T-21, P-66). */
public final class PtmOnlineZuordnungWiederherstellRunner extends SheetRunner {

    private final @Nullable Integer spieltagNr;
    private final LibreOfficePtmOnlineSpeicher.Zugangsdaten config;

    public PtmOnlineZuordnungWiederherstellRunner(WorkingSpreadsheet ws, TurnierSystem turnierSystem,
            @Nullable Integer spieltagNr, LibreOfficePtmOnlineSpeicher.Zugangsdaten config) {
        super(ws, turnierSystem, "PTM-Online");
        this.spieltagNr = spieltagNr;
        this.config = config;
    }

    @Override
    protected IKonfigurationSheet getKonfigurationSheet() {
        return null;
    }

    @Override
    protected void doRun() throws GenerateException {
        PtmOnlineRegistrationMapping mapping = new PtmOnlineRegistrationMapping(getWorkingSpreadsheet(),
                getTurnierSystem(), spieltagNr);
        String tournamentId = mapping.getTournamentId()
                .orElseThrow(() -> new GenerateException(I18n.get("ptmonline.fehler.turnier_nicht_verbunden")));
        String documentId = mapping.getSyncDocumentId()
                .orElseThrow(() -> new GenerateException(I18n.get("ptmonline.fehler.dokumentbindung_unvollstaendig")));
        String leaseToken = mapping.getLeaseToken()
                .orElseThrow(() -> new GenerateException(I18n.get("ptmonline.fehler.dokumentbindung_unvollstaendig")));
        MeldelisteZiel ziel = MeldelisteZielFactory.fuerPtmOnline(getWorkingSpreadsheet())
                .orElseThrow(() -> new GenerateException(I18n.get("ptmonline.fehler.meldeliste_fehlt")));
        int anzahl;
        try {
            anzahl = PtmOnlineZuordnungWiederherstellung.ausfuehren(
                    new TournamentSyncClient(config.baseUrl(), config.apiKey(), documentId, leaseToken), mapping,
                    tournamentId, ziel);
        } catch (IOException e) {
            throw new GenerateException(PtmOnlineFehlerText.fuer(e));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw verarbeitungAbgebrochen();
        }
        MessageBox.from(getxContext(), MessageBoxTypeEnum.INFO_OK).caption(I18n.get("ptmonline.menu.toplevel"))
                .message(I18n.get("ptmonline.erfolg.zuordnungen_wiederhergestellt", anzahl)).show();
    }
}
