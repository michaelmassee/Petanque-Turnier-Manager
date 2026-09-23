/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.time.Instant;
import java.util.Optional;

import com.google.gson.Gson;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.onlinesync.OnlineTournamentDto;
import de.petanqueturniermanager.onlinesync.sheet.OnlineTurnierInfoSheet;
import de.petanqueturniermanager.onlinesync.sheet.OnlineTurnierMeldungenSheet;
import de.petanqueturniermanager.ptmonline.dto.SyncBindingDto;
import de.petanqueturniermanager.ptmonline.dto.RegistrationAnswerDto;
import de.petanqueturniermanager.ptmonline.dto.RegistrationDto;
import de.petanqueturniermanager.ptmonline.dto.RegistrationFeeDto;

/**
 * Bindeglied zu den beiden sichtbaren Sheets einer Online-Turnier-Verbindung
 * ({@link OnlineTurnierInfoSheet} "Turnierinformationen", {@link OnlineTurnierMeldungenSheet}
 * "Meldungen"). Bei Supermelee (mehrere Spieltage) gilt {@code spieltagNrOderNull} — jeder
 * Spieltag hat seine eigene Verbindung/eigenes Sheet-Paar; für alle anderen Turniersysteme ist er
 * {@code null} (eine Verbindung pro Dokument).
 */
public class PtmOnlineRegistrationMapping {

    private static final Gson GSON = new Gson();

    private final OnlineTurnierInfoSheet infoSheet;
    private final OnlineTurnierMeldungenSheet meldungenSheet;

    public PtmOnlineRegistrationMapping(WorkingSpreadsheet ws, TurnierSystem turnierSystem, Integer spieltagNrOderNull) {
        this.infoSheet = new OnlineTurnierInfoSheet(ws, turnierSystem, spieltagNrOderNull);
        this.meldungenSheet = new OnlineTurnierMeldungenSheet(ws, turnierSystem, spieltagNrOderNull);
    }

    /** Legt beide Sheets an (falls nötig) und schreibt die Verbindungsdaten - einmalig beim Verbinden. */
    public void verbinden(OnlineTournamentDto turnier) throws GenerateException, InterruptedException {
        Optional<String> bisherigeTurnierId = infoSheet.getTournamentIdWennVorhanden();
        infoSheet.verbinden(turnier);
        meldungenSheet.sicherstellen();
        if (bisherigeTurnierId.isPresent() && !bisherigeTurnierId.get().equals(turnier.id)) {
            meldungenSheet.leeren();
            infoSheet.setLastSync(null);
        }
    }

    public void setSyncBinding(SyncBindingDto binding, String leaseToken) throws GenerateException {
        infoSheet.setSyncBinding(binding.syncDocumentId(), leaseToken, binding.bindingRevision());
    }

    /** Aktualisiert die sichtbaren Überschriften einer bestehenden Zuordnungstabelle. */
    public void sicherstellen() throws GenerateException, InterruptedException {
        meldungenSheet.sicherstellen();
    }

    /** Entfernt beide Sheets wieder aus dem Dokument (Gegenstück zu {@link #verbinden}). */
    public void trennen() throws GenerateException {
        infoSheet.entfernen();
        meldungenSheet.entfernen();
    }

    public void addMapping(String lokaleUuid, String onlineRegistrationId, String nummerFormel, int executionRevision,
            String lokaleBezeichnung, String onlineBezeichnung, String onlineStatus) throws GenerateException {
        meldungenSheet.addMapping(lokaleUuid, onlineRegistrationId, nummerFormel, executionRevision,
                lokaleBezeichnung, onlineBezeichnung, onlineStatus);
    }

    public Optional<String> getOnlineId(String lokaleUuid) throws GenerateException {
        return meldungenSheet.getOnlineId(lokaleUuid);
    }

    /** Ob {@code onlineRegistrationId} bereits einer lokalen Team-Nr zugeordnet ist (bereits importiert). */
    public boolean istBereitsImportiert(String onlineRegistrationId) throws GenerateException {
        return meldungenSheet.istBereitsImportiert(onlineRegistrationId);
    }

    public Optional<String> getLokaleUuid(String onlineRegistrationId) throws GenerateException {
        return meldungenSheet.getLokaleUuid(onlineRegistrationId);
    }

    /** Ob die zuletzt übernommene Online-Anmeldung dieser lokalen Meldung online storniert ist. */
    public boolean istOnlineStorniert(String lokaleUuid) throws GenerateException {
        return meldungenSheet.getOnlineSnapshot(lokaleUuid)
                .map(json -> GSON.fromJson(json, RegistrationDto.class))
                .map(registration -> OnlineAnmeldeStatus.istStorniert(registration.status()))
                .orElse(false);
    }

    /** Hängt eine bereits zugeordnete lokale Meldung auf eine andere Online-Anmeldung um. */
    public void ersetzeOnlineId(String lokaleUuid, String onlineRegistrationId, int executionRevision)
            throws GenerateException {
        meldungenSheet.ersetzeOnlineId(lokaleUuid, onlineRegistrationId, executionRevision);
    }

    public int getExecutionRevision(String lokaleUuid) throws GenerateException {
        return meldungenSheet.getExecutionRevision(lokaleUuid);
    }

    public void setExecutionRevision(String lokaleUuid, int executionRevision) throws GenerateException {
        meldungenSheet.setExecutionRevision(lokaleUuid, executionRevision);
    }

    public void setBezeichnungen(String lokaleUuid, String lokaleBezeichnung, String onlineBezeichnung,
            String onlineStatus) throws GenerateException {
        meldungenSheet.setBezeichnungen(lokaleUuid, lokaleBezeichnung, onlineBezeichnung, onlineStatus);
    }

    /** Schreibt die vollständigen Online-Anmeldedetails als lesbare Felder plus unveränderten JSON-Snapshot. */
    public void setOnlineDetails(String lokaleUuid, RegistrationDto registration) throws GenerateException {
        String tarife = registration.feeSelections() == null ? "" : registration.feeSelections().stream()
                .map(PtmOnlineRegistrationMapping::tarifText).filter(text -> !text.isBlank())
                .collect(java.util.stream.Collectors.joining(" | "));
        String fragen = registration.registrationAnswers() == null ? "" : registration.registrationAnswers().stream()
                .map(PtmOnlineRegistrationMapping::frageText).filter(text -> !text.isBlank())
                .collect(java.util.stream.Collectors.joining(" | "));
        meldungenSheet.setOnlineDetails(lokaleUuid, tarife, fragen, GSON.toJson(registration));
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

    public void migriereLegacyTeamnummern(java.util.Map<Integer, String> uuidProTeamnummer) throws GenerateException {
        meldungenSheet.migriereLegacyTeamnummern(uuidProTeamnummer);
    }

    public void aktualisiereAnzeigeFormeln(java.util.Map<String, String> formelnProUuid) throws GenerateException {
        meldungenSheet.aktualisiereAnzeigeFormeln(formelnProUuid);
    }


    public void setLastSync(Instant zeitpunkt) throws GenerateException {
        infoSheet.setLastSync(zeitpunkt);
    }

    public Optional<Instant> getLastSync() throws GenerateException {
        return infoSheet.getLastSync();
    }

    public Optional<String> getTournamentId() throws GenerateException {
        return infoSheet.getTournamentId();
    }

    public Optional<String> getSyncDocumentId() throws GenerateException {
        return infoSheet.getSyncDocumentId();
    }

    public Optional<String> getLeaseToken() throws GenerateException {
        return infoSheet.getLeaseToken();
    }
}
