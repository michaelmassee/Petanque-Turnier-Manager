/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
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
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.LoMainThread;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxResult;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.model.IMeldung;
import de.petanqueturniermanager.model.IMeldungen;
import de.petanqueturniermanager.ptmonline.auftrag.AuftragsBestand;
import de.petanqueturniermanager.ptmonline.auftrag.SyncAuftrag;
import de.petanqueturniermanager.spielerdb.MeldelisteZiel;
import de.petanqueturniermanager.spielerdb.MeldelisteSpielerDaten;
import de.petanqueturniermanager.spielerdb.MeleeAnmeldungZiel;

/**
 * Gleicht bei jedem Spielrunden-Start die Meldeliste des Turnierdokuments automatisch mit dem
 * verbundenen PTM-Online-Turnier ab (Prinzip "Turnierdokument ist Master"). Wird synchron aus dem
 * jeweiligen {@code *SpielrundeSheetNaechste}-{@code SheetRunner} heraus aufgerufen.
 * <p>
 * Synchron läuft nur der lokale Teil: Nr-Formeln, lokale UUIDs, vor der ersten Runde der Vorabcheck mit kurzem
 * Zeitlimit, und das Speichern der Schreibaufträge ({@link PtmOnlineAuftraege}). Die erste Runde verwirft
 * ungesendete Aufträge der Anmeldephase und setzt {@code running} als nächsten Auftrag (KP-05); danach folgt der
 * vollständige Teilnahme-Stand. Gesendet wird im Hintergrund ({@link PtmOnlineLiveBeobachter}) mit Wiederholung bei
 * Netzfehlern: die Auslosung wartet nie auf das Netz. Lokale Meldungen ohne Online-Zuordnung bleiben ab dem Start rein
 * lokal (E-13).
 * <p>
 * No-Op, wenn PTM-Online nicht konfiguriert oder das Dokument nicht verbunden ist. Bei pausiertem Sync entsteht vor
 * der ersten Runde nur auf ausdrücklichen Wunsch der Übergang zu {@code running} (P-58), sonst nichts.
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

    private static void abgleichen(WorkingSpreadsheet ws, PtmOnlineVerbindung verbindung, boolean istErsteRunde,
            Set<Integer> alleTeamNummern, Set<Integer> aktiveTeamNummern, Set<Integer> ausgestiegeneTeamNummern)
            throws GenerateException {
        XComponentContext ctx = ws.getxContext();
        MeldelisteZiel ziel = verbindung.ziel();
        PtmOnlineRegistrationMapping mapping = verbindung.mapping();
        String tournamentId = verbindung.tournamentId();
        boolean pausiert = !istSyncAktiv(mapping);
        List<String> fehler = new ArrayList<>();

        if (!pausiert) {
            aktualisiereNrFormeln(ziel, mapping, fehler);
        }
        boolean startSenden = true;
        if (istErsteRunde) {
            startSenden = pausiert ? nurStartSendenTrotzPause(ctx) : vorabcheckBestaetigt(ctx, verbindung);
        }

        AuftragsBestand bestand = PtmOnlineAuftraege.bestand(ws, mapping, verbindung.spieltagNr());
        if (istErsteRunde) {
            Instant lokalerStart = Instant.now();
            mapping.setRunningAusstehendSeit(lokalerStart);
            if (startSenden) {
                List<SyncAuftrag> verworfen = PtmOnlineAuftraege.start(bestand, tournamentId, lokalerStart,
                        I18n.get("ptmonline.auftrag.verworfen.turnierstart"));
                if (!verworfen.isEmpty()) {
                    logger.info("PTM-Online: {} ungesendete Aufträge der Anmeldephase beim Turnierstart verworfen",
                            verworfen.size());
                }
            }
        }
        if (!pausiert) {
            PtmOnlineStatusAuftrag status = statusAuftrag(ziel, tournamentId, lokaleMeldungen(ziel,
                    verbindung.meldeliste(), alleTeamNummern, aktiveTeamNummern, ausgestiegeneTeamNummern));
            PtmOnlineAuftraege.teilnahme(bestand, tournamentId, status.eintraege(), mapping.getOnlineIdsProUuid(),
                    mapping.getExecutionRevisionenProUuid());
            PtmOnlineLiveBeobachter.teilnahmeErfasst(ws.getWorkingSpreadsheetDocument(), status);
        }
        PtmOnlineAuftraege.speichern(bestand, mapping);
        PtmOnlineLiveBeobachter.anstossen(ws.getWorkingSpreadsheetDocument());

        if (!fehler.isEmpty()) {
            zeigeFehlerSammlung(ctx, fehler);
        }
    }

    private static void aktualisiereNrFormeln(MeldelisteZiel ziel, PtmOnlineRegistrationMapping mapping,
            List<String> fehler) {
        try {
            mapping.aktualisiereAnzeigeFormeln(formelnProUuid(ziel));
        } catch (GenerateException e) {
            logger.error("PTM-Online: Nr-Formeln der Zuordnung konnten nicht aktualisiert werden", e);
            fehler.add(e.getMessage());
        } catch (RuntimeException e) {
            logger.error("PTM-Online: Unerwarteter Mapping-Fehler", e);
            fehler.add(netzwerkFehlerText(e));
        }
    }

    /**
     * Pausierter Sync vor der ersten Runde (P-58): Die Online-Anmeldung bliebe offen. Auf Wunsch wird trotz Pause nur
     * der Übergang zu {@code running} gesendet; alle übrigen Übertragungen bleiben pausiert. Sonst bleibt
     * {@code running} ausstehend und wird beim Fortsetzen zuerst gesendet.
     *
     * @return {@code true}, wenn {@code running} sofort gesendet werden soll
     */
    private static boolean nurStartSendenTrotzPause(XComponentContext ctx) {
        return MessageBox.from(ctx, MessageBoxTypeEnum.WARN_YES_NO)
                .caption(I18n.get("ptmonline.frage.pause_start.titel"))
                .message(I18n.get("ptmonline.frage.pause_start")).show() == MessageBoxResult.YES;
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
     * Vorabcheck vor der ersten Runde (E-04, E-05, KP-05): Die Turnierleitung bestätigt ausdrücklich, dass mit der
     * Runde die Online-Anmeldung geschlossen wird – mit der Liste bestätigter Online-Meldungen, die in der Meldeliste
     * fehlen. Ist PTM-Online nicht erreichbar, nennt die Rückfrage den letzten erfolgreichen Abgleich; die Bestätigung
     * wird im Sync-Blatt protokolliert (P-25). Übernommen wird hier nichts – dafür ist der manuelle Abgleich da.
     *
     * @return immer {@code true}; ohne Bestätigung wird die Runde abgebrochen
     */
    private static boolean vorabcheckBestaetigt(XComponentContext ctx, PtmOnlineVerbindung verbindung)
            throws GenerateException {
        PtmOnlineRegistrationMapping mapping = verbindung.mapping();
        MessageBoxResult antwort;
        try {
            List<String> fehlend = RegistrationImportTask.pruefeVorTurnierstart(verbindung.config(), mapping,
                    verbindung.tournamentId(), verbindung.ziel());
            String meldung = fehlend.isEmpty() ? I18n.get("ptmonline.frage.turnierstart")
                    : I18n.get("ptmonline.frage.fehlende_meldungen", fehlend.size(), namensListe(fehlend));
            antwort = MessageBox.from(ctx, MessageBoxTypeEnum.WARN_YES_NO)
                    .caption(I18n.get("ptmonline.frage.fehlende_meldungen.titel")).message(meldung).show();
        } catch (IOException e) {
            logger.warn("PTM-Online: Online-Meldungen vor Turnierstart nicht prüfbar", e);
            Optional<Instant> letzterSync = mapping.getLastSync();
            antwort = MessageBox.from(ctx, MessageBoxTypeEnum.WARN_YES_NO)
                    .caption(I18n.get("ptmonline.frage.fehlende_meldungen.titel"))
                    .message(I18n.get("ptmonline.frage.turnierstart_offline", netzwerkFehlerText(e),
                            letzterSync.map(PtmOnlineSpielrundeSync::zeitpunktText)
                                    .orElse(I18n.get("ptmonline.letzter_sync.nie"))))
                    .show();
            if (antwort == MessageBoxResult.YES) {
                mapping.protokolliereOfflineBestaetigung(Instant.now(), letzterSync.orElse(null));
            }
        } catch (InterruptedException e) {
            logger.debug("PTM-Online: Prüfung vor Turnierstart abgebrochen", e);
            throw SheetRunner.verarbeitungAbgebrochen();
        }
        if (antwort != MessageBoxResult.YES) {
            throw SheetRunner.verarbeitungAbgebrochen();
        }
        return true;
    }

    private static String zeitpunktText(Instant zeitpunkt) {
        return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault())
                .format(zeitpunkt);
    }

    private static String namensListe(List<String> namen) {
        if (namen.size() <= MAX_NAMEN_IN_RUECKFRAGE) {
            return String.join("\n", namen);
        }
        return String.join("\n", namen.subList(0, MAX_NAMEN_IN_RUECKFRAGE)) + "\n…";
    }

    /**
     * Teilnahme und Setzposition aller lokalen Meldungen mit ihren lokalen UUIDs (fehlende werden angelegt). Läuft im
     * SheetRunner; der daraus erzeugte Auftrag braucht das Dokument nicht mehr.
     */
    static PtmOnlineStatusAuftrag statusAuftrag(MeldelisteZiel ziel, String tournamentId,
            List<LokaleOnlineMeldung> meldungen) throws GenerateException {
        Map<Integer, String> uuidProZeile = lokaleUuids(ziel, meldungen.stream()
                .map(LokaleOnlineMeldung::zeile1Basiert).filter(zeile -> zeile > 0).toList());
        List<PtmOnlineStatusAuftrag.Eintrag> eintraege = new ArrayList<>();
        for (LokaleOnlineMeldung meldung : meldungen) {
            String uuid = uuidProZeile.get(meldung.zeile1Basiert());
            if (uuid != null) {
                eintraege.add(new PtmOnlineStatusAuftrag.Eintrag(uuid, meldung.teilnahme(), meldung.seedingPosition()));
            }
        }
        return new PtmOnlineStatusAuftrag(tournamentId, eintraege);
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
