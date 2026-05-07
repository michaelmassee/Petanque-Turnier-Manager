package de.petanqueturniermanager.spielerdb.importer;

import java.util.List;

/**
 * Counts pro Entity plus gesammelte Warnungen. Bei {@code dryRun=true} sind
 * die Counts identisch zum echten Import — nur die DB wurde nicht persistiert.
 */
public record ImportErgebnis(
        int spielerEingefuegt,
        int spielerAktualisiert,
        int spielerUebersprungen,
        int vereineEingefuegt,
        int vereineAktualisiert,
        int vereineUebersprungen,
        int labelsEingefuegt,
        int labelsAktualisiert,
        int labelsUebersprungen,
        int junctionEingefuegt,
        boolean dryRun,
        List<ImportWarnung> warnungen) {

    public ImportErgebnis {
        warnungen = List.copyOf(warnungen);
    }
}
