package de.petanqueturniermanager.helper.sheetsync;

/**
 * Ergebnis-Typ von {@link EingabeSignatur#berechne}.
 * <p>
 * Differenziert vier Fälle, damit der Listener entsprechend handeln kann:
 * <ul>
 *   <li>{@link Ok}: Hash ermittelt – Rebuild nur bei Abweichung vom gespeicherten Hash.</li>
 *   <li>{@link SheetFehlt}: Quelle fehlt; je nach {@code erwartet}-Flag Recovery oder skip.</li>
 *   <li>{@link TransientFehler}: kurzfristiger UNO-Fehler (Lock, Race); Re-Schedule sinnvoll.</li>
 *   <li>{@link PermanenterFehler}: Dauerhafter Fehler; log-warn und skip (kein Fail-Safe-Rebuild).</li>
 * </ul>
 */
public sealed interface SignaturErgebnis {

    /** Hash erfolgreich berechnet. */
    record Ok(String hash) implements SignaturErgebnis {
    }

    /**
     * Sheet zu einer Quelle fehlt im Dokument.
     *
     * @param stabileId betroffene Quell-ID
     * @param erwartet  {@code true}: laut Modell ein Konsistenzproblem → Recovery-Rebuild;
     *                  {@code false}: optionale Quelle (z.B. noch keine Spielrunde) → skip.
     */
    record SheetFehlt(String stabileId, boolean erwartet) implements SignaturErgebnis {
    }

    /**
     * Vorübergehender UNO-Fehler (Lock, Race, Read-Timeout). Versuch wird mitgegeben,
     * damit Listener das Retry-Limit zählen kann.
     */
    record TransientFehler(String grund, Throwable cause, int versuch) implements SignaturErgebnis {
    }

    /** Dauerhafter Fehler – kein automatischer Rebuild. */
    record PermanenterFehler(String grund, Throwable cause) implements SignaturErgebnis {
    }
}
