/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;

import de.petanqueturniermanager.basesheet.meldeliste.TeilnehmerSheetBuilder.TeilnehmerEintrag;

/**
 * Sortierreihenfolge der Teilnehmerliste, konfigurierbar in der Turnier-Konfiguration.
 * <p>
 * Der {@code key} wird als Wert der Auswahl-Property (Combobox) in den Document-Properties
 * gespeichert und über {@code readEnumProperty(...)} wieder eingelesen.
 */
public enum TeilnehmerListeSortModus {

    /** Sortierung nach Melde-/Teamnummer. */
    NUMMER,
    /** Standard: Sortierung nach Nachname von Spieler 1. */
    NAME,
    /** Sortierung nach freiem Teamnamen. */
    TEAMNAME;

    /**
     * Schlüssel für die Speicherung als Document-Property.
     *
     * @return Enum-Name als Schlüssel
     */
    public String getKey() {
        return name();
    }

    /**
     * Liefert den passenden {@link Comparator} für diesen Sortiermodus. Namen werden
     * locale-korrekt (Deutsch, Umlaute, case-insensitiv) verglichen; bei Gleichstand
     * entscheidet die Nummer.
     *
     * @return Comparator über {@link TeilnehmerEintrag}
     */
    public Comparator<TeilnehmerEintrag> comparator() {
        return switch (this) {
            case NUMMER -> Comparator.comparingInt(TeilnehmerEintrag::nr);
            case NAME -> nachTextDannNr(TeilnehmerEintrag::sortNachname);
            case TEAMNAME -> nachTextDannNr(TeilnehmerEintrag::teamname);
        };
    }

    private static Comparator<TeilnehmerEintrag> nachTextDannNr(
            java.util.function.Function<TeilnehmerEintrag, String> schluessel) {
        Collator collator = Collator.getInstance(Locale.GERMAN);
        collator.setStrength(Collator.SECONDARY);
        Comparator<TeilnehmerEintrag> nachText = Comparator.comparing(
                eintrag -> schluessel.apply(eintrag) != null ? schluessel.apply(eintrag) : "", collator);
        return nachText.thenComparingInt(TeilnehmerEintrag::nr);
    }
}
