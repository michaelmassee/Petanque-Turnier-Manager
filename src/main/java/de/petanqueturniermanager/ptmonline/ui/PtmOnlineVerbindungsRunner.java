/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.ui;

import java.io.IOException;
import java.util.Optional;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.LibreOfficePtmOnlineSpeicher;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.ptmonline.PtmOnlineFehlerText;
import de.petanqueturniermanager.ptmonline.PtmOnlineHttpException;
import de.petanqueturniermanager.ptmonline.PtmOnlineRegistrationMapping;
import de.petanqueturniermanager.ptmonline.TournamentSyncClient;

/**
 * Änderungen an einer bestehenden PTM-Online-Verbindung. Pausieren/Fortsetzen kommen ohne Server-Aufruf aus und
 * funktionieren daher auch offline; Trennen löst die Bindung online und entfernt danach das Blatt. Läuft als
 * {@link SheetRunner}: eigener Blattschutz-Scope (das Blatt „PTMOnline Sync“ ist im Turnier-Modus gesperrt), kein
 * paralleler Lauf, und die Sidebar aktualisiert ihren Verbindungsstatus über den Runner-Zustandswechsel.
 */
final class PtmOnlineVerbindungsRunner extends SheetRunner {

    enum Aktion {
        /** Sync anhalten: kein Meldungsabgleich, kein Rundenstart-Sync. Die Verbindung bleibt bestehen. */
        PAUSIEREN,
        FORTSETZEN,
        /** Verbindung online lösen und danach das Blatt „PTMOnline Sync“ entfernen. */
        TRENNEN
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
            case TRENNEN -> {
                onlineTrennen(mapping);
                mapping.trennen();
                zeigeInfo(I18n.get("ptmonline.erfolg.verbindung_getrennt"));
            }
            case PAUSIEREN, FORTSETZEN -> schalteSync(mapping, aktion == Aktion.PAUSIEREN);
        }
    }

    /**
     * Hebt serverseitig die Dokument-Verwaltung auf. Hält online inzwischen ein anderes Dokument das Turnier (oder
     * niemand), wird trotzdem lokal aufgeräumt.
     */
    private void onlineTrennen(PtmOnlineRegistrationMapping mapping) throws GenerateException {
        Optional<String> tournamentId = mapping.getTournamentId();
        if (tournamentId.isEmpty()) {
            throw new GenerateException(I18n.get("ptmonline.fehler.turnier_nicht_verbunden"));
        }
        Optional<String> documentId = mapping.getSyncDocumentId();
        Optional<String> leaseToken = mapping.getLeaseToken();
        if (documentId.isEmpty() || leaseToken.isEmpty()) {
            throw new GenerateException(I18n.get("ptmonline.fehler.dokumentbindung_unvollstaendig"));
        }
        var config = new LibreOfficePtmOnlineSpeicher(getxContext()).laden();
        try {
            new TournamentSyncClient(config.baseUrl(), config.apiKey(), documentId.get(), leaseToken.get())
                    .disconnect(tournamentId.get());
            getLogger().info("PTM-Online: Turnier {} getrennt (Server-Aufruf ok)", tournamentId.get());
        } catch (PtmOnlineHttpException e) {
            if (!e.istBindungAbgeloest()) {
                getLogger().error("PTM-Online: Verbindung trennen (Server-Aufruf) fehlgeschlagen", e);
                throw new GenerateException(PtmOnlineFehlerText.fuer(e));
            }
            getLogger().info("PTM-Online: Turnier {} online nicht mehr an dieses Dokument gebunden, trenne nur lokal",
                    tournamentId.get(), e);
        } catch (IOException e) {
            getLogger().error("PTM-Online: Verbindung trennen (Server-Aufruf) fehlgeschlagen", e);
            throw new GenerateException(PtmOnlineFehlerText.fuer(e));
        } catch (InterruptedException e) {
            // Interrupt-Flag bleibt verbraucht, damit die UNO-Aufräumaufrufe nicht auf einem unterbrochenen Thread laufen.
            getLogger().debug("PTM-Online: Trennen während des Serveraufrufs abgebrochen", e);
            throw verarbeitungAbgebrochen();
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
