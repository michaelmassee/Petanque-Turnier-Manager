/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.ptmonline.dto.NeueOnlineAnmeldung;
import de.petanqueturniermanager.spielerdb.MeldelisteSpielerDaten;

/**
 * Inhalt einer lokalen Meldung, wie er an PTM-Online geht – gemeinsam für den manuellen Abgleich
 * ({@link RegistrationImportTask}) und den automatischen Meldelisten-Abgleich ({@link PtmOnlineMeldelistenSync}).
 */
final class PtmOnlineMeldungsInhalt {

    private PtmOnlineMeldungsInhalt() {
    }

    /** Spieler der Meldeliste gruppiert nach ihrer 1-basierten Sheet-Zeile, in Zeilenreihenfolge. */
    static Map<Integer, List<MeldelisteSpielerDaten>> proZeile(List<MeldelisteSpielerDaten> spieler) {
        return spieler.stream().collect(Collectors.groupingBy(MeldelisteSpielerDaten::zeile1Basiert,
                LinkedHashMap::new, Collectors.toList()));
    }

    /**
     * Lokale Meldung als Online-Anmeldung. Der Teamname wird immer mitgeschickt: PTM-Online übernimmt ihn bei einer
     * Änderung aus dem Dokument auch dann, wenn er fehlt, und würde den Online-Teamnamen sonst löschen.
     * <p>
     * „Nur Teamname“: Die Meldung hat keine Personen, nur den Teamnamen (einziger Namens-Slot). PTM-Online legt sie
     * ohne Kontaktperson an und behält bei einer Änderung aus dem Dokument die Online-Kontaktperson.
     *
     * @param teamname Inhalt der Teamname-Spalte; bei „Nur Teamname“ unbeachtet
     */
    static NeueOnlineAnmeldung anmeldung(Formation formation, List<MeldelisteSpielerDaten> spieler,
            @Nullable String teamname) {
        return anmeldung(formation, spieler, teamname, spieler.getFirst().vereinName());
    }

    /**
     * Wie {@link #anmeldung(Formation, List, String)}, mit ausdrücklichem Verein (siehe {@link #verein}). PTM-Online
     * übernimmt den Verein bei einer Änderung aus dem Dokument immer – ein fehlender löscht den Online-Verein.
     */
    static NeueOnlineAnmeldung anmeldung(Formation formation, List<MeldelisteSpielerDaten> spieler,
            @Nullable String teamname, @Nullable String verein) {
        if (formation == Formation.NUR_TEAMNAME) {
            return new NeueOnlineAnmeldung("", "", null, null, null, null, null, null, spieler.getFirst().vorname(),
                    true, true, List.of(), List.of());
        }
        MeldelisteSpielerDaten erster = spieler.get(0);
        MeldelisteSpielerDaten zweiter = spieler.size() > 1 ? spieler.get(1) : null;
        MeldelisteSpielerDaten dritter = spieler.size() > 2 ? spieler.get(2) : null;
        return new NeueOnlineAnmeldung(
                erster.vorname(), erster.nachname(), verein, null,
                zweiter == null ? null : zweiter.vorname(), zweiter == null ? null : zweiter.nachname(),
                dritter == null ? null : dritter.vorname(), dritter == null ? null : dritter.nachname(),
                StringUtils.trimToNull(teamname), true, true, List.of(), List.of());
    }

    /**
     * Verein einer geänderten Meldung: aus der Vereinsspalte, sonst der zuletzt abgeglichene Online-Verein – das
     * Dokument kennt ihn dann nicht und darf ihn online nicht löschen, etwa bei einer reinen Teamnamenänderung.
     *
     * @param zuletzt zuletzt abgeglichene (oder aktuelle Online-)Besetzung samt Verein
     */
    static @Nullable String verein(List<MeldelisteSpielerDaten> spieler, boolean vereinsspalte,
            AbgeglicheneBesetzung zuletzt) {
        return vereinsspalte ? spieler.getFirst().vereinName() : zuletzt.vereinFuer(personen(spieler)).orElse(null);
    }

    /** Personen der Meldung ohne Benutzer-IDs, für den Vergleich mit der zuletzt abgeglichenen Besetzung. */
    static List<AbgeglicheneBesetzung.Person> personen(List<MeldelisteSpielerDaten> spieler) {
        return spieler.stream().map(daten -> new AbgeglicheneBesetzung.Person(daten.vorname(), daten.nachname(), null))
                .toList();
    }

    /** Lokale Bezeichnung „Vorname Nachname / …“ der Meldung. */
    static String bezeichnung(List<MeldelisteSpielerDaten> spieler) {
        return spieler.stream().map(daten -> name(daten.vorname(), daten.nachname())).filter(name -> !name.isBlank())
                .collect(Collectors.joining(" / "));
    }

    /** „Vorname Nachname“, getrimmt; fehlende Teile entfallen. */
    static String name(@Nullable String vorname, @Nullable String nachname) {
        return (StringUtils.strip(StringUtils.defaultString(vorname)) + " "
                + StringUtils.strip(StringUtils.defaultString(nachname))).strip();
    }
}
