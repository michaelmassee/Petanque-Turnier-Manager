/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.auftrag.versand;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.petanqueturniermanager.ptmonline.auftrag.SyncAuftrag;

/**
 * Sendet Schreibaufträge strikt in der Reihenfolge ihres Schreibzählers. Liest und schreibt kein Dokument – die
 * Aufträge sind fertig serialisiert, die Ergebnisse wendet der Dokument-Kontext an (T-09).
 * <p>
 * Ein Auftrag endet angenommen oder fachlich abgelehnt; beides ist endgültig und geht an den Ergebnis-Empfänger.
 * Netz-, Server- und Berechtigungsfehler sowie Bindungsprobleme beenden den Lauf, ohne den Auftrag zu verbrauchen: er
 * wird später mit derselben Auftrags-ID wiederholt, PTM-Online erkennt ihn dann wieder (T-23).
 */
public final class AuftragsVersand {

    private static final Logger logger = LogManager.getLogger(AuftragsVersand.class);

    private static final int HTTP_UNAUTHORIZED = 401;
    private static final int HTTP_FORBIDDEN = 403;
    private static final int HTTP_NOT_FOUND = 404;
    private static final int HTTP_GONE = 410;
    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private static final int HTTP_SERVER_ERROR = 500;
    private static final Set<String> BINDUNG_ABGELOEST = Set.of("document_replaced", "lease_invalid",
            "document_unbound");
    /** Fehlertext von PTM-Online, wenn es das Turnier nicht (mehr) gibt; unübersetzt, Teil des API-Vertrags. */
    private static final String TURNIER_NICHT_GEFUNDEN = "Turnier nicht gefunden";

    private AuftragsVersand() {}

    /**
     * @param auftraege zu sendende Aufträge; werden nach Schreibzähler sortiert gesendet
     * @param ergebnis  erhält jeden endgültig erledigten Auftrag, sofort nach seiner Antwort
     * @return {@link VersandStopp#FERTIG}, wenn alle Aufträge erledigt sind, sonst der Grund des Abbruchs
     */
    public static VersandStopp sende(List<SyncAuftrag> auftraege, AuftragsSender sender,
            Consumer<VersandErgebnis> ergebnis) throws InterruptedException {
        List<SyncAuftrag> sortiert = auftraege.stream().sorted(Comparator.comparingLong(SyncAuftrag::zaehler))
                .toList();
        for (SyncAuftrag auftrag : sortiert) {
            SyncAntwort antwort;
            try {
                antwort = sender.sende(auftrag);
            } catch (IOException e) {
                logger.info("PTM-Online: Auftrag {} ({}) nicht gesendet, PTM-Online nicht erreichbar",
                        auftrag.zaehler(), auftrag.art(), e);
                return VersandStopp.NETZ;
            }
            Optional<VersandStopp> stopp = stoppGrund(antwort);
            if (stopp.isPresent()) {
                logger.warn("PTM-Online: Versand bei Auftrag {} ({}) angehalten: {} – HTTP {} {}", auftrag.zaehler(),
                        auftrag.art(), stopp.get(), antwort.status(), antwort.body());
                return stopp.get();
            }
            if (!antwort.erfolgreich()) {
                logger.warn("PTM-Online: Auftrag {} ({}) abgelehnt – HTTP {} {}", auftrag.zaehler(), auftrag.art(),
                        antwort.status(), antwort.body());
            }
            ergebnis.accept(new VersandErgebnis(auftrag, antwort, antwort.erfolgreich()));
        }
        return VersandStopp.FERTIG;
    }

    /**
     * Antworten, nach denen weitere Aufträge sinnlos sind oder später gelingen können. Leer bei Erfolg und bei
     * fachlicher Ablehnung dieses einen Auftrags.
     */
    static Optional<VersandStopp> stoppGrund(SyncAntwort antwort) {
        if (antwort.erfolgreich()) {
            return Optional.empty();
        }
        Optional<String> code = antwort.code();
        if (code.filter("document_forked"::equals).isPresent()) {
            return Optional.of(VersandStopp.DOKUMENT_GEFORKT);
        }
        if (code.filter(BINDUNG_ABGELOEST::contains).isPresent()) {
            return Optional.of(VersandStopp.BINDUNG_ABGELOEST);
        }
        if (istTurnierGeloescht(antwort, code)) {
            return Optional.of(VersandStopp.TURNIER_GELOESCHT);
        }
        int status = antwort.status();
        if (status == HTTP_UNAUTHORIZED || status == HTTP_FORBIDDEN) {
            return Optional.of(VersandStopp.NICHT_BERECHTIGT);
        }
        if (status == HTTP_TOO_MANY_REQUESTS || status >= HTTP_SERVER_ERROR) {
            return Optional.of(VersandStopp.SERVERFEHLER);
        }
        return Optional.empty();
    }

    private static boolean istTurnierGeloescht(SyncAntwort antwort, Optional<String> code) {
        if (antwort.status() == HTTP_GONE || code.filter("tournament_deleted"::equals).isPresent()) {
            return true;
        }
        return antwort.status() == HTTP_NOT_FOUND
                && antwort.fehlertext().filter(TURNIER_NICHT_GEFUNDEN::equals).isPresent();
    }
}
