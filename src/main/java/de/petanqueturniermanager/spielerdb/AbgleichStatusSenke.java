package de.petanqueturniermanager.spielerdb;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Optionale Senke für den Abgleich-Dialog: erlaubt es einer Quelle (z.B. der
 * Vorlage), das Ergebnis pro Zeile zurückzuschreiben (Status-Spalte +
 * Fehlerursache). Bewusst getrennt von {@link AbgleichQuelle}, damit das
 * Read-Only-Interface read-only bleibt – nicht jede Quelle ist beschreibbar
 * (Meldeliste z.B. nicht).
 */
public interface AbgleichStatusSenke {

    void schreibeStatus(List<ZeilenStatus> eintraege);

    /** Status pro Zeile der Quelle nach abgeschlossenem Import-Klick. */
    record ZeilenStatus(int zeile1Basiert, AbgleichStatus status, @Nullable String fehlerursache) {}

    enum AbgleichStatus {
        /** Datensatz war vor dem Import bereits in der DB. */
        IN_DB,
        /** Datensatz wurde beim Import-Klick neu in die DB übernommen. */
        NEU,
        /** Datensatz war zum Import markiert, das Schreiben in die DB scheiterte. */
        FEHLER,
        /** Datensatz fehlt weiterhin in der DB (nicht zum Import markiert). */
        FEHLT
    }
}
