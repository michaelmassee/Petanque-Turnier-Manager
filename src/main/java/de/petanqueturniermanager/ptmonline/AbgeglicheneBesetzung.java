/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import de.petanqueturniermanager.ptmonline.dto.PersonDto;

/**
 * Zuletzt abgeglichene Besetzung einer Meldung (T-15, T-17, T-18): Namen je Personen-Slot und die Benutzer-ID
 * registrierter Personen. Liegt im Sync-Blatt in einer ausgeblendeten Spalte, nicht in einer frei editierbaren Zelle
 * der Meldeliste, und bleibt beim Sortieren an der lokalen UUID.
 * <p>
 * Sie beantwortet zwei Fragen: Hat sich eine Seite seit dem letzten Abgleich geändert (E-16)? Und welche Benutzer-ID
 * gehört zu einer lokal stehenden Person? Eine Person behält ihre Benutzer-ID, solange ihr Name unverändert ist; steht
 * im Slot ein anderer Name, ist das ein Personenwechsel und die ID entfällt (A-14, P-45).
 */
public final class AbgeglicheneBesetzung {

    private static final Logger logger = LogManager.getLogger(AbgeglicheneBesetzung.class);
    private static final Gson GSON = new Gson();
    private static final AbgeglicheneBesetzung LEER = new AbgeglicheneBesetzung(List.of());

    /** Eine Person der Besetzung; {@code u} ist die Benutzer-ID oder {@code null}. Kurze Feldnamen sparen Zellinhalt. */
    public record Person(String v, String n, @Nullable String u) {

        public Person {
            v = Objects.toString(v, "").strip();
            n = Objects.toString(n, "").strip();
            u = u == null || u.isBlank() ? null : u.strip();
        }

        public String vorname() {
            return v;
        }

        public String nachname() {
            return n;
        }

        public Optional<String> benutzerId() {
            return Optional.ofNullable(u);
        }

        String schluessel() {
            return OnlineSpielerName.schluessel(v, n);
        }
    }

    private final List<Person> personen;

    private AbgeglicheneBesetzung(List<Person> personen) {
        this.personen = List.copyOf(personen);
    }

    public static AbgeglicheneBesetzung leer() {
        return LEER;
    }

    public static AbgeglicheneBesetzung von(List<Person> personen) {
        return personen.isEmpty() ? LEER : new AbgeglicheneBesetzung(personen);
    }

    /** Besetzung einer Online-Anmeldung in Slot-Reihenfolge, samt Benutzer-IDs. */
    public static AbgeglicheneBesetzung ausOnline(List<PersonDto> personen) {
        return von(personen.stream().map(person -> new Person(person.firstName(), person.lastName(), person.userId()))
                .toList());
    }

    /** Liest den gespeicherten Zellinhalt; Unlesbares gilt als „noch nie abgeglichen“ und wird protokolliert. */
    public static AbgeglicheneBesetzung lese(@Nullable String text) {
        if (text == null || text.isBlank()) {
            return LEER;
        }
        try {
            Person[] gelesen = GSON.fromJson(text, Person[].class);
            return gelesen == null ? LEER : von(List.of(gelesen));
        } catch (JsonParseException e) {
            logger.warn("PTM-Online: zuletzt abgeglichene Besetzung unlesbar: {}", text, e);
            return LEER;
        }
    }

    /** Zellinhalt für das Sync-Blatt. */
    public String alsText() {
        return personen.isEmpty() ? "" : GSON.toJson(personen);
    }

    public List<Person> personen() {
        return personen;
    }

    public boolean istLeer() {
        return personen.isEmpty();
    }

    /** Vergleichsschlüssel der Namen, reihenfolgeunabhängig – „gleiche Besetzung“ im Sinn von E-16. */
    public String namensSchluessel() {
        return personen.stream().map(Person::schluessel).sorted().reduce((a, b) -> a + "\u0000" + b).orElse("");
    }

    /**
     * Personen für die Übertragung an PTM-Online: die lokale Besetzung in Slot-Reihenfolge, jede Person mit der
     * Benutzer-ID, die sie zuletzt trug. Steht eine Person nicht mehr in der Besetzung (Personenwechsel), wird sie ohne
     * Benutzer-ID gesendet; PTM-Online löst dann die Kontoverknüpfung dieses Slots (P-45).
     */
    public List<PersonDto> fuerUebertragung(List<Person> lokal) {
        List<String> vergeben = new ArrayList<>();
        List<PersonDto> ergebnis = new ArrayList<>();
        for (int i = 0; i < lokal.size(); i++) {
            Person person = lokal.get(i);
            String benutzerId = personen.stream()
                    .filter(bisher -> bisher.u() != null && !vergeben.contains(bisher.u()))
                    .filter(bisher -> bisher.schluessel().equals(person.schluessel()))
                    .map(Person::u).findFirst().orElse(null);
            if (benutzerId != null) {
                vergeben.add(benutzerId);
            }
            ergebnis.add(new PersonDto(i + 1, person.vorname(), person.nachname(), null, benutzerId));
        }
        return ergebnis;
    }

    /** Lokale Besetzung mit den Benutzer-IDs, die {@link #fuerUebertragung} vergeben würde. */
    public AbgeglicheneBesetzung mitBenutzerIdsFuer(List<Person> lokal) {
        return von(fuerUebertragung(lokal).stream()
                .map(person -> new Person(person.firstName(), person.lastName(), person.userId())).toList());
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof AbgeglicheneBesetzung besetzung && personen.equals(besetzung.personen);
    }

    @Override
    public int hashCode() {
        return personen.hashCode();
    }

    @Override
    public String toString() {
        return alsText();
    }
}
