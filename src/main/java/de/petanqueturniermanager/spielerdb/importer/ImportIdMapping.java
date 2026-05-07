package de.petanqueturniermanager.spielerdb.importer;

import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * Mappt alte NRs aus der Import-Datei auf die effektiven DB-NRs nach
 * Anwendung der Konflikt-Strategien.
 *
 * <p>Ein Eintrag {@code null}/fehlend bedeutet: der Datensatz wurde
 * übersprungen (z.B. Lizenznr-Verletzung bei INSERT_NEU). Junction-Zeilen,
 * die auf eine fehlende Mapping-Eintrag zeigen, werden in der Junction-Phase
 * still verworfen — die Ursache ist bereits über eine eigene Warnung
 * berichtet.
 */
public final class ImportIdMapping {

    private final Map<Integer, Integer> vereineAltZuNeu = new HashMap<>();
    private final Map<Integer, Integer> labelsAltZuNeu = new HashMap<>();
    private final Map<Integer, Integer> spielerAltZuNeu = new HashMap<>();

    public void merkeVerein(@Nullable Integer altNr, int neueNr) {
        if (altNr != null) {
            vereineAltZuNeu.put(altNr, neueNr);
        }
    }

    public void merkeLabel(@Nullable Integer altNr, int neueNr) {
        if (altNr != null) {
            labelsAltZuNeu.put(altNr, neueNr);
        }
    }

    public void merkeSpieler(@Nullable Integer altNr, int neueNr) {
        if (altNr != null) {
            spielerAltZuNeu.put(altNr, neueNr);
        }
    }

    @Nullable
    public Integer neueVereinNr(@Nullable Integer altNr) {
        return altNr == null ? null : vereineAltZuNeu.get(altNr);
    }

    @Nullable
    public Integer neueLabelNr(int altNr) {
        return labelsAltZuNeu.get(altNr);
    }

    @Nullable
    public Integer neueSpielerNr(int altNr) {
        return spielerAltZuNeu.get(altNr);
    }
}
