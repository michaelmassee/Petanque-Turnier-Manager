/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.onlinesync.speicher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Unveränderliche Zuordnungstabelle lokale Meldung ↔ Online-Anmeldung einer Verbindung, in Einfügereihenfolge. Jede
 * lokale UUID kommt höchstens einmal vor. Ändernde Operationen liefern eine neue Tabelle; ist sie gleich der alten,
 * muss nichts gespeichert werden.
 */
public record PtmOnlineZuordnungsTabelle(List<PtmOnlineZuordnung> zuordnungen) {

    public static final PtmOnlineZuordnungsTabelle LEER = new PtmOnlineZuordnungsTabelle(List.of());

    public PtmOnlineZuordnungsTabelle {
        Map<String, PtmOnlineZuordnung> eindeutig = new LinkedHashMap<>();
        zuordnungen.forEach(zuordnung -> eindeutig.putIfAbsent(zuordnung.lokaleUuid(), zuordnung));
        zuordnungen = List.copyOf(eindeutig.values());
    }

    public int anzahl() {
        return zuordnungen.size();
    }

    public Set<String> uuids() {
        return proUuid(PtmOnlineZuordnung::lokaleUuid).keySet();
    }

    public Optional<PtmOnlineZuordnung> zuordnung(String lokaleUuid) {
        return zuordnungen.stream().filter(zuordnung -> zuordnung.lokaleUuid().equals(lokaleUuid)).findFirst();
    }

    /** Wert je lokaler UUID, in Tabellenreihenfolge. */
    public <T> Map<String, T> proUuid(Function<PtmOnlineZuordnung, T> wert) {
        Map<String, T> ergebnis = new LinkedHashMap<>();
        zuordnungen.forEach(zuordnung -> ergebnis.put(zuordnung.lokaleUuid(), wert.apply(zuordnung)));
        return ergebnis;
    }

    /** Hängt neue Zuordnungen an; bereits zugeordnete lokale UUIDs bleiben unverändert. */
    public PtmOnlineZuordnungsTabelle mitNeuen(Collection<NeueZuordnung> neue) {
        List<PtmOnlineZuordnung> ergebnis = new ArrayList<>(zuordnungen);
        neue.stream().map(PtmOnlineZuordnung::aus).forEach(ergebnis::add);
        return new PtmOnlineZuordnungsTabelle(ergebnis);
    }

    public PtmOnlineZuordnungsTabelle mitAnzeigen(Map<String, ZuordnungsAnzeige> anzeigeProUuid) {
        return aendere(anzeigeProUuid, PtmOnlineZuordnung::mitAnzeige);
    }

    public PtmOnlineZuordnungsTabelle mitBezeichnung(String lokaleUuid, String bezeichnung, String onlineStatus) {
        return aendere(Map.of(lokaleUuid, Boolean.TRUE),
                (zuordnung, betroffen) -> zuordnung.mitBezeichnung(bezeichnung, onlineStatus));
    }

    public PtmOnlineZuordnungsTabelle mitDetails(String lokaleUuid, ZuordnungsAnzeige.OnlineDetails details) {
        return aendere(Map.of(lokaleUuid, details), PtmOnlineZuordnung::mitDetails);
    }

    public PtmOnlineZuordnungsTabelle mitRevisionen(Map<String, Integer> revisionProUuid) {
        return aendere(revisionProUuid, PtmOnlineZuordnung::mitRevision);
    }

    public PtmOnlineZuordnungsTabelle mitZusaetzen(Map<String, ZuordnungsZusatz> zusatzProUuid) {
        return aendere(zusatzProUuid, PtmOnlineZuordnung::mitZusatz);
    }

    /** Entfernt die Zuordnungen der genannten lokalen UUIDs; die übrigen behalten ihre Reihenfolge. */
    public PtmOnlineZuordnungsTabelle ohne(Set<String> lokaleUuids) {
        return new PtmOnlineZuordnungsTabelle(
                zuordnungen.stream().filter(zuordnung -> !lokaleUuids.contains(zuordnung.lokaleUuid())).toList());
    }

    /** Wendet {@code aenderung} auf jede Zuordnung an, für deren lokale UUID ein Wert vorliegt. */
    private <T> PtmOnlineZuordnungsTabelle aendere(Map<String, T> wertProUuid,
            BiFunction<PtmOnlineZuordnung, T, PtmOnlineZuordnung> aenderung) {
        return new PtmOnlineZuordnungsTabelle(zuordnungen.stream().map(zuordnung -> {
            T wert = wertProUuid.get(zuordnung.lokaleUuid());
            return wert == null ? zuordnung : aenderung.apply(zuordnung, wert);
        }).toList());
    }
}
