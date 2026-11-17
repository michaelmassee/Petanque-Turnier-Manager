/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.util.Optional;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.ptmonline.auftrag.AuftragsArt;
import de.petanqueturniermanager.ptmonline.auftrag.AuftragsBestand;
import de.petanqueturniermanager.ptmonline.auftrag.versand.SyncAntwort;
import de.petanqueturniermanager.ptmonline.auftrag.versand.VersandErgebnis;

/**
 * Ankündigung der Turnierleitung an ein verbundenes PTM-Online-Turnier (Turnierseite, Live-Ansicht, Live-Push).
 * Die Ankündigung ist ein gezählter Auftrag und geht synchron hinaus; ohne Netz bleibt sie gespeichert und wird mit
 * dem nächsten Versand nachgeholt. Läuft im Hintergrund-Thread eines {@code SheetRunner}.
 */
public final class PtmOnlineAnkuendigung {

    private static final String CODE_TURNIER_ABGESCHLOSSEN = "tournament_finished";

    /** Ausgang des Versands. */
    public enum Ergebnis {
        /** Das Dokument ist nicht mit PTM-Online verbunden; nichts gesendet. */
        NICHT_VERBUNDEN,
        /** PTM-Online hat die Ankündigung übernommen. */
        GESENDET
    }

    private PtmOnlineAnkuendigung() {
    }

    /**
     * @param text bereinigter Ankündigungstext; leer löscht die Ankündigung online
     * @throws GenerateException mit lesbarem Grund, wenn PTM-Online die Ankündigung nicht übernommen hat
     */
    public static Ergebnis senden(WorkingSpreadsheet ws, TurnierSystem ts, String text)
            throws GenerateException, InterruptedException {
        if (!PtmOnlineVerbindung.hatVerbindung(ws, ts)) {
            return Ergebnis.NICHT_VERBUNDEN;
        }
        Optional<PtmOnlineVerbindung> verbindung = PtmOnlineVerbindung.ermitteln(ws, ts);
        if (verbindung.isEmpty()) {
            return Ergebnis.NICHT_VERBUNDEN;
        }
        TournamentSyncClient client = verbindung.get().gebundenerClient();
        AuftragsBestand bestand = PtmOnlineAuftraege.bestand(ws, verbindung.get().mapping(),
                verbindung.get().spieltagNr());
        PtmOnlineAuftraege.ankuendigung(bestand, verbindung.get().tournamentId(), text);
        PtmOnlineAuftraege.SynchronerVersand versand = PtmOnlineAuftraege.sendeSynchron(bestand,
                verbindung.get().mapping(), client, false);
        pruefeVersand(versand);
        return Ergebnis.GESENDET;
    }

    private static void pruefeVersand(PtmOnlineAuftraege.SynchronerVersand versand) throws GenerateException {
        switch (versand.stopp()) {
            case FERTIG -> pruefeAblehnung(versand.anwendung().abgelehnt(AuftragsArt.ANKUENDIGUNG));
            case DOKUMENT_GEFORKT -> throw new PtmOnlineDokumentGeforktException();
            case BINDUNG_ABGELOEST -> throw new GenerateException(I18n.get("ptmonline.fehler.bindung_abgeloest"));
            case TURNIER_GELOESCHT -> throw new GenerateException(I18n.get("ptmonline.turnier.geloescht"));
            case NETZ, SERVERFEHLER, NICHT_BERECHTIGT -> throw new GenerateException(
                    I18n.get("ptmonline.fehler.auftraege_nicht_gesendet", versand.stopp()));
        }
    }

    private static void pruefeAblehnung(Optional<VersandErgebnis> ablehnung) throws GenerateException {
        if (ablehnung.isEmpty()) {
            return;
        }
        SyncAntwort antwort = ablehnung.get().antwort();
        if (antwort.code().filter(CODE_TURNIER_ABGESCHLOSSEN::equals).isPresent()) {
            throw new GenerateException(I18n.get("ankuendigung.fehler.ptmonline.abgeschlossen"));
        }
        throw new GenerateException(antwort.fehlertext().orElse("HTTP " + antwort.status()));
    }
}
