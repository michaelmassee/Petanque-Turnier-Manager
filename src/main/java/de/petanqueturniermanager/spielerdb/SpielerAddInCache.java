package de.petanqueturniermanager.spielerdb;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * TTL-basierter Result-Cache für die Calc-AddIn-Funktionen
 * {@code PTM.DB.SPIELER.*}. Verhindert, dass jeder Recalc einer
 * Calc-Tabelle mit vielen Lookup-Zellen die HSQLDB-Datei n-fach hittet.
 *
 * <p>Bewusst keine Invalidierung bei Repository-Schreiboperationen —
 * die UI-Dialoge laufen in einer anderen Calc-Recalc-Welt; 30 s Staleness
 * sind akzeptabel und vermeiden Coupling zwischen UI und AddIn.
 */
public final class SpielerAddInCache {

    static final long TTL_MILLIS = 30_000L;

    private final ConcurrentHashMap<String, Eintrag> cache = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public <T> T holeOderBerechne(String key, Supplier<T> berechner) {
        long jetzt = System.currentTimeMillis();
        Eintrag vorhanden = cache.get(key);
        if (vorhanden != null && jetzt - vorhanden.zeitstempel < TTL_MILLIS) {
            return (T) vorhanden.wert;
        }
        T neu = berechner.get();
        cache.put(key, new Eintrag(jetzt, neu));
        return neu;
    }

    public void leeren() {
        cache.clear();
    }

    private record Eintrag(long zeitstempel, Object wert) {}
}
