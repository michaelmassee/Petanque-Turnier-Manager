/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import org.jspecify.annotations.Nullable;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.ptmonline.auftrag.AuftragsArt;
import de.petanqueturniermanager.ptmonline.auftrag.AuftragsBestand;
import de.petanqueturniermanager.ptmonline.dto.SyncStandDto;
import de.petanqueturniermanager.ptmonline.live.LiveStandQuelle;
import de.petanqueturniermanager.ptmonline.live.LiveStandQuellen;

/**
 * Dokument-Seite der Live-Übertragung (T-09): wendet die Ergebnisse gesendeter Aufträge im Dokument an und erfasst
 * neue Aufträge aus einem Snapshot – Check-in-Änderungen, Live-Stand, ausstehender Turnierstart. Als stiller
 * {@link SheetRunner}, damit Blattschutz-Scope und „nur ein Lauf gleichzeitig“ gelten; der Hintergrund-Thread des
 * {@link PtmOnlineLiveBeobachter} wartet darauf und sendet danach nur noch.
 */
final class PtmOnlineErfassungsRunner extends SheetRunner {

    /**
     * Was der Hintergrund zum Senden braucht, ohne das Dokument zu lesen.
     *
     * @param brauchtStand der Live-Stand wurde nicht erfasst, weil zuerst der Online-Zustand abzurufen ist
     * @param anwendung    angewendete Ergebnisse (für Hinweise an die Turnierleitung)
     */
    record Erfassung(PtmOnlineVerbindung verbindung, AuftragsBestand bestand, boolean pausiert, boolean brauchtStand,
            PtmOnlineAuftraege.Anwendung anwendung) {
    }

    /**
     * Abruf des Online-Zustands eines Turniers durch den Hintergrund.
     *
     * @param stand leer, wenn der Abruf gescheitert ist (z.&nbsp;B. ohne Netz)
     */
    record OnlineStandAbruf(String tournamentId, Optional<SyncStandDto> stand) {
    }

    private final PtmOnlineCheckin checkin;
    private final @Nullable OnlineStandAbruf abruf;
    private volatile @Nullable Erfassung erfassung;

    /** @param abruf letzter Abruf des Online-Zustands, {@code null} ohne */
    PtmOnlineErfassungsRunner(WorkingSpreadsheet ws, TurnierSystem turnierSystem, PtmOnlineCheckin checkin,
            @Nullable OnlineStandAbruf abruf) {
        super(ws, turnierSystem, "PTM-Online");
        this.checkin = checkin;
        this.abruf = abruf;
    }

    @Override
    protected IKonfigurationSheet getKonfigurationSheet() {
        return null;
    }

    /** Ergebnis des Laufs; leer, wenn das Dokument nicht verbunden ist. */
    Optional<Erfassung> erfassung() {
        return Optional.ofNullable(erfassung);
    }

    @Override
    protected void doRun() throws GenerateException {
        Optional<PtmOnlineVerbindung> verbindung;
        try {
            verbindung = PtmOnlineVerbindung.ermitteln(getWorkingSpreadsheet(), getTurnierSystem());
        } catch (IllegalStateException e) {
            // Zugangsdaten nicht lesbar, z. B. während LibreOffice beendet wird: im nächsten Durchlauf erneut.
            getLogger().warn("PTM-Online: Verbindung nicht ermittelbar, Erfassung übersprungen", e);
            return;
        }
        if (verbindung.isEmpty()) {
            return;
        }
        PtmOnlineVerbindung v = verbindung.get();
        OnlineStandAbruf passenderAbruf = abruf != null && abruf.tournamentId().equals(v.tournamentId()) ? abruf
                : null;
        PtmOnlineRegistrationMapping mapping = v.mapping();
        AuftragsBestand bestand = PtmOnlineAuftraege.bestand(getWorkingSpreadsheet(), mapping, v.spieltagNr());
        PtmOnlineAuftraege.Anwendung anwendung = PtmOnlineAuftraege.anwenden(bestand, mapping);
        boolean pausiert = !PtmOnlineSpielrundeSync.istSyncAktiv(mapping);
        boolean brauchtStand = false;
        if (!pausiert) {
            startNachholen(bestand, v);
            erfasseCheckin(bestand, v);
            brauchtStand = !erfasseLiveStand(bestand, v, passenderAbruf);
        }
        PtmOnlineAuftraege.speichern(bestand, mapping);
        erfassung = new Erfassung(v, bestand, pausiert, brauchtStand, anwendung);
    }

    /**
     * Holt einen ausstehenden Übergang zu {@code running} nach (KP-05): nach einer Pause, oder wenn PTM-Online das
     * Turnier nicht als laufend kennt, obwohl lokal schon Runden gespielt werden (z.&nbsp;B. nach neuer Bindung).
     */
    private void startNachholen(AuftragsBestand bestand, PtmOnlineVerbindung verbindung) throws GenerateException {
        if (bestand.hatOffenen(AuftragsArt.START)) {
            return;
        }
        Optional<Instant> ausstehend = verbindung.mapping().getRunningAusstehendSeit();
        if (ausstehend.isPresent()) {
            PtmOnlineAuftraege.start(bestand, verbindung.tournamentId(), ausstehend.get(),
                    I18n.get("ptmonline.auftrag.verworfen.turnierstart"));
        }
    }

    private void erfasseCheckin(AuftragsBestand bestand, PtmOnlineVerbindung verbindung) throws GenerateException {
        Optional<PtmOnlineCheckin.Aenderung> aenderung = checkin.ermittle(verbindung.tournamentId(),
                PtmOnlineCheckin.leseStand(verbindung));
        if (aenderung.isEmpty()) {
            return;
        }
        PtmOnlineAuftraege.teilnahme(bestand, verbindung.tournamentId(), aenderung.get().auftrag().eintraege(),
                verbindung.mapping().getOnlineIdsProUuid(), verbindung.mapping().getExecutionRevisionenProUuid());
        checkin.bestaetigen(aenderung.get());
        getLogger().info("PTM-Online: {} geänderte Check-ins als Auftrag erfasst",
                aenderung.get().auftrag().eintraege().size());
    }

    /**
     * Ist noch kein Live-Stand erfasst (nach Öffnen oder Verbinden), wird zuerst der Online-Zustand abgerufen: er
     * nennt die online vorhandenen Runden (überzählige werden gelöscht) und ob das Turnier online läuft.
     *
     * @param standAbruf Abruf des Online-Zustands dieses Turniers, {@code null} ohne
     * @return {@code false}, wenn zuerst der Online-Zustand abzurufen ist
     */
    private boolean erfasseLiveStand(AuftragsBestand bestand, PtmOnlineVerbindung verbindung,
            @Nullable OnlineStandAbruf standAbruf) throws GenerateException {
        Optional<LiveStandQuelle> quelle = LiveStandQuellen.fuer(getWorkingSpreadsheet(), getTurnierSystem(),
                verbindung.spieltagNr());
        if (quelle.isEmpty()) {
            return true;
        }
        boolean ersterStand = !PtmOnlineLiveSync.hatStand(verbindung.tournamentId());
        if (ersterStand && standAbruf == null) {
            return false;
        }
        Optional<SyncStandDto> onlineStand = standAbruf == null ? Optional.empty() : standAbruf.stand();
        PtmOnlineLiveSync.Momentaufnahme momentaufnahme = PtmOnlineLiveSync.lese(verbindung, quelle.get());
        if (ersterStand && !momentaufnahme.stand().runden().isEmpty()
                && onlineStand.filter(stand -> !stand.istRunning()).isPresent()
                && !bestand.hatOffenen(AuftragsArt.START)) {
            List<?> verworfen = PtmOnlineAuftraege.start(bestand, verbindung.tournamentId(), Instant.now(),
                    I18n.get("ptmonline.auftrag.verworfen.turnierstart"));
            getLogger().info("PTM-Online: Turnierstart nachgeholt (online nicht laufend), {} Aufträge verworfen",
                    verworfen.size());
        }
        OptionalInt rundenOnline = onlineStand.map(stand -> OptionalInt.of(stand.roundsOnline()))
                .orElse(OptionalInt.empty());
        PtmOnlineLiveSync.erfasse(bestand, verbindung.tournamentId(), momentaufnahme, rundenOnline);
        return true;
    }
}
