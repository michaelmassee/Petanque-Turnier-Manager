/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.uno.UnoRuntime;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.onlinesync.sheet.NeueZuordnung;
import de.petanqueturniermanager.ptmonline.auftrag.AuftragsArt;
import de.petanqueturniermanager.ptmonline.auftrag.AuftragsBestand;
import de.petanqueturniermanager.ptmonline.auftrag.SyncAuftrag;
import de.petanqueturniermanager.ptmonline.auftrag.versand.AuftragsVersand;
import de.petanqueturniermanager.ptmonline.auftrag.versand.VersandErgebnis;
import de.petanqueturniermanager.ptmonline.auftrag.versand.VersandStopp;
import de.petanqueturniermanager.ptmonline.dto.LiveMatchDto;
import de.petanqueturniermanager.ptmonline.dto.LiveRankingEntryDto;
import de.petanqueturniermanager.ptmonline.dto.NeueOnlineAnmeldung;
import de.petanqueturniermanager.ptmonline.dto.RegistrationDto;
import de.petanqueturniermanager.ptmonline.dto.RegistrationResultDto;

/**
 * Schreibaufträge einer Dokumentbindung an PTM-Online (T-09, T-19, T-23): erzeugen, Ergebnisse anwenden, synchron
 * senden. Erzeugen und Anwenden lesen und schreiben das Dokument und laufen daher nur im Dokument-Kontext
 * (SheetRunner); gesendet wird im Hintergrund ({@link PtmOnlineLiveBeobachter}) oder synchron im SheetRunner einer
 * Menüaktion. Beides hält die Versandsperre des Bestands, damit der Schreibzähler in Reihenfolge ankommt.
 * <p>
 * Je Dokument und Supermelee-Spieltag gibt es genau einen Bestand im Speicher; er wird beim ersten Zugriff aus dem
 * Blatt „PTMOnline Sync“ geladen und nach jeder Änderung dorthin zurückgeschrieben.
 */
public final class PtmOnlineAuftraege {

    private static final Logger logger = LogManager.getLogger(PtmOnlineAuftraege.class);
    private static final Map<String, AuftragsBestand> BESTAENDE = new ConcurrentHashMap<>();
    private static final String KONTEXT_UUID = "lokaleUuid";
    private static final String KONTEXT_NUMMER_FORMEL = "nummerFormel";
    private static final String KONTEXT_BEZEICHNUNG = "bezeichnung";
    private static final String KONTEXT_REVISIONEN = "revisionen";
    private static final String CODE_TURNIER_LAEUFT = "tournament_running";

    private PtmOnlineAuftraege() {}

    // ── Bestand je Bindung ──────────────────────────────────────────────────

    /** Bestand der Bindung; lädt ihn beim ersten Zugriff aus dem Blatt. Nur im Dokument-Kontext. */
    static AuftragsBestand bestand(WorkingSpreadsheet ws, PtmOnlineRegistrationMapping mapping,
            @Nullable Integer spieltagNr) throws GenerateException {
        String schluessel = schluessel(ws.getWorkingSpreadsheetDocument(), spieltagNr);
        AuftragsBestand bestand = BESTAENDE.get(schluessel);
        if (bestand != null) {
            return bestand;
        }
        AuftragsBestand geladen = mapping.leseAuftragsBestand();
        AuftragsBestand vorhanden = BESTAENDE.putIfAbsent(schluessel, geladen);
        return vorhanden == null ? geladen : vorhanden;
    }

    /** Bestand der Bindung, sofern im Speicher geladen – für den Hintergrund, der das Dokument nicht liest. */
    static Optional<AuftragsBestand> geladen(XSpreadsheetDocument xDoc, @Nullable Integer spieltagNr) {
        return Optional.ofNullable(BESTAENDE.get(schluessel(xDoc, spieltagNr)));
    }

    /**
     * Neue Bindung (Verbinden oder Übernehmen): offene Aufträge der alten Bindung werden verworfen, der Zähler
     * übernimmt den Stand des Servers. Speichert sofort.
     */
    public static void neueBindung(WorkingSpreadsheet ws, PtmOnlineRegistrationMapping mapping,
            @Nullable Integer spieltagNr, long serverZaehler, String grund) throws GenerateException {
        AuftragsBestand bestand = bestand(ws, mapping, spieltagNr);
        bestand.neueBindung(serverZaehler, grund);
        mapping.schreibeAuftragsBestand(bestand);
    }

    /** Vergisst alle Bestände eines geschlossenen Dokuments. */
    static void vergessen(XSpreadsheetDocument xDoc) {
        String praefix = UnoRuntime.generateOid(xDoc) + "|";
        BESTAENDE.keySet().removeIf(schluessel -> schluessel.startsWith(praefix));
    }

    private static String schluessel(XSpreadsheetDocument xDoc, @Nullable Integer spieltagNr) {
        return UnoRuntime.generateOid(xDoc) + "|" + (spieltagNr == null ? "" : spieltagNr);
    }

    /** Speichert den Bestand, falls er sich geändert hat. */
    static void speichern(AuftragsBestand bestand, PtmOnlineRegistrationMapping mapping) throws GenerateException {
        if (bestand.istGeaendert()) {
            mapping.schreibeAuftragsBestand(bestand);
        }
    }

    // ── Erzeugen ────────────────────────────────────────────────────────────

    /**
     * Lokaler Turnierstart (KP-05): ungesendete Aufträge der Anmeldephase werden verworfen und protokolliert, dann
     * folgt {@code running} mit dem nächsten Zähler.
     *
     * @return die verworfenen Aufträge
     */
    static List<SyncAuftrag> start(AuftragsBestand bestand, String tournamentId, Instant lokalerStart,
            String grundVerworfen) {
        List<SyncAuftrag> verworfen = bestand.verwerfe(AuftragsArt::wirdBeimStartVerworfen, grundVerworfen);
        bestand.erzeuge(AuftragsArt.START, "POST", TournamentSyncClient.startPfad(tournamentId),
                TournamentSyncClient.startBody(lokalerStart), "{}");
        return verworfen;
    }

    /**
     * Teilnahme und Setzposition zugeordneter Meldungen. Die erwartete Ausführungsrevision berücksichtigt noch offene
     * Aufträge derselben Meldung: gelingen sie, erhöht jeder die Revision um eins.
     *
     * @return leer, wenn keine der Meldungen online zugeordnet ist
     */
    static Optional<SyncAuftrag> teilnahme(AuftragsBestand bestand, String tournamentId,
            List<PtmOnlineStatusAuftrag.Eintrag> eintraege, Map<String, String> onlineIds,
            Map<String, Integer> revisionen) {
        Map<String, Integer> offeneRevisionen = offeneRevisionen(bestand);
        List<RegistrationResultDto> results = new ArrayList<>();
        JsonObject neueRevisionen = new JsonObject();
        for (PtmOnlineStatusAuftrag.Eintrag eintrag : eintraege) {
            String onlineId = onlineIds.get(eintrag.lokaleUuid());
            if (onlineId == null) {
                continue;
            }
            int erwartet = Math.max(revisionen.getOrDefault(eintrag.lokaleUuid(), 1),
                    offeneRevisionen.getOrDefault(eintrag.lokaleUuid(), 0));
            results.add(new RegistrationResultDto(onlineId, null, eintrag.seedingPosition(),
                    eintrag.teilnahme().apiWert(), erwartet));
            neueRevisionen.addProperty(eintrag.lokaleUuid(), erwartet + 1);
        }
        if (results.isEmpty()) {
            return Optional.empty();
        }
        JsonObject kontext = new JsonObject();
        kontext.add(KONTEXT_REVISIONEN, neueRevisionen);
        return Optional.of(bestand.erzeuge(AuftragsArt.TEILNAHME, "POST",
                TournamentSyncClient.ergebnissePfad(tournamentId), TournamentSyncClient.ergebnisseBody(results),
                kontext.toString()));
    }

    /** Online-Anlage einer lokalen Meldung samt ihrer Teilnahme und Setzposition (T-24). */
    static SyncAuftrag anlage(AuftragsBestand bestand, String tournamentId, String lokaleUuid,
            NeueOnlineAnmeldung anmeldung, OnlineTeilnahme teilnahme, @Nullable Integer setzposition,
            String nummerFormel, String bezeichnung) {
        JsonObject kontext = new JsonObject();
        kontext.addProperty(KONTEXT_UUID, lokaleUuid);
        kontext.addProperty(KONTEXT_NUMMER_FORMEL, nummerFormel);
        kontext.addProperty(KONTEXT_BEZEICHNUNG, bezeichnung);
        return bestand.erzeuge(AuftragsArt.ANMELDUNG_ANLEGEN, "PUT",
                TournamentSyncClient.anmeldungPfad(tournamentId, lokaleUuid),
                TournamentSyncClient.anlageBody(anmeldung, teilnahme.apiWert(), setzposition), kontext.toString());
    }

    /** Namenskorrektur einer zugeordneten Meldung aus dem Dokument; die lokale UUID wird dabei online vermerkt. */
    static SyncAuftrag aenderung(AuftragsBestand bestand, String tournamentId, String lokaleUuid,
            String onlineRegistrationId, NeueOnlineAnmeldung anmeldung, int erwarteteRevision, String bezeichnung) {
        JsonObject revisionen = new JsonObject();
        revisionen.addProperty(lokaleUuid, erwarteteRevision + 1);
        JsonObject kontext = new JsonObject();
        kontext.addProperty(KONTEXT_UUID, lokaleUuid);
        kontext.addProperty(KONTEXT_BEZEICHNUNG, bezeichnung);
        kontext.add(KONTEXT_REVISIONEN, revisionen);
        return bestand.erzeuge(AuftragsArt.ANMELDUNG_AENDERN, "PUT",
                TournamentSyncClient.anmeldungPfad(tournamentId, lokaleUuid),
                TournamentSyncClient.aenderungBody(anmeldung, onlineRegistrationId, erwarteteRevision),
                kontext.toString());
    }

    /** Spielrunde; ein noch ungesendeter älterer Stand derselben Runde entfällt. */
    static void runde(AuftragsBestand bestand, String tournamentId, int rundeNr, List<LiveMatchDto> matches) {
        String pfad = TournamentSyncClient.rundePfad(tournamentId, rundeNr);
        bestand.entferneUeberholte(auftrag -> auftrag.pfad().equals(pfad));
        bestand.erzeuge(AuftragsArt.RUNDE, "PUT", pfad, TournamentSyncClient.rundeBody(matches), "{}");
    }

    /** Löschen einer lokal nicht mehr vorhandenen Runde; ein ungesendeter Stand derselben Runde entfällt. */
    static void rundeLoeschen(AuftragsBestand bestand, String tournamentId, int rundeNr) {
        String pfad = TournamentSyncClient.rundePfad(tournamentId, rundeNr);
        bestand.entferneUeberholte(auftrag -> auftrag.pfad().equals(pfad));
        bestand.erzeuge(AuftragsArt.RUNDE_LOESCHEN, "DELETE", pfad, "", "{}");
    }

    /** Ranglisten-Snapshot; ein ungesendeter älterer Snapshot entfällt. */
    static void rangliste(AuftragsBestand bestand, String tournamentId, List<LiveRankingEntryDto> eintraege) {
        String pfad = TournamentSyncClient.ranglistePfad(tournamentId);
        bestand.entferneUeberholte(auftrag -> auftrag.pfad().equals(pfad));
        bestand.erzeuge(AuftragsArt.RANGLISTE, "PUT", pfad, TournamentSyncClient.ranglisteBody(eintraege), "{}");
    }

    /** Trennen: offene Aufträge sind danach sinnlos und werden verworfen. */
    static SyncAuftrag trennen(AuftragsBestand bestand, String tournamentId, String grundVerworfen) {
        bestand.verwerfe(art -> true, grundVerworfen);
        return bestand.erzeuge(AuftragsArt.TRENNEN, "POST", TournamentSyncClient.trennenPfad(tournamentId), "{}",
                "{}");
    }

    /** Höchste erwartete Revision je UUID nach allen offenen Teilnahme- und Änderungsaufträgen. */
    private static Map<String, Integer> offeneRevisionen(AuftragsBestand bestand) {
        Map<String, Integer> ergebnis = new LinkedHashMap<>();
        for (AuftragsBestand.Eintrag eintrag : bestand.eintraege()) {
            if (eintrag.zustand() != AuftragsBestand.Zustand.OFFEN) {
                continue;
            }
            revisionenAusKontext(eintrag.auftrag())
                    .forEach((uuid, revision) -> ergebnis.merge(uuid, revision, Math::max));
        }
        return ergebnis;
    }

    // ── Ergebnisse anwenden ─────────────────────────────────────────────────

    /**
     * Was beim Anwenden für die Turnierleitung wichtig ist.
     *
     * @param angelegt      Anzahl online angelegter Meldungen
     * @param abgelehnt     Bezeichnungen lokaler Meldungen, die PTM-Online als bereits angemeldet abgelehnt hat
     * @param nachStart     Bezeichnungen lokaler Meldungen, die wegen des Turnierstarts nicht mehr angelegt wurden
     * @param geaendert     Online-Stand nach einer Namenskorrektur je lokaler UUID
     * @param abgelehnteAuftraege alle fachlich abgelehnten Aufträge samt Antwort
     */
    record Anwendung(int angelegt, List<String> abgelehnt, List<String> nachStart,
            Map<String, RegistrationDto> geaendert, List<VersandErgebnis> abgelehnteAuftraege) {

        Anwendung {
            abgelehnt = List.copyOf(abgelehnt);
            nachStart = List.copyOf(nachStart);
            geaendert = Map.copyOf(geaendert);
            abgelehnteAuftraege = List.copyOf(abgelehnteAuftraege);
        }

        /** Ablehnung eines Auftrags dieser Art, falls einer abgelehnt wurde. */
        Optional<VersandErgebnis> abgelehnt(AuftragsArt art) {
            return abgelehnteAuftraege.stream().filter(ergebnis -> ergebnis.auftrag().art() == art).findFirst();
        }
    }

    /**
     * Wendet die Ergebnisse gesendeter Aufträge im Dokument an (Zuordnungen, Revisionen, ausstehender Start) und nimmt
     * die Aufträge aus dem Puffer. Nur im Dokument-Kontext.
     */
    static Anwendung anwenden(AuftragsBestand bestand, PtmOnlineRegistrationMapping mapping)
            throws GenerateException {
        List<VersandErgebnis> ergebnisse = bestand.ergebnisseZumAnwenden();
        List<NeueZuordnung> zuordnungen = new ArrayList<>();
        Map<String, Integer> revisionen = new LinkedHashMap<>();
        Map<String, RegistrationDto> geaendert = new LinkedHashMap<>();
        List<String> abgelehnt = new ArrayList<>();
        List<String> nachStart = new ArrayList<>();
        boolean startErledigt = false;
        for (VersandErgebnis ergebnis : ergebnisse) {
            SyncAuftrag auftrag = ergebnis.auftrag();
            JsonObject kontext = kontext(auftrag);
            // Auch ein abgelehnter Start ist erledigt: er würde sonst bei jedem Durchlauf neu erzeugt.
            startErledigt |= auftrag.art() == AuftragsArt.START;
            if (!ergebnis.angenommen()) {
                if (auftrag.art() == AuftragsArt.ANMELDUNG_ANLEGEN) {
                    String bezeichnung = text(kontext, KONTEXT_BEZEICHNUNG);
                    if (ergebnis.antwort().nenntFeld()) {
                        abgelehnt.add(bezeichnung);
                    } else if (ergebnis.antwort().code().filter(CODE_TURNIER_LAEUFT::equals).isPresent()) {
                        nachStart.add(bezeichnung);
                    }
                }
                continue;
            }
            try {
                switch (auftrag.art()) {
                    case ANMELDUNG_ANLEGEN -> zuordnungen.add(PtmOnlineRegistrationMapping.neueZuordnung(
                            text(kontext, KONTEXT_UUID), text(kontext, KONTEXT_NUMMER_FORMEL),
                            text(kontext, KONTEXT_BEZEICHNUNG),
                            TournamentSyncClient.registrationAus(ergebnis.antwort())));
                    case ANMELDUNG_AENDERN -> {
                        RegistrationDto registration = TournamentSyncClient.registrationAus(ergebnis.antwort());
                        geaendert.put(text(kontext, KONTEXT_UUID), registration);
                        revisionen.put(text(kontext, KONTEXT_UUID), Objects.requireNonNullElse(registration.executionRevision(), 1));
                    }
                    case TEILNAHME -> {
                        Map<String, Integer> erwartet = revisionenAusKontext(auftrag);
                        if (TournamentSyncClient.zahlAus(ergebnis.antwort(), "updatedCount") == erwartet.size()) {
                            revisionen.putAll(erwartet);
                        } else {
                            logger.warn("PTM-Online: Teilnahme-Auftrag {} aktualisierte nicht alle Meldungen; "
                                    + "Revisionen bleiben für den nächsten Abgleich", auftrag.zaehler());
                        }
                    }
                    case START, TRENNEN, RUNDE, RUNDE_LOESCHEN, RANGLISTE -> {
                        // Nichts im Dokument festzuhalten.
                    }
                }
            } catch (IOException e) {
                logger.error("PTM-Online: Antwort auf Auftrag {} ({}) nicht auswertbar", auftrag.zaehler(),
                        auftrag.art(), e);
            }
        }
        if (!zuordnungen.isEmpty()) {
            mapping.addMappings(zuordnungen);
        }
        if (!revisionen.isEmpty()) {
            mapping.setExecutionRevisionen(revisionen);
        }
        if (startErledigt) {
            mapping.setRunningAusstehendSeit(null);
        }
        for (VersandErgebnis ergebnis : ergebnisse) {
            bestand.angewendet(ergebnis, ablehnungsGrund(ergebnis));
        }
        speichern(bestand, mapping);
        return new Anwendung(zuordnungen.size(), abgelehnt, nachStart, geaendert,
                ergebnisse.stream().filter(ergebnis -> !ergebnis.angenommen()).toList());
    }

    private static String ablehnungsGrund(VersandErgebnis ergebnis) {
        if (ergebnis.angenommen()) {
            return "";
        }
        return ergebnis.antwort().code()
                .or(() -> ergebnis.antwort().fehlertext())
                .orElse("HTTP " + ergebnis.antwort().status());
    }

    // ── Synchroner Versand (Menüaktionen im SheetRunner) ─────────────────────

    /**
     * Speichert den Bestand, sendet alle offenen Aufträge in Zählerreihenfolge – auch ältere aus dem Hintergrund, die
     * noch ausstehen – und wendet die Ergebnisse an. Hält währenddessen die Versandsperre.
     *
     * @return Grund des Versandendes und die angewendeten Ergebnisse
     */
    static SynchronerVersand sendeSynchron(AuftragsBestand bestand, PtmOnlineRegistrationMapping mapping,
            TournamentSyncClient client, boolean pausiert) throws GenerateException, InterruptedException {
        speichern(bestand, mapping);
        VersandStopp stopp;
        bestand.versandSperre().lockInterruptibly();
        try {
            stopp = AuftragsVersand.sende(bestand.zuSenden(pausiert), client, bestand::gesendet);
        } finally {
            bestand.versandSperre().unlock();
        }
        return new SynchronerVersand(stopp, anwenden(bestand, mapping));
    }

    /** Ergebnis von {@link #sendeSynchron}. */
    record SynchronerVersand(VersandStopp stopp, Anwendung anwendung) {
    }

    // ── Kontext ─────────────────────────────────────────────────────────────

    private static JsonObject kontext(SyncAuftrag auftrag) {
        try {
            JsonElement element = JsonParser.parseString(auftrag.kontext());
            return element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
        } catch (JsonParseException e) {
            logger.warn("PTM-Online: Kontext von Auftrag {} unlesbar", auftrag.zaehler(), e);
            return new JsonObject();
        }
    }

    private static Map<String, Integer> revisionenAusKontext(SyncAuftrag auftrag) {
        Map<String, Integer> ergebnis = new LinkedHashMap<>();
        JsonElement revisionen = kontext(auftrag).get(KONTEXT_REVISIONEN);
        if (revisionen != null && revisionen.isJsonObject()) {
            revisionen.getAsJsonObject().entrySet().stream()
                    .filter(eintrag -> eintrag.getValue().isJsonPrimitive())
                    .forEach(eintrag -> ergebnis.put(eintrag.getKey(), eintrag.getValue().getAsInt()));
        }
        return ergebnis;
    }

    private static String text(JsonObject kontext, String feld) {
        JsonElement wert = kontext.get(feld);
        return wert != null && wert.isJsonPrimitive() ? wert.getAsString() : "";
    }
}
