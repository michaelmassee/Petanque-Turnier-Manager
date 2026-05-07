package de.petanqueturniermanager.spielerdb.importer;

import java.util.prefs.Preferences;

import org.jspecify.annotations.Nullable;

import de.petanqueturniermanager.spielerdb.SpielerDbDateiFormat;

/**
 * Persistiert den zuletzt gewählten Quellpfad pro {@link SpielerDbDateiFormat}.
 * Spiegelbild zu {@code ExportSettings} — eigener Preferences-Knoten, damit
 * Export- und Import-Pfade unabhängig voneinander gemerkt werden.
 */
public final class ImportSettings {

    private static final String PREFS_PATH = "de/petanqueturniermanager/spielerdb/import";
    private static final String SCHLUESSEL_LETZTER_PFAD = "letzterPfad.";

    private final Preferences prefs;

    public ImportSettings() {
        this(Preferences.userRoot().node(PREFS_PATH));
    }

    /** Test-Konstruktor mit injiziertem Preferences-Knoten. */
    ImportSettings(Preferences prefs) {
        this.prefs = prefs;
    }

    @Nullable
    public String letzterPfad(SpielerDbDateiFormat format) {
        return prefs.get(SCHLUESSEL_LETZTER_PFAD + format.name(), null);
    }

    public void merkeLetztenPfad(SpielerDbDateiFormat format, String pfad) {
        prefs.put(SCHLUESSEL_LETZTER_PFAD + format.name(), pfad);
    }
}
