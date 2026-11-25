/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.uno.UnoRuntime;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.onlinesync.speicher.NeueZuordnung;
import de.petanqueturniermanager.onlinesync.speicher.ZuordnungsZusatz;
import de.petanqueturniermanager.ptmonline.auftrag.AuftragsArt;
import de.petanqueturniermanager.ptmonline.auftrag.AuftragsBestand;
import de.petanqueturniermanager.ptmonline.auftrag.SyncAuftrag;
import de.petanqueturniermanager.ptmonline.auftrag.versand.AuftragsVersand;
import de.petanqueturniermanager.ptmonline.auftrag.versand.SyncAntwort;
import de.petanqueturniermanager.ptmonline.auftrag.versand.VersandErgebnis;
import de.petanqueturniermanager.ptmonline.auftrag.versand.VersandStopp;
import de.petanqueturniermanager.ptmonline.dto.LiveMatchDto;
import de.petanqueturniermanager.ptmonline.dto.LiveKoWinnerDto;
import de.petanqueturniermanager.ptmonline.dto.LiveRankingEntryDto;
import de.petanqueturniermanager.ptmonline.dto.MeleeTeamDto;
import de.petanqueturniermanager.ptmonline.dto.NeueOnlineAnmeldung;
import de.petanqueturniermanager.ptmonline.dto.PersonDto;
import de.petanqueturniermanager.ptmonline.dto.RegistrationDto;
import de.petanqueturniermanager.ptmonline.dto.RegistrationResultDto;
import de.petanqueturniermanager.ptmonline.dto.SpieleinstellungenDto;

/**
 * Schreibaufträge einer Dokumentbindung an PTM-Online (T-09, T-19, T-23): erzeugen, Ergebnisse anwenden, synchron
 * senden. Erzeugen und Anwenden lesen und schreiben das Dokument und laufen daher nur im Dokument-Kontext
 * (SheetRunner); gesendet wird im Hintergrund ({@link PtmOnlineLiveBeobachter}) oder synchron im SheetRunner einer
 * Menüaktion. Beides hält die Versandsperre des Bestands, damit der Schreibzähler in Reihenfolge ankommt.
 * <p>
 * Je Dokument und Supermelee-Spieltag gibt es genau einen Bestand im Speicher; er wird beim ersten Zugriff aus dem
 * gespeicherten Sync-Zustand (Dokument-Properties) geladen und nach jeder Änderung dorthin zurückgeschrieben.
 */
public final class PtmOnlineAuftraege {

    private static final Logger logger = LogManager.getLogger(PtmOnlineAuftraege.class);
    private static final Map<String, AuftragsBestand> BESTAENDE = new ConcurrentHashMap<>();
    private static final String KONTEXT_UUID = "lokaleUuid";
    private static final String KONTEXT_BEZEICHNUNG = "bezeichnung";
    private static final String KONTEXT_REVISIONEN = "revisionen";
    private static final String KONTEXT_TIMER_STAND = "timerStand";
    /** Kürzere Verzögerungen bleiben unberücksichtigt; sonst entstünde fast jeder Timer-Auftrag zweimal. */
    static final Duration TIMER_NACHSTELLEN_AB = Duration.ofSeconds(2);
    private static final String CODE_TURNIER_LAEUFT = "tournament_running";
    /** Status, den PTM-Online nach angenommenem Turnierstart meldet. */
    private static final String ONLINE_STATUS_LAEUFT = "running";
    /** Status, den PTM-Online nach angenommenem Turnierabschluss meldet. */
    private static final String ONLINE_STATUS_ABGESCHLOSSEN = "finished";
    private static final String KONTEXT_ONLINE_ID = "onlineId";
    private static final String KONTEXT_VERKNUEPFUNGEN = "verknuepfungen";
    /** Gesendete Besetzung einer Änderung; nach der Annahme die zuletzt abgeglichene. */
    private static final String KONTEXT_BESETZUNG = "besetzung";
    /** Gesendeter Teamname einer Änderung; nach der Annahme der zuletzt abgeglichene. */
    private static final String KONTEXT_TEAMNAME = "teamname";
    private static final String CODE_AUSFUEHRUNGSKONFLIKT = "execution_conflict";
    private static final String CODE_GLEICHE_ANMELDUNG = "possible_same_registration";
    private static final String BODY_ONLINE_ID = "onlineRegistrationId";
    private static final String BODY_REGISTRATIONS = "registrations";
    private static final String BODY_ERWARTETE_REVISION = "expectedExecutionRevision";
    private static final int MAX_ENTSCHEIDUNGEN_JE_AUFTRAG = 200;
    private static final int MAX_VERKNUEPFUNGEN_JE_AUFTRAG = 200;

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

    /** Alle geladenen Bestände eines Dokuments, je Bindung (Supermêlée: je Spieltag) einer. */
    static List<AuftragsBestand> geladene(XSpreadsheetDocument xDoc) {
        String praefix = UnoRuntime.generateOid(xDoc) + "|";
        return BESTAENDE.entrySet().stream().filter(eintrag -> eintrag.getKey().startsWith(praefix))
                .map(Map.Entry::getValue).toList();
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
        erzeugeStart(bestand, tournamentId, lokalerStart);
        return verworfen;
    }

    /**
     * Turnierstart bei pausiertem Sync, den die Turnierleitung ausdrücklich bestätigt hat (P-58): wie
     * {@link #start}, aber {@code running} wird vor ältere ungesendete Aufträge gezogen (z.&nbsp;B. Spieleinstellungen
     * nach einem Netzfehler), damit es in der Pause allein hinausgeht. Die übrigen bleiben bis zum Fortsetzen offen.
     *
     * @return die verworfenen Aufträge
     */
    static List<SyncAuftrag> startTrotzPause(AuftragsBestand bestand, String tournamentId, Instant lokalerStart,
            String grundVerworfen) {
        List<SyncAuftrag> verworfen = bestand.verwerfe(AuftragsArt::wirdBeimStartVerworfen, grundVerworfen);
        bestand.zieheVor(erzeugeStart(bestand, tournamentId, lokalerStart).auftragsId());
        return verworfen;
    }

    private static SyncAuftrag erzeugeStart(AuftragsBestand bestand, String tournamentId, Instant lokalerStart) {
        return bestand.erzeuge(AuftragsArt.START, "POST", TournamentSyncClient.startPfad(tournamentId),
                TournamentSyncClient.startBody(lokalerStart), "{}");
    }

    /**
     * Teilnahme, Setzposition und Teamnummer zugeordneter Meldungen. Die erwartete Ausführungsrevision berücksichtigt noch offene
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
                    eintrag.teilnahme().apiWert(), eintrag.teamNummer(), erwartet));
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

    /**
     * Online-Anlage einer lokalen Meldung samt ihrer Teilnahme und Setzposition (T-24).
     *
     * @param automatisch Anlage durch den automatischen Meldelisten-Abgleich: PTM-Online legt keine Dublette einer
     *                    gleichnamigen, noch nicht verknüpften Online-Anmeldung an
     */
    static SyncAuftrag anlage(AuftragsBestand bestand, String tournamentId, String lokaleUuid,
            NeueOnlineAnmeldung anmeldung, OnlineTeilnahme teilnahme, @Nullable Integer setzposition,
            String bezeichnung, boolean automatisch) {
        JsonObject kontext = new JsonObject();
        kontext.addProperty(KONTEXT_UUID, lokaleUuid);
        kontext.addProperty(KONTEXT_BEZEICHNUNG, bezeichnung);
        return bestand.erzeuge(AuftragsArt.ANMELDUNG_ANLEGEN, "PUT",
                TournamentSyncClient.anmeldungPfad(tournamentId, lokaleUuid),
                TournamentSyncClient.anlageBody(anmeldung, teilnahme.apiWert(), setzposition, automatisch),
                kontext.toString());
    }

    /**
     * Namenskorrektur einer zugeordneten Meldung aus dem Dokument; die lokale UUID wird dabei online vermerkt. Nach der
     * Annahme gelten die gesendete Besetzung und der gesendete Teamname als zuletzt abgeglichen.
     *
     * @param besetzung gesendete Besetzung samt Benutzer-IDs (bei „Nur Teamname“ der Teamname)
     */
    static SyncAuftrag aenderung(AuftragsBestand bestand, String tournamentId, String lokaleUuid,
            String onlineRegistrationId, NeueOnlineAnmeldung anmeldung, List<PersonDto> personen,
            AbgeglicheneBesetzung besetzung, int erwarteteRevision, String bezeichnung) {
        JsonObject revisionen = new JsonObject();
        revisionen.addProperty(lokaleUuid, erwarteteRevision + 1);
        JsonObject kontext = new JsonObject();
        kontext.addProperty(KONTEXT_UUID, lokaleUuid);
        kontext.addProperty(KONTEXT_ONLINE_ID, onlineRegistrationId);
        kontext.addProperty(KONTEXT_BEZEICHNUNG, bezeichnung);
        kontext.add(KONTEXT_REVISIONEN, revisionen);
        kontext.addProperty(KONTEXT_BESETZUNG, besetzung.alsText());
        kontext.addProperty(KONTEXT_TEAMNAME, AbgeglichenerTeamname.alsText(anmeldung.teamName()));
        return bestand.erzeuge(AuftragsArt.ANMELDUNG_AENDERN, "PUT",
                TournamentSyncClient.anmeldungPfad(tournamentId, lokaleUuid),
                TournamentSyncClient.aenderungBody(anmeldung, personen, onlineRegistrationId, erwarteteRevision),
                kontext.toString());
    }

    /**
     * Verknüpft zugeordnete Online-Anmeldungen mit ihrer lokalen UUID (T-21), damit PTM-Online die Zuordnung kennt –
     * auch nach dem Löschen der lokalen Zeile, wenn die Nr-Zelle mit der Online-ID fehlt. Paare, die schon ein
     * offener Auftrag trägt, entfallen; PTM-Online nimmt höchstens 200 je Aufruf an.
     *
     * @param onlineIdProUuid Online-ID je lokaler UUID
     * @return ob ein Auftrag entstanden ist
     */
    static boolean verknuepfungen(AuftragsBestand bestand, String tournamentId, Map<String, String> onlineIdProUuid) {
        Map<String, String> offen = offeneVerknuepfungen(bestand);
        List<Map.Entry<String, String>> neu = onlineIdProUuid.entrySet().stream()
                .filter(paar -> !paar.getValue().equals(offen.get(paar.getKey()))).toList();
        for (int start = 0; start < neu.size(); start += MAX_VERKNUEPFUNGEN_JE_AUFTRAG) {
            Map<String, String> teil = new LinkedHashMap<>();
            neu.subList(start, Math.min(neu.size(), start + MAX_VERKNUEPFUNGEN_JE_AUFTRAG))
                    .forEach(paar -> teil.put(paar.getKey(), paar.getValue()));
            JsonObject paare = new JsonObject();
            teil.forEach(paare::addProperty);
            JsonObject kontext = new JsonObject();
            kontext.add(KONTEXT_VERKNUEPFUNGEN, paare);
            bestand.erzeuge(AuftragsArt.ANMELDUNG_VERKNUEPFEN, "POST",
                    TournamentSyncClient.verknuepfungenPfad(tournamentId),
                    TournamentSyncClient.verknuepfungenBody(teil), kontext.toString());
        }
        return !neu.isEmpty();
    }

    /**
     * Online-ID je lokaler UUID aus offenen Verknüpfungs- und Änderungsaufträgen: Solange sie nicht angenommen sind,
     * kennt PTM-Online die lokale UUID womöglich noch nicht, der Auftrag aber schon.
     */
    static Map<String, String> offeneVerknuepfungen(AuftragsBestand bestand) {
        Map<String, String> onlineIdProUuid = new LinkedHashMap<>();
        for (AuftragsBestand.Eintrag eintrag : bestand.eintraege()) {
            if (eintrag.zustand() != AuftragsBestand.Zustand.OFFEN) {
                continue;
            }
            JsonObject kontext = kontext(eintrag.auftrag());
            switch (eintrag.auftrag().art()) {
                case ANMELDUNG_VERKNUEPFEN -> {
                    JsonElement paare = kontext.get(KONTEXT_VERKNUEPFUNGEN);
                    if (paare != null && paare.isJsonObject()) {
                        paare.getAsJsonObject().entrySet()
                                .forEach(paar -> onlineIdProUuid.put(paar.getKey(), paar.getValue().getAsString()));
                    }
                }
                case ANMELDUNG_AENDERN -> {
                    String uuid = text(kontext, KONTEXT_UUID);
                    String onlineId = text(kontext, KONTEXT_ONLINE_ID);
                    if (!uuid.isEmpty() && !onlineId.isEmpty()) {
                        onlineIdProUuid.put(uuid, onlineId);
                    }
                }
                default -> {
                    // Andere Aufträge tragen keine Zuordnung.
                }
            }
        }
        return onlineIdProUuid;
    }

    /** Spielrunde; ein noch ungesendeter älterer Stand derselben Runde entfällt. */
    static void runde(AuftragsBestand bestand, String tournamentId, int rundeNr, List<LiveMatchDto> matches) {
        String pfad = TournamentSyncClient.rundePfad(tournamentId, rundeNr);
        bestand.entferneUeberholte(auftrag -> auftrag.pfad().equals(pfad));
        bestand.erzeuge(AuftragsArt.RUNDE, "PUT", pfad, TournamentSyncClient.rundeBody(matches), "{}");
        abschlussZuletzt(bestand);
    }

    /** Löschen einer lokal nicht mehr vorhandenen Runde; ein ungesendeter Stand derselben Runde entfällt. */
    static void rundeLoeschen(AuftragsBestand bestand, String tournamentId, int rundeNr) {
        String pfad = TournamentSyncClient.rundePfad(tournamentId, rundeNr);
        bestand.entferneUeberholte(auftrag -> auftrag.pfad().equals(pfad));
        bestand.erzeuge(AuftragsArt.RUNDE_LOESCHEN, "DELETE", pfad, "", "{}");
        abschlussZuletzt(bestand);
    }

    /**
     * Ranglisten-Snapshot samt KO-Siegern (zweiter Teil der Maastrichter-Endrangliste); ein ungesendeter älterer
     * Snapshot entfällt.
     */
    static void rangliste(AuftragsBestand bestand, String tournamentId, List<LiveRankingEntryDto> eintraege,
            List<LiveKoWinnerDto> koSieger) {
        String pfad = TournamentSyncClient.ranglistePfad(tournamentId);
        bestand.entferneUeberholte(auftrag -> auftrag.pfad().equals(pfad));
        bestand.erzeuge(AuftragsArt.RANGLISTE, "PUT", pfad, TournamentSyncClient.ranglisteBody(eintraege, koSieger),
                "{}");
        abschlussZuletzt(bestand);
    }

    /**
     * Ein wartender Abschluss friert online Endrangliste und Ergebnisse ein und muss daher nach jeder später erfassten
     * Runde oder Rangliste gesendet werden – sonst veröffentlicht er einen veralteten Zwischenstand, den der danach
     * gesendete Stand nicht mehr korrigiert. War er schon unterwegs (Antwort verloren), friert ihn die Wiederholung
     * unter neuer Auftrags-ID neu ein; PTM-Online benachrichtigt dabei niemanden ein zweites Mal.
     */
    private static void abschlussZuletzt(AuftragsBestand bestand) {
        bestand.wiederholeAmEnde(AuftragsArt.ABSCHLUSS::equals);
    }

    /** Spieleinstellungen des Dokuments; ein ungesendeter älterer Stand entfällt. */
    static void spieleinstellungen(AuftragsBestand bestand, String tournamentId,
            SpieleinstellungenDto einstellungen) {
        String pfad = TournamentSyncClient.spieleinstellungenPfad(tournamentId);
        bestand.entferneUeberholte(auftrag -> auftrag.pfad().equals(pfad));
        bestand.erzeuge(AuftragsArt.SPIELEINSTELLUNGEN, "PUT", pfad,
                TournamentSyncClient.spieleinstellungenBody(einstellungen), "{}");
    }

    /**
     * Mêlée-Teamzuordnung (KP-18); ersetzt online die vorige vollständig, ein ungesendeter älterer Stand entfällt.
     * Ohne Team mit zugeordneter Anmeldung entsteht kein Auftrag.
     */
    static void meleeTeams(AuftragsBestand bestand, String tournamentId, List<MeleeTeamDto> teams) {
        if (teams.isEmpty()) {
            return;
        }
        String pfad = TournamentSyncClient.meleeTeamsPfad(tournamentId);
        bestand.entferneUeberholte(auftrag -> auftrag.pfad().equals(pfad));
        bestand.erzeuge(AuftragsArt.MELEE_TEAMS, "PUT", pfad, TournamentSyncClient.meleeTeamsBody(teams), "{}");
    }

    /**
     * Entscheidungen der Turnierleitung über Ergebnismeldungen von Spielern (E-27); wird ohne Netz nachgeholt.
     * PTM-Online nimmt höchstens 200 je Aufruf an, mehr verteilen sich auf
     * mehrere Aufträge.
     *
     * @param uebernommenProMeldung je Meldungs-ID {@code true} = übernommen, {@code false} = verworfen
     */
    static void ergebnismeldungen(AuftragsBestand bestand, String tournamentId,
            Map<String, Boolean> uebernommenProMeldung) {
        List<Map.Entry<String, Boolean>> alle = List.copyOf(uebernommenProMeldung.entrySet());
        for (int start = 0; start < alle.size(); start += MAX_ENTSCHEIDUNGEN_JE_AUFTRAG) {
            Map<String, Boolean> teil = new LinkedHashMap<>();
            alle.subList(start, Math.min(alle.size(), start + MAX_ENTSCHEIDUNGEN_JE_AUFTRAG))
                    .forEach(eintrag -> teil.put(eintrag.getKey(), eintrag.getValue()));
            bestand.erzeuge(AuftragsArt.ERGEBNISMELDUNGEN, "POST",
                    TournamentSyncClient.ergebnismeldungEntscheidungenPfad(tournamentId),
                    TournamentSyncClient.ergebnismeldungEntscheidungenBody(teil), "{}");
        }
    }

    /**
     * IDs der Ergebnismeldungen, deren Entscheidung als offener Auftrag auf den Versand oder die Antwort wartet. Der
     * Auftragspuffer wird mit dem Dokument gespeichert; so bleibt eine Entscheidung auch nach einem Neustart gültig,
     * wenn PTM-Online die Meldung bis dahin noch als offen liefert.
     */
    static Set<String> beauftragteErgebnismeldungen(AuftragsBestand bestand) {
        Set<String> ids = new LinkedHashSet<>();
        for (AuftragsBestand.Eintrag eintrag : bestand.eintraege()) {
            if (eintrag.zustand() != AuftragsBestand.Zustand.OFFEN
                    || eintrag.auftrag().art() != AuftragsArt.ERGEBNISMELDUNGEN) {
                continue;
            }
            JsonElement entscheidungen = json(eintrag.auftrag().body(), eintrag.auftrag()).get("decisions");
            if (entscheidungen != null && entscheidungen.isJsonArray()) {
                entscheidungen.getAsJsonArray().forEach(entscheidung -> {
                    String id = entscheidung.isJsonObject() ? text(entscheidung.getAsJsonObject(), "id") : "";
                    if (!id.isEmpty()) {
                        ids.add(id);
                    }
                });
            }
        }
        return ids;
    }

    /**
     * Online-Stornierung einer lokal entfernten, verknüpften Meldung (KP-15); wird ohne Netz nachgeholt. Nach der
     * Annahme ist die Zuordnung als „lokal entfernt“ vermerkt.
     */
    static void storno(AuftragsBestand bestand, String tournamentId, String lokaleUuid, String onlineRegistrationId,
            int erwarteteRevision) {
        JsonObject kontext = new JsonObject();
        kontext.addProperty(KONTEXT_UUID, lokaleUuid);
        bestand.erzeuge(AuftragsArt.ANMELDUNG_STORNIEREN, "PUT", TournamentSyncClient.anmeldungPfad(tournamentId,
                lokaleUuid), TournamentSyncClient.stornoBody(onlineRegistrationId, erwarteteRevision),
                kontext.toString());
    }

    /**
     * Nimmt nie gesendete Stornierungen von Meldungen zurück, deren Zeile wieder in der Meldeliste steht (Löschen
     * rückgängig gemacht, ausgeschnitten und wieder eingefügt). Ein Storno, der PTM-Online erreicht haben kann (Antwort
     * ausgeblieben, vor dem Laden offen), geht dagegen hinaus: Nur so erfährt das Dokument, ob online storniert ist.
     * Hält dazu kurz die Versandsperre, damit kein Storno zwischen Prüfen und Entfernen hinausgeht; sendet gerade der
     * Hintergrund, bleibt es beim nächsten Lauf.
     *
     * @param vorhandeneUuids lokale UUIDs aller Zeilen der Meldeliste
     * @return {@code false}, wenn ein zurückzunehmender Storno wegen eines laufenden Versands noch aussteht
     */
    static boolean stornosZuruecknehmen(AuftragsBestand bestand, Set<String> vorhandeneUuids) {
        Predicate<SyncAuftrag> zurueckgekehrt = auftrag -> auftrag.art() == AuftragsArt.ANMELDUNG_STORNIEREN
                && vorhandeneUuids.contains(text(kontext(auftrag), KONTEXT_UUID));
        if (!bestand.versandSperre().tryLock()) {
            return bestand.nieGesendete().stream().noneMatch(zurueckgekehrt);
        }
        try {
            List<SyncAuftrag> zurueck = bestand.nieGesendete().stream().filter(zurueckgekehrt).toList();
            if (!zurueck.isEmpty()) {
                bestand.entferneUeberholte(zurueck::contains);
                logger.info("PTM-Online: {} Stornierungen zurückgenommen, die Zeilen stehen wieder in der Meldeliste",
                        zurueck.size());
            }
            return true;
        } finally {
            bestand.versandSperre().unlock();
        }
    }

    /** Ob das Online-Turnier laut Sync-Zustand läuft oder abgeschlossen ist; danach lehnt PTM-Online Anlagen ab. */
    static boolean istGestartet(Optional<String> onlineStatus) {
        return onlineStatus.filter(status -> ONLINE_STATUS_LAEUFT.equals(status)
                || ONLINE_STATUS_ABGESCHLOSSEN.equals(status)).isPresent();
    }

    /**
     * Erwartete Ausführungsrevision für einen neuen Auftrag zu einer Meldung: die gespeicherte, erhöht um noch offene
     * Teilnahme- und Änderungsaufträge derselben Meldung.
     */
    static int erwarteteRevision(AuftragsBestand bestand, String lokaleUuid, Map<String, Integer> gespeichert) {
        return Math.max(gespeichert.getOrDefault(lokaleUuid, 1), offeneRevisionen(bestand).getOrDefault(lokaleUuid, 0));
    }

    /**
     * Meldungen, für die der automatische Meldelisten-Abgleich keinen Auftrag erzeugen darf: offene Anlage-, Änderungs-
     * und Stornoaufträge (keine Doppelaufträge) sowie fachlich abgelehnte, noch nicht quittierte Aufträge dieser
     * Arten. Ohne Klärung würde sonst dieselbe Ablehnung bei jeder Erfassung erneut entstehen. Revisionskonflikte
     * werden nach Übernahme der Online-Revision von {@link #anwenden} quittiert und dürfen erneut gesendet werden;
     * eine Stornierung erzeugt {@link #anwenden} dabei gleich neu ({@link #stornoErneut}).
     */
    static Set<String> gesperrteMeldungen(AuftragsBestand bestand) {
        Set<String> gesperrt = new HashSet<>();
        for (AuftragsBestand.Eintrag eintrag : bestand.eintraege()) {
            boolean anmeldung = eintrag.auftrag().art().sperrtMeldungBisZurKlaerung();
            boolean sperrt = eintrag.zustand() == AuftragsBestand.Zustand.OFFEN
                    || eintrag.zustand() == AuftragsBestand.Zustand.ABGELEHNT;
            String uuid = text(kontext(eintrag.auftrag()), KONTEXT_UUID);
            if (anmeldung && sperrt && !uuid.isEmpty()) {
                gesperrt.add(uuid);
            }
        }
        return gesperrt;
    }

    /**
     * Protokoll angewendeter Entscheidungen (A-29). Entscheidungen, die ein eigener Schreibvorgang protokolliert,
     * fehlen; bleibt keine übrig, entsteht kein Auftrag.
     */
    static void entscheidungen(AuftragsBestand bestand, String tournamentId,
            List<KonfliktSammlung.Protokoll> protokoll) {
        List<KonfliktSammlung.Protokoll> zuMelden = protokoll.stream()
                .filter(eintrag -> eintrag.entscheidung().serverCode().isPresent()).toList();
        // PTM-Online nimmt höchstens 200 Entscheidungen je Aufruf an.
        for (int start = 0; start < zuMelden.size(); start += MAX_ENTSCHEIDUNGEN_JE_AUFTRAG) {
            List<KonfliktSammlung.Protokoll> teil = zuMelden.subList(start,
                    Math.min(zuMelden.size(), start + MAX_ENTSCHEIDUNGEN_JE_AUFTRAG));
            bestand.erzeuge(AuftragsArt.ENTSCHEIDUNGEN, "POST", TournamentSyncClient.entscheidungenPfad(tournamentId),
                    TournamentSyncClient.entscheidungenBody(teil), "{}");
        }
    }

    /** Abschluss des Online-Turniers; ein noch nicht gesendeter früherer Abschluss geht in diesem auf. */
    static SyncAuftrag abschluss(AuftragsBestand bestand, String tournamentId, boolean ranglisteVeroeffentlichen,
            boolean teilnehmerBenachrichtigen) {
        String pfad = TournamentSyncClient.abschlussPfad(tournamentId);
        bestand.entferneUeberholte(auftrag -> auftrag.pfad().equals(pfad));
        return bestand.erzeuge(AuftragsArt.ABSCHLUSS, "POST", pfad,
                TournamentSyncClient.abschlussBody(ranglisteVeroeffentlichen, teilnehmerBenachrichtigen), "{}");
    }

    /** Schließt die Online-Anmeldung, damit bis zum Rundenstart keine Anmeldungen mehr eingehen (KP-05). */
    static void anmeldungSchliessen(AuftragsBestand bestand, String tournamentId) {
        String pfad = TournamentSyncClient.anmeldungSchliessenPfad(tournamentId);
        bestand.entferneUeberholte(auftrag -> auftrag.pfad().equals(pfad));
        bestand.erzeuge(AuftragsArt.ANMELDUNG_SCHLIESSEN, "PUT", pfad, "{\"closed\":true}", "{}");
    }

    /**
     * Ankündigung der Turnierleitung; eine noch nicht gesendete ältere entfällt, online gilt nur die letzte. Bei
     * pausiertem Sync wird sie vor ältere ungesendete Aufträge gezogen und geht allein hinaus; die übrigen bleiben bis
     * zum Fortsetzen offen.
     */
    static void ankuendigung(AuftragsBestand bestand, String tournamentId, String text, boolean pausiert) {
        String pfad = TournamentSyncClient.ankuendigungPfad(tournamentId);
        bestand.entferneUeberholte(auftrag -> auftrag.pfad().equals(pfad));
        SyncAuftrag auftrag = bestand.erzeuge(AuftragsArt.ANKUENDIGUNG, "PUT", pfad,
                TournamentSyncClient.ankuendigungBody(text), "{}");
        if (pausiert) {
            bestand.zieheVor(auftrag.auftragsId());
        }
    }

    /**
     * Stand des Timers zum Zeitpunkt {@code stand}; ein noch nicht gesendeter älterer Stand entfällt, online gilt nur
     * der letzte. Bei pausiertem Sync wird er wie die Ankündigung vorgezogen und geht allein hinaus.
     */
    static void timer(AuftragsBestand bestand, String tournamentId, String body, Instant stand, boolean pausiert) {
        erfasseTimer(bestand, TournamentSyncClient.timerPfad(tournamentId), body, stand, pausiert);
    }

    private static void erfasseTimer(AuftragsBestand bestand, String pfad, String body, Instant stand,
            boolean pausiert) {
        bestand.entferneUeberholte(auftrag -> auftrag.pfad().equals(pfad));
        JsonObject kontext = new JsonObject();
        kontext.addProperty(KONTEXT_TIMER_STAND, stand.toString());
        SyncAuftrag auftrag = bestand.erzeuge(AuftragsArt.TIMER, "PUT", pfad, body, kontext.toString());
        if (pausiert) {
            bestand.zieheVor(auftrag.auftragsId());
        }
    }

    /**
     * PTM-Online rechnet das Ende eines laufenden Timers ab Eingang. Ein liegengebliebener, noch nicht bestätigter
     * Timer-Stand (ohne Netz, Dokument gerade belegt) wird daher unmittelbar vor dem Senden durch einen neuen Auftrag
     * mit der bis jetzt verbleibenden Restzeit ersetzt. Neuer Auftrag statt geänderter Nutzlast: Eine schon
     * angekommene Auftrags-ID mit anderem Inhalt lehnt PTM-Online ab ({@code idempotency_mismatch}); ein doppelt
     * angekommener Timer-Stand ist dagegen harmlos, er ist absolut. Nur unter der Versandsperre aufrufen.
     */
    static void timerNachstellen(AuftragsBestand bestand, Instant jetzt, boolean pausiert) {
        for (SyncAuftrag auftrag : bestand.zuSenden(false)) {
            if (auftrag.art() == AuftragsArt.TIMER) {
                nachgestellterTimerBody(auftrag, jetzt)
                        .ifPresent(body -> erfasseTimer(bestand, auftrag.pfad(), body, jetzt, pausiert));
            }
        }
    }

    private static Optional<String> nachgestellterTimerBody(SyncAuftrag auftrag, Instant jetzt) {
        String stand = text(kontext(auftrag), KONTEXT_TIMER_STAND);
        if (stand.isEmpty()) {
            return Optional.empty();
        }
        try {
            Duration verstrichen = Duration.between(Instant.parse(stand), jetzt);
            if (verstrichen.compareTo(TIMER_NACHSTELLEN_AB) < 0) {
                return Optional.empty();
            }
            return TournamentSyncClient.timerBodyNachgestellt(auftrag.body(), verstrichen);
        } catch (DateTimeParseException | JsonParseException | IllegalStateException | UnsupportedOperationException e) {
            logger.warn("PTM-Online: Timer-Auftrag {} nicht nachstellbar, geht unverändert hinaus", auftrag.zaehler(),
                    e);
            return Optional.empty();
        }
    }

    /** Ein abgelehnter, noch nicht zur Kenntnis genommener Auftrag als Fall der Konfliktliste. */
    record AbgelehnterFall(String auftragsId, KonfliktFall fall) {}

    /**
     * Abgelehnte, noch nicht quittierte Aufträge als Fälle der Konfliktliste (A-29) – auch die aus dem
     * Hintergrundversand. Die Bezeichnung trägt den Schreibzähler, unter dem der Auftrag gespeichert ist.
     */
    static List<AbgelehnterFall> abgelehnteFaelle(AuftragsBestand bestand) {
        List<AbgelehnterFall> faelle = new ArrayList<>();
        for (AuftragsBestand.Eintrag eintrag : bestand.eintraege()) {
            if (eintrag.zustand() != AuftragsBestand.Zustand.ABGELEHNT) {
                continue;
            }
            SyncAuftrag auftrag = eintrag.auftrag();
            JsonObject kontext = kontext(auftrag);
            String bezeichnung = text(kontext, KONTEXT_BEZEICHNUNG);
            String lokal = (bezeichnung.isEmpty() ? auftrag.art().anzeige() : bezeichnung) + " (#" + auftrag.zaehler()
                    + ")";
            String uuid = text(kontext, KONTEXT_UUID);
            faelle.add(new AbgelehnterFall(auftrag.auftragsId(), new KonfliktFall(KonfliktArt.AUFTRAG_ABGELEHNT,
                    uuid.isEmpty() ? null : uuid, List.of(), lokal, auftrag.art().anzeige(),
                    ablehnungsHinweis(eintrag.grund()), List.of(Entscheidung.ZUR_KENNTNIS))));
        }
        return faelle;
    }

    /** Was eine Ablehnung bedeutet und was zu tun ist; unbekannte Gründe erscheinen unverändert. */
    static String ablehnungsHinweis(String grund) {
        return switch (grund) {
            case "composition_locked", "formation_locked", "registration_drawn", "user_id_invalid", "unit_invalid",
                    CODE_AUSFUEHRUNGSKONFLIKT, CODE_GLEICHE_ANMELDUNG, CODE_TURNIER_LAEUFT -> I18n.get("ptmonline.konflikt.hinweis.abgelehnt." + grund);
            default -> I18n.get("ptmonline.konflikt.hinweis.abgelehnt", grund);
        };
    }

    /**
     * Trennen, vor alle offenen Aufträge gezogen, um es allein zu senden ({@link #sendeAllein}). Die übrigen bleiben
     * offen, bis das Trennen gelungen ist ({@link #nachTrennen}); scheitert es, nimmt {@link #trennenZuruecknehmen} es
     * wieder heraus – sonst trennte der Hintergrund später doch noch, obwohl die Turnierleitung abgebrochen hat.
     */
    static SyncAuftrag trennen(AuftragsBestand bestand, String tournamentId) {
        SyncAuftrag trennen = bestand.erzeuge(AuftragsArt.TRENNEN, "POST",
                TournamentSyncClient.trennenPfad(tournamentId), "{}", "{}");
        bestand.zieheVor(trennen.auftragsId());
        return trennen;
    }

    /** Nach dem Trennen (oder online bereits gelöster Bindung) sind die übrigen offenen Aufträge sinnlos. */
    static void nachTrennen(AuftragsBestand bestand, String grundVerworfen) {
        bestand.verwerfe(art -> true, grundVerworfen);
    }

    /** Gescheitertes Trennen: Der Auftrag entfällt, die übrigen bleiben unverändert offen. */
    static void trennenZuruecknehmen(AuftragsBestand bestand, SyncAuftrag trennen) {
        bestand.entferneUeberholte(auftrag -> auftrag.auftragsId().equals(trennen.auftragsId()));
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
     * @param abschluss     Antwort auf einen angenommenen Turnierabschluss dieses Laufs, sonst leer
     */
    record Anwendung(int angelegt, List<String> abgelehnt, List<String> nachStart,
            Map<String, RegistrationDto> geaendert, List<VersandErgebnis> abgelehnteAuftraege,
            Optional<SyncAntwort> abschluss) {

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
        Map<String, ZuordnungsZusatz> zusaetze = new LinkedHashMap<>();
        Set<String> storniert = new LinkedHashSet<>();
        List<VersandErgebnis> ueberholt = new ArrayList<>();
        Map<SyncAuftrag, Integer> stornosErneut = new LinkedHashMap<>();
        List<VersandErgebnis> teilnahmeKonflikte = new ArrayList<>();
        boolean startErledigt = false;
        boolean startAngenommen = false;
        SyncAntwort abschluss = null;
        for (VersandErgebnis ergebnis : ergebnisse) {
            SyncAuftrag auftrag = ergebnis.auftrag();
            JsonObject kontext = kontext(auftrag);
            // Auch ein abgelehnter Start ist erledigt: er würde sonst bei jedem Durchlauf neu erzeugt.
            startErledigt |= auftrag.art() == AuftragsArt.START;
            startAngenommen |= auftrag.art() == AuftragsArt.START && ergebnis.angenommen();
            if (!ergebnis.angenommen()) {
                Optional<Integer> onlineRevision = onlineRevision(ergebnis, kontext);
                onlineRevision.ifPresent(revision -> {
                    revisionen.put(text(kontext, KONTEXT_UUID), revision);
                    ueberholt.add(ergebnis);
                    if (auftrag.art() == AuftragsArt.ANMELDUNG_STORNIEREN) {
                        stornosErneut.put(auftrag, revision);
                    }
                });
                if (auftrag.art() == AuftragsArt.TEILNAHME && istAusfuehrungskonflikt(ergebnis)) {
                    teilnahmeKonflikte.add(ergebnis);
                }
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
                            text(kontext, KONTEXT_UUID), text(kontext, KONTEXT_BEZEICHNUNG),
                            TournamentSyncClient.registrationAus(ergebnis.antwort())));
                    case ANMELDUNG_AENDERN -> {
                        RegistrationDto registration = TournamentSyncClient.registrationAus(ergebnis.antwort());
                        String uuid = text(kontext, KONTEXT_UUID);
                        geaendert.put(uuid, registration);
                        revisionen.put(uuid, Objects.requireNonNullElse(registration.executionRevision(), 1));
                        if (kontext.has(KONTEXT_BESETZUNG)) {
                            zusaetze.put(uuid, ZuordnungsZusatz.besetzungUndTeamname(
                                    text(kontext, KONTEXT_BESETZUNG), text(kontext, KONTEXT_TEAMNAME)));
                        }
                    }
                    case ANMELDUNG_STORNIEREN -> {
                        String uuid = text(kontext, KONTEXT_UUID);
                        if (!uuid.isEmpty()) {
                            storniert.add(uuid);
                        }
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
                    case ANMELDUNG_VERKNUEPFEN -> meldeNichtVerknuepfte(auftrag, ergebnis.antwort());
                    case ABSCHLUSS -> abschluss = ergebnis.antwort();
                    case START, TRENNEN, RUNDE, RUNDE_LOESCHEN, RANGLISTE, SPIELEINSTELLUNGEN, MELEE_TEAMS, ENTSCHEIDUNGEN,
                            ANMELDUNG_SCHLIESSEN, ERGEBNISMELDUNGEN, ANKUENDIGUNG, TIMER -> {
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
        vermerkeLokalEntfernt(storniert, mapping, zusaetze);
        if (!zusaetze.isEmpty()) {
            mapping.setZusaetze(zusaetze);
        }
        if (startErledigt) {
            mapping.setRunningAusstehendSeit(null);
        }
        if (startAngenommen) {
            mapping.setOnlineStatus(ONLINE_STATUS_LAEUFT);
        }
        if (abschluss != null) {
            mapping.setOnlineStatus(ONLINE_STATUS_ABGESCHLOSSEN);
        }
        for (VersandErgebnis ergebnis : ergebnisse) {
            bestand.angewendet(ergebnis, ablehnungsGrund(ergebnis));
        }
        // Das Dokument ist Master: Mit der übernommenen Online-Revision sendet der nächste Lauf den Stand erneut.
        ueberholt.forEach(ergebnis -> bestand.quittiere(ergebnis.auftrag().auftragsId()));
        stornosErneut.forEach((storno, revision) -> stornoErneut(bestand, storno, revision));
        if (!teilnahmeKonflikte.isEmpty()) {
            wiederholeTeilnahmen(bestand, ergebnisse, teilnahmeKonflikte, mapping.getExecutionRevisionenProUuid());
        }
        speichern(bestand, mapping);
        return new Anwendung(zuordnungen.size(), abgelehnt, nachStart, geaendert,
                ergebnisse.stream().filter(ergebnis -> !ergebnis.angenommen()).toList(), Optional.ofNullable(abschluss));
    }

    /**
     * Eine Änderung oder Stornierung scheiterte an einer zwischenzeitlich online geänderten Anmeldung
     * ({@code execution_conflict}): Deren Revision gilt danach als gespeichert.
     *
     * @return die Online-Revision, sofern die Antwort sie nennt
     */
    private static Optional<Integer> onlineRevision(VersandErgebnis ergebnis, JsonObject kontext) {
        AuftragsArt art = ergebnis.auftrag().art();
        if ((art != AuftragsArt.ANMELDUNG_AENDERN && art != AuftragsArt.ANMELDUNG_STORNIEREN)
                || text(kontext, KONTEXT_UUID).isEmpty() || !istAusfuehrungskonflikt(ergebnis)) {
            return Optional.empty();
        }
        return TournamentSyncClient.konfliktRegistrationAus(ergebnis.antwort()).map(RegistrationDto::executionRevision);
    }

    private static boolean istAusfuehrungskonflikt(VersandErgebnis ergebnis) {
        return ergebnis.antwort().code().filter(CODE_AUSFUEHRUNGSKONFLIKT::equals).isPresent();
    }

    /**
     * Neuer Versuch einer Stornierung, die an einem Revisionskonflikt scheiterte, mit der übernommenen Online-Revision.
     * Das Dokument ist Master; anders als bei einer Namensänderung erkennt der nächste Lauf eine Löschung aber nicht
     * zuverlässig wieder (das Löschgedächtnis lebt nur im Speicher, nach einem Neustart fehlt es). Steht die Zeile
     * inzwischen wieder in der Meldeliste, nimmt {@link #stornosZuruecknehmen} den Auftrag zurück.
     */
    static SyncAuftrag stornoErneut(AuftragsBestand bestand, SyncAuftrag gescheitert, int onlineRevision) {
        String onlineId = text(json(gescheitert.body(), gescheitert), BODY_ONLINE_ID);
        return bestand.erzeuge(AuftragsArt.ANMELDUNG_STORNIEREN, "PUT", gescheitert.pfad(),
                TournamentSyncClient.stornoBody(onlineId, onlineRevision), gescheitert.kontext());
    }

    /**
     * Teilnahme-Aufträge, die nur an einem vorausgehenden abgelehnten Auftrag scheiterten, entstehen neu; ihre
     * Ablehnung gilt damit als erledigt. Echte Online-Konflikte bleiben für den Abgleich stehen.
     * <p>
     * Die Wiederholung erhält den nächsten Schreibzähler und geht damit nach allen schon erzeugten Aufträgen hinaus.
     * Meldungen, die ein neuerer Teilnahme-Auftrag trägt, entfallen daher – sonst überschriebe der ältere Stand den
     * neueren. Ein neuerer Auftrag steht entweder unter den Ergebnissen dieses Anwendens (angenommen: sein Stand gilt;
     * abgelehnt: er wird selbst wiederholt) oder offen im Bestand (er geht noch hinaus). Ältere Ergebnisse gibt es
     * nicht: Gesendet wird in Zählerreihenfolge, und alle Ergebnisse seit dem letzten Anwenden liegen hier vor.
     *
     * @param ergebnisse alle Ergebnisse dieses Anwendens
     * @param konflikte  Teilnahme-Aufträge mit Revisionskonflikt
     * @param gespeichert gespeicherte Ausführungsrevision je lokaler UUID nach dem Anwenden
     */
    static void wiederholeTeilnahmen(AuftragsBestand bestand, List<VersandErgebnis> ergebnisse,
            List<VersandErgebnis> konflikte, Map<String, Integer> gespeichert) {
        List<SyncAuftrag> teilnahmen = new ArrayList<>();
        ergebnisse.stream().map(VersandErgebnis::auftrag).filter(auftrag -> auftrag.art() == AuftragsArt.TEILNAHME)
                .forEach(teilnahmen::add);
        bestand.eintraege().stream().filter(eintrag -> eintrag.zustand() == AuftragsBestand.Zustand.OFFEN)
                .map(AuftragsBestand.Eintrag::auftrag).filter(auftrag -> auftrag.art() == AuftragsArt.TEILNAHME)
                .forEach(teilnahmen::add);
        // Vor dem ersten neuen Auftrag bestimmt, damit eine Wiederholung nicht als neuerer Stand zählt.
        Map<SyncAuftrag, Set<String>> neuerGemeldet = new LinkedHashMap<>();
        konflikte.stream().map(VersandErgebnis::auftrag)
                .filter(auftrag -> !folgefehlerTeilnahme(auftrag, gespeichert).isEmpty())
                .forEach(auftrag -> neuerGemeldet.put(auftrag, neuerGemeldet(auftrag, teilnahmen)));
        neuerGemeldet.forEach((gescheitert, ueberholt) -> {
            boolean allesNeuer = ueberholt.containsAll(revisionenAusKontext(gescheitert).keySet());
            if (allesNeuer || teilnahmeErneut(bestand, gescheitert, gespeichert, ueberholt).isPresent()) {
                bestand.quittiere(gescheitert.auftragsId());
            }
        });
    }

    /** Meldungen eines Teilnahme-Auftrags, die ein neuerer (höherer Schreibzähler) ebenfalls trägt. */
    private static Set<String> neuerGemeldet(SyncAuftrag auftrag, List<SyncAuftrag> teilnahmen) {
        Set<String> uuids = new HashSet<>();
        teilnahmen.stream().filter(andere -> andere.zaehler() > auftrag.zaehler())
                .forEach(andere -> uuids.addAll(revisionenAusKontext(andere).keySet()));
        uuids.retainAll(revisionenAusKontext(auftrag).keySet());
        return uuids;
    }

    /**
     * Neuer Versuch eines Teilnahme-Auftrags nach einem Folgefehler ({@link #folgefehlerTeilnahme}): gleiche Teilnahmen,
     * Setzpositionen und Teamnummern, erwartet wird die gespeicherte Revision. Als gezählter Auftrag wird er mit dem
     * Dokument gespeichert und geht auch nach Pause oder Neuladen hinaus. Scheitert er erneut an einem davor
     * eingereihten Auftrag, erkennt der nächste Durchlauf wieder einen Folgefehler mit dann aktueller Revision.
     *
     * @param ausgenommen Meldungen, deren neueren Stand ein anderer Auftrag trägt; sie entfallen
     * @return der neue Auftrag, leer bei unlesbarer Nutzlast (der abgelehnte bleibt dann für den Abgleich stehen) oder
     *         wenn keine Meldung übrig bleibt
     */
    static Optional<SyncAuftrag> teilnahmeErneut(AuftragsBestand bestand, SyncAuftrag gescheitert,
            Map<String, Integer> gespeichert, Set<String> ausgenommen) {
        List<String> uuids = List.copyOf(revisionenAusKontext(gescheitert).keySet());
        JsonObject body = json(gescheitert.body(), gescheitert).deepCopy();
        JsonElement registrations = body.get(BODY_REGISTRATIONS);
        if (registrations == null || !registrations.isJsonArray()
                || registrations.getAsJsonArray().size() != uuids.size()) {
            logger.warn("PTM-Online: Teilnahme-Auftrag {} nicht wiederholbar, Nutzlast passt nicht zum Kontext",
                    gescheitert.zaehler());
            return Optional.empty();
        }
        JsonArray bisher = registrations.getAsJsonArray();
        JsonArray eintraege = new JsonArray();
        JsonObject neueRevisionen = new JsonObject();
        for (int i = 0; i < uuids.size(); i++) {
            if (ausgenommen.contains(uuids.get(i))) {
                continue;
            }
            int erwartet = gespeichert.getOrDefault(uuids.get(i), 1);
            JsonObject eintrag = bisher.get(i).getAsJsonObject();
            eintrag.addProperty(BODY_ERWARTETE_REVISION, erwartet);
            eintraege.add(eintrag);
            neueRevisionen.addProperty(uuids.get(i), erwartet + 1);
        }
        if (eintraege.isEmpty()) {
            return Optional.empty();
        }
        body.add(BODY_REGISTRATIONS, eintraege);
        JsonObject kontext = kontext(gescheitert).deepCopy();
        kontext.add(KONTEXT_REVISIONEN, neueRevisionen);
        logger.info("PTM-Online: Teilnahme-Auftrag {} scheiterte an einem vorausgehenden abgelehnten Auftrag; {} "
                + "Check-ins werden erneut gemeldet", gescheitert.zaehler(), eintraege.size());
        return Optional.of(bestand.erzeuge(AuftragsArt.TEILNAHME, gescheitert.methode(), gescheitert.pfad(),
                body.toString(), kontext.toString()));
    }

    /**
     * Ein Teilnahme-Auftrag scheiterte am Revisionskonflikt. PTM-Online lehnt dann den ganzen Auftrag ab, nennt aber
     * keine Revision. Erwartete er für eine Meldung eine andere Revision als die jetzt gespeicherte, war ein
     * vorausgehender Auftrag derselben Meldung eingerechnet, der abgelehnt wurde – ein Folgefehler. Dann ist die
     * Teilnahme aller Meldungen des Auftrags erneut zu melden. Stimmt die Revision überein, hat sich die Anmeldung
     * online geändert; das klärt der manuelle Abgleich (sonst scheiterte jede Wiederholung erneut).
     *
     * @param gespeichert gespeicherte Ausführungsrevision je lokaler UUID nach dem Anwenden
     * @return die erneut zu meldenden Meldungen, leer ohne Folgefehler
     */
    static Set<String> folgefehlerTeilnahme(SyncAuftrag teilnahme, Map<String, Integer> gespeichert) {
        Map<String, Integer> nachher = revisionenAusKontext(teilnahme);
        boolean folgefehler = nachher.entrySet().stream()
                .anyMatch(eintrag -> eintrag.getValue() - 1 != gespeichert.getOrDefault(eintrag.getKey(), 1));
        return folgefehler ? Set.copyOf(nachher.keySet()) : Set.of();
    }

    /** Ergänzt bei online stornierten Meldungen den Vermerk „lokal entfernt“, ohne andere Vermerke zu verlieren. */
    private static void vermerkeLokalEntfernt(Set<String> storniert, PtmOnlineRegistrationMapping mapping,
            Map<String, ZuordnungsZusatz> zusaetze) throws GenerateException {
        if (storniert.isEmpty()) {
            return;
        }
        Map<String, ZuordnungsZusatz> bisher = mapping.getZusaetzeProUuid();
        for (String uuid : storniert) {
            String vermerk = ZuordnungsVermerke.lese(Optional.ofNullable(bisher.get(uuid))
                    .map(ZuordnungsZusatz::vermerk).orElse(null)).mit(ZuordnungsVermerke.LOKAL_ENTFERNT).alsText();
            zusaetze.merge(uuid, ZuordnungsZusatz.nurVermerk(vermerk),
                    (alt, neu) -> new ZuordnungsZusatz(alt.besetzung(), neu.vermerk(), alt.teamname()));
        }
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
        VersandStopp stopp;
        bestand.versandSperre().lockInterruptibly();
        try {
            timerNachstellen(bestand, Instant.now(), pausiert);
            speichern(bestand, mapping);
            stopp = AuftragsVersand.sende(bestand.zuSenden(pausiert), bestand.mitVersandVermerk(client),
                    bestand::gesendet);
        } finally {
            bestand.versandSperre().unlock();
        }
        return new SynchronerVersand(stopp, anwenden(bestand, mapping));
    }

    /** Sendet nur diesen einen offenen Auftrag, sofort und unabhängig von einer Pause; die übrigen bleiben liegen. */
    static SynchronerVersand sendeAllein(AuftragsBestand bestand, PtmOnlineRegistrationMapping mapping,
            TournamentSyncClient client, SyncAuftrag auftrag) throws GenerateException, InterruptedException {
        VersandStopp stopp;
        bestand.versandSperre().lockInterruptibly();
        try {
            speichern(bestand, mapping);
            List<SyncAuftrag> nurDieser = bestand.zuSenden(false).stream()
                    .filter(offen -> offen.auftragsId().equals(auftrag.auftragsId())).toList();
            stopp = AuftragsVersand.sende(nurDieser, bestand.mitVersandVermerk(client), bestand::gesendet);
        } finally {
            bestand.versandSperre().unlock();
        }
        return new SynchronerVersand(stopp, anwenden(bestand, mapping));
    }

    /** Ergebnis von {@link #sendeSynchron}. */
    record SynchronerVersand(VersandStopp stopp, Anwendung anwendung) {
    }

    // ── Kontext ─────────────────────────────────────────────────────────────

    /**
     * Unbekannte Anmeldungen (online gelöscht) und Konflikte (lokale UUID trägt schon eine andere Anmeldung) ändern
     * nichts im Dokument; der nächste Abgleich bildet die Zuordnung ohnehin neu.
     */
    private static void meldeNichtVerknuepfte(SyncAuftrag auftrag, SyncAntwort antwort) throws IOException {
        List<String> unbekannt = TournamentSyncClient.textListeAus(antwort, "unknown");
        List<String> konflikte = TournamentSyncClient.textListeAus(antwort, "conflicts");
        if (!unbekannt.isEmpty() || !konflikte.isEmpty()) {
            logger.warn("PTM-Online: Verknüpfungsauftrag {} – unbekannte Anmeldungen {}, Konflikte {}",
                    auftrag.zaehler(), unbekannt, konflikte);
        }
    }

    private static JsonObject kontext(SyncAuftrag auftrag) {
        return json(auftrag.kontext(), auftrag);
    }

    private static JsonObject json(String text, SyncAuftrag auftrag) {
        try {
            JsonElement element = JsonParser.parseString(text);
            return element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
        } catch (JsonParseException e) {
            logger.warn("PTM-Online: JSON von Auftrag {} unlesbar", auftrag.zaehler(), e);
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
