/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import de.petanqueturniermanager.onlinesync.sheet.PtmOnlineKonfliktSheet;
import de.petanqueturniermanager.ptmonline.auftrag.AuftragsBestand;
import de.petanqueturniermanager.ptmonline.dto.ErgebnismeldungenDto;

/**
 * „Gemeldete Ergebnisse abholen“ (Toolbar, Menü PTM-Online): holt die offenen Ergebnismeldungen von Spielern sofort,
 * statt auf den nächsten Abruf des Live-Beobachters zu warten (E-27). Eingetragene Entscheidungen der Konfliktliste
 * werden dabei angewendet; die Entscheidungen gehen anschließend mit dem Live-Durchlauf an PTM-Online.
 */
public final class PtmOnlineErgebnismeldungenRunner extends SheetRunner implements PtmOnlineLiveAusloeser {

    private final @Nullable Integer spieltagNr;
    private final LibreOfficePtmOnlineSpeicher.Zugangsdaten config;
    private final String tournamentId;

    public PtmOnlineErgebnismeldungenRunner(WorkingSpreadsheet ws, TurnierSystem turnierSystem,
            @Nullable Integer spieltagNr, LibreOfficePtmOnlineSpeicher.Zugangsdaten config, String tournamentId) {
        super(ws, turnierSystem, "PTM-Online");
        this.spieltagNr = spieltagNr;
        this.config = config;
        this.tournamentId = tournamentId;
    }

    @Override
    protected IKonfigurationSheet getKonfigurationSheet() {
        return null;
    }

    @Override
    protected void doRun() throws GenerateException {
        processBox().info(I18n.get("ptmonline.fortschritt.ergebnismeldungen_abholen"));
        Optional<PtmOnlineVerbindung> verbindung = PtmOnlineVerbindung.ermitteln(getWorkingSpreadsheet(),
                getTurnierSystem());
        if (verbindung.isEmpty()) {
            throw new GenerateException(I18n.get("ptmonline.fehler.turnier_nicht_verbunden"));
        }
        ErgebnismeldungenDto abruf = hole();
        PtmOnlineRegistrationMapping mapping = verbindung.get().mapping();
        PtmOnlineKonfliktSheet konfliktListe = mapping.konfliktListe();
        AuftragsBestand bestand = PtmOnlineAuftraege.bestand(getWorkingSpreadsheet(), mapping, spieltagNr);
        Map<String, String> eingetragen = konfliktListe.leseEntscheidungen();
        Runnable imScope = () -> {
            // Der Runner läuft bereits im Blattschutz-Scope des SheetRunners.
        };
        List<KonfliktFall> offen = PtmOnlineErgebnismeldungen.verarbeite(getWorkingSpreadsheet(), getTurnierSystem(),
                verbindung.get(), bestand, abruf.reports(),
                fall -> Entscheidung.aus(eingetragen.get(fall.schluessel()), fall.optionen()), imScope);
        PtmOnlineErgebnismeldungen.aktualisiereKonfliktliste(konfliktListe, offen, imScope);
        PtmOnlineAuftraege.speichern(bestand, mapping);
        PtmOnlineLiveBeobachter.ergebnismeldungenAbgeholt(getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
                tournamentId, abruf);
        meldeErgebnis(abruf, offen.size(), konfliktListe);
    }

    private ErgebnismeldungenDto hole() throws GenerateException {
        try {
            return new TournamentSyncClient(config.baseUrl(), config.apiKey()).fetchErgebnismeldungen(tournamentId);
        } catch (InterruptedException e) {
            getLogger().debug("PTM-Online: Abholen der Ergebnismeldungen abgebrochen", e);
            throw verarbeitungAbgebrochen();
        } catch (IOException e) {
            getLogger().error("PTM-Online: Ergebnismeldungen nicht abrufbar", e);
            throw new GenerateException(PtmOnlineFehlerText.fuer(e));
        }
    }

    private void meldeErgebnis(ErgebnismeldungenDto abruf, int offen, PtmOnlineKonfliktSheet konfliktListe)
            throws GenerateException {
        if (!abruf.enabled()) {
            MessageBox.from(getxContext(), MessageBoxTypeEnum.INFO_OK).caption(I18n.get("ptmonline.menu.toplevel"))
                    .message(I18n.get("ptmonline.ergebnismeldungen.abgeschaltet")).show();
            return;
        }
        if (offen == 0) {
            processBox().info(I18n.get("ptmonline.ergebnismeldungen.keine"));
            return;
        }
        processBox().info(I18n.get("ptmonline.ergebnismeldungen.offen", offen));
        getSheetHelper().setActiveSheet(konfliktListe.getXSpreadSheet());
    }
}
