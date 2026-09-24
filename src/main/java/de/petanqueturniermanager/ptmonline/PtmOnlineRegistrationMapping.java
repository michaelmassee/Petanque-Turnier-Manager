/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.time.Instant;
import java.util.Optional;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.onlinesync.OnlineTournamentDto;
import de.petanqueturniermanager.onlinesync.sheet.PtmOnlineSyncSheet;
import de.petanqueturniermanager.ptmonline.dto.SyncBindingDto;
import de.petanqueturniermanager.ptmonline.dto.RegistrationAnswerDto;
import de.petanqueturniermanager.ptmonline.dto.RegistrationDto;
import de.petanqueturniermanager.ptmonline.dto.RegistrationFeeDto;

/**
 * Bindeglied zum Blatt „PTMOnline Sync“ ({@link PtmOnlineSyncSheet}) einer Online-Turnier-Verbindung. Bei
 * Supermelee (mehrere Spieltage) gilt {@code spieltagNrOderNull} — jeder Spieltag hat seine eigene
 * Verbindung/sein eigenes Blatt; für alle anderen Turniersysteme ist er {@code null} (eine Verbindung pro
 * Dokument).
 */
public class PtmOnlineRegistrationMapping {

    private final PtmOnlineSyncSheet syncSheet;

    public PtmOnlineRegistrationMapping(WorkingSpreadsheet ws, TurnierSystem turnierSystem, Integer spieltagNrOderNull) {
        this.syncSheet = new PtmOnlineSyncSheet(ws, turnierSystem, spieltagNrOderNull);
    }

    /**
     * Legt das Blatt an (falls nötig) und schreibt die Verbindungsdaten - einmalig beim Verbinden. Wechselt das
     * Online-Turnier, wird die bisherige Zuordnung geleert.
     */
    public void verbinden(OnlineTournamentDto turnier, SyncBindingDto binding, String leaseToken)
            throws GenerateException, InterruptedException {
        syncSheet.verbinden(turnier, binding.syncDocumentId(), leaseToken);
    }

    /**
     * Legt das Blatt bei Bedarf an und aktualisiert Überschriften und Formatierung. Läuft synchron und ist für
     * Aufrufer innerhalb eines laufenden SheetRunners gedacht (Meldungsabgleich).
     */
    public void sicherstellen() throws GenerateException {
        syncSheet.anlegen();
    }

    /** Entfernt das Blatt wieder aus dem Dokument (Gegenstück zu {@link #verbinden}). */
    public void trennen() throws GenerateException {
        syncSheet.entfernen();
    }

    public void addMapping(String lokaleUuid, String onlineRegistrationId, String nummerFormel, int executionRevision,
            String lokaleBezeichnung, String onlineStatus) throws GenerateException {
        syncSheet.addMapping(lokaleUuid, onlineRegistrationId, nummerFormel, executionRevision, lokaleBezeichnung,
                onlineStatus);
    }

    public Optional<String> getOnlineId(String lokaleUuid) throws GenerateException {
        return syncSheet.getOnlineId(lokaleUuid);
    }

    /** Ob {@code onlineRegistrationId} bereits einer lokalen Meldung zugeordnet ist (bereits importiert). */
    public boolean istBereitsImportiert(String onlineRegistrationId) throws GenerateException {
        return syncSheet.istBereitsImportiert(onlineRegistrationId);
    }

    public Optional<String> getLokaleUuid(String onlineRegistrationId) throws GenerateException {
        return syncSheet.getLokaleUuid(onlineRegistrationId);
    }

    /** Ob die zuletzt übernommene Online-Anmeldung dieser lokalen Meldung online storniert ist. */
    public boolean istOnlineStorniert(String lokaleUuid) throws GenerateException {
        return syncSheet.getRohStatus(lokaleUuid).map(OnlineAnmeldeStatus::istStorniert).orElse(false);
    }

    /** Hängt eine bereits zugeordnete lokale Meldung auf eine andere Online-Anmeldung um. */
    public void ersetzeOnlineId(String lokaleUuid, String onlineRegistrationId, int executionRevision)
            throws GenerateException {
        syncSheet.ersetzeOnlineId(lokaleUuid, onlineRegistrationId, executionRevision);
    }

    public int getExecutionRevision(String lokaleUuid) throws GenerateException {
        return syncSheet.getExecutionRevision(lokaleUuid);
    }

    public void setExecutionRevision(String lokaleUuid, int executionRevision) throws GenerateException {
        syncSheet.setExecutionRevision(lokaleUuid, executionRevision);
    }

    public void setBezeichnung(String lokaleUuid, String lokaleBezeichnung, String onlineStatus)
            throws GenerateException {
        syncSheet.setBezeichnung(lokaleUuid, lokaleBezeichnung, onlineStatus);
    }

    /** Schreibt Tarife und Anmeldefragen lesbar sowie den unübersetzten Anmeldestatus (Storno-Erkennung). */
    public void setOnlineDetails(String lokaleUuid, RegistrationDto registration) throws GenerateException {
        String tarife = registration.feeSelections() == null ? "" : registration.feeSelections().stream()
                .map(PtmOnlineRegistrationMapping::tarifText).filter(text -> !text.isBlank())
                .collect(java.util.stream.Collectors.joining(" | "));
        String fragen = registration.registrationAnswers() == null ? "" : registration.registrationAnswers().stream()
                .map(PtmOnlineRegistrationMapping::frageText).filter(text -> !text.isBlank())
                .collect(java.util.stream.Collectors.joining(" | "));
        syncSheet.setOnlineDetails(lokaleUuid, tarife, fragen, registration.status());
    }

    private static String tarifText(RegistrationFeeDto tarif) {
        if (tarif == null) return "";
        String betrag = tarif.amountCents() == null ? "" : String.format(java.util.Locale.ROOT, "%.2f EUR", tarif.amountCents() / 100.0);
        return (String.valueOf(tarif.name() == null ? "" : tarif.name()).strip() + (betrag.isBlank() ? "" : ": " + betrag)).strip();
    }

    private static String frageText(RegistrationAnswerDto antwort) {
        if (antwort == null) return "";
        String frage = antwort.questionLabel() == null ? antwort.questionId() : antwort.questionLabel();
        String teilnehmer = switch (antwort.participant() == null ? "" : antwort.participant()) {
            case "primary" -> de.petanqueturniermanager.helper.i18n.I18n.get("ptmonline.teilnehmer.spieler1");
            case "partner" -> de.petanqueturniermanager.helper.i18n.I18n.get("ptmonline.teilnehmer.partner");
            case "partner2" -> de.petanqueturniermanager.helper.i18n.I18n.get("ptmonline.teilnehmer.partner2");
            default -> "";
        };
        return (String.valueOf(frage == null ? "" : frage).strip()
                + (teilnehmer.isBlank() ? "" : " (" + teilnehmer + ")")).strip();
    }

    public void aktualisiereAnzeigeFormeln(java.util.Map<String, String> formelnProUuid) throws GenerateException {
        syncSheet.aktualisiereAnzeigeFormeln(formelnProUuid);
    }

    public void setLastSync(Instant zeitpunkt) throws GenerateException {
        syncSheet.setLastSync(zeitpunkt);
    }

    public Optional<Instant> getLastSync() throws GenerateException {
        return syncSheet.getLastSync();
    }

    public Optional<String> getTournamentId() throws GenerateException {
        return syncSheet.getTournamentId();
    }

    public Optional<String> getSyncDocumentId() throws GenerateException {
        return syncSheet.getSyncDocumentId();
    }

    public Optional<String> getLeaseToken() throws GenerateException {
        return syncSheet.getLeaseToken();
    }
}
