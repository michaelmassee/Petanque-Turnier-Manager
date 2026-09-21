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
import de.petanqueturniermanager.onlinesync.sheet.OnlineTurnierInfoSheet;
import de.petanqueturniermanager.onlinesync.sheet.OnlineTurnierMeldungenSheet;

/**
 * Bindeglied zu den beiden sichtbaren Sheets einer Online-Turnier-Verbindung
 * ({@link OnlineTurnierInfoSheet} "Turnierinformationen", {@link OnlineTurnierMeldungenSheet}
 * "Meldungen"). Bei Supermelee (mehrere Spieltage) gilt {@code spieltagNrOderNull} — jeder
 * Spieltag hat seine eigene Verbindung/eigenes Sheet-Paar; für alle anderen Turniersysteme ist er
 * {@code null} (eine Verbindung pro Dokument).
 */
public class PtmOnlineRegistrationMapping {

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

    /** Entfernt beide Sheets wieder aus dem Dokument (Gegenstück zu {@link #verbinden}). */
    public void trennen() throws GenerateException {
        infoSheet.entfernen();
        meldungenSheet.entfernen();
    }

    public void addMapping(String lokaleUuid, String onlineRegistrationId, String nummerFormel) throws GenerateException {
        meldungenSheet.addMapping(lokaleUuid, onlineRegistrationId, nummerFormel);
    }

    public Optional<String> getOnlineId(String lokaleUuid) throws GenerateException {
        return meldungenSheet.getOnlineId(lokaleUuid);
    }

    /** Ob {@code onlineRegistrationId} bereits einer lokalen Team-Nr zugeordnet ist (bereits importiert). */
    public boolean istBereitsImportiert(String onlineRegistrationId) throws GenerateException {
        return meldungenSheet.istBereitsImportiert(onlineRegistrationId);
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
}
