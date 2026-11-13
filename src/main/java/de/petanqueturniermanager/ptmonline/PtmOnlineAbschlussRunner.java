/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import org.jspecify.annotations.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxResult;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.sheetsync.NachziehModus;
import de.petanqueturniermanager.helper.sheetsync.SheetSyncNachzieher;
import de.petanqueturniermanager.ptmonline.auftrag.AuftragsArt;
import de.petanqueturniermanager.ptmonline.auftrag.AuftragsBestand;
import de.petanqueturniermanager.ptmonline.auftrag.versand.SyncAntwort;
import de.petanqueturniermanager.ptmonline.auftrag.versand.VersandErgebnis;
import de.petanqueturniermanager.ptmonline.dto.SyncStandDto;
import de.petanqueturniermanager.ptmonline.live.LiveStandQuelle;
import de.petanqueturniermanager.ptmonline.live.LiveStandQuellen;

/**
 * „Turnier online abschließen“: Nach Rückfrage überträgt das Dokument Spielrunden, Rangliste und Spieleinstellungen
 * vollständig und schließt das Online-Turnier ab – auf Wunsch mit Veröffentlichung der Endrangliste und
 * Benachrichtigung der Teilnehmer. Der Abschluss ist ein gezählter Auftrag und geht synchron hinaus; ohne Netz bleibt
 * er gespeichert und wird mit dem nächsten Versand nachgeholt.
 */
public final class PtmOnlineAbschlussRunner extends SheetRunner implements PtmOnlineLiveAusloeser {

    private static final String CODE_KEINE_RANGLISTE = "ranking_missing";
    private static final String CODE_FREIGABELINK = "share_link_missing";

    private final @Nullable Integer spieltagNr;

    /** Was die Turnierleitung in den Rückfragen gewählt hat. */
    private record Wahl(boolean ranglisteVeroeffentlichen, boolean teilnehmerBenachrichtigen) {}

    public PtmOnlineAbschlussRunner(WorkingSpreadsheet ws, TurnierSystem turnierSystem, @Nullable Integer spieltagNr) {
        super(ws, turnierSystem, "PTM-Online");
        this.spieltagNr = spieltagNr;
    }

    @Override
    protected IKonfigurationSheet getKonfigurationSheet() {
        return null;
    }

    @Override
    protected void doRun() throws GenerateException {
        PtmOnlineVerbindung verbindung = PtmOnlineVerbindung.ermitteln(getWorkingSpreadsheet(), getTurnierSystem())
                .orElseThrow(() -> new GenerateException(I18n.get("ptmonline.fehler.turnier_nicht_verbunden")));
        Optional<Wahl> wahl = frageNach();
        if (wahl.isEmpty()) {
            getLogger().info("PTM-Online: Abschluss von Turnier {} abgebrochen", verbindung.tournamentId());
            return;
        }
        processBox().info(I18n.get("ptmonline.fortschritt.abschluss"));
        TournamentSyncClient client = verbindung.gebundenerClient();
        AuftragsBestand bestand = PtmOnlineAuftraege.bestand(getWorkingSpreadsheet(), verbindung.mapping(), spieltagNr);
        try {
            erfasseVollstaendigenStand(verbindung, bestand, client);
            PtmOnlineAuftraege.abschluss(bestand, verbindung.tournamentId(), wahl.get().ranglisteVeroeffentlichen(),
                    wahl.get().teilnehmerBenachrichtigen());
            PtmOnlineAuftraege.SynchronerVersand versand = PtmOnlineAuftraege.sendeSynchron(bestand,
                    verbindung.mapping(), client, false);
            pruefeVersand(versand);
            zeigeErgebnis(versand.anwendung());
        } catch (InterruptedException e) {
            getLogger().debug("PTM-Online: Abschluss abgebrochen", e);
            throw verarbeitungAbgebrochen();
        }
    }

    /** Drei Rückfragen: abschließen, Endrangliste veröffentlichen, Teilnehmer benachrichtigen. */
    private Optional<Wahl> frageNach() {
        if (!ja(MessageBoxTypeEnum.WARN_YES_NO, I18n.get("ptmonline.frage.abschluss"))) {
            return Optional.empty();
        }
        boolean veroeffentlichen = ja(MessageBoxTypeEnum.QUESTION_YES_NO,
                I18n.get("ptmonline.frage.abschluss.rangliste"));
        boolean benachrichtigen = veroeffentlichen
                && ja(MessageBoxTypeEnum.QUESTION_YES_NO, I18n.get("ptmonline.frage.abschluss.mail"));
        return Optional.of(new Wahl(veroeffentlichen, benachrichtigen));
    }

    private boolean ja(MessageBoxTypeEnum typ, String text) {
        return MessageBox.from(getxContext(), typ).caption(I18n.get("ptmonline.frage.abschluss.titel")).message(text)
                .show() == MessageBoxResult.YES;
    }

    /**
     * Abgeleitete Blätter (Rangliste) nachziehen und Runden, Rangliste und Spieleinstellungen komplett neu erfassen,
     * damit online der Endstand des Dokuments steht. Der Online-Zustand nennt die online vorhandenen Runden, damit
     * dort überzählige gelöscht werden; ohne Netz geht es ohne ihn weiter (der Versand scheitert dann ohnehin).
     */
    private void erfasseVollstaendigenStand(PtmOnlineVerbindung verbindung, AuftragsBestand bestand,
            TournamentSyncClient client) throws GenerateException, InterruptedException {
        SheetSyncNachzieher.get().zieheNach(getWorkingSpreadsheet(), NachziehModus.NUR_VORHANDENE, () -> {
            // Der Runner läuft bereits im Blattschutz-Scope des SheetRunners.
        });
        String tournamentId = verbindung.tournamentId();
        PtmOnlineLiveSync.vergessen(tournamentId);
        PtmOnlineSpieleinstellungen.vergessen(tournamentId);
        PtmOnlineSpieleinstellungen.erfasse(bestand, tournamentId,
                PtmOnlineSpieleinstellungen.lese(getWorkingSpreadsheet(), getTurnierSystem()));
        Optional<LiveStandQuelle> quelle = LiveStandQuellen.fuer(getWorkingSpreadsheet(), getTurnierSystem(),
                spieltagNr);
        if (quelle.isPresent()) {
            PtmOnlineLiveSync.erfasse(bestand, tournamentId, PtmOnlineLiveSync.lese(verbindung, quelle.get()),
                    rundenOnline(client, tournamentId));
        }
    }

    private OptionalInt rundenOnline(TournamentSyncClient client, String tournamentId) throws InterruptedException {
        try {
            SyncStandDto stand = client.fetchSyncStand(tournamentId);
            return OptionalInt.of(stand.roundsOnline());
        } catch (IOException e) {
            getLogger().info("PTM-Online: Online-Zustand vor dem Abschluss nicht abrufbar", e);
            return OptionalInt.empty();
        }
    }

    /** Wie beim manuellen Abgleich: Netz- und Serverfehler lassen die Aufträge für den nächsten Versand liegen. */
    private void pruefeVersand(PtmOnlineAuftraege.SynchronerVersand versand) throws GenerateException {
        switch (versand.stopp()) {
            case FERTIG -> pruefeAblehnung(versand.anwendung().abgelehnt(AuftragsArt.ABSCHLUSS));
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
        String grund = switch (antwort.code().orElse("")) {
            case CODE_KEINE_RANGLISTE -> I18n.get("ptmonline.fehler.abschluss.keine_rangliste");
            case CODE_FREIGABELINK -> I18n.get("ptmonline.fehler.abschluss.freigabelink");
            default -> antwort.fehlertext().orElse("HTTP " + antwort.status());
        };
        throw new GenerateException(I18n.get("ptmonline.fehler.abschluss", grund));
    }

    private void zeigeErgebnis(PtmOnlineAuftraege.Anwendung anwendung) {
        JsonObject antwort = anwendung.abschluss().flatMap(SyncAntwort::json).orElseGet(JsonObject::new);
        List<String> absaetze = new ArrayList<>();
        absaetze.add(I18n.get("ptmonline.erfolg.abschluss"));
        if (wahr(antwort, "rankingPublished")) {
            absaetze.add(I18n.get("ptmonline.erfolg.abschluss.rangliste"));
        }
        if (wahr(antwort, "mailed")) {
            absaetze.add(I18n.get("ptmonline.erfolg.abschluss.mail"));
        } else if (wahr(antwort, "alreadyMailed")) {
            absaetze.add(I18n.get("ptmonline.erfolg.abschluss.bereits_benachrichtigt"));
        }
        absaetze.forEach(absatz -> processBox().info(absatz));
        MessageBox.from(getxContext(), MessageBoxTypeEnum.INFO_OK).caption(I18n.get("ptmonline.frage.abschluss.titel"))
                .message(String.join("\n\n", absaetze)).show();
    }

    private static boolean wahr(JsonObject json, String feld) {
        JsonElement wert = json.get(feld);
        return wert != null && wert.isJsonPrimitive() && wert.getAsBoolean();
    }
}
