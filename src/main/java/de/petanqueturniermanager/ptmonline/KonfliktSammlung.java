/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import de.petanqueturniermanager.onlinesync.sheet.PtmOnlineKonfliktSheet;

/**
 * Konfliktliste eines Abgleichs im Aufbau (A-29): die eingetragenen Entscheidungen der vorigen Liste, die neu
 * gefundenen offenen Fälle, die angewendeten Entscheidungen fürs Protokoll und die lokalen Meldungen, die wegen eines
 * offenen Falls nicht online angelegt werden dürfen. Eine angewendete Entscheidung schließt ihren Fall; ein Fall, der
 * offen bleibt, behält eine noch gültige Wahl in der neuen Liste.
 */
public final class KonfliktSammlung {

    /** Angewendete Entscheidung für das Online-Protokoll. */
    public record Protokoll(Entscheidung entscheidung, @Nullable String lokaleUuid, @Nullable String onlineId) {}

    private final Map<String, String> eingetragen;
    private final Map<String, KonfliktFall> faelle = new LinkedHashMap<>();
    private final Set<String> zurueckgehalten = new HashSet<>();
    private final List<Protokoll> protokoll = new ArrayList<>();

    private KonfliktSammlung(Map<String, String> eingetragen) {
        this.eingetragen = Map.copyOf(eingetragen);
    }

    /** @param eingetragen Zelltext der Entscheidung je Fallschlüssel, wie in der Konfliktliste eingetragen */
    public static KonfliktSammlung mit(Map<String, String> eingetragen) {
        return new KonfliktSammlung(eingetragen);
    }

    public static KonfliktSammlung ohneEntscheidungen() {
        return new KonfliktSammlung(Map.of());
    }

    public boolean hatEntscheidungen() {
        return !eingetragen.isEmpty();
    }

    /** Die eingetragene Entscheidung zum Fall, sofern sie zu seinen Optionen passt. */
    public Optional<Entscheidung> entscheidung(KonfliktFall fall) {
        return Entscheidung.aus(eingetragen.get(fall.schluessel()), fall.optionen());
    }

    /** Nimmt einen offenen Fall auf; ein zweiter Fall mit demselben Schlüssel ersetzt nichts. */
    public void melde(KonfliktFall fall) {
        faelle.putIfAbsent(fall.schluessel(), fall);
    }

    /** Hält eine lokale Meldung von der Online-Anlage zurück, bis ihr Fall entschieden ist. */
    public void halteZurueck(String lokaleUuid) {
        zurueckgehalten.add(lokaleUuid);
    }

    public boolean istZurueckgehalten(String lokaleUuid) {
        return zurueckgehalten.contains(lokaleUuid);
    }

    /** Merkt eine angewendete Entscheidung fürs Protokoll. */
    public void angewendet(Entscheidung entscheidung, @Nullable String lokaleUuid, @Nullable String onlineId) {
        protokoll.add(new Protokoll(entscheidung, lokaleUuid, onlineId));
    }

    public List<KonfliktFall> faelle() {
        return List.copyOf(faelle.values());
    }

    public List<Protokoll> protokoll() {
        return List.copyOf(protokoll);
    }

    /** Offene Fälle mit einem Ausschlussgrund (Vorabcheck vor dem ersten Rundenstart). */
    public List<KonfliktFall> ausschlussgruende() {
        return faelle.values().stream().filter(fall -> fall.art().istAusschlussgrund()).toList();
    }

    /** Zeilen der neuen Konfliktliste, nach Art gruppiert; eine weiter gültige Wahl bleibt eingetragen. */
    public List<PtmOnlineKonfliktSheet.Zeile> zeilen() {
        return faelle.values().stream()
                .sorted((a, b) -> Integer.compare(a.art().ordinal(), b.art().ordinal()))
                .map(fall -> new PtmOnlineKonfliktSheet.Zeile(fall.art().anzeige(), fall.lokal(), fall.online(),
                        fall.hinweis(), fall.schluessel(), fall.optionen().stream().map(Entscheidung::anzeige).toList(),
                        entscheidung(fall).map(Entscheidung::anzeige).orElse("")))
                .toList();
    }
}
