package de.petanqueturniermanager.spielerdb.importer;

import java.util.ArrayList;
import java.util.List;

import de.petanqueturniermanager.spielerdb.SpielerDbException;

/**
 * Fasst alle harten Validierungs-Fehler einer Import-Quelle in einer Exception
 * zusammen — der Anwender bekommt alle Probleme auf einmal, statt sie iterativ
 * zu fixen. {@link #fehler()} liefert die einzelnen Meldungen.
 */
public final class SpielerDbValidationException extends SpielerDbException {

    private static final long serialVersionUID = 1L;

    /** ArrayList ist {@code Serializable}, daher ist die Exception serialisierbar. */
    private final ArrayList<String> fehler;

    public SpielerDbValidationException(List<String> fehler) {
        super("Import-Validierung fehlgeschlagen: " + String.join("; ", fehler));
        this.fehler = new ArrayList<>(fehler);
    }

    public List<String> fehler() {
        return List.copyOf(fehler);
    }
}
