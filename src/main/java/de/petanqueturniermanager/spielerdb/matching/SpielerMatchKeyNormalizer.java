package de.petanqueturniermanager.spielerdb.matching;

import java.text.Normalizer;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

/**
 * Normalisiert Strings für den Vergleich von Spieler-/Verein-/Label-Identitäten.
 *
 * <p>Regeln (bewusst eng):
 * <ul>
 *   <li>Unicode-Normalisierung NFC — verhindert, dass kombinierende Akzente und
 *       precomposed Zeichen unterschiedlich gehasht werden.</li>
 *   <li>{@code toLowerCase(Locale.ROOT)} — vermeidet türkisches İ-Problem.</li>
 *   <li>Whitespace-Collapse {@code \\s+ → " "} und {@code strip()}.</li>
 *   <li><b>KEINE Diacritics-Faltung</b> — „Müller" ≠ „Muller".</li>
 * </ul>
 *
 * <p>Das Trennzeichen {@code |} im zusammengesetzten Schlüssel ist Whitespace-frei
 * und kann nach der Normalisierung in den Bestandteilen nicht vorkommen — daher
 * ist die Schlüssel-Konstruktion eindeutig umkehrbar (auch wenn das hier nicht
 * gebraucht wird, eliminiert es Kollisionen).
 */
public final class SpielerMatchKeyNormalizer {

    private SpielerMatchKeyNormalizer() { /* Utility */ }

    /**
     * Liefert die normalisierte Form von {@code s}. Für {@code null} wird ein
     * leerer String geliefert — Aufrufer müssen Null nicht selbst behandeln.
     */
    public static String normalisiere(@Nullable String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        String nfc = Normalizer.normalize(s, Normalizer.Form.NFC);
        String lower = nfc.toLowerCase(Locale.ROOT);
        String collapsed = lower.replaceAll("\\s+", " ");
        return collapsed.strip();
    }

    /**
     * Match-Schlüssel für Spieler über Vereins-<b>Name</b>. Wird vom
     * Meldelisten-Abgleich verwendet, der nur Namen aus der Meldeliste hat.
     */
    public static String spielerSchluesselMitVereinName(String vorname, String nachname,
            @Nullable String vereinName) {
        return normalisiere(vorname) + "|" + normalisiere(nachname)
                + "|N:" + normalisiere(vereinName);
    }

    /**
     * Match-Schlüssel für Spieler über Vereins-<b>NR</b>. Wird vom Import nach
     * dem Vereins-Mapping verwendet — IDs sind stabiler als Namen, sobald sie
     * einmal aufgelöst sind.
     */
    public static String spielerSchluesselMitVereinNr(String vorname, String nachname,
            @Nullable Integer vereinNr) {
        return normalisiere(vorname) + "|" + normalisiere(nachname)
                + "|R:" + (vereinNr == null ? "" : vereinNr.toString());
    }

    /** Match-Schlüssel für Vereine. */
    public static String vereinSchluessel(String name) {
        return normalisiere(name);
    }

    /** Match-Schlüssel für Labels. */
    public static String labelSchluessel(String name) {
        return normalisiere(name);
    }
}
