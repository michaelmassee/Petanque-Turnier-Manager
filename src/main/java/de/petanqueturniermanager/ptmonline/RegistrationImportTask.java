/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.LibreOfficePtmOnlineSpeicher;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.LoMainThread;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.onlinesync.SpieltagKontext;
import de.petanqueturniermanager.ptmonline.dto.RegistrationDto;
import de.petanqueturniermanager.spielerdb.MeldelisteZiel;
import de.petanqueturniermanager.spielerdb.MeldelisteZielFactory;
import de.petanqueturniermanager.spielerdb.MeldelisteSpielerDaten;
import de.petanqueturniermanager.spielerdb.SpielerMitVerein;

/**
 * Importiert online eingegangene, bestaetigte und noch nicht lokal vorhandene Anmeldungen
 * (PTM-Online) in die aktive Meldeliste. Nutzt denselben turniersystem-generischen Schreibpfad wie die Spieler-DB-
 * Integration ({@link MeldelisteZiel#schreibeBlock}, {@link MeldelisteZielFactory#starteMeldelisteUpdate}).
 * <p>
 * {@link #fuehreImportDurch} ist die synchrone Kernlogik: sie darf auf jedem Hintergrund-Thread
 * laufen (Sheet-Schreibzugriffe hier sind reine SheetRunner-Background-Thread-Operationen, siehe
 * CLAUDE.md-Threading-Regel) und wird sowohl vom menuegetriggerten {@link #starte} (eigener
 * Worker-Thread, UI-Feedback per {@link LoMainThread#post}) als auch synchron aus einem bereits
 * laufenden {@code SheetRunner} heraus genutzt (Rundenstart-Hook, {@link PtmOnlineSpielrundeSync}).
 */
public final class RegistrationImportTask {

    private static final Logger logger = LogManager.getLogger(RegistrationImportTask.class);

    private RegistrationImportTask() {}

    @FunctionalInterface
    public interface MeldelistenAktualisierung {
        void aktualisieren() throws GenerateException, InterruptedException;
    }

    private record GeschriebeneAnmeldung(RegistrationDto registration, int zeile1Basiert) {}

    public record AbgleichErgebnis(int lokalImportiert, int onlineAngelegt) {}

    public static void starte(WorkingSpreadsheet ws) {
        XComponentContext ctx = ws.getxContext();
        var config = new LibreOfficePtmOnlineSpeicher(ctx).laden();
        if (!config.isConfigured()) {
            zeigeFehler(ctx, I18n.get("ptmonline.fehler.nicht_konfiguriert"));
            return;
        }

        Optional<MeldelisteZiel> zielOpt = MeldelisteZielFactory.fuerAktivesSheet(ws);
        if (zielOpt.isEmpty()) {
            zeigeFehler(ctx, I18n.get("ptmonline.fehler.keine_meldeliste"));
            return;
        }

        TurnierSystem ts = new DocumentPropertiesHelper(ws).getTurnierSystemAusDocument();
        PtmOnlineRegistrationMapping mapping;
        Optional<String> tournamentId;
        try {
            Integer spieltagNr = SpieltagKontext.aktiverSpieltagOderNull(ws, ts);
            mapping = new PtmOnlineRegistrationMapping(ws, ts, spieltagNr);
            tournamentId = mapping.getTournamentId();
        } catch (GenerateException e) {
            zeigeFehler(ctx, e.getMessage());
            return;
        }
        if (tournamentId.isEmpty()) {
            zeigeFehler(ctx, I18n.get("ptmonline.fehler.turnier_nicht_angelegt"));
            return;
        }

        MeldelisteZiel ziel = zielOpt.get();
        PtmOnlineRegistrationMapping finaleMapping = mapping;
        String finaleTournamentId = tournamentId.get();

        Thread worker = new Thread(
                () -> importiereUndZeigeErgebnis(ws, ctx, config, finaleMapping, finaleTournamentId, ts, ziel),
                "PTM-Online-Import");
        worker.start();
    }

    private static void importiereUndZeigeErgebnis(WorkingSpreadsheet ws, XComponentContext ctx,
            LibreOfficePtmOnlineSpeicher.Zugangsdaten config, PtmOnlineRegistrationMapping mapping,
            String tournamentId, TurnierSystem ts, MeldelisteZiel ziel) {
        try {
            AbgleichErgebnis ergebnis = fuehreAbgleichDurch(ws, config, mapping, tournamentId, ts, ziel,
                    () -> aktualisiereMeldeliste(ws, ts));
            String meldung = I18n.get("ptmonline.erfolg.anmeldungen_abgeglichen",
                    ergebnis.lokalImportiert(), ergebnis.onlineAngelegt());
            LoMainThread.post(ctx, () -> zeigeInfo(ctx, meldung));
        } catch (IOException e) {
            logger.error("PTM-Online: Anmeldungen importieren fehlgeschlagen", e);
            LoMainThread.post(ctx, () -> zeigeNetzwerkFehler(ctx, e));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (GenerateException e) {
            logger.error("PTM-Online: Meldungen-Sheet lesen/schreiben fehlgeschlagen", e);
            LoMainThread.post(ctx, () -> zeigeFehler(ctx, e.getMessage()));
        }
    }

    /**
     * Holt neue Online-Anmeldungen, schreibt sie in die Meldeliste und aktualisiert das Mapping.
     * Synchron, blockierend (inkl. Warten auf den angestossenen Meldeliste-Update-Runner) - darf
     * NICHT vom LO-Main-Thread aus aufgerufen werden, wenn der Aufrufer selbst schon auf dem
     * Main-Thread laeuft und dort auf einen SheetRunner wartet (Deadlock-Risiko, siehe
     * {@code LoMainThread}-Dokumentation); auf jedem anderen (Hintergrund-)Thread unkritisch.
     *
     * @return Anzahl tatsaechlich importierter Anmeldungen (0, wenn keine neuen vorlagen oder keine
     *         zur Meldeliste-Formation passte).
     */
    public static int fuehreImportDurch(WorkingSpreadsheet ws, LibreOfficePtmOnlineSpeicher.Zugangsdaten config,
            PtmOnlineRegistrationMapping mapping, String tournamentId, TurnierSystem ts, MeldelisteZiel ziel)
            throws IOException, InterruptedException, GenerateException {
        return fuehreImportDurch(ws, config, mapping, tournamentId, ts, ziel, () -> aktualisiereMeldeliste(ws, ts));
    }

    /** Manueller Abgleich: importiert neue Online-Meldungen und legt neue lokale Meldungen online an. */
    public static AbgleichErgebnis fuehreAbgleichDurch(WorkingSpreadsheet ws,
            LibreOfficePtmOnlineSpeicher.Zugangsdaten config, PtmOnlineRegistrationMapping mapping, String tournamentId,
            TurnierSystem ts, MeldelisteZiel ziel, MeldelistenAktualisierung aktualisierung)
            throws IOException, InterruptedException, GenerateException {
        mapping.sicherstellen();
        int lokalImportiert = fuehreImportDurch(ws, config, mapping, tournamentId, ts, ziel, aktualisierung);
        int onlineAngelegt = neueLokaleMeldungenAnlegen(config, mapping, tournamentId, ziel);
        aktualisiereBezeichnungen(config, mapping, tournamentId, ziel);
        return new AbgleichErgebnis(lokalImportiert, onlineAngelegt);
    }

    public static int fuehreImportDurch(WorkingSpreadsheet ws, LibreOfficePtmOnlineSpeicher.Zugangsdaten config,
            PtmOnlineRegistrationMapping mapping, String tournamentId, TurnierSystem ts, MeldelisteZiel ziel,
            MeldelistenAktualisierung aktualisierung)
            throws IOException, InterruptedException, GenerateException {
        TournamentSyncClient client = new TournamentSyncClient(config.baseUrl(), config.apiKey());
        Instant abgleichStart = Instant.now();
        Instant since = mapping.getLastSync().orElse(Instant.EPOCH);
        List<RegistrationDto> alle = client.fetchRegistrations(tournamentId, since);
        uebernehmeOnlineStornierungen(ziel, mapping, alle);
        List<RegistrationDto> neue = new ArrayList<>();
        for (RegistrationDto reg : alle) {
            if (istImportierbar(reg) && !mapping.istBereitsImportiert(reg.id())) {
                neue.add(reg);
            }
        }
        if (neue.isEmpty()) {
            mapping.setLastSync(abgleichStart);
            return 0;
        }

        List<GeschriebeneAnmeldung> geschrieben = new ArrayList<>();
        boolean vollstaendigImportiert = true;
        Map<String, List<Integer>> vorhandeneZeilen = vorhandeneZeilenNachBesetzung(ziel);
        for (RegistrationDto reg : neue) {
            List<SpielerMitVerein> spieler = zuSpielerListe(reg, ziel.getFormation());
            if (spieler == null) {
                logger.warn("PTM-Online: Anmeldung {} passt nicht zur Formation {} der Meldeliste, übersprungen",
                        reg.id(), ziel.getFormation());
                vollstaendigImportiert = false;
                continue;
            }
            List<Integer> gleicheZeilen = vorhandeneZeilen.getOrDefault(besetzungsSchluessel(spieler), List.of());
            if (gleicheZeilen.size() == 1) {
                verknuepfeBestehendeZeile(mapping, ziel, reg, gleicheZeilen.get(0));
                continue;
            }
            if (gleicheZeilen.size() > 1) {
                logger.warn("PTM-Online: Anmeldung {} passt zu mehreren lokalen Meldelistenzeilen; nicht importiert", reg.id());
                vollstaendigImportiert = false;
                continue;
            }
            try {
                int zeile = ziel.schreibeBlockUndLiefereZeile(spieler);
                if (zeile <= 0) {
                    logger.error("PTM-Online: Anmeldung {} lieferte keine eindeutige Meldeliste-Zeile", reg.id());
                    vollstaendigImportiert = false;
                } else {
                    geschrieben.add(new GeschriebeneAnmeldung(reg, zeile));
                    vorhandeneZeilen.computeIfAbsent(besetzungsSchluessel(spieler), ignored -> new ArrayList<>()).add(zeile);
                }
            } catch (MeldelisteZiel.MeldelisteSchreibException e) {
                logger.error("PTM-Online: Anmeldung {} konnte nicht in die Meldeliste geschrieben werden", reg.id(), e);
                vollstaendigImportiert = false;
            }
        }
        if (geschrieben.isEmpty()) {
			if (vollstaendigImportiert) {
				mapping.setLastSync(abgleichStart);
			}
            return 0;
        }

        aktualisierung.aktualisieren();

        for (GeschriebeneAnmeldung geschriebene : geschrieben) {
            RegistrationDto reg = geschriebene.registration();
            try {
                String uuid = ziel.getOderErzeugeLokaleUuid(geschriebene.zeile1Basiert());
                mapping.addMapping(uuid, reg.id(), ziel.formelTeamNrAusLokalerUuid(uuid), executionRevision(reg),
                        lokaleBezeichnung(ziel, geschriebene.zeile1Basiert()), onlineBezeichnung(reg), onlineStatus(reg));
                mapping.setOnlineDetails(uuid, reg);
            } catch (MeldelisteZiel.MeldelisteSchreibException e) {
                logger.warn("PTM-Online: Lokale UUID für importierte Anmeldung {} nicht ermittelt", reg.id(), e);
                vollstaendigImportiert = false;
            }
        }
        if (vollstaendigImportiert) {
            mapping.setLastSync(abgleichStart);
        }
        return geschrieben.size();
    }

    /**
     * Nur bestaetigte Anmeldungen werden lokal in die Meldeliste uebernommen. Offene, Warteliste- und
     * stornierte Anmeldungen bleiben online; wird eine offene Anmeldung spaeter bestaetigt, liefert der
     * {@code since}-Abruf (Filter auf {@code updated_at}) sie beim naechsten Abgleich erneut.
     */
    static boolean istImportierbar(RegistrationDto registration) {
        return OnlineAnmeldeStatus.istBestaetigt(registration.status());
    }

    private static void verknuepfeBestehendeZeile(PtmOnlineRegistrationMapping mapping, MeldelisteZiel ziel,
            RegistrationDto reg, int zeile) throws GenerateException {
        try {
            String uuid = ziel.getOderErzeugeLokaleUuid(zeile);
            mapping.addMapping(uuid, reg.id(), ziel.formelTeamNrAusLokalerUuid(uuid), executionRevision(reg),
                    lokaleBezeichnung(ziel, zeile), onlineBezeichnung(reg), onlineStatus(reg));
            mapping.setOnlineDetails(uuid, reg);
        } catch (MeldelisteZiel.MeldelisteSchreibException e) {
            throw new GenerateException("Lokale vorhandene Anmeldung konnte nicht verknüpft werden: " + e.getMessage());
        }
    }

    private static Map<String, List<Integer>> vorhandeneZeilenNachBesetzung(MeldelisteZiel ziel) {
        return ziel.leseAlleSpielerRoh().stream().collect(Collectors.groupingBy(
                MeldelisteSpielerDaten::zeile1Basiert, LinkedHashMap::new, Collectors.toList())).values().stream()
                .collect(Collectors.groupingBy(RegistrationImportTask::besetzungsSchluessel,
                        LinkedHashMap::new,
                        Collectors.mapping(zeile -> zeile.get(0).zeile1Basiert(), Collectors.toCollection(ArrayList::new))));
    }

    private static String besetzungsSchluessel(List<? extends Object> spieler) {
        return spieler.stream().map(RegistrationImportTask::nameSchluessel).sorted().collect(Collectors.joining("\u0000"));
    }

    private static String nameSchluessel(Object spieler) {
        if (spieler instanceof SpielerMitVerein s) {
            return (s.vorname() + "\u0000" + s.nachname()).strip().toLowerCase(Locale.ROOT);
        }
        MeldelisteSpielerDaten s = (MeldelisteSpielerDaten) spieler;
        return (s.vorname() + "\u0000" + s.nachname()).strip().toLowerCase(Locale.ROOT);
    }

    private static void aktualisiereMeldeliste(WorkingSpreadsheet ws, TurnierSystem ts)
            throws GenerateException, InterruptedException {
        SheetRunner runner = MeldelisteZielFactory.starteMeldelisteUpdate(ws, ts);
        if (runner == null) {
            throw new GenerateException("Meldeliste konnte nicht aktualisiert werden");
        }
        runner.join();
        if (runner.isLetzterLaufFehlgeschlagen()) {
            throw new GenerateException("Meldeliste konnte nicht aktualisiert werden");
        }
    }

    private static int executionRevision(RegistrationDto registration) {
        return registration.executionRevision() == null ? 1 : Math.max(1, registration.executionRevision());
    }

    private static int neueLokaleMeldungenAnlegen(LibreOfficePtmOnlineSpeicher.Zugangsdaten config,
            PtmOnlineRegistrationMapping mapping, String tournamentId, MeldelisteZiel ziel)
            throws IOException, InterruptedException, GenerateException {
        String documentId = mapping.getSyncDocumentId()
                .orElseThrow(() -> new GenerateException(I18n.get("ptmonline.fehler.dokumentbindung_unvollstaendig")));
        String leaseToken = mapping.getLeaseToken()
                .orElseThrow(() -> new GenerateException(I18n.get("ptmonline.fehler.dokumentbindung_unvollstaendig")));
        TournamentSyncClient client = new TournamentSyncClient(config.baseUrl(), config.apiKey(), documentId, leaseToken);
        Map<Integer, List<MeldelisteSpielerDaten>> proZeile = ziel.leseAlleSpielerRoh().stream()
                .collect(Collectors.groupingBy(MeldelisteSpielerDaten::zeile1Basiert, LinkedHashMap::new, Collectors.toList()));
        int angelegt = 0;
        for (Map.Entry<Integer, List<MeldelisteSpielerDaten>> eintrag : proZeile.entrySet()) {
            List<MeldelisteSpielerDaten> spieler = eintrag.getValue();
            if (spieler.isEmpty()) {
                continue;
            }
            String uuid;
            try {
                uuid = ziel.getOderErzeugeLokaleUuid(eintrag.getKey());
            } catch (MeldelisteZiel.MeldelisteSchreibException e) {
                throw new GenerateException(e.getMessage());
            }
            if (mapping.getOnlineId(uuid).isPresent()) {
                continue;
            }
            RegistrationDto remote = client.upsertRegistration(tournamentId, uuid, zuOnlineAnmeldung(spieler));
            try {
                mapping.addMapping(uuid, remote.id(), ziel.formelTeamNrAusLokalerUuid(uuid), executionRevision(remote),
                        lokaleBezeichnung(ziel, eintrag.getKey()), onlineBezeichnung(remote), onlineStatus(remote));
                mapping.setOnlineDetails(uuid, remote);
            } catch (MeldelisteZiel.MeldelisteSchreibException e) {
                throw new GenerateException(e.getMessage());
            }
            angelegt++;
        }
        return angelegt;
    }

    private static de.petanqueturniermanager.ptmonline.dto.NeueOnlineAnmeldung zuOnlineAnmeldung(
            List<MeldelisteSpielerDaten> spieler) {
        MeldelisteSpielerDaten erster = spieler.get(0);
        MeldelisteSpielerDaten zweiter = spieler.size() > 1 ? spieler.get(1) : null;
        MeldelisteSpielerDaten dritter = spieler.size() > 2 ? spieler.get(2) : null;
        return new de.petanqueturniermanager.ptmonline.dto.NeueOnlineAnmeldung(
                erster.vorname(), erster.nachname(), erster.vereinName(), null,
                zweiter == null ? null : zweiter.vorname(), zweiter == null ? null : zweiter.nachname(),
                dritter == null ? null : dritter.vorname(), dritter == null ? null : dritter.nachname(),
                null, true, true, List.of(), List.of());
    }

    private static void aktualisiereBezeichnungen(LibreOfficePtmOnlineSpeicher.Zugangsdaten config,
            PtmOnlineRegistrationMapping mapping, String tournamentId, MeldelisteZiel ziel)
            throws IOException, InterruptedException, GenerateException {
        String documentId = mapping.getSyncDocumentId()
                .orElseThrow(() -> new GenerateException(I18n.get("ptmonline.fehler.dokumentbindung_unvollstaendig")));
        String leaseToken = mapping.getLeaseToken()
                .orElseThrow(() -> new GenerateException(I18n.get("ptmonline.fehler.dokumentbindung_unvollstaendig")));
        Map<String, RegistrationDto> remoteProId = new TournamentSyncClient(config.baseUrl(), config.apiKey(), documentId, leaseToken)
                .fetchRegistrations(tournamentId, null).stream()
                .collect(Collectors.toMap(RegistrationDto::id, registration -> registration));
        for (MeldelisteSpielerDaten spieler : ziel.leseAlleSpielerRoh()) {
            String uuid;
            try {
                uuid = ziel.getOderErzeugeLokaleUuid(spieler.zeile1Basiert());
            } catch (MeldelisteZiel.MeldelisteSchreibException e) {
                throw new GenerateException(e.getMessage());
            }
            Optional<String> onlineId = mapping.getOnlineId(uuid);
            if (onlineId.isEmpty()) {
                continue;
            }
            RegistrationDto remote = remoteProId.get(onlineId.get());
            mapping.setBezeichnungen(uuid, lokaleBezeichnung(ziel, spieler.zeile1Basiert()),
                    remote == null ? "" : onlineBezeichnung(remote), remote == null ? "" : onlineStatus(remote));
            if (remote != null) {
                mapping.setOnlineDetails(uuid, remote);
            }
        }
    }

    private static String lokaleBezeichnung(MeldelisteZiel ziel, int zeile1Basiert) {
        return ziel.leseAlleSpielerRoh().stream()
                .filter(spieler -> spieler.zeile1Basiert() == zeile1Basiert)
                .map(RegistrationImportTask::spielerBezeichnung)
                .filter(name -> !name.isBlank())
                .collect(Collectors.joining(" / "));
    }

    private static String onlineBezeichnung(RegistrationDto registration) {
        return java.util.stream.Stream.of(
                name(registration.firstName(), registration.lastName()),
                name(registration.partnerFirstName(), registration.partnerLastName()),
                name(registration.partner2FirstName(), registration.partner2LastName()))
                .filter(name -> !name.isBlank())
                .collect(Collectors.joining(" / "));
    }

    private static String spielerBezeichnung(MeldelisteSpielerDaten spieler) {
        return name(spieler.vorname(), spieler.nachname());
    }

    private static String name(String vorname, String nachname) {
        return (String.valueOf(vorname == null ? "" : vorname).strip() + " "
                + String.valueOf(nachname == null ? "" : nachname).strip()).strip();
    }

    private static void uebernehmeOnlineStornierungen(MeldelisteZiel ziel, PtmOnlineRegistrationMapping mapping,
            List<RegistrationDto> registrations) throws GenerateException {
        for (RegistrationDto registration : registrations) {
            if (!OnlineAnmeldeStatus.istStorniert(registration.status())) {
                continue;
            }
            Optional<String> lokaleUuid = mapping.getLokaleUuid(registration.id());
            if (lokaleUuid.isEmpty()) {
                continue;
            }
            for (MeldelisteSpielerDaten spieler : ziel.leseAlleSpielerRoh()) {
                try {
                    if (lokaleUuid.get().equals(ziel.getOderErzeugeLokaleUuid(spieler.zeile1Basiert()))) {
                        ziel.markiereAlsAbgemeldet(spieler.zeile1Basiert());
                        mapping.setBezeichnungen(lokaleUuid.get(), lokaleBezeichnung(ziel, spieler.zeile1Basiert()),
                                onlineBezeichnung(registration), onlineStatus(registration));
                        mapping.setOnlineDetails(lokaleUuid.get(), registration);
                        break;
                    }
                } catch (MeldelisteZiel.MeldelisteSchreibException e) {
                    throw new GenerateException(e.getMessage());
                }
            }
        }
    }

    private static String onlineStatus(RegistrationDto registration) {
        return OnlineAnmeldeStatus.anzeige(registration.status());
    }

    /**
     * Baut die Spielerliste passend zur Meldeliste-Formation. Liefert {@code null}, wenn die
     * Registrierung fuer die geforderte Spielerzahl nicht genug ausgefuellte Namen mitbringt
     * (defensiv statt Absturz — z.B. Doublette-Meldeliste, aber Anmeldung ohne Partnername).
     */
    private static @Nullable List<SpielerMitVerein> zuSpielerListe(RegistrationDto reg, Formation formation) {
        if (istLeer(reg.firstName()) || istLeer(reg.lastName())) {
            return null;
        }
        List<SpielerMitVerein> spieler = new ArrayList<>();
        spieler.add(neuerSpieler(reg.firstName(), reg.lastName(), reg.club(), reg.licenseNr()));

        if (formation.getAnzSpieler() >= 2) {
            if (istLeer(reg.partnerFirstName()) || istLeer(reg.partnerLastName())) {
                return null;
            }
            spieler.add(neuerSpieler(reg.partnerFirstName(), reg.partnerLastName(), reg.club(), null));
        }
        if (formation.getAnzSpieler() >= 3) {
            if (istLeer(reg.partner2FirstName()) || istLeer(reg.partner2LastName())) {
                return null;
            }
            spieler.add(neuerSpieler(reg.partner2FirstName(), reg.partner2LastName(), reg.club(), null));
        }
        return spieler;
    }

    private static boolean istLeer(@Nullable String wert) {
        return wert == null || wert.isBlank();
    }

    /** {@code nr=0}: Online-Anmeldungen haben keinen Bezug zu einem lokalen Spieler-DB-Datensatz. */
    private static SpielerMitVerein neuerSpieler(String vorname, String nachname, @Nullable String vereinName, @Nullable String lizenznr) {
        return new SpielerMitVerein(0, vorname, nachname, null, vereinName, List.of(), List.of(), lizenznr);
    }

    private static void zeigeNetzwerkFehler(XComponentContext ctx, IOException e) {
        String meldung = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        boolean nichtFreigeschaltet = meldung.contains(" 401");
        MessageBox.from(ctx, MessageBoxTypeEnum.ERROR_OK)
                .caption(I18n.get("ptmonline.fehler.titel"))
                .message(nichtFreigeschaltet
                        ? I18n.get("ptmonline.fehler.nicht_freigeschaltet")
                        : I18n.get("ptmonline.fehler.netzwerk", meldung))
                .show();
    }

    private static void zeigeFehler(XComponentContext ctx, String meldung) {
        MessageBox.from(ctx, MessageBoxTypeEnum.ERROR_OK)
                .caption(I18n.get("ptmonline.fehler.titel"))
                .message(meldung)
                .show();
    }

    private static void zeigeInfo(XComponentContext ctx, String meldung) {
        MessageBox.from(ctx, MessageBoxTypeEnum.INFO_OK)
                .caption(I18n.get("ptmonline.menu.toplevel"))
                .message(meldung)
                .show();
    }
}
