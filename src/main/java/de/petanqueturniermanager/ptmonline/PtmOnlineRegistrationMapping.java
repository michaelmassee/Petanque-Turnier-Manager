/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.lang.reflect.Type;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import de.petanqueturniermanager.helper.DocumentPropertiesHelper;

/**
 * Ordnet lokale Team-Nummern der Meldeliste den zugehoerigen PTM-Online-Registrierungs-IDs zu, und
 * merkt sich Turnier-ID sowie Zeitpunkt des letzten Abgleichs. Wird bewusst als kleine JSON-Struktur
 * in Custom Document Properties gehalten (kein neue Spalte in den 8 Meldeliste-Layouts).
 */
public class PtmOnlineRegistrationMapping {

    private static final String PROP_REGISTRATION_MAP = "ptmOnlineRegistrationMap";
    private static final String PROP_LAST_SYNC = "ptmOnlineLastSync";
    private static final String PROP_TOURNAMENT_ID = "ptmOnlineTournamentId";

    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<Integer, String>>() {}.getType();

    private final DocumentPropertiesHelper docProps;

    public PtmOnlineRegistrationMapping(DocumentPropertiesHelper docProps) {
        this.docProps = docProps;
    }

    public void addMapping(int teamNr, String onlineRegistrationId) {
        Map<Integer, String> mappings = loadMappings();
        mappings.put(teamNr, onlineRegistrationId);
        docProps.setStringProperty(PROP_REGISTRATION_MAP, GSON.toJson(mappings, MAP_TYPE));
    }

    public Optional<String> getOnlineId(int teamNr) {
        return Optional.ofNullable(loadMappings().get(teamNr));
    }

    /** Ob {@code onlineRegistrationId} bereits einer lokalen Team-Nr zugeordnet ist (bereits importiert). */
    public boolean istBereitsImportiert(String onlineRegistrationId) {
        return loadMappings().containsValue(onlineRegistrationId);
    }

    /** Unveraenderliche Kopie aller aktuell gespeicherten Team-Nr/Online-ID-Zuordnungen. */
    public Map<Integer, String> getAlleMappings() {
        return Map.copyOf(loadMappings());
    }

    public void setLastSync(Instant zeitpunkt) {
        docProps.setStringProperty(PROP_LAST_SYNC, zeitpunkt.toString());
    }

    public Optional<Instant> getLastSync() {
        String wert = docProps.getStringProperty(PROP_LAST_SYNC, "");
        return wert.isBlank() ? Optional.empty() : Optional.of(Instant.parse(wert));
    }

    public void setTournamentId(String tournamentId) {
        docProps.setStringProperty(PROP_TOURNAMENT_ID, tournamentId);
    }

    public Optional<String> getTournamentId() {
        String wert = docProps.getStringProperty(PROP_TOURNAMENT_ID, "");
        return wert.isBlank() ? Optional.empty() : Optional.of(wert);
    }

    private Map<Integer, String> loadMappings() {
        String json = docProps.getStringProperty(PROP_REGISTRATION_MAP, "");
        if (json.isBlank()) {
            return new LinkedHashMap<>();
        }
        Map<Integer, String> geparst = GSON.fromJson(json, MAP_TYPE);
        return geparst == null ? new LinkedHashMap<>() : new LinkedHashMap<>(geparst);
    }
}
