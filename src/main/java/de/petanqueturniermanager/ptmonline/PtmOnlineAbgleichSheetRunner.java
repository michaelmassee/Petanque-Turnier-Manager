/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.LibreOfficePtmOnlineSpeicher;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.ptmonline.RegistrationImportTask.AbgleichErgebnis;
import de.petanqueturniermanager.spielerdb.MeldelisteZiel;
import de.petanqueturniermanager.spielerdb.MeldelisteZielFactory;

/**
 * Manueller Abgleich der Meldungen mit PTM-Online als normaler {@link SheetRunner}: Statuszeilen in
 * der ProcessBox, Abbruch über den Stop-Knopf, eigener Blattschutz-Scope. Die Meldeliste wird nach
 * dem Import synchron im selben Runner aktualisiert, da parallel kein zweiter Runner starten kann.
 */
public final class PtmOnlineAbgleichSheetRunner extends SheetRunner {

    private static final int HTTP_NICHT_AUTORISIERT = 401;

    private final LibreOfficePtmOnlineSpeicher.Zugangsdaten config;
    private final PtmOnlineRegistrationMapping mapping;
    private final String tournamentId;
    private final MeldelisteZiel ziel;

    public PtmOnlineAbgleichSheetRunner(WorkingSpreadsheet ws, TurnierSystem turnierSystem,
            LibreOfficePtmOnlineSpeicher.Zugangsdaten config, PtmOnlineRegistrationMapping mapping,
            String tournamentId, MeldelisteZiel ziel) {
        super(ws, turnierSystem, "PTM-Online");
        this.config = config;
        this.mapping = mapping;
        this.tournamentId = tournamentId;
        this.ziel = ziel;
    }

    @Override
    protected IKonfigurationSheet getKonfigurationSheet() {
        return null;
    }

    @Override
    protected void doRun() throws GenerateException {
        processBox().info(I18n.get("ptmonline.fortschritt.abgleich_start"));
        if (ziel.istMeleeAnmeldung()) {
            // Mêlée-Anmeldung-Sheet anlegen, falls es fehlt – sonst hätten Online-Anmeldungen kein Ziel.
            zielAktualisieren();
        }
        AbgleichErgebnis ergebnis;
        try {
            ergebnis = RegistrationImportTask.fuehreAbgleichDurch(config, mapping, tournamentId, ziel,
                    this::zielAktualisieren, new ProcessBoxFortschritt());
        } catch (InterruptedException e) {
            // Stop-Knopf unterbricht den Thread, auch mitten in einem HTTP-Aufruf. Die Unterbrechung ist
            // damit vollständig als Abbruch behandelt; das Flag bleibt bewusst verbraucht, damit die
            // UNO-Aufräumaufrufe des SheetRunners nicht auf einem unterbrochenen Thread laufen.
            getLogger().debug("PTM-Online: Abgleich während eines Serveraufrufs abgebrochen", e);
            throw verarbeitungAbgebrochen();
        } catch (IOException e) {
            getLogger().error("PTM-Online: Abgleich der Meldungen fehlgeschlagen", e);
            throw new GenerateException(netzwerkFehlerText(e));
        }
        zeigeErgebnis(ergebnis);
    }

    private void zielAktualisieren() throws GenerateException {
        MeldelisteZielFactory.aktualisiereZielSynchron(getWorkingSpreadsheet(), getTurnierSystem(), ziel);
    }

    private void zeigeErgebnis(AbgleichErgebnis ergebnis) {
        String zusammenfassung = I18n.get("ptmonline.erfolg.anmeldungen_abgeglichen",
                ergebnis.importErgebnis().importiert(), ergebnis.onlineAngelegt());
        processBox().info(zusammenfassung);
        List<String> hinweise = ergebnis.hinweise();
        if (hinweise.isEmpty()) {
            return;
        }
        hinweise.forEach(hinweis -> processBox().fehler(hinweis));
        List<String> absaetze = new ArrayList<>();
        absaetze.add(zusammenfassung);
        absaetze.addAll(hinweise);
        MessageBox.from(getxContext(), MessageBoxTypeEnum.WARN_OK)
                .caption(I18n.get("ptmonline.menu.toplevel"))
                .message(String.join("\n\n", absaetze))
                .show();
    }

    private static String netzwerkFehlerText(IOException e) {
        if (e instanceof PtmOnlineHttpException http && http.getStatusCode() == HTTP_NICHT_AUTORISIERT) {
            return I18n.get("ptmonline.fehler.nicht_freigeschaltet");
        }
        String meldung = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        return I18n.get("ptmonline.fehler.netzwerk", meldung);
    }

    /** Statuszeilen in die ProcessBox, Abbruchpunkte über den SheetRunner-Stop-Knopf. */
    private final class ProcessBoxFortschritt implements AbgleichFortschritt {

        @Override
        public void status(String text) {
            processBox().info(text);
        }

        @Override
        public void pruefeAbbruch() throws GenerateException {
            if (Thread.interrupted()) {
                throw verarbeitungAbgebrochen();
            }
        }
    }
}
