/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.LibreOfficePtmOnlineSpeicher;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.LoMainThread;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxResult;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.model.IMeldung;
import de.petanqueturniermanager.model.IMeldungen;
import de.petanqueturniermanager.ptmonline.dto.NeueOnlineAnmeldung;
import de.petanqueturniermanager.spielerdb.MeldelisteZiel;
import de.petanqueturniermanager.spielerdb.MeldelisteSpielerDaten;
import de.petanqueturniermanager.spielerdb.MeleeAnmeldungZiel;

/**
 * Gleicht bei jedem Spielrunden-Start die Meldeliste des Turnierdokuments automatisch mit dem
 * verbundenen PTM-Online-Turnier ab (Prinzip "Turnierdokument ist Master"). Wird synchron aus dem
 * jeweiligen {@code *SpielrundeSheetNaechste}-{@code SheetRunner} heraus aufgerufen.
 * <p>
 * Synchron läuft nur der lokale Teil (Nr-Formeln, lokale UUIDs, Momentaufnahme als
 * {@link PtmOnlineStatusAuftrag}) und vor der ersten Runde die Rückfrage zu fehlenden Online-Meldungen – mit
 * kurzem Zeitlimit. Turnierstart, Nachmeldungen und Teilnahme-Push sendet der {@link PtmOnlineLiveBeobachter} im
 * Hintergrund, mit Wiederholung bei Netzfehlern: die Auslosung wartet nie auf das Netz.
 * <p>
 * No-Op, wenn PTM-Online nicht konfiguriert, das Dokument nicht verbunden oder der Sync pausiert ist.
 */
public final class PtmOnlineSpielrundeSync {

    private static final Logger logger = LogManager.getLogger(PtmOnlineSpielrundeSync.class);
    private static final int MAX_NAMEN_IN_RUECKFRAGE = 15;

    private PtmOnlineSpielrundeSync() {}

    /**
     * Extrahiert die Team-/Spieler-Nummern aus einem {@link IMeldungen}-Ergebnis (z.&nbsp;B.
     * {@code TeamMeldungen} oder Supermelees {@code SpielerMeldungen}) — gemeinsamer Helfer für die
     * Aufrufer von {@link #abgleichen}, die ihre system-spezifische Meldeliste bereits kennen.
     */
    public static Set<Integer> nummern(IMeldungen<?, ?> meldungen) {
        return meldungen.getMeldungen().stream().map(IMeldung::getNr).collect(Collectors.toSet());
    }

    /**
     * @param istErsteRunde        {@code true}, wenn dieser Aufruf die allererste Spielrunde des
     *                             Turniers (bzw. bei Supermelee: des Spieltags) erzeugt — prüft dann, ob
     *                             online noch bestätigte Meldungen fehlen (Rückfrage), und startet das
     *                             Online-Turnier.
     * @param alleTeamNummern      Team-/Spieler-Nummern aller aktuell in der Meldeliste erfassten
     *                             Teams (entspricht der Zeilennummerierung, die auch
     *                             {@link PtmOnlineRegistrationMapping} als Team-Nr verwendet).
     * @param aktiveTeamNummern    Teilmenge davon: aktuell aktiv/teilnehmend.
     * @param ausgestiegeneTeamNummern
     *                             Teilmenge davon: ausgesetzt (Aktiv-Spalte = 2, nimmt nicht mehr
     *                             teil). Wird als Teilnahme {@link OnlineTeilnahme#AUSGESETZT}
     *                             gemeldet, der online verwaltete Anmeldestatus bleibt unverändert.
     *                             Teams, die weder aktiv noch ausgesetzt sind, gelten als
     *                             {@link OnlineTeilnahme#INAKTIV}.
     */
    public static void abgleichen(WorkingSpreadsheet ws, TurnierSystem ts, boolean istErsteRunde,
            Set<Integer> alleTeamNummern, Set<Integer> aktiveTeamNummern, Set<Integer> ausgestiegeneTeamNummern)
            throws GenerateException {
        Optional<PtmOnlineVerbindung> verbindung = verbindung(ws, ts);
        if (verbindung.isPresent()) {
            abgleichen(ws, verbindung.get(), istErsteRunde, alleTeamNummern, aktiveTeamNummern,
                    ausgestiegeneTeamNummern);
        }
    }

    /**
     * Rundenstart-Abgleich für Systeme, deren Meldeliste „ausgestiegen“ (Aktiv-Spalte = 2) nicht als eigene
     * Meldungsmenge liefert (KO, JGJ, Poule, Kaskade, Trip-Tête): Ausgestiegene werden aus der Aktiv-Spalte
     * gelesen, sofern das System sie nicht ohnehin als aktiv führt. Sonst wie {@link #abgleichen}.
     *
     * @param alleMeldungen   alle Meldungen der Meldeliste
     * @param aktiveMeldungen die Meldungen, die das System in der Runde tatsächlich spielen lässt
     */
    public static void turnierstartAbgleichen(WorkingSpreadsheet ws, TurnierSystem ts, boolean istErsteRunde,
            IMeldungen<?, ?> alleMeldungen, IMeldungen<?, ?> aktiveMeldungen) throws GenerateException {
        Optional<PtmOnlineVerbindung> verbindung = verbindung(ws, ts);
        if (verbindung.isEmpty()) {
            return;
        }
        Set<Integer> aktive = nummern(aktiveMeldungen);
        Set<Integer> ausgestiegene = ausgestiegeneTeamNummern(verbindung.get().meldeliste());
        ausgestiegene.removeAll(aktive);
        abgleichen(ws, verbindung.get(), istErsteRunde, nummern(alleMeldungen), aktive, ausgestiegene);
    }

    private static Set<Integer> ausgestiegeneTeamNummern(MeldelisteZiel ziel) {
        return new HashSet<>(TeilnahmeNummern.ausAktivSpalte(ziel).ausgesetzt());
    }

    /**
     * Lokaler Teil synchron (Nr-Formeln, Rückfrage vor dem Turnierstart, Momentaufnahme); Turnierstart,
     * Nachmeldungen und Teilnahme-Push laufen im Hintergrund ({@link PtmOnlineLiveBeobachter}), damit die Auslosung
     * nicht auf das Netz wartet.
     */
    private static void abgleichen(WorkingSpreadsheet ws, PtmOnlineVerbindung verbindung, boolean istErsteRunde,
            Set<Integer> alleTeamNummern, Set<Integer> aktiveTeamNummern, Set<Integer> ausgestiegeneTeamNummern)
            throws GenerateException {
        XComponentContext ctx = ws.getxContext();
        var config = verbindung.config();
        MeldelisteZiel ziel = verbindung.ziel();
        PtmOnlineRegistrationMapping mapping = verbindung.mapping();
        String tournamentId = verbindung.tournamentId();
        if (!istSyncAktiv(mapping)) {
            return;
        }
        TournamentSyncClient client;
        try {
            client = verbindung.gebundenerClient();
        } catch (GenerateException e) {
            zeigeFehlerSammlung(ctx, List.of(e.getMessage()));
            return;
        }
        List<String> fehler = new ArrayList<>();

        try {
            mapping.aktualisiereAnzeigeFormeln(formelnProUuid(ziel));
        } catch (GenerateException e) {
            logger.error("PTM-Online: Nr-Formeln der Zuordnung konnten nicht aktualisiert werden", e);
            fehler.add(e.getMessage());
        } catch (RuntimeException e) {
            logger.error("PTM-Online: Unerwarteter Mapping-Fehler", e);
            fehler.add(netzwerkFehlerText(e));
        }

        if (istErsteRunde && !weiterTrotzFehlenderOnlineMeldungen(ctx, config, mapping, tournamentId, ziel, fehler)) {
            throw SheetRunner.verarbeitungAbgebrochen();
        }

        try {
            PtmOnlineStatusAuftrag auftrag = statusAuftrag(ziel, mapping, tournamentId, istErsteRunde,
                    lokaleMeldungen(ziel, verbindung.meldeliste(), alleTeamNummern, aktiveTeamNummern,
                            ausgestiegeneTeamNummern));
            if (!PtmOnlineLiveBeobachter.statusEinreihen(ws.getWorkingSpreadsheetDocument(), auftrag)) {
                // Ohne Hintergrund-Beobachter (nur in Tests ohne Plugin-Start) wie früher direkt senden.
                sendeSynchron(mapping, client, auftrag, fehler);
            }
        } catch (GenerateException e) {
            logger.error("PTM-Online: Status-Abgleich nicht vorbereitbar", e);
            fehler.add(e.getMessage());
        } catch (RuntimeException e) {
            logger.error("PTM-Online: Unerwarteter Status-Abgleichfehler", e);
            fehler.add(netzwerkFehlerText(e));
        }

        if (!fehler.isEmpty()) {
            zeigeFehlerSammlung(ctx, fehler);
        }
    }

    private static void sendeSynchron(PtmOnlineRegistrationMapping mapping, TournamentSyncClient client,
            PtmOnlineStatusAuftrag auftrag, List<String> fehler) throws GenerateException {
        try {
            PtmOnlineStatusAbgleich.Ergebnis ergebnis = PtmOnlineStatusAbgleich.senden(auftrag, mapping, client);
            PtmOnlineStatusAbgleich.schreiben(mapping, ergebnis);
            if (!ergebnis.abgelehnt().isEmpty()) {
                fehler.add(RegistrationImportTask.onlineAbgelehntHinweis(ergebnis.abgelehnt()));
            }
        } catch (IOException e) {
            logger.error("PTM-Online: Status-Abgleich fehlgeschlagen", e);
            fehler.add(netzwerkFehlerText(e));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Teilnahme und Setzposition je Meldung im Sync-Ziel. Ohne Mêlée entspricht jede Meldung einem Team der
     * Meldeliste. Bei Mêlée-Anmeldung sind die Online-Anmeldungen Einzelspieler: jeder erhält die Teilnahme
     * seines lokal gemischten Teams ({@link MeleeTeilnahme}). Gemeldet wird immer die lokale Setzposition – das
     * Turnierdokument ist Master, eine lokal leere Setzposition löscht die online gepflegte.
     */
    static List<LokaleOnlineMeldung> lokaleMeldungen(MeldelisteZiel ziel, MeldelisteZiel meldeliste,
            Set<Integer> alle, Set<Integer> aktive, Set<Integer> ausgestiegen) {
        if (ziel instanceof MeleeAnmeldungZiel melee) {
            return MeleeTeilnahme.ermittle(melee.leseMeleeZeilen(), spielerProTeam(meldeliste), aktive, ausgestiegen);
        }
        Map<Integer, Integer> zeileProTeam = zeileProTeam(ziel);
        return alle.stream()
                .filter(zeileProTeam::containsKey)
                .map(teamNr -> teamMeldung(ziel, zeileProTeam.get(teamNr),
                        OnlineTeilnahme.aus(aktive.contains(teamNr), ausgestiegen.contains(teamNr))))
                .toList();
    }

    private static LokaleOnlineMeldung teamMeldung(MeldelisteZiel ziel, int zeile, OnlineTeilnahme teilnahme) {
        OptionalInt setzposition = ziel.getSetzpositionAusZeile(zeile);
        return new LokaleOnlineMeldung(zeile, teilnahme, setzposition.isPresent() ? setzposition.getAsInt() : null);
    }

    static Map<Integer, List<MeldelisteSpielerDaten>> spielerProTeam(MeldelisteZiel meldeliste) {
        Map<Integer, List<MeldelisteSpielerDaten>> ergebnis = new LinkedHashMap<>();
        for (MeldelisteSpielerDaten spieler : meldeliste.leseAlleSpielerRoh()) {
            int teamNr = meldeliste.getTeamNrAusZeile(spieler.zeile1Basiert());
            if (teamNr > 0) {
                ergebnis.computeIfAbsent(teamNr, ignored -> new ArrayList<>()).add(spieler);
            }
        }
        return ergebnis;
    }

    /**
     * Pausierter Sync: die Spielrunde entsteht normal, aber ohne Rückfrage, Online-Start und Status-Push. Ist der
     * Zustand nicht lesbar, wird wie bisher synchronisiert.
     */
    static boolean istSyncAktiv(PtmOnlineRegistrationMapping mapping) {
        try {
            if (mapping.istPausiert()) {
                logger.info("PTM-Online: Sync pausiert, Rundenstart-Abgleich übersprungen");
                return false;
            }
        } catch (GenerateException e) {
            logger.warn("PTM-Online: Pausenzustand nicht lesbar, synchronisiere", e);
        }
        return true;
    }

    /**
     * Verbindungsdaten des Dokuments, leer wenn nicht verbunden. Nicht lesbare Verbindungsdaten werden gemeldet und
     * wie „nicht verbunden“ behandelt – der Online-Abgleich darf den Turnierbetrieb nie blockieren.
     */
    private static Optional<PtmOnlineVerbindung> verbindung(WorkingSpreadsheet ws, TurnierSystem ts) {
        try {
            return PtmOnlineVerbindung.ermitteln(ws, ts);
        } catch (GenerateException e) {
            logger.error("PTM-Online: Verbindungsdaten lesen fehlgeschlagen", e);
            zeigeFehlerSammlung(ws.getxContext(), List.of(e.getMessage()));
            return Optional.empty();
        }
    }

    /**
     * Vor dem Turnierstart: Fehlen in der Meldeliste bestätigte Online-Meldungen, wird nachgefragt, ob die
     * Runde trotzdem erstellt werden soll. Übernommen wird hier nichts – dafür ist der manuelle Abgleich da.
     * Kann online nicht geprüft werden, geht es mit einem Hinweis weiter, damit ein Netzproblem den
     * Turnierbeginn nicht blockiert.
     *
     * @return {@code false}, wenn der Anwender abbricht.
     */
    private static boolean weiterTrotzFehlenderOnlineMeldungen(XComponentContext ctx,
            LibreOfficePtmOnlineSpeicher.Zugangsdaten config, PtmOnlineRegistrationMapping mapping,
            String tournamentId, MeldelisteZiel ziel, List<String> fehler) throws GenerateException {
        List<String> fehlend;
        try {
            fehlend = RegistrationImportTask.pruefeVorTurnierstart(config, mapping, tournamentId, ziel);
        } catch (IOException e) {
            logger.error("PTM-Online: Online-Meldungen vor Turnierstart prüfen fehlgeschlagen", e);
            fehler.add(netzwerkFehlerText(e));
            return true;
        } catch (InterruptedException e) {
            logger.debug("PTM-Online: Prüfung vor Turnierstart abgebrochen", e);
            throw SheetRunner.verarbeitungAbgebrochen();
        }
        if (fehlend.isEmpty()) {
            return true;
        }
        MessageBoxResult antwort = MessageBox.from(ctx, MessageBoxTypeEnum.WARN_YES_NO)
                .caption(I18n.get("ptmonline.frage.fehlende_meldungen.titel"))
                .message(I18n.get("ptmonline.frage.fehlende_meldungen", fehlend.size(), namensListe(fehlend)))
                .show();
        return antwort == MessageBoxResult.YES;
    }

    private static String namensListe(List<String> namen) {
        if (namen.size() <= MAX_NAMEN_IN_RUECKFRAGE) {
            return String.join("\n", namen);
        }
        return String.join("\n", namen.subList(0, MAX_NAMEN_IN_RUECKFRAGE)) + "\n…";
    }

    /**
     * Legt für aktive Meldungen ohne Online-Zuordnung (vor Ort erfasst) eine neue Anmeldung an und pusht danach
     * die lokale Teilnahme aller online zugeordneten Meldungen – synchron, für den manuellen Abgleich und Tests.
     *
     * @return Bezeichnungen der Meldungen, die PTM-Online beim Anlegen als bereits angemeldet abgelehnt hat.
     */
    static List<String> statusPushenUndNeueAnlegen(MeldelisteZiel ziel, PtmOnlineRegistrationMapping mapping,
            TournamentSyncClient client, String tournamentId, List<LokaleOnlineMeldung> meldungen)
            throws IOException, InterruptedException, GenerateException {
        PtmOnlineStatusAbgleich.Ergebnis ergebnis = PtmOnlineStatusAbgleich.senden(
                statusAuftrag(ziel, mapping, tournamentId, false, meldungen), mapping, client);
        PtmOnlineStatusAbgleich.schreiben(mapping, ergebnis);
        return ergebnis.abgelehnt();
    }

    /**
     * Momentaufnahme für den Status-Abgleich: lokale UUIDs (fehlende werden angelegt), Teilnahme, Setzposition und
     * für aktive Meldungen ohne Online-Zuordnung die Anmeldedaten. Läuft im SheetRunner; danach braucht das Senden
     * das Dokument nicht mehr.
     */
    static PtmOnlineStatusAuftrag statusAuftrag(MeldelisteZiel ziel, PtmOnlineRegistrationMapping mapping,
            String tournamentId, boolean turnierStarten, List<LokaleOnlineMeldung> meldungen)
            throws GenerateException {
        Map<Integer, String> uuidProZeile = lokaleUuids(ziel, meldungen.stream()
                .map(LokaleOnlineMeldung::zeile1Basiert).filter(zeile -> zeile > 0).toList());
        Map<Integer, List<MeldelisteSpielerDaten>> proZeile = ziel.leseAlleSpielerRoh().stream()
                .collect(Collectors.groupingBy(MeldelisteSpielerDaten::zeile1Basiert, LinkedHashMap::new,
                        Collectors.toList()));
        Map<String, String> onlineIds = mapping.getOnlineIdsProUuid();
        List<PtmOnlineStatusAuftrag.Eintrag> eintraege = new ArrayList<>();
        for (LokaleOnlineMeldung meldung : meldungen) {
            String uuid = uuidProZeile.get(meldung.zeile1Basiert());
            if (uuid == null) {
                continue;
            }
            List<MeldelisteSpielerDaten> spieler = proZeile.getOrDefault(meldung.zeile1Basiert(), List.of());
            boolean neuAnlegen = meldung.teilnahme() == OnlineTeilnahme.AKTIV && !onlineIds.containsKey(uuid)
                    && !spieler.isEmpty();
            PtmOnlineStatusAuftrag.NeueAnlage anlage = neuAnlegen ? new PtmOnlineStatusAuftrag.NeueAnlage(
                    zuAnmeldung(spieler), bezeichnung(spieler), teamnummerFormel(ziel, uuid)) : null;
            eintraege.add(new PtmOnlineStatusAuftrag.Eintrag(uuid, meldung.teilnahme(), meldung.seedingPosition(),
                    anlage));
        }
        return new PtmOnlineStatusAuftrag(tournamentId, turnierStarten, eintraege);
    }

    static Map<Integer, Integer> zeileProTeam(MeldelisteZiel ziel) {
        Map<Integer, Integer> ergebnis = new LinkedHashMap<>();
        for (MeldelisteSpielerDaten spieler : ziel.leseAlleSpielerRoh()) {
            int teamNr = ziel.getTeamNrAusZeile(spieler.zeile1Basiert());
            if (teamNr > 0) {
                ergebnis.putIfAbsent(teamNr, spieler.zeile1Basiert());
            }
        }
        return ergebnis;
    }

    private static Map<String, String> formelnProUuid(MeldelisteZiel ziel) throws GenerateException {
        List<Integer> zeilen = ziel.leseAlleSpielerRoh().stream().map(MeldelisteSpielerDaten::zeile1Basiert)
                .distinct().toList();
        Map<String, String> ergebnis = new LinkedHashMap<>();
        for (String uuid : lokaleUuids(ziel, zeilen).values()) {
            ergebnis.put(uuid, teamnummerFormel(ziel, uuid));
        }
        return ergebnis;
    }

    /** Lokale UUIDs der Zeilen in einem Block; fehlende werden angelegt. */
    static Map<Integer, String> lokaleUuids(MeldelisteZiel ziel, Collection<Integer> zeilen1Basiert)
            throws GenerateException {
        try {
            return ziel.getOderErzeugeLokaleUuids(zeilen1Basiert);
        } catch (MeldelisteZiel.MeldelisteSchreibException e) {
            throw new GenerateException(e.getMessage());
        }
    }

    private static String teamnummerFormel(MeldelisteZiel ziel, String uuid) throws GenerateException {
        try {
            return ziel.formelTeamNrAusLokalerUuid(uuid);
        } catch (MeldelisteZiel.MeldelisteSchreibException e) {
            throw new GenerateException(e.getMessage());
        }
    }

    private static NeueOnlineAnmeldung zuAnmeldung(List<MeldelisteSpielerDaten> spieler) {
        MeldelisteSpielerDaten erster = spieler.get(0);
        MeldelisteSpielerDaten zweiter = spieler.size() >= 2 ? spieler.get(1) : null;
        MeldelisteSpielerDaten dritter = spieler.size() >= 3 ? spieler.get(2) : null;
        return new NeueOnlineAnmeldung(
                erster.vorname(), erster.nachname(), erster.vereinName(), null,
                zweiter != null ? zweiter.vorname() : null, zweiter != null ? zweiter.nachname() : null,
                dritter != null ? dritter.vorname() : null, dritter != null ? dritter.nachname() : null,
                null, true, true, List.of(), List.of());
    }

    private static String bezeichnung(List<MeldelisteSpielerDaten> spieler) {
        return spieler.stream().map(eintrag -> bezeichnung(eintrag.vorname(), eintrag.nachname()))
                .filter(name -> !name.isBlank()).collect(Collectors.joining(" / "));
    }

    private static String bezeichnung(String vorname, String nachname) {
        return ((vorname == null ? "" : vorname.strip()) + " " + (nachname == null ? "" : nachname.strip())).strip();
    }

    private static String netzwerkFehlerText(Exception e) {
        if (e instanceof PtmOnlineHttpException http && http.istBindungAbgeloest()) {
            return I18n.get("ptmonline.fehler.bindung_abgeloest");
        }
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }

    static void zeigeFehlerSammlung(XComponentContext ctx, List<String> fehler) {
        LoMainThread.post(ctx, () -> MessageBox.from(ctx, MessageBoxTypeEnum.WARN_OK)
                .caption(I18n.get("ptmonline.fehler.titel"))
                .message(I18n.get("ptmonline.fehler.rundenstart_abgleich", String.join("\n", fehler)))
                .show());
    }
}
