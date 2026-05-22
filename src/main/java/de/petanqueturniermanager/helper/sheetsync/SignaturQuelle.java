package de.petanqueturniermanager.helper.sheetsync;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Set;
import java.util.TreeSet;

/**
 * Deklarative Beschreibung einer Eingangsquelle für eine Sheet-Sync-Signatur.
 * <p>
 * Eine Quelle adressiert ein Sheet ausschließlich über seinen Named-Range-Schlüssel
 * ({@link de.petanqueturniermanager.helper.sheet.SheetMetadataHelper}). Tab-Position,
 * Sheet-Name und Sheet-Index spielen für die Identität keine Rolle.
 *
 * @param stabileId            stabile, system-eindeutige ID der Quelle
 *                             (z.B. {@code "SCHWEIZER-MELDELISTE"} oder
 *                             {@code "SCHWEIZER-SPIELRUNDE-3"}). Wird in den Hash
 *                             aufgenommen und bestimmt die Sortier-Reihenfolge.
 * @param sheetNamedRangeSchluessel  Named-Range-Schlüssel des Sheets (z.B.
 *                             {@code SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_MELDELISTE}).
 * @param ersteZeile           erste zu hashende Zeile (0-basiert, inkl.).
 * @param maxAnzahlZeilen      maximale Zeilen-Anzahl, die gelesen wird. Leere
 *                             Zeilen am Ende werden bei der Serialisierung übersprungen.
 * @param relevanteSpalten     Whitelist semantisch relevanter Spalten (0-basiert,
 *                             absolut). Nicht-gelistete Spalten fließen nicht in den Hash.
 * @param erwartet             {@code true} wenn die Quelle laut Modell existieren MUSS.
 *                             Bei {@code SheetFehlt}/erwartet=true löst der Listener
 *                             einen einmaligen Recovery-Rebuild aus. Bei {@code false}
 *                             (optionale Quelle, z.B. noch keine Spielrunde angelegt)
 *                             wird das Fehlen ignoriert.
 */
public record SignaturQuelle(
        String stabileId,
        String sheetNamedRangeSchluessel,
        int ersteZeile,
        int maxAnzahlZeilen,
        Set<Integer> relevanteSpalten,
        boolean erwartet) {

    public SignaturQuelle {
        checkNotNull(stabileId, "stabileId");
        checkArgument(!stabileId.isBlank(), "stabileId darf nicht leer sein");
        checkNotNull(sheetNamedRangeSchluessel, "sheetNamedRangeSchluessel");
        checkArgument(!sheetNamedRangeSchluessel.isBlank(),
                "sheetNamedRangeSchluessel darf nicht leer sein");
        checkArgument(ersteZeile >= 0, "ersteZeile muss >= 0 sein");
        checkArgument(maxAnzahlZeilen > 0, "maxAnzahlZeilen muss > 0 sein");
        checkNotNull(relevanteSpalten, "relevanteSpalten");
        checkArgument(!relevanteSpalten.isEmpty(),
                "relevanteSpalten darf nicht leer sein für Quelle '%s'", stabileId);
        // unveränderlich + sortiert für deterministische Iteration im Hash
        relevanteSpalten = new TreeSet<>(relevanteSpalten);
    }

    /** Kleinster Spaltenindex der Whitelist (für Bulk-Read-Start). */
    public int ersteRelevanteSpalte() {
        return relevanteSpalten.iterator().next();
    }

    /** Größter Spaltenindex der Whitelist (für Bulk-Read-Ende). */
    public int letzteRelevanteSpalte() {
        int letzte = ersteRelevanteSpalte();
        for (int s : relevanteSpalten) {
            if (s > letzte) {
                letzte = s;
            }
        }
        return letzte;
    }
}
