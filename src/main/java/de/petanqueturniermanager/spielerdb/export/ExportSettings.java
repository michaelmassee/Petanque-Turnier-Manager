package de.petanqueturniermanager.spielerdb.export;

import java.util.prefs.Preferences;

import org.jspecify.annotations.Nullable;

/**
 * Persistiert den zuletzt gewählten Zielpfad pro {@link ExportFormat} in den
 * Java-User-Preferences (OS-spezifischer User-Bereich, keine separate
 * Konfig-Datei nötig). Ermöglicht im Dialog ein vorgefülltes Pfad-Feld beim
 * erneuten Öffnen.
 */
public final class ExportSettings {

    private static final String PREFS_PATH = "de/petanqueturniermanager/spielerdb/export";
    private static final String SCHLUESSEL_LETZTER_PFAD = "letzterPfad.";

    private final Preferences prefs;

    public ExportSettings() {
        this(Preferences.userRoot().node(PREFS_PATH));
    }

    /** Test-Konstruktor mit injiziertem Preferences-Knoten. */
    ExportSettings(Preferences prefs) {
        this.prefs = prefs;
    }

    @Nullable
    public String letzterPfad(ExportFormat format) {
        return prefs.get(SCHLUESSEL_LETZTER_PFAD + format.name(), null);
    }

    public void merkeLetztenPfad(ExportFormat format, String pfad) {
        prefs.put(SCHLUESSEL_LETZTER_PFAD + format.name(), pfad);
    }
}
