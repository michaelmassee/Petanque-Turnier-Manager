package de.petanqueturniermanager.helper.rangliste;

import static com.google.common.base.Preconditions.checkNotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.helper.DocumentPropertiesHelper;

/**
 * Persistenter Speicher für Rangliste-Eingangs-Signaturen.
 * <p>
 * Werte werden als UserDefinedProperty des Spreadsheet-Dokuments abgelegt
 * (siehe {@link DocumentPropertiesHelper}) und überleben dadurch Reload und Speichern.
 * <p>
 * Property-Schema pro Rangliste-Schlüssel:
 * <ul>
 *   <li>{@code ranking.<key>.last.rebuild.hash} – Hash der zuletzt verbauten Eingabe</li>
 *   <li>{@code ranking.<key>.last.rebuild.ts}   – ISO-8601 UTC Zeitstempel des letzten Rebuilds</li>
 *   <li>{@code ranking.<key>.last.rebuild.reason} – Grund des letzten Rebuilds</li>
 *   <li>{@code ranking.<key>.last.verify.ts}    – ISO-8601 UTC der letzten Hash-Verifikation
 *       (auch ohne Rebuild) – steuert die Safety-Revalidation.</li>
 *   <li>{@code ranking.<key>.recovery.attempted} – true nach einmaligem Recovery-Rebuild
 *       (SheetFehlt(erwartet=true)); wird beim nächsten {@code Ok}-Pfad zurückgesetzt.</li>
 * </ul>
 */
public final class RanglisteSignaturStore {

    private static final Logger logger = LogManager.getLogger(RanglisteSignaturStore.class);

    private static final String PREFIX = "ranking.";
    private static final String SUFFIX_HASH = ".last.rebuild.hash";
    private static final String SUFFIX_REBUILD_TS = ".last.rebuild.ts";
    private static final String SUFFIX_REASON = ".last.rebuild.reason";
    private static final String SUFFIX_VERIFY_TS = ".last.verify.ts";
    private static final String SUFFIX_RECOVERY = ".recovery.attempted";

    private RanglisteSignaturStore() {
    }

    private static String propName(String schluessel, String suffix) {
        return PREFIX + schluessel + suffix;
    }

    public static Optional<String> ladeHash(XSpreadsheetDocument xDoc, String schluessel) {
        checkNotNull(xDoc);
        checkNotNull(schluessel);
        try {
            String wert = new DocumentPropertiesHelper(xDoc)
                    .getStringProperty(propName(schluessel, SUFFIX_HASH), "");
            return wert.isEmpty() ? Optional.empty() : Optional.of(wert);
        } catch (RuntimeException e) {
            logger.warn("Hash lesen fehlgeschlagen für '{}'", schluessel, e);
            return Optional.empty();
        }
    }

    /**
     * Schreibt Hash + Zeitstempel + Grund nach erfolgreichem Rebuild und setzt das
     * Recovery-Flag zurück. Der Verify-Zeitstempel wird ebenfalls aktualisiert
     * (jeder Rebuild zählt als Verifikation).
     */
    public static void speichereNachRebuild(XSpreadsheetDocument xDoc, String schluessel,
            String hash, String reason) {
        checkNotNull(xDoc);
        checkNotNull(schluessel);
        checkNotNull(hash);
        checkNotNull(reason);
        try {
            DocumentPropertiesHelper props = new DocumentPropertiesHelper(xDoc);
            String jetzt = Instant.now().toString();
            props.setStringProperty(propName(schluessel, SUFFIX_HASH), hash);
            props.setStringProperty(propName(schluessel, SUFFIX_REBUILD_TS), jetzt);
            props.setStringProperty(propName(schluessel, SUFFIX_REASON), reason);
            props.setStringProperty(propName(schluessel, SUFFIX_VERIFY_TS), jetzt);
            props.setBooleanProperty(propName(schluessel, SUFFIX_RECOVERY), false);
        } catch (RuntimeException e) {
            logger.warn("Hash speichern fehlgeschlagen für '{}'", schluessel, e);
        }
    }

    /**
     * Aktualisiert ausschließlich den Verify-Zeitstempel – ohne Rebuild. Wird gerufen,
     * wenn der frisch berechnete Hash identisch zum gespeicherten ist (Skip-Pfad nach
     * Safety-Revalidation oder regulärem Skip).
     */
    public static void aktualisiereVerifyZeit(XSpreadsheetDocument xDoc, String schluessel) {
        checkNotNull(xDoc);
        checkNotNull(schluessel);
        try {
            new DocumentPropertiesHelper(xDoc).setStringProperty(
                    propName(schluessel, SUFFIX_VERIFY_TS), Instant.now().toString());
        } catch (RuntimeException e) {
            logger.warn("Verify-Zeit aktualisieren fehlgeschlagen für '{}'", schluessel, e);
        }
    }

    /**
     * Liest den letzten Verifikations-Zeitstempel. Liefert {@link Optional#empty()},
     * wenn noch nie verifiziert wurde oder das Property unleserlich ist.
     */
    public static Optional<Instant> ladeVerifyZeit(XSpreadsheetDocument xDoc, String schluessel) {
        checkNotNull(xDoc);
        checkNotNull(schluessel);
        try {
            String wert = new DocumentPropertiesHelper(xDoc)
                    .getStringProperty(propName(schluessel, SUFFIX_VERIFY_TS), "");
            if (wert.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(Instant.parse(wert));
        } catch (RuntimeException e) {
            logger.warn("Verify-Zeit lesen fehlgeschlagen für '{}'", schluessel, e);
            return Optional.empty();
        }
    }

    /** {@code true} wenn die letzte Verifikation länger als {@code intervall} her ist. */
    public static boolean verifyVeraltet(XSpreadsheetDocument xDoc, String schluessel,
            Duration intervall) {
        Optional<Instant> verify = ladeVerifyZeit(xDoc, schluessel);
        if (verify.isEmpty()) {
            return true;
        }
        return Duration.between(verify.get(), Instant.now()).compareTo(intervall) > 0;
    }

    public static boolean recoveryBereitsVersucht(XSpreadsheetDocument xDoc, String schluessel) {
        checkNotNull(xDoc);
        checkNotNull(schluessel);
        try {
            return new DocumentPropertiesHelper(xDoc)
                    .getBooleanProperty(propName(schluessel, SUFFIX_RECOVERY), false);
        } catch (RuntimeException e) {
            logger.warn("Recovery-Flag lesen fehlgeschlagen für '{}'", schluessel, e);
            return false;
        }
    }

    /**
     * Berechnet die aktuelle Eingabe-Signatur und schreibt sie nach einem Vollaufbau
     * (forceCreate-Pfad) in den Store. Wird am Ende von
     * {@code *RanglisteSheet.doRunIntern()} aufgerufen, damit der Listener beim
     * nächsten Trigger erkennt: „Hash unverändert" – kein zweiter Rebuild.
     * <p>
     * Nur bei {@link SignaturErgebnis.Ok} wird geschrieben. Andere Fälle werden
     * geloggt und ignoriert: der Vollaufbau bleibt erfolgreich; beim nächsten
     * Listener-Trigger entscheidet die dann frische Signatur-Berechnung.
     */
    public static void commitVollaufbau(XSpreadsheetDocument xDoc, String schluessel,
            RanglisteEingabeSignatur signatur) {
        checkNotNull(xDoc);
        checkNotNull(schluessel);
        checkNotNull(signatur);
        SignaturErgebnis ergebnis = signatur.berechne(xDoc, 1);
        if (ergebnis instanceof SignaturErgebnis.Ok ok) {
            speichereNachRebuild(xDoc, schluessel, ok.hash(), "fullBuild");
        } else {
            logger.debug("commitVollaufbau: Hash nicht gespeichert für '{}' (Signatur != Ok): {}",
                    schluessel, ergebnis);
        }
    }

    public static void markiereRecoveryVersucht(XSpreadsheetDocument xDoc, String schluessel) {
        checkNotNull(xDoc);
        checkNotNull(schluessel);
        try {
            new DocumentPropertiesHelper(xDoc).setBooleanProperty(
                    propName(schluessel, SUFFIX_RECOVERY), true);
        } catch (RuntimeException e) {
            logger.warn("Recovery-Flag setzen fehlgeschlagen für '{}'", schluessel, e);
        }
    }
}
