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

    /**
     * Eine Person der Besetzung; {@code u} ist die Benutzer-ID, {@code l} die Lizenznummer und {@code c} der Verein aus
     * PTM-Online, jeweils {@code null} wenn unbekannt. PTM-Online führt den Verein der Anmeldung bei der ersten Person.
     * Kurze Feldnamen sparen Zellinhalt; ältere Zellinhalte ohne {@code l}/{@code c} bleiben lesbar.
     */
    public record Person(String v, String n, @Nullable String u, @Nullable String l, @Nullable String c) {

        public Person {
            v = Objects.toString(v, "").strip();
            n = Objects.toString(n, "").strip();
            u = ohneLeer(u);
            l = ohneLeer(l);
            c = ohneLeer(c);
        }

        /** Person ohne Angaben aus PTM-Online außer der Benutzer-ID (lokale Besetzung, Teamname). */
        public Person(String v, String n, @Nullable String u) {
            this(v, n, u, null, null);
        }

        private static @Nullable String ohneLeer(@Nullable String wert) {
            return wert == null || wert.isBlank() ? null : wert.strip();
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

        public Optional<String> lizenzNr() {
            return Optional.ofNullable(l);
        }

        public Optional<String> verein() {
            return Optional.ofNullable(c);
        }

        /** Diese Person (Name aus dem Dokument) mit den Online-Angaben der zuletzt abgeglichenen. */
        private Person mitOnlineAngabenVon(Optional<Person> bisher) {
            return new Person(v, n, bisher.map(Person::u).orElse(null), bisher.map(Person::l).orElse(null),
                    bisher.map(Person::c).orElse(null));
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

    /** Besetzung einer Online-Anmeldung in Slot-Reihenfolge, samt Benutzer-IDs und Lizenznummern. */
    public static AbgeglicheneBesetzung ausOnline(List<PersonDto> personen) {
        return ausOnline(personen, null);
    }

    /** Wie {@link #ausOnline(List)}, zusätzlich mit dem Verein der Anmeldung bei der ersten Person. */
    public static AbgeglicheneBesetzung ausOnline(List<PersonDto> personen, @Nullable String verein) {
        List<Person> ergebnis = new ArrayList<>();
        for (PersonDto person : personen) {
            ergebnis.add(new Person(person.firstName(), person.lastName(), person.userId(), person.licenseNr(),
                    ergebnis.isEmpty() ? verein : null));
        }
        return von(ergebnis);
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
     * Personen für die Übertragung an PTM-Online: die lokale Besetzung in Slot-Reihenfolge, jede Person mit Benutzer-ID
     * und Lizenznummer, die sie zuletzt trug. Das Dokument kennt keine Lizenznummern; PTM-Online übernimmt aber die
     * gesendete (auch eine fehlende) – ohne Rückgabe ginge sie bei jeder Änderung verloren, etwa des Teamnamens. Steht
     * eine Person nicht mehr in der Besetzung (Personenwechsel), wird sie ohne beides gesendet; PTM-Online löst dann die
     * Kontoverknüpfung dieses Slots (P-45).
     */
    public List<PersonDto> fuerUebertragung(List<Person> lokal) {
        List<Person> zugeordnet = mitOnlineAngabenFuer(lokal);
        List<PersonDto> ergebnis = new ArrayList<>();
        for (int i = 0; i < zugeordnet.size(); i++) {
            Person person = zugeordnet.get(i);
            ergebnis.add(new PersonDto(i + 1, person.vorname(), person.nachname(), person.l(), person.u()));
        }
        return ergebnis;
    }

    /**
     * Verein, den PTM-Online für die lokale Besetzung behalten soll, wenn die Meldeliste keine Vereinsspalte hat: der
     * zuletzt abgeglichene der Person, die lokal an erster Stelle steht. Steht dort eine neue Person, ist er unbekannt.
     */
    public Optional<String> vereinFuer(List<Person> lokal) {
        return mitOnlineAngabenFuer(lokal).stream().findFirst().flatMap(Person::verein);
    }

    /** Lokale Besetzung mit den Online-Angaben, die {@link #fuerUebertragung} vergeben würde. */
    public AbgeglicheneBesetzung mitBenutzerIdsFuer(List<Person> lokal) {
        return von(mitOnlineAngabenFuer(lokal));
    }

    /** Ordnet jeder lokalen Person die zuletzt abgeglichene gleichen Namens zu und übernimmt deren Online-Angaben. */
    private List<Person> mitOnlineAngabenFuer(List<Person> lokal) {
        List<Person> frei = new ArrayList<>(personen);
        List<Person> ergebnis = new ArrayList<>();
        for (Person person : lokal) {
            Optional<Person> bisher = bisherigePerson(frei, person);
            bisher.ifPresent(frei::remove);
            ergebnis.add(person.mitOnlineAngabenVon(bisher));
        }
        return ergebnis;
    }

    /**
     * Zuletzt abgeglichene Person mit demselben Namen, die noch keiner lokalen Person zugeteilt ist. Eine mit Benutzer-ID
     * hat Vorrang: Bei Namensgleichheit im Team bekommt so jede ID genau eine Person.
     */
    private static Optional<Person> bisherigePerson(List<Person> frei, Person person) {
        List<Person> gleichnamig = frei.stream().filter(bisher -> bisher.schluessel().equals(person.schluessel()))
                .toList();
        return gleichnamig.stream().filter(bisher -> bisher.u() != null).findFirst()
                .or(() -> gleichnamig.stream().findFirst());
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
