/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.onlinesync.sheet.PtmOnlineKonfliktSheet;
import de.petanqueturniermanager.ptmonline.auftrag.AuftragsBestand;
import de.petanqueturniermanager.ptmonline.dto.ErgebnismeldungDto;
import de.petanqueturniermanager.ptmonline.live.ErgebnisOrt;
import de.petanqueturniermanager.ptmonline.live.ErgebnisSchreiber;
import de.petanqueturniermanager.ptmonline.live.LivePartie;
import de.petanqueturniermanager.ptmonline.live.LiveRunde;
import de.petanqueturniermanager.ptmonline.live.LiveStandQuelle;
import de.petanqueturniermanager.ptmonline.live.LiveStandQuellen;
import de.petanqueturniermanager.ptmonline.live.LiveTurnierStand;

/**
 * Ergebnismeldungen von Spielern bei einem verbundenen Turnierdokument (E-27, E-28). Jede offene Meldung steht als
 * Fall „Ergebnis gemeldet“ in der Konfliktliste. Wählt die Turnierleitung „Ergebnis übernehmen“, trägt PTM das
 * Ergebnis in die Spielrunde ein; bei „Meldung verwerfen“ bleibt die Spielrunde unverändert. Beide Entscheidungen
 * gehen als Auftrag an PTM-Online. Eingetragen werden kann nur in zeilenbasierte Spielrunden (Schweizer, Formule X,
 * Maastrichter-Vorrunden, Supermêlée, Jeder gegen Jeden); bei anderen Partien bleibt nur Verwerfen.
 */
final class PtmOnlineErgebnismeldungen {

    /** Schlüsselanfang der Fälle in der Konfliktliste; der Block steht dort immer am Ende. */
    static final String SCHLUESSEL_PRAEFIX = KonfliktArt.ERGEBNIS_GEMELDET.name() + "|";

    private static final List<Entscheidung> OPTIONEN = List.of(Entscheidung.ERGEBNIS_UEBERNEHMEN,
            Entscheidung.ERGEBNIS_VERWERFEN);

    /** Bereits entschiedene Meldungen je Turnier, bis PTM-Online sie nicht mehr als offen liefert. */
    private static final Map<String, Set<String>> ENTSCHIEDEN = new ConcurrentHashMap<>();

    private PtmOnlineErgebnismeldungen() {}

    /**
     * Eine Meldung und die Partie im Dokument, zu der sie gehört.
     *
     * @param partie     Partie der Runde mit denselben Online-Anmeldungen, {@code null} wenn es keine gibt
     * @param vertauscht die Seiten der Partie stehen im Dokument andersherum als in der Meldung
     * @param nummernA   lokale Nummern der Seite A der Meldung (für die Anzeige, auch ohne Partie)
     * @param nummernB   wie {@code nummernA} für Seite B
     */
    record Zuordnung(ErgebnismeldungDto meldung, @Nullable LivePartie partie, boolean vertauscht,
            List<Integer> nummernA, List<Integer> nummernB) {

        Zuordnung {
            nummernA = List.copyOf(nummernA);
            nummernB = List.copyOf(nummernB);
        }

        /** Punkte für die linke Ergebniszelle der Partie. */
        int punkteA() {
            return vertauscht ? meldung.scoreB() : meldung.scoreA();
        }

        /** Punkte für die rechte Ergebniszelle der Partie. */
        int punkteB() {
            return vertauscht ? meldung.scoreA() : meldung.scoreB();
        }

        @Nullable
        ErgebnisOrt ort() {
            return partie == null ? null : partie.ergebnisOrt();
        }

        KonfliktFall fall() {
            List<Integer> links = partie == null ? nummernA : partie.teamA();
            List<Integer> rechts = partie == null ? nummernB : partie.teamB();
            String lokal = I18n.get("ptmonline.konflikt.ergebnis.partie", meldung.roundNumber(), seite(links),
                    seite(rechts));
            String online = I18n.get("ptmonline.konflikt.ergebnis.meldung", punkteA(), punkteB(), meldung.reportedBy());
            if (ort() == null) {
                return new KonfliktFall(KonfliktArt.ERGEBNIS_GEMELDET, null, List.of(meldung.id()), lokal, online,
                        I18n.get("ptmonline.konflikt.hinweis.ergebnis_partie_fehlt"),
                        List.of(Entscheidung.ERGEBNIS_VERWERFEN));
            }
            String hinweis = partie.punkteA() != null && partie.punkteB() != null
                    ? I18n.get("ptmonline.konflikt.hinweis.ergebnis_vorhanden", partie.punkteA() + ":" + partie.punkteB())
                    : I18n.get("ptmonline.konflikt.hinweis.ergebnis_gemeldet");
            return new KonfliktFall(KonfliktArt.ERGEBNIS_GEMELDET, null, List.of(meldung.id()), lokal, online, hinweis,
                    OPTIONEN);
        }

        private static String seite(List<Integer> nummern) {
            return nummern.isEmpty() ? "?"
                    : nummern.stream().map(String::valueOf).collect(Collectors.joining("+"));
        }
    }

    /**
     * Ordnet jede Meldung der Partie ihrer Runde zu, deren Seiten dieselben Online-Anmeldungen haben (in beliebiger
     * Seitenreihenfolge).
     *
     * @param onlineIds Online-IDs je lokaler Nummer, wie bei der Live-Übertragung
     */
    static List<Zuordnung> ordneZu(List<ErgebnismeldungDto> meldungen, LiveTurnierStand stand,
            Map<Integer, List<String>> onlineIds) {
        Map<String, Integer> nummerProOnlineId = new HashMap<>();
        onlineIds.forEach((nr, ids) -> ids.forEach(id -> nummerProOnlineId.putIfAbsent(id, nr)));
        List<Zuordnung> zuordnungen = new ArrayList<>();
        for (ErgebnismeldungDto meldung : meldungen) {
            Set<String> a = Set.copyOf(meldung.teamA());
            Set<String> b = Set.copyOf(meldung.teamB());
            LivePartie gefunden = null;
            boolean vertauscht = false;
            for (LivePartie partie : partienDerRunde(stand, meldung.roundNumber())) {
                Optional<Set<String>> seiteA = idsVon(partie.teamA(), onlineIds);
                Optional<Set<String>> seiteB = idsVon(partie.teamB(), onlineIds);
                if (seiteA.isEmpty() || seiteB.isEmpty() || partie.istFreilos()) {
                    continue;
                }
                if (seiteA.get().equals(a) && seiteB.get().equals(b)) {
                    gefunden = partie;
                    break;
                }
                if (seiteA.get().equals(b) && seiteB.get().equals(a)) {
                    gefunden = partie;
                    vertauscht = true;
                    break;
                }
            }
            zuordnungen.add(new Zuordnung(meldung, gefunden, vertauscht, nummern(meldung.teamA(), nummerProOnlineId),
                    nummern(meldung.teamB(), nummerProOnlineId)));
        }
        return zuordnungen;
    }

    private static List<LivePartie> partienDerRunde(LiveTurnierStand stand, int rundeNr) {
        return stand.runden().stream().filter(runde -> runde.nr() == rundeNr).findFirst().map(LiveRunde::partien)
                .orElse(List.of());
    }

    private static Optional<Set<String>> idsVon(List<Integer> nummern, Map<Integer, List<String>> onlineIds) {
        return LiveUebertragungsDaten.onlineIdsVon(nummern, onlineIds).map(Set::copyOf);
    }

    private static List<Integer> nummern(List<String> ids, Map<String, Integer> nummerProOnlineId) {
        return ids.stream().map(nummerProOnlineId::get).filter(Objects::nonNull).distinct().toList();
    }

    /**
     * Ergebnis des Anwendens.
     *
     * @param entscheidungen je Meldungs-ID {@code true} = übernommen, {@code false} = verworfen
     * @param offen          Fälle ohne Entscheidung, für die Konfliktliste
     */
    record Anwendung(Map<String, Boolean> entscheidungen, List<KonfliktFall> offen) {

        Anwendung {
            entscheidungen = Map.copyOf(entscheidungen);
            offen = List.copyOf(offen);
        }
    }

    /**
     * Wendet die eingetragenen Entscheidungen an. „Ergebnis übernehmen“ schreibt das Ergebnis in die Spielrunde; ist
     * das Blatt inzwischen verschwunden, bleibt der Fall offen. Nur im Dokument-Kontext eines SheetRunners.
     *
     * @param entscheidung  eingetragene Entscheidung je Fall
     * @param vorAenderung  öffnet den Blattschutz-Scope, bevor das Dokument geändert wird
     */
    static Anwendung wendeAn(WorkingSpreadsheet ws, List<Zuordnung> zuordnungen,
            Function<KonfliktFall, Optional<Entscheidung>> entscheidung, Runnable vorAenderung)
            throws GenerateException {
        Map<String, Boolean> entschieden = new LinkedHashMap<>();
        List<KonfliktFall> offen = new ArrayList<>();
        for (Zuordnung zuordnung : zuordnungen) {
            KonfliktFall fall = zuordnung.fall();
            Optional<Entscheidung> gewaehlt = entscheidung.apply(fall);
            ErgebnisOrt ort = zuordnung.ort();
            if (gewaehlt.filter(Entscheidung.ERGEBNIS_UEBERNEHMEN::equals).isPresent() && ort != null) {
                vorAenderung.run();
                if (ErgebnisSchreiber.schreibe(ws, ort, zuordnung.punkteA(), zuordnung.punkteB())) {
                    entschieden.put(zuordnung.meldung().id(), Boolean.TRUE);
                    continue;
                }
            } else if (gewaehlt.filter(Entscheidung.ERGEBNIS_VERWERFEN::equals).isPresent()) {
                entschieden.put(zuordnung.meldung().id(), Boolean.FALSE);
                continue;
            }
            offen.add(fall);
        }
        return new Anwendung(entschieden, offen);
    }

    /**
     * Verarbeitet die offenen Meldungen im Dokument: Entscheidungen anwenden, als Auftrag erfassen und die
     * verbleibenden Fälle liefern. Nur im Dokument-Kontext eines SheetRunners.
     *
     * @param meldungen    zuletzt von PTM-Online abgeholte offene Meldungen
     * @param entscheidung eingetragene Entscheidung je Fall
     * @param vorAenderung öffnet den Blattschutz-Scope, bevor das Dokument geändert wird
     * @return Fälle ohne Entscheidung
     */
    static List<KonfliktFall> verarbeite(WorkingSpreadsheet ws, TurnierSystem turnierSystem,
            PtmOnlineVerbindung verbindung, AuftragsBestand bestand, List<ErgebnismeldungDto> meldungen,
            Function<KonfliktFall, Optional<Entscheidung>> entscheidung, Runnable vorAenderung)
            throws GenerateException {
        String tournamentId = verbindung.tournamentId();
        List<ErgebnismeldungDto> offen = unentschieden(tournamentId, meldungen);
        if (offen.isEmpty()) {
            return List.of();
        }
        Optional<LiveStandQuelle> quelle = LiveStandQuellen.fuer(ws, turnierSystem, verbindung.spieltagNr());
        LiveTurnierStand stand = quelle.isPresent() ? quelle.get().lese() : LiveTurnierStand.ohneRangliste(List.of());
        Anwendung anwendung = wendeAn(ws, ordneZu(offen, stand, LiveNummernAufloesung.ermitteln(verbindung)),
                entscheidung, vorAenderung);
        PtmOnlineAuftraege.ergebnismeldungen(bestand, tournamentId, anwendung.entscheidungen());
        merkeEntschieden(tournamentId, anwendung.entscheidungen().keySet());
        return anwendung.offen();
    }

    /** Merkt entschiedene Meldungen, damit sie bis zur Annahme der Entscheidung nicht erneut als Fall erscheinen. */
    static void merkeEntschieden(String tournamentId, Collection<String> meldungsIds) {
        ENTSCHIEDEN.computeIfAbsent(tournamentId, id -> ConcurrentHashMap.newKeySet()).addAll(meldungsIds);
    }

    /**
     * Meldungen ohne bereits getroffene Entscheidung. Was PTM-Online nicht mehr als offen liefert, wird vergessen.
     */
    static List<ErgebnismeldungDto> unentschieden(String tournamentId, List<ErgebnismeldungDto> meldungen) {
        Set<String> entschieden = ENTSCHIEDEN.get(tournamentId);
        if (entschieden == null) {
            return meldungen;
        }
        Set<String> geliefert = meldungen.stream().map(ErgebnismeldungDto::id).collect(Collectors.toSet());
        entschieden.retainAll(geliefert);
        return meldungen.stream().filter(meldung -> !entschieden.contains(meldung.id())).toList();
    }

    /** Neue Bindung oder Turnier gelöscht: Entscheidungsgedächtnis verwerfen. */
    static void vergessen(String tournamentId) {
        ENTSCHIEDEN.remove(tournamentId);
    }

    /**
     * Bringt den Block „Ergebnis gemeldet“ am Ende der Konfliktliste auf den Stand der offenen Fälle; andere Fälle
     * bleiben unberührt. Eine eingetragene, noch nicht angewendete Wahl bleibt stehen. Ohne Änderung wird nichts
     * geschrieben.
     *
     * @param vorAenderung öffnet den Blattschutz-Scope, bevor das Dokument geändert wird
     * @return {@code true}, wenn der Block neu geschrieben wurde
     */
    static boolean aktualisiereKonfliktliste(PtmOnlineKonfliktSheet konfliktListe, List<KonfliktFall> offen,
            Runnable vorAenderung) throws GenerateException {
        List<PtmOnlineKonfliktSheet.Zeile> bisher = konfliktListe.leseBlock(SCHLUESSEL_PRAEFIX);
        Map<String, String> eingetragen = new HashMap<>();
        bisher.stream().filter(zeile -> !zeile.entscheidung().isEmpty())
                .forEach(zeile -> eingetragen.put(zeile.schluessel(), zeile.entscheidung()));
        KonfliktSammlung sammlung = KonfliktSammlung.mit(eingetragen);
        offen.forEach(sammlung::melde);
        List<PtmOnlineKonfliktSheet.Zeile> neu = sammlung.zeilen();
        if (inhalt(bisher).equals(inhalt(neu))) {
            return false;
        }
        vorAenderung.run();
        konfliktListe.ersetzeBlock(SCHLUESSEL_PRAEFIX, neu, RegistrationImportTask.zeitpunktText(Instant.now()));
        return true;
    }

    /** Vergleichbarer Inhalt ohne Optionen (die stehen nur in der Auswahlliste, nicht in den Zellen). */
    private static Set<List<String>> inhalt(List<PtmOnlineKonfliktSheet.Zeile> zeilen) {
        Set<List<String>> inhalt = new HashSet<>();
        zeilen.forEach(zeile -> inhalt.add(List.of(zeile.schluessel(), zeile.lokal(), zeile.online(), zeile.hinweis(),
                zeile.entscheidung())));
        return inhalt;
    }
}
