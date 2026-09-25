/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.onlinesync.OnlineTournamentDto;
import de.petanqueturniermanager.onlinesync.sheet.NeueZuordnung;
import de.petanqueturniermanager.onlinesync.sheet.PtmOnlineSyncSheet;
import de.petanqueturniermanager.onlinesync.sheet.ZuordnungsAnzeige;
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
     * Legt das Blatt an (falls nötig) und schreibt die Verbindungsdaten - einmalig beim Verbinden, synchron
     * innerhalb eines laufenden SheetRunners. Wechselt das Online-Turnier, wird die bisherige Zuordnung geleert.
     */
    public void verbinden(OnlineTournamentDto turnier, SyncBindingDto binding, String leaseToken)
            throws GenerateException {
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

    /** Alle bereits zugeordneten Online-IDs – einmal lesen statt {@link #istBereitsImportiert} je Anmeldung. */
    public Set<String> getImportierteOnlineIds() throws GenerateException {
        return syncSheet.getOnlineIds();
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

    /** Online-ID je lokaler UUID aller Zuordnungen – einmal lesen statt {@link #getOnlineId} je Meldung. */
    public Map<String, String> getOnlineIdsProUuid() throws GenerateException {
        return syncSheet.getOnlineIdsProUuid();
    }

    public Map<String, Integer> getExecutionRevisionenProUuid() throws GenerateException {
        return syncSheet.getExecutionRevisionenProUuid();
    }

    /** Setzt die Ausführungsrevisionen mehrerer Zuordnungen in einem Schreibzugriff. */
    public void setExecutionRevisionen(Map<String, Integer> revisionProUuid) throws GenerateException {
        syncSheet.setExecutionRevisionen(revisionProUuid);
    }

    public void setBezeichnung(String lokaleUuid, String lokaleBezeichnung, String onlineStatus)
            throws GenerateException {
        syncSheet.setBezeichnung(lokaleUuid, lokaleBezeichnung, onlineStatus);
    }

    /** Schreibt Tarife und Anmeldefragen lesbar sowie den unübersetzten Anmeldestatus (Storno-Erkennung). */
    public void setOnlineDetails(String lokaleUuid, RegistrationDto registration) throws GenerateException {
        ZuordnungsAnzeige.OnlineDetails details = onlineDetails(registration);
        syncSheet.setOnlineDetails(lokaleUuid, details.tarife(), details.fragen(), details.rohStatus());
    }

    /**
     * Neue Zuordnung zu einer Online-Anmeldung samt lesbarer Details, zum gesammelten Schreiben per
     * {@link #addMappings}.
     */
    public static NeueZuordnung neueZuordnung(String lokaleUuid, String nummerFormel, String lokaleBezeichnung,
            RegistrationDto registration) {
        ZuordnungsAnzeige.OnlineDetails details = onlineDetails(registration);
        return new NeueZuordnung(lokaleUuid, registration.id(), nummerFormel,
                registration.executionRevision() == null ? 1 : registration.executionRevision(), lokaleBezeichnung,
                OnlineAnmeldeStatus.anzeige(registration.status()), details.tarife(), details.fragen(),
                details.rohStatus());
    }

    /** Schreibt mehrere neue Zuordnungen in einem Zugriff (siehe {@link PtmOnlineSyncSheet#addMappings}). */
    public void addMappings(List<NeueZuordnung> zuordnungen) throws GenerateException {
        syncSheet.addMappings(zuordnungen);
    }

    /**
     * Aktualisiert Bezeichnung, Status und – sofern die Online-Anmeldung bekannt ist – die Details mehrerer
     * Zuordnungen in einem Zugriff.
     *
     * @param bezeichnungProUuid   lokale Bezeichnung je lokaler UUID
     * @param registrationProUuid  aktuelle Online-Anmeldung je lokaler UUID; fehlt sie, wird der Status geleert
     */
    public void aktualisiereAnzeigen(Map<String, String> bezeichnungProUuid,
            Map<String, RegistrationDto> registrationProUuid) throws GenerateException {
        Map<String, ZuordnungsAnzeige> anzeigen = new LinkedHashMap<>();
        bezeichnungProUuid.forEach((uuid, bezeichnung) -> {
            RegistrationDto registration = registrationProUuid.get(uuid);
            anzeigen.put(uuid, registration == null
                    ? new ZuordnungsAnzeige(bezeichnung, "", Optional.empty())
                    : new ZuordnungsAnzeige(bezeichnung, OnlineAnmeldeStatus.anzeige(registration.status()),
                            Optional.of(onlineDetails(registration))));
        });
        syncSheet.setAnzeigen(anzeigen);
    }

    private static ZuordnungsAnzeige.OnlineDetails onlineDetails(RegistrationDto registration) {
        String tarife = registration.feeSelections() == null ? "" : registration.feeSelections().stream()
                .map(PtmOnlineRegistrationMapping::tarifText).filter(text -> !text.isBlank())
                .collect(Collectors.joining(" | "));
        String fragen = registration.registrationAnswers() == null ? "" : registration.registrationAnswers().stream()
                .map(PtmOnlineRegistrationMapping::frageText).filter(text -> !text.isBlank())
                .collect(Collectors.joining(" | "));
        return new ZuordnungsAnzeige.OnlineDetails(tarife, fragen, registration.status());
    }

    private static String tarifText(RegistrationFeeDto tarif) {
        if (tarif == null) return "";
        String betrag = tarif.amountCents() == null ? "" : String.format(Locale.ROOT, "%.2f EUR", tarif.amountCents() / 100.0);
        return (String.valueOf(tarif.name() == null ? "" : tarif.name()).strip() + (betrag.isBlank() ? "" : ": " + betrag)).strip();
    }

    private static String frageText(RegistrationAnswerDto antwort) {
        if (antwort == null) return "";
        String frage = antwort.questionLabel() == null ? antwort.questionId() : antwort.questionLabel();
        String teilnehmer = switch (antwort.participant() == null ? "" : antwort.participant()) {
            case "primary" -> I18n.get("ptmonline.teilnehmer.spieler1");
            case "partner" -> I18n.get("ptmonline.teilnehmer.partner");
            case "partner2" -> I18n.get("ptmonline.teilnehmer.partner2");
            default -> "";
        };
        return (String.valueOf(frage == null ? "" : frage).strip()
                + (teilnehmer.isBlank() ? "" : " (" + teilnehmer + ")")).strip();
    }

    public void aktualisiereAnzeigeFormeln(Map<String, String> formelnProUuid) throws GenerateException {
        syncSheet.aktualisiereAnzeigeFormeln(formelnProUuid);
    }

    /** Pausiert: kein Meldungsabgleich und kein Rundenstart-Sync; die Verbindung selbst bleibt bestehen. */
    public boolean istPausiert() throws GenerateException {
        return syncSheet.istPausiert();
    }

    public void setPausiert(boolean pausiert) throws GenerateException {
        syncSheet.setPausiert(pausiert);
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
