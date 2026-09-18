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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.LibreOfficePtmOnlineSpeicher;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.model.IMeldung;
import de.petanqueturniermanager.model.IMeldungen;
import de.petanqueturniermanager.onlinesync.SpieltagKontext;
import de.petanqueturniermanager.ptmonline.dto.NeueOnlineAnmeldung;
import de.petanqueturniermanager.ptmonline.dto.RegistrationDto;
import de.petanqueturniermanager.ptmonline.dto.RegistrationResultDto;
import de.petanqueturniermanager.spielerdb.MeldelisteSpielerDaten;
import de.petanqueturniermanager.spielerdb.MeldelisteZiel;
import de.petanqueturniermanager.spielerdb.MeldelisteZielFactory;

/**
 * Gleicht bei jedem Spielrunden-Start die Meldeliste des Turnierdokuments automatisch mit dem
 * verbundenen PTM-Online-Turnier ab (Prinzip "Turnierdokument ist Master"). Wird synchron aus dem
 * jeweiligen {@code *SpielrundeSheetNaechste}-{@code SheetRunner} heraus aufgerufen — läuft also
 * bereits auf einem Hintergrund-Thread, kein zusätzliches Threading/{@code LoMainThread} nötig
 * (Sheet-Schreibzugriffe und {@link MessageBox}-Dialoge sind auf einem SheetRunner-Thread erlaubt,
 * siehe CLAUDE.md-Threading-Regel).
 * <p>
 * No-Op, wenn PTM-Online nicht konfiguriert oder das Dokument nicht mit einem Online-Turnier
 * verbunden ist. Netzwerk-/API-Fehler werden gesammelt und am Ende in einer einzigen
 * {@link MessageBox}-Warnung gezeigt — die Spielrundenerstellung selbst läuft in jedem Fall weiter,
 * der Online-Abgleich darf den Turnierbetrieb nie blockieren.
 */
public final class PtmOnlineSpielrundeSync {

    private static final Logger logger = LogManager.getLogger(PtmOnlineSpielrundeSync.class);

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
     *                             Turniers (bzw. bei Supermelee: des Spieltags) erzeugt — startet in
     *                             diesem Fall zusätzlich das Online-Turnier.
     * @param alleTeamNummern      Team-/Spieler-Nummern aller aktuell in der Meldeliste erfassten
     *                             Teams (entspricht der Zeilennummerierung, die auch
     *                             {@link PtmOnlineRegistrationMapping} als Team-Nr verwendet).
     * @param aktiveTeamNummern    Teilmenge davon: aktuell aktiv/teilnehmend.
     * @param ausgestiegeneTeamNummern
     *                             Teilmenge davon: endgültig ausgestiegen (permanent, nicht nur für
     *                             die aktuelle Runde pausiert). Leer, wenn das Turniersystem diesen
     *                             Zustand nicht zuverlässig von "pausiert" unterscheiden kann (z.&nbsp;B.
     *                             Supermelee: AUSGESETZT ist nur ein Spieltag-Bye, keine dauerhafte
     *                             Abmeldung) — dann bleibt der Online-Status für diese Teams
     *                             unverändert, nur {@code active} wird gepusht.
     */
    public static void abgleichen(WorkingSpreadsheet ws, TurnierSystem ts, boolean istErsteRunde,
            Set<Integer> alleTeamNummern, Set<Integer> aktiveTeamNummern, Set<Integer> ausgestiegeneTeamNummern) {
        XComponentContext ctx = ws.getxContext();
        var config = new LibreOfficePtmOnlineSpeicher(ctx).laden();
        if (!config.isConfigured()) {
            return;
        }

        Optional<MeldelisteZiel> zielOpt = MeldelisteZielFactory.fuerAktivesSheet(ws);
        if (zielOpt.isEmpty()) {
            return;
        }
        MeldelisteZiel ziel = zielOpt.get();

        PtmOnlineRegistrationMapping mapping;
        Optional<String> tournamentIdOpt;
        try {
            Integer spieltagNr = SpieltagKontext.aktiverSpieltagOderNull(ws, ts);
            mapping = new PtmOnlineRegistrationMapping(ws, ts, spieltagNr);
            tournamentIdOpt = mapping.getTournamentId();
        } catch (GenerateException e) {
            logger.error("PTM-Online: Verbindungsdaten lesen fehlgeschlagen", e);
            zeigeFehlerSammlung(ctx, List.of(e.getMessage()));
            return;
        }
        if (tournamentIdOpt.isEmpty()) {
            return;
        }
        String tournamentId = tournamentIdOpt.get();
        TournamentSyncClient client = new TournamentSyncClient(config.baseUrl(), config.apiKey());
        List<String> fehler = new ArrayList<>();

        if (istErsteRunde) {
            try {
                client.start(tournamentId);
            } catch (IOException e) {
                logger.error("PTM-Online: Turnier starten fehlgeschlagen", e);
                fehler.add(netzwerkFehlerText(e));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }

        try {
            RegistrationImportTask.fuehreImportDurch(ws, config, mapping, tournamentId, ts, ziel);
        } catch (IOException e) {
            logger.error("PTM-Online: Anmeldungen importieren fehlgeschlagen", e);
            fehler.add(netzwerkFehlerText(e));
        } catch (GenerateException e) {
            logger.error("PTM-Online: Meldungen-Sheet lesen/schreiben fehlgeschlagen", e);
            fehler.add(e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        try {
            statusPushenUndNeueAnlegen(ziel, mapping, client, tournamentId,
                    alleTeamNummern, aktiveTeamNummern, ausgestiegeneTeamNummern);
        } catch (IOException e) {
            logger.error("PTM-Online: Status-Abgleich fehlgeschlagen", e);
            fehler.add(netzwerkFehlerText(e));
        } catch (GenerateException e) {
            logger.error("PTM-Online: Mapping-Sheet lesen/schreiben fehlgeschlagen", e);
            fehler.add(e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        try {
            mapping.setLastSync(Instant.now());
        } catch (GenerateException e) {
            logger.error("PTM-Online: Sync-Zeitpunkt schreiben fehlgeschlagen", e);
            fehler.add(e.getMessage());
        }

        if (!fehler.isEmpty()) {
            zeigeFehlerSammlung(ctx, fehler);
        }
    }

    /**
     * Pusht den lokalen Aktiv-/Ausgestiegen-Status aller bereits online zugeordneten Teams und legt
     * für lokal neu erfasste, aktive Teams ohne Online-Zuordnung eine neue Anmeldung an.
     */
    private static void statusPushenUndNeueAnlegen(MeldelisteZiel ziel, PtmOnlineRegistrationMapping mapping,
            TournamentSyncClient client, String tournamentId, Set<Integer> alle, Set<Integer> aktive,
            Set<Integer> ausgestiegen) throws IOException, InterruptedException, GenerateException {
        List<RegistrationResultDto> results = new ArrayList<>();
        for (int teamNr : alle) {
            Optional<String> onlineId = mapping.getOnlineId(teamNr);
            if (onlineId.isEmpty()) {
                continue;
            }
            boolean istAktiv = aktive.contains(teamNr);
            String status = ausgestiegen.contains(teamNr) ? "withdrawn" : null;
            results.add(new RegistrationResultDto(onlineId.get(), status, teamNr, istAktiv));
        }
        if (!results.isEmpty()) {
            client.pushResults(tournamentId, results);
        }

        Map<Integer, List<MeldelisteSpielerDaten>> proTeam = ziel.leseAlleSpielerRoh().stream()
                .collect(Collectors.groupingBy(MeldelisteSpielerDaten::zeile1Basiert, LinkedHashMap::new, Collectors.toList()));

        for (int teamNr : aktive) {
            if (mapping.getOnlineId(teamNr).isPresent()) {
                continue;
            }
            List<MeldelisteSpielerDaten> spieler = proTeam.get(teamNr);
            if (spieler == null || spieler.isEmpty()) {
                continue;
            }
            NeueOnlineAnmeldung anmeldung = zuAnmeldung(spieler);
            RegistrationDto angelegt = client.createRegistration(tournamentId, anmeldung);
            mapping.addMapping(teamNr, angelegt.id(), anmeldung.firstName(), anmeldung.lastName());
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
                null, true, true, List.of());
    }

    private static String netzwerkFehlerText(IOException e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }

    private static void zeigeFehlerSammlung(XComponentContext ctx, List<String> fehler) {
        MessageBox.from(ctx, MessageBoxTypeEnum.WARN_OK)
                .caption(I18n.get("ptmonline.fehler.titel"))
                .message(I18n.get("ptmonline.fehler.rundenstart_abgleich", String.join("\n", fehler)))
                .show();
    }
}
