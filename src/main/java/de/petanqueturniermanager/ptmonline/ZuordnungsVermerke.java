/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import org.jspecify.annotations.Nullable;

/**
 * Sprachneutrale Vermerke einer Zuordnung im Sync-Blatt (Mapping-Tabelle der Spezifikation), gespeichert als
 * {@code SCHLUESSEL[=wert];...}. Unbekannte Schlüssel bleiben beim Ändern erhalten.
 */
public final class ZuordnungsVermerke {

    /**
     * Online storniert oder auf der Warteliste (KP-14): Die Meldung ist lokal als abgemeldet markiert und von der
     * Auslosung ausgeschlossen. Der Wert ist der Aktiv-Wert vor der Markierung, damit eine erneute Bestätigung den
     * lokalen Check-in-Zustand unverändert wiederherstellt.
     */
    static final String AUSGESCHLOSSEN = "AUSGESCHLOSSEN";
    /** Die Turnierleitung behält die online stornierte Meldung bewusst (KP-14); kein erneuter Ausschluss. */
    static final String BEHALTEN = "BEHALTEN";
    /** Die Meldung wurde lokal entfernt; die Online-Anmeldung wird nicht erneut importiert (KP-15). */
    static final String LOKAL_ENTFERNT = "LOKAL_ENTFERNT";

    private static final String KEIN_WERT = "";

    private final Map<String, String> eintraege;

    private ZuordnungsVermerke(Map<String, String> eintraege) {
        this.eintraege = Map.copyOf(eintraege);
    }

    public static ZuordnungsVermerke lese(@Nullable String text) {
        Map<String, String> eintraege = new LinkedHashMap<>();
        if (text != null) {
            for (String teil : text.split(";")) {
                String eintrag = teil.strip();
                if (eintrag.isEmpty()) {
                    continue;
                }
                int gleich = eintrag.indexOf('=');
                if (gleich < 0) {
                    eintraege.put(eintrag, KEIN_WERT);
                } else {
                    eintraege.put(eintrag.substring(0, gleich).strip(), eintrag.substring(gleich + 1).strip());
                }
            }
        }
        return new ZuordnungsVermerke(eintraege);
    }

    public boolean hat(String schluessel) {
        return eintraege.containsKey(schluessel);
    }

    public Optional<String> wert(String schluessel) {
        return Optional.ofNullable(eintraege.get(schluessel)).filter(wert -> !wert.isEmpty());
    }

    /** Ganzzahliger Wert eines Vermerks, leer wenn er fehlt oder keine Zahl ist. */
    public OptionalInt zahl(String schluessel) {
        Optional<String> wert = wert(schluessel);
        if (wert.isEmpty()) {
            return OptionalInt.empty();
        }
        try {
            return OptionalInt.of(Integer.parseInt(wert.get()));
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
    }

    public ZuordnungsVermerke mit(String schluessel) {
        return mit(schluessel, KEIN_WERT);
    }

    public ZuordnungsVermerke mit(String schluessel, String wert) {
        Map<String, String> neu = new LinkedHashMap<>(eintraege);
        neu.put(schluessel, wert);
        return new ZuordnungsVermerke(neu);
    }

    public ZuordnungsVermerke ohne(String schluessel) {
        Map<String, String> neu = new LinkedHashMap<>(eintraege);
        neu.remove(schluessel);
        return new ZuordnungsVermerke(neu);
    }

    /** Zellinhalt, Schlüssel sortiert für einen stabilen Vergleich. */
    public String alsText() {
        return eintraege.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(eintrag -> eintrag.getValue().isEmpty() ? eintrag.getKey() : eintrag.getKey() + "=" + eintrag.getValue())
                .reduce((a, b) -> a + ";" + b).orElse("");
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ZuordnungsVermerke vermerke && eintraege.equals(vermerke.eintraege);
    }

    @Override
    public int hashCode() {
        return eintraege.hashCode();
    }

    @Override
    public String toString() {
        return alsText();
    }
}
